package com.lklengl.youtubevideodownloader;
import java.util.*;

/** Stable, screen-independent selection and layout rules for v0.5.12. */
public final class Release062Rules {
 private Release062Rules(){}
 private static final String[] VIDEO_ORDER={"best","2160","1440","1080","720","480","360"};
 private static final String[] AUDIO_ORDER={"a:best","a:320","a:256","a:192","a:128"};

 /** Video first, audio second; a prior manual ceiling remains selectable after discovery.
  * An explicit audio-only/video-only probe removes the unavailable group altogether. */
 public static List<String> individualChoices(List<String> discovered,String format,String quality){
  boolean audioOnly=discovered!=null&&discovered.contains("audio-only");
  boolean videoOnly=discovered!=null&&discovered.contains("video-only");
  ArrayList<String> result=new ArrayList<>();
  if(!audioOnly){
   Set<String> heights=new HashSet<>();
   if(discovered==null)heights.addAll(Arrays.asList(VIDEO_ORDER));
   else heights.addAll(discovered);
   heights.add("best");
   if("mp4".equals(format)&&PlaylistQualityPolicy.allowed(quality))heights.add(quality);
   for(String q:VIDEO_ORDER)if(heights.contains(q))result.add("mp4|"+q);
  }
  if(!videoOnly)for(String q:AUDIO_ORDER)result.add("mp3|"+q);
  return result;
 }
 /** Index in selectable choices where the non-selectable section divider is inserted. */
 public static int dividerIndex(List<String> codes){
  if(codes==null||codes.isEmpty())return -1;
  int index=-1;
  for(int i=0;i<codes.size();i++){
   if(codes.get(i).startsWith("mp3|")){index=i;break;}
  }
  return index>0&&index<codes.size() ? index:-1;
 }
 public static int popupIndexToChoice(int popup,int divider){
  return divider<0?popup:popup==divider?-1:popup>divider?popup-1:popup;
 }
 public static int choiceIndexToPopup(int choice,int divider){
  return divider>=0&&choice>=divider?choice+1:choice;
 }
 public static String fullChoice(String code,boolean english){
  int sep=code==null?-1:code.indexOf('|');
  if(sep<0)return "";
  String format=code.substring(0,sep);
  String quality=code.substring(sep+1);
  return (english?("mp3".equals(format)?"Audio MP3":"Video MP4"):
    ("mp3".equals(format)?"Аудио MP3":"Видео MP4"))+" · "+
    PlaylistMediaPolicy.full(format,quality,english);
 }
 /** Align the list-card paint with the visual stretch of the ListView's header.
  * Android's overscroll scrollY is negative when the top is pulled down. */
 public static int stretchedCardTop(int firstPosition,int firstChildTop,int heroHeight,int overScrollY){
  int normal=Release060Rules.cardTop(firstPosition,firstChildTop,heroHeight);
  return Math.max(0,normal-Math.min(0,overScrollY));
 }
 public static String qualityProgress(int count,int total,boolean english){
  return (english?"Checked\nqualities:\n":"Определено\nкачеств:\n")+
    count+(english?" of ":" из ")+total;
 }
 public static String priorityLabel(int count,boolean english){
  return (english?"Download immediately (":"Скачать без очереди (")+count+")";
 }
}
