package org.cleanas2.cmd

import org.cleanas2.common.service.ConfigurableService
import org.cleanas2.common.service.StoppableService

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
@Singleton
class CommandLineService @Inject
constructor(private val cl: CommandLineHandler) : ConfigurableService, StoppableService {

    @Throws(Exception::class)
    override fun initialize() {
        cl.isDaemon = true
        cl.run()
    }

    override fun stop() {
        cl.interrupt()
    }
}
