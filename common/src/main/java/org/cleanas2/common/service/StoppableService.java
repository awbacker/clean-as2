package org.cleanas2.common.service;

/**
 * Any service that needs to be STOPPED before the application exits.  For example, any
 * file receiver, directory poller, or other threaded task should implement this.
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public interface StoppableService {
    public void stop();
}
