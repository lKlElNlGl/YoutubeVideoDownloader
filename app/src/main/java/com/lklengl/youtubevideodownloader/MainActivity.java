package com.lklengl.youtubevideodownloader;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;
import com.yausername.youtubedl_android.mapper.VideoFormat;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native Android UI. Downloads are owned by DownloadService, not this Activity. */
public final class MainActivity extends Activity {
    private static final int PICK_COOKIES=41, PICK_FOLDER=42;
    private final ExecutorService uiWorker=Executors.newSingleThreadExecutor();
    private final ExecutorService updaterWorker=Executors.newSingleThreadExecutor();
    private EditText url, proxy;
    private CheckBox proxyEnabled, cookieEnabled;
    private Spinner mode, quality;
    private TextView preview, folderLabel, cookieLabel, status;
    private ImageView thumbnail;
    private LinearLayout queueList;
    private Button checkAppButton, updateEngineButton, previewButton;
    private GithubUpdater.Cancel updateCancel;
    private File pendingUpdate;
    private boolean updateBusy=false;
    private boolean receiverRegistered=false;
    private final BroadcastReceiver changes=new BroadcastReceiver() {
        @Override public void onReceive(Context context,Intent intent) { refreshQueue(); }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("YoutubeVideoDownloader");
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},49);
        buildScreen();
        loadPreferences();
        Intent received=getIntent();
        if(Intent.ACTION_SEND.equals(received.getAction()) && "text/plain".equals(received.getType())) {
            String shared=received.getStringExtra(Intent.EXTRA_TEXT);
            if(shared!=null)url.setText(shared.trim());
        }
        refreshQueue();
        if(DownloadQueue.hasActive(this)) DownloadService.kick(this);
        checkAppUpdate(false);
    }

    private void buildScreen() {
        ScrollView scroll=new ScrollView(this);
        LinearLayout page=new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16),dp(12),dp(16),dp(30));
        scroll.addView(page);
        setContentView(scroll);

        TextView heading=text("Скачивание видео и аудио с YouTube",21);
        heading.setTextColor(Color.rgb(20,53,91));page.addView(heading);
        label(page,"Ссылка на видео");
        LinearLayout linkRow=new LinearLayout(this);linkRow.setOrientation(LinearLayout.HORIZONTAL);
        url=new EditText(this);url.setSingleLine(true);
        url.setHint("https://www.youtube.com/watch?v=...");
        linkRow.addView(url,new LinearLayout.LayoutParams(0,dp(52),1));
        Button clear=button(linkRow,"✕");clear.setContentDescription("Очистить ссылку");
        clear.setOnClickListener(v->url.setText(""));
        page.addView(linkRow);
        Button paste=button(page,"Вставить ссылку");
        paste.setOnClickListener(v->{
            ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            if(cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount()>0)
                url.setText(cm.getPrimaryClip().getItemAt(0).coerceToText(this));
        });
        previewButton=button(page,"Информация о видео");
        previewButton.setOnClickListener(v->loadPreview());
        thumbnail=new ImageView(this);
        thumbnail.setScaleType(ImageView.ScaleType.FIT_CENTER);
        thumbnail.setVisibility(View.GONE);
        page.addView(thumbnail,new LinearLayout.LayoutParams(-1,dp(170)));
        preview=text("",14);page.addView(preview);

        label(page,"Формат");
        mode=spinner(page,new String[]{"Видео MP4","Аудио MP3 · 192 кбит/с"});
        label(page,"Качество видео");
        quality=spinner(page,new String[]{"Максимальное","2160p","1440p","1080p","720p","480p","360p"});
        mode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int i,long id){
                quality.setEnabled(i==0);
            }
        });

        Button enqueue=button(page,"Добавить в очередь загрузок");
        enqueue.setOnClickListener(v->enqueue());

        status=text("Загрузки продолжатся при сворачивании приложения",13);
        status.setPadding(0,dp(8),0,dp(8));page.addView(status);
        label(page,"Очередь загрузок");
        queueList=new LinearLayout(this);
        queueList.setOrientation(LinearLayout.VERTICAL);
        page.addView(queueList);
        Button clean=button(page,"Очистить завершённые задания");
        clean.setOnClickListener(v->{DownloadQueue.clearFinished(this);refreshQueue();});

        Button settingsToggle=button(page,"⚙ Настройки ▾");
        LinearLayout settings=new LinearLayout(this);
        settings.setOrientation(LinearLayout.VERTICAL);
        settings.setVisibility(View.GONE);
        page.addView(settings);
        settingsToggle.setOnClickListener(v->{
            boolean open=settings.getVisibility()!=View.VISIBLE;
            settings.setVisibility(open?View.VISIBLE:View.GONE);
            settingsToggle.setText(open?"⚙ Настройки ▴":"⚙ Настройки ▾");
        });

        proxyEnabled=new CheckBox(this);proxyEnabled.setText("Использовать HTTP/HTTPS-прокси");
        settings.addView(proxyEnabled);
        proxy=new EditText(this);proxy.setSingleLine(true);proxy.setHint("http://server:8118");
        settings.addView(proxy);
        cookieEnabled=new CheckBox(this);cookieEnabled.setText("Использовать cookies.txt");
        settings.addView(cookieEnabled);
        Button cookies=button(settings,"Импортировать cookies.txt");
        cookies.setOnClickListener(v->{
            Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("*/*");
            startActivityForResult(pick,PICK_COOKIES);
        });
        cookieLabel=text("Файл cookies не выбран",13);settings.addView(cookieLabel);

        label(settings,"Папка сохранения");
        folderLabel=text("",13);settings.addView(folderLabel);
        Button chooseFolder=button(settings,"Выбрать папку сохранения");
        chooseFolder.setOnClickListener(v->{
            Intent tree=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            tree.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(tree,PICK_FOLDER);
        });
        Button defaultFolder=button(settings,"Сохранять в папку Загрузки (по умолчанию)");
        defaultFolder.setOnClickListener(v->{
            getPreferences(MODE_PRIVATE).edit().remove("folderUri").apply();
            folderLabel.setText("Загрузки / YoutubeVideoDownloader");
        });
        Button openFolder=button(settings,"Открыть папку с файлами");
        openFolder.setOnClickListener(v->openFolder());

        label(settings,"Движок загрузки");
        updateEngineButton=button(settings,"Обновить yt-dlp отдельно от APK");
        updateEngineButton.setOnClickListener(v->updateEngine());

        label(settings,"Приложение v"+version());
        checkAppButton=button(settings,"Проверить новую версию приложения");
        checkAppButton.setOnClickListener(v->checkAppUpdate(true));
        Button openReleases=button(settings,"GitHub Releases");
        openReleases.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,
            Uri.parse("https://github.com/lKlElNlGl/YoutubeVideoDownloader/releases/latest"))));
    }
    private int dp(int value){return (int)(getResources().getDisplayMetrics().density*value+0.5f);}
    private TextView text(String value,int size){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);return t;
    }
    private void label(LinearLayout parent,String value){
        TextView t=text(value,15);t.setPadding(0,dp(13),0,dp(4));parent.addView(t);
    }
    private Button button(LinearLayout parent,String value){
        Button b=new Button(this);b.setText(value);parent.addView(b);return b;
    }
    private Spinner spinner(LinearLayout parent,String[] items){
        Spinner s=new Spinner(this);
        s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,items));
        parent.addView(s);return s;
    }
    private void loadPreferences(){
        android.content.SharedPreferences p=getPreferences(MODE_PRIVATE);
        mode.setSelection(p.getInt("mode",0));quality.setSelection(p.getInt("quality",0));
        proxyEnabled.setChecked(p.getBoolean("proxyEnabled",false));
        proxy.setText(p.getString("proxy",""));
        cookieEnabled.setChecked(p.getBoolean("cookiesEnabled",false));
        cookieLabel.setText(new File(getFilesDir(),"cookies.txt").isFile()?"cookies.txt импортирован":"Файл cookies не выбран");
        showFolderLabel();
    }
    private String selectedFolder(){
        return getPreferences(MODE_PRIVATE).getString("folderUri","");
    }
    private void showFolderLabel(){
        String selected=selectedFolder();
        folderLabel.setText(selected.isEmpty()?"Загрузки / YoutubeVideoDownloader":
            "Выбранная папка: "+Uri.parse(selected).getLastPathSegment());
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri chosen=data.getData();
        if(request==PICK_FOLDER){
            try{
                getContentResolver().takePersistableUriPermission(chosen,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getPreferences(MODE_PRIVATE).edit().putString("folderUri",chosen.toString()).apply();
                showFolderLabel();
            }catch(Exception e){error("Выбор папки",e);}
        }else if(request==PICK_COOKIES){
            File file=new File(getFilesDir(),"cookies.txt");
            try(InputStream input=getContentResolver().openInputStream(chosen);
                OutputStream output=new FileOutputStream(file)){
                if(input==null)throw new IOException("Файл недоступен");
                byte[] bytes=new byte[8192];int n;
                while((n=input.read(bytes))!=-1)output.write(bytes,0,n);
                cookieEnabled.setChecked(true);
                cookieLabel.setText("cookies.txt импортирован");
            }catch(Exception e){file.delete();error("Импорт cookies",e);}
        }
    }
    private void enqueue(){
        final String link, proxyUrl;
        try{
            link=DownloadOptions.validateUrl(url.getText().toString());
            proxyUrl=proxyEnabled.isChecked()?DownloadOptions.validateProxy(proxy.getText().toString()):"";
            if(proxyEnabled.isChecked() && proxyUrl.isEmpty())throw new IllegalArgumentException("Укажите адрес прокси");
            if(cookieEnabled.isChecked() && !new File(getFilesDir(),"cookies.txt").isFile())
                throw new IllegalArgumentException("Сначала импортируйте cookies.txt");
            DownloadQueue.enqueue(this,link,mode.getSelectedItemPosition()==1,
                DownloadOptions.QUALITIES[quality.getSelectedItemPosition()],proxyUrl,cookieEnabled.isChecked(),selectedFolder());
        }catch(Exception e){error("Невозможно добавить загрузку",e);return;}
        getPreferences(MODE_PRIVATE).edit().putInt("mode",mode.getSelectedItemPosition())
            .putInt("quality",quality.getSelectedItemPosition()).putBoolean("proxyEnabled",proxyEnabled.isChecked())
            .putString("proxy",proxy.getText().toString()).putBoolean("cookiesEnabled",cookieEnabled.isChecked()).apply();
        refreshQueue();
        try{DownloadService.kick(this);}catch(Exception e){error("Запуск загрузки",e);}
    }
    private void refreshQueue(){
        queueList.removeAllViews();
        List<DownloadQueue.Item> jobs=DownloadQueue.all(this);
        if(jobs.isEmpty()){queueList.addView(text("Очередь пуста",14));return;}
        // Most recent jobs shown first, all pending work remains in persistent FIFO order.
        for(int k=jobs.size()-1;k>=0;k--){
            DownloadQueue.Item j=jobs.get(k);
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(9),dp(6),dp(9),dp(9));
            row.setBackgroundColor(k%2==0?0xFFF2F6FF:0xFFF9FAFE);
            String name=TextUtils.isEmpty(j.filename)?j.url:j.filename;
            TextView title=text(name,13);
            title.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);
            row.addView(title);
            row.addView(text(j.message+" · "+statusName(j.status)+(DownloadQueue.RUNNING.equals(j.status)?" ("+j.progress+"%)":""),13));
            if(DownloadQueue.RUNNING.equals(j.status)){
                ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
                progress.setMax(100);progress.setProgress(j.progress);row.addView(progress);
            }
            if(DownloadQueue.WAITING.equals(j.status) || DownloadQueue.RUNNING.equals(j.status)){
                Button cancel=button(row,"Отменить");
                cancel.setOnClickListener(v->{
                    DownloadService.cancel(this,j.id);refreshQueue();
                });
            }else if(DownloadQueue.ERROR.equals(j.status)||DownloadQueue.CANCELLED.equals(j.status)){
                Button retry=button(row,"Повторить загрузку");
                retry.setOnClickListener(v->{
                    DownloadQueue.retry(this,j.id);refreshQueue();DownloadService.kick(this);
                });
            }else if(DownloadQueue.DONE.equals(j.status)){
                Button folder=button(row,"Открыть папку");
                folder.setOnClickListener(v->openFolderFor(j.directoryUri));
            }
            queueList.addView(row);
        }
    }
    private String statusName(String value){
        switch(value){
            case DownloadQueue.WAITING:return "Ожидает";
            case DownloadQueue.RUNNING:return "Выполняется";
            case DownloadQueue.DONE:return "Готово";
            case DownloadQueue.ERROR:return "Ошибка";
            case DownloadQueue.CANCELLED:return "Отменено";
            default:return value;
        }
    }
    private void openFolder(){openFolderFor(selectedFolder());}
    private void openFolderFor(String path){
        Uri target;
        if(path==null||path.isEmpty()){
            target=DocumentsContract.buildDocumentUri("com.android.externalstorage.documents",
                "primary:Download/YoutubeVideoDownloader");
        }else{
            Uri tree=Uri.parse(path);
            target=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
        }
        try{
            Intent i=new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(target,DocumentsContract.Document.MIME_TYPE_DIR);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        }catch(Exception ignored){
            try{
                Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT);
                picker.setType("*/*");picker.addCategory(Intent.CATEGORY_OPENABLE);
                picker.putExtra(DocumentsContract.EXTRA_INITIAL_URI,target);
                startActivity(picker);
            }catch(Exception e){error("Файловый менеджер",e);}
        }
    }
    private void loadPreview(){
        final String videoUrl;
        try{videoUrl=DownloadOptions.validateUrl(url.getText().toString());}
        catch(Exception e){error("Ссылка",e);return;}
        previewButton.setEnabled(false);
        preview.setText("Получение информации о видео…");
        thumbnail.setVisibility(View.GONE);
        uiWorker.execute(()->{
            try{
                YoutubeDL.getInstance().init(getApplicationContext());
                YoutubeDLRequest request=new YoutubeDLRequest(videoUrl);
                request.addOption("--no-playlist");
                if(proxyEnabled.isChecked()){
                    String p=DownloadOptions.validateProxy(proxy.getText().toString());
                    if(!p.isEmpty())request.addOption("--proxy",p);
                }
                if(cookieEnabled.isChecked() && new File(getFilesDir(),"cookies.txt").isFile())
                    request.addOption("--cookies",new File(getFilesDir(),"cookies.txt").getAbsolutePath());
                VideoInfo info=YoutubeDL.getInstance().getInfo(request);
                String title=info.getTitle()==null?"Без названия":info.getTitle();
                Set<Integer> heights=new TreeSet<>();
                if(info.getFormats()!=null)for(VideoFormat format:info.getFormats())
                    if(format.getHeight()>0 && format.getVcodec()!=null && !"none".equals(format.getVcodec()))
                        heights.add(format.getHeight());
                String details="Название: "+title+"\nДлительность: "+QueueRules.readableDuration(info.getDuration())+
                    "\nДоступное разрешение: "+(heights.isEmpty()?"Не указано":TextUtils.join("p, ",heights)+"p");
                Bitmap picture=loadImage(info.getThumbnail());
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    preview.setText(details);
                    if(picture!=null){thumbnail.setImageBitmap(picture);thumbnail.setVisibility(View.VISIBLE);}
                });
            }catch(Exception e){
                runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())preview.setText("Не удалось получить данные: "+e.getMessage());});
            }finally{runOnUiThread(()->previewButton.setEnabled(true));}
        });
    }
    private Bitmap loadImage(String thumbnailUrl){
        if(thumbnailUrl==null)return null;
        HttpURLConnection c=null;
        try{
            URL u=new URL(thumbnailUrl);
            if(!u.getProtocol().equals("https"))return null;
            c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(8000);c.setReadTimeout(8000);
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buf=new byte[8192];int n;
                while((n=in.read(buf))!=-1){if(out.size()+n>2_000_000)return null;out.write(buf,0,n);}
                byte[] b=out.toByteArray();
                BitmapFactory.Options opts=new BitmapFactory.Options();
                opts.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(b,0,b.length,opts);
                opts.inJustDecodeBounds=false;opts.inSampleSize=Math.max(1,Math.max(opts.outWidth,opts.outHeight)/700);
                return BitmapFactory.decodeByteArray(b,0,b.length,opts);
            }
        }catch(Exception ignored){return null;}
        finally{if(c!=null)c.disconnect();}
    }
    private void updateEngine(){
        if(DownloadQueue.hasActive(this)){
            new AlertDialog.Builder(this).setMessage("Завершите очередь перед обновлением yt-dlp.")
                .setPositiveButton("ОК",null).show();return;
        }
        updateEngineButton.setEnabled(false);updateEngineButton.setText("Обновление yt-dlp…");
        uiWorker.execute(()->{
            try{
                YoutubeDL.getInstance().init(getApplicationContext());
                YoutubeDL.UpdateStatus response=YoutubeDL.getInstance().updateYoutubeDL(
                    getApplicationContext(),YoutubeDL.UpdateChannel.STABLE.INSTANCE);
                runOnUiThread(()->new AlertDialog.Builder(this).setTitle("yt-dlp")
                    .setMessage(response==YoutubeDL.UpdateStatus.DONE?
                        "Движок обновлён успешно.":"Уже установлена последняя версия.")
                    .setPositiveButton("ОК",null).show());
            }catch(Exception e){runOnUiThread(()->error("Обновление yt-dlp",e));}
            finally{runOnUiThread(()->{updateEngineButton.setEnabled(true);updateEngineButton.setText("Обновить yt-dlp отдельно от APK");});}
        });
    }
    private String version(){
        try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}
        catch(Exception ignored){return "?";}
    }
    private void error(String title,Exception e){
        new AlertDialog.Builder(this).setTitle(title).setMessage(e.getMessage())
            .setPositiveButton("ОК",null).show();
    }
    private void checkAppUpdate(boolean manual){
        if(updateBusy)return;
        updateBusy=true;checkAppButton.setEnabled(false);
        checkAppButton.setText("Проверка…");
        updaterWorker.execute(()->{
            try{
                GithubUpdater.Candidate candidate=new GithubUpdater(this).check();
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    if(candidate!=null)
                        new AlertDialog.Builder(this).setTitle("Доступна версия "+candidate.tag)
                            .setMessage("Скачать подписанный APK с GitHub Releases?")
                            .setNegativeButton("Позже",null)
                            .setPositiveButton("Скачать",(d,w)->downloadAppUpdate(candidate)).show();
                    else if(manual)new AlertDialog.Builder(this).setMessage("Установлена последняя версия.")
                        .setPositiveButton("ОК",null).show();
                });
            }catch(Exception e){if(manual)runOnUiThread(()->error("Проверка обновлений",e));}
            finally{runOnUiThread(()->{updateBusy=false;checkAppButton.setEnabled(true);checkAppButton.setText("Проверить новую версию приложения");});}
        });
    }
    private void downloadAppUpdate(GithubUpdater.Candidate candidate){
        updateCancel=new GithubUpdater.Cancel();
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(22),dp(8),dp(22),dp(8));
        TextView percent=text("0%",14);ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);layout.addView(percent);layout.addView(bar);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Скачивание "+candidate.tag)
            .setView(layout).setNegativeButton("Отмена",(d,w)->updateCancel.cancel())
            .setOnCancelListener(d->updateCancel.cancel()).create();
        dialog.show();
        updaterWorker.execute(()->{
            try{
                File apk=new GithubUpdater(this).download(candidate,updateCancel,(n,total)->runOnUiThread(()->{
                    int progress=(int)(n*100/Math.max(1,total));
                    bar.setProgress(progress);percent.setText(progress+"%");
                }));
                pendingUpdate=apk;
                runOnUiThread(()->{
                    dialog.dismiss();
                    new AlertDialog.Builder(this).setTitle("APK проверен")
                        .setMessage("Установить "+candidate.tag+"?")
                        .setNegativeButton("Позже",null)
                        .setPositiveButton("Установить",(d,w)->installAppUpdate()).show();
                });
            }catch(Exception e){runOnUiThread(()->{
                dialog.dismiss();
                if(updateCancel==null || !updateCancel.isCancelled())error("Скачивание APK",e);
            });}
        });
    }
    private void installAppUpdate(){
        if(pendingUpdate==null||!pendingUpdate.exists())return;
        try{new GithubUpdater(this).install(pendingUpdate);}
        catch(Exception e){error("Установка обновления",e);}
    }
    @Override protected void onStart(){
        super.onStart();
        if(!receiverRegistered){
            IntentFilter f=new IntentFilter(DownloadService.ACTION_CHANGED);
            if(Build.VERSION.SDK_INT>=33)registerReceiver(changes,f,Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(changes,f);
            receiverRegistered=true;
        }
        if(queueList!=null)refreshQueue();
    }
    @Override protected void onStop(){
        if(receiverRegistered){unregisterReceiver(changes);receiverRegistered=false;}
        super.onStop();
    }
    @Override protected void onResume(){
        super.onResume();
        if(pendingUpdate!=null&&pendingUpdate.isFile()&&getPackageManager().canRequestPackageInstalls()){
            File cached=pendingUpdate;pendingUpdate=null;
            new AlertDialog.Builder(this).setTitle("Установить загруженное обновление?")
                .setNegativeButton("Позже",null)
                .setPositiveButton("Установить",(d,w)->{pendingUpdate=cached;installAppUpdate();}).show();
        }
    }
    @Override protected void onDestroy(){
        uiWorker.shutdownNow();
        if(updateCancel!=null)updateCancel.cancel();
        updaterWorker.shutdownNow();
        super.onDestroy();
    }
}
