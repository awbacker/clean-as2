package org.cleanas2.service.net.processor

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.entity.StringEntity
import org.cleanas2.server.MessageBus
import org.cleanas2.bus.DeletePendingMdnMsg
import org.cleanas2.bus.LoadPendingMdnMsg
import org.cleanas2.common.MdnReceiveStatus
import org.cleanas2.message.IncomingAsyncMdn
import org.cleanas2.message.IncomingMessage
import org.cleanas2.service.net.util.MdnUtil
import org.cleanas2.common.disposition.DispositionType

import javax.mail.MessagingException
import javax.mail.internet.MimeBodyPart
import java.io.IOException

/**
 * Handle basic processing when a request has been determined to be an incoming ASYNC MDN.  This is
 * in its own class because this logic is shared by the AsyncMdnReceiver AND the FileReceiverService.
 *
 * It is possible that the remote partner will ignore the URL provided (I'm looking at you, Cyclone),
 * and actually send the Async MDN to the same url:port that sent the file, so we need to handle that in both places.
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class AsyncMdnProcessor(msg: IncomingMessage, private val decryptedData: MimeBodyPart) {
    private val mdn: IncomingAsyncMdn = IncomingAsyncMdn(msg)

    @Throws(IOException::class, MessagingException::class)
    fun process(response: HttpResponse) {
        when (processMdn()) {
            MdnReceiveStatus.OK -> {
                logger.debug("successfully received MDN")
                //E2ELogUtil.mdnAsyncReceiveInfo(as2msg, "Successfully received");
                response.setStatusCode(HttpStatus.SC_OK)
                response.entity = StringEntity("MDN Received Successfully")
            }
            MdnReceiveStatus.MIC_NOT_MATCHED -> {
                logger.debug("MDN MIC does not match")
                //E2ELogUtil.mdnAsyncReceiveError(as2msg, null, "Returned MIC does not match, or file not found in directory");
                response.setStatusCode(HttpStatus.SC_NOT_FOUND)
                response.entity = StringEntity("The MIC does not match")
            }
            MdnReceiveStatus.NO_CONTENT -> {
                //E2ELogUtil.mdnAsyncReceiveError(as2msg, null, "No MDN found in incoming request");
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST)
                response.entity = StringEntity("No MDN entity found in request body")
            }
            MdnReceiveStatus.INVALID_DISPOSITION -> {
                logger.debug("MDN Disposition returned from the partner is invalid for: " + mdn.loggingText)
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST)
                response.entity = StringEntity("The POST did not contain a valid content-disposition for the MDN")
            }
            MdnReceiveStatus.PROCESSING_FAILED -> {
                response.setStatusCode(HttpStatus.SC_OK)
                logger.debug("MDN indicates that processing failed for: " + mdn.loggingText)
            }
            MdnReceiveStatus.ASYNC_LOAD_ERROR -> logger.error("Error loading pending MDN info.  Maybe already received?")
        }// todo: move file to "error" directory
        // ok, the server says processing failed.  now what do we do? move the file back?
    }

    @Throws(IOException::class, MessagingException::class)
    private fun processMdn(): MdnReceiveStatus {
        MdnUtil.populateMdnFromMimeBodyPart(mdn, decryptedData)

        val dt = DispositionType.fromString(mdn.attributes.contentDisposition)

        if (!dt.isFormatValid) return MdnReceiveStatus.INVALID_DISPOSITION
        if (!dt.isSuccess) return MdnReceiveStatus.PROCESSING_FAILED

        // send a message to load the MIC data from the file
        val msg = LoadPendingMdnMsg(mdn.attributes.originalMessageId)
        MessageBus.publish(msg)
        if (msg.isError) {
            return MdnReceiveStatus.ASYNC_LOAD_ERROR
        }

        // compare the MIC codes
        val outgoingMic = msg.fileData?.outgoingMic ?: "" // todo()
        val partnerCalculatedMic = mdn.attributes.receivedContentMic
        if (!MdnUtil.validateReturnedMic(outgoingMic, partnerCalculatedMic)) {
            return MdnReceiveStatus.MIC_NOT_MATCHED
        }

        // send a message (out of band) to delete the pending mdn files
        MessageBus.publishAsync(DeletePendingMdnMsg(mdn.attributes.originalMessageId))

        return MdnReceiveStatus.OK
    }

    companion object {

        private val logger = LogFactory.getLog(AsyncMdnProcessor::class.java.simpleName)
    }

}
