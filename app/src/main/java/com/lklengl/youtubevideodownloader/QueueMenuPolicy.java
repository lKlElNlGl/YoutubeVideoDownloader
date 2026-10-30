package com.lklengl.youtubevideodownloader;
public final class QueueMenuPolicy {
 private QueueMenuPolicy(){}
 public static boolean needsSeparator(int count,int index,boolean danger){
  return danger && count>1 && index>0;
 }
}