package org.cleanas2.service.net;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.BHttpConnectionBase;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.cleanas2.message.IncomingMessage;
import org.cleanas2.service.CertificateService;
import org.cleanas2.service.net.processor.AsyncMdnProcessor;
import org.cleanas2.service.net.util.*;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.boon.Lists.list;

/**
 * Handles receiving Async MDNs, silly.
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class AsyncMdnReceiverHandler implements HttpRequestHandler {

    private static final Log logger = LogFactory.getLog(AsyncMdnReceiverHandler.class.getSimpleName());
    private final CertificateService certs;

    @Inject
    public AsyncMdnReceiverHandler(CertificateService certs) {
        this.certs = certs;
    }

    private void validateRequestFormat(HttpRequest request) throws HttpException {

        if (!NetUtil.isPost(request)) {
            logger.error("request was not POST");
            throw new ProtocolException(request.getRequestLine().getMethod() + " method not supported");
        }

        for (String s : list("AS2-From", "AS2-To")) {
            Header h = request.getFirstHeader(s);
            if (h == null || isBlank(h.getValue())) {
                throw new ProtocolException("The required '" + s + "' header was not present");
            }
        }

        if (!(request instanceof HttpEntityEnclosingRequest)) {
            throw new ProtocolException("The request did not contain an entity");
        }

    }

    /**
     * Handles the actual incoming request.  This should set up and deal with connection specific items, such as creating
     * the container class, and handling the errors in a way that the MDN receiver does not see the details of server
     * to server communication.
     */
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        logger.info("handler invoked");

        BHttpConnectionBase connection = (BHttpConnectionBase) context.getAttribute("connection");

        IncomingMessage msg = new IncomingMessage(connection, request);

        try {
            validateRequestFormat(request);
        } catch (Exception e) {
            logger.error("Invalid request: " + e.getMessage());
            throw e;
        }

        try {
            //E2ELogUtil.mdnAsyncReceiveStart(as2msg);
            MimeBodyPart mimeData = MimeUtil.fromHttpRequest((HttpEntityEnclosingRequest) request);

            if (MimeUtil.isSigned(mimeData)) {
                X509Certificate senderCert = certs.getCertificate(msg.senderId);
                mimeData = MimeUtil.verifyAndRemoveSignature(mimeData, senderCert);
            }

            if (MdnUtil.isBodyPartMdn(mimeData)) {
                AsyncMdnProcessor mp = new AsyncMdnProcessor(msg, mimeData);
                mp.process(response);
            } else {
                logger.error("Mime body part was not valid or or not an MDN ");
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                response.setEntity(new StringEntity("Mime body part was not recognized as an MDN"));
            }
        } catch (MessagingException e) {
            logger.debug("Messaging Exception", e);
            response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.setEntity(new StringEntity(e.getMessage()));
        } catch (GeneralSecurityException e) {
            response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.setEntity(new StringEntity(e.getMessage()));
            logger.debug("Encryption/Security related exception", e);
        } finally {
            logger.debug("MDN Receipt Finished");
            //E2ELogUtil.mdnReceiveEnd(as2msg);
        }
    }
}
