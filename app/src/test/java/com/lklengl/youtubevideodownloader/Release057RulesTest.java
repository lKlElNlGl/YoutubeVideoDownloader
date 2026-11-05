package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class Release057RulesTest {
 @Test public void purposefulBackSwipeWorksAcrossFreeArea() {
  assertTrue(Release057Rules.backSwipe(270,75,0,60,0));
  assertFalse(Release057Rules.backSwipe(270,75,0,59,0));
  assertFalse(Release057Rules.backSwipe(270,75,0,99,88));
 }
 @Test public void filteringKeepsAnchorVideoOrClosestRemaining() {
  assertEquals(1,Release057Rules.anchorPosition(Arrays.asList(1,9,14),9));
  assertEquals(1,Release057Rules.anchorPosition(Arrays.asList(1,8,14),9));
  assertEquals(0,Release057Rules.anchorPosition(Arrays.asList(1,8),0));
  assertEquals(0,Release057Rules.anchorPosition(Collections.emptyList(),9));
 }
 @Test public void qualityLabelReservesTextAndArrow() {
  assertEquals(134,Release057Rules.qualityWidth(96,38,200));
  assertEquals(160,Release057Rules.qualityWidth(145,38,160));
 }
 @Test public void completedRefreshNeverNavigatesOrNotifiesOnCancel() {
  assertFalse(Release057Rules.shouldAutoOpenAfterRefresh());
  assertFalse(Release057Rules.shouldNotifyRefresh(PlaylistRefreshManager.Stage.CANCELLED));
  assertTrue(Release057Rules.shouldNotifyRefresh(PlaylistRefreshManager.Stage.DONE));
  assertTrue(Release057Rules.shouldNotifyRefresh(PlaylistRefreshManager.Stage.ERROR));
 }
}
