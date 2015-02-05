package org.cleanas2.service.net.pipelines.fileSend;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cleanas2.pipeline.PipelineTask;

import javax.inject.Inject;

/**
 * Closes the connection used for sending.  Must always be run after the pipeline finishes (even if it fails)
 */
public class CleanupAndPostProcess implements PipelineTask<Context> {
    private static final Log logger = LogFactory.getLog(CleanupAndPostProcess.class.getSimpleName());

    @Inject
    public CleanupAndPostProcess() {
    }

    @Override
    public void process(Context ctx) throws Exception {
        if (ctx.client != null) {
            ctx.client.close();
        }
        if (ctx.response != null) {
            ctx.response.close();
        }

    }
}
