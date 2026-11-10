package com.lklengl.youtubevideodownloader;

/** v0.5.11 exact compact-control geometry; all distances are dp unless noted. */
public final class Release061Rules {
 private Release061Rules(){}
 public static int individualLeftInsetDp(){return 9;}
 public static int individualRightInsetDp(){return 2;}
 public static int individualExtraWidthDp(){return 29;}
 public static int individualTextPaddingLeftDp(){return 4;}
 public static int individualTextPaddingRightDp(){return 14;}

 /** A scrollbar is permitted only when the playlist controls are pinned. */
 public static boolean showScrollbar(int y,boolean canScrollUp,boolean controlsPinned){
  return y<=0 && canScrollUp && controlsPinned;
 }
 public static int scrollbarTopPx(int controlsHeightPx,int gapPx){
  return Math.max(0,controlsHeightPx+gapPx);
 }
}
