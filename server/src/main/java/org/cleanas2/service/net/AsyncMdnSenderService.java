package org.cleanas2.service.net;

import net.engio.mbassy.listener.Handler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.cleanas2.bus.SendAsyncMdnMsg;
import org.cleanas2.common.exception.AS2Exception;
import org.cleanas2.message.ReplyMdn;
import org.cleanas2.service.CertificateService;
import org.cleanas2.service.net.util.MdnUtil;
import org.cleanas2.service.net.util.MimeUtil;
import org.cleanas2.service.net.util.NetUtil;
import org.cleanas2.util.DebugUtil;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import java.io.IOException;

import static org.boon.Maps.map;

/**
 * Sends an MDN reply.  The MDN information is created by the receiver, this class
 * simply handles the sending, signing, and MDN specific parts.  For example, the headers
 * to send are calculated by the receiver.  This way we avoid carrying around a LOT of extra state
 * inside the original "message", and decouple sending an MDN from the receipt of the file.
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class AsyncMdnSenderService {

    private static final Log logger = LogFactory.getLog(AsyncMdnSenderService.class.getSimpleName());
    private final CertificateService certs;

    @Inject
    public AsyncMdnSenderService(CertificateService certs) {
        this.certs = certs;
    }

    @Handler
    public void sendAsyncMdn(SendAsyncMdnMsg busMessage) {
        try {
            logger.info("Starting to send Async MDN");
            // E2ELogUtil.mdnAsyncSendStart(as2message);
            send(busMessage.mdn);
            logger.debug("Async MDN - Sent Successfully");
        } catch (Exception e) {
            logger.info("Error sending async MDN", e);
            // E2ELogUtil.mdnAsyncSendError(as2message, ex, "Error sending ASYNC mdn");
        } finally {
            logger.debug("Async MDN Sent");
            // E2ELogUtil.mdnAsyncSendEnd(as2message);
        }
    }

    private void send(final ReplyMdn mdn) throws AS2Exception, IOException, MessagingException {

        try (CloseableHttpClient client = HttpClients.createMinimal()) {
            // this code is the same as the code in the FileReceiverHandler.  Should it be merged somehow?

            MimeBodyPart outBody = MdnUtil.createMdnMimeData(mdn);
            if (mdn.isSignedReply) {
                try {
                    outBody = MimeUtil.signBodyPart(
                            outBody,
                            certs.getCertificate(mdn.companyId),
                            certs.getPrivateKey(mdn.companyId),
                            mdn.signedReceiptMicAlgorithm
                    );
                } catch (Exception e) {
                    // this also cached any GeneralCertificateExceptions raised by not finding
                    // the certificate/key for the company
                    logger.error("Error signing MDN for received file, sending unsigned", e);
                    // todo? retry? send other MDN? not clear
                }
            }

            // PREPARE THE POST AND DEBUG THE STATUS     -------------------------------------------------
            final HttpPost post = new HttpPost(mdn.asyncReplyToUrl);
            post.setHeaders(NetUtil.mapToHttpHeaders(mdn.responseHeaders));
            post.setHeader("Content-Type", outBody.getContentType());
            post.setEntity(new ByteArrayEntity(IOUtils.toByteArray(outBody.getInputStream())));

            DebugUtil.debugPrintObject(logger, "Async MDN - POST prepared", map(
                    "bytes", post.getEntity().getContentLength(),
                    "url", post.getURI().toString(),
                    "content type", post.getFirstHeader("Content-Type"),
                    "original message id", mdn.attributes.originalMessageId
            ));

            // EXECUTE THE ACTUAL POST -------------------------------------------------------------------
            try {
                client.execute(post, new AsyncMdnResponseHandler(mdn, post));
            } catch (ClientProtocolException ex) {
                // todo: resend if a network error occurs during transmission?
                logger.debug("error sending async mdn");
                //E2ELogUtil.mdnAsyncSendError(mdn, ex, "Http Response Error");
                throw ex;
            }
        }
    }

    /**
     * Handles the response received (or not received) when sending an ASYNC mdn
     */
    private static class AsyncMdnResponseHandler implements ResponseHandler<Void> {
        private final ReplyMdn mdn;
        private final HttpPost post;

        public AsyncMdnResponseHandler(ReplyMdn mdn, HttpPost post) {
            this.mdn = mdn;
            this.post = post;
        }

        @Override
        public Void handleResponse(HttpResponse response) throws IOException {
            StatusLine status = response.getStatusLine();
            logger.debug("Async MDN - Received Reply : " + status);
            if (NetUtil.isInvalidResponseCode(response)) {
                logger.error(String.format("Failed to send async MDN, status code %d (%s) - %s - %s",
                        status.getStatusCode(),
                        status.getReasonPhrase(),
                        status.toString(),
                        mdn.attributes.originalMessageId));
                throw new ClientProtocolException("Unexpected Error: " + post.getURI().toString() + " " + status.getStatusCode() + " " + status.getReasonPhrase());
            }
            // consume the response, if there is one, so the connection will close properly
            EntityUtils.consumeQuietly(response.getEntity());
            return null;
        }
    }

}

