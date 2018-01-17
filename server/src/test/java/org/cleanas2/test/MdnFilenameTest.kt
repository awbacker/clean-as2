package org.cleanas2.test

import org.cleanas2.util.AS2Util
import org.testng.Assert
import org.testng.annotations.Test

import org.testng.Assert.assertEquals

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class MdnFilenameTest {

    @Test
    fun removesBrackets() {
        Assert.assertEquals("abc-123-jim@place-sam@place_2", AS2Util.makeFileName("<abc-123-jim@place-sam@place_2>"))
    }

    @Test
    fun deletesBadChars() {
        Assert.assertEquals("", AS2Util.makeFileName("<>()$%^&*"))
    }

    @Test
    fun allowsGoodChars() {
        Assert.assertEquals("@#-_.", AS2Util.makeFileName("@#-_."))
    }

    @Test
    fun normalizesFileName() {
        Assert.assertEquals("abc", AS2Util.makeFileName("../../abc"))
    }


}
