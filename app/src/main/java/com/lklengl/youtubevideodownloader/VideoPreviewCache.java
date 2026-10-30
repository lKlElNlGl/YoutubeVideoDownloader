package com.lklengl.youtubevideodownloader;
import android.content.Context;
import org.json.JSONObject;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
/** Bounded local metadata cache: completed records survive app restarts and tab navigation. */
public final class VideoPreviewCache {
 private VideoPreviewCache(){}
 private static String key(String url){
  try{
   byte[] hash=MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
   StringBuilder b=new StringBuilder("video_");
   for(int i=0;i<12;i++)b.append(String.format(Locale.ROOT,"%02x",hash[i]&255));
   return b.toString();
  }catch(Exception ex){throw new IllegalStateException(ex);}
 }
 public static PreviewSnapshot load(Context c,String source){
  try{
   String value=c.getSharedPreferences("preview_cache",0).getString(key(source),null);
   if(value==null)return null;
   JSONObject o=new JSONObject(value);
   if(!source.equals(o.optString("source")))return null;
   return PreviewSnapshot.video(source,o.optString("title"),o.optString("duration"),
    o.optString("resolutions"),o.optString("formats"));
  }catch(Exception ignored){return null;}
 }
 public static void save(Context c,PreviewSnapshot p){
  if(p==null||p.playlist)return;
  try{
   JSONObject o=new JSONObject();
   o.put("source",p.source);o.put("title",p.title);o.put("duration",p.duration);
   o.put("resolutions",p.resolutions);o.put("formats",p.formats);
   android.content.SharedPreferences prefs=c.getSharedPreferences("preview_cache",0);
   java.util.Map<String,?> all=prefs.getAll();
   android.content.SharedPreferences.Editor editor=prefs.edit();
   if(all.size()>=30){
    for(String k:all.keySet()){editor.remove(k);break;}
   }
   editor.putString(key(p.source),o.toString()).apply();
  }catch(Exception ignored){}
 }
 public static void clear(Context c,String source){
  c.getSharedPreferences("preview_cache",0).edit().remove(key(source)).apply();
 }
}
