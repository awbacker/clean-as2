package org.cleanas2.pipeline

import org.cleanas2.server.ServerEvents
import org.cleanas2.common.serverEvent.Phase

abstract class ServerEventEmitter<TContext>(protected val phase: Phase, protected val context: TContext) {

    abstract fun GetMessageId(): String

    fun Info(msg: String): PipelineTask<TContext> {
        return object : PipelineTask<TContext> {
            override fun process(ctx: TContext) {
                ServerEvents.Info(phase, GetMessageId(), msg)
            }
        }
    }

    fun Error(msg: String): FailureTask<TContext> {
        return object : FailureTask<TContext> {
            override fun process(ctx: TContext, e: Exception?) {
                ServerEvents.Error(phase, GetMessageId(), msg, e)
            }
        }
    }
}
