package org.cleanas2.pipeline;

import org.cleanas2.server.ServerEvents;
import org.cleanas2.common.serverEvent.Phase;

public abstract class ServerEventEmitter<TContext> {
    protected final Phase phase;
    protected final TContext context;

    public abstract String GetMessageId();

    public ServerEventEmitter(Phase phase, TContext context) {
        this.phase = phase;
        this.context = context;
    }

    public PipelineTask<TContext> Info(final String msg) {
        return new PipelineTask<TContext>() {
            @Override
            public void process(TContext ctx) throws Exception {
                ServerEvents.Info(phase, GetMessageId(), msg);
            }
        };
    }

    public FailureTask<TContext> Error(final String msg) {
        return new FailureTask<TContext>() {
            @Override
            public void process(TContext ctx, Exception e) {
                ServerEvents.Error(phase, GetMessageId(), msg, e);
            }
        };
    }
}
