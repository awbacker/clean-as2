package org.cleanas2.service.polling

import java.nio.file.Path

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class WatchedDir(val directory: Path, val senderId: String, val receiverId: String) {

    override fun toString(): String {
        return String.format("{dir=%s, sender=%s, receiver=%s}", directory.toString(), senderId, receiverId)
    }
}
