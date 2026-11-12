package com.lklengl.youtubevideodownloader;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.*;

/** Per-video yt-dlp format discovery; never blocks playlist rendering or download queue. */
public final class PlaylistResolutionWorker {
 public interface Listener {void changed(String videoId,boolean failed);}
 private static final String PREFS="playlist_resolutions_v058";
 private final Context app;
 private final String proxy;
 private final Listener listener;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final LinkedBlockingDeque<PlaylistRepository.Entry> queue=new LinkedBlockingDeque<>();
 private final Map<String,PlaylistRepository.Entry> pending=new ConcurrentHashMap<>();
 private final Map<String,List<String>> cache=new HashMap<>();
 private final Set<String> finished=new HashSet<>();
 private final Set<String> failures=new HashSet<>();
 private volatile boolean closed=false;
 private volatile boolean scanning=false;
 private volatile String currentProcessId="";
 private volatile int completed=0,total=0;
 private volatile int generation=0;
 private static final String SETTINGS="playlist_quality_scan_options";
 public static boolean enabled(Context c){
  return c.getApplicationContext().getSharedPreferences(SETTINGS,0).getBoolean("enabled",false);
 }
 public static void setEnabled(Context c,boolean yes){
  c.getApplicationContext().getSharedPreferences(SETTINGS,0).edit()
   .putBoolean("enabled",yes).apply();
 }
 public boolean scanning(){return scanning;}
 public int completed(){return completed;}
 public int total(){return total;}
 public boolean pending(String id){return scanning&&!ready(id)&&!failed(id);}
 public boolean started(){return total>0;}

 public PlaylistResolutionWorker(Context context,String proxy,Listener listener){
  app=context.getApplicationContext();this.proxy=proxy==null?"":proxy;this.listener=listener;
 }
 private static String key(String id){return "v_"+id.replaceAll("[^A-Za-z0-9_-]","_");}
 public static void markPlaylistRefreshed(Context context,String url){
  context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
   .putBoolean("refresh_"+url.hashCode(),true).apply();
 }
 public static boolean takeRefreshFlag(Context context,String url){
  SharedPreferences prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
  boolean refresh=prefs.getBoolean("refresh_"+url.hashCode(),false);
  if(refresh)prefs.edit().remove("refresh_"+url.hashCode()).apply();
  return refresh;
 }
 /** Read persisted real formats from a previous session without a network request. */
 public List<String> cached(String videoId){
  List<String> known=cache.get(videoId);
  if(known!=null)return known;
  String saved=app.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
   .getString(key(videoId),null);
  if(saved==null)return null;
  try{
   JSONArray arr=new JSONArray(saved);
   List<String> values=new ArrayList<>();
   for(int i=0;i<arr.length();i++)values.add(arr.getString(i));
   if(!values.contains("best")&&!values.contains("audio-only"))return null;
   known=Collections.unmodifiableList(values);cache.put(videoId,known);return known;
  }catch(Exception ignored){return null;}
 }
 public boolean failed(String id){return failures.contains(id);}
 public boolean ready(String id){return cached(id)!=null||finished.contains(id);}
 public List<String> choices(String id){
  List<String> known=cached(id);
  return known!=null?known:Release058Rules.fallback();
 }
 public void start(PlaylistRepository.Playlist playlist,boolean force){
  final int serial=++generation;
  queue.clear();pending.clear();finished.clear();failures.clear();
  total=playlist.entries.size();completed=0;
  for(PlaylistRepository.Entry entry:playlist.entries){
   if(!force&&cached(entry.id)!=null){completed++;continue;}
   queue.addLast(entry);pending.put(entry.id,entry);
  }
  scanning=!queue.isEmpty();
  if(!scanning){main.post(()->listener.changed("",false));return;}
  worker.execute(()->{
   try{YoutubeDL.getInstance().init(app);}catch(Exception ignored){}
   while(!closed&&serial==generation){
    PlaylistRepository.Entry entry=queue.pollFirst();
    if(entry==null)break;
    synchronized(pending){pending.remove(entry.id);}
    List<String> result=null;boolean failure=false;
    try{
     YoutubeDLRequest request=new YoutubeDLRequest(entry.url);
     String process="quality-"+java.util.UUID.randomUUID();
     currentProcessId=process;
     request.addOption("--no-playlist");request.addOption("--dump-single-json");
     request.addOption("--skip-download");request.addOption("--no-warnings");
     request.addOption("--socket-timeout","8");request.addOption("--retries","1");
     request.addOption("--extractor-retries","1");
     if(!proxy.isEmpty())request.addOption("--proxy",proxy);
     String raw=YoutubeDL.getInstance().execute(request,process,null).getOut();
     JSONObject info=new JSONObject(raw);
     JSONArray streams=info.optJSONArray("formats");
     if(streams==null)throw new IllegalStateException("Formats missing");
     Set<Integer> heights=new HashSet<>();
     boolean hasVideo=false,hasAudio=false;
     for(int i=0;i<streams.length();i++){
      JSONObject stream=streams.optJSONObject(i);if(stream==null)continue;
      boolean video=!"none".equals(stream.optString("vcodec","none"));
      boolean audio=!"none".equals(stream.optString("acodec","none"));
      hasVideo|=video;hasAudio|=audio;
      int height=stream.optInt("height",0);
      if(video&&height>0)heights.add(height);
     }
     if(!hasVideo&&!hasAudio)throw new IllegalStateException("No media formats");
     result=hasVideo?new ArrayList<>(Release058Rules.available(heights)):
       new ArrayList<>(Collections.singletonList("audio-only"));
     if(hasVideo&&!hasAudio)result.add("video-only");
     JSONArray json=new JSONArray();for(String q:result)json.put(q);
     app.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
      .putString(key(entry.id),json.toString()).apply();
    }catch(Exception e){failure=true;}
    currentProcessId="";
    if(closed||serial!=generation)break;
    final List<String> resolved=result;
    final boolean failed=failure;
    main.post(()->{
     if(closed||serial!=generation)return;
     if(resolved!=null)cache.put(entry.id,Collections.unmodifiableList(resolved));
     if(failed)failures.add(entry.id);
     finished.add(entry.id);
     completed++;
     if(completed>=total)scanning=false;
     listener.changed(entry.id,failed);
    });
   }
   if(serial==generation){
    scanning=false;
    main.post(()->{if(!closed&&serial==generation)listener.changed("",false);});
   }
  });
 }
 /** Move visible entries to the front, preserving their screen order. */
 public void prioritize(List<PlaylistRepository.Entry> visible){
  synchronized(pending){
   for(int i=visible.size()-1;i>=0;i--){
    PlaylistRepository.Entry e=pending.get(visible.get(i).id);
    if(e!=null){queue.remove(e);queue.addFirst(e);}
   }
  }
 }
 /** Cancels a very slow quality scan without deleting completed cached results. */
 public void stop(){
  generation++;scanning=false;queue.clear();pending.clear();
  String process=currentProcessId;currentProcessId="";
  if(!process.isEmpty())try{YoutubeDL.getInstance().destroyProcessById(process);}
   catch(Exception ignored){}
  main.post(()->{if(!closed)listener.changed("",false);});
 }
 public void close(){stop();closed=true;worker.shutdownNow();}
}
