package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;
public class Release056RulesTest {
 @Test public void stripsAndroidStoragePrefixOnlyFromDisplay() {
  assertEquals("Music/Recordings", Release056Rules.folderDisplay("primary:Music/Recordings"));
  assertEquals("Download", Release056Rules.folderDisplay("primary:Download"));
  assertEquals("Папка", Release056Rules.folderDisplay("Папка"));
 }
 @Test public void arrowsAreTriggeredByBoundaryContactNotByScrollOffsetAlone() {
  assertFalse(Release056Rules.showUp(true,101,100));
  assertTrue(Release056Rules.showUp(true,100,100));
  assertFalse(Release056Rules.showUp(false,99,100));
  assertFalse(Release056Rules.showDown(true,599,600));
  assertTrue(Release056Rules.showDown(true,600,600));
  assertFalse(Release056Rules.showDown(false,601,600));
 }
 @Test public void qualityWidthIncludesRealTextAndArrowWithoutExceedingSpace() {
  assertEquals(90, Release056Rules.qualityWidth(62,28,100));
  assertEquals(100, Release056Rules.qualityWidth(90,28,100));
 }
}
