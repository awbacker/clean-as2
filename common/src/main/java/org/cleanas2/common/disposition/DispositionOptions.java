package org.cleanas2.common.disposition;

import org.cleanas2.common.exception.AS2Exception;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;


/**
 * FROM THE SPEC
 * ---------------------------------
 * Finally, the header, Disposition-notification-options, identifies
 * characteristics of message disposition notification as in [5].  The
 * most important of these options is for indicating the signing options
 * for the MDN, as in the following example:
 * <p/>
 * Disposition-notification-options:
 * signed-receipt-protocol=optional,pkcs7-signature; signed-receipt-micalg=optional,sha1,md5
 * <p/>
 * <p/>
 * The "signed-receipt-protocol" parameter is used to request a
 * signed receipt from the recipient trading partner.  The "signed-
 * receipt-protocol" parameter also specifies the format in which the
 * signed receipt SHOULD be returned to the requester.
 * <p/>
 * The "signed-receipt-micalg" parameter is a list of MIC algorithms
 * preferred by the requester for use in signing the returned
 * receipt.  The list of MIC algorithms SHOULD be honored by the
 * recipient from left to right.
 * <p/>
 * Both the "signed-receipt-protocol" and the "signed- receipt-
 * micalg" option parameters are REQUIRED when requesting a signed
 * receipt.
 */
public class DispositionOptions {

    public final String micAlgorithm;
    public final String micAlgorithmImportance;
    public final String protocol;
    public final String protocolImportance;

    public DispositionOptions(String options) throws AS2Exception {
        if (options == null) throw new AS2Exception("Invalid disposition options format: <NULL>");
        StringTokenizer optionTokens = new StringTokenizer(options, "=,;", false);
        if (optionTokens.countTokens() <= 5) {
            throw new AS2Exception("Invalid disposition options format: " + options);
        }
        try {
            optionTokens.nextToken();
            this.protocolImportance = optionTokens.nextToken().trim();
            this.protocol = optionTokens.nextToken().trim();
            optionTokens.nextToken();
            this.micAlgorithmImportance = optionTokens.nextToken().trim();
            this.micAlgorithm = optionTokens.nextToken().trim();
        } catch (NoSuchElementException e) {
            throw new AS2Exception("Invalid disposition options format: " + options);
        }
    }

    public String toString() {
        return String.format("signed-receipt-protocol=%s, %s; signed-receipt-micalg=%s, %s",
                protocolImportance,
                protocol,
                micAlgorithmImportance,
                micAlgorithm);
    }
}
