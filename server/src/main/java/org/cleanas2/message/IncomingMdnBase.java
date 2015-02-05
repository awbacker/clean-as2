package org.cleanas2.message;

import org.cleanas2.common.MdnAttributes;

/**
 * Base class for both IncomingAsyncMdn and IncomingSyncMdn.
 *
 * The fields in this base class are used when initializing an MDN from an incoming MIME message
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public abstract class IncomingMdnBase {

    /**
     * The content of the "text/plain" body part (contained in the multipart)
     */
    public String bodyText;

    /**
     * The attributes in the "message/disposition-notification" body part (contained in the multipart)
     */
    public final MdnAttributes attributes = new MdnAttributes();
}
