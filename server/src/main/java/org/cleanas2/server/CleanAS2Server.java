package org.cleanas2.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cleanas2.cmd.CommandLineService;
import org.cleanas2.common.service.ConfigurableService;
import org.cleanas2.service.*;
import org.cleanas2.service.net.*;
import org.cleanas2.service.polling.DirectoryPollingService;
import org.cleanas2.service.storage.FileSystemStorageService;

import java.io.File;
import java.util.List;

import static org.boon.Lists.list;
import static org.cleanas2.util.AS2Util.ofType;

/**
 * Ugly default server startup code
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class CleanAS2Server {

    private static final Log logger = LogFactory.getLog(CleanAS2Server.class.getSimpleName());

    public static void main(String[] args) {
        logger.info("Server starting");
        if (args.length == 0) {
            logger.info("first parameter must be the config file to load");
            return;
        }

        File configFile = new File(args[0]).getAbsoluteFile();
        if (!configFile.exists()) {
            logger.error("First parameter must be a configuration file (home/config.json, etc)");
            return;
        }

        try {
            run(configFile);
        } catch (Exception e) {
            logger.error("Error starting up server: ", e);
        }

        logger.debug("server stopped");
    }

    private static void run(File configFile) throws Exception {
        CommandLineConfig c = new CommandLineConfig(configFile);
        Injector i = Guice.createInjector(new JsonConfiguredServerModule(c));

        ServerSession.initialize(i);
        ServerSession session = ServerSession.getSession();

        // create the additional services
        List<Class<?>> services = list(
                // primary services... start first
                ServerConfiguration.class,
                CertificateService.class,
                CompanyService.class,
                PartnerService.class,
                // ---------
                ServerEventConsoleLog.class,
                AsyncMdnReceiverService.class,
                AsyncMdnSenderService.class,
                DirectoryPollingService.class,
                FileSystemStorageService.class,
                FileReceiverService.class,
                FileSenderService.class,
                CommandLineService.class
        );

        for (Class c1 : services) {
            session.startService(c1);
        }

        List<ConfigurableService> configable = ofType(session.getAllServices(), ConfigurableService.class);

        for (ConfigurableService svc : configable) {
            svc.initialize();
        }
    }

}
