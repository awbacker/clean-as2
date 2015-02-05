package org.cleanas2.bus;

import org.cleanas2.message.ReplyMdn;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class SendAsyncMdnMsg extends MessageBase {

    public final ReplyMdn mdn;

    public SendAsyncMdnMsg(ReplyMdn mdn) {
        super();
        this.mdn = mdn;
    }
}
