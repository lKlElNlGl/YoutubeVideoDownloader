package com.lklengl.youtubevideodownloader;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.yausername.youtubedl_android.YoutubeDL;
import java.util.concurrent.locks.ReentrantLock;

/** One updater shared by manual actions, daily worker, previews and download recovery. */
public final class EngineMaintenance {
    public enum Result { UPDATED, CURRENT, SKIPPED }
    private static final String PREFS="engine_maintenance";
    private static final ReentrantLock LOCK=new ReentrantLock();
    private EngineMaintenance(){}
    public static Result update(Context context,boolean force,boolean fromActiveDownload) throws Exception {
        Context app=context.getApplicationContext();
        if(!fromActiveDownload && DownloadQueue.hasActive(app))return Result.SKIPPED;
        if(!LOCK.tryLock())return Result.SKIPPED;
        try {
            // Only one automatic check per day; manual checks always bypass the cooldown.
            SharedPreferences p=app.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
            long now=System.currentTimeMillis();
            if(!force&&!EngineRecoveryPolicy.isCheckDue(now,p.getLong("last_attempt",0)))
                return Result.SKIPPED;
            p.edit().putLong("last_attempt",now).apply();
            YoutubeDL.getInstance().init(app);
            YoutubeDL.UpdateStatus result=YoutubeDL.getInstance().updateYoutubeDL(
                app,YoutubeDL.UpdateChannel.STABLE.INSTANCE);
            if(result==YoutubeDL.UpdateStatus.DONE)return Result.UPDATED;
            return Result.CURRENT;
        }finally {LOCK.unlock();}
    }
}
