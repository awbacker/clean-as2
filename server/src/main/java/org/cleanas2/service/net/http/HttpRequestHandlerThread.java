package org.cleanas2.service.net.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpService;
import org.cleanas2.service.net.util.NetUtil;

import java.io.IOException;

/**
 * A thread that is spawned each time an request comes in.  It's main job is to set up the
 * context for the handler, then pass that to the server to do the http processing (headers/protocol/etc)
 * which will then create an instance of the configured handler thread (e.g. AS2IncomingFileTransferHandler)
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class HttpRequestHandlerThread extends Thread {

    private static final Log logger = LogFactory.getLog(HttpRequestHandlerThread.class.getSimpleName());
    private final HttpService httpService;
    private final DefaultBHttpServerConnection conn;
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"}) // we don't use this right now, but may need to later(?)
    private final HttpReceiverServiceBase ownerModule;

    public HttpRequestHandlerThread(final HttpService httpService, final HttpServerConnection conn, HttpReceiverServiceBase ownerModule) {
        super();
        this.httpService = httpService;
        this.ownerModule = ownerModule;
        this.conn = (DefaultBHttpServerConnection) conn;
    }

    private void processConnection() throws IOException, HttpException {
        HttpContext context = new BasicHttpContext(null);

        while (!Thread.interrupted() && this.conn.isOpen()) {
            /* have the service create a handler and pass it the processed request/response/context */
            context.setAttribute("connection", conn);
            this.httpService.handleRequest(this.conn, context);
        }
    }

    /**
     * Just runs the main logic, and handles the exceptions.  If the logic is inside this
     * method it is difficult to see because of all the catches
     */
    @Override
    public void run() {
        try {
            logger.info("request handler thread started, processing");
            processConnection();
        } catch (ConnectionClosedException ex) {
            logger.error(getMessage("Client closed connection", ex), ex);
        } catch (IOException ex) {
            logger.error(getMessage("I/O error", ex), ex);
        } catch (HttpException ex) {
            logger.error(getMessage("Unrecoverable HTTP protocol violation", ex), ex);
        } finally {
            try {
                logger.info("shutting down connection " + NetUtil.getEndpointInfo(this.conn));
                this.conn.shutdown();
            } catch (IOException ignore) {
            }
        }
    }

    private String getMessage(String headerMessage, Exception ex) {
        return String.format("%s on %s : %s", headerMessage, NetUtil.getEndpointInfo(this.conn), ex.getLocalizedMessage());
    }


}
