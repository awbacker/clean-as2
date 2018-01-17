package org.cleanas2.service.net.util

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.util.Args
import org.boon.Str
import org.cleanas2.common.MdnMode
import org.cleanas2.common.disposition.DispositionOptions
import org.cleanas2.common.disposition.DispositionType
import org.cleanas2.common.exception.AS2Exception
import org.cleanas2.common.exception.DispositionException
import org.cleanas2.message.*
import org.cleanas2.util.Constants
import org.joda.time.DateTime

import javax.mail.MessagingException
import javax.mail.internet.*
import java.io.IOException

import org.apache.commons.lang3.StringUtils.isBlank
import org.boon.Maps.map
import org.cleanas2.util.AS2Util.getOrBlank
import org.cleanas2.util.AS2Util.getOrDefault

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
object MdnUtil {

    private val logger = LogFactory.getLog(MdnUtil::class.java.simpleName)

    /**
     * Validates that MIC code string (INCLUDING THE HASH ALGORITHM USED, EG 0x090293029,SHA1)
     * This header is returned (or calculated) as an rfc822 header, so spaces are allowed everywhere
     * and we need to remove them all first.
     */
    fun validateReturnedMic(originalMic: String, returnedMic: String): Boolean {
        val returned = returnedMic.replace("\\s+".toRegex(), "")
        val original = originalMic.replace("\\s+".toRegex(), "")
        if (returned != original) {
            logger.info(String.format("mic not matched, original mic: '%s' return mic: '%s'", original, returned))
            return false
            // file was sent completely but the returned mic was not matched,
            // don't know it needs or needs not to be resent ? it's depended on what !
            // anyway, just log the warning message here.
            //E2ELogUtil.mdnReceiveInfo(msg, String.format("MIC does not match.  Expected: %s, got : %s", originalMic, returnedMic));
        }
        return true
    }

    /**
     * Validates the disposition returned by the server, and throws an exception if it is not "processed".
     */
    @Throws(DispositionException::class)
    fun validateMdnDisposition(msg: OutgoingFileMessage, mdn: IncomingSyncMdn) {
        Args.notNull(msg, "AS2 Outgoing Message")
        Args.notNull(mdn, "AS2 Outgoing MDN")
        val d = DispositionType.fromString(mdn.attributes.contentDisposition)
        if (!d.isSuccess) {
            val de = DispositionException(d, "Disposition indicates a server processing error")
            de.text = mdn.bodyText
            throw de
        }
    }

    /**
     * Parses the MDN attributes from the incoming MIME Body part, and puts them in the MDN.  Verifies the signature, if present.
     * This does not modify the original MDN
     *
     * @param mdn             The MDN to set the attributes/text on
     * @param mdnMimeBodyPart The MIME part to retrieve the data from
     */
    @Throws(MessagingException::class, IOException::class)
    fun populateMdnFromMimeBodyPart(mdn: IncomingMdnBase, mdnMimeBodyPart: MimeBodyPart) {
        if (!isBodyPartMdn(mdnMimeBodyPart)) return
        val reportParts = MimeMultipart(mdnMimeBodyPart.dataHandler.dataSource)

        for (j in 0 until reportParts.count) {
            val reportPart = reportParts.getBodyPart(j) as MimeBodyPart
            if (reportPart.isMimeType("text/plain")) {
                mdn.bodyText = reportPart.content.toString()
            }

            //AS2-SPEC: This is the content type returned with the body when an AS2 server is sending an MDN
            if (reportPart.isMimeType("message/disposition-notification")) {
                val ih = InternetHeaders(reportPart.inputStream)
                // Use the IH wrapper to read the headers from the BODY of the message.  these will not be present
                // in the ACTUAL headers of the reportPart
                //DebugUtil.debugPrintHeaders(logger, "Headers from reportPart.getInputStream()", ih);
                //DebugUtil.debugPrintHeaders(logger, "Headers from the reportPart itself", reportPart.getAllHeaders());

                mdn.attributes.reportingUa = ih.getHeader("Reporting-UA", ", ")
                mdn.attributes.originalRecipient = ih.getHeader("Original-Recipient", ",")
                mdn.attributes.finalRecipient = ih.getHeader("Final-Recipient", ",")
                mdn.attributes.originalMessageId = ih.getHeader("Original-Message-ID", ", ")
                mdn.attributes.contentDisposition = ih.getHeader("Disposition", ", ")
                mdn.attributes.receivedContentMic = ih.getHeader("Received-Content-MIC", ", ")
            }
        }
    }


