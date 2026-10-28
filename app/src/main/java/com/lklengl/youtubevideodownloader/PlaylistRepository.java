package com.lklengl.youtubevideodownloader;

import android.content.Context;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Only extract metadata here; every video is downloaded individually by DownloadService. */
public final class PlaylistRepository {
    public static final class Entry {
        public final String id, url, title;
        public final int duration;
        Entry(String id,String title,int duration) {
            this.id=id;this.url=PlaylistSupport.videoUrl(id);this.title=title;this.duration=duration;
        }
    }
    public static final class Playlist {
        public final String title;
        public final List<Entry> entries;
        public final int reportedCount;
        Playlist(String title,List<Entry> entries,int reportedCount){
            this.title=title;this.entries=entries;this.reportedCount=reportedCount;
        }
    }
    public static Playlist fetch(Context context,String link,String proxy,boolean cookies) throws Exception {
        if(!PlaylistSupport.isPlaylist(link))throw new IllegalArgumentException("Не найдена ссылка на плейлист");
        YoutubeDL.getInstance().init(context.getApplicationContext());
        YoutubeDLRequest req=new YoutubeDLRequest(link);
        req.addOption("--flat-playlist");
        req.addOption("--dump-single-json");
        req.addOption("--skip-download");
        req.addOption("--yes-playlist");
        req.addOption("--ignore-errors");
        req.addOption("--no-warnings");
        if(proxy!=null && !proxy.isEmpty())req.addOption("--proxy",proxy);
        if(cookies){
            File file=new File(context.getFilesDir(),"cookies.txt");
            if(!file.isFile())throw new IllegalArgumentException("Нет cookies.txt");
            req.addOption("--cookies",file.getAbsolutePath());
        }
        String result=YoutubeDL.getInstance().execute(req,null,null).getOut();
        JSONObject data=new JSONObject(result);
        JSONArray entries=data.optJSONArray("entries");
        if(entries==null)throw new IllegalArgumentException("Не удалось получить список видео плейлиста");
        List<Entry> accepted=new ArrayList<>();
        for(int i=0;i<entries.length();i++){
            JSONObject e=entries.optJSONObject(i);if(e==null)continue;
            String id=e.optString("id");
            try {
                String title=e.optString("title");
                if(title.isEmpty())title="Видео "+(i+1);
                accepted.add(new Entry(id,title,e.optInt("duration",-1)));
            }catch(IllegalArgumentException ignored) {} // Deleted/private/nested entries.
        }
        if(accepted.isEmpty())throw new IllegalArgumentException("В плейлисте нет доступных видео");
        int count=data.optInt("playlist_count",entries.length());
        return new Playlist(data.optString("title","Плейлист YouTube"),accepted,count);
    }
}
