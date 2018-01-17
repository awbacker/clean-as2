package org.cleanas2.util

import com.cedarsoftware.util.io.JsonWriter
import jdk.nashorn.internal.ir.debug.JSONWriter
import org.boon.json.JsonSerializer
import org.boon.json.JsonSerializerFactory
import org.cleanas2.config.json.JodaTimeSerializer
import org.cleanas2.config.json.NullStringsToBlank
import org.joda.time.DateTime

import java.io.IOException

import org.boon.Maps.map

/**
 * Simple JSON  utility class, mostly used for formatting a JSON document produced by BOON, which is ugly
 * one-liner.  But boon allows a lot more control in exporting, and much better importing, so we use that
 * to do the actual work.
 */
object JsonUtil {


    private val defaultSerializer = JsonSerializerFactory()
            .includeDefaultValues()     // default int = 0.  if value == 0, put it in json anyway
            .includeEmpty()
            .includeNulls()
            .addPropertySerializer(NullStringsToBlank()) // manually convert null strings to ""
            .addTypeSerializer(DateTime::class.java, JodaTimeSerializer())
            .useFieldsOnly()  // don't read from get/set properties
            .useAnnotations() // for @JsonIgnore etc
            .create()

    fun toJson(obj: Any): String {
        return toJson(defaultSerializer, obj)
    }

    fun toJson(serializer: JsonSerializer, obj: Any): String {
        return serializer.serialize(obj).toString()
    }

    fun toPrettyJson(obj: Any): String {
        return toPrettyJson(defaultSerializer, obj)
    }

    fun toPrettyJson(serializer: JsonSerializer, obj: Any): String {
        val uglyJson = toJson(serializer, obj)
        try {
            return JsonWriter.formatJson(uglyJson)
        } catch (e: IOException) {
            try {
                return JsonWriter.objectToJson(
                        mapOf("Error" to "Error formatting json", "Exception" to e.localizedMessage),
                        mapOf(JsonWriter.PRETTY_PRINT to "true" as Any)
                )
            } catch (e1: IOException) {
                return "Fatal error writing json"
            }

        }

    }
}
