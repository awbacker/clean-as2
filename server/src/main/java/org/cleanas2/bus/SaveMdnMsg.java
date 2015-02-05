package org.cleanas2.bus;

import org.cleanas2.message.IncomingSyncMdn;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class SaveMdnMsg extends MessageBase {
    public final IncomingSyncMdn mdn;

    public SaveMdnMsg(IncomingSyncMdn as2mdn) {
        mdn = as2mdn;
    }
}
