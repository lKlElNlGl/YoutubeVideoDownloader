package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;
public class SwipeAccessibilityTest {
    @Test public void typicalFortyFiveDpHorizontalGestureChangesPage(){
        assertEquals(1,SwipeRules.target(0,-45,8,40));
        assertEquals(0,SwipeRules.target(1,45,8,40));
    }
    @Test public void predominantlyVerticalMotionDoesNotChangePage(){
        assertEquals(0,SwipeRules.target(0,-120,130,40));
    }
}
