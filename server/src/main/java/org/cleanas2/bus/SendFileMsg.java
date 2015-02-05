package org.cleanas2.bus;

import org.cleanas2.message.OutgoingFileMessage;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class SendFileMsg extends MessageBase {
    public final OutgoingFileMessage as2message;

    public SendFileMsg(OutgoingFileMessage msg) {
        as2message = msg;
    }
}
