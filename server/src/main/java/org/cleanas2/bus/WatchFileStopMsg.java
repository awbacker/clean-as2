package org.cleanas2.bus;

import java.nio.file.Path;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class WatchFileStopMsg extends MessageBase {
    public final Path filePath;
    public WatchFileStopMsg(Path filePath) {
        this.filePath = filePath;
    }
}
