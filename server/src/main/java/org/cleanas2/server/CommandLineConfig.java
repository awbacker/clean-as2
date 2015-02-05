package org.cleanas2.server;

import java.io.File;
import java.nio.file.Path;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class CommandLineConfig {
    public Path configFile;
    public Path homeDirectory;

    public CommandLineConfig(File configFile) {
        this.configFile = configFile.toPath();
        this.homeDirectory = configFile.toPath().getParent();
    }
}
