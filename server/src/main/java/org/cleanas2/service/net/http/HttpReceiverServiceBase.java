package org.cleanas2.service.net.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.protocol.*;
import org.cleanas2.common.service.AdminDump;
import org.cleanas2.common.service.ConfigurableService;
import org.cleanas2.common.service.StoppableService;
import org.cleanas2.util.Constants;

import java.io.IOException;
import java.util.List;

/**
 * Base class for OpenAS2 HTTP Modules.  Classes that listen for incoming connections should
 * inherit from this class.
 * <p/>
 * The class controls the creation of a HTTP server that listens on the specified port.  All
 * incoming connections are passed to the custom handler, and all protocol details are handled
 * by the server.  The handlers only need to read from / write to the request when appropriate.
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public abstract class HttpReceiverServiceBase implements AdminDump, ConfigurableService, StoppableService {

    private static final Log logger = LogFactory.getLog(HttpReceiverServiceBase.class.getSimpleName());
    private HttpServiceManagerThread serviceManager;

    @Override
    public void initialize() throws Exception {
        try {

            HttpProcessor httpProc = HttpProcessorBuilder.create()
                    .add(new ResponseDate())
                    .add(new ResponseServer(Constants.AS2_SERVER_SENDER_NAME))
                    .add(new ResponseContent())
                    .add(new ResponseConnControl()).build();

            UriHttpRequestHandlerMapper mappings = new UriHttpRequestHandlerMapper();

            // AS2 does not support path mappings, so just accept everything and ignore the path/querystring/etc
            // At some later date we may be able to add support for mapping such as
            //   http://localhost:8100/incoming - post a file
            //   http://localhost:8100/asyncmdn - post an async MDN to us
            mappings.register("*", getHttpRequestHandler());

            // the thread that manages this HttpService will create the socket, etc
            HttpService httpService = new HttpService(httpProc, mappings);

            logger.info(String.format("Starting %s on %d", this.getClass().getSimpleName(), getPort()));

            serviceManager = new HttpServiceManagerThread(this, getPort(), httpService, null);
            serviceManager.setDaemon(true);
            serviceManager.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Provides a custom HTTP handler that is implemented by the deriving class.
     */
    protected abstract HttpRequestHandler getHttpRequestHandler();

    protected abstract int getPort();

    @Override
    public void stop() {
        serviceManager.interrupt();
    }

    @Override
    public List<String> dumpCurrentStatus() {
        return this.serviceManager.dumpCurrentStatus();
    }

}
