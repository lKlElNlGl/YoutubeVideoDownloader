package com.lklengl.youtubevideodownloader;

/** Approved original playlist card dimensions with v0.5.10 sticky scrolling. */
public final class Release060Rules {
 private Release060Rules(){}

 /** The card follows the entire playlist header, then pins beneath the fixed toolbar. */
 public static int cardTop(int firstVisiblePosition,int firstChildTop,int headerHeight) {
  if(firstVisiblePosition>0)return 0;
  return Math.max(0,firstChildTop+Math.max(0,headerHeight));
 }
 public static int playlistCardRadiusDp(){return 14;}
 public static int playlistCardTopInsetDp(){return 8;}

 /** Current closed-state text is centered in a field with a fixed 18dp arrow zone. */
 public static int qualityTextPaddingLeftDp(){return 3;}
 public static int qualityTextPaddingRightDp(){return 15;}

 /** Add 3dp to the existing dialog space; do not alter the button area below. */
 public static int bulkDialogTopInsetDp(int existingDp){return existingDp+3;}
}
