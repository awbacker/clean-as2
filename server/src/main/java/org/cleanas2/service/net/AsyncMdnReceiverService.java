package org.cleanas2.service.net;

import org.apache.http.protocol.HttpRequestHandler;
import org.cleanas2.server.ServerSession;
import org.cleanas2.config.json.JsonConfigMap;
import org.cleanas2.service.net.http.HttpReceiverServiceBase;

import javax.inject.Inject;

/**
 * This service is a very thin wrapper over the base HttpReceiverServiceBase.  It just provides the
 * configuration port, and the class that should handle the HTTP connection.
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class AsyncMdnReceiverService extends HttpReceiverServiceBase {

    private final int port;

    @Inject
    public AsyncMdnReceiverService(JsonConfigMap options) throws Exception {
        port = options.getSection("server.ports").getInt("receiveMdn");
    }

    @Override
    protected HttpRequestHandler getHttpRequestHandler() {
        return ServerSession.getSession().getInstance(AsyncMdnReceiverHandler.class);
    }

    @Override
    protected int getPort() {
        return port;
    }

}
