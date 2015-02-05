package org.cleanas2.pipeline;

/**
 * Base pipeline context class that lets us set the
 */
public class PipelineContext {
    private boolean terminated = false;

    public void terminateProcessing() {
        this.terminated = true;
    }

    public boolean isTerminated() { return this.terminated; }
}
