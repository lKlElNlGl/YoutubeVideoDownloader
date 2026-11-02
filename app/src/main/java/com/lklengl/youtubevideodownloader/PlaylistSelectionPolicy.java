package com.lklengl.youtubevideodownloader;
import java.util.*;
/** Pure selection rules: already-added videos remain visible but start unchecked. */
public final class PlaylistSelectionPolicy {
 private PlaylistSelectionPolicy(){}
 /** New videos are initially selected unless previously queued. Existing choices stay tied to IDs. */
 public static List<Boolean> byId(List<String> ids,Map<String,Boolean> prior,Set<String> added){
  List<Boolean> result=new ArrayList<>();
  for(String id:ids)result.add(prior.containsKey(id)?prior.get(id):!added.contains(id));
  return result;
 }
 public static List<Boolean> initial(List<String> links,Set<String> added){
  List<Boolean> result=new ArrayList<>();
  for(String url:links)result.add(!added.contains(url));
  return result;
 }
 public static List<Boolean> resume(List<Boolean> previous,List<String> urls,Set<String> added){
  List<Boolean> result=new ArrayList<>();
  for(int i=0;i<urls.size();i++)result.add((i<previous.size()?previous.get(i):!added.contains(urls.get(i))));
  return result;
 }
}
