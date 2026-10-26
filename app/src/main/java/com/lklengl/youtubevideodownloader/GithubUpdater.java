package com.lklengl.youtubevideodownloader;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.HttpsURLConnection;

public final class GithubUpdater {
    private static final String API = "https://api.github.com/repos/lKlElNlGl/YoutubeVideoDownloader";
    private static final long MAX_APK = 350L * 1024 * 1024;
    private final Context context;

    public GithubUpdater(Context context) { this.context=context.getApplicationContext(); }

    public static final class Candidate {
        public final String tag, downloadUrl, digest;
        public final long size;
        Candidate(String tag, String url, String digest, long size) {
            this.tag=tag;this.downloadUrl=url;this.digest=digest;this.size=size;
        }
    }
    public static final class Cancel {
        private final AtomicBoolean stopped=new AtomicBoolean();
        private volatile HttpsURLConnection active;
        public void cancel() { stopped.set(true); HttpsURLConnection c=active; if(c!=null)c.disconnect(); }
        public void check() { if(stopped.get())throw new CancellationException("Отменено"); }
    }

    private HttpsURLConnection connect(String url, String accept, Cancel cancel) throws IOException {
        String location=url;
        for(int n=0;n<6;n++) {
            if(cancel!=null)cancel.check();
            URL parsed=new URL(location);
            String host=parsed.getHost().toLowerCase(Locale.ROOT);
            if(!"https".equals(parsed.getProtocol()) ||
                    !(host.equals("api.github.com") || host.equals("github.com") ||
                      host.equals("release-assets.githubusercontent.com") ||
                      host.equals("objects.githubusercontent.com")))
                throw new IOException("Недопустимый адрес обновления");
            HttpsURLConnection c=(HttpsURLConnection)parsed.openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(12000);c.setReadTimeout(20000);
            c.setRequestProperty("User-Agent","YoutubeVideoDownloader-Android-Updater");
            c.setRequestProperty("Accept",accept);
            if(cancel!=null){cancel.active=c;cancel.check();}
            int code=c.getResponseCode();
            if(code==301 || code==302 || code==303 || code==307 || code==308) {
                String next=c.getHeaderField("Location");
                c.disconnect();
                if(next==null)throw new IOException("Нет адреса перенаправления");
                location=new URL(parsed,next).toExternalForm();
                continue;
            }
            if(code!=200){c.disconnect();throw new IOException("GitHub HTTP "+code);}
            return c;
        }
        throw new IOException("Слишком много перенаправлений");
    }

