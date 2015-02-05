package org.cleanas2.service.net.pipelines.fileReceive;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.cleanas2.server.MessageBus;
import org.cleanas2.bus.SaveIncomingMdnMsg;
import org.cleanas2.bus.SendAsyncMdnMsg;
import org.cleanas2.common.MdnMode;
import org.cleanas2.common.PartnerRecord;
import org.cleanas2.common.disposition.DispositionType;
import org.cleanas2.common.exception.DispositionException;
import org.cleanas2.message.ReplyMdn;
import org.cleanas2.service.CertificateService;
import org.cleanas2.service.PartnerService;
import org.cleanas2.service.net.util.MdnUtil;
import org.cleanas2.service.net.util.MimeUtil;
import org.cleanas2.service.net.util.NetUtil;
import org.cleanas2.pipeline.PipelineTask;
import org.cleanas2.util.CryptoHelper;

import javax.inject.Inject;
import javax.mail.internet.MimeBodyPart;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Sends an MDN in response to a file being received
 */
public class SendMdn implements PipelineTask<Context> {
    private static final Log logger = LogFactory.getLog(SendMdn.class.getSimpleName());

    private final PartnerService partners;
    private final CertificateService certs;

    @Inject
    public SendMdn(CertificateService certs, PartnerService partners) {
        this.partners = partners;
        this.certs = certs;
    }

    @Override
    public void process(Context ctx) throws Exception {
        try {
            logger.info("Sending MDN (mode = " + ctx.fileMessage.mdnMode + ")");

            // if no MDN was requested, set status to OK and exit immediately
            if (ctx.fileMessage.mdnMode == MdnMode.NONE) {
                ctx.httpResponse.setStatusCode(HttpStatus.SC_OK);
                ctx.httpResponse.setEntity(new StringEntity(format("File Received Ok, NO MDN was requested")));
                return;
            }

            // incoming message, so the sender is our partner.
            PartnerRecord partner = this.partners.getPartner(ctx.fileMessage.senderId);
            ReplyMdn mdn = MdnUtil.createReplyMdn(
                    ctx.fileMessage,
                    DispositionType.success(),
                    partner.email
            );

            if (!isBlank(mdn.signedReceiptMicAlgorithm)) {
                try {
                    mdn.attributes.receivedContentMic = CryptoHelper.calculateMIC(ctx.mimeData, mdn.signedReceiptMicAlgorithm);
                    //TODO: Validate MIC here, or in some other place ?
                } catch (Exception e) {
                    throw DispositionException.error(DispositionType.ERR_UNEXPECTED, "Unable to calculate the MIC for your message", e);
                }
            }

            switch (ctx.fileMessage.mdnMode) {
                case STANDARD:
                    MessageBus.publishAsync(new SaveIncomingMdnMsg(mdn));
                    sendMdnOnSameConnection(ctx.httpResponse, mdn);
                    break;
                case ASYNC:
                    //E2ELogUtil.receiveFileInfo(msg, "Sender requested an ASYNC MDN, posting event bus");
                    MessageBus.publishAsync(new SendAsyncMdnMsg(mdn));
                    break;
            }
        } finally {
            logger.info("Done sending MDN");
        }
    }

    private void sendMdnOnSameConnection(HttpResponse response, ReplyMdn mdn) {
        try {
            //if asyncMDN requested, close connection and initiate separate MDN send
            //E2ELogUtil.mdnSendStart(mdn);

            // this code is the same as the code in the AsyncMdnSenderService.  Should it be merged somehow?
            MimeBodyPart outBody = MdnUtil.createMdnMimeData(mdn);
            // Sign the data if needed.  this will make yet another multi-part in that case
            if (mdn.isSignedReply) {
                try {
                    X509Certificate certificate = certs.getCertificate(mdn.companyId);
                    PrivateKey privateKey = certs.getPrivateKey(mdn.companyId);
                    outBody = MimeUtil.signBodyPart(outBody, certificate, privateKey, mdn.signedReceiptMicAlgorithm);
                } catch (Exception e) {
                    // this also cached any GeneralCertificateExceptions raised by not finding
                    // the certificate/key for the company
                    logger.error("Error signing MDN for received file, sending unsigned", e);
                }
            }

            response.setStatusCode(HttpStatus.SC_OK);
            response.setHeaders(NetUtil.mapToHttpHeaders(mdn.responseHeaders));

            response.setHeader("Content-Type", outBody.getContentType());
            response.setEntity(new InputStreamEntity(outBody.getInputStream()));

        } catch (Exception e) {
            logger.error("mdn send error", e);
            //E2ELogUtil.mdnSendError(mdn, e, "General error occurred sending MDN");
        } finally {
            logger.debug("mdn send end");
            //E2ELogUtil.mdnSendEnd(mdn);
        }
    }


}
