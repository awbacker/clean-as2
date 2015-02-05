package org.cleanas2.test;

import org.cleanas2.util.AS2Util;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class MdnFilenameTest {

    @Test
    public void removesBrackets() {
        Assert.assertEquals("abc-123-jim@place-sam@place_2", AS2Util.makeFileName("<abc-123-jim@place-sam@place_2>"));
    }

    @Test
    public void deletesBadChars() {
        Assert.assertEquals("", AS2Util.makeFileName("<>()$%^&*"));
    }

    @Test
    public void allowsGoodChars() {
        Assert.assertEquals("@#-_.", AS2Util.makeFileName("@#-_."));
    }

    @Test
    public void normalizesFileName() {
        Assert.assertEquals("abc", AS2Util.makeFileName("../../abc"));
    }


}
