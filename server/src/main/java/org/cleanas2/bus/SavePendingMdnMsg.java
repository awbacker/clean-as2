package org.cleanas2.bus;

import org.cleanas2.message.OutgoingFileMessage;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class SavePendingMdnMsg extends MessageBase {
    public final OutgoingFileMessage msg;

    public SavePendingMdnMsg(OutgoingFileMessage msg) {
        this.msg = msg;
    }
}
