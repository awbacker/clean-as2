package org.cleanas2.server

import com.google.inject.Injector
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.cleanas2.common.service.StoppableService

import java.util.ArrayList

import org.cleanas2.util.AS2Util.ofType

/**
 * Default context for the running server.  Event bus and others may hold weak references, so any
 * long lived services must be stored here or they will be disposed by the garbage collector.  This
 * also allows services access to other services that they may need (generally just partner/company/config)
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class ServerSession private constructor(private val injector: Injector) {
    private val allServices = ArrayList<Any>()

    fun getAllServices(): List<Any> {
        return allServices
    }

    fun <T> getInstance(klass: Class<T>): T {
        return session!!.injector.getInstance(klass)
    }

    fun <T> startService(klass: Class<T>) {
        val svc = injector.getInstance(klass)!!
        allServices.add(svc)
        MessageBus.subscribe(svc)
    }

    fun shutdown() {
        stopAllServices()
        allServices.clear()
        ServerSession.session = null
    }

    fun stopAllServices() {
        logger.debug("Shutting down running services")
        for (svc in ofType(allServices, StoppableService::class.java)) {
            logger.debug("   " + svc.javaClass.simpleName)
            svc.stop()
        }
    }

    companion object {

        private val logger = LogFactory.getLog(ServerSession::class.java.simpleName)
        var session: ServerSession? = null
            private set

        fun initialize(i: Injector) {
            if (session == null) {
                session = ServerSession(i)
            }
        }
    }

}
