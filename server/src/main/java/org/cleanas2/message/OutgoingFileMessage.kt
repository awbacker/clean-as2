package org.cleanas2.message

import org.joda.time.DateTime

import java.nio.file.Path

/**
 * Represents a file that this AS2 server is sending to a remote partner.  It is created
 * by DirectoryPollingModule, or other file watcher, and passed along to the sender to be
 * sent to the partner.
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class OutgoingFileMessage(val filePath: Path, val senderId: String, val receiverId: String) {

    val messageId: String = String.format("<CLEANAS2-%s-%s-%s>", senderId, receiverId, DateTime.now().toString("yyyy-MM-dd-hh-mm-ss"))
    var contentType: String? = null
    var contentDisposition: String? = null
    var outgoingMic: String = ""
    val pendingInfo = PendingInfo()
    var status: String? = null // pending, etc

    val loggingText: String
        get() = receiverId


    class PendingInfo {
        var dataFile: String? = null
        var infoFile: String? = null
    }
}
