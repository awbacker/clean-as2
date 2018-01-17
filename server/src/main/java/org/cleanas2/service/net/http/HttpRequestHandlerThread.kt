package org.cleanas2.service.net.http

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.ConnectionClosedException
import org.apache.http.HttpException
import org.apache.http.HttpServerConnection
import org.apache.http.impl.DefaultBHttpServerConnection
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.protocol.HttpContext
import org.apache.http.protocol.HttpService
import org.cleanas2.service.net.util.NetUtil

import java.io.IOException

/**
 * A thread that is spawned each time an request comes in.  It's main job is to set up the
 * context for the handler, then pass that to the server to do the http processing (headers/protocol/etc)
 * which will then create an instance of the configured handler thread (e.g. AS2IncomingFileTransferHandler)
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class HttpRequestHandlerThread(private val httpService: HttpService, conn: HttpServerConnection, private// we don't use this right now, but may need to later(?)
val ownerModule: HttpReceiverServiceBase) : Thread() {
    private val conn: DefaultBHttpServerConnection

    init {
        this.conn = conn as DefaultBHttpServerConnection
    }

    @Throws(IOException::class, HttpException::class)
    private fun processConnection() {
        val context = BasicHttpContext(null)

        while (!Thread.interrupted() && this.conn.isOpen) {
            /* have the service create a handler and pass it the processed request/response/context */
            context.setAttribute("connection", conn)
            this.httpService.handleRequest(this.conn, context)
        }
    }

    /**
     * Just runs the main logic, and handles the exceptions.  If the logic is inside this
     * method it is difficult to see because of all the catches
     */
    override fun run() {
        try {
            logger.info("request handler thread started, processing")
            processConnection()
        } catch (ex: ConnectionClosedException) {
            logger.error(getMessage("Client closed connection", ex), ex)
        } catch (ex: IOException) {
            logger.error(getMessage("I/O error", ex), ex)
        } catch (ex: HttpException) {
            logger.error(getMessage("Unrecoverable HTTP protocol violation", ex), ex)
        } finally {
            try {
                logger.info("shutting down connection " + NetUtil.getEndpointInfo(this.conn))
                this.conn.shutdown()
            } catch (ignore: IOException) {
            }

        }
    }

    private fun getMessage(headerMessage: String, ex: Exception): String {
        return String.format("%s on %s : %s", headerMessage, NetUtil.getEndpointInfo(this.conn), ex.localizedMessage)
    }

    companion object {

        private val logger = LogFactory.getLog(HttpRequestHandlerThread::class.java.simpleName)
    }


}
