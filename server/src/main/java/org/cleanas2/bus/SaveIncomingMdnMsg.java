package org.cleanas2.bus;

import org.cleanas2.message.ReplyMdn;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class SaveIncomingMdnMsg extends MessageBase {
    public final ReplyMdn mdn;

    public SaveIncomingMdnMsg(ReplyMdn mdn) {
        this.mdn = mdn;
    }
}
