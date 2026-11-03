package com.lklengl.youtubevideodownloader;
/** What is shown next to a playlist item depends on its file/queue state. */
public final class PlaylistQualityText {
 private PlaylistQualityText(){}
 public static String label(int status,String savedQuality,int actualHeight,String currentChoice) {
  if(status==PlaylistHistory.DOWNLOADED)
   return actualHeight>0?QueueMetadata.resolution(actualHeight):"Качество неизвестно";
  String choice=status==PlaylistHistory.QUEUED?savedQuality:currentChoice;
  return PlaylistQualityPolicy.LABELS[PlaylistQualityPolicy.index(choice)];
 }
}