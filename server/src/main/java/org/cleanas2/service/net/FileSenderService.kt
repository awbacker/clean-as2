package org.cleanas2.service.net

import net.engio.mbassy.listener.Handler
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.cleanas2.bus.SendFileMsg
import org.cleanas2.common.serverEvent.Phase
import org.cleanas2.pipeline.FailureTask
import org.cleanas2.pipeline.ServerEventEmitter
import org.cleanas2.service.net.pipelines.fileSend.*
import org.cleanas2.pipeline.Pipeline
import org.cleanas2.pipeline.PipelineContext

import javax.inject.Inject

class FileSenderService @Inject
constructor() {

    @Handler
    @Throws(Exception::class)
    fun handleSendFile(busMessage: SendFileMsg) {
        val context = Context(busMessage.as2message)
        val fileSendEvent = FileSendEmitter(context)

        val p = Pipeline<Context>()
        p.step(fileSendEvent.Info("Starting to send file"))
        p.step(
                CreateMimeBodyPart::class.java,
                EncryptMimeBodyPart::class.java,
                ValidateMessage::class.java,
                SendFile::class.java,
                ReceiveMdn::class.java
        )
        p.done(CleanupAndPostProcess::class.java)
        p.done(fileSendEvent.Info("File Send Finished"))
        p.fail(fileSendEvent.Error("Error Sending File"))
        p.fail(object: FailureTask<Context> {
            override fun process(ctx: Context, e: Exception?) {
                busMessage.isError = true
                busMessage.errorCause = e
            }
        })
        p.run(context)
    }

    private inner class FileSendEmitter(context: Context) : ServerEventEmitter<Context>(Phase.FILE_SEND, context) {

        override fun GetMessageId(): String {
            return this.context.message.loggingText
        }
    }

    companion object {

        private val logger = LogFactory.getLog(FileSenderService::class.java.simpleName)
    }
}
