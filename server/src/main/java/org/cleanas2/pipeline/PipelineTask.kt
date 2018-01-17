package org.cleanas2.pipeline

interface PipelineTask<TContext> {
    @Throws(Exception::class)
    fun process(ctx: TContext)
}
