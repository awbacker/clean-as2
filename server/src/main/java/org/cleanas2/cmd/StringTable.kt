package org.cleanas2.cmd

import org.boon.Str

import java.util.ArrayList

import org.boon.Str.*
import org.boon.primitive.Int.sum

/**
 * Helper for printing out a text-based table of data in the help system.  Very simple, no checking, so be careful.
 *
 *
 * Example:
 * <pre>
 * StringTable t = new StringTable("Name", "Age", "Weight");
 * t.setNoDataMessage("No person records found");
 * for (Person p : repo.getAllPeople()) {
 * t.add(p.name, p.age, p.weight);
 * }
 * List<String> lines = t.toList();   // each line of output, as a string in a list
 * String everything = t.toString();  // all lines of ouput, joined with '\n'
</String></pre> *
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class StringTable(vararg headers: String?) {

    private var indent = 2
    private var columnSpacing = 4

    private val headers = headers.map { it ?: "null" }.toTypedArray()
    private val rows = ArrayList<Array<Any>>(5)
    private var noDataMessage: String? = null

    /**
     * Adds a row of objects to be output.  Must be same length as the # of headers declared.
     * Any null objects are replaced with "<null>"
    </null> */
    fun add(vararg row: Any?) {
        rows.add(row.map { it ?: "any" }.toTypedArray())
    }

    /**
     * Each line of output, returned as a separate item in a list.  This is most useful for outputting
     * as the result of a command, since that takes a list of output lines to show.
     */
    fun toList(): List<String> {
        val sizes = calculateMaxSizes()
        val lineFormat = createLineFormat(sizes)

        val lines = ArrayList<String>(rows.size + 2)

        lines.add(String.format(lineFormat, *headers)) // silence compiler warning with downcast
        lines.add(rpad(lpad("", indent), sum(sizes) + columnSpacing * headers.size, '-'))

        for (row in rows) {
            lines.add(String.format(lineFormat, *row)) // silence compiler warning with downcast
        }

        if (rows.size == 0) {
            lines.add(lpad(noDataMessage!!, indent))
        }

        return lines
    }

    /**
     * Returns all the output lines, joined into a single string separated by a newline
     */
    override fun toString(): String {
        return Str.joinCollection('\n', toList())
    }

    /**
     * Calculates the maximum size of the data in each column, so the fields can be justified
     */
    private fun calculateMaxSizes(): IntArray {
        val sizes = IntArray(headers.size)
        for (i in headers.indices) {
            sizes[i] = headers[i].length
            for (r in rows) {
                sizes[i] = Math.max(sizes[i], r[i].toString().length)
            }
        }
        return sizes
    }

    private fun createLineFormat(sizes: IntArray): String {
        val format = StringBuilder(100)
        if (indent > 0) format.append(rpad("", indent))
        for (sz in sizes) {
            format.append("%-")
            format.append(sz)
            format.append("s")
            format.append(rpad("", columnSpacing))
        }

        return format.toString()
    }

    /**
     * Sets the left margin indent.  Ex: 4 will put 4 spaces before the start of each row
     */
    fun setIndent(indent: Int) {
        this.indent = indent
    }

    /**
     * Sets the spacing between each column of data.  Final column does
     */
    fun setColumnSpacing(columnSpacing: Int) {
        this.columnSpacing = columnSpacing
    }

    /**
     * Sets the message to show when no data rows are added
     */
    fun setNoDataMessage(noDataMessage: String) {
        this.noDataMessage = noDataMessage
    }
}
