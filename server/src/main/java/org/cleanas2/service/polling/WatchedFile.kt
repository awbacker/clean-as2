package org.cleanas2.service.polling

import org.joda.time.DateTime

import java.nio.file.Path

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class WatchedFile(val file: Path, val senderId: String, val receiverId: String) {
    var status = WatchStatus.NEW
    var retries: Int = 0
    var sendAt: DateTime

    constructor(f: Path, parent: WatchedDir) : this(f, parent.senderId, parent.receiverId) {}

    init {
        this.sendAt = DateTime.now().plusSeconds(15)
    }

    override fun toString(): String {
        return String.format("{file=%s, sender=%s, receiver=%s}", file.toString(), senderId, receiverId)
    }

}
