package com.lklengl.youtubevideodownloader;

import java.util.*;

/** One independently selected output format and quality per playlist entry. */
public final class PlaylistMediaPolicy {
 private PlaylistMediaPolicy(){}
 public static final String VIDEO="mp4", AUDIO="mp3";
 public static final String[] AUDIO_VALUES={"a:best","a:128","a:192","a:256","a:320"};
 public static final String[] AUDIO_LABELS={"Максимальное","128 кбит/с","192 кбит/с","256 кбит/с","320 кбит/с"};

 public static String defaultQuality(String format){return AUDIO.equals(format)?"a:best":"best";}
 public static boolean allowed(String format,String quality){
  if(AUDIO.equals(format))return Arrays.asList(AUDIO_VALUES).contains(quality);
  return VIDEO.equals(format)&&PlaylistQualityPolicy.allowed(quality);
 }
 public static boolean allowedAny(String quality){
  return PlaylistQualityPolicy.allowed(quality)||Arrays.asList(AUDIO_VALUES).contains(quality);
 }
 public static String[] choices(String format,List<String> resolved){
  if(AUDIO.equals(format))return AUDIO_VALUES.clone();
  if(resolved==null||resolved.isEmpty())return PlaylistQualityPolicy.VALUES.clone();
  LinkedHashSet<String> result=new LinkedHashSet<>(resolved);result.add("best");
  ArrayList<String> order=new ArrayList<>();order.add("best");
  for(String value:PlaylistQualityPolicy.VALUES)
   if(!"best".equals(value)&&result.contains(value))order.add(value);
  return order.toArray(new String[0]);
 }
 public static String full(String format,String quality,boolean english){
  if(AUDIO.equals(format)){
   if("a:best".equals(quality))return english?"Maximum":"Максимальное";
   String number=quality!=null&&quality.startsWith("a:")?quality.substring(2):"192";
   return number+(english?" kbps":" кбит/с");
  }
  if("best".equals(quality))return english?"Maximum":"Максимальное";
  return "2160".equals(quality)?"4K · 2160p":quality+"p";
 }
 public static String compact(String format,String quality,boolean english){
  String prefix=AUDIO.equals(format)?"MP3":"MP4";
  if("best".equals(quality)||"a:best".equals(quality))
   return prefix+(english?" · Max":" · Макс.");
  if(AUDIO.equals(format))
   return "MP3 · "+(quality!=null&&quality.startsWith("a:")?quality.substring(2):"192")+"k";
  return "MP4 · "+quality+"p";
 }
 /** Quality is an upper bound; never enlarge a video beyond the user's chosen limit. */
 public static String effectiveVideo(String requested,Collection<String> available){
  if("best".equals(requested)||available==null||available.isEmpty())return requested;
  int ceiling;
  try{ceiling=Integer.parseInt(requested);}catch(Exception e){return "best";}
  int largest=-1;
  for(String s:available){
   try{int h=Integer.parseInt(s);if(h<=ceiling&&h>largest)largest=h;}
   catch(Exception ignored){}
  }
  // If there is no stream at or below the requested limit, preserve the limit.
  // yt-dlp will report unavailable rather than silently upscale.
  return largest>0?Integer.toString(largest):requested;
 }
 /**
  * MP3 is less efficient than Opus/AAC. Estimate an output ceiling based on the
  * source's *encoding bitrate*, not a promise of equivalent acoustic quality.
  * Never exceed the user's requested bitrate; unknown sources cap at 192 kbps.
  */
 public static int mp3Kbps(String choice,int sourceKbps,String codec){
  int ceiling=320;
  if(choice!=null&&choice.startsWith("a:")&&!"a:best".equals(choice)){
   try{ceiling=Integer.parseInt(choice.substring(2));}
   catch(Exception ignored){ceiling=192;}
  }
  int useful=192;
  if(sourceKbps>0){
   String format=codec==null?"":codec.toLowerCase(Locale.ROOT);
   double multiplier=format.contains("opus")?1.35:format.contains("aac")||format.contains("mp4a")?1.25:1.0;
   double equivalent=sourceKbps*multiplier;
   useful=equivalent<=128?128:equivalent<=192?192:equivalent<=256?256:320;
  }
  return Math.min(Math.max(128,ceiling),useful);
 }
}
