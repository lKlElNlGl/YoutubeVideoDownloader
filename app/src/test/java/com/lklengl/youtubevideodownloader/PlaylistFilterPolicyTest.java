package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class PlaylistFilterPolicyTest {
 @Test public void selectedFirstPreservesSourceOrderWithinGroups() {
  assertEquals(Arrays.asList(1,3,0,2), PlaylistFilterPolicy.visible(
   new boolean[]{false,true,false,true},new int[]{0,0,0,0},0,0,0,true));
 }
 @Test public void selectionFilterExcludesNewlySelectedItemsWithoutChangingSavedFlags() {
  boolean[] chosen={false,true,false};
  assertEquals(Arrays.asList(0,2), PlaylistFilterPolicy.visible(
   chosen,new int[]{0,1,2},2,0,0,false));
  chosen[0]=true;
  assertEquals(Arrays.asList(2), PlaylistFilterPolicy.visible(
   chosen,new int[]{0,1,2},2,0,0,false));
 }
 @Test public void downloadAndQueueFiltersAreIndependent() {
  assertEquals(Arrays.asList(1), PlaylistFilterPolicy.visible(
   new boolean[]{false,false,false},new int[]{0,1,2},0,2,1,false));
  assertEquals(Arrays.asList(2), PlaylistFilterPolicy.visible(
   new boolean[]{false,false,false},new int[]{0,1,2},0,1,0,false));
 }
}