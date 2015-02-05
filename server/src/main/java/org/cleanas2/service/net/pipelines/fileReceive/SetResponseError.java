package org.cleanas2.service.net.pipelines.fileReceive;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.cleanas2.pipeline.FailureTask;

import java.io.UnsupportedEncodingException;

/**
 * Error task that sends an 500 status when an exception is raised during receive.
 */
public class SetResponseError implements FailureTask<Context> {
    private static final Log logger = LogFactory.getLog(SetResponseError.class.getSimpleName());

    @Override
    public void process(Context ctx, Exception e) {
        ctx.httpResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        try {
            if (e == null) {
                ctx.httpResponse.setEntity(new StringEntity("An unknown error occurred processing the file"));
            } else {
                ctx.httpResponse.setEntity(new StringEntity(e.getMessage()));
            }
        } catch (UnsupportedEncodingException e1) {
            logger.error("Unable to set HTTP response reason text.  500 sent, but blank reason.", e1);
        }
    }
}
