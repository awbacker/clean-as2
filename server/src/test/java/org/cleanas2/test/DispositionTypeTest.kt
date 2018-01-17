package org.cleanas2.test

import org.cleanas2.common.exception.AS2Exception
import org.cleanas2.common.disposition.DispositionType
import org.testng.Assert
import org.testng.annotations.Test

import org.testng.Assert.*

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class DispositionTypeTest {

    @Test
    @Throws(AS2Exception::class)
    fun lowercaseFields() {
        val dt = DispositionType.fromString("AutoMatic-Action/MDN-sent-automatically; processed")
        assertEquals(dt.actionMode.toLowerCase(), dt.actionMode)
        dt.isFormatValid
    }

    @Test
    @Throws(AS2Exception::class)
    fun fromSuccess() {
        val dt = DispositionType.fromString("automatic-action/MDN-sent-automatically; processed")
        assertEquals(dt.actionMode, "automatic-action")
        assertEquals(dt.sendingMode, "mdn-sent-automatically")
        assertEquals(dt.dispositionType, "processed")
        Assert.assertFalse(dt.isWarning)
        dt.isFormatValid
    }

    @Test
    @Throws(AS2Exception::class)
    fun fromSuccessHasNullFields() {
        val dt = DispositionType.fromString("automatic-action/MDN-sent-automatically; processed")
        assertEquals("", dt.dispositionDescription)
        assertEquals("", dt.dispositionModifier)
        dt.isFormatValid
    }

    @Test
    @Throws(AS2Exception::class)
    fun inOutEqualWithSimpleString() {
        val dt = DispositionType.fromString("automatic-action/MDN-sent-automatically; processed")
        assertEquals(dt.toString(), "automatic-action/mdn-sent-automatically; processed")
    }

    @Test
    @Throws(AS2Exception::class)
    fun inOutWithSpacesStripped() {
        val dt = DispositionType.fromString("automatic-action / MDN-sent-automatically;    processed")
        assertEquals(dt.toString(), "automatic-action/mdn-sent-automatically; processed")
    }

    @Test
    @Throws(AS2Exception::class)
    fun ingegrityCheckError() {
        val dt = DispositionType.error(DispositionType.ERR_INTEGRITY_CHECK)
        Assert.assertEquals(dt.toString(), "automatic-action/mdn-sent-automatically; processed/error:integrity-check-failed")
    }

}
