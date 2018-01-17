package org.cleanas2.bus

import java.nio.file.Path

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class WatchDirectoryMsg(val directory: Path, val senderId: String, val receiverId: String) : MessageBase()
