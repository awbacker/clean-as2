package org.cleanas2.common.service;

import java.util.List;

/**
 * Interface that allows the admin console to identify services that can automatically
 * report their status when a user types > "status" at the command prompt.
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public interface AdminDump {
    public List<String> dumpCurrentStatus();
}
