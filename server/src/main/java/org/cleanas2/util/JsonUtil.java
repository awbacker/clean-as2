package org.cleanas2.util;

import com.cedarsoftware.util.io.JsonWriter;
import org.boon.json.JsonSerializer;
import org.boon.json.JsonSerializerFactory;
import org.cleanas2.config.json.JodaTimeSerializer;
import org.cleanas2.config.json.NullStringsToBlank;
import org.joda.time.DateTime;

import java.io.IOException;

import static org.boon.Maps.map;

/**
 * Simple JSON  utility class, mostly used for formatting a JSON document produced by BOON, which is ugly
 * one-liner.  But boon allows a lot more control in exporting, and much better importing, so we use that
 * to do the actual work.
 */
public class JsonUtil {

    public static final JsonSerializer defaultSerializer = new JsonSerializerFactory()
            .includeDefaultValues()     // default int = 0.  if value == 0, put it in json anyway
            .includeEmpty()
            .includeNulls()
            .addPropertySerializer(new NullStringsToBlank()) // manually convert null strings to ""
            .addTypeSerializer(DateTime.class, new JodaTimeSerializer())
            .useFieldsOnly()  // don't read from get/set properties
            .useAnnotations() // for @JsonIgnore etc
            .create();

    public static String toJson(Object obj) {
        return toJson(defaultSerializer, obj);
    }

    public static String toJson(JsonSerializer serializer, Object obj) {
        return serializer.serialize(obj).toString();
    }

    public static String toPrettyJson(Object obj) {
        return toPrettyJson(defaultSerializer, obj);
    }

    public static String toPrettyJson(JsonSerializer serializer, Object obj) {
        String uglyJson = toJson(serializer, obj);
        try {
            return JsonWriter.formatJson(uglyJson);
        } catch (IOException e) {
            try {
                return JsonWriter.objectToJson(
                        map(
                                "Error", "Error formatting json",
                                "Exception", e.getLocalizedMessage()
                        ),
                        map(
                                JsonWriter.PRETTY_PRINT, (Object)"true"
                        )
                );
            } catch (IOException e1) {
                return "Fatal error writing json";
            }
        }
    }

}
