package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;
public class FormatChoicesTest {
 @Test public void detectsOnlyAvailableVideoAndAudioFormats(){
  FormatChoices c=FormatChoices.fromDetected("mp4,webm,m4a");
  assertEquals("mp4",c.values().get(0));
  assertTrue(c.values().contains("webm"));
  assertTrue(c.values().contains("m4a"));
  assertTrue(c.values().contains("mp3"));
  assertTrue(c.isAudio("m4a"));
 }
 @Test public void fallbackAlwaysWorks(){
  FormatChoices c=FormatChoices.fromDetected("");
  assertEquals(2,c.values().size());
  assertEquals("mp3",c.values().get(1));
 }
}
