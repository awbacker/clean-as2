package org.cleanas2.common.exception;


import org.cleanas2.common.disposition.DispositionType;

public class DispositionException extends Exception {
    private DispositionType disposition;
    private String text;

    public DispositionException(DispositionType disposition, String text, Throwable cause) {
        super(disposition.toString());
        initCause(cause);
        this.disposition = disposition;
        this.text = text;
    }

    public DispositionException(DispositionType disposition, String text) {
        super(disposition.toString());
        this.disposition = disposition;
        this.text = text;
    }

    public static DispositionException error(String statusDescription, String statusMessage) {
        DispositionType typ = DispositionType.error(statusDescription);
        return new DispositionException(typ, statusMessage);
    }

    public static DispositionException error(String statusDescription, String statusMessage, Throwable e) {
        DispositionType typ = DispositionType.error(statusDescription);
        return new DispositionException(typ, statusMessage, e);
    }

    public DispositionType getDisposition() {
        return disposition;
    }

    public void setDisposition(DispositionType disposition) {
        this.disposition = disposition;
    }

    public String getText() {
        return text;
    }

    public void setText(String string) {
        text = string;
    }

}
