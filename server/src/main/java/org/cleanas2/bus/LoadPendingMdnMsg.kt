package org.cleanas2.bus

import org.cleanas2.common.PendingMdnInfoFile

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class LoadPendingMdnMsg(val originalMessageId: String) : MessageBase() {
    var fileData: PendingMdnInfoFile? = null
}
