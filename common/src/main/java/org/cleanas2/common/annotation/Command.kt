package org.cleanas2.common.annotation

import java.lang.annotation.*

/**
 * Annotation interface that is applied to all commands, which provides the name
 * of the command (used to run it) and a description (shown in the 'help') list,
 * as well as the "group" the command belongs to.
 *
 * e.g.  "#> cert view my-partner" =>  @Command(name="view", group="cert", description="show an individual cert")
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
annotation class Command(val name: String, val group: String = "", val description: String = "No description provided")
