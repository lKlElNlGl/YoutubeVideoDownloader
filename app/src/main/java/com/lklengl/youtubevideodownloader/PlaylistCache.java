package com.lklengl.youtubevideodownloader;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
/** Durable metadata and selection for previously fetched playlists. */
public final class PlaylistCache {
 private PlaylistCache(){}
 private static String key(String url){
  try{
   byte[] bytes=MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
   StringBuilder b=new StringBuilder();
   for(byte x:bytes)b.append(String.format(Locale.ROOT,"%02x",x&0xff));
   return b.substring(0,32);
  }catch(Exception ex){throw new IllegalStateException(ex);}
 }
 private static File file(Context c,String url){
  File directory=new File(c.getFilesDir(),"playlists");
  if(!directory.isDirectory())directory.mkdirs();
  return new File(directory,key(url)+".json");
 }
 public static void clear(Context c,String url){
  if(url!=null&&!url.isEmpty()){file(c,url).delete();c.getSharedPreferences("playlist_choices",0).edit().remove(key(url)).apply();}
 }
 public static boolean has(Context c,String url){return url!=null&&!url.isEmpty()&&file(c,url).isFile();}
 public static void save(Context c,String url,PlaylistRepository.Playlist p){
  try{
   JSONObject root=new JSONObject();
   root.put("url",url);root.put("title",p.title);root.put("uploader",p.uploader);root.put("reportedCount",p.reportedCount);
   JSONArray array=new JSONArray();
   for(PlaylistRepository.Entry e:p.entries){
    JSONObject entry=new JSONObject();entry.put("id",e.id);entry.put("title",e.title);
    entry.put("duration",e.duration);array.put(entry);
   }
   root.put("entries",array);
   File dest=file(c,url);File temp=new File(dest.getAbsolutePath()+".tmp");
   try(FileOutputStream out=new FileOutputStream(temp)){
    out.write(root.toString().getBytes(StandardCharsets.UTF_8));out.getFD().sync();
   }
   if(!temp.renameTo(dest)){
    if(dest.exists())dest.delete();
    if(!temp.renameTo(dest))throw new IOException("Playlist save failed");
   }
  }catch(Exception ex){throw new IllegalStateException("Не удалось сохранить плейлист",ex);}
 }
 public static PlaylistRepository.Playlist load(Context c,String url){
  File f=file(c,url);if(!f.isFile()||f.length()>8_000_000)return null;
  try(FileInputStream stream=new FileInputStream(f);ByteArrayOutputStream buffer=new ByteArrayOutputStream()){
   byte[] b=new byte[8192];int n;while((n=stream.read(b))!=-1)buffer.write(b,0,n);
   JSONObject root=new JSONObject(buffer.toString("UTF-8"));
   if(!url.equals(root.optString("url")))return null;
   JSONArray a=root.getJSONArray("entries");List<PlaylistRepository.Entry> entries=new ArrayList<>();
   for(int i=0;i<a.length();i++){
    JSONObject j=a.getJSONObject(i);
    entries.add(new PlaylistRepository.Entry(j.getString("id"),j.optString("title"),j.optInt("duration",-1)));
   }
   return new PlaylistRepository.Playlist(root.optString("title"),root.optString("uploader"),entries,root.optInt("reportedCount",entries.size()));
  }catch(Exception e){return null;}
 }
 public static List<Boolean> readSelection(Context c,String url){
  String value=c.getSharedPreferences("playlist_choices",0).getString(key(url),null);
  if(value==null)return null;
  try{
   JSONArray a=new JSONArray(value);List<Boolean> selected=new ArrayList<>();
   for(int i=0;i<a.length();i++)selected.add(a.getBoolean(i));
   return selected;
  }catch(Exception ignored){return null;}
 }
 public static List<String> readQualities(Context c,String url,int count,String fallback){
  String stored=c.getSharedPreferences("playlist_quality",0).getString(key(url),null);
  List<String> result=new ArrayList<>();
  try{
   JSONArray a=stored==null?null:new JSONArray(stored);
   for(int i=0;i<count;i++){
    String v=a!=null&&i<a.length()?a.optString(i,fallback):fallback;
    result.add(PlaylistQualityPolicy.allowed(v)?v:fallback);
   }
  }catch(Exception ex){for(int i=result.size();i<count;i++)result.add(fallback);}
  return result;
 }
 public static void saveQualities(Context c,String url,List<String> qualities){
  JSONArray a=new JSONArray();
  for(String value:qualities)a.put(PlaylistQualityPolicy.allowed(value)?value:"best");
  c.getSharedPreferences("playlist_quality",0).edit().putString(key(url),a.toString()).apply();
 }
 public static List<String> readQualities(Context c,String url,PlaylistRepository.Playlist playlist,String fallback){
  String saved=c.getSharedPreferences("playlist_quality_by_id",0).getString(key(url),null);
  JSONObject obj=null;
  try{if(saved!=null)obj=new JSONObject(saved);}catch(Exception ignored){}
  List<String> result=new ArrayList<>();
  for(PlaylistRepository.Entry entry:playlist.entries){
   String value=obj==null?fallback:obj.optString(entry.id,fallback);
   result.add(PlaylistQualityPolicy.allowed(value)?value:fallback);
  }
  return result;
 }
 public static void saveQualities(Context c,String url,PlaylistRepository.Playlist playlist,List<String> qualities){
  if(qualities.size()!=playlist.entries.size())throw new IllegalArgumentException("Изменился список видео");
  JSONObject saved=new JSONObject();
  try{
   for(int i=0;i<qualities.size();i++){
    String v=qualities.get(i);
    if(PlaylistQualityPolicy.allowed(v))saved.put(playlist.entries.get(i).id,v);
   }
   c.getSharedPreferences("playlist_quality_by_id",0).edit()
    .putString(key(url),saved.toString()).apply();
  }catch(Exception ex){throw new IllegalStateException("Невозможно сохранить качество",ex);}
 }
 public static void saveSelection(Context c,String url,boolean[] flags){
  JSONArray a=new JSONArray();for(boolean b:flags)a.put(b);
  c.getSharedPreferences("playlist_choices",0).edit().putString(key(url),a.toString()).apply();
 }
}
