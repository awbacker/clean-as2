package org.cleanas2.service.net.pipelines.fileReceive;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cleanas2.service.net.processor.AsyncMdnProcessor;
import org.cleanas2.service.net.util.MdnUtil;
import org.cleanas2.pipeline.PipelineTask;

/**
 * Handles receiving an ASYNC mdn rather than a file.  Handling a file is done in a separate task.
 *
 * After an MDN is received, the pipeline is terminated.  Unfortunately, it is not possible to tell
 * if the incoming request is MDN or FILE until the pipeline decrypts the body.
 *
 * For example... Cyclone ALWAYS ignores the option that tells it where to send the MDN, and always
 * sends it to the URL declared for receiving files (on the cyclone end).  This means that the File Receive
 * path MUST support receiving ASYNC MDN.  Thankfully this is a fairly simple process.
 */
public class HandleAsyncMdn implements PipelineTask<Context> {
    private static final Log logger = LogFactory.getLog(HandleAsyncMdn.class.getSimpleName());

    @Override
    public void process(Context ctx) throws Exception {
        if (MdnUtil.isBodyPartMdn(ctx.mimeData)) {
            logger.info("Incoming connection looks like an ASYNC MDN");
            AsyncMdnProcessor mp = new AsyncMdnProcessor(ctx.message, ctx.mimeData);
            mp.process(ctx.httpResponse);
            ctx.terminateProcessing(); // exit pipeline processing
        }
    }
}
