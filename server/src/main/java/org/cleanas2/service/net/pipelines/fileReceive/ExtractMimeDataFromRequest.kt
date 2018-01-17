package org.cleanas2.service.net.pipelines.fileReceive

import org.apache.commons.logging.LogFactory
import org.apache.http.HttpEntityEnclosingRequest
import org.cleanas2.common.disposition.DispositionType
import org.cleanas2.common.disposition.DispositionType.Companion.ERR_DECRYPTION
import org.cleanas2.common.disposition.DispositionType.Companion.ERR_INTEGRITY_CHECK
import org.cleanas2.common.exception.DispositionException
import org.cleanas2.pipeline.PipelineTask
import org.cleanas2.service.CertificateService
import org.cleanas2.service.net.util.MimeUtil
import java.text.MessageFormat
import javax.inject.Inject

/**
 * Reads the incoming request, and extracts the MIME body
 * Optionally decrypts it and verifies the signature, if required.  This could be 3 separate
 * steps, but they are so small it seemed better to group them.
 */
class ExtractMimeDataFromRequest @Inject
constructor(private val certs: CertificateService) : PipelineTask<Context> {

    @Throws(Exception::class)
    override fun process(ctx: Context) {
        var incomingData = MimeUtil.fromHttpRequest(ctx.httpRequest as HttpEntityEnclosingRequest)

        try {
            if (MimeUtil.isEncrypted(incomingData)) {
                logger.debug("decrypting")
                val receiverCert = certs.getCertificate(ctx.message.receiverId)
                val receiverKey = certs.getPrivateKey(ctx.message.receiverId)
                incomingData = MimeUtil.decryptBodyPart(incomingData, receiverCert, receiverKey)
                ctx.wasEncryptedOrSigned = true
            }
        } catch (e: Exception) {
            logger.error("Exception decrypting signature", e)
            throw DispositionException(DispositionType.error(ERR_DECRYPTION), MessageFormat.format(
                    "The message sent to Recipient {0} by {1} was received but an error occurred during decryption",
                    ctx.message.senderId, ctx.message.receiverId
            ))
        }

        try {
            if (MimeUtil.isSigned(incomingData)) {
                logger.debug("verifying signature")
                val senderCert = certs.getCertificate(ctx.message.senderId)
                incomingData = MimeUtil.verifyAndRemoveSignature(incomingData, senderCert)
                ctx.wasEncryptedOrSigned = true
            }
        } catch (e: Exception) {
            logger.error("Exception verifying signature", e)
            throw DispositionException(DispositionType.error(ERR_INTEGRITY_CHECK), MessageFormat.format(
                    "The message sent to Recipient {0} by {1} was received and decrypted, but the sender's certificate could not be verified",
                    ctx.message.senderId, ctx.message.receiverId
            ))
        }

        ctx.mimeData = incomingData
    }

    companion object {
        private val logger = LogFactory.getLog(ExtractMimeDataFromRequest::class.java.simpleName)
    }

}
