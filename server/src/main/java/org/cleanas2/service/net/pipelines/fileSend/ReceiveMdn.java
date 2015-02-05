package org.cleanas2.service.net.pipelines.fileSend;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.cleanas2.server.MessageBus;
import org.cleanas2.server.ServerEvents;
import org.cleanas2.bus.SaveMdnMsg;
import org.cleanas2.bus.SavePendingMdnMsg;
import org.cleanas2.common.PartnerRecord;
import org.cleanas2.common.exception.AS2Exception;
import org.cleanas2.common.exception.DispositionException;
import org.cleanas2.message.IncomingSyncMdn;
import org.cleanas2.message.OutgoingFileMessage;
import org.cleanas2.service.CertificateService;
import org.cleanas2.service.PartnerService;
import org.cleanas2.service.net.util.MdnUtil;
import org.cleanas2.service.net.util.MimeUtil;
import org.cleanas2.pipeline.PipelineTask;
import org.cleanas2.util.DebugUtil;

import javax.inject.Inject;
import javax.mail.internet.MimeBodyPart;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

/**
 * Receives the MDN for the file that was just sent.
 * - if SYNC, then waits on the current connection
 * - if ASYNC, then posts the message to save the verification data, and exits
 */
public class ReceiveMdn implements PipelineTask<Context> {
    private static final Log logger = LogFactory.getLog(ReceiveMdn.class.getSimpleName());

    private final PartnerService partners;
    private final CertificateService certs;

    @Inject
    public ReceiveMdn(CertificateService certs, PartnerService partners) {
        this.partners = partners;
        this.certs = certs;
    }

    @Override
    public void process(Context ctx) throws Exception {
        PartnerRecord p = this.partners.getPartner(ctx.message.receiverId);

        switch (p.sendSettings.mdnMode) {
            case ASYNC:
                MessageBus.publishAsync(new SavePendingMdnMsg(ctx.message));
                break;
            case STANDARD:
                receiveMdn(ctx.message, ctx.response);
                break;
            default:
                logger.debug("No MDN was requested (" + p.sendSettings.mdnMode + ")");
        }
    }

    private void receiveMdn(OutgoingFileMessage message, HttpResponse response) throws IOException, AS2Exception, DispositionException, GeneralSecurityException {

        IncomingSyncMdn mdn = new IncomingSyncMdn(message, response);

        ServerEvents.mdnReceiveStart(message);

        if (response.getEntity() == null) {
            ServerEvents.mdnReceiveError(message, null, "Expected an MDN reply (non-async), but body was empty");
            ServerEvents.mdnReceiveEnd(message);
            throw new HttpResponseException(response.getStatusLine().getStatusCode(), "Expected an MDN reply (non-async), but body was empty");
        }

        try {
            MimeBodyPart mimeData = MimeUtil.fromHttpResponse(response);

            // if the mdn is signed, first verify the signature and strip it, before using that to populate
            if (MimeUtil.isSigned(mimeData)) {
                X509Certificate senderCert = certs.getCertificate(mdn.responseHeaders.get("AS2-From"));
                mimeData = MimeUtil.verifyAndRemoveSignature(mimeData, senderCert);
            }

            MdnUtil.populateMdnFromMimeBodyPart(mdn, mimeData);
            DebugUtil.debugPrintObject(logger, "mdn attributes ", mdn.attributes);

            MessageBus.publish(new SaveMdnMsg(mdn));

            // this will check for errors that the other server is reporting, and throw a DispositionException
            MdnUtil.validateMdnDisposition(message, mdn);

            MdnUtil.validateReturnedMic(mdn.attributes.receivedContentMic, message.outgoingMic);

        } catch (GeneralSecurityException e) {
            throw e;
        } catch (DispositionException ex) {
            // a disposition exception means that the server had an error decrypting/verifying signature/etc.  this
            // means the file needs to go into the error directory, so pass this exception all the way up. eventually the
            // directory polling module will handle it
            ServerEvents.mdnReceiveError(message, ex, "Remote server reported an error receiving the message: " + ex.getDisposition().dispositionDescription);
            throw ex;
        } catch (IOException ex) {
            // IO exception means an error of some kind with the connection.  In this case we should pass it up and let the caller handle
            ServerEvents.mdnReceiveError(message, ex, "IO error occurred receiving the MDN");
            throw ex;
        } catch (Exception ex) {
            ServerEvents.mdnReceiveError(message, ex, "Generic exception receiving MDN: " + ex.getLocalizedMessage());
            throw new AS2Exception("Generic exception processing the MDN", ex);
        } finally {
            ServerEvents.mdnReceiveEnd(message);
        }
    }
}
