package com.lklengl.youtubevideodownloader;
import java.util.Locale;
/** Actual media characteristics, not the selection label "Maximum". */
public final class QueueMetadata {
 private QueueMetadata(){}
 public static String resolution(int height){
  if(height>=2160)return "4K";
  if(height>0)return height+"p";
  return "Определяется…";
 }
 public static String label(boolean audio,String quality,String format,int actualHeight,int durationSeconds){
  String leading=audio?"Аудио":resolution(actualHeight);
  String code=(format==null||format.isEmpty()?(audio?"mp3":"mp4"):format).toUpperCase(Locale.ROOT);
  String time=durationSeconds>=0? (durationSeconds>=3600?QueueRules.readableDuration(durationSeconds):
   String.format(Locale.ROOT,"%02d:%02d",durationSeconds/60,durationSeconds%60)):"—";
  return leading+" · "+code+" · "+time;
 }
}
