package com.lklengl.youtubevideodownloader;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/** Lightweight permanent playlist membership: queue entry removal never erases history. */
public final class PlaylistHistory {
 public static final int NEW=0, QUEUED=1, DOWNLOADED=2;
 private PlaylistHistory(){}
 private static SharedPreferences prefs(Context c){return c.getSharedPreferences("playlist_history",0);}
 private static String key(String url){
  try {
   byte[] hash=MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
   StringBuilder b=new StringBuilder("v_");
   for(byte val:hash)b.append(String.format(java.util.Locale.ROOT,"%02x",val&255));
   return b.toString();
  }catch(Exception e){throw new IllegalStateException(e);}
 }
 public static int classify(boolean added,boolean fileExists){
  return fileExists?DOWNLOADED:added?QUEUED:NEW;
 }
 public static void markQueued(Context c,String url){
  if(url!=null&&!url.isEmpty()&&!prefs(c).contains(key(url)))
   prefs(c).edit().putString(key(url),"queued").apply();
 }
 public static void markDownloaded(Context c,String url,String contentUri){
  if(url!=null&&!url.isEmpty()&&contentUri!=null&&!contentUri.isEmpty())
   prefs(c).edit().putString(key(url),contentUri).apply();
 }
 public static void syncFromQueue(Context c){
  List<DownloadQueue.Item> queue=DownloadQueue.all(c);
  SharedPreferences values=prefs(c);
  SharedPreferences.Editor changes=values.edit();
  boolean dirty=false;
  for(DownloadQueue.Item item:queue){
   if(item.url==null||item.url.isEmpty())continue;
   String key=key(item.url),old=values.getString(key,null);
   String wanted=DownloadQueue.DONE.equals(item.status)&&item.contentUri!=null&&
    !item.contentUri.isEmpty()?item.contentUri:old==null?"queued":old;
   if(!wanted.equals(old)){changes.putString(key,wanted);dirty=true;}
  }
  if(dirty)changes.apply();
 }
 public static boolean hasAdded(Context c,String url){return prefs(c).contains(key(url));}
 public static int status(Context c,String url){
  String value=prefs(c).getString(key(url),null);
  if(value==null)return NEW;
  if("queued".equals(value))return QUEUED;
  try(ParcelFileDescriptor descriptor=c.getContentResolver().openFileDescriptor(Uri.parse(value),"r")){
   if(descriptor!=null)return DOWNLOADED;
  }catch(Exception ignored){}
  return QUEUED;
 }
 public static void markRemovedFile(Context c,String url){markQueued(c,url);
  if(url!=null&&!url.isEmpty())prefs(c).edit().putString(key(url),"queued").apply();
 }
}
