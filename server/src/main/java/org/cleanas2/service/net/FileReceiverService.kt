package org.cleanas2.service.net

import org.apache.http.protocol.HttpRequestHandler
import org.cleanas2.config.json.JsonConfigMap
import org.cleanas2.service.net.http.HttpReceiverServiceBase

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
@Singleton
class FileReceiverService @Inject
@Throws(Exception::class)
constructor(options: JsonConfigMap) : HttpReceiverServiceBase() {

    protected override val port: Int

    protected override val httpRequestHandler: HttpRequestHandler
        get() = PipelineFileReceiverHandler()

    init {
        port = options.getSection("server.ports").getInt("receiveFile")
    }

}
