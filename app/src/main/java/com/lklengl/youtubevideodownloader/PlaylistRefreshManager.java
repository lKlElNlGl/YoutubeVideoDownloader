package com.lklengl.youtubevideodownloader;
import android.content.Context;
import android.os.SystemClock;
import com.yausername.youtubedl_android.YoutubeDL;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Process-wide playlist refresh. UI screens only observe; leaving a screen never cancels yt-dlp. */
public final class PlaylistRefreshManager {
 public enum Stage {IDLE,RUNNING,DONE,ERROR,CANCELLED}
 public static final class Snapshot {
  public final String url,processId,error,message;
  public final Stage stage;
  public final long startedMs,revision;
  Snapshot(String url,String id,Stage stage,String error,String message,long start,long revision){
   this.url=url;this.processId=id;this.stage=stage;this.error=error;
   this.message=message;this.startedMs=start;this.revision=revision;
  }
  public boolean running(){return stage==Stage.RUNNING;}
 }
 private static final ExecutorService worker=Executors.newSingleThreadExecutor();
 private static volatile Snapshot latest=new Snapshot("","",Stage.IDLE,"","",0,0);
 private PlaylistRefreshManager(){}
 public static Snapshot current(){return latest;}
 public static synchronized boolean start(Context context,String url,String proxy){
  if(latest.running())return url.equals(latest.url);
  String id=UUID.randomUUID().toString();
  final Context application=context.getApplicationContext();
  final long serial=latest.revision+1;
  latest=new Snapshot(url,id,Stage.RUNNING,"","Получаем список видео плейлиста…",
   SystemClock.elapsedRealtime(),serial);
  worker.execute(()->{
   try{
    PlaylistRepository.Playlist data;
    try{data=PlaylistRepository.fetch(application,url,proxy,id);}
    catch(Exception first){
     if(!isActive(id)||!EngineRecoveryPolicy.shouldUpdate(first.getMessage())||
        DownloadQueue.hasActive(application))throw first;
     changeMessage(id,"Обновление компонента YouTube…");
     if(EngineMaintenance.update(application,false,false)!=EngineMaintenance.Result.UPDATED)throw first;
     changeMessage(id,"Повторное получение списка…");
     data=PlaylistRepository.fetch(application,url,proxy,id);
    }
    if(!isActive(id))return;
    PlaylistCache.save(application,url,data);
    finish(id,Stage.DONE,"");
   }catch(Exception e){finish(id,Stage.ERROR,e.getMessage()==null?"Не удалось обновить плейлист":e.getMessage());}
  });
  return true;
 }
 private static synchronized boolean isActive(String id){
  return latest.running()&&latest.processId.equals(id);
 }
 private static synchronized void changeMessage(String id,String message){
  if(!isActive(id))return;
  latest=new Snapshot(latest.url,latest.processId,Stage.RUNNING,"",message,
    latest.startedMs,latest.revision);
 }
 private static synchronized void finish(String id,Stage stage,String error){
  if(!isActive(id))return;
  latest=new Snapshot(latest.url,latest.processId,stage,error,latest.message,
    latest.startedMs,latest.revision+1);
 }
 public static void cancel(String url){
  final String id;
  synchronized(PlaylistRefreshManager.class){
   if(!latest.running()||!latest.url.equals(url))return;
   id=latest.processId;
   latest=new Snapshot(latest.url,id,Stage.CANCELLED,"",latest.message,
    latest.startedMs,latest.revision+1);
  }
  try{YoutubeDL.getInstance().destroyProcessById(id);}catch(Exception ignored){}
 }
}
