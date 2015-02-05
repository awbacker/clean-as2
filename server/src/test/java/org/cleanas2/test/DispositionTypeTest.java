package org.cleanas2.test;

import org.cleanas2.common.exception.AS2Exception;
import org.cleanas2.common.disposition.DispositionType;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class DispositionTypeTest {

    @Test
    public void lowercaseFields() throws AS2Exception {
        DispositionType dt = DispositionType.fromString("AutoMatic-Action/MDN-sent-automatically; processed");
        assertEquals(dt.actionMode.toLowerCase(), dt.actionMode);
        dt.isFormatValid();
    }

    @Test
    public void fromSuccess() throws AS2Exception {
        DispositionType dt = DispositionType.fromString("automatic-action/MDN-sent-automatically; processed");
        assertEquals(dt.actionMode, "automatic-action");
        assertEquals(dt.sendingMode, "mdn-sent-automatically");
        assertEquals(dt.dispositionType, "processed");
        Assert.assertFalse(dt.isWarning());
        dt.isFormatValid();
    }

    @Test
    public void fromSuccessHasNullFields() throws AS2Exception {
        DispositionType dt = DispositionType.fromString("automatic-action/MDN-sent-automatically; processed");
        assertEquals("", dt.dispositionDescription);
        assertEquals("", dt.dispositionModifier);
        dt.isFormatValid();
    }

    @Test
    public void inOutEqualWithSimpleString() throws AS2Exception {
        DispositionType dt = DispositionType.fromString("automatic-action/MDN-sent-automatically; processed");
        assertEquals(dt.toString(), "automatic-action/mdn-sent-automatically; processed");
    }

    @Test
    public void inOutWithSpacesStripped() throws AS2Exception {
        DispositionType dt = DispositionType.fromString("automatic-action / MDN-sent-automatically;    processed");
        assertEquals(dt.toString(), "automatic-action/mdn-sent-automatically; processed");
    }

    @Test
    public void ingegrityCheckError() throws AS2Exception {
        Object dt = DispositionType.error(DispositionType.ERR_INTEGRITY_CHECK);
        Assert.assertEquals(dt.toString(), "automatic-action/mdn-sent-automatically; processed/error:integrity-check-failed");
    }

}
