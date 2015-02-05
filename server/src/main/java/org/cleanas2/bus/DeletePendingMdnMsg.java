package org.cleanas2.bus;

import org.cleanas2.common.PendingMdnInfoFile;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class DeletePendingMdnMsg extends MessageBase {

    public final String originalMessageId;
    public PendingMdnInfoFile fileData;

    public DeletePendingMdnMsg(String originalMessageId) {
        this.originalMessageId = originalMessageId;
    }
}
