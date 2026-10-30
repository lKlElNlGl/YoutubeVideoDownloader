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
   root.put("url",url);root.put("title",p.title);root.put("reportedCount",p.reportedCount);
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
   return new PlaylistRepository.Playlist(root.optString("title"),entries,root.optInt("reportedCount",entries.size()));
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
 public static void saveSelection(Context c,String url,boolean[] flags){
  JSONArray a=new JSONArray();for(boolean b:flags)a.put(b);
  c.getSharedPreferences("playlist_choices",0).edit().putString(key(url),a.toString()).apply();
 }
}
