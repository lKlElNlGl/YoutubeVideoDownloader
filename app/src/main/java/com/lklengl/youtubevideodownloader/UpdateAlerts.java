package com.lklengl.youtubevideodownloader;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

/** Same periodic release notification model as SSH Button Next, with hourly checks. */
public final class UpdateAlerts {
    private static final String CHANNEL="app_updates_v1";
    private static final String TASK="github_release_check";
    private UpdateAlerts(){}
    public static void schedule(Context c){
        ensureChannel(c);
        Constraints network=new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest check=new PeriodicWorkRequest.Builder(CheckWorker.class,1,TimeUnit.HOURS)
            .setConstraints(network).build();
        WorkManager.getInstance(c.getApplicationContext()).enqueueUniquePeriodicWork(
            TASK,ExistingPeriodicWorkPolicy.UPDATE,check);
    }
    private static void ensureChannel(Context c){
        NotificationManager manager=c.getSystemService(NotificationManager.class);
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Новые версии YoutubeVideoDownloader",
            NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Уведомления со звуком о новых версиях APK");
        channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
            new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        manager.createNotificationChannel(channel);
    }
    public static synchronized void notifyIfNew(Context c,GithubUpdater.Candidate candidate){
        if(Build.VERSION.SDK_INT>=33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        NotificationManager manager=c.getSystemService(NotificationManager.class);
        if(!manager.areNotificationsEnabled())return;
        android.content.SharedPreferences prefs=c.getSharedPreferences("release_alerts",Context.MODE_PRIVATE);
        if(candidate.tag.equals(prefs.getString("last_notified","")))return;
        Intent i=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending=PendingIntent.getActivity(c,2001,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Обновление YoutubeVideoDownloader")
            .setContentText("Доступна версия "+candidate.tag).setContentIntent(pending)
            .setAutoCancel(true).build();
        try{manager.notify(2211,n);prefs.edit().putString("last_notified",candidate.tag).apply();}
        catch(SecurityException ignored){}
    }
    public static final class CheckWorker extends Worker {
        public CheckWorker(Context c,WorkerParameters params){super(c,params);}
        @Override public Result doWork(){
            try {
                GithubUpdater.Candidate version=new GithubUpdater(getApplicationContext()).check();
                if(version!=null)UpdateAlerts.notifyIfNew(getApplicationContext(),version);
                return Result.success();
            }catch(Exception e){return Result.success();}
        }
    }
}
