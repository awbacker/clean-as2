package org.cleanas2.service.polling;

import org.joda.time.DateTime;

import java.nio.file.Path;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class WatchedFile {
    public final Path file;
    public final String senderId;
    public final String receiverId;
    public WatchStatus status = WatchStatus.NEW;
    public int retries;
    public DateTime sendAt;

    public WatchedFile(Path f, WatchedDir parent) {
        this(f, parent.senderId, parent.receiverId);
    }

    public WatchedFile(Path f, String s, String r) {
        this.file = f;
        this.senderId = s;
        this.receiverId = r;
        this.sendAt = DateTime.now().plusSeconds(15);
    }

    @Override
    public String toString() {
        return String.format("{file=%s, sender=%s, receiver=%s}", file.toString(), senderId, receiverId);
    }

}
