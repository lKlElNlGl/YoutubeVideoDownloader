package com.lklengl.youtubevideodownloader;

/** Pure geometry and contrast rules for the v0.5.9 playlist layout. */
public final class Release059Rules {
 private Release059Rules(){}

 /** Until the playlist cover leaves the viewport, controls follow it normally. */
 public static int stickyControlsY(int headerTop,int heroHeight){
  return Math.max(0,headerTop+heroHeight);
 }

 /** 31% in dark theme; 60% in light theme, preserving other palette values. */
 public static int highlightColor(int base,int tint,boolean dark){
  float strength=dark?.31f:.60f;
  int r=Math.round(((base>>16)&255)*(1f-strength)+((tint>>16)&255)*strength);
  int g=Math.round(((base>>8)&255)*(1f-strength)+((tint>>8)&255)*strength);
  int b=Math.round((base&255)*(1f-strength)+(tint&255)*strength);
  return 0xFF000000|(r<<16)|(g<<8)|b;
 }

 /** Effective inset for centrally aligned closed-state text. */
 public static int qualityLeftInsetDp(int excessWidthDp,int leftPaddingDp,int rightPaddingDp){
  return (excessWidthDp+leftPaddingDp-rightPaddingDp)/2;
 }
 public static int qualityRightInsetDp(int excessWidthDp,int leftPaddingDp,int rightPaddingDp){
  return excessWidthDp-18-qualityLeftInsetDp(excessWidthDp,leftPaddingDp,rightPaddingDp);
 }
}
