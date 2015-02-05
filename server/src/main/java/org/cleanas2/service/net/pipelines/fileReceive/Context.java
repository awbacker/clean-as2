package org.cleanas2.service.net.pipelines.fileReceive;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.protocol.HttpContext;
import org.cleanas2.message.IncomingFileMessage;
import org.cleanas2.message.IncomingMessage;
import org.cleanas2.pipeline.PipelineContext;

import javax.mail.internet.MimeBodyPart;

/**
 * Holds the context for a file receive operation.  Fields that are not "final" are meant to be
 * initialized by the stages themselves.  Final fields are initialized by the handler and sent to
 * the pipeline
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class Context extends PipelineContext {
    public final IncomingMessage message;
    public final HttpResponse httpResponse;
    public final HttpRequest httpRequest;
    public final HttpContext httpContext;
    public final DefaultBHttpServerConnection httpConnection;
    public MimeBodyPart mimeData;
    public boolean wasEncryptedOrSigned;
    public IncomingFileMessage fileMessage;

    public Context(IncomingMessage message, HttpRequest request, HttpResponse response, HttpContext context, DefaultBHttpServerConnection connection) {
        this.message = message;
        this.httpContext = context;
        this.httpRequest = request;
        this.httpResponse = response;
        this.httpConnection = connection;
    }
}
