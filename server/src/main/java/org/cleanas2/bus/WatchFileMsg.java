package org.cleanas2.bus;

import java.nio.file.Path;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class WatchFileMsg extends MessageBase {
    public final Path file;
    public WatchFileMsg(Path file) {
        this.file = file;
    }
}
