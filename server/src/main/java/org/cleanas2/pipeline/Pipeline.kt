package org.cleanas2.pipeline

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.cleanas2.server.ServerSession

import java.util.ArrayList

class Pipeline<TContext : PipelineContext> {

    private val stages = ArrayList<PipelineTask<TContext>>(10)
    private val onDone = ArrayList<PipelineTask<TContext>>(2)
    private val onFailure = ArrayList<FailureTask<TContext>>(2)

    fun step(inst: PipelineTask<TContext>) {
        stages.add(inst)
    }

    fun step(klass: Class<out PipelineTask<TContext>>) {
        step(ServerSession.session!!.getInstance(klass))
    }

    @SafeVarargs
    fun step(vararg klasses: Class<out PipelineTask<TContext>>) {
        for (c in klasses) {
            step(ServerSession.session!!.getInstance(c))
        }
    }

    fun done(inst: PipelineTask<TContext>) {
        onDone.add(inst)
    }

    fun done(klass: Class<out PipelineTask<TContext>>) {
        done(ServerSession.session!!.getInstance(klass))
    }

    fun fail(inst: FailureTask<TContext>) {
        onFailure.add(inst)
    }

    fun fail(klass: Class<out FailureTask<TContext>>) {
        fail(ServerSession.session!!.getInstance(klass))
    }

    @Throws(Exception::class)
    fun run(context: TContext) {

        try {
            for (h in stages) {
                logStage("Running stage", h)
                h.process(context)
                if (context.isTerminated) {
                    break
                }
            }
        } catch (e: Exception) {
            for (h in onFailure) {
                logStage("Running failure handler", h)
                h.process(context, e)
            }
        }

        for (h in onDone) {
            try {
                logStage("Running done handler", h)
                h.process(context)
            } catch (e: Exception) {
                logger.error("Failure processing 'On Complete' action", e)
            }

        }
    }

    companion object {
        private val logger = LogFactory.getLog(Pipeline::class.java.simpleName)

        private fun logStage(message: String, obj: Any) {
            val c = obj.javaClass
            val stage = if (c.isAnonymousClass) "(anon class)" else c.simpleName
            logger.debug(message + ": " + stage)
        }
    }

}
