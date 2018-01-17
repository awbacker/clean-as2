package org.cleanas2.service.net.pipelines.fileReceive

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.MethodNotSupportedException
import org.apache.http.ProtocolException
import org.cleanas2.pipeline.PipelineTask

/**
 * Validates that the incoming request meets basic requirements
 */
class ValidateRequest : PipelineTask<Context> {

    @Throws(Exception::class)
    override fun process(ctx: Context) {
        // validate the basics of this request... headers, request type, content is present, and so on
        if (!"POST".equals(ctx.message.connectionInfo.requestMethod, ignoreCase = true)) {
            throw MethodNotSupportedException(ctx.httpRequest.requestLine.method + " method not supported")
        }

        if (ctx.httpRequest !is HttpEntityEnclosingRequest) {
            throw ProtocolException("Request did not contain a recognizable HTTP Entity")
        }
    }

    companion object {
        private val logger = LogFactory.getLog(ValidateRequest::class.java.simpleName)
    }
}
