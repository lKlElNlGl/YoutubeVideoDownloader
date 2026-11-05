package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class Release055BehaviorTest {
 @Test public void brandNewPlaylistStartsFullyUnchecked() {
  List<Boolean> flags=PlaylistSelectionPolicy.initial(Arrays.asList("a","b","c"),new HashSet<>(Arrays.asList("b")));
  assertEquals(Arrays.asList(false,false,false), flags);
 }
 @Test public void newPlaylistIdsWithoutPriorSelectionStartUnchecked() {
  List<Boolean> flags=PlaylistSelectionPolicy.byId(Arrays.asList("1","2"),Collections.emptyMap(),Collections.singleton("1"));
  assertEquals(Arrays.asList(false,false),flags);
 }
 @Test public void playlistQualityNeverOffersUnder360() {
  assertFalse(Arrays.asList(PlaylistQualityPolicy.VALUES).contains("240"));
  assertFalse(Arrays.asList(PlaylistQualityPolicy.VALUES).contains("144"));
  assertEquals("360",PlaylistQualityPolicy.VALUES[PlaylistQualityPolicy.VALUES.length-1]);
 }
 @Test public void aSecondStreamStartsProgressFromZeroButOverallNeverFalls() {
  DownloadProgressTracker t=new DownloadProgressTracker(false);
  DownloadProgressTracker.Status a=t.observe(85,"[download] 85.0% of 20.00MiB at 2.00MiB/s");
  DownloadProgressTracker.Status b=t.observe(99,"[download] 100.0% of 20.00MiB at 2.00MiB/s");
  DownloadProgressTracker.Status c=t.observe(32,"[download] 32.0% of 2.00MiB at 0.2MiB/s");
  assertEquals("Аудио",c.streamLabel);
  assertEquals(32,c.streamPercent);
  assertTrue(c.totalPercent>=b.totalPercent);
  assertTrue(a.totalPercent<=b.totalPercent);
  assertTrue(c.totalPercent<100);
 }
}
