package com.lklengl.youtubevideodownloader;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Environment;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.ffmpeg.FFmpeg;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    private static final int COOKIES_PICKER = 42;
    private static final String JOB = "android-download";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile boolean cancelled = false;
    private EditText urlField, proxyField;
    private Spinner formatField, qualityField;
    private CheckBox proxyEnabled, cookiesEnabled;
    private Button startButton, cancelButton;
    private ProgressBar progress;
    private TextView status, cookieStatus, result;
    private File cookieFile;
    private Uri lastSaved;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("YoutubeVideoDownloader");
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(20,20,20,16);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(outer);
        setContentView(scroll);
        TextView heading = new TextView(this);
        heading.setText("Скачивание видео и аудио с YouTube");
        heading.setTextSize(20); heading.setTextColor(Color.rgb(22,42,74));
        outer.addView(heading);
        label(outer, "Ссылка на видео");
        urlField = new EditText(this); urlField.setSingleLine(true);
        urlField.setHint("https://www.youtube.com/watch?v=...");
        outer.addView(urlField);
        Button paste = button(outer, "Вставить из буфера обмена");
        paste.setOnClickListener(v -> {
            android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            if (cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount()>0)
                urlField.setText(cm.getPrimaryClip().getItemAt(0).coerceToText(this));
        });
        label(outer, "Формат");
        formatField = spinner(outer, new String[]{"Видео MP4","Аудио MP3 (192 кбит/с)"});
        label(outer, "Качество видео");
        qualityField = spinner(outer, new String[]{"Максимальное","2160p","1440p","1080p","720p","480p","360p"});
        formatField.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos,long id) { qualityField.setEnabled(pos==0); }
        });
        label(outer, "Настройки");
        proxyEnabled = new CheckBox(this); proxyEnabled.setText("Использовать HTTP/HTTPS-прокси"); outer.addView(proxyEnabled);
        proxyField = new EditText(this); proxyField.setHint("http://server:8118"); proxyField.setSingleLine(true); outer.addView(proxyField);
        cookiesEnabled = new CheckBox(this); cookiesEnabled.setText("Использовать cookies.txt"); outer.addView(cookiesEnabled);
        Button pick = button(outer, "Выбрать cookies.txt");
        pick.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, COOKIES_PICKER);
        });
        cookieStatus = new TextView(this); cookieStatus.setText("Файл не выбран"); outer.addView(cookieStatus);
        label(outer, "Сохранение: системная папка Загрузки / Downloads");
        progress = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100); outer.addView(progress, new LinearLayout.LayoutParams(-1, 16));
        status = new TextView(this); status.setText("Готово к загрузке."); status.setPadding(0,12,0,12); outer.addView(status);
        startButton = button(outer, "Скачать");
        cancelButton = button(outer, "Отменить"); cancelButton.setEnabled(false);
        startButton.setOnClickListener(v -> start());
        cancelButton.setOnClickListener(v -> { cancelled=true; status.setText("Отмена загрузки…"); YoutubeDL.getInstance().destroyProcessById(JOB); });
        result = new TextView(this); outer.addView(result);
        Button open = button(outer, "Открыть скачанный файл");
        open.setOnClickListener(v -> {
            if(lastSaved==null) { Toast.makeText(this,"Файл пока не загружен",Toast.LENGTH_SHORT).show(); return; }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(lastSaved,getContentResolver().getType(lastSaved));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try { startActivity(intent); } catch(Exception e) { status.setText("Нет приложения для открытия файла"); }
        });
        android.content.SharedPreferences prefs=getPreferences(MODE_PRIVATE);
        formatField.setSelection(prefs.getInt("mode",0));
        qualityField.setSelection(prefs.getInt("quality",0));
        proxyEnabled.setChecked(prefs.getBoolean("proxyEnabled",false));
        proxyField.setText(prefs.getString("proxy",""));
        cookiesEnabled.setChecked(prefs.getBoolean("cookiesEnabled",false));
        cookieFile=new File(getFilesDir(),"cookies.txt");
        if(cookieFile.isFile())cookieStatus.setText("cookies.txt импортирован");
        Intent input=getIntent();
        if(Intent.ACTION_SEND.equals(input.getAction()) && "text/plain".equals(input.getType())) {
            String shared=input.getStringExtra(Intent.EXTRA_TEXT);
            if(shared!=null) urlField.setText(shared);
        }
    }
    private void label(LinearLayout box, String value){TextView t=new TextView(this);t.setText(value);t.setPadding(0,16,0,4);box.addView(t);}
    private Button button(LinearLayout box,String value){ Button b=new Button(this);b.setText(value);box.addView(b);return b;}
    private Spinner spinner(LinearLayout box,String[] values) {
        Spinner s=new Spinner(this);
        ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values);
        s.setAdapter(a);box.addView(s);return s;
    }
    @Override protected void onActivityResult(int req,int code,Intent data) {
        super.onActivityResult(req,code,data);
        if(req!=COOKIES_PICKER || code!=RESULT_OK || data==null || data.getData()==null)return;
        Uri uri=data.getData();
        try(InputStream in=getContentResolver().openInputStream(uri);
            OutputStream out=new FileOutputStream(new File(getFilesDir(),"cookies.txt"))) {
            if(in==null)throw new IOException("Файл недоступен");
            byte[] buffer=new byte[8192];int read;
            while((read=in.read(buffer))!=-1)out.write(buffer,0,read);
            cookieFile=new File(getFilesDir(),"cookies.txt");
            cookieStatus.setText("cookies.txt импортирован");
            cookiesEnabled.setChecked(true);
        }catch(Exception e){cookieStatus.setText("Ошибка импорта: "+e.getMessage());}
    }
    private void state(boolean active) {
        startButton.setEnabled(!active); cancelButton.setEnabled(active);
        formatField.setEnabled(!active);qualityField.setEnabled(!active && formatField.getSelectedItemPosition()==0);
    }
    private void start() {
        if(running.get())return;
        final String url,proxy;
        try {
            url=DownloadOptions.validateUrl(urlField.getText().toString());
            proxy=proxyEnabled.isChecked()?DownloadOptions.validateProxy(proxyField.getText().toString()):"";
            if(proxyEnabled.isChecked() && proxy.isEmpty())throw new IllegalArgumentException("Укажите адрес прокси");
            if(cookiesEnabled.isChecked() && !cookieFile.isFile())throw new IllegalArgumentException("Выберите cookies.txt");
        }catch(IllegalArgumentException ex){status.setText(ex.getMessage());return;}
        final boolean audio=formatField.getSelectedItemPosition()==1;
        final String quality=DownloadOptions.QUALITIES[qualityField.getSelectedItemPosition()];
        final boolean cookies=cookiesEnabled.isChecked();
        getPreferences(MODE_PRIVATE).edit().putInt("mode",formatField.getSelectedItemPosition())
            .putInt("quality",qualityField.getSelectedItemPosition())
            .putBoolean("proxyEnabled",proxyEnabled.isChecked()).putString("proxy",proxyField.getText().toString())
            .putBoolean("cookiesEnabled",cookies).apply();
        if(!running.compareAndSet(false,true))return;
        cancelled=false;lastSaved=null;result.setText("");progress.setProgress(0);state(true);
        status.setText("Подготовка движка загрузки…");
        worker.execute(() -> {
            File temp = new File(getCacheDir(),"job-"+System.nanoTime());
            try {
                if(!temp.mkdirs())throw new IOException("Не удалось создать временную папку");
                YoutubeDL.getInstance().init(getApplicationContext());
                FFmpeg.getInstance().init(getApplicationContext());
                if(cancelled)throw new InterruptedException();
                YoutubeDLRequest request=new YoutubeDLRequest(url);
                request.addOption("--no-playlist");
                request.addOption("--no-overwrites");
                request.addOption("--no-part");
                request.addOption("--no-mtime");
                request.addOption("--newline");
                request.addOption("--restrict-filenames");
                request.addOption("--print","after_move:filepath");
                request.addOption("-P",temp.getAbsolutePath());
                request.addOption("-o",audio?"%(title)s [%(id)s] [MP3 192kbps].%(ext)s":"%(title)s [%(id)s] [%(height)sp].%(ext)s");
                request.addOption("-f",DownloadOptions.format(audio,quality));
                if(audio) {
                    request.addOption("-x");request.addOption("--audio-format","mp3");
                    request.addOption("--audio-quality","192K");
                }else{request.addOption("--merge-output-format","mp4");}
                if(!proxy.isEmpty())request.addOption("--proxy",proxy);
                if(cookies)request.addOption("--cookies",cookieFile.getAbsolutePath());
                runOnUiThread(() -> status.setText("Загрузка…"));
                YoutubeDL.getInstance().execute(request,JOB,(percent,eta,line) -> {
                    runOnUiThread(() -> {
                        progress.setProgress(Math.max(0,Math.min(100,Math.round(percent))));
                        if(line!=null)status.setText("Загрузка: "+Math.round(percent)+"% · осталось "+Math.max(0,eta)+" с");
                    });
                    return kotlin.Unit.INSTANCE;
                });
                if(cancelled)throw new InterruptedException();
                File downloaded=findResult(temp);
                if(downloaded==null)throw new IOException("Итоговый файл не найден");
                Uri saved=publish(downloaded,audio);
                lastSaved=saved;
                runOnUiThread(() -> {progress.setProgress(100);status.setText("Готово! Файл сохранён в Downloads.");result.setText(downloaded.getName());});
            }catch(Exception ex){
                String message=cancelled?"Загрузка отменена.":"Ошибка: "+ex.getMessage();
                runOnUiThread(() -> status.setText(message));
            }finally{
                cleanup(temp);running.set(false);
                runOnUiThread(() -> state(false));
            }
        });
    }
    private File findResult(File dir) {
        File[] items=dir.listFiles(f->f.isFile()&&!f.getName().endsWith(".part")&&!f.getName().endsWith(".ytdl"));
        if(items==null||items.length==0)return null;
        Arrays.sort(items,Comparator.comparingLong(File::lastModified).reversed());
        return items[0];
    }
    private Uri publish(File file,boolean audio) throws IOException {
        ContentValues values=new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME,file.getName());
        values.put(MediaStore.MediaColumns.MIME_TYPE,audio?"audio/mpeg":"video/mp4");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/YoutubeVideoDownloader");
        values.put(MediaStore.MediaColumns.IS_PENDING,1);
        Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
        if(uri==null)throw new IOException("MediaStore недоступен");
        boolean done=false;
        try(InputStream in=new FileInputStream(file);OutputStream out=getContentResolver().openOutputStream(uri)) {
            if(out==null)throw new IOException("Не удалось сохранить файл");
            byte[] buffer=new byte[65536];int read;
            while((read=in.read(buffer))!=-1) {
                if(cancelled)throw new IOException("Загрузка отменена");
                out.write(buffer,0,read);
            }
            done=true;
        }finally {
            if(!done)getContentResolver().delete(uri,null,null);
        }
        values.clear();values.put(MediaStore.MediaColumns.IS_PENDING,0);
        getContentResolver().update(uri,values,null,null);
        return uri;
    }
    private void cleanup(File dir) {
        File[] files=dir.listFiles();if(files!=null)for(File f:files)if(f.isDirectory())cleanup(f);else f.delete();dir.delete();
    }
    @Override protected void onDestroy() {
        if(running.get()){cancelled=true;YoutubeDL.getInstance().destroyProcessById(JOB);}
        worker.shutdownNow();
        super.onDestroy();
    }
}
