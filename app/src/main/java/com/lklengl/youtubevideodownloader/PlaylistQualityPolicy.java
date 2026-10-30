package com.lklengl.youtubevideodownloader;
import java.util.*;
/** Requested height is a target limit, not a claim that a stream exists. */
public final class PlaylistQualityPolicy {
 private PlaylistQualityPolicy(){}
 public static final String[] VALUES={"best","2160","1440","1080","720","480","360","240","144"};
 public static final String[] LABELS={"Максимальное","4K · 2160p","1440p","1080p","720p","480p","360p","240p","144p"};
 public static boolean allowed(String quality){
  if(quality==null)return false;
  for(String v:VALUES)if(v.equals(quality))return true;
  return false;
 }
 public static int index(String value){
  for(int n=0;n<VALUES.length;n++)if(VALUES[n].equals(value))return n;
  return 0;
 }
 public static String qualityFor(List<String> values,int index,String fallback){
  if(index<0||index>=values.size()||!allowed(values.get(index)))return allowed(fallback)?fallback:"best";
  return values.get(index);
 }
 public static List<String> applyToSelected(List<String> existing,boolean[] selected,String requested){
  if(!allowed(requested))throw new IllegalArgumentException("Неизвестное качество");
  if(existing.size()!=selected.length)throw new IllegalArgumentException("Несовпадение количества видео");
  List<String> result=new ArrayList<>(existing);
  for(int i=0;i<selected.length;i++)if(selected[i])result.set(i,requested);
  return result;
 }
}