package org.cleanas2.service.net.http

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.protocol.*
import org.cleanas2.common.service.AdminDump
import org.cleanas2.common.service.ConfigurableService
import org.cleanas2.common.service.StoppableService
import org.cleanas2.util.Constants

import java.io.IOException

/**
 * Base class for OpenAS2 HTTP Modules.  Classes that listen for incoming connections should
 * inherit from this class.
 *
 *
 * The class controls the creation of a HTTP server that listens on the specified port.  All
 * incoming connections are passed to the custom handler, and all protocol details are handled
 * by the server.  The handlers only need to read from / write to the request when appropriate.
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
abstract class HttpReceiverServiceBase : AdminDump, ConfigurableService, StoppableService {
    private var serviceManager: HttpServiceManagerThread? = null

    /**
     * Provides a custom HTTP handler that is implemented by the deriving class.
     */
    protected abstract val httpRequestHandler: HttpRequestHandler

    protected abstract val port: Int

    @Throws(Exception::class)
    override fun initialize() {
        try {

            val httpProc = HttpProcessorBuilder.create()
                    .add(ResponseDate())
                    .add(ResponseServer(Constants.AS2_SERVER_SENDER_NAME))
                    .add(ResponseContent())
                    .add(ResponseConnControl()).build()

            val mappings = UriHttpRequestHandlerMapper()

            // AS2 does not support path mappings, so just accept everything and ignore the path/querystring/etc
            // At some later date we may be able to add support for mapping such as
            //   http://localhost:8100/incoming - post a file
            //   http://localhost:8100/asyncmdn - post an async MDN to us
            mappings.register("*", httpRequestHandler)

            // the thread that manages this HttpService will create the socket, etc
            val httpService = HttpService(httpProc, mappings)

            logger.info(String.format("Starting %s on %d", this.javaClass.simpleName, port))

            serviceManager = HttpServiceManagerThread(this, port, httpService, null)
            serviceManager!!.isDaemon = true
            serviceManager!!.start()

        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun stop() {
        serviceManager!!.interrupt()
    }

    override fun dumpCurrentStatus(): List<String> {
        return this.serviceManager!!.dumpCurrentStatus()
    }

    companion object {

        private val logger = LogFactory.getLog(HttpReceiverServiceBase::class.java.simpleName)
    }

}
