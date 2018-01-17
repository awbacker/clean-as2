package org.cleanas2.pipeline

/**
 * Base pipeline context class that lets us set the
 */
open class PipelineContext {
    var isTerminated = false
        private set

    fun terminateProcessing() {
        this.isTerminated = true
    }
}
