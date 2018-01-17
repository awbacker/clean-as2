package org.cleanas2.service.net

import org.apache.http.HttpException
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.impl.DefaultBHttpServerConnection
import org.apache.http.protocol.HttpContext
import org.apache.http.protocol.HttpRequestHandler
import org.cleanas2.common.serverEvent.Phase
import org.cleanas2.message.IncomingMessage
import org.cleanas2.pipeline.Pipeline
import org.cleanas2.pipeline.ServerEventEmitter
import org.cleanas2.service.net.pipelines.fileReceive.*

import java.io.IOException


class PipelineFileReceiverHandler : HttpRequestHandler {

    @Throws(HttpException::class, IOException::class)
    override fun handle(request: HttpRequest, response: HttpResponse, context: HttpContext) {
        try {
            val connection = context.getAttribute("connection") as DefaultBHttpServerConnection
            val msg = IncomingMessage(connection, request)
            RunPipeline(request, response, context, connection, msg)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @Throws(Exception::class)
    private fun RunPipeline(request: HttpRequest, response: HttpResponse, context: HttpContext, connection: DefaultBHttpServerConnection, msg: IncomingMessage) {
        val ctx = Context(msg, request, response, context, connection)
        val evt = FileSendEvent(ctx)
        val p = Pipeline<Context>()
        p.step(evt.Info("Starting file receive"))
        p.step(ExtractMimeDataFromRequest::class.java)
        p.step(HandleAsyncMdn::class.java)
        p.step(HandleFile::class.java)
        p.step(SendMdn::class.java)
        p.done(evt.Info("File receive finished"))
        p.fail(SetResponseError::class.java)
        p.fail(evt.Error("Error sending file"))
        p.run(ctx)
    }

    class FileSendEvent(context: Context) : ServerEventEmitter<Context>(Phase.FILE_RECEIVE, context) {

        override fun GetMessageId(): String {
            return context.message.receiverId
        }
    }
}
