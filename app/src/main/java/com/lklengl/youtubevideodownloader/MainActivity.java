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
    private static final int PICK_FOLDER=42;
    private final ExecutorService uiWorker=Executors.newSingleThreadExecutor();
    private final ExecutorService updaterWorker=Executors.newSingleThreadExecutor();
    private EditText url, proxy;
    private CheckBox proxyEnabled;
    private Spinner mode, quality;
    private TextView preview, folderLabel, status;
    private ImageView thumbnail;
    private LinearLayout queueList;
    private Button checkAppButton, updateEngineButton, previewButton, defaultFolderButton;
    private SwipeNavigationScrollView mainScroll;
    private LinearLayout[] pagePanels;
    private Button[] pageTabs;
    private int currentPage=0;
    private UpdateDialogController updateDialog;
    private boolean loadingPlaylist=false;
    private File pendingUpdate;
    private boolean updateBusy=false;
    private boolean receiverRegistered=false;
    private final BroadcastReceiver changes=new BroadcastReceiver() {
        @Override public void onReceive(Context context,Intent intent) { refreshQueue(); }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        ThemeStyle.initialize(this);
        super.onCreate(savedInstanceState);
        UpdateAlerts.schedule(getApplicationContext());
        // Remove unused legacy credentials when migrating from v0.3.0.
        new File(getFilesDir(),"cookies.txt").delete();
        getPreferences(MODE_PRIVATE).edit().remove("cookiesEnabled").apply();
        currentPage=getPreferences(MODE_PRIVATE).getInt("page",0);
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
        mainScroll=new SwipeNavigationScrollView(this);
        mainScroll.setFillViewport(true);
        mainScroll.setBackgroundColor(ThemeStyle.background(this));
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16),dp(16),dp(16),dp(28));
        mainScroll.addView(page);setContentView(mainScroll);

        TextView heading=text("YoutubeVideoDownloader",23);
        heading.setTypeface(null,android.graphics.Typeface.BOLD);
        heading.setPadding(dp(2),dp(2),dp(2),0);page.addView(heading);
        TextView subtitle=text("СКАЧИВАНИЕ ВИДЕО И МУЗЫКИ",11);
        subtitle.setLetterSpacing(0.08f);subtitle.setTextColor(ThemeStyle.subdued(this));
        subtitle.setPadding(dp(3),0,0,dp(13));page.addView(subtitle);

        LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(tabs);
        pageTabs=new Button[]{new Button(this),new Button(this),new Button(this)};
        String[] titles={"▶ Загрузка","☷ Очередь","⚙ Настройки"};
        for(int i=0;i<3;i++){
            Button tab=pageTabs[i];tab.setText(titles[i]);
            ThemeStyle.styleButton(tab,i==currentPage);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,dp(48),1);
            params.setMargins(dp(2),dp(4),dp(2),dp(11));tabs.addView(tab,params);
            final int destination=i;
            tab.setOnClickListener(v->showPage(destination));
        }
        pagePanels=new LinearLayout[3];
        for(int i=0;i<3;i++){
            LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);
            pagePanels[i]=panel;page.addView(panel);
        }
        mainScroll.setPageChanged(this::showPage);

        LinearLayout download=pagePanels[0];
        LinearLayout linkCard=ThemeStyle.card(download,"Ссылка на YouTube");
        label(linkCard,"Ссылка на видео или плейлист");
        LinearLayout linkRow=new LinearLayout(this);linkRow.setOrientation(LinearLayout.HORIZONTAL);
        url=new EditText(this);ThemeStyle.styleField(url);
        url.setHint("https://www.youtube.com/watch?v=...");
        linkRow.addView(url,new LinearLayout.LayoutParams(0,dp(51),1));
        Button clear=new Button(this);clear.setText("✕");
        clear.setContentDescription("Очистить ссылку");ThemeStyle.styleButton(clear,false);
        LinearLayout.LayoutParams clearParams=new LinearLayout.LayoutParams(dp(51),dp(51));
        clearParams.setMargins(dp(9),0,0,0);linkRow.addView(clear,clearParams);
        clear.setOnClickListener(v->url.setText(""));
        linkCard.addView(linkRow);
        Button paste=button(linkCard,"Вставить ссылку");
        paste.setOnClickListener(v->{
            ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            if(cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount()>0)
                url.setText(cm.getPrimaryClip().getItemAt(0).coerceToText(this));
        });
        previewButton=button(linkCard,"Информация о видео / плейлисте");
        previewButton.setOnClickListener(v->{
            if(PlaylistSupport.isPlaylist(url.getText().toString()))loadPlaylist(false);
            else loadPreview();
        });
        thumbnail=new ImageView(this);thumbnail.setScaleType(ImageView.ScaleType.FIT_CENTER);
        thumbnail.setVisibility(View.GONE);linkCard.addView(thumbnail,new LinearLayout.LayoutParams(-1,dp(170)));
        preview=text("",14);preview.setPadding(dp(4),dp(6),dp(4),0);linkCard.addView(preview);

        LinearLayout formatCard=ThemeStyle.card(download,"Параметры загрузки");
        label(formatCard,"Формат");
        mode=spinner(formatCard,new String[]{"Видео MP4","Аудио MP3 · 192 кбит/с"});
        label(formatCard,"Качество видео");
        quality=spinner(formatCard,new String[]{"Максимальное","2160p","1440p","1080p","720p","480p","360p"});
        mode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent){}
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,View view,int i,long id){
                quality.setEnabled(i==0);
            }
        });

        LinearLayout actions=ThemeStyle.card(download,"Скачивание");
        Button immediate=button(actions,"Скачать без очереди");
        ThemeStyle.styleButton(immediate,true);
        immediate.setOnClickListener(v->{
            if(PlaylistSupport.isPlaylist(url.getText().toString()))loadPlaylist(true);
            else enqueue(true);
        });
        Button enqueue=button(actions,"Добавить в очередь");
        enqueue.setOnClickListener(v->{
            if(PlaylistSupport.isPlaylist(url.getText().toString()))loadPlaylist(false);
            else enqueue(false);
        });

        LinearLayout queueCard=ThemeStyle.card(pagePanels[1],"Очередь загрузок");
        status=text("Загрузки продолжаются при сворачивании приложения",13);
        status.setTextColor(ThemeStyle.subdued(this));status.setPadding(0,0,0,dp(8));
        queueCard.addView(status);
        queueList=new LinearLayout(this);queueList.setOrientation(LinearLayout.VERTICAL);
        queueCard.addView(queueList);
        Button clean=button(queueCard,"Очистить завершённые задания");
        clean.setOnClickListener(v->{DownloadQueue.clearFinished(this);refreshQueue();});

        LinearLayout settingsPage=pagePanels[2];
        LinearLayout look=ThemeStyle.card(settingsPage,"Внешний вид");
        label(look,"Тема оформления");
        Spinner themePicker=spinner(look,ThemeStyle.MODES);
        themePicker.setSelection(getSharedPreferences("design",MODE_PRIVATE).getInt("theme",2));
        themePicker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent){}
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,View view,int value,long id){
                if(value!=getSharedPreferences("design",MODE_PRIVATE).getInt("theme",2)){
                    saveDraft();
                    getSharedPreferences("design",MODE_PRIVATE).edit().putInt("theme",value).apply();
                    recreate();
                }
            }
        });

        LinearLayout network=ThemeStyle.card(settingsPage,"Сетевые параметры");
        proxyEnabled=new CheckBox(this);proxyEnabled.setText("Использовать HTTP/HTTPS-прокси");
        proxyEnabled.setTextColor(ThemeStyle.text(this));network.addView(proxyEnabled);
        proxy=new EditText(this);ThemeStyle.styleField(proxy);proxy.setHint("http://server:8118");
        network.addView(proxy,new LinearLayout.LayoutParams(-1,dp(50)));

        LinearLayout save=ThemeStyle.card(settingsPage,"Сохранение файлов");
        label(save,"Папка сохранения");
        folderLabel=text("",14);folderLabel.setPadding(dp(3),dp(4),dp(3),dp(8));
        save.addView(folderLabel);
        Button chooseFolder=button(save,"Изменить папку");
        chooseFolder.setOnClickListener(v->{
            Intent tree=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            tree.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(tree,PICK_FOLDER);
        });
        defaultFolderButton=button(save,"Вернуть папку по умолчанию");
        defaultFolderButton.setOnClickListener(v->{
            getPreferences(MODE_PRIVATE).edit().remove("folderUri").apply();
            showFolderLabel();
        });
        Button openFolder=button(save,"Открыть папку с файлами");
        openFolder.setOnClickListener(v->openFolder());

        LinearLayout updates=ThemeStyle.card(settingsPage,"Обновления");
        label(updates,"Движок yt-dlp");
        updateEngineButton=button(updates,"Обновить yt-dlp");
        updateEngineButton.setOnClickListener(v->updateEngine());
        label(updates,"Приложение");
        checkAppButton=button(updates,"Проверить новую версию");
        checkAppButton.setOnClickListener(v->checkAppUpdate(true));

        LinearLayout about=ThemeStyle.card(settingsPage,"О приложении");
        TextView release=text("YoutubeVideoDownloader · v"+version(),14);
        release.setTextColor(ThemeStyle.subdued(this));about.addView(release);
        Button openReleases=button(about,"GitHub Releases");
        openReleases.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,
            Uri.parse("https://github.com/lKlElNlGl/YoutubeVideoDownloader/releases/latest"))));

        showPage(Math.max(0,Math.min(2,currentPage)));
    }

    private void showPage(int index){
        currentPage=Math.max(0,Math.min(2,index));
        for(int i=0;i<pagePanels.length;i++){
            pagePanels[i].setVisibility(i==currentPage?View.VISIBLE:View.GONE);
            ThemeStyle.styleButton(pageTabs[i],i==currentPage);
        }
        mainScroll.setPageIndex(currentPage);
        mainScroll.scrollTo(0,0);
        getPreferences(MODE_PRIVATE).edit().putInt("page",currentPage).apply();
    }

    private void saveDraft(){
        if(url==null||proxy==null)return;
        getPreferences(MODE_PRIVATE).edit()
            .putString("draftUrl",url.getText().toString())
            .putString("proxy",proxy.getText().toString())
            .putBoolean("proxyEnabled",proxyEnabled.isChecked())
            .putInt("mode",mode.getSelectedItemPosition())
            .putInt("quality",quality.getSelectedItemPosition()).apply();
    }
    private int dp(int value){return (int)(getResources().getDisplayMetrics().density*value+0.5f);}
    private TextView text(String value,int size){
        TextView t=new TextView(this);
        t.setText(value);t.setTextSize(size);t.setTextColor(ThemeStyle.text(this));return t;
    }
    private void label(LinearLayout parent,String value){
        TextView t=text(value,15);t.setTextColor(ThemeStyle.subdued(this));
        t.setPadding(0,dp(13),0,dp(4));parent.addView(t);
    }
    private Button button(LinearLayout parent,String value){
        Button b=new Button(this);b.setText(value);ThemeStyle.styleButton(b,false);
        parent.addView(b);return b;
    }
    private Spinner spinner(LinearLayout parent,String[] items){
        Spinner s=new Spinner(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,items){
            @Override public View getView(int position,View convertView,android.view.ViewGroup parent){
                TextView v=(TextView)super.getView(position,convertView,parent);
                v.setTextColor(ThemeStyle.text(MainActivity.this));v.setTextSize(14);return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);ThemeStyle.styleSpinner(s);
        parent.addView(s);return s;
    }
    private void loadPreferences(){
        android.content.SharedPreferences p=getPreferences(MODE_PRIVATE);
        mode.setSelection(p.getInt("mode",0));quality.setSelection(p.getInt("quality",0));
        proxyEnabled.setChecked(p.getBoolean("proxyEnabled",false));
        proxy.setText(p.getString("proxy",""));
        url.setText(p.getString("draftUrl",""));
        showFolderLabel();
    }
    private String selectedFolder(){
        return getPreferences(MODE_PRIVATE).getString("folderUri","");
    }
    private void showFolderLabel(){
        String selected=selectedFolder();
        folderLabel.setText(selected.isEmpty()?"Загрузки / YoutubeVideoDownloader":
            "Выбрана: "+Uri.parse(selected).getLastPathSegment());
        defaultFolderButton.setVisibility(FolderRules.isCustom(selected)?View.VISIBLE:View.GONE);
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
        }
    }
    private void enqueue(boolean priority){
        final String link, proxyUrl;
        try{
            link=DownloadOptions.validateUrl(url.getText().toString());
            proxyUrl=proxyEnabled.isChecked()?DownloadOptions.validateProxy(proxy.getText().toString()):"";
            if(proxyEnabled.isChecked() && proxyUrl.isEmpty())throw new IllegalArgumentException("Укажите адрес прокси");
            DownloadQueue.enqueue(this,link,mode.getSelectedItemPosition()==1,
                DownloadOptions.QUALITIES[quality.getSelectedItemPosition()],proxyUrl,selectedFolder(),priority);
        }catch(Exception e){error("Невозможно добавить загрузку",e);return;}
        saveDraft();
        refreshQueue();
        try{
            DownloadService.kick(this);
            Toast.makeText(this,priority?"Приоритетная загрузка добавлена":"Добавлено в очередь",Toast.LENGTH_SHORT).show();
        }catch(Exception e){error("Запуск загрузки",e);}
    }
    private void refreshQueue(){
        queueList.removeAllViews();
        List<DownloadQueue.Item> jobs=DownloadQueue.all(this);
        if(jobs.isEmpty()){queueList.addView(text("Очередь пуста",14));return;}
        Map<String,int[]> playlists=new LinkedHashMap<>();
        for(DownloadQueue.Item item:jobs){
            if(item.playlistTitle==null || item.playlistTitle.isEmpty())continue;
            int[] counts=playlists.computeIfAbsent(item.playlistTitle,k->new int[2]);
            counts[1]++;
            if(DownloadQueue.DONE.equals(item.status))counts[0]++;
        }
        for(Map.Entry<String,int[]> group:playlists.entrySet()){
            int[] counts=group.getValue();
            TextView header=text("Плейлист: "+group.getKey()+" · "+counts[0]+"/"+counts[1]+" завершено",14);
            header.setPadding(dp(8),dp(8),dp(8),dp(8));
            header.setTextColor(ThemeStyle.RED);queueList.addView(header);
        }
        // Most recent jobs shown first, all pending work remains in persistent FIFO order.
        for(int k=jobs.size()-1;k>=0;k--){
            DownloadQueue.Item j=jobs.get(k);
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(9),dp(6),dp(9),dp(9));
            row.setBackground(ThemeStyle.rounded(ThemeStyle.cardColor(this),
                ThemeStyle.isDark(this)?0xFF424750:0xFFD7DAE1,dp(13)));
            LinearLayout.LayoutParams cardParams=new LinearLayout.LayoutParams(-1,-2);
            cardParams.setMargins(0,dp(7),0,dp(7));
            row.setLayoutParams(cardParams);
            String name=TextUtils.isEmpty(j.filename)?
                ((j.title==null || j.title.isEmpty())?j.url:j.title):j.filename;
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
    private void loadPlaylist(boolean priority){
        if(loadingPlaylist)return;
        final String link, proxyValue;

        try{
            link=DownloadOptions.validateUrl(url.getText().toString());
            if(!PlaylistSupport.isPlaylist(link))throw new IllegalArgumentException("Нужна ссылка на плейлист YouTube");
            proxyValue=proxyEnabled.isChecked()?DownloadOptions.validateProxy(proxy.getText().toString()):"";
            if(proxyEnabled.isChecked() && proxyValue.isEmpty())throw new IllegalArgumentException("Укажите адрес прокси");
        }catch(Exception e){error("Плейлист",e);return;}
        final boolean audio=mode.getSelectedItemPosition()==1;
        final String resolution=DownloadOptions.QUALITIES[quality.getSelectedItemPosition()];
        final String directory=selectedFolder();
        loadingPlaylist=true;previewButton.setEnabled(false);
        preview.setText("Получение списка видео плейлиста…");
        uiWorker.execute(()->{
            try {
                PlaylistRepository.Playlist data=PlaylistRepository.fetch(getApplicationContext(),link,proxyValue);
                runOnUiThread(()->{
                    loadingPlaylist=false;previewButton.setEnabled(true);
                    preview.setText("Плейлист: "+data.title+"\nДоступно видео: "+data.entries.size()+
                        (data.reportedCount>data.entries.size()?" (часть видео недоступна)":""));
                    showPlaylistSelection(data,audio,resolution,proxyValue,directory,priority);
                });
            }catch(Exception ex){
                runOnUiThread(()->{
                    loadingPlaylist=false;previewButton.setEnabled(true);
                    error("Не удалось открыть плейлист",ex);
                    preview.setText("Не удалось прочитать плейлист");
                });
            }
        });
    }
    private void showPlaylistSelection(PlaylistRepository.Playlist data,
            boolean audio,String resolution,String proxyValue,String directory,boolean priority){
        int count=data.entries.size();
        CharSequence[] labels=new CharSequence[count];
        boolean[] checked=new boolean[count];
        java.util.Arrays.fill(checked,true);
        for(int i=0;i<count;i++){
            PlaylistRepository.Entry entry=data.entries.get(i);
            labels[i]=(i+1)+". "+entry.title+
                (entry.duration>=0?" · "+QueueRules.readableDuration(entry.duration):"");
        }
        AlertDialog dialog=new AlertDialog.Builder(this)
            .setTitle(data.title+" ("+count+" видео)")
            .setMultiChoiceItems(labels,checked,(d,which,value)->checked[which]=value)
            .setNegativeButton("Отмена",null)
            .setNeutralButton("Выбрать все",null)
            .setPositiveButton(priority?"Скачать без очереди":"Добавить в очередь",null).create();
        dialog.setOnShowListener(d->{
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{
                boolean all=true;
                for(boolean value:checked)if(!value){all=false;break;}
                for(int i=0;i<count;i++){checked[i]=!all;dialog.getListView().setItemChecked(i,!all);}
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setText(all?"Выбрать все":"Снять выбор");
            });
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                try{
                    List<Boolean> selected=new ArrayList<>();
                    for(boolean value:checked)selected.add(value);
                    int added=DownloadQueue.enqueuePlaylist(this,data,selected,audio,resolution,proxyValue,directory,priority);
                    dialog.dismiss();refreshQueue();
                    DownloadService.kick(this);
                    Toast.makeText(this,"Добавлено в очередь: "+added,Toast.LENGTH_LONG).show();
                }catch(Exception e){error("Очередь плейлиста",e);}
            });
        });
        dialog.show();
    }
    private void loadPreview(){
        final String videoUrl;
        try{videoUrl=DownloadOptions.validateUrl(url.getText().toString());}
        catch(Exception e){error("Ссылка",e);return;}
        previewButton.setEnabled(false);
        preview.setText("Получение информации о видео…");
        thumbnail.setVisibility(View.GONE);
        final String effectiveProxy=proxyEnabled.isChecked()?proxy.getText().toString():"";

        uiWorker.execute(()->{
            try{
                YoutubeDL.getInstance().init(getApplicationContext());
                YoutubeDLRequest request=new YoutubeDLRequest(videoUrl);
                request.addOption("--no-playlist");
                if(!effectiveProxy.isEmpty()){
                    String p=DownloadOptions.validateProxy(effectiveProxy);
                    if(!p.isEmpty())request.addOption("--proxy",p);
                }
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
                    if(candidate!=null){
                        UpdateAlerts.notifyIfNew(this,candidate);
                        updateDialog=new UpdateDialogController(this,candidate,apk->{
                            pendingUpdate=apk;
                            installAppUpdate();
                        });
                        updateDialog.show();
                    }
                    else if(manual)new AlertDialog.Builder(this).setMessage("Установлена последняя версия.")
                        .setPositiveButton("ОК",null).show();
                });
            }catch(Exception e){if(manual)runOnUiThread(()->error("Проверка обновлений",e));}
            finally{runOnUiThread(()->{updateBusy=false;checkAppButton.setEnabled(true);checkAppButton.setText("Проверить новую версию приложения");});}
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
        saveDraft();
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
        if(updateDialog!=null)updateDialog.close();
        updaterWorker.shutdownNow();
        super.onDestroy();
    }
}
