package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class PlaylistQualityPolicyTest {
 @Test public void applyBulkQualityOnlyToSelectedVideos(){
  boolean[] chosen={true,false,true};
  List<String> current=Arrays.asList("720","480","best");
  assertEquals(Arrays.asList("1080","480","1080"),
   PlaylistQualityPolicy.applyToSelected(current,chosen,"1080"));
 }
 @Test public void retainIndividualQualityUntilAnotherBulkChange(){
  List<String> values=Arrays.asList("1080","720","1080");
  assertEquals("720",PlaylistQualityPolicy.qualityFor(values,1,"best"));
  assertEquals("best",PlaylistQualityPolicy.qualityFor(values,9,"best"));
  assertEquals(Arrays.asList("best","720","best"),
   PlaylistQualityPolicy.applyToSelected(values,new boolean[]{true,false,true},"best"));
 }
 @Test public void validateOnlyKnownQualities(){
  assertTrue(PlaylistQualityPolicy.allowed("best"));
  assertTrue(PlaylistQualityPolicy.allowed("2160"));
  assertTrue(PlaylistQualityPolicy.allowed("720"));
  assertFalse(PlaylistQualityPolicy.allowed("some script"));
 }
}