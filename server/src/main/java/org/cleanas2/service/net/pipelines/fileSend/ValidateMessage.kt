package org.cleanas2.service.net.pipelines.fileSend

import org.apache.http.util.Args
import org.cleanas2.pipeline.PipelineTask

/**
 * Validates that the outgoing message meets basic requirements
 */
class ValidateMessage : PipelineTask<Context> {
    @Throws(Exception::class)
    override fun process(ctx: Context) {
        Args.notEmpty(ctx.message.contentType!!, "message: Content Type")
        Args.notEmpty(ctx.message.outgoingMic, "message: MIC code")
        Args.notEmpty(ctx.message.senderId, "message: Sender ID")
        Args.notEmpty(ctx.message.receiverId, "message: Receiver ID")
    }
}