    public Candidate check() throws Exception {
        HttpsURLConnection c=connect(API+"/releases?per_page=15","application/vnd.github+json",null);
        JSONArray list;
        try(InputStream input=c.getInputStream()) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            byte[] buffer=new byte[8192];int n;
            while((n=input.read(buffer))!=-1) {
                if(out.size()+n>1000000)throw new IOException("Слишком большой ответ GitHub");
                out.write(buffer,0,n);
            }
            list=new JSONArray(out.toString("UTF-8"));
        }finally{c.disconnect();}
        String local=context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName;
        Candidate best=null;
        for(int i=0;i<list.length();i++) {
            JSONObject release=list.getJSONObject(i);
            if(release.optBoolean("draft") || release.optBoolean("prerelease"))continue;
            String tag=release.optString("tag_name");
            try { if(VersionSupport.compare(tag,local)<=0)continue; }
            catch(IllegalArgumentException ignored){continue;}
            JSONArray assets=release.optJSONArray("assets");if(assets==null)continue;
            for(int j=0;j<assets.length();j++){
                JSONObject asset=assets.getJSONObject(j);
                String name=asset.optString("name");
                long size=asset.optLong("size",-1);
                String sha=asset.optString("digest").replaceFirst("^sha256:","").toLowerCase(Locale.ROOT);
                String link=asset.optString("browser_download_url");
                if(!name.toLowerCase(Locale.ROOT).endsWith(".apk") || size<=0 || size>MAX_APK ||
                   !sha.matches("[a-f0-9]{64}") || !link.startsWith("https://github.com/"))continue;
                if(best==null || VersionSupport.compare(tag,best.tag)>0)
                    best=new Candidate(tag,link,sha,size);
            }
        }
        return best;
    }

    public interface Progress {
        void received(long downloaded,long total);
    }

    public File download(Candidate release, Cancel cancellation, Progress onProgress) throws Exception {
        File folder=new File(context.getCacheDir(),"updates");
        if(!folder.isDirectory() && !folder.mkdirs())throw new IOException("Нет папки обновлений");
        File temporary=new File(folder,"downloading.tmp");
        File target=new File(folder,UpdateFileProvider.FILE);
        temporary.delete();
        HttpsURLConnection conn=null;
        try {
            cancellation.check();
            conn=connect(release.downloadUrl,"application/octet-stream",cancellation);
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            try(InputStream input=conn.getInputStream(); OutputStream output=new FileOutputStream(temporary)) {
                byte[] buffer=new byte[32768];long received=0;
                onProgress.received(0,release.size);
                int n;
                while((n=input.read(buffer))!=-1) {
                    cancellation.check();
                    received+=n;
                    if(received>release.size || received>MAX_APK)throw new IOException("Размер APK превышен");
                    output.write(buffer,0,n);
                    digest.update(buffer,0,n);
                    onProgress.received(received,release.size);
                }
                if(received!=release.size)throw new IOException("Неверный размер APK");
            }
            cancellation.check();
            byte[] expected=new byte[32];
            for(int i=0;i<32;i++)expected[i]=(byte)Integer.parseInt(release.digest.substring(i*2,i*2+2),16);
            if(!MessageDigest.isEqual(expected,digest.digest()))throw new IOException("Не совпадает SHA-256 APK");
            verifySigner(temporary);
            cancellation.check();
            if(target.exists() && !target.delete())throw new IOException("Не удалось удалить старое обновление");
            if(!temporary.renameTo(target))throw new IOException("Не удалось сохранить APK");
            return target;
        }finally{
            if(conn!=null)conn.disconnect();
            temporary.delete();
        }
    }

    @SuppressWarnings("deprecation")
    private void verifySigner(File downloaded) throws Exception {
        PackageManager pm=context.getPackageManager();
        PackageInfo current=pm.getPackageInfo(context.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES);
        PackageInfo incoming=pm.getPackageArchiveInfo(downloaded.getAbsolutePath(),PackageManager.GET_SIGNING_CERTIFICATES);
        if(incoming==null || !context.getPackageName().equals(incoming.packageName))
            throw new IOException("APK другого приложения");
        if(incoming.getLongVersionCode()<=current.getLongVersionCode())
            throw new IOException("Версия APK не новее установленной");
        if(current.signingInfo==null || incoming.signingInfo==null)
            throw new IOException("Подпись не найдена");
        android.content.pm.Signature[] a=current.signingInfo.getApkContentsSigners();
        android.content.pm.Signature[] b=incoming.signingInfo.getApkContentsSigners();
        if(a==null || b==null || a.length==0 || a.length!=b.length)
            throw new IOException("Подписи не совпадают");
        for(android.content.pm.Signature signer:a) {
            boolean match=false;
            for(android.content.pm.Signature other:b)
                if(MessageDigest.isEqual(signer.toByteArray(),other.toByteArray()))match=true;
            if(!match)throw new IOException("Подпись APK отличается от установленной");
        }
    }

    public void install(File file) throws Exception {
        if(!file.isFile())throw new IOException("APK не найден");
        if(!context.getPackageManager().canRequestPackageInstalls()){
            Intent settings=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:"+context.getPackageName()));
            settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(settings);
            throw new SecurityException("Разрешите установку APK для приложения, затем вернитесь сюда");
        }
        Intent intent=new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(UpdateFileProvider.uri(context),"application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
    }
}
