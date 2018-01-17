package org.cleanas2.pipeline

interface FailureTask<TContext> {
    fun process(ctx: TContext, e: Exception?)
}
