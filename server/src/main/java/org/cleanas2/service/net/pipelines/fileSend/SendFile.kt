package org.cleanas2.service.net.pipelines.fileSend

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.Header
import org.apache.http.client.HttpResponseException
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.InputStreamEntity
import org.apache.http.impl.client.HttpClients
import org.cleanas2.common.MdnMode
import org.cleanas2.common.PartnerRecord
import org.cleanas2.service.CompanyService
import org.cleanas2.service.PartnerService
import org.cleanas2.service.ServerConfiguration
import org.cleanas2.service.net.util.HeaderBuilder
import org.cleanas2.service.net.util.NetUtil
import org.cleanas2.pipeline.PipelineTask
import org.cleanas2.util.Constants
import org.cleanas2.util.DebugUtil
import org.joda.time.DateTime

import javax.inject.Inject

import java.text.MessageFormat.format

/**
 * Actually sends the file out over the connection.
 * - Sets outgoing headers
 * - Sets the HTTP entity body
 * - Validates the returned response code  (must be 200)
 */
class SendFile @Inject
constructor(private val partners: PartnerService, private val company: CompanyService, private val config: ServerConfiguration) : PipelineTask<Context> {

    @Throws(Exception::class)
    override fun process(ctx: Context) {
        val p = this.partners.getPartner(ctx.message.receiverId)

        ctx.client = HttpClients.createDefault()

        val post = HttpPost(p!!.sendSettings.url)
        post.setHeaders(getOutgoingHeaders(ctx, p))
        post.entity = InputStreamEntity(ctx.mimeData!!.inputStream)

        DebugUtil.debugPrintHeaders(logger, "OUTGOING", post.allHeaders)

        ctx.response = ctx.client!!.execute(post)

        DebugUtil.debugPrintHeaders(logger, "Response Headers", ctx.response!!.allHeaders)

        if (NetUtil.isInvalidResponseCode(ctx.response!!)) {
            throw HttpResponseException(ctx.response!!.statusLine.statusCode,
                    "Server responded with an invalid eventLevel code")
        }
    }

    private fun getOutgoingHeaders(ctx: Context, p: PartnerRecord): Array<Header> {
        val b = HeaderBuilder()
        b.add("Connection", "close, TE")
        b.add("User-Agent", Constants.AS2_SERVER_SENDER_NAME)
        b.add("Date", DateTime.now().toString("EEE, dd MMM yyyy HH:mm:ss Z"))
        b.add("Mime-Version", Constants.MIME_VERSION_1_0) // make sure this is the encoding used in the as2Msg, run TBF1
        b.add("Message-ID", ctx.message.loggingText)
        b.add("Recipient-Address", p.sendSettings.url)
        b.add("Content-Type", ctx.message.contentType)
        b.add("AS2-Version", Constants.AS2_PROTOCOL_VERSION)
        b.add("AS2-To", ctx.message.receiverId)
        b.add("AS2-From", ctx.message.senderId)
        b.add("Subject", format("From {0} to {1}", ctx.message.senderId, ctx.message.receiverId))
        b.add("From", company.email)
        when (p.sendSettings.mdnMode) {
            MdnMode.NONE -> {
            }
            MdnMode.STANDARD -> {
                b.add("Disposition-Notification-To", p.email)
                b.add("Disposition-Notification-Options", ctx.message.contentDisposition)
            }
            MdnMode.ASYNC -> {
                b.add("Disposition-Notification-To", p.email)
                b.add("Disposition-Notification-Options", ctx.message.contentDisposition)
                b.add("Receipt-Delivery-Option", config.asyncMdnUrl)
            }
        }
        b.add("Content-Disposition", String.format("Attachment; filename=\"%s\"", ctx.message.filePath.fileName.toString()))
        return b.toArray()
    }

    companion object {
        private val logger = LogFactory.getLog(SendFile::class.java.simpleName)
    }

}
