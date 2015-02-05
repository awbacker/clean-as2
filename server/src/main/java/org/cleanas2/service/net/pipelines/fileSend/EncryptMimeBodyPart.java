package org.cleanas2.service.net.pipelines.fileSend;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cleanas2.common.PartnerRecord;
import org.cleanas2.service.CertificateService;
import org.cleanas2.service.PartnerService;
import org.cleanas2.service.net.FileSenderService;
import org.cleanas2.service.net.util.MimeUtil;
import org.cleanas2.pipeline.PipelineTask;

import javax.inject.Inject;
import javax.mail.internet.MimeBodyPart;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Takes the MIME message created by a previous task, and encrypts/signs it as requested
 * by the partnership agreement.
 */
public class EncryptMimeBodyPart implements PipelineTask<Context> {
    private static final Log logger = LogFactory.getLog(FileSenderService.class.getSimpleName());
    private final PartnerService partners;
    private final CertificateService certs;

    @Inject
    public EncryptMimeBodyPart(CertificateService certs, PartnerService partners) {
        this.partners = partners;
        this.certs = certs;
    }

    @Override
    public void process(Context ctx) throws Exception {
        PartnerRecord p = partners.getPartner(ctx.message.receiverId);

        if (!p.shouldSign() && !p.shouldEncrypt()) return;

        MimeBodyPart part = ctx.mimeData;

        if (p.shouldSign()) {
            X509Certificate senderCert = certs.getCertificate(ctx.message.senderId);
            PrivateKey senderKey = certs.getPrivateKey(ctx.message.senderId);
            part = MimeUtil.signBodyPart(part, senderCert, senderKey, p.sendSettings.signAlgorithm);
            logger.debug("Message signed successfully");
        }

        if (p.shouldEncrypt()) {
            X509Certificate receiverCert = certs.getCertificate(ctx.message.receiverId);
            part = MimeUtil.encryptBodyPart(part, receiverCert, p.sendSettings.encryptAlgorithm);
            logger.debug("Message encrypted successfully");
        }

        ctx.message.contentType = part.getContentType();
        ctx.message.contentDisposition = p.sendSettings.mdnOptions;
        ctx.message.outgoingMic = MimeUtil.calculateMicString(ctx.mimeData, p.sendSettings.mdnOptions);

        ctx.mimeData = part;
    }

}
