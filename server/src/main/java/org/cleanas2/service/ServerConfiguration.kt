package org.cleanas2.service

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.cleanas2.common.service.ConfigurableService
import org.cleanas2.config.json.JsonConfigMap
import org.cleanas2.server.CommandLineConfig

import javax.inject.*
import java.nio.file.*

import org.boon.Maps.map

/**
 * Responsible for holding server configuration values, and mapping/getting the system directories
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
@Singleton
class ServerConfiguration @Inject
@Throws(Exception::class)
constructor(config: CommandLineConfig, map: JsonConfigMap) : ConfigurableService {

    private val systemDirs = map(
            SystemDir.Home, HOME,
            SystemDir.Mdn, "{system}/mdn",
            SystemDir.System, "{home}/system",
            SystemDir.Certs, "{home}/certs",
            SystemDir.PendingMdn, "{system}/pending/mdn",
            SystemDir.PendingMdnInfo, "{system}/pending/mdn-info",
            SystemDir.Inbox, "{home}/inbox",
            SystemDir.Outbox, "{home}/outbox",
            SystemDir.Temp, "{system}/temp"
    )

    private var url: String? = null
    private var mdnPort: Int = 0

    val asyncMdnUrl: String
        get() = url + ":" + mdnPort

    init {
        systemDirs.put(SystemDir.Home, config.homeDirectory.toString())
        this.configure(map)
    }

    fun getDirectory(dir: SystemDir): Path {
        return Paths.get(systemDirs[dir])
    }

    @Throws(Exception::class)
    private fun configure(options: JsonConfigMap) {
        val server = options.getSection("server", true)
        this.url = server.getString("url")
        this.mdnPort = server.getInt("ports.receiveMdn")
        if (server.hasSection("directories")) {
            val dirs = server.getSection("directories")
            val mapping = map(
                    "certificates", SystemDir.Certs,
                    "system", SystemDir.System,
                    "inbox", SystemDir.Inbox,
                    "outbox", SystemDir.Outbox
            )
            for (key in mapping.keys) {
                if (dirs.has(key)) {
                    systemDirs.put(mapping[key], dirs.getString(key))
                }
            }
        }
        logger.info("Home = " + getDirectory(SystemDir.Home))
    }

    @Throws(Exception::class)
    override fun initialize() {
        resolvePaths()
        for (d in systemDirs.keys) {
            Files.createDirectories(getDirectory(d))
        }
    }

    private fun resolvePaths() {
        var i = 0
        while (true) {
            var replaced = false
            for (entry in systemDirs.entries) {
                if (entry.value.contains(SYSTEM) || entry.value.contains(HOME)) {
                    entry.setValue(
                            entry.value
                            .replace(SYSTEM, systemDirs[SystemDir.System]!!)
                            .replace(HOME, systemDirs[SystemDir.Home]!!)
                    )
                    replaced = true
                }
            }
            if (!replaced || i++ > 4) break
        }
    }

    companion object {
        private val logger = LogFactory.getLog(ServerConfiguration::class.java.simpleName)
        private val SYSTEM: String = "{system}"
        private val HOME = "{home}"
    }
}
