package org.cleanas2.test

import org.junit.Assert
import org.boon.Lists
import org.boon.Str
import org.boon.core.Function
import org.cleanas2.util.JsonUtil
import org.testng.annotations.Test

import java.util.ArrayList

import org.boon.Str.joinCollection
import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class BoonExamplesTest {

    @Test
    fun map_list_to_new_list_by_inline_function() {
        val x = ArrayList<P>()
        x.add(P())
        x.add(P())
        try {
            // can't figure out a way to call the age() function directly on the object.  grr...
            val together = Str.joinCollection(',', Lists.mapBy(x) { p -> p.name })
            Assert.assertEquals(together, "abc,abc")
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail()
        }

    }

    @Test
    fun map_list_to_new_list_by_instance_function() {
        val x = ArrayList<P>()
        x.add(P())
        x.add(P())
        Assert.assertEquals(Str.joinCollection(',', Lists.mapBy(x, this, "getNameFromP")), "abc,abc")
    }

    @Test
    fun pretty_json() {
        val obj = P()
        obj.name = "steve"
        obj.age = 97
        val json = JsonUtil.toPrettyJson(obj)
        Assert.assertTrue(json.contains("{"))
        Assert.assertTrue(json.contains("\n"))
        Assert.assertTrue(json.contains("nullString"))
        Assert.assertTrue(json.contains("\"\""))
    }

    private fun getNameFromP(p: P): String {
        return p.name
    }

    private class P {
        var nullString: String? = null
        var name = "abc"
        var age = 32
    }
}
