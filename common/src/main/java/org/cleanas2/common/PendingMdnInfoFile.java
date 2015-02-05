package org.cleanas2.common;

/**
 * Data that is stored when waiting for an ASYNC MDN to be received.   These are matched by MESSAGE ID,
 * but the message ID is not stored in this structure.
 *
 * THE FILE NAME THAT CONTAINS THIS DATA *IS* THE MESSAGE ID
 * eg:  /system/pending/.../cleanas2-from-me-to-you-2010-02-02-11-11-32
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class PendingMdnInfoFile {
    public String originalFile;
    public String pendingFile;
    public String outgoingMic;
}
