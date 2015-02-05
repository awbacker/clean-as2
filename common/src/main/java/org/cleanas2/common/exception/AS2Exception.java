package org.cleanas2.common.exception;

import java.util.Map;

public class AS2Exception extends Exception {

    private Map<String, Object> sources;

    public AS2Exception(String msg) {
        super(msg);
    }

    public AS2Exception(String msg, Throwable cause) {
        super(msg, cause);
    }

}
