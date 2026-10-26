package com.lklengl.youtubevideodownloader;

import org.junit.Test;
import static org.junit.Assert.*;

public class VersionSupportTest {
    @Test public void numericComparison() {
        assertTrue(VersionSupport.compare("v0.2.10", "0.2.9") > 0);
        assertTrue(VersionSupport.compare("v0.3.0", "0.2.999") > 0);
        assertEquals(0, VersionSupport.compare("0.2.1", "v0.2.1"));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsMalformedTags() {
        VersionSupport.compare("v0.2.1-rc1", "0.2.0");
    }
}
