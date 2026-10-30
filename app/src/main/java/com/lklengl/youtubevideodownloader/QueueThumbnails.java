package com.lklengl.youtubevideodownloader;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.widget.ImageView;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.*;
import java.util.Set;

/** Tiny thumbnails cached locally per queue entry; never blocks UI or downloads. */
public final class QueueThumbnails {
    private static final ThreadPoolExecutor POOL=new ThreadPoolExecutor(2,2,0,
        TimeUnit.SECONDS,new ArrayBlockingQueue<>(48),new ThreadPoolExecutor.AbortPolicy());
    private static final Set<String> ACTIVE=ConcurrentHashMap.newKeySet();
    private QueueThumbnails(){}
    private static File file(Context c,String id){
        File folder=new File(c.getFilesDir(),"queue-thumbnails");
        if(!folder.exists())folder.mkdirs();
        return new File(folder,id+".jpg");
    }
    public static void store(Context c,String id,Bitmap image){
        if(image==null||!id.matches("[a-zA-Z0-9-]{8,80}"))return;
        File dst=file(c,id);
        try(FileOutputStream out=new FileOutputStream(dst)){
            image.compress(Bitmap.CompressFormat.JPEG,76,out);
        }catch(Exception ignored){}
    }
    public static void delete(Context c,String id){if(id.matches("[a-zA-Z0-9-]{8,80}"))file(c,id).delete();}
    public static void show(Activity activity,DownloadQueue.Item item,ImageView view,int radius){
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        view.setBackground(ThemeStyle.rounded(ThemeStyle.field(activity),ThemeStyle.border(activity),radius));
        view.setClipToOutline(true);
        view.setTag(item.id);
        File local=file(activity,item.id);
        if(local.isFile()){
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inSampleSize=2;
            Bitmap b=BitmapFactory.decodeFile(local.getAbsolutePath(),options);
            if(b!=null){view.setImageBitmap(b);return;}
        }
        view.setImageDrawable(new UiIcons("play",ThemeStyle.subdued(activity),ThemeStyle.dp(activity,28)));
        String vid=QueuePolicy.videoId(item.url);
        if(!vid.matches("[A-Za-z0-9_-]{11}")||!ACTIVE.add(item.id))return;
        try{
            POOL.execute(()->{
                HttpURLConnection conn=null;
                try{
                    URL u=new URL("https://i.ytimg.com/vi/"+vid+"/mqdefault.jpg");
                    conn=(HttpURLConnection)u.openConnection();
                    conn.setConnectTimeout(6000);conn.setReadTimeout(7000);
                    if(conn.getResponseCode()!=200)return;
                    try(InputStream input=conn.getInputStream();ByteArrayOutputStream buf=new ByteArrayOutputStream()){
                        byte[] bytes=new byte[8192];int count;
                        while((count=input.read(bytes))!=-1){
                            if(buf.size()+count>350000)return;
                            buf.write(bytes,0,count);
                        }
                        byte[] data=buf.toByteArray();
                        BitmapFactory.Options options=new BitmapFactory.Options();
                        options.inSampleSize=2;
                        Bitmap b=BitmapFactory.decodeByteArray(data,0,data.length,options);
                        if(b==null)return;
                        store(activity.getApplicationContext(),item.id,b);
                        activity.runOnUiThread(()->{
                            if(item.id.equals(view.getTag())&&!activity.isFinishing()&&!activity.isDestroyed())
                                view.setImageBitmap(b);
                        });
                    }
                }catch(Exception ignored){}
                finally{if(conn!=null)conn.disconnect();ACTIVE.remove(item.id);}
            });
        }catch(RejectedExecutionException ignored){ACTIVE.remove(item.id);}
    }
}
