package org.cleanas2.config.json

import org.boon.json.serializers.CustomObjectSerializer
import org.boon.json.serializers.JsonSerializerInternal
import org.boon.primitive.CharBuf
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

/**
 * Handles serializing a Joda DateTime instance into a string for storage in a JSON.  This does
 * not handle deserialization.
 *
 * It writes in the format "2012-02-02T19:30:15.323" for Jan 02, 2012 @ 7pm 30m 15s 323ms
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class JodaTimeSerializer : CustomObjectSerializer<DateTime> {

    /**
     * Yay for java type erasure!  Return the same time we were created with so that we can figure it out later
     */
    override fun type(): Class<DateTime> {
        return DateTime::class.java
    }

    override fun serializeObject(serializer: JsonSerializerInternal, instance: DateTime, builder: CharBuf) {
        val s = instance.toString(ISODateTimeFormat.dateHourMinuteSecondMillis())
        builder.addJsonEscapedString(s.toCharArray())
    }

}
