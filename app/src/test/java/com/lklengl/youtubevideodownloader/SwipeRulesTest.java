package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;
public class SwipeRulesTest {
    @Test public void swipeLeftMovesToNextTabAndRightToPrevious(){
        assertEquals(1,SwipeRules.target(0,-105,8,65));
        assertEquals(2,SwipeRules.target(1,-105,8,65));
        assertEquals(1,SwipeRules.target(2,105,8,65));
    }
    @Test public void cannotMoveOutsideFirstOrLastTab(){
        assertEquals(0,SwipeRules.target(0,90,3,65));
        assertEquals(2,SwipeRules.target(2,-90,3,65));
    }
    @Test public void verticalScrollAndSmallGesturesDoNotSwitchTabs(){
        assertEquals(1,SwipeRules.target(1,-120,170,65));
        assertEquals(1,SwipeRules.target(1,20,2,65));
        assertEquals(1,SwipeRules.target(1,-90,85,65));
    }
}
