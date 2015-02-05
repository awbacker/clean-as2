package org.cleanas2.bus;

import java.nio.file.Path;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class WatchDirectoryMsg extends MessageBase {

    public final Path directory;
    public final String senderId;
    public final String receiverId;

    public WatchDirectoryMsg(Path directory, String senderId, String receiverId) {
        this.directory = directory;
        this.senderId = senderId;
        this.receiverId = receiverId;
    }
}
