package com.lklengl.youtubevideodownloader;
import android.content.Context;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import org.json.*;
import java.util.*;

/** Cancellable metadata extraction: give title/quality immediately, thumbnail separately. */
public final class VideoPreviewRepository {
 private VideoPreviewRepository(){}
 public static final class Result {
  public final PreviewSnapshot data;
  public final String imageUrl;
  Result(PreviewSnapshot data,String imageUrl){this.data=data;this.imageUrl=imageUrl;}
 }
 public static Result fetch(Context context,String url,String proxy,String processId)throws Exception{
  YoutubeDL.getInstance().init(context.getApplicationContext());
  YoutubeDLRequest req=new YoutubeDLRequest(url);
  req.addOption("--no-playlist");
  req.addOption("--dump-single-json");
  req.addOption("--skip-download");
  req.addOption("--no-warnings");
  if(proxy!=null&&!proxy.isEmpty())req.addOption("--proxy",proxy);
  String output=YoutubeDL.getInstance().execute(req,processId,null).getOut();
  JSONObject info=new JSONObject(output);
  String title=info.optString("title","Без названия");
  int seconds=info.optInt("duration",-1);
  String duration=seconds>=0?QueueRules.readableDuration(seconds):"—";
  Set<Integer> heights=new TreeSet<>(Collections.reverseOrder());
  Set<String> formats=new LinkedHashSet<>();
  JSONArray streams=info.optJSONArray("formats");
  if(streams!=null)for(int i=0;i<streams.length();i++){
   JSONObject stream=streams.optJSONObject(i);if(stream==null)continue;
   String ext=stream.optString("ext","").toLowerCase(Locale.ROOT);
   if(Arrays.asList("mp4","webm","m4a").contains(ext))formats.add(ext);
   String vcodec=stream.optString("vcodec","none");
   int height=stream.optInt("height",0);
   if(height>0&&!"none".equals(vcodec))heights.add(height);
  }
  ArrayList<String> resolutions=new ArrayList<>();
  for(int h:heights)resolutions.add(h+"p");
  String detected=resolutions.isEmpty()?"Не указано":android.text.TextUtils.join(", ",resolutions);
  return new Result(PreviewSnapshot.video(url,title,duration,detected,
   android.text.TextUtils.join(",",formats)),info.optString("thumbnail",""));
 }
}
