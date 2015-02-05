package org.cleanas2.cmd;

import org.boon.Str;

import java.util.ArrayList;
import java.util.List;

import static org.boon.Str.*;
import static org.boon.primitive.Int.sum;

/**
 * Helper for printing out a text-based table of data in the help system.  Very simple, no checking, so be careful.
 * <p/>
 * Example:
 * <pre>
 *     StringTable t = new StringTable("Name", "Age", "Weight");
 *     t.setNoDataMessage("No person records found");
 *     for (Person p : repo.getAllPeople()) {
 *         t.add(p.name, p.age, p.weight);
 *     }
 *     List<String> lines = t.toList();   // each line of output, as a string in a list
 *     String everything = t.toString();  // all lines of ouput, joined with '\n'
 * </pre>
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class StringTable {

    private int indent = 2;
    private int columnSpacing = 4;

    private final String[] headers;
    private final List<Object[]> rows = new ArrayList<>(5);
    private String noDataMessage;

    public StringTable(String... headers) {
        this.headers = headers;
    }

    /**
     * Adds a row of objects to be output.  Must be same length as the # of headers declared.
     * Any null objects are replaced with "<null>"
     */
    public void add(Object... row) {
        for (int i = 0; i < row.length; i++) {
            if (row[i] == null) row[i] = "<null>";
        }
        rows.add(row);
    }

    /**
     * Each line of output, returned as a separate item in a list.  This is most useful for outputting
     * as the result of a command, since that takes a list of output lines to show.
     */
    public List<String> toList() {
        int[] sizes = calculateMaxSizes();
        String lineFormat = createLineFormat(sizes);

        List<String> lines = new ArrayList<>(rows.size() + 2);

        lines.add(String.format(lineFormat, (Object[]) headers)); // silence compiler warning with downcast
        lines.add(rpad(lpad("", indent), sum(sizes) + columnSpacing * headers.length, '-'));

        for (Object[] row : rows) {
            lines.add(String.format(lineFormat, row)); // silence compiler warning with downcast
        }

        if (rows.size() == 0) {
            lines.add(lpad(noDataMessage, indent));
        }

        return lines;
    }

    /**
     * Returns all the output lines, joined into a single string separated by a newline
     */
    public String toString() {
        return Str.joinCollection('\n', toList());
    }

    /**
     * Calculates the maximum size of the data in each column, so the fields can be justified
     */
    private int[] calculateMaxSizes() {
        int[] sizes = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            sizes[i] = headers[i].length();
            for (Object[] r : rows) {
                sizes[i] = Math.max(sizes[i], r[i].toString().length());
            }
        }
        return sizes;
    }

    private String createLineFormat(int[] sizes) {
        StringBuilder format = new StringBuilder(100);
        if (indent > 0) format.append(rpad("", indent));
        for (int sz : sizes) {
            format.append("%-");
            format.append(sz);
            format.append("s");
            format.append(rpad("", columnSpacing));
        }

        return format.toString();
    }

    /**
     * Sets the left margin indent.  Ex: 4 will put 4 spaces before the start of each row
     */
    public void setIndent(int indent) {
        this.indent = indent;
    }

    /**
     * Sets the spacing between each column of data.  Final column does
     */
    public void setColumnSpacing(int columnSpacing) {
        this.columnSpacing = columnSpacing;
    }

    /**
     * Sets the message to show when no data rows are added
     */
    public void setNoDataMessage(String noDataMessage) {
        this.noDataMessage = noDataMessage;
    }
}
