package org.cleanas2.service.net.pipelines.fileReceive;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.ProtocolException;
import org.cleanas2.pipeline.PipelineTask;

/**
 * Validates that the incoming request meets basic requirements
 */
public class ValidateRequest implements PipelineTask<Context> {
    private static final Log logger = LogFactory.getLog(ValidateRequest.class.getSimpleName());

    @Override
    public void process(Context ctx) throws Exception {
        // validate the basics of this request... headers, request type, content is present, and so on
        if (!"POST".equalsIgnoreCase(ctx.message.connectionInfo.requestMethod)) {
            throw new MethodNotSupportedException(ctx.httpRequest.getRequestLine().getMethod() + " method not supported");
        }

        if (!(ctx.httpRequest instanceof HttpEntityEnclosingRequest)) {
            throw new ProtocolException("Request did not contain a recognizable HTTP Entity");
        }
    }
}
