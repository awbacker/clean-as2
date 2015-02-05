package org.cleanas2.service;

/**
 * The available pre-defined system directories.  The config service can be queried to get the
 * path based on these values.
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public enum SystemDir {
    Home,
    System,
    Certs,
    PendingMdn,
    PendingMdnInfo,
    Inbox,
    Outbox,
    Mdn,
    Temp,
}
