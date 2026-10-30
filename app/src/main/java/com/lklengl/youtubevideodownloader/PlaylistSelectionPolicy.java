package com.lklengl.youtubevideodownloader;
import java.util.*;
/** Pure selection rules: already-added videos remain visible but start unchecked. */
public final class PlaylistSelectionPolicy {
 private PlaylistSelectionPolicy(){}
 public static List<Boolean> initial(List<String> links,Set<String> added){
  List<Boolean> result=new ArrayList<>();
  for(String url:links)result.add(!added.contains(url));
  return result;
 }
 public static List<Boolean> resume(List<Boolean> previous,List<String> urls,Set<String> added){
  List<Boolean> result=new ArrayList<>();
  for(int i=0;i<urls.size();i++)result.add((i<previous.size()?previous.get(i):true)&&!added.contains(urls.get(i)));
  return result;
 }
}
