package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class QueueOrderingV4Test {
 private DownloadQueue.Item item(String name,String status){DownloadQueue.Item i=new DownloadQueue.Item();i.id=name;i.status=status;return i;}
 @Test public void newJobsGoAboveOldWaitingButNeverInterruptRunning(){
  java.util.List<DownloadQueue.Item> jobs=new ArrayList<>(Arrays.asList(
   item("old","waiting"),item("in-progress","running"),item("completed","done")));
  QueueRules.prependPriority(jobs,Arrays.asList(item("new","waiting")));
  assertEquals("in-progress",jobs.get(0).id);
  assertEquals("new",jobs.get(1).id);
  assertEquals("old",jobs.get(2).id);
  assertEquals("completed",jobs.get(3).id);
 }
 @Test public void pausedJobsAreNotDequeuedAndCountAsActive(){
  java.util.List<DownloadQueue.Item> list=Arrays.asList(item("paused","paused"),item("ready","waiting"));
  assertEquals("ready",QueueRules.nextWaiting(list).id);
  assertEquals(2,QueueRules.activeCount(list));
 }
}
