package org.cleanas2.bus

import org.cleanas2.message.ReplyMdn

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class SendAsyncMdnMsg(val mdn: ReplyMdn) : MessageBase()