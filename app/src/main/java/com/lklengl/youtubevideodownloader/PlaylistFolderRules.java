package com.lklengl.youtubevideodownloader;
import java.util.Collection;
/** Portable human-readable child directory names under the selected downloads folder. */
public final class PlaylistFolderRules {
 private PlaylistFolderRules(){}
 public static String safeName(String input){
  String name=(input==null?"":input).replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]"," ")
    .replaceAll("\\s+"," ").trim();
  while(name.endsWith(".")||name.endsWith(" "))name=name.substring(0,name.length()-1);
  if(name.length()>70){name=name.substring(0,70).trim();
   while(name.endsWith("."))name=name.substring(0,name.length()-1);}
  return name.isEmpty()?"Плейлист":name;
 }
 public static String uniqueName(String requested,Collection<String> allocated){
  String base=safeName(requested),candidate=base;int suffix=2;
  while(containsIgnoreCase(allocated,candidate)){candidate=base+" ("+(suffix++)+")";}
  return candidate;
 }
 private static boolean containsIgnoreCase(Collection<String> existing,String wanted){
  for(String name:existing)if(name.equalsIgnoreCase(wanted))return true;
  return false;
 }
}
