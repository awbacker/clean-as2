package org.cleanas2.test;

import org.boon.json.*;
import org.cleanas2.config.json.JodaTimeSerializer;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.*;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class JodaTimeSerializerTest {

    @Test
    public void serializesToCorrectText() {
        Dummy d = new Dummy();
        d.dateField = new DateTime(2012, 2, 1, 1, 2, 3, 456);

        String help = serializer.serialize(d).toString();
        Map x = JsonFactory.create().fromJson(help, Map.class);

        Assert.assertNotNull(x.get("dateField"));
        Assert.assertEquals(x.get("dateField"), "2012-02-01T01:02:03.456");
    }


    private static class Dummy {
        DateTime dateField;
    }

    public static final JsonSerializer serializer = new JsonSerializerFactory()
            .addTypeSerializer(DateTime.class, new JodaTimeSerializer())
            .useFieldsOnly()  // don't read from get/set properties
            .create();
}
