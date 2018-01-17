package org.cleanas2.service.net.pipelines.fileReceive

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.HttpStatus
import org.apache.http.entity.StringEntity
import org.cleanas2.pipeline.FailureTask

import java.io.UnsupportedEncodingException

/**
 * Error task that sends an 500 status when an exception is raised during receive.
 */
class SetResponseError : FailureTask<Context> {
    override fun process(ctx: Context, e: Exception?) {
        ctx.httpResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
        try {
            if (e == null) {
                ctx.httpResponse.entity = StringEntity("An unknown error occurred processing the file")
            } else {
                ctx.httpResponse.entity = StringEntity(e.message)
            }
        } catch (e1: UnsupportedEncodingException) {
            logger.error("Unable to set HTTP response reason text.  500 sent, but blank reason.", e1)
        }

    }

    companion object {
        private val logger = LogFactory.getLog(SetResponseError::class.java.simpleName)
    }
}
