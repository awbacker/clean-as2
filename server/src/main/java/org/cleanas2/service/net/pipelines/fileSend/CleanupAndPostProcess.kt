package org.cleanas2.service.net.pipelines.fileSend

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.cleanas2.pipeline.PipelineTask

import javax.inject.Inject

/**
 * Closes the connection used for sending.  Must always be run after the pipeline finishes (even if it fails)
 */
class CleanupAndPostProcess @Inject
constructor() : PipelineTask<Context> {

    @Throws(Exception::class)
    override fun process(ctx: Context) {
        if (ctx.client != null) {
            ctx.client!!.close()
        }
        if (ctx.response != null) {
            ctx.response!!.close()
        }

    }

    companion object {
        private val logger = LogFactory.getLog(CleanupAndPostProcess::class.java.simpleName)
    }
}
