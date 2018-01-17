package org.cleanas2.bus

import org.cleanas2.message.IncomingSyncMdn

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class SaveMdnMsg(val mdn: IncomingSyncMdn) : MessageBase()
