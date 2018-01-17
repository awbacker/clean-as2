package org.cleanas2.service.net.pipelines.fileSend

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.cleanas2.common.PartnerRecord
import org.cleanas2.service.PartnerService
import org.cleanas2.service.net.FileSenderService
import org.cleanas2.service.net.util.MimeUtil
import org.cleanas2.pipeline.PipelineTask

import javax.inject.Inject
import javax.mail.internet.MimeBodyPart

/**
 * Creates the MIME body part to send to the client, reading from the local file.
 */
class CreateMimeBodyPart @Inject
constructor(private val partners: PartnerService) : PipelineTask<Context> {

    @Throws(Exception::class)
    override fun process(ctx: Context) {
        val p = partners.getPartner(ctx.message.receiverId)
        val filename = ctx.message.filePath.fileName.toString()

        // keep this here (not in a function) so it is clear what is going on with transfer encoding & content type
        val data = MimeUtil.fromFile(ctx.message.filePath, p!!.sendSettings.contentType)
        data.setHeader("Content-Transfer-Encoding", p.sendSettings.transferEncoding) // this must be binary or everything screws up
        data.setHeader("Content-Type", p.sendSettings.contentType)
        // set this here ? also in outgoing headers?
        data.setHeader("Content-Disposition", String.format("Attachment; filename=\"%s\"", filename))

        ctx.mimeData = data
    }

    companion object {
        private val logger = LogFactory.getLog(FileSenderService::class.java.simpleName)
    }
}
