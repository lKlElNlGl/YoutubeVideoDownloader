package com.lklengl.youtubevideodownloader;

import org.junit.Test;
import static org.junit.Assert.*;

/** Regression: old v0.5.8 card styling with one continuous, pinned list. */
public class Release060RulesTest {
 @Test public void cardBeginsBelowSummaryAndStopsAtToolbarWhenPinned(){
  assertEquals(132,Release060Rules.cardTop(0,0,132));
  assertEquals(20,Release060Rules.cardTop(0,-112,132));
  assertEquals(0,Release060Rules.cardTop(0,-132,132));
  assertEquals(0,Release060Rules.cardTop(4,-20,132));
 }
 @Test public void playlistCardRetainsOriginalCornersAndContentInset(){
  assertEquals(14,Release060Rules.playlistCardRadiusDp());
  assertEquals(8,Release060Rules.playlistCardTopInsetDp());
 }
 @Test public void individualQualityShiftsOneDpRightWithoutChangingWidth(){
  assertEquals(3,Release060Rules.qualityTextPaddingLeftDp());
  assertEquals(15,Release060Rules.qualityTextPaddingRightDp());
  assertEquals(8,Release059Rules.qualityLeftInsetDp(28,
   Release060Rules.qualityTextPaddingLeftDp(),Release060Rules.qualityTextPaddingRightDp()));
  assertEquals(2,Release059Rules.qualityRightInsetDp(28,
   Release060Rules.qualityTextPaddingLeftDp(),Release060Rules.qualityTextPaddingRightDp()));
 }
 @Test public void bulkQualityDialogAddsExactlyThreeDpAboveDropdown(){
  assertEquals(9,Release060Rules.bulkDialogTopInsetDp(6));
 }
}
