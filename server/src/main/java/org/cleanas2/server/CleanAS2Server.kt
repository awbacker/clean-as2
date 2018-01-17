package org.cleanas2.server

import com.google.inject.Guice
import com.google.inject.Injector
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.cleanas2.cmd.CommandLineService
import org.cleanas2.common.service.ConfigurableService
import org.cleanas2.service.*
import org.cleanas2.service.net.*
import org.cleanas2.service.polling.DirectoryPollingService
import org.cleanas2.service.storage.FileSystemStorageService

import java.io.File

import org.boon.Lists.list
import org.cleanas2.util.AS2Util.ofType

/**
 * Ugly default server startup code
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
object CleanAS2Server {

    private val logger = LogFactory.getLog(CleanAS2Server::class.java.simpleName)

    @JvmStatic
    fun main(args: Array<String>) {
        logger.info("Server starting")
        if (args.size == 0) {
            logger.info("first parameter must be the config file to load")
            return
        }

        val configFile = File(args[0]).absoluteFile
        if (!configFile.exists()) {
            logger.error("First parameter must be a configuration file (home/config.json, etc)")
            return
        }

        try {
            run(configFile)
        } catch (e: Exception) {
            logger.error("Error starting up server: ", e)
        }

        logger.debug("server stopped")
    }

    @Throws(Exception::class)
    private fun run(configFile: File) {
        val c = CommandLineConfig(configFile)
        val i = Guice.createInjector(JsonConfiguredServerModule(c))

        ServerSession.initialize(i)
        val session = ServerSession.session

        // create the additional services
        val services = list(
                // primary services... start first
                ServerConfiguration::class.java,
                CertificateService::class.java,
                CompanyService::class.java,
                PartnerService::class.java,
                // ---------
                ServerEventConsoleLog::class.java,
                AsyncMdnReceiverService::class.java,
                AsyncMdnSenderService::class.java,
                DirectoryPollingService::class.java,
                FileSystemStorageService::class.java,
                FileReceiverService::class.java,
                FileSenderService::class.java,
                CommandLineService::class.java
        )

        for (c1 in services) {
            session!!.startService(c1)
        }

        val configable = ofType(session!!.getAllServices(), ConfigurableService::class.java)

        for (svc in configable) {
            svc.initialize()
        }
    }

}
