package org.cleanas2.common.service

/**
 * Interface that allows the admin console to identify services that can automatically
 * report their status when a user types > "status" at the command prompt.
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
interface AdminDump {
    fun dumpCurrentStatus(): List<String>
}
