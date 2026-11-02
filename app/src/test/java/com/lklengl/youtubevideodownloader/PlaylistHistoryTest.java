package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaylistHistoryTest {
 @Test public void queuedEntryUsesYellowStatusUntilFileExists() {
  assertEquals(PlaylistHistory.QUEUED, PlaylistHistory.classify(true,false));
  assertEquals(PlaylistHistory.DOWNLOADED, PlaylistHistory.classify(true,true));
  assertEquals(PlaylistHistory.NEW, PlaylistHistory.classify(false,false));
 }
 @Test public void completedFileHasPriorityOverQueueHistory() {
  assertEquals(PlaylistHistory.DOWNLOADED, PlaylistHistory.classify(false,true));
 }
}