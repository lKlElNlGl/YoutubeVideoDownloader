package com.lklengl.youtubevideodownloader;

import android.content.Context;
import android.util.Log;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.concurrent.TimeUnit;

/** Periodic maintenance only when network is available and no queue is active. */
public final class EngineUpdateWorker extends Worker {
    private static final String TASK="ytdlp_daily_stable_check";
    public EngineUpdateWorker(Context context,WorkerParameters p){super(context,p);}
    public static void schedule(Context context){
        Constraints constraints=new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest work=new PeriodicWorkRequest.Builder(EngineUpdateWorker.class,24,TimeUnit.HOURS)
            .setConstraints(constraints).build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
            TASK,ExistingPeriodicWorkPolicy.KEEP,work);
    }
    @Override public Result doWork(){
        try{
            EngineMaintenance.update(getApplicationContext(),false,false);
            return Result.success();
        }catch(Exception e){
            Log.w("YoutubeVideoDownloader","Плановая проверка yt-dlp не выполнена",e);
            return Result.success();
        }
    }
}
