package org.cleanas2.common.disposition;

import org.apache.commons.lang.StringUtils;
import org.boon.Lists;
import org.boon.Str;

import java.util.List;
import java.util.StringTokenizer;

import static java.text.MessageFormat.format;


/*
   AS2-disposition-field =
       "Disposition" ":" disposition-mode ";"
       AS2-disposition-type [ '/' AS2-disposition-modifier ]

   disposition-mode =
       action-mode "/" sending-mode

   action-mode =
       "manual-action" | "automatic-action"

   sending-mode =
       "MDN-sent-manually" | "MDN-sent-automatically"

   AS2-disposition-type =
       "processed" | "failed"

   AS2-disposition-modifier =
       ( "error" | "warning" ) | AS2-disposition-modifier-extension

   AS2-disposition-modifier-extension =
       "error: authentication-failed" |
       "error: decompression-failed" |
       "error: decryption-failed" |
       "error: insufficient-message-security" |
       "error: integrity-check-failed" |
       "error: unexpected-processing-error" |
       "warning: " AS2-MDN-warning-description |
       "failure: " AS2-MDN-failure-description

   AS2-MDN-warning-description = *( TEXT )

   AS2-MDN-failure-description = *( TEXT )

   AS2-received-content-MIC-field =
       "Received-content-MIC" ":" encoded-message-digest ","
       digest-alg-id CRLF

   encoded-message-digest =
       1*( 'A'-Z' | 'a'-'z' | '0'-'9' | '/' | '+' | '=' )  (
       i.e. base64( message-digest ) )

   digest-alg-id = "sha1" | "md5"
 */

public class DispositionType {

    public static final String ERR_AUTH = "authentication-failed";
    public static final String ERR_DECOMPRESSION = "decompression-failed";
    public static final String ERR_DECRYPTION = "decryption-failed";
    public static final String ERR_INTEGRITY_CHECK = "integrity-check-failed";
    public static final String ERR_NOT_ENOUGH_SECURITY = "insufficient-message-security";
    public static final String ERR_UNEXPECTED = "unexpected-processing-error";

    public static final String AUTOMATIC_ACTION = "automatic-action";
    public static final String MDN_SENT_AUTOMATICALLY = "mdn-sent-automatically";

    public static final String TYPE_PROCESSED = "processed";
    public static final String TYPE_FAILED = "failed";
    public static final String MOD_ERROR = "error";
    public static final String MOD_WARNING = "warning";


    private static final List<String> _types = Lists.list(TYPE_PROCESSED, TYPE_FAILED);
    private static final List<String> _modifiers = Lists.list(MOD_ERROR, MOD_WARNING, "failure");
    private static final List<String> _errorDescriptions = Lists.list(
            ERR_AUTH, ERR_DECOMPRESSION, ERR_DECRYPTION, ERR_INTEGRITY_CHECK, ERR_UNEXPECTED, ERR_NOT_ENOUGH_SECURITY
    );
    // warnings & failures can have any text

    public final String actionMode;
    public final String sendingMode;
    public final String dispositionType; //
    public final String dispositionModifier;
    public final String dispositionDescription;

    protected DispositionType(String actionMode, String mdnAction, String status, String statusModifier, String statusDescription) {
        super();
        this.actionMode = actionMode;
        this.sendingMode = mdnAction;
        this.dispositionType = status;
        this.dispositionModifier = statusModifier;
        this.dispositionDescription = statusDescription;
    }

    public static DispositionType error(String dispositionDescription) {
        assert (_errorDescriptions.contains(dispositionDescription));
        return new DispositionType(AUTOMATIC_ACTION, MDN_SENT_AUTOMATICALLY,
                TYPE_PROCESSED,
                MOD_ERROR,
                dispositionDescription // should be something like integrity-check-failed
        );
    }

    public static DispositionType success() {
        return new DispositionType(AUTOMATIC_ACTION, MDN_SENT_AUTOMATICALLY,
                TYPE_PROCESSED,
                null,
                null
        );
    }

    public static DispositionType failure(String failureDescription) {
        return new DispositionType(AUTOMATIC_ACTION, MDN_SENT_AUTOMATICALLY,
                TYPE_FAILED,
                "failure",
                failureDescription
        );
    }

    public static DispositionType warning(String warningDescription) {
        return new DispositionType(
                AUTOMATIC_ACTION,
                MDN_SENT_AUTOMATICALLY,
                TYPE_FAILED,
                "failure",
                warningDescription
        );
    }

    public static DispositionType fromString(String disposition) {
        int i = 0;
        String[] items = new String[]{"", "", "", "", ""};
        StringTokenizer tok = new StringTokenizer(disposition + "", "/;:", false);
        while (tok.hasMoreTokens()) {
            items[i] = tok.nextToken().trim().toLowerCase();
            i += 1;
            if (i > 4) break;
        }
        return new DispositionType(items[0], items[1], items[2], items[3], items[4]);
    }

    public static boolean isFormatValid(String dispositionType) {
        return fromString(dispositionType).isFormatValid();
    }

    /**
     * If the disposition type indicated processing was successful
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isSuccess() {
        return TYPE_PROCESSED.equals(dispositionType) && StringUtils.isBlank(dispositionModifier);
    }

    public boolean isWarning() {
        return MOD_WARNING.equalsIgnoreCase(dispositionModifier);
    }

    public String toString() {
        String x = format("{0}/{1}; {2}", actionMode, sendingMode, dispositionType);
        if (!Str.isEmpty(dispositionModifier)) {
            x += format("/{0}:{1}", dispositionModifier, Str.isEmpty(dispositionDescription) ? "" : dispositionDescription);
        }
        return x;
    }

    /**
     * If the format of the disposition type is valid or not (e.g. does not violate any internal rules)
     */
    public boolean isFormatValid() {
        // we don't care about "manual-action" mode right now
        if (StringUtils.isBlank(actionMode) || !actionMode.equalsIgnoreCase(AUTOMATIC_ACTION)) return false;
        if (StringUtils.isBlank(dispositionType) || !_types.contains(dispositionType)) return false;
        if (TYPE_PROCESSED.equals(dispositionType)) {
            if (StringUtils.isBlank(dispositionModifier)) return true;
            if (!_modifiers.contains(dispositionModifier)) return false;
            if (MOD_ERROR.equals(dispositionModifier)) {
                // only certain values are allowed for "error"
                if (!_errorDescriptions.contains(dispositionDescription)) return false;
            } else {
                // warning & failure can have any description, but must have one (?)
                if (StringUtils.isBlank(dispositionDescription)) return false;
            }
        }
        return true;
    }

}
