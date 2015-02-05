package org.cleanas2.common;

/**
 * The MDN status is used during processing of an incoming ASYNC mdn to indicate
 * either errors or successes.  The processing failure can then be dealt with at a central
 * location, rather than through throwing exceptions
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public enum MdnReceiveStatus {
    OK,
    MIC_NOT_MATCHED,
    NO_CONTENT,
    INVALID_DISPOSITION,
    PROCESSING_FAILED,
    ASYNC_LOAD_ERROR
}
