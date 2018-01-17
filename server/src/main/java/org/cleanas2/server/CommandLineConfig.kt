package org.cleanas2.server

import java.io.File
import java.nio.file.Path

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class CommandLineConfig(configFile: File) {
    var configFile: Path
    var homeDirectory: Path

    init {
        this.configFile = configFile.toPath()
        this.homeDirectory = configFile.toPath().parent
    }
}
