package com.lklengl.youtubevideodownloader;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistent sequential queue; all read-modify-write operations are serialized. */
public final class DownloadQueue {
    public static final String WAITING="waiting", RUNNING="running", DONE="done", ERROR="error", CANCELLED="cancelled";
    private static final String PREFS="download_queue_v1", KEY="items";
    private static final Object LOCK=new Object();

    public static final class Item {
        public String id, url, quality, proxy, directoryUri, status, message, filename, title, playlistTitle;
        public boolean audio;
        public int progress;

        public Item() {}
        JSONObject toJson() throws Exception {
            JSONObject j=new JSONObject();
            j.put("id",id); j.put("url",url);j.put("quality",quality);j.put("proxy",proxy);
            j.put("directoryUri",directoryUri);j.put("status",status);j.put("message",message);
            j.put("filename",filename);j.put("title",title);j.put("playlistTitle",playlistTitle);j.put("audio",audio);j.put("progress",progress);
            return j;
        }
        static Item fromJson(JSONObject j) {
            Item i=new Item();i.id=j.optString("id");i.url=j.optString("url");
            i.quality=j.optString("quality","best");i.proxy=j.optString("proxy");
            i.directoryUri=j.optString("directoryUri");i.status=j.optString("status",WAITING);
            i.message=j.optString("message");i.filename=j.optString("filename");i.title=j.optString("title");i.playlistTitle=j.optString("playlistTitle");
            i.audio=j.optBoolean("audio");
            i.progress=j.optInt("progress");return i;
        }
    }

    private DownloadQueue(){}
    private static SharedPreferences prefs(Context context){return context.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    private static List<Item> read(Context context) {
        List<Item> data=new ArrayList<>();
        try{
            JSONArray json=new JSONArray(prefs(context).getString(KEY,"[]"));
            for(int n=0;n<json.length();n++) {
                Item item=Item.fromJson(json.getJSONObject(n));
                if(!item.id.isEmpty())data.add(item);
            }
        }catch(Exception ignored) {} // Keep UI usable if local state is damaged.
        return data;
    }
    private static void save(Context context,List<Item> items) {
        JSONArray json=new JSONArray();
        try{for(Item item:items)json.put(item.toJson());}
        catch(Exception ex){throw new IllegalStateException("Queue serialization failed",ex);}
        if(!prefs(context).edit().putString(KEY,json.toString()).commit())
            throw new IllegalStateException("Не удалось сохранить очередь");
    }
    public static List<Item> all(Context context){synchronized(LOCK){return read(context);}}
    public static String enqueue(Context context,String url,boolean audio,String quality,String proxy,String directoryUri,boolean priority) {
        synchronized(LOCK){
            List<Item> list=read(context);
            if(QueueRules.activeCount(list)>=2000)throw new IllegalStateException("Очередь ограничена 2000 активными заданиями");
            Item i=new Item();i.id=UUID.randomUUID().toString();i.url=url;i.audio=audio;
            i.quality=quality;i.proxy=proxy;i.directoryUri=directoryUri;
            i.status=WAITING;i.message="Ожидает";i.filename="";i.title="";i.playlistTitle="";i.progress=0;
            if(priority)QueueRules.prependPriority(list,java.util.Collections.singletonList(i));
            else list.add(i);
            save(context,list);return i.id;
        }
    }
    /** Adds the selected playlist entries atomically; avoids partial playlists on capacity failures. */
    public static int enqueuePlaylist(Context context,PlaylistRepository.Playlist playlist,List<Boolean> selected,
            boolean audio,String quality,String proxy,String directoryUri,boolean priority) {
        synchronized(LOCK) {
            if(selected.size()!=playlist.entries.size())throw new IllegalArgumentException("Выбор видео изменился");
            List<Item> list=read(context);
            int count=0;
            for(Boolean checked:selected)if(Boolean.TRUE.equals(checked))count++;
            if(count==0)throw new IllegalArgumentException("Выберите хотя бы одно видео");
            if(QueueRules.activeCount(list)+count>2000)throw new IllegalStateException("Слишком много задач в очереди (максимум 2000)");
            List<Item> added=new ArrayList<>();
            for(int n=0;n<selected.size();n++) {
                if(!Boolean.TRUE.equals(selected.get(n)))continue;
                PlaylistRepository.Entry entry=playlist.entries.get(n);
                Item i=new Item();i.id=UUID.randomUUID().toString();i.url=entry.url;i.title=entry.title;
                i.playlistTitle=playlist.title;i.quality=quality;i.proxy=proxy;i.audio=audio;
                i.directoryUri=directoryUri;i.status=WAITING;i.message="Ожидает";
                i.filename="";i.progress=0;added.add(i);
            }
            if(priority)QueueRules.prependPriority(list,added);
            else list.addAll(added);
            save(context,list);return count;
        }
    }
    public static Item next(Context context) {
        synchronized(LOCK){
            return QueueRules.nextWaiting(read(context));
        }
    }
    public static boolean hasActive(Context context){synchronized(LOCK){return QueueRules.activeCount(read(context))>0;}}
    public static void recover(Context context) {
        synchronized(LOCK){
            List<Item> list=read(context);boolean changed=false;
            for(Item i:list)if(RUNNING.equals(i.status)){i.status=WAITING;i.progress=0;i.message="Возобновление после перезапуска";changed=true;}
            if(changed)save(context,list);
        }
    }
    public static void update(Context context,String id,String status,String message,int progress,String filename){
        synchronized(LOCK){
            List<Item> list=read(context);
            for(Item i:list)if(i.id.equals(id)){
                i.status=status;i.message=message;i.progress=Math.max(0,Math.min(100,progress));
                if(filename!=null)i.filename=filename;save(context,list);return;
            }
        }
    }
    public static void cancelWaiting(Context context,String id) {
        synchronized(LOCK){
            List<Item> list=read(context);
            for(Item i:list)if(i.id.equals(id) && WAITING.equals(i.status)) {
                i.status=CANCELLED;i.message="Отменено";save(context,list);return;
            }
        }
    }
    public static void retry(Context context,String id) {
        synchronized(LOCK){
            List<Item> list=read(context);
            for(Item i:list)if(i.id.equals(id) &&
                    (ERROR.equals(i.status)||CANCELLED.equals(i.status))) {
                i.status=WAITING;i.progress=0;i.message="Ожидает";save(context,list);return;
            }
        }
    }
    public static void clearFinished(Context context){
        synchronized(LOCK){
            List<Item> list=read(context);
            list.removeIf(i->DONE.equals(i.status)||ERROR.equals(i.status)||CANCELLED.equals(i.status));
            save(context,list);
        }
    }
}
