package org.cleanas2.cmd;

import org.cleanas2.common.service.ConfigurableService;
import org.cleanas2.common.service.StoppableService;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
@Singleton
public class CommandLineService implements ConfigurableService, StoppableService {

    private final CommandLineHandler cl;

    @Inject
    public CommandLineService(CommandLineHandler cl) {
        this.cl = cl;
    }

    @Override
    public void initialize() throws Exception {
        cl.setDaemon(true);
        cl.run();
    }

    @Override
    public void stop() {
        cl.interrupt();
    }
}
