package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;
public class QueueMetadataParsingTest {
 @Test public void preservesVideoAndAudioDurations() {
  assertEquals(754,QueueMetadata.parseDuration("12:34"));
  assertEquals(278,QueueMetadata.parseDuration("4:38"));
  assertEquals(5143,QueueMetadata.parseDuration("1:25:43"));
  assertEquals(-1,QueueMetadata.parseDuration("—"));
 }
 @Test public void usesConcreteResolutionWithinRequestedLimit(){
  assertEquals(2160,QueueMetadata.preferredHeight("2160p, 1440p, 1080p","best"));
  assertEquals(1080,QueueMetadata.preferredHeight("2160p, 1080p","1080"));
  assertEquals(0,QueueMetadata.preferredHeight("720p","480"));
 }
 @Test public void neverShowsMaximumAsAFileProperty(){
  assertEquals("Определяется… · WEBM · —",QueueMetadata.label(false,"best","webm",0,-1));
 }
}
