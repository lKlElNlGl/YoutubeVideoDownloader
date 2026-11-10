package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;

public class Release061RulesTest {
 @Test public void qualityTextHasApprovedNineTwoInsets(){
  assertEquals(9,Release061Rules.individualLeftInsetDp());
  assertEquals(2,Release061Rules.individualRightInsetDp());
  assertEquals(29,Release061Rules.individualExtraWidthDp());
  assertEquals(9,Release059Rules.qualityLeftInsetDp(
   Release061Rules.individualExtraWidthDp(),4,14));
  assertEquals(2,Release059Rules.qualityRightInsetDp(
   Release061Rules.individualExtraWidthDp(),4,14));
 }
 @Test public void scrollbarStartsOnlyBelowPinnedControls(){
  assertFalse(Release061Rules.showScrollbar(1,true,false));
  assertFalse(Release061Rules.showScrollbar(0,false,true));
  assertTrue(Release061Rules.showScrollbar(0,true,true));
  assertFalse(Release061Rules.showScrollbar(4,true,false));
  assertEquals(108,Release061Rules.scrollbarTopPx(100,8));
 }
}
