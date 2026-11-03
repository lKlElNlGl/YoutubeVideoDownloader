package com.lklengl.youtubevideodownloader;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
/** Keeps each playlist's subfolder stable across renames and later downloads. */
public final class PlaylistFolders {
 private static final String PREFS="playlist_storage",MAP="playlist_directory_map";
 private PlaylistFolders(){}
 public static void initialize(Context context){
  SharedPreferences prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
  if(prefs.contains("enabled"))return;
  boolean freshInstall=false;
  try{
   PackageInfo info=context.getPackageManager().getPackageInfo(context.getPackageName(),0);
   freshInstall=info.firstInstallTime==info.lastUpdateTime;
  }catch(Exception ignored){}
  prefs.edit().putBoolean("enabled",freshInstall).apply();
 }
 public static boolean enabled(Context context){
  return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean("enabled",false);
 }
 public static void setEnabled(Context context,boolean enabled){
  context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean("enabled",enabled).apply();
 }
 public static synchronized String folderFor(Context context,String url,String title){
  if(!enabled(context)||url==null||url.isEmpty())return "";
  SharedPreferences map=context.getSharedPreferences(MAP,Context.MODE_PRIVATE);
  String key=key(url),saved=map.getString(key,"");
  if(!saved.isEmpty())return saved;
  List<String> allocated=new ArrayList<>();
  for(Object value:map.getAll().values())if(value instanceof String)allocated.add((String)value);
  String name=PlaylistFolderRules.uniqueName(title,allocated);
  if(!map.edit().putString(key,name).commit())
   throw new IllegalStateException("Не удалось сохранить имя папки плейлиста");
  return name;
 }
 private static String key(String url){
  try{
   byte[] hash=MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
   StringBuilder result=new StringBuilder("p_");
   for(int i=0;i<16;i++)result.append(String.format(Locale.ROOT,"%02x",hash[i]&255));
   return result.toString();
  }catch(Exception ex){throw new IllegalStateException(ex);}
 }
}
