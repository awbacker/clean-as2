package org.cleanas2.service

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.cleanas2.server.MessageBus
import org.cleanas2.bus.LoadCertificateMsg
import org.cleanas2.common.service.ConfigurableService
import org.cleanas2.config.json.JsonConfigMap

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
@Singleton
class CompanyService @Inject
@Throws(Exception::class)
constructor(options: JsonConfigMap) : ConfigurableService {

    var as2id: String? = null
        private set
    var name: String? = null
        private set
    var email: String? = null
        private set
    private var cert: JsonConfigMap? = null // turn this into a real class

    init {
        configure(options)
    }

    @Throws(Exception::class)
    private fun configure(options: JsonConfigMap) {
        val opts = options.getSection("company", true)
        opts.requireValues("as2id", "email", "name")
        cert = opts.getSectionOrCreate("certificate", "file")
        this.as2id = opts.getString("as2id")
        this.email = opts.getString("email")
        this.name = opts.getString("name")
    }

    @Throws(Exception::class)
    override fun initialize() {
        val loadCert = LoadCertificateMsg(cert!!.getString("file"), this.as2id!!, cert!!.getString("password", ""))
        MessageBus.publish(loadCert)
        if (loadCert.isError) {
            throw loadCert.errorCause!!
        }
    }

    companion object {
        private val logger = LogFactory.getLog(CompanyService::class.java.simpleName)
    }
}
