package com.lklengl.youtubevideodownloader;
/** Keep the job progress monotonic even when yt-dlp starts a new media stream. */
public final class ProgressPolicy {
 private ProgressPolicy(){}
 public static int displayPercent(int previous,int streamPercent){
  return Math.min(99,Math.max(Math.max(0,previous),Math.max(0,streamPercent)));
 }
 public static int completed(){return 100;}
}