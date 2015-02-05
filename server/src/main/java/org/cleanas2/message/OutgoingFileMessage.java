package org.cleanas2.message;

import org.joda.time.DateTime;

import java.nio.file.Path;

/**
 * Represents a file that this AS2 server is sending to a remote partner.  It is created
 * by DirectoryPollingModule, or other file watcher, and passed along to the sender to be
 * sent to the partner.
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class OutgoingFileMessage {

    public final String senderId;
    public final String receiverId;
    public final String messageId;
    public final Path filePath;
    public String contentType;
    public String contentDisposition;
    public String outgoingMic;
    public final PendingInfo pendingInfo = new PendingInfo();
    public String status; // pending, etc

    public OutgoingFileMessage(Path file, String senderId, String receiverId) {
        this.filePath = file;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageId = String.format("<CLEANAS2-%s-%s-%s>", senderId, receiverId, DateTime.now().toString("yyyy-MM-dd-hh-mm-ss"));
    }

     public static class PendingInfo {
        public String dataFile;
        public String infoFile;
    }

    public String getLoggingText() {
        return messageId;
    }

}
