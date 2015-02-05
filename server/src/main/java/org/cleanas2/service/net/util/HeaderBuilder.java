package org.cleanas2.service.net.util;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.util.ArrayList;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class HeaderBuilder {
    private final ArrayList<BasicHeader> headers = new ArrayList<>(10);

    public void add(String name, String value) {
        if (value != null) {
            headers.add(new BasicHeader(name, value));
        }
    }

    public Header[] toArray() {
        return headers.toArray(new Header[headers.size()]);
    }
}
