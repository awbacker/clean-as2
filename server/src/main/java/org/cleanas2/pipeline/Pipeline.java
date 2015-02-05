package org.cleanas2.pipeline;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cleanas2.server.ServerSession;

import java.util.ArrayList;
import java.util.List;

public class Pipeline<TContext extends PipelineContext> {
    private static final Log logger = LogFactory.getLog(Pipeline.class.getSimpleName());

    private final List<PipelineTask<TContext>> stages = new ArrayList<>(10);
    private final List<PipelineTask<TContext>> onDone = new ArrayList<>(2);
    private final List<FailureTask<TContext>> onFailure = new ArrayList<>(2);

    public void step(PipelineTask<TContext> inst) {
        stages.add(inst);
    }

    public void step(Class<? extends PipelineTask<TContext>> klass) {
        step(ServerSession.getSession().getInstance(klass));
    }

    @SafeVarargs
    public final void step(Class<? extends PipelineTask<TContext>>... klasses) {
        for (Class<? extends PipelineTask<TContext>> c : klasses) {
            step(ServerSession.getSession().getInstance(c));
        }
    }

    public void done(PipelineTask<TContext> inst) {
        onDone.add(inst);
    }

    public void done(Class<? extends PipelineTask<TContext>> klass) {
        done(ServerSession.getSession().getInstance(klass));
    }

    public void fail(FailureTask<TContext> inst) {
        onFailure.add(inst);
    }

    public void fail(Class<? extends FailureTask<TContext>> klass) {
        fail(ServerSession.getSession().getInstance(klass));
    }

    public void run(TContext context) throws Exception {

        try {
            for (PipelineTask<TContext> h : stages) {
                logStage("Running stage", h);
                h.process(context);
                if (context.isTerminated()) {
                    break;
                }
            }
        } catch (Exception e) {
            for (FailureTask<TContext> h : onFailure) {
                logStage("Running failure handler", h);
                h.process(context, e);
            }
        }

        for (PipelineTask<TContext> h : onDone) {
            try {
                logStage("Running done handler", h);
                h.process(context);
            } catch (Exception e) {
                logger.error("Failure processing 'On Complete' action", e);
            }
        }
    }

    private static void logStage(String message, Object obj) {
        Class c = obj.getClass();
        String stage = c.isAnonymousClass() ? "(anon class)" : c.getSimpleName();
        logger.debug(message + ": " + stage);
    }

}
