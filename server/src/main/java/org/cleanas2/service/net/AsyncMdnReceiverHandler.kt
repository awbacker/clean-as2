package org.cleanas2.service.net

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.*
import org.apache.http.entity.StringEntity
import org.apache.http.impl.BHttpConnectionBase
import org.apache.http.protocol.HttpContext
import org.apache.http.protocol.HttpRequestHandler
import org.cleanas2.message.IncomingMessage
import org.cleanas2.service.CertificateService
import org.cleanas2.service.net.processor.AsyncMdnProcessor
import org.cleanas2.service.net.util.*

import javax.inject.Inject
import javax.mail.MessagingException
import javax.mail.internet.MimeBodyPart
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.cert.X509Certificate

import org.apache.commons.lang3.StringUtils.isBlank
import org.boon.Lists.list

/**
 * Handles receiving Async MDNs, silly.
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class AsyncMdnReceiverHandler @Inject
constructor(private val certs: CertificateService) : HttpRequestHandler {

    @Throws(HttpException::class)
    private fun validateRequestFormat(request: HttpRequest) {

        if (!NetUtil.isPost(request)) {
            logger.error("request was not POST")
            throw ProtocolException(request.requestLine.method + " method not supported")
        }

        for (s in list("AS2-From", "AS2-To")) {
            val h = request.getFirstHeader(s)
            if (h == null || isBlank(h.value)) {
                throw ProtocolException("The required '$s' header was not present")
            }
        }

        if (request !is HttpEntityEnclosingRequest) {
            throw ProtocolException("The request did not contain an entity")
        }

    }

    /**
     * Handles the actual incoming request.  This should set up and deal with connection specific items, such as creating
     * the container class, and handling the errors in a way that the MDN receiver does not see the details of server
     * to server communication.
     */
    @Throws(HttpException::class, IOException::class)
    override fun handle(request: HttpRequest, response: HttpResponse, context: HttpContext) {
        logger.info("handler invoked")

        val connection = context.getAttribute("connection") as BHttpConnectionBase

        val msg = IncomingMessage(connection, request)

        try {
            validateRequestFormat(request)
        } catch (e: Exception) {
            logger.error("Invalid request: " + e.message)
            throw e
        }

        try {
            //E2ELogUtil.mdnAsyncReceiveStart(as2msg);
            var mimeData = MimeUtil.fromHttpRequest(request as HttpEntityEnclosingRequest)

            if (MimeUtil.isSigned(mimeData)) {
                val senderCert = certs.getCertificate(msg.senderId)
                mimeData = MimeUtil.verifyAndRemoveSignature(mimeData, senderCert)
            }

            if (MdnUtil.isBodyPartMdn(mimeData)) {
                val mp = AsyncMdnProcessor(msg, mimeData)
                mp.process(response)
            } else {
                logger.error("Mime body part was not valid or or not an MDN ")
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST)
                response.entity = StringEntity("Mime body part was not recognized as an MDN")
            }
        } catch (e: MessagingException) {
            logger.debug("Messaging Exception", e)
            response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
            response.entity = StringEntity(e.message)
        } catch (e: GeneralSecurityException) {
            response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
            response.entity = StringEntity(e.message)
            logger.debug("Encryption/Security related exception", e)
        } finally {
            logger.debug("MDN Receipt Finished")
            //E2ELogUtil.mdnReceiveEnd(as2msg);
        }
    }

    companion object {

        private val logger = LogFactory.getLog(AsyncMdnReceiverHandler::class.java.simpleName)
    }
}
