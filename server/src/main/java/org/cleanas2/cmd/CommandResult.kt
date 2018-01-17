package org.cleanas2.cmd

import java.util.*

import org.boon.Lists.list

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class CommandResult {
    val results: MutableList<String> = ArrayList(5)
    var terminateService = false

    fun add(formatString: String, vararg params: Any) {
        results.add(String.format(formatString, *params))
    }

    fun add(lines: Array<String>) {
        results.addAll(list(*lines))
    }

    fun add(items: Collection<String>) {
        results.addAll(items)
    }
}
