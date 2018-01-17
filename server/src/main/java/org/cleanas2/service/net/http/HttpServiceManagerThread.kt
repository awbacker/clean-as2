package org.cleanas2.service.net.http

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.HttpConnectionFactory
import org.apache.http.HttpServerConnection
import org.apache.http.impl.DefaultBHttpServerConnection
import org.apache.http.impl.DefaultBHttpServerConnectionFactory
import org.apache.http.protocol.HttpService
import org.cleanas2.common.service.AdminDump
import org.cleanas2.service.net.util.NetUtil

import javax.net.ServerSocketFactory
import java.io.IOException
import java.io.InterruptedIOException
import java.net.*
import java.util.ArrayList

/**
 * A Persistent thread that runs the HTTP server and accepts connections on a socket
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
internal class HttpServiceManagerThread @Throws(IOException::class)
constructor(private val ownerModule: HttpReceiverServiceBase, port: Int, private val httpService: HttpService, sf: ServerSocketFactory?) : Thread(), AdminDump {
    private val connFactory: HttpConnectionFactory<DefaultBHttpServerConnection>

    private val serverSocket: ServerSocket

    init {
        this.connFactory = DefaultBHttpServerConnectionFactory.INSTANCE
        this.serverSocket = if (sf != null) sf.createServerSocket(port) else ServerSocket(port)
        this.serverSocket.soTimeout = 30 * 1000 // 30 second timeout
    }

    override fun run() {
        while (!Thread.interrupted()) {
            try {
                // wait for a connection on this socket
                val socket = this.serverSocket.accept()
                logger.info("Incoming connection " + NetUtil.getClientInfo(socket) + " ==> " + NetUtil.getServerInfo(socket))

                // converts the socket request into an HTTP connection
                val conn = this.connFactory.createConnection(socket)
                val t = HttpRequestHandlerThread(this.httpService, conn, this.ownerModule)
                t.isDaemon = true
                t.start()
            } catch (ignored: SocketTimeoutException) {
                // ignore and keep looping
            } catch (ignored: InterruptedIOException) {
                logger.info("  ~ interrupted io exception")
            } catch (e: SocketException) {
                // if the thread WAS interrupted, it was because we were asked to shut down, so
                // we can ignore that case and just wait for the while() loop to force exit
                if (!this.isInterrupted) {
                    logger.error("Socket exception in active thread", e)
                }
            } catch (e: IOException) {
                logger.error("I/O exception: " + e.message)
                break
            }

        }
    }


    override fun interrupt() {
        super.interrupt()
        try {
            if (!serverSocket.isClosed) {
                this.serverSocket.close()
            }
        } catch (e: IOException) {
            logger.error("Error closing server socket", e)
        }

    }

    override fun dumpCurrentStatus(): List<String> {
        val messages = ArrayList<String>()
        messages.add("listening on port  = " + serverSocket.localPort)
        messages.add("http service       = " + httpService.javaClass.simpleName)
        messages.add("connection factory = " + connFactory.javaClass.simpleName)
        return messages
    }

    companion object {

        private val logger = LogFactory.getLog(HttpServiceManagerThread::class.java.simpleName)
    }
}