    /**
     * Checks if an MIME body part is an MDN or not.  This looks at the main part of the content type for "multipart/report"
     */
    @Throws(MessagingException::class)
    fun isBodyPartMdn(mdnMimeBodyPart: MimeBodyPart): Boolean {
        val reportParts = MimeMultipart(mdnMimeBodyPart.dataHandler.dataSource)
        val reportType = ContentType(reportParts.contentType)
        return "multipart/report".equals(reportType.baseType, ignoreCase = true)
    }


    /**
     * Creates an MDN that should be SENT in RESPONSE to a file being received.  This is pre-populated with
     * all the required book-keeping values (version/date/server/subject/etc), headers to be sent, and etc
     *
     * @param message     The message that we received
     * @param disposition The content disposition to send
     * @param fromHeader  The value to put in the "From" header.  Not terribly important, usually the email address
     */
    @Throws(AS2Exception::class)
    fun createReplyMdn(message: IncomingFileMessage, disposition: DispositionType, fromHeader: String): ReplyMdn {

        val mdn = ReplyMdn()
        mdn.mdnMode = message.mdnMode

        if (message.mdnMode == MdnMode.ASYNC) {
            // [spec 7.3.1] this will already have been verified to be present when the incoming file message was created, if it was ASYNC
            mdn.asyncReplyToUrl = getOrDefault(message.requestHeaders, "Receipt-Delivery-Option", "")
        }
        mdn.partnerId = message.senderId
        mdn.companyId = message.receiverId
        mdn.responseHeaders.putAll(map(
                "Connection", "close, TE",
                "AS2-Version", Constants.AS2_PROTOCOL_VERSION,
                "Date", DateTime.now().toString("EEE, dd MMM yyyy HH:mm:ss Z"),
                "Server", Constants.AS2_SERVER_SENDER_NAME,
                "Mime-Version", Constants.MIME_VERSION_1_0,
                "AS2-From", message.receiverId, // swap the from/notifyTo ids when we send the MDN
                "AS2-To", message.senderId,
                "From", fromHeader,
                "Subject", "MDN Response"
        ))

        mdn.attributes.reportingUa = Constants.AS2_SERVER_SENDER_NAME + "@" + message.connectionInfo.destinationIp + ":" + message.connectionInfo.destinationPort
        mdn.attributes.originalRecipient = "rfc822; " + message.receiverId
        mdn.attributes.finalRecipient = "rfc822; " + message.receiverId // we don't have any re-mapping
        mdn.attributes.originalMessageId = getOrDefault(message.requestHeaders, "Message-ID", "")
        mdn.attributes.contentDisposition = disposition.toString()

        if (message.requestHeaders.containsKey("Disposition-Notification-Options")) {
            // we really should store the disposition options in the message, but we can't right now since
            // we can't deserialize the JSON back into the message correctly, so we have to do it this shitty way for the moment
            val orDefault = getOrDefault(message.requestHeaders, "Disposition-Notification-Options", "")
            val dispOptions = DispositionOptions(orDefault)
            mdn.isSignedReply = !isBlank(dispOptions.protocol)
            mdn.signedReceiptMicAlgorithm = getOrBlank(dispOptions.micAlgorithm)
        } else {
            mdn.isSignedReply = false
            mdn.signedReceiptMicAlgorithm = ""
        }

        return mdn
    }

    /**
     * Creates the MIME body part to send as an MDN.  This is used by both the Async sender and the File Receiver.
     * This method is used so that the ReplyMdn does not need to save the calculated MIME body part, since it may be
     * serialized and saved for later in the case of a "retry" (not currently implemented)
     */
    @Throws(MessagingException::class)
    fun createMdnMimeData(mdn: ReplyMdn): MimeBodyPart {
        val multiPart = MimeMultipart()
        multiPart.addBodyPart(MimeUtil.textBodyPart(mdn.bodyText))
        multiPart.addBodyPart(MimeUtil.textBodyPart(Str.EMPTY_STRING, "message/disposition-notification", map(
                "Reporting-UA", mdn.attributes.reportingUa,
                "Original-Recipient", mdn.attributes.originalRecipient,
                "Final-Recipient", mdn.attributes.finalRecipient,
                "Original-Message-ID", mdn.attributes.originalMessageId,
                "Disposition", mdn.attributes.contentDisposition,
                "Received-Content-MIC", mdn.attributes.receivedContentMic
        )))
        multiPart.setSubType("report; report-type=disposition-notification")
        return MimeUtil.multiPartToBodyPart(multiPart)
    }
}
