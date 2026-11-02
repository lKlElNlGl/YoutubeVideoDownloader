package com.lklengl.youtubevideodownloader;
public final class QueueNotice {
 private QueueNotice(){}
 public static String message(int count,boolean priority){
  if(count!=1)return "Добавлено в очередь: "+count+" видео";
  return priority?"Приоритетная загрузка добавлена":"Добавлено в очередь";
 }
}