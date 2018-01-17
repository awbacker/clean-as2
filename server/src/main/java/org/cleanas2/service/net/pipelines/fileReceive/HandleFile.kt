package org.cleanas2.service.net.pipelines.fileReceive

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.Header
import org.cleanas2.server.MessageBus
import org.cleanas2.bus.SaveIncomingFileMsg
import org.cleanas2.common.disposition.DispositionType
import org.cleanas2.common.exception.DispositionException
import org.cleanas2.message.IncomingFileMessage
import org.cleanas2.pipeline.PipelineTask

import javax.mail.MessagingException
import javax.mail.internet.ContentDisposition
import java.text.MessageFormat

import org.apache.commons.lang3.StringUtils.isBlank

/**
 * Handles a request IF it is an incoming file request.  Incoming ASYNC MDN requests can be received
 * on the same port, and are handled by a different pipeline task
 */
class HandleFile : PipelineTask<Context> {

    @Throws(Exception::class)
    override fun process(ctx: Context) {
        logger.info("Incoming connection looks like a FILE")

        val fileMsg = IncomingFileMessage(ctx.httpConnection, ctx.httpRequest)
        var fileName = fileMsg.messageId
        var contentDisp = ""

        if (ctx.wasEncryptedOrSigned) {
            logger.debug("File Name - incoming data was encrypted or signed, getting from MIME part")
            try {
                val h = ctx.mimeData!!.getHeader("Content-Disposition")
                contentDisp = if (h == null || h.size == 0) "" else h[0]
            } catch (ex: MessagingException) {
                logger.error("Error getting ContentDisposition header from mime data", ex)
            }

        } else {
            val h = ctx.httpRequest.getFirstHeader("Content-Disposition")
            contentDisp = if (h == null) "" else h.value
        }

        if (isBlank(contentDisp)) {
            logger.debug("Content disposition was blank, using default value")
        } else {
            try {
                fileName = ContentDisposition(contentDisp).getParameter("filename")
            } catch (ex: MessagingException) {
                logger.error("Error parsing Content Disposition : " + ex.message, ex)
            }

        }

        logger.info("Setting incoming file name to: " + fileName)
        fileMsg.fileName = fileName

        val busMsg = SaveIncomingFileMsg(fileMsg, ctx.mimeData!!)
        MessageBus.publish(busMsg)
        if (busMsg.isError) {
            throw DispositionException.error(DispositionType.ERR_UNEXPECTED, MessageFormat.format(
                    "The message from {0} to {1} was decrypted and verified, but an error saving the file.",
                    fileMsg.senderId, fileMsg.receiverId
            ), busMsg.errorCause!!)
        }

        ctx.fileMessage = fileMsg
    }

    companion object {
        private val logger = LogFactory.getLog(HandleFile::class.java.simpleName)
    }

}
