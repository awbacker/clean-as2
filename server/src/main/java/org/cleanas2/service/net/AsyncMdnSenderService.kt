package org.cleanas2.service.net

import net.engio.mbassy.listener.Handler
import org.apache.commons.io.IOUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.cleanas2.bus.SendAsyncMdnMsg
import org.cleanas2.common.exception.AS2Exception
import org.cleanas2.message.ReplyMdn
import org.cleanas2.service.CertificateService
import org.cleanas2.service.net.util.MdnUtil
import org.cleanas2.service.net.util.MimeUtil
import org.cleanas2.service.net.util.NetUtil
import org.cleanas2.util.DebugUtil

import javax.inject.Inject
import javax.mail.MessagingException
import javax.mail.internet.MimeBodyPart
import java.io.IOException

import org.boon.Maps.map

/**
 * Sends an MDN reply.  The MDN information is created by the receiver, this class
 * simply handles the sending, signing, and MDN specific parts.  For example, the headers
 * to send are calculated by the receiver.  This way we avoid carrying around a LOT of extra state
 * inside the original "message", and decouple sending an MDN from the receipt of the file.
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class AsyncMdnSenderService @Inject
constructor(private val certs: CertificateService) {

    @Handler
    fun sendAsyncMdn(busMessage: SendAsyncMdnMsg) {
        try {
            logger.info("Starting to send Async MDN")
            // E2ELogUtil.mdnAsyncSendStart(as2message);
            send(busMessage.mdn)
            logger.debug("Async MDN - Sent Successfully")
        } catch (e: Exception) {
            logger.info("Error sending async MDN", e)
            // E2ELogUtil.mdnAsyncSendError(as2message, ex, "Error sending ASYNC mdn");
        } finally {
            logger.debug("Async MDN Sent")
            // E2ELogUtil.mdnAsyncSendEnd(as2message);
        }
    }

    @Throws(AS2Exception::class, IOException::class, MessagingException::class)
    private fun send(mdn: ReplyMdn) {

        HttpClients.createMinimal().use { client ->
            // this code is the same as the code in the FileReceiverHandler.  Should it be merged somehow?

            var outBody = MdnUtil.createMdnMimeData(mdn)
            if (mdn.isSignedReply) {
                try {
                    outBody = MimeUtil.signBodyPart(
                            outBody,
                            certs.getCertificate(mdn.companyId),
                            certs.getPrivateKey(mdn.companyId),
                            mdn.signedReceiptMicAlgorithm
                    )
                } catch (e: Exception) {
                    // this also cached any GeneralCertificateExceptions raised by not finding
                    // the certificate/key for the company
                    logger.error("Error signing MDN for received file, sending unsigned", e)
                    // todo? retry? send other MDN? not clear
                }

            }

            // PREPARE THE POST AND DEBUG THE STATUS     -------------------------------------------------
            val post = HttpPost(mdn.asyncReplyToUrl)
            post.setHeaders(NetUtil.mapToHttpHeaders(mdn.responseHeaders))
            post.setHeader("Content-Type", outBody.contentType)
            post.entity = ByteArrayEntity(IOUtils.toByteArray(outBody.inputStream))

            DebugUtil.debugPrintObject(logger, "Async MDN - POST prepared", map(
                    "bytes", post.entity.contentLength,
                    "url", post.uri.toString(),
                    "content type", post.getFirstHeader("Content-Type"),
                    "original message id", mdn.attributes.originalMessageId
            ))

            // EXECUTE THE ACTUAL POST -------------------------------------------------------------------
            try {
                client.execute(post, AsyncMdnResponseHandler(mdn, post))
            } catch (ex: ClientProtocolException) {
                // todo: resend if a network error occurs during transmission?
                logger.debug("error sending async mdn")
                //E2ELogUtil.mdnAsyncSendError(mdn, ex, "Http Response Error");
                throw ex
            }
        }
    }

    /**
     * Handles the response received (or not received) when sending an ASYNC mdn
     */
    private class AsyncMdnResponseHandler(private val mdn: ReplyMdn, private val post: HttpPost) : ResponseHandler<Void> {

        @Throws(IOException::class)
        override fun handleResponse(response: HttpResponse): Void? {
            val status = response.statusLine
            logger.debug("Async MDN - Received Reply : " + status)
            if (NetUtil.isInvalidResponseCode(response)) {
                logger.error(String.format("Failed to send async MDN, status code %d (%s) - %s - %s",
                        status.statusCode,
                        status.reasonPhrase,
                        status.toString(),
                        mdn.attributes.originalMessageId))
                throw ClientProtocolException("Unexpected Error: " + post.uri.toString() + " " + status.statusCode + " " + status.reasonPhrase)
            }
            // consume the response, if there is one, so the connection will close properly
            EntityUtils.consumeQuietly(response.entity)
            return null
        }
    }

    companion object {

        private val logger = LogFactory.getLog(AsyncMdnSenderService::class.java.simpleName)
    }

}

