package org.cleanas2.service.net;

import org.apache.http.protocol.HttpRequestHandler;
import org.cleanas2.config.json.JsonConfigMap;
import org.cleanas2.service.net.http.HttpReceiverServiceBase;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
@Singleton
public class FileReceiverService extends HttpReceiverServiceBase {

    private final int port;

    @Inject
    public FileReceiverService(JsonConfigMap options) throws Exception {
        port = options.getSection("server.ports").getInt("receiveFile");
    }

    @Override
    protected HttpRequestHandler getHttpRequestHandler() {
        return new PipelineFileReceiverHandler();
    }

    @Override
    protected int getPort() {
        return port;
    }

}
