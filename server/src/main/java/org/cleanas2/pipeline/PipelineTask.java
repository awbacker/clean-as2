package org.cleanas2.pipeline;

public interface PipelineTask<TContext> {
    public void process(TContext ctx) throws Exception;
}
