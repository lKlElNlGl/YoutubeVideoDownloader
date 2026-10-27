package com.lklengl.youtubevideodownloader;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.ffmpeg.FFmpeg;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** A foreground data-sync service owns the yt-dlp process and sequential queue. */
public final class DownloadService extends Service {
    public static final String ACTION_CHANGED="com.lklengl.youtubevideodownloader.QUEUE_CHANGED";
    private static final String ACTION_CANCEL="com.lklengl.youtubevideodownloader.CANCEL";
    private static final int NOTIFICATION_ID=40;
    private static final String CHANNEL="active_downloads";
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final AtomicBoolean draining=new AtomicBoolean();
    private volatile String currentId=null;
    private volatile boolean cancelCurrent=false;
    private long lastNotification=0L;
    private PowerManager.WakeLock activeLock;

    public static void kick(Context context) {
        Intent i=new Intent(context,DownloadService.class);
        context.startForegroundService(i);
    }
    public static void cancel(Context context,String id) {
        Intent i=new Intent(context,DownloadService.class);
        i.setAction(ACTION_CANCEL);i.putExtra("id",id);
        context.startForegroundService(i);
    }
    @Override public void onCreate() {
        super.onCreate();
        NotificationManager manager=getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL,"Загрузки YoutubeVideoDownloader",NotificationManager.IMPORTANCE_LOW));
        promote("Ожидание загрузок",0);
        DownloadQueue.recover(this);
    }
    private void promote(String message,int percentage) {
        Intent open=new Intent(this,MainActivity.class);
        PendingIntent content=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Intent cancel=new Intent(this,DownloadService.class).setAction(ACTION_CANCEL).putExtra("id",currentId);
        PendingIntent cancelAction=PendingIntent.getService(this,1,cancel,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder=new Notification.Builder(this,CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("YoutubeVideoDownloader")
            .setContentText(message).setContentIntent(content).setOnlyAlertOnce(true)
            .setOngoing(true).setProgress(100,Math.max(0,Math.min(100,percentage)),percentage<=0);
        if(currentId!=null)builder.addAction(new Notification.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,"Отменить",cancelAction).build());
        Notification n=builder.build();
        if(Build.VERSION.SDK_INT>=29)
            startForeground(NOTIFICATION_ID,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        else startForeground(NOTIFICATION_ID,n);
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null && ACTION_CANCEL.equals(intent.getAction())) {
            String id=intent.getStringExtra("id");
            if(id!=null){
                DownloadQueue.cancelWaiting(this,id);
                if(id.equals(currentId)){
                    cancelCurrent=true;
                    YoutubeDL.getInstance().destroyProcessById(id);
                }
                announce();
            }
        }
        runQueue();
        return START_STICKY;
    }
    private void runQueue(){
        if(!draining.compareAndSet(false,true))return;
        executor.execute(() -> {
            try {
                PowerManager manager=getSystemService(PowerManager.class);
                activeLock=manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"YoutubeVideoDownloader:Download");
                activeLock.acquire(5L * 60L * 60L * 1000L);
                for(DownloadQueue.Item item=DownloadQueue.next(this);item!=null;item=DownloadQueue.next(this)){
                    download(item);
                }
            }finally {
                if(activeLock!=null && activeLock.isHeld())activeLock.release();
                activeLock=null;
                draining.set(false);
                if(DownloadQueue.next(this)!=null)runQueue();
                else {currentId=null;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
            }
        });
    }
    private void announce(){
        sendBroadcast(new Intent(ACTION_CHANGED).setPackage(getPackageName()));
    }
    private void progress(DownloadQueue.Item item,String text,int percentage){
        DownloadQueue.update(this,item.id,DownloadQueue.RUNNING,text,percentage,null);
        announce();
        long now=android.os.SystemClock.elapsedRealtime();
        if(now-lastNotification>=500) {
            promote(text,percentage);
            lastNotification=now;
        }
    }
    private void download(DownloadQueue.Item item){
        currentId=item.id;cancelCurrent=false;
        File temp=new File(getCacheDir(),"download-"+item.id);
        try {
            if(!temp.isDirectory()&&!temp.mkdirs())throw new IOException("Невозможно создать папку загрузки");
            progress(item,"Подготовка загрузки",0);
            YoutubeDL.getInstance().init(getApplicationContext());
            FFmpeg.getInstance().init(getApplicationContext());
            checkCancel();
            YoutubeDLRequest request=new YoutubeDLRequest(item.url);
            request.addOption("--no-playlist");
            request.addOption("--no-overwrites");
            request.addOption("--no-mtime");
            request.addOption("--newline");
            request.addOption("-P",temp.getAbsolutePath());
            request.addOption("-o",QueueRules.filenameTemplate());
            request.addOption("-f",DownloadOptions.format(item.audio,item.quality));
            if(item.audio){
                request.addOption("-x");
                request.addOption("--audio-format","mp3");
                request.addOption("--audio-quality","192K");
            }else request.addOption("--merge-output-format","mp4");
            if(!item.proxy.isEmpty())request.addOption("--proxy",item.proxy);
            if(item.cookies){
                File cookies=new File(getFilesDir(),"cookies.txt");
                if(!cookies.isFile())throw new IOException("Не найден cookies.txt");
                request.addOption("--cookies",cookies.getAbsolutePath());
            }
            YoutubeDL.getInstance().execute(request,item.id,(p,eta,line) -> {
                int percent=Math.max(0,Math.min(99,Math.round(p)));
                // Throttle preference writes to reduce flash wear.
                long now=android.os.SystemClock.elapsedRealtime();
                if(now-lastNotification>400) {
                    String etaText=eta>=0?" · осталось "+eta+" с":"";
                    progress(item,"Загрузка "+percent+"%"+etaText,percent);
                }
                return kotlin.Unit.INSTANCE;
            });
            checkCancel();
            File finished=findFinalFile(temp);
            if(finished==null)throw new IOException("Не найден файл после обработки");
            progress(item,"Сохранение файла",99);
            Uri saved=saveFile(finished,item);
            checkCancel();
            DownloadQueue.update(this,item.id,DownloadQueue.DONE,"Сохранено",100,finished.getName());
            announce();
        } catch(Exception ex){
            boolean cancelled=cancelCurrent;
            DownloadQueue.update(this,item.id,cancelled?DownloadQueue.CANCELLED:DownloadQueue.ERROR,
                cancelled?"Отменено":("Ошибка: "+(ex.getMessage()==null?"неизвестная":ex.getMessage())),0,null);
            announce();
        }finally{
            deleteRecursive(temp);
            currentId=null;cancelCurrent=false;
        }
    }
    private void checkCancel() throws IOException {
        if(cancelCurrent)throw new IOException("Загрузка отменена");
    }
    private File findFinalFile(File folder){
        File[] list=folder.listFiles(f->f.isFile() && !f.getName().endsWith(".part") &&
          !f.getName().endsWith(".ytdl") && !f.getName().endsWith(".temp"));
        if(list==null||list.length==0)return null;
        Arrays.sort(list,Comparator.comparingLong(File::lastModified).reversed());
        return list[0];
    }
    private Uri saveFile(File source,DownloadQueue.Item item) throws IOException {
        ContentResolver resolver=getContentResolver();
        ContentValues values=null;
        Uri dest;
        boolean media=item.directoryUri.isEmpty();
        if(media){
            values=new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME,source.getName());
            values.put(MediaStore.MediaColumns.MIME_TYPE,mime(source.getName()));
            values.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/YoutubeVideoDownloader");
            values.put(MediaStore.MediaColumns.IS_PENDING,1);
            dest=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
        }else {
            Uri tree=Uri.parse(item.directoryUri);
            Uri root=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
            dest=DocumentsContract.createDocument(resolver,root,mime(source.getName()),source.getName());
        }
        if(dest==null)throw new IOException("Не удалось создать файл. Проверьте разрешение папки.");
        boolean complete=false;
        try(InputStream input=new FileInputStream(source);OutputStream output=resolver.openOutputStream(dest,"w")){
            if(output==null)throw new IOException("Нет доступа к файлу");
            byte[] bytes=new byte[131072];int n;
            while((n=input.read(bytes))>=0){
                checkCancel();if(n>0)output.write(bytes,0,n);
            }
            checkCancel();
            complete=true;
        }finally {
            if(!complete) {
                if(media)resolver.delete(dest,null,null);
                else DocumentsContract.deleteDocument(resolver,dest);
            }
        }
        if(media){
            values.clear();values.put(MediaStore.MediaColumns.IS_PENDING,0);
            resolver.update(dest,values,null,null);
        }
        return dest;
    }
    private String mime(String name){
        String lower=name.toLowerCase(Locale.ROOT);
        if(lower.endsWith(".mp3"))return "audio/mpeg";
        if(lower.endsWith(".m4a"))return "audio/mp4";
        if(lower.endsWith(".webm"))return "video/webm";
        if(lower.endsWith(".mkv"))return "video/x-matroska";
        return "video/mp4";
    }
    private void deleteRecursive(File file){
        if(!file.exists())return;
        if(file.isDirectory()){
            File[] children=file.listFiles();
            if(children!=null)for(File child:children)deleteRecursive(child);
        }
        file.delete();
    }
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public void onDestroy(){
        if(currentId!=null){cancelCurrent=true;YoutubeDL.getInstance().destroyProcessById(currentId);}
        executor.shutdownNow();super.onDestroy();
    }
}
