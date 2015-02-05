package org.cleanas2.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cleanas2.common.service.ConfigurableService;
import org.cleanas2.config.json.JsonConfigMap;
import org.cleanas2.server.CommandLineConfig;

import javax.inject.*;
import java.nio.file.*;
import java.util.Map;

import static org.boon.Maps.map;

/**
 * Responsible for holding server configuration values, and mapping/getting the system directories
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
@Singleton
public class ServerConfiguration implements ConfigurableService {

    private static final Log logger = LogFactory.getLog(ServerConfiguration.class.getSimpleName());
    private static final String SYSTEM = "{system}";
    private static final String HOME = "{home}";

    private final Map<SystemDir, String> systemDirs = map(
            SystemDir.Home, HOME,
            SystemDir.Mdn, "{system}/mdn",
            SystemDir.System, "{home}/system",
            SystemDir.Certs, "{home}/certs",
            SystemDir.PendingMdn, "{system}/pending/mdn",
            SystemDir.PendingMdnInfo, "{system}/pending/mdn-info",
            SystemDir.Inbox, "{home}/inbox",
            SystemDir.Outbox, "{home}/outbox",
            SystemDir.Temp, "{system}/temp"
    );

    @Inject
    public ServerConfiguration(CommandLineConfig config, JsonConfigMap map) throws Exception {
        systemDirs.put(SystemDir.Home, config.homeDirectory.toString());
        this.configure(map);
    }

    private String url;
    private int mdnPort;

    public Path getDirectory(SystemDir dir) {
        return Paths.get(systemDirs.get(dir));
    }

    private void configure(JsonConfigMap options) throws Exception {
        JsonConfigMap server = options.getSection("server", true);
        this.url = server.getString("url");
        this.mdnPort = server.getInt("ports.receiveMdn");
        if (server.hasSection("directories")) {
            JsonConfigMap dirs = server.getSection("directories");
            Map<String, SystemDir> mapping = map(
                    "certificates", SystemDir.Certs,
                    "system", SystemDir.System,
                    "inbox", SystemDir.Inbox,
                    "outbox", SystemDir.Outbox
            );
            for (String key : mapping.keySet()) {
                if (dirs.has(key)) {
                    systemDirs.put(mapping.get(key), dirs.getString(key));
                }
            }
        }
        logger.info("Home = " + getDirectory(SystemDir.Home));
    }

    @Override
    public void initialize() throws Exception {
        resolvePaths();
        for (SystemDir d : systemDirs.keySet()) {
            Files.createDirectories(getDirectory(d));
        }
    }

    private void resolvePaths() {
        int i = 0;
        while (true) {
            boolean replaced = false;
            for (Map.Entry<SystemDir, String> entry : systemDirs.entrySet()) {
                if (entry.getValue().contains(SYSTEM) || entry.getValue().contains(HOME)) {
                    entry.setValue(
                            entry.getValue()
                                    .replace(SYSTEM, systemDirs.get(SystemDir.System))
                                    .replace(HOME, systemDirs.get(SystemDir.Home))
                    );
                    replaced = true;
                }
            }
            if (!replaced || i++ > 4) break;
        }
    }

    public String getAsyncMdnUrl() {
        return url + ":" + mdnPort;
    }
}
