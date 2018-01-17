package org.cleanas2.service

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.boon.json.*
import org.cleanas2.server.MessageBus
import org.cleanas2.bus.*
import org.cleanas2.common.PartnerRecord
import org.cleanas2.common.service.AdminDump
import org.cleanas2.common.service.ConfigurableService
import org.cleanas2.config.json.JsonConfigMap

import javax.inject.Inject
import javax.inject.Singleton
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayList

/**
 * Contains a partner record read from the configuration file
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
@Singleton
class PartnerService @Inject
@Throws(Exception::class)
constructor(options: JsonConfigMap, private val company: CompanyService, private val config: ServerConfiguration) : ConfigurableService, AdminDump {
    var allPartners: List<PartnerRecord>? = null
        private set

    init {
        configure(options)
    }

    @Throws(Exception::class)
    private fun configure(options: JsonConfigMap) {
        val parse = JsonParserFactory()
        val serial = JsonSerializerFactory()

        parse.isCaseInsensitiveFields = true
        parse.isRespectIgnore = true
        parse.isUseAnnotations = true
        serial.isUseAnnotations = true

        options.requireList("partners")
        val mapper = JsonFactory.create(parse, serial)

        // convert back to a json string so it can be mapped back into an object again
        val json = mapper.toJson(options.get("partners")) // partners should be an array (e.g. ValueArray
        this.allPartners = mapper.parser().parseList(PartnerRecord::class.java, json) //mapper.readValue(json, List.class, PartnerRecord.class);
    }

    @Throws(Exception::class)
    override fun initialize() {
        for (p in allPartners!!) {
            val msg = LoadCertificateMsg(p.certificate!!, p.as2id!!)
            MessageBus.publish(msg)
            if (msg.isError) {
                throw msg.errorCause!!
            }

            // create a directory in the outbox of the company for this partner
            val newFolder = config.getDirectory(SystemDir.Outbox).resolve(p.as2id)
            if (!Files.isDirectory(newFolder)) {
                logger.debug("creating new directory : " + newFolder)
                Files.createDirectory(newFolder)
            }

            val wd = WatchDirectoryMsg(newFolder, company.as2id!!, p.as2id!!)
            MessageBus.publishAsync(wd)
        }
    }

    fun getPartner(as2id: String): PartnerRecord? {
        for (p in allPartners!!) {
            if (p.as2id.equals(as2id, ignoreCase = true)) return p
        }
        return null
    }

    override fun dumpCurrentStatus(): List<String> {
        val items = ArrayList<String>()
        items.add("partners = " + this.allPartners!!.size)
        return items
    }

    companion object {
        private val logger = LogFactory.getLog(PartnerService::class.java.simpleName)
    }
}
