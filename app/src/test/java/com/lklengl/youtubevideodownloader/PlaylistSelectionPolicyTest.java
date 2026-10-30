package com.lklengl.youtubevideodownloader;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaylistSelectionPolicyTest {
 @Test public void previouslyAddedEntriesAreUnselectedAndFreshOnesSelected(){
  List<String> urls=Arrays.asList("u1","u2","u3");
  Set<String> added=new HashSet<>(Arrays.asList("u1","u2"));
  assertEquals(Arrays.asList(false,false,true),PlaylistSelectionPolicy.initial(urls,added));
 }
 @Test public void explicitUnselectedItemsStayUnselectedAcrossReopening(){
  List<Boolean> current=Arrays.asList(false,true,false);
  Set<String> added=new HashSet<>(Arrays.asList("u2"));
  assertEquals(Arrays.asList(false,false,false),PlaylistSelectionPolicy.resume(current,
      Arrays.asList("u1","u2","u3"),added));
 }
 @Test public void timeAndQualityAreConcreteAndDurationUnknownIsDash(){
  assertEquals("4K · MP4 · 12:34",QueueMetadata.label(false,"best","mp4",2160,754));
  assertEquals("Аудио · MP3 · 04:38",QueueMetadata.label(true,"best","mp3",0,278));
  assertEquals("Определяется… · MP4 · —",QueueMetadata.label(false,"best","mp4",0,-1));
 }
}
