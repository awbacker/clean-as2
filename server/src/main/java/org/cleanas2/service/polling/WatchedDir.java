package org.cleanas2.service.polling;

import java.nio.file.Path;

/**
* @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
*/
public class WatchedDir {
    public final Path directory;
    public final String senderId;
    public final String receiverId;

    public WatchedDir(Path dir, String senderId, String receiverId) {
        this.directory = dir;
        this.senderId = senderId;
        this.receiverId = receiverId;
    }

    @Override
    public String toString() {
        return String.format("{dir=%s, sender=%s, receiver=%s}", directory.toString(), senderId, receiverId);
    }
}
