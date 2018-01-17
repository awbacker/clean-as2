package org.cleanas2.common.service

/**
 * Any service that may need an "initialization" step after construction
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
interface ConfigurableService {
    @Throws(Exception::class)
    fun initialize()
}
