package com.lklengl.youtubevideodownloader;
import java.util.*;
/** Filters are display-only: selected flags and per-video qualities remain keyed to source indices. */
public final class PlaylistFilterPolicy {
 private PlaylistFilterPolicy(){}
 // selection: 0 all, 1 selected, 2 unselected.
 // downloaded: 0 all, 1 downloaded, 2 not downloaded.
 // queue: 0 all, 1 previously queued, 2 never queued.
 public static List<Integer> visible(boolean[] selected,int[] statuses,
  int selection,int downloaded,int queued,boolean selectedFirst){
  if(selected.length!=statuses.length)throw new IllegalArgumentException("Video count mismatch");
  List<Integer> result=new ArrayList<>();
  for(int i=0;i<selected.length;i++){
   if(selection==1&&!selected[i]||selection==2&&selected[i])continue;
   boolean done=statuses[i]==PlaylistHistory.DOWNLOADED;
   boolean added=statuses[i]!=PlaylistHistory.NEW;
   if(downloaded==1&&!done||downloaded==2&&done)continue;
   if(queued==1&&!added||queued==2&&added)continue;
   result.add(i);
  }
  if(selectedFirst)result.sort((a,b)->Boolean.compare(selected[b],selected[a]));
  return result;
 }
}