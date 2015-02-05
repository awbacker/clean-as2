package org.cleanas2.common.service;

/**
 * Any service that may need an "initialization" step after construction
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public interface ConfigurableService {
    void initialize() throws Exception;
}
