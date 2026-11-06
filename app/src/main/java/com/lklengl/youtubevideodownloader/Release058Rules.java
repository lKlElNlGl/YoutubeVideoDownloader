package com.lklengl.youtubevideodownloader;
import java.util.*;
/** Deterministic policies for lazy per-video quality and brief refresh feedback. */
public final class Release058Rules {
 private Release058Rules(){}
 public static List<String> fallback(){return Arrays.asList(PlaylistQualityPolicy.VALUES);}
 public static List<String> available(Collection<Integer> heights){
  Set<Integer> present=new HashSet<>(heights);
  ArrayList<String> values=new ArrayList<>();values.add("best");
  for(int i=1;i<PlaylistQualityPolicy.VALUES.length;i++)
   if(present.contains(Integer.parseInt(PlaylistQualityPolicy.VALUES[i])))
    values.add(PlaylistQualityPolicy.VALUES[i]);
  return values;
 }
 public static boolean canSelect(boolean cached,boolean done,boolean failed){
  return cached||done||failed;
 }
 /** A brief upward tap moves one screen unless already at the list's top;
  * holding the arrow returns to the beginning, including the playlist cover. */
 public static boolean playlistArrowShouldJumpToTop(boolean held,boolean atListTop){
  return held||atListTop;
 }
 public static boolean showCancel(long cancelledAt,long now){
  return cancelledAt>0&&now>=cancelledAt&&now-cancelledAt<15000;
 }
 public static boolean hasActiveFilters(int selection,int downloaded,int queued,boolean selectedFirst){
  return selection!=0||downloaded!=0||queued!=0||selectedFirst;
 }
}
