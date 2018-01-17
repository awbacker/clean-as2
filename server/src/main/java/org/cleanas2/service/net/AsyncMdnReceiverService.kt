package org.cleanas2.service.net

import org.apache.http.protocol.HttpRequestHandler
import org.cleanas2.server.ServerSession
import org.cleanas2.config.json.JsonConfigMap
import org.cleanas2.service.net.http.HttpReceiverServiceBase

import javax.inject.Inject

/**
 * This service is a very thin wrapper over the base HttpReceiverServiceBase.  It just provides the
 * configuration port, and the class that should handle the HTTP connection.
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class AsyncMdnReceiverService @Inject
@Throws(Exception::class)
constructor(options: JsonConfigMap) : HttpReceiverServiceBase() {

    protected override val port: Int

    protected override val httpRequestHandler: HttpRequestHandler
        get() = ServerSession.session!!.getInstance(AsyncMdnReceiverHandler::class.java)

    init {
        port = options.getSection("server.ports").getInt("receiveMdn")
    }

}
