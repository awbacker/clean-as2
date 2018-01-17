package org.cleanas2.service.net.pipelines.fileReceive

import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.impl.DefaultBHttpServerConnection
import org.apache.http.protocol.HttpContext
import org.cleanas2.message.IncomingFileMessage
import org.cleanas2.message.IncomingMessage
import org.cleanas2.pipeline.PipelineContext

import javax.mail.internet.MimeBodyPart

/**
 * Holds the context for a file receive operation.  Fields that are not "final" are meant to be
 * initialized by the stages themselves.  Final fields are initialized by the handler and sent to
 * the pipeline
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class Context(val message: IncomingMessage, val httpRequest: HttpRequest, val httpResponse: HttpResponse, val httpContext: HttpContext, val httpConnection: DefaultBHttpServerConnection) : PipelineContext() {
    var mimeData: MimeBodyPart? = null
    var wasEncryptedOrSigned: Boolean = false
    var fileMessage: IncomingFileMessage? = null
}
