package org.cleanas2.service.net.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpConnectionFactory;
import org.apache.http.HttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.protocol.HttpService;
import org.cleanas2.common.service.AdminDump;
import org.cleanas2.service.net.util.NetUtil;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A Persistent thread that runs the HTTP server and accepts connections on a socket
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
class HttpServiceManagerThread extends Thread implements AdminDump {

    private static final Log logger = LogFactory.getLog(HttpServiceManagerThread.class.getSimpleName());
    private final HttpConnectionFactory<DefaultBHttpServerConnection> connFactory;

    private final ServerSocket serverSocket;
    private final HttpService httpService;
    private final HttpReceiverServiceBase ownerModule;

    public HttpServiceManagerThread(HttpReceiverServiceBase module, final int port, final HttpService httpService, final ServerSocketFactory sf) throws IOException {
        this.connFactory = DefaultBHttpServerConnectionFactory.INSTANCE;
        this.serverSocket = sf != null ? sf.createServerSocket(port) : new ServerSocket(port);
        this.httpService = httpService;
        this.ownerModule = module;
        this.serverSocket.setSoTimeout(30 * 1000); // 30 second timeout
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                // wait for a connection on this socket
                Socket socket = this.serverSocket.accept();
                logger.info("Incoming connection " + NetUtil.getClientInfo(socket) + " ==> " + NetUtil.getServerInfo(socket));

                // converts the socket request into an HTTP connection
                HttpServerConnection conn = this.connFactory.createConnection(socket);
                Thread t = new HttpRequestHandlerThread(this.httpService, conn, this.ownerModule);
                t.setDaemon(true);
                t.start();
            } catch (SocketTimeoutException ignored) {
                // ignore and keep looping
            } catch (InterruptedIOException ignored) {
                logger.info("  ~ interrupted io exception");
            } catch (SocketException e) {
                // if the thread WAS interrupted, it was because we were asked to shut down, so
                // we can ignore that case and just wait for the while() loop to force exit
                if (!this.isInterrupted()) {
                    logger.error("Socket exception in active thread", e);
                }
            } catch (IOException e) {
                logger.error("I/O exception: " + e.getMessage());
                break;
            }
        }
    }


    @Override
    public void interrupt() {
        super.interrupt();
        try {
            if (!serverSocket.isClosed()) {
                this.serverSocket.close();
            }
        } catch (IOException e) {
            logger.error("Error closing server socket", e);
        }
    }

    @Override
    public List<String> dumpCurrentStatus() {
        List<String> messages = new ArrayList<>();
        messages.add("listening on port  = " + serverSocket.getLocalPort());
        messages.add("http service       = " + httpService.getClass().getSimpleName());
        messages.add("connection factory = " + connFactory.getClass().getSimpleName());
        return messages;
    }
}
