package com.lklengl.youtubevideodownloader;
import java.util.Locale;
/** Actual media characteristics, not the selection label "Maximum". */
public final class QueueMetadata {
 private QueueMetadata(){}
 public static int parseDuration(String input){
  if(input==null||input.trim().isEmpty())return -1;
  String[] parts=input.trim().split(":");int sec=0;
  if(parts.length<2||parts.length>3)return -1;
  try{
   for(String part:parts){int n=Integer.parseInt(part);if(n<0||n>59&&sec>0)return -1;sec=sec*60+n;}
   return sec;
  }catch(NumberFormatException ex){return -1;}
 }
 public static int preferredHeight(String available,String chosen){
  if(available==null)return 0;
  int max=0,limit=Integer.MAX_VALUE;
  if(chosen!=null&&!"best".equals(chosen))
   try{limit=Integer.parseInt(chosen);}catch(NumberFormatException ignored){}
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d{3,4})p").matcher(available);
  while(m.find()){
   int n=Integer.parseInt(m.group(1));if(n<=limit&&n>max)max=n;
  }
  return max;
 }
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
