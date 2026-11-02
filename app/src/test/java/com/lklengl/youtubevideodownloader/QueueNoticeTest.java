package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;
public class QueueNoticeTest {
 @Test public void countsAndPriorityDoNotClaimImmediateStart(){
  assertEquals("Приоритетная загрузка добавлена",QueueNotice.message(1,true));
  assertEquals("Добавлено в очередь",QueueNotice.message(1,false));
  assertEquals("Добавлено в очередь: 8 видео",QueueNotice.message(8,false));
 }
}