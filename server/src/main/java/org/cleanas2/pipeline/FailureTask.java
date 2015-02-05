package org.cleanas2.pipeline;

public interface FailureTask<TContext> {
    public void process(TContext ctx, Exception e);
}
