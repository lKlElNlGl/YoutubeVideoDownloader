package com.lklengl.youtubevideodownloader;
import java.util.List;
/** Pure policies for predictable playlist UI and background refresh behavior. */
public final class Release057Rules {
 private Release057Rules(){}
 /** Release as Back only for deliberate rightward gestures from free list space. */
 public static boolean backSwipe(float startX,float startY,float endX,float dx,float dy) {
  return dx>=60f && dx>Math.abs(dy)*1.5f;
 }
 public static int anchorPosition(List<Integer> visible,int originalVideo) {
  if(visible==null||visible.isEmpty())return 0;
  int exact=visible.indexOf(originalVideo);
  if(exact>=0)return exact;
  int closest=0,dist=Integer.MAX_VALUE;
  for(int i=0;i<visible.size();i++){
   int current=Math.abs(visible.get(i)-originalVideo);
   if(current<dist){dist=current;closest=i;}
  }
  return closest;
 }
 public static int qualityWidth(int textPixels,int fixedPixels,int availablePixels) {
  return Math.max(0,Math.min(textPixels+fixedPixels,availablePixels));
 }
 public static boolean shouldAutoOpenAfterRefresh(){return false;}
 public static boolean shouldNotifyRefresh(PlaylistRefreshManager.Stage stage){
  return stage==PlaylistRefreshManager.Stage.DONE||stage==PlaylistRefreshManager.Stage.ERROR;
 }
}
