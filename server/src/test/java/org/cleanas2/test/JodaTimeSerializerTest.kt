package org.cleanas2.test

import org.boon.json.*
import org.cleanas2.config.json.JodaTimeSerializer
import org.joda.time.DateTime
import org.testng.Assert
import org.testng.annotations.Test

import org.testng.Assert.*

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class JodaTimeSerializerTest {

    @Test
    fun serializesToCorrectText() {
        val d = Dummy()
        d.dateField = DateTime(2012, 2, 1, 1, 2, 3, 456)

        val help = serializer.serialize(d).toString()
        val x = JsonFactory.create().fromJson(help, Map::class.java)

        Assert.assertNotNull(x["dateField"])
        Assert.assertEquals(x["dateField"], "2012-02-01T01:02:03.456")
    }


    private class Dummy {
        internal var dateField: DateTime? = null
    }

    companion object {

        val serializer = JsonSerializerFactory()
                .addTypeSerializer(DateTime::class.java, JodaTimeSerializer())
                .useFieldsOnly()  // don't read from get/set properties
                .create()
    }
}
