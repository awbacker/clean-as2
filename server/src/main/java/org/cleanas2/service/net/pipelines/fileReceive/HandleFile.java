package org.cleanas2.service.net.pipelines.fileReceive;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.cleanas2.server.MessageBus;
import org.cleanas2.bus.SaveIncomingFileMsg;
import org.cleanas2.common.disposition.DispositionType;
import org.cleanas2.common.exception.DispositionException;
import org.cleanas2.message.IncomingFileMessage;
import org.cleanas2.pipeline.PipelineTask;

import javax.mail.MessagingException;
import javax.mail.internet.ContentDisposition;
import java.text.MessageFormat;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Handles a request IF it is an incoming file request.  Incoming ASYNC MDN requests can be received
 * on the same port, and are handled by a different pipeline task
 */
public class HandleFile implements PipelineTask<Context> {
    private static final Log logger = LogFactory.getLog(HandleFile.class.getSimpleName());

    @Override
    public void process(Context ctx) throws Exception {
        logger.info("Incoming connection looks like a FILE");

        IncomingFileMessage fileMsg = new IncomingFileMessage(ctx.httpConnection, ctx.httpRequest);
        String fileName = fileMsg.messageId;
        String contentDisp = "";

        if (ctx.wasEncryptedOrSigned) {
            logger.debug("File Name - incoming data was encrypted or signed, getting from MIME part");
            try {
                String[] h = ctx.mimeData.getHeader("Content-Disposition");
                contentDisp = (h == null || h.length == 0) ? "" : h[0];
            } catch (MessagingException ex) {
                logger.error("Error getting ContentDisposition header from mime data", ex);
            }
        } else {
            Header h = ctx.httpRequest.getFirstHeader("Content-Disposition");
            contentDisp = h == null ? "" : h.getValue();
        }

        if (isBlank(contentDisp)) {
            logger.debug("Content disposition was blank, using default value");
        } else {
            try {
                fileName = new ContentDisposition(contentDisp).getParameter("filename");
            } catch (MessagingException ex) {
                logger.error("Error parsing Content Disposition : " + ex.getMessage(), ex);
            }
        }

        logger.info("Setting incoming file name to: " + fileName);
        fileMsg.fileName = fileName;

        SaveIncomingFileMsg busMsg = new SaveIncomingFileMsg(fileMsg, ctx.mimeData);
        MessageBus.publish(busMsg);
        if (busMsg.isError()) {
            throw DispositionException.error(DispositionType.ERR_UNEXPECTED, MessageFormat.format(
                    "The message from {0} to {1} was decrypted and verified, but an error saving the file.",
                    fileMsg.senderId, fileMsg.receiverId
            ), busMsg.getErrorCause());
        }

        ctx.fileMessage = fileMsg;
    }

}
