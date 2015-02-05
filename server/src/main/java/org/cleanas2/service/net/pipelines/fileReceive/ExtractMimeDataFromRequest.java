package org.cleanas2.service.net.pipelines.fileReceive;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntityEnclosingRequest;
import org.cleanas2.common.disposition.DispositionType;
import org.cleanas2.common.exception.DispositionException;
import org.cleanas2.service.CertificateService;
import org.cleanas2.service.net.util.MimeUtil;
import org.cleanas2.pipeline.PipelineTask;

import javax.inject.Inject;
import javax.mail.internet.MimeBodyPart;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;

import static org.cleanas2.common.disposition.DispositionType.ERR_DECRYPTION;
import static org.cleanas2.common.disposition.DispositionType.ERR_INTEGRITY_CHECK;

/**
 * Reads the incoming request, and extracts the MIME body
 * Optionally decrypts it and verifies the signature, if required.  This could be 3 separate
 * steps, but they are so small it seemed better to group them.
 */
public class ExtractMimeDataFromRequest implements PipelineTask<Context> {
    private static final Log logger = LogFactory.getLog(ExtractMimeDataFromRequest.class.getSimpleName());

    private final CertificateService certs;

    @Inject
    public ExtractMimeDataFromRequest(CertificateService certs) {
        this.certs = certs;
    }

    @Override
    public void process(Context ctx) throws Exception {
        MimeBodyPart incomingData = MimeUtil.fromHttpRequest((HttpEntityEnclosingRequest) ctx.httpRequest);

        try {
            if (MimeUtil.isEncrypted(incomingData)) {
                logger.debug("decrypting");
                X509Certificate receiverCert = certs.getCertificate(ctx.message.receiverId);
                PrivateKey receiverKey = certs.getPrivateKey(ctx.message.receiverId);
                incomingData = MimeUtil.decryptBodyPart(incomingData, receiverCert, receiverKey);
                ctx.wasEncryptedOrSigned = true;
            }
        } catch (Exception e) {
            logger.error("Exception decrypting signature", e);
            throw new DispositionException(DispositionType.error(ERR_DECRYPTION), MessageFormat.format(
                    "The message sent to Recipient {0} by {1} was received but an error occurred during decryption",
                    ctx.message.senderId, ctx.message.receiverId
            ));
        }

        try {
            if (MimeUtil.isSigned(incomingData)) {
                logger.debug("verifying signature");
                X509Certificate senderCert = certs.getCertificate(ctx.message.senderId);
                incomingData = MimeUtil.verifyAndRemoveSignature(incomingData, senderCert);
                ctx.wasEncryptedOrSigned = true;
            }
        } catch (Exception e) {
            logger.error("Exception verifying signature", e);
            throw new DispositionException(DispositionType.error(ERR_INTEGRITY_CHECK), MessageFormat.format(
                    "The message sent to Recipient {0} by {1} was received and decrypted, but the sender's certificate could not be verified",
                    ctx.message.senderId, ctx.message.receiverId
            ));
        }

        ctx.mimeData = incomingData;
    }

}
