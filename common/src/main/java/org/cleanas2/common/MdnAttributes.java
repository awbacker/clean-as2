package org.cleanas2.common;

/**
 * Generic, shared MDN attributes as defined by the spec.  In the case of the messages:
 *
 * incoming async mdn: embedded inside the mime body part in the request
 * outgoing sync mdn : populated, and then placed in the body part
 * incoming sync mdn:  embedded inside the mime body part in the response to the file send
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class MdnAttributes {
    public String reportingUa;
    public String originalRecipient;
    public String finalRecipient;
    public String originalMessageId;

    /**
     * The "content disposition" header from the server.  This will indicate processing errors, etc
     */
    public String contentDisposition;

    /**
     * The MiC that the remote server calculated for the data it received
     */
    public String receivedContentMic;
}
