package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class PlaylistSelectionIdsTest {
 @Test public void selectionsFollowIdsAfterReorderAndNewVideos() {
  Map<String,Boolean> saved=new HashMap<>();saved.put("A",true);saved.put("B",false);
  assertEquals(Arrays.asList(false,true,false),
   PlaylistSelectionPolicy.byId(Arrays.asList("B","A","C"),saved,Collections.emptySet()));
 }
 @Test public void explicitlySelectedQueuedVideoStaysSelected(){
  Map<String,Boolean> saved=Collections.singletonMap("A",true);
  assertEquals(Arrays.asList(true),
   PlaylistSelectionPolicy.byId(Arrays.asList("A"),saved,Collections.singleton("A")));
 }
}