package org.cleanas2.test;

import junit.framework.Assert;
import org.boon.Lists;
import org.boon.Str;
import org.boon.core.Function;
import org.cleanas2.util.JsonUtil;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.boon.Str.joinCollection;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class BoonExamplesTest {

    @Test
    public void map_list_to_new_list_by_inline_function() {
        List<P> x = new ArrayList<P>();
        x.add(new P());
        x.add(new P());
        try {
            // can't figure out a way to call the age() function directly on the object.  grr...
            String together = Str.joinCollection(',', Lists.mapBy(x, new Function<P, Object>() {
                @Override
                public Object apply(P p) {
                    return p.getName();
                }
            }));
            Assert.assertEquals(together, "abc,abc");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void map_list_to_new_list_by_instance_function() {
        List<P> x = new ArrayList<P>();
        x.add(new P());
        x.add(new P());
        Assert.assertEquals(Str.joinCollection(',', Lists.mapBy(x, this, "getNameFromP")), "abc,abc");
    }

    @Test
    public void pretty_json() {
        P obj = new P();
        obj.name = "steve";
        obj.age = 97;
        String json = JsonUtil.toPrettyJson(obj);
        Assert.assertTrue(json.contains("{"));
        Assert.assertTrue(json.contains("\n"));
        Assert.assertTrue(json.contains("nullString"));
        Assert.assertTrue(json.contains("\"\""));
    }

    private String getNameFromP(P p) {
        return p.getName();
    }

    private static class P {
        public String nullString = null;
        public String name = "abc";
        public int age = 32;
        public String getName() {
            return name;
        }
    }
}
