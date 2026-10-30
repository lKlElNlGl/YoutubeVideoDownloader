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
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Gravity;
import android.text.TextWatcher;
import android.text.Editable;
import android.graphics.drawable.Drawable;
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
    private QualityChoices qualityChoices=QualityChoices.fallback();
    private FormatChoices formatChoices=FormatChoices.fallback();
    private String queueSignature=null;
    private ProgressBar activeQueueBar;
    private TextView activeQueueInfo;
    private TextView preview, folderLabel, previewTitle, previewDuration, previewQuality, previewHeader, previewAllQualities;
    private Button previewDetailsButton;
    private String previewErrorLog="";
    private LinearLayout previewCard;
    private ImageView thumbnail;
    private LinearLayout queueList;
    private Button checkAppButton, updateEngineButton, previewButton, defaultFolderButton;
    private ScrollView mainScroll;
    private float swipeStartX,swipeStartY;
    private boolean swipeEligible;
    private PreviewSnapshot previewSnapshot;
    private Bitmap previewBitmap;
    private LinearLayout[] pagePanels;
    private LinearLayout[] pageTabs;
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
        EngineUpdateWorker.schedule(getApplicationContext());
        // Remove unused legacy credentials when migrating from v0.3.0.
        new File(getFilesDir(),"cookies.txt").delete();
        getPreferences(MODE_PRIVATE).edit().remove("cookiesEnabled").apply();
        // A fresh launch always opens Downloads; Activity recreation keeps the current tab.
        Object previous=getLastNonConfigurationInstance();
        currentPage=previous instanceof Integer?(Integer)previous:0;
        getPreferences(MODE_PRIVATE).edit().remove("page").apply();
        setTitle("YoutubeVideoDownloader");
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},49);
        buildScreen();
        loadPreferences();
        restorePreview();
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
        mainScroll=new ScrollView(this);
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
        pageTabs=new LinearLayout[]{new LinearLayout(this),new LinearLayout(this),new LinearLayout(this)};
        String[] titles={"Загрузка","Очередь","Настройки"};
        String[] tabIcons={"play","queue","settings"};
        for(int i=0;i<3;i++){
            LinearLayout tab=pageTabs[i];
            ThemeStyle.styleTab(tab,i==currentPage,tabIcons[i],titles[i]);
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
        // Activity-level dispatchTouchEvent observes gestures even if a child consumes them.

        LinearLayout download=pagePanels[0];
        LinearLayout linkCard=ThemeStyle.card(download,"Ссылка на YouTube");
        label(linkCard,"Ссылка на видео или плейлист");
        FrameLayout linkRow=new FrameLayout(this);
        url=new EditText(this);ThemeStyle.styleField(url);
        url.setHint("https://www.youtube.com/watch?v=...");
        url.setHintTextColor(ThemeStyle.isDark(this)?0xFF303847:0xFFD9DADE);
        url.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence a,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence text,int start,int before,int count){
                previewErrorLog="";updatePreviewMessage("");renderPreview(false);
            }
            @Override public void afterTextChanged(Editable value){}
        });
        url.setPadding(dp(12),0,dp(51),0);
        linkRow.addView(url,new FrameLayout.LayoutParams(-1,dp(51)));
        ImageView clear=new ImageView(this);
        clear.setImageDrawable(new UiIcons("close",ThemeStyle.subdued(this),dp(18)));
        clear.setContentDescription("Очистить ссылку");
        FrameLayout.LayoutParams clearParams=new FrameLayout.LayoutParams(dp(49),dp(49),Gravity.END|Gravity.CENTER_VERTICAL);
        clear.setPadding(dp(14),dp(14),dp(14),dp(14));
        linkRow.addView(clear,clearParams);
        clear.setOnClickListener(v->{
            if(previewSnapshot==null){url.setText("");return;}
            ThemeStyle.dialog(this).setTitle("Очистить ссылку и информацию о видео?")
                .setMessage("Вместе со ссылкой будут удалены название, обложка, длительность, варианты качества и формата. Для повторного получения информации потребуется новый запрос к YouTube.")
                .setNegativeButton("Отмена",null)
                .setPositiveButton("Очистить",(d,w)->{
                    previewSnapshot=null;previewBitmap=null;
                    getPreferences(MODE_PRIVATE).edit()
                        .remove("previewSource").remove("previewTitle").remove("previewPlaylist")
                        .remove("previewCount").remove("previewDuration")
                        .remove("previewQualities").remove("previewFormats").apply();
                    new File(getFilesDir(),"preview-thumbnail.jpg").delete();
                    url.setText("");
                    renderPreview(false);
                }).show();
        });
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
        preview=text("",14);preview.setPadding(dp(4),0,dp(4),0);
        preview.setVisibility(View.GONE);
        preview.setTextColor(ThemeStyle.subdued(this));linkCard.addView(preview);
        previewDetailsButton=button(linkCard,"Подробности");
        previewDetailsButton.setVisibility(View.GONE);
        previewDetailsButton.setOnClickListener(v->showTechnicalDetails(previewErrorLog));
        previewCard=new LinearLayout(this);previewCard.setOrientation(LinearLayout.VERTICAL);
        previewCard.setPadding(dp(12),dp(13),dp(12),dp(13));
        previewCard.setBackground(ThemeStyle.rounded(ThemeStyle.field(this),ThemeStyle.border(this),dp(12)));
        LinearLayout.LayoutParams pc=new LinearLayout.LayoutParams(-1,-2);
        pc.setMargins(0,dp(8),0,dp(5));linkCard.addView(previewCard,pc);
        previewHeader=text("ИНФОРМАЦИЯ О ВИДЕО",12);
        previewHeader.setTextColor(ThemeStyle.subdued(this));previewHeader.setLetterSpacing(0.06f);
        previewHeader.setPadding(0,0,0,dp(9));previewCard.addView(previewHeader);
        thumbnail=new ImageView(this);thumbnail.setScaleType(ImageView.ScaleType.FIT_CENTER);
        thumbnail.setVisibility(View.GONE);
        previewCard.addView(thumbnail,new LinearLayout.LayoutParams(-1,dp(164)));
        previewTitle=text("",16);previewTitle.setTypeface(null,android.graphics.Typeface.BOLD);
        previewTitle.setPadding(0,dp(9),0,dp(12));previewCard.addView(previewTitle);
        LinearLayout stats=new LinearLayout(this);stats.setOrientation(LinearLayout.HORIZONTAL);
        previewCard.addView(stats);
        previewDuration=statView(stats,"clock");
        previewQuality=statView(stats,"quality");
        previewAllQualities=text("",12);
        previewAllQualities.setTextColor(ThemeStyle.subdued(this));
        previewAllQualities.setPadding(dp(4),dp(9),dp(4),0);
        previewCard.addView(previewAllQualities);
        previewCard.setVisibility(View.GONE);

        LinearLayout formatCard=ThemeStyle.card(download,"Параметры загрузки");
        label(formatCard,"Формат");
        mode=spinner(formatCard,formatChoices.labels().toArray(new String[0]));
        label(formatCard,"Качество видео");
        quality=spinner(formatCard,qualityChoices.labels().toArray(new String[0]));
        mode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent){}
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,View view,int i,long id){
                quality.setEnabled(!formatChoices.isAudio(selectedFormat()));
            }
        });

        LinearLayout actions=ThemeStyle.card(download,"Скачивание");
        Button immediate=button(actions,"Скачать без очереди");
        ThemeStyle.styleButton(immediate,true);ThemeStyle.icon(immediate,"download",19);
        immediate.setOnClickListener(v->{
            if(PlaylistSupport.isPlaylist(url.getText().toString()))loadPlaylist(true);
            else enqueue(true);
        });
        Button enqueue=button(actions,"Добавить в очередь");
        ThemeStyle.icon(enqueue,"queue",19);
        enqueue.setOnClickListener(v->{
            if(PlaylistSupport.isPlaylist(url.getText().toString()))loadPlaylist(false);
            else enqueue(false);
        });

        queueList=new LinearLayout(this);queueList.setOrientation(LinearLayout.VERTICAL);
        pagePanels[1].addView(queueList);
        Button clean=button(pagePanels[1],"Очистить завершённые");
        clean.setOnClickListener(v->{
            for(DownloadQueue.Item j:DownloadQueue.all(this))
                if(DownloadQueue.DONE.equals(j.status)||DownloadQueue.ERROR.equals(j.status)||
                    DownloadQueue.CANCELLED.equals(j.status)){
                    QueueThumbnails.delete(this,j.id);
                    DownloadService.discardPartial(this,j.id);
                }
            DownloadQueue.clearFinished(this);queueSignature=null;refreshQueue();
        });

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
        label(updates,"Загрузка с YouTube");
        updateEngineButton=button(updates,"Обновить компонент работы с YouTube");
        updateEngineButton.setTextSize(13);
        updateEngineButton.setSingleLine(false);updateEngineButton.setMaxLines(2);
        // Normal button height is shared with other settings actions.
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

    private TextView statView(LinearLayout parent,String icon){
        LinearLayout cell=new LinearLayout(this);cell.setOrientation(LinearLayout.HORIZONTAL);
        cell.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,dp(36),1);
        params.setMargins(0,0,dp(6),0);
        cell.setPadding(dp(7),0,dp(4),0);
        cell.setBackground(ThemeStyle.rounded(ThemeStyle.field(this),
            icon.equals("quality")?0xFF327DB4:ThemeStyle.border(this),dp(8)));
        parent.addView(cell,params);
        ImageView symbol=new ImageView(this);
        symbol.setImageDrawable(new UiIcons(icon,icon.equals("clock")?ThemeStyle.RED:0xFF39A8DD,dp(16)));
        cell.addView(symbol,new LinearLayout.LayoutParams(dp(16),dp(16)));
        TextView value=text("",12);
        value.setMaxLines(1);value.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(0,-2,1);
        vp.setMargins(dp(6),0,0,0);cell.addView(value,vp);
        return value;
    }

    /** Observe complete gestures at Activity level; child widgets cannot swallow horizontal swipes. */
    @Override public boolean dispatchTouchEvent(MotionEvent e){
        if(mainScroll==null)return super.dispatchTouchEvent(e);
        int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){
            swipeStartX=e.getRawX();swipeStartY=e.getRawY();
            swipeEligible=!overEditable(mainScroll,e.getRawX(),e.getRawY());
        }else if(action==MotionEvent.ACTION_POINTER_DOWN || action==MotionEvent.ACTION_CANCEL){
            swipeEligible=false;
        }else if(action==MotionEvent.ACTION_UP && swipeEligible){
            int destination=SwipeRules.target(currentPage,e.getRawX()-swipeStartX,
                e.getRawY()-swipeStartY,dp(40));
            swipeEligible=false;
            if(destination!=currentPage){
                MotionEvent cancel=MotionEvent.obtain(e);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancel);cancel.recycle();
                showPage(destination);
                return true;
            }
        }
        return super.dispatchTouchEvent(e);
    }
    private boolean overEditable(View v,float x,float y){
        if(v.getVisibility()!=View.VISIBLE)return false;
        int[] loc=new int[2];v.getLocationOnScreen(loc);
        if(x<loc[0]||x>=loc[0]+v.getWidth()||y<loc[1]||y>=loc[1]+v.getHeight())return false;
        if(v instanceof EditText||v instanceof Spinner){
            int[] xy=new int[2];v.getLocationOnScreen(xy);
            if(x>=xy[0]&&x<xy[0]+v.getWidth()&&y>=xy[1]&&y<xy[1]+v.getHeight())return true;
        }
        if(v instanceof ViewGroup){
            ViewGroup group=(ViewGroup)v;
            for(int i=0;i<group.getChildCount();i++)
                if(overEditable(group.getChildAt(i),x,y))return true;
        }
        return false;
    }

    private void showPage(int index){
        currentPage=Math.max(0,Math.min(2,index));
        for(int i=0;i<pagePanels.length;i++){
            pagePanels[i].setVisibility(i==currentPage?View.VISIBLE:View.GONE);
            ThemeStyle.styleTab(pageTabs[i],i==currentPage,
                new String[]{"play","queue","settings"}[i],
                new String[]{"Загрузка","Очередь","Настройки"}[i]);
        }
        mainScroll.scrollTo(0,0);
        // Tab selection is intentionally not persisted across fresh launches.
    }

    @Override public Object onRetainNonConfigurationInstance(){return currentPage;}

    private void saveDraft(){
        if(url==null||proxy==null)return;
        getPreferences(MODE_PRIVATE).edit()
            .putString("draftUrl",url.getText().toString())
            .putString("proxy",proxy.getText().toString())
            .putBoolean("proxyEnabled",proxyEnabled.isChecked())
            .putString("formatCode",selectedFormat())
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
        Spinner s=new Spinner(this,Spinner.MODE_DROPDOWN);
        s.setPopupBackgroundDrawable(ThemeStyle.popupBackground(this));
        boolean themeChoices=items==ThemeStyle.MODES;
        String[] modes={"sun","moon","phone"};
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,items){
            private TextView decorate(TextView v,int position){
                v.setTextColor(ThemeStyle.text(MainActivity.this));v.setTextSize(14);
                v.setSingleLine(true);v.setEllipsize(TextUtils.TruncateAt.END);
                if(themeChoices){
                    v.setCompoundDrawablesWithIntrinsicBounds(
                        new UiIcons(modes[position],ThemeStyle.subdued(MainActivity.this),dp(18)),
                        null,null,null);
                    v.setCompoundDrawablePadding(dp(10));
                }
                return v;
            }
            @Override public View getView(int position,View old,ViewGroup parent){
                return decorate((TextView)super.getView(position,old,parent),position);
            }
            @Override public View getDropDownView(int position,View old,ViewGroup parent){
                TextView v=decorate((TextView)super.getDropDownView(position,old,parent),position);
                v.setPadding(dp(16),dp(12),dp(16),dp(12));
                v.setBackground(ThemeStyle.popupRowBackground(MainActivity.this,
                    position==s.getSelectedItemPosition(),position,getCount()));
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);ThemeStyle.styleSpinner(s);
        FrameLayout shell=new FrameLayout(this);
        shell.setBackground(ThemeStyle.rounded(ThemeStyle.field(this),ThemeStyle.buttonStroke(this),dp(10)));
        LinearLayout.LayoutParams shellParams=new LinearLayout.LayoutParams(-1,dp(52));
        shellParams.setMargins(0,dp(4),0,dp(7));parent.addView(shell,shellParams);
        shell.addView(s,new FrameLayout.LayoutParams(-1,-1));
        ImageView arrow=new ImageView(this);
        arrow.setImageDrawable(new UiIcons("chevron",ThemeStyle.text(this),dp(20)));
        FrameLayout.LayoutParams arrParams=new FrameLayout.LayoutParams(dp(40),-1,Gravity.END|Gravity.CENTER_VERTICAL);
        arrow.setPadding(dp(10),0,dp(10),0);
        arrow.setContentDescription("Открыть список");
        shell.addView(arrow,arrParams);
        arrow.setOnClickListener(v->s.performClick());
        return s;
    }
    private String selectedFormat(){
        int index=mode==null?0:mode.getSelectedItemPosition();
        return index>=0&&index<formatChoices.values().size()?formatChoices.values().get(index):"mp4";
    }
    private void updateFormatChoices(FormatChoices next,boolean restoreSaved){
        if(mode==null||formatChoices.values().equals(next.values()))return;
        String previous=restoreSaved?getPreferences(MODE_PRIVATE).getString("formatCode","mp4"):selectedFormat();
        formatChoices=next;
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,next.labels()){
            @Override public View getView(int position,View old,ViewGroup parent){
                TextView v=(TextView)super.getView(position,old,parent);
                v.setTextColor(ThemeStyle.text(MainActivity.this));v.setTextSize(14);return v;
            }
            @Override public View getDropDownView(int position,View old,ViewGroup parent){
                TextView v=(TextView)super.getDropDownView(position,old,parent);
                v.setTextColor(ThemeStyle.text(MainActivity.this));
                v.setPadding(dp(16),dp(12),dp(16),dp(12));
                v.setBackground(ThemeStyle.popupRowBackground(MainActivity.this,
                    position==mode.getSelectedItemPosition(),position,getCount()));
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mode.setAdapter(adapter);
        int index=next.values().indexOf(previous);
        mode.setSelection(Math.max(0,index));
        quality.setEnabled(!next.isAudio(selectedFormat()));
    }
    private String selectedQuality(){
        int pos=quality.getSelectedItemPosition();
        List<String> values=qualityChoices.values();
        return pos>=0 && pos<values.size()?values.get(pos):"best";
    }
    private void updateQualityChoices(QualityChoices next,boolean restoreSaved){
        if(quality==null || qualityChoices.values().equals(next.values()))return;
        qualityChoices=next;
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,next.labels()){
            @Override public View getView(int position,View convertView,ViewGroup parent){
                TextView view=(TextView)super.getView(position,convertView,parent);
                view.setTextColor(ThemeStyle.text(MainActivity.this));view.setTextSize(14);
                return view;
            }
            @Override public View getDropDownView(int position,View convertView,ViewGroup parent){
                TextView view=(TextView)super.getDropDownView(position,convertView,parent);
                view.setTextColor(ThemeStyle.text(MainActivity.this));
                view.setPadding(dp(16),dp(12),dp(16),dp(12));
                view.setBackground(ThemeStyle.popupRowBackground(MainActivity.this,
                    position==quality.getSelectedItemPosition(),position,getCount()));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        quality.setAdapter(adapter);
        int selected=restoreSaved?getPreferences(MODE_PRIVATE).getInt("quality",0):0;
        if(restoreSaved && selected>=0 && selected<next.values().size())
            quality.setSelection(selected);
        else quality.setSelection(0);
    }
    private void updatePreviewMessage(String message){
        preview.setText(message);
        preview.setVisibility(message==null||message.isEmpty()?View.GONE:View.VISIBLE);
        if(previewDetailsButton!=null)previewDetailsButton.setVisibility(View.GONE);
    }
    private void showPreviewFailure(Exception ex){
        previewErrorLog=ex.getMessage()==null?ex.toString():ex.getMessage();
        updatePreviewMessage(EngineRecoveryPolicy.shortMessage(previewErrorLog));
        previewDetailsButton.setVisibility(View.VISIBLE);
    }
    private void restorePreview(){
        android.content.SharedPreferences p=getPreferences(MODE_PRIVATE);
        String source=p.getString("previewSource","");
        if(source.isEmpty())return;
        String title=p.getString("previewTitle","");
        if(title.isEmpty())return;
        if(p.getBoolean("previewPlaylist",false))
            previewSnapshot=PreviewSnapshot.playlist(source,title,p.getInt("previewCount",0));
        else
            previewSnapshot=PreviewSnapshot.video(source,title,p.getString("previewDuration",""),
                p.getString("previewQualities",""),p.getString("previewFormats",""));
        if(!previewSnapshot.playlist){
            File image=new File(getFilesDir(),"preview-thumbnail.jpg");
            if(image.isFile())previewBitmap=BitmapFactory.decodeFile(image.getAbsolutePath());
        }
        renderPreview(true);
    }
    private void rememberPreview(PreviewSnapshot value,Bitmap picture){
        previewSnapshot=value;previewBitmap=picture;
        getPreferences(MODE_PRIVATE).edit()
            .putString("previewSource",value.source)
            .putString("previewTitle",value.title)
            .putBoolean("previewPlaylist",value.playlist)
            .putInt("previewCount",value.videoCount)
            .putString("previewDuration",value.duration)
            .putString("previewQualities",value.resolutions)
            .putString("previewFormats",value.formats).apply();
        File thumb=new File(getFilesDir(),"preview-thumbnail.jpg");
        if(picture==null)thumb.delete();
        else try(FileOutputStream out=new FileOutputStream(thumb)){
            picture.compress(Bitmap.CompressFormat.JPEG,82,out);
        }catch(IOException ignored){thumb.delete();}
        updatePreviewMessage("");
        renderPreview(false);
    }
    private void renderPreview(boolean restoreSaved){
        if(previewCard==null||url==null)return;
        boolean valid=previewSnapshot!=null && previewSnapshot.matches(url.getText().toString());
        previewCard.setVisibility(valid?View.VISIBLE:View.GONE);
        if(!valid){
            updateQualityChoices(QualityChoices.fallback(),false);
            updateFormatChoices(FormatChoices.fallback(),false);
            return;
        }
        updateQualityChoices(previewSnapshot.playlist?QualityChoices.fallback():
            QualityChoices.fromDetected(previewSnapshot.resolutions),restoreSaved);
        updateFormatChoices(previewSnapshot.playlist?FormatChoices.fallback():
            FormatChoices.fromDetected(previewSnapshot.formats),restoreSaved);
        previewHeader.setText(previewSnapshot.playlist?"ИНФОРМАЦИЯ О ПЛЕЙЛИСТЕ":"ИНФОРМАЦИЯ О ВИДЕО");
        previewTitle.setText(previewSnapshot.title);
        if(previewSnapshot.playlist){
            previewDuration.setText(previewSnapshot.videoCount+" видео");
            previewQuality.setText("Плейлист");
            previewAllQualities.setVisibility(View.GONE);
        }else{
            previewDuration.setText(previewSnapshot.duration);
            String qualities=previewSnapshot.resolutions;
            previewQuality.setText(qualities.split(",")[0]);
            previewAllQualities.setText("Доступно: "+qualities);
            previewAllQualities.setVisibility(View.VISIBLE);
        }
        if(previewBitmap!=null && !previewSnapshot.playlist){
            thumbnail.setImageBitmap(previewBitmap);
            thumbnail.setVisibility(View.VISIBLE);
        }else{
            thumbnail.setImageDrawable(null);
            thumbnail.setVisibility(View.GONE);
        }
    }

    private void loadPreferences(){
        android.content.SharedPreferences p=getPreferences(MODE_PRIVATE);
        String savedFormat=p.getString("formatCode",p.getInt("mode",0)==1?"mp3":"mp4");
        int modeIndex=formatChoices.values().indexOf(savedFormat);
        mode.setSelection(Math.max(0,modeIndex));quality.setSelection(p.getInt("quality",0));
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
    private void enqueue(boolean priority){ enqueue(priority,false); }
    private void enqueue(boolean priority,boolean approvedDuplicate){
        final String link, proxyUrl;
        try{
            link=DownloadOptions.validateUrl(url.getText().toString());
            proxyUrl=proxyEnabled.isChecked()?DownloadOptions.validateProxy(proxy.getText().toString()):"";
            if(proxyEnabled.isChecked() && proxyUrl.isEmpty())throw new IllegalArgumentException("Укажите адрес прокси");
            if(!approvedDuplicate && QueuePolicy.hasActiveDuplicate(DownloadQueue.all(this),link)){
                ThemeStyle.dialog(this).setTitle("Видео уже в очереди")
                    .setMessage("Такая ссылка уже ожидает загрузки или скачивается. Добавить ещё раз?")
                    .setNegativeButton("Отмена",null)
                    .setPositiveButton("Добавить",(dialog,which)->enqueue(priority,true)).show();
                return;
            }
            String title=previewSnapshot!=null && previewSnapshot.matches(link) && !previewSnapshot.playlist?
                previewSnapshot.title:"";
            String fmt=selectedFormat();
            String id=DownloadQueue.enqueue(this,link,formatChoices.isAudio(fmt),
                selectedQuality(),proxyUrl,selectedFolder(),priority,title,fmt);
            if(previewSnapshot!=null&&previewSnapshot.matches(link)&&previewBitmap!=null)
                QueueThumbnails.store(this,id,previewBitmap);
        }catch(Exception e){error("Невозможно добавить загрузку",e);return;}
        saveDraft();
        refreshQueue();
        try{
            DownloadService.kick(this);
            Toast.makeText(this,priority?"Приоритетная загрузка добавлена":"Добавлено в очередь",Toast.LENGTH_SHORT).show();
        }catch(Exception e){error("Запуск загрузки",e);}
    }
    private String queueName(DownloadQueue.Item j){
        return !TextUtils.isEmpty(j.title)?j.title:
            (!TextUtils.isEmpty(j.filename)?j.filename:j.url);
    }
    private String queueFormat(DownloadQueue.Item j){
        String quality=j.audio?"Аудио":("best".equals(j.quality)?"Максимальное":j.quality+"p");
        String fmt=j.format==null||j.format.isEmpty()?(j.audio?"mp3":"mp4"):j.format;
        return quality+" · "+fmt.toUpperCase(Locale.ROOT);
    }
    private void queueHeading(String label,int count){
        TextView header=text(label+(count>=0?" ("+count+")":""),15);
        header.setTypeface(null,android.graphics.Typeface.BOLD);
        header.setPadding(dp(2),dp(12),dp(2),dp(7));
        queueList.addView(header);
    }
    private LinearLayout queueCard(boolean active){
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(active?10:8),dp(active?10:7),dp(active?10:8),dp(active?10:7));
        card.setBackground(ThemeStyle.rounded(ThemeStyle.cardColor(this),
            ThemeStyle.border(this),dp(12)));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);
        params.setMargins(0,0,0,dp(active?7:4));
        queueList.addView(card,params);
        return card;
    }
    private TextView compactInfo(String value,int size,boolean secondary){
        TextView t=text(value,size);
        if(secondary)t.setTextColor(ThemeStyle.subdued(this));
        t.setSingleLine(true);t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }
    private ImageView queuePhoto(LinearLayout parent,DownloadQueue.Item j,int side){
        ImageView picture=new ImageView(this);
        parent.addView(picture,new LinearLayout.LayoutParams(dp(side),dp(side)));
        QueueThumbnails.show(this,j,picture,dp(8));
        return picture;
    }
    private ImageView queueAction(LinearLayout parent,String icon,int tint,String description){
        ImageView action=new ImageView(this);
        action.setImageDrawable(new UiIcons(icon,tint,dp(22)));
        action.setContentDescription(description);
        action.setPadding(dp(7),dp(7),dp(7),dp(7));
        parent.addView(action,new LinearLayout.LayoutParams(dp(39),dp(39)));
        return action;
    }
    private void refreshQueue(){
        if(queueList==null)return;
        List<DownloadQueue.Item> jobs=DownloadQueue.all(this);
        StringBuilder key=new StringBuilder();
        for(DownloadQueue.Item j:jobs)key.append(j.id).append('/').append(j.status).append(';');
        String signature=key.toString();
        if(signature.equals(queueSignature) && activeQueueBar!=null){
            for(DownloadQueue.Item j:jobs)if(DownloadQueue.RUNNING.equals(j.status)){
                activeQueueBar.setProgress(j.progress);
                if(activeQueueInfo!=null)
                    activeQueueInfo.setText(j.message);
                return;
            }
        }
        if(signature.equals(queueSignature)&&activeQueueBar==null)return;
        queueSignature=signature;activeQueueBar=null;activeQueueInfo=null;
        queueList.removeAllViews();
        if(jobs.isEmpty()){
            queueList.addView(compactInfo("Очередь пуста",14,true));return;
        }
        List<DownloadQueue.Item> running=new ArrayList<>(),waiting=new ArrayList<>(),
            done=new ArrayList<>(),problems=new ArrayList<>();
        for(DownloadQueue.Item j:jobs){
            if(DownloadQueue.RUNNING.equals(j.status))running.add(j);
            else if(DownloadQueue.WAITING.equals(j.status)||DownloadQueue.PAUSED.equals(j.status))waiting.add(j);
            else if(DownloadQueue.DONE.equals(j.status))done.add(j);
            else problems.add(j);
        }
        if(!running.isEmpty()){
            queueHeading("Текущая загрузка",-1);
            for(DownloadQueue.Item j:running){
                LinearLayout card=queueCard(true);
                LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);
                top.setGravity(Gravity.CENTER_VERTICAL);card.addView(top);
                queuePhoto(top,j,67);
                LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
                lp.setMargins(dp(9),0,dp(4),0);top.addView(labels,lp);
                TextView title=compactInfo(queueName(j),14,false);
                title.setTypeface(null,android.graphics.Typeface.BOLD);labels.addView(title);
                labels.addView(compactInfo(queueFormat(j),12,false));
                TextView percent=compactInfo(j.progress+"%",12,true);labels.addView(percent);
                ImageView pause=queueAction(top,"pause",ThemeStyle.text(this),"Приостановить загрузку");
                pause.setOnClickListener(v->{DownloadService.pause(this,j.id);queueSignature=null;refreshQueue();});
                activeQueueBar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
                activeQueueBar.setMax(100);activeQueueBar.setProgress(j.progress);
                activeQueueBar.setProgressTintList(android.content.res.ColorStateList.valueOf(ThemeStyle.RED));
                LinearLayout.LayoutParams bar=new LinearLayout.LayoutParams(-1,dp(6));
                bar.setMargins(0,dp(8),0,dp(5));card.addView(activeQueueBar,bar);
                activeQueueInfo=compactInfo(j.message,12,true);
                card.addView(activeQueueInfo);
            }
        }
        if(!waiting.isEmpty()){
            queueHeading("Следующие в очереди",waiting.size());
            for(DownloadQueue.Item j:waiting)compactQueueRow(j,false);
        }
        if(!done.isEmpty()){
            queueHeading("Завершённые",done.size());
            for(DownloadQueue.Item j:done)compactQueueRow(j,true);
        }
        if(!problems.isEmpty()){
            queueHeading("Ошибки и отменённые",problems.size());
            for(DownloadQueue.Item j:problems)compactQueueRow(j,false);
        }
    }
    private void compactQueueRow(DownloadQueue.Item j,boolean finished){
        LinearLayout card=queueCard(false);
        LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);card.addView(line);
        queuePhoto(line,j,54);
        LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
        lp.setMargins(dp(9),0,0,0);line.addView(labels,lp);
        TextView title=compactInfo(queueName(j),13,false);
        title.setTypeface(null,android.graphics.Typeface.BOLD);labels.addView(title);
        labels.addView(compactInfo(queueFormat(j),11,false));
        String status=finished?(j.bytesSaved>0?"Готово · "+readableBytes(j.bytesSaved):"Готово"):
            DownloadQueue.PAUSED.equals(j.status)?"На паузе":
            DownloadQueue.ERROR.equals(j.status)?QueuePolicy.errorSummary(j.message):
            DownloadQueue.CANCELLED.equals(j.status)?"Отменено":"Ожидает…";
        TextView state=compactInfo(status,11,true);
        if(finished)state.setTextColor(0xFF4EC67B);
        if(DownloadQueue.ERROR.equals(j.status))state.setTextColor(ThemeStyle.RED);
        labels.addView(state);
        if(finished){
            ImageView play=queueAction(line,"play",0xFF4EC67B,"Открыть скачанный файл");
            play.setOnClickListener(v->openDownloaded(j));
        }else if(DownloadQueue.PAUSED.equals(j.status)){
            ImageView resume=queueAction(line,"play",ThemeStyle.RED,"Продолжить загрузку");
            resume.setOnClickListener(v->{DownloadService.resume(this,j.id);queueSignature=null;refreshQueue();});
        }else if(DownloadQueue.WAITING.equals(j.status)){
            ImageView paused=queueAction(line,"pause",ThemeStyle.subdued(this),"Поставить на паузу");
            paused.setOnClickListener(v->{DownloadService.pause(this,j.id);queueSignature=null;refreshQueue();});
        }
        ImageView menu=queueAction(line,"more",ThemeStyle.subdued(this),"Действия с загрузкой");
        menu.setOnClickListener(v->queueMenu(menu,j));
    }
    private static String readableBytes(long value){
        if(value>=1024L*1024*1024)return String.format(Locale.getDefault(),"%.2f ГБ",value/(1024d*1024*1024));
        if(value>=1024*1024)return String.format(Locale.getDefault(),"%.0f МБ",value/(1024d*1024));
        return String.format(Locale.getDefault(),"%.0f КБ",value/1024d);
    }
    private void queueMenu(View anchor,DownloadQueue.Item j){
        PopupMenu menu=new PopupMenu(this,anchor);
        if(DownloadQueue.DONE.equals(j.status)){
            menu.getMenu().add("Открыть файл");
            menu.getMenu().add("Открыть папку");
        }
        if(DownloadQueue.ERROR.equals(j.status)){
            menu.getMenu().add("Подробности");
            menu.getMenu().add("Повторить загрузку");
        }
        if(DownloadQueue.CANCELLED.equals(j.status))menu.getMenu().add("Повторить загрузку");
        if(DownloadQueue.PAUSED.equals(j.status))menu.getMenu().add("Продолжить");
        menu.getMenu().add("Удалить запись");
        menu.setOnMenuItemClickListener(option->{
            String name=option.getTitle().toString();
            switch(name){
                case "Открыть файл":openDownloaded(j);break;
                case "Открыть папку":openFolderFor(j.directoryUri);break;
                case "Подробности":showTechnicalDetails(j);break;
                case "Повторить загрузку":DownloadQueue.retry(this,j.id);DownloadService.kick(this);break;
                case "Продолжить":DownloadService.resume(this,j.id);break;
                case "Удалить запись":confirmRemove(j);return true;
            }
            queueSignature=null;refreshQueue();return true;
        });
        menu.show();
    }
    private void openDownloaded(DownloadQueue.Item j){
        if(TextUtils.isEmpty(j.contentUri)){
            ThemeStyle.dialog(this).setTitle("Файл")
                .setMessage("Для загрузок, выполненных до этой версии, адрес файла не сохранён. Откройте папку загрузок.")
                .setNegativeButton("Закрыть",null)
                .setPositiveButton("Открыть папку",(d,w)->openFolderFor(j.directoryUri)).show();
            return;
        }
        String mime=j.audio?"audio/*":"video/*";
        try{
            Intent view=new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(Uri.parse(j.contentUri),mime);
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(view,"Открыть скачанный файл"));
        }catch(Exception e){
            ThemeStyle.dialog(this).setMessage("Не найден подходящий проигрыватель либо файл был перемещён.")
                .setPositiveButton("ОК",null).show();
        }
    }
    private void confirmRemove(DownloadQueue.Item job){
        AlertDialog confirm=ThemeStyle.dialog(this).setTitle("Удалить запись из очереди?")
            .setMessage("Скачанные файлы сохранятся. Если загрузка выполняется, она будет остановлена.")
            .setNegativeButton("Отмена",null)
            .setPositiveButton("Удалить",(dialog,which)->{
                boolean running=DownloadQueue.requestRemove(this,job.id);
                if(running)DownloadService.cancel(this,job.id);
                else DownloadService.discardPartial(this,job.id);
                QueueThumbnails.delete(this,job.id);
                queueSignature=null;refreshQueue();
            }).create();
        confirm.setOnShowListener(dialog->{
            confirm.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ThemeStyle.text(this));
            confirm.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ThemeStyle.RED);
        });
        confirm.show();
    }
    private void showTechnicalDetails(DownloadQueue.Item job){
        showTechnicalDetails(job.message);
    }
    /** Keep a natural almost-fullscreen dialog for long logs; the hint never changes its height. */
    private void showTechnicalDetails(String message){
        FrameLayout frame=new FrameLayout(this);
        ScrollView scroller=new ScrollView(this);
        scroller.setFillViewport(false);
        TextView log=text(message==null?"":message,12);
        log.setTextIsSelectable(true);
        log.setPadding(dp(16),dp(12),dp(16),dp(12));
        scroller.addView(log);
        frame.addView(scroller,new FrameLayout.LayoutParams(-1,-2));
        TextView hint=text("Прокрутите вниз",12);
        hint.setTextColor(ThemeStyle.subdued(this));
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(dp(12),dp(4),dp(12),dp(4));
        hint.setBackground(ThemeStyle.rounded(ThemeStyle.cardColor(this),
            ThemeStyle.border(this),dp(8)));
        FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-2,-2,
            Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        hp.bottomMargin=dp(8);
        frame.addView(hint,hp);
        hint.setVisibility(View.GONE);
        Runnable refreshHint=()->hint.setVisibility(scroller.canScrollVertically(1)?
            View.VISIBLE:View.GONE);
        scroller.setOnScrollChangeListener((v,x,y,oldX,oldY)->refreshHint.run());
        ThemeStyle.dialog(this).setTitle("Подробности ошибки")
            .setView(frame).setPositiveButton("Закрыть",null).show();
        scroller.post(refreshHint);
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
        final String outputFormat=selectedFormat();
        final boolean audio=formatChoices.isAudio(outputFormat);
        final String resolution=selectedQuality();
        final String directory=selectedFolder();
        loadingPlaylist=true;previewButton.setEnabled(false);
        updatePreviewMessage("Получение списка видео плейлиста…");
        uiWorker.execute(()->{
            try {
                PlaylistRepository.Playlist data;
                try{data=PlaylistRepository.fetch(getApplicationContext(),link,proxyValue);}
                catch(Exception first){
                    if(!EngineRecoveryPolicy.shouldUpdate(first.getMessage())||DownloadQueue.hasActive(this))throw first;
                    runOnUiThread(()->updatePreviewMessage("Обновление компонента YouTube…"));
                    if(EngineMaintenance.update(getApplicationContext(),false,false)!=EngineMaintenance.Result.UPDATED)
                        throw first;
                    runOnUiThread(()->updatePreviewMessage("Повторное получение списка…"));
                    data=PlaylistRepository.fetch(getApplicationContext(),link,proxyValue);
                }
                final PlaylistRepository.Playlist loaded=data;
                runOnUiThread(()->{
                    loadingPlaylist=false;previewButton.setEnabled(true);
                    if(!link.equals(url.getText().toString().trim()))return;
                    rememberPreview(PreviewSnapshot.playlist(link,loaded.title,loaded.entries.size()),null);
                    showPlaylistSelection(loaded,audio,resolution,proxyValue,directory,priority,outputFormat);
                });
            }catch(Exception ex){
                runOnUiThread(()->{
                    loadingPlaylist=false;previewButton.setEnabled(true);
                    if(link.equals(url.getText().toString().trim()))showPreviewFailure(ex);
                });
            }
        });
    }
    private void showPlaylistSelection(PlaylistRepository.Playlist data,
            boolean audio,String resolution,String proxyValue,String directory,boolean priority,String outputFormat){
        int count=data.entries.size();
        CharSequence[] labels=new CharSequence[count];
        boolean[] checked=new boolean[count];
        java.util.Arrays.fill(checked,true);
        for(int i=0;i<count;i++){
            PlaylistRepository.Entry entry=data.entries.get(i);
            labels[i]=(i+1)+". "+entry.title+
                (entry.duration>=0?" · "+QueueRules.readableDuration(entry.duration):"");
        }
        AlertDialog dialog=ThemeStyle.dialog(this)
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
                    List<DownloadQueue.Item> active=DownloadQueue.all(this);
                    int existing=0;
                    for(int i=0;i<selected.size();i++)
                        if(Boolean.TRUE.equals(selected.get(i)) &&
                            QueuePolicy.hasActiveDuplicate(active,data.entries.get(i).url))existing++;
                    if(existing>0){
                        final int duplicates=existing;
                        ThemeStyle.dialog(this).setTitle("Повторные видео")
                            .setMessage("Уже в очереди: "+duplicates+". Добавить их повторно?")
                            .setNegativeButton("Отмена",null)
                            .setPositiveButton("Добавить",(confirm,which)->{
                                try{addSelectedPlaylist(data,selected,audio,resolution,proxyValue,directory,priority,outputFormat);
                                    dialog.dismiss();
                                }catch(Exception e){error("Очередь плейлиста",e);}
                            }).show();
                    }else{
                        addSelectedPlaylist(data,selected,audio,resolution,proxyValue,directory,priority,outputFormat);
                        dialog.dismiss();
                    }
                }catch(Exception e){error("Очередь плейлиста",e);}
            });
        });
        dialog.show();
    }
    private void addSelectedPlaylist(PlaylistRepository.Playlist data,List<Boolean> selected,
            boolean audio,String resolution,String proxyValue,String directory,boolean priority,String outputFormat){
        int added=DownloadQueue.enqueuePlaylist(this,data,selected,audio,resolution,proxyValue,directory,priority,outputFormat);
        refreshQueue();DownloadService.kick(this);
        Toast.makeText(this,"Добавлено в очередь: "+added,Toast.LENGTH_LONG).show();
    }
    private void loadPreview(){
        final String videoUrl;
        try{videoUrl=DownloadOptions.validateUrl(url.getText().toString());}
        catch(Exception e){error("Ссылка",e);return;}
        previewButton.setEnabled(false);
        updatePreviewMessage("Получение информации о видео…");
        previewCard.setVisibility(View.GONE);
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
                VideoInfo info;
                try{info=YoutubeDL.getInstance().getInfo(request);}
                catch(Exception first){
                    if(!EngineRecoveryPolicy.shouldUpdate(first.getMessage())||DownloadQueue.hasActive(this))throw first;
                    runOnUiThread(()->updatePreviewMessage("Обновление компонента YouTube…"));
                    if(EngineMaintenance.update(getApplicationContext(),false,false)!=EngineMaintenance.Result.UPDATED)
                        throw first;
                    runOnUiThread(()->updatePreviewMessage("Повторное получение информации…"));
                    info=YoutubeDL.getInstance().getInfo(request);
                }
                String title=info.getTitle()==null?"Без названия":info.getTitle();
                NavigableSet<Integer> heights=new TreeSet<>();
                Set<String> availableFormats=new HashSet<>();
                if(info.getFormats()!=null)for(VideoFormat format:info.getFormats()){
                    String ext=format.getExt();
                    if(ext!=null&&Arrays.asList("mp4","webm","m4a").contains(ext.toLowerCase(Locale.ROOT)))
                        availableFormats.add(ext.toLowerCase(Locale.ROOT));
                    if(format.getHeight()>0 && format.getVcodec()!=null && !"none".equals(format.getVcodec()))
                        heights.add(format.getHeight());
                }
                String duration=QueueRules.readableDuration(info.getDuration());
                List<String> options=new ArrayList<>();
                for(Integer height:heights.descendingSet())options.add(height+"p");
                String resolution=options.isEmpty()?"Не указано":TextUtils.join(", ",options);
                Bitmap picture=loadImage(info.getThumbnail());
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed()||!videoUrl.equals(url.getText().toString().trim()))return;
                    rememberPreview(PreviewSnapshot.video(videoUrl,title,duration,resolution,
                        TextUtils.join(",",availableFormats)),picture);
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    if(!isFinishing()&&!isDestroyed()&&videoUrl.equals(url.getText().toString().trim()))
                        showPreviewFailure(e);
                });
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
            ThemeStyle.dialog(this).setMessage("Завершите очередь перед обновлением yt-dlp.")
                .setPositiveButton("ОК",null).show();return;
        }
        updateEngineButton.setEnabled(false);updateEngineButton.setText("Обновление компонента…");
        uiWorker.execute(()->{
            try{
                EngineMaintenance.Result response=EngineMaintenance.update(getApplicationContext(),true,false);
                runOnUiThread(()->ThemeStyle.dialog(this).setTitle("Компонент YouTube")
                    .setMessage(response==EngineMaintenance.Result.UPDATED?"Компонент обновлён.":
                        response==EngineMaintenance.Result.SKIPPED?"Обновление уже выполняется.":
                        "Уже установлена последняя версия.")
                    .setPositiveButton("ОК",null).show());
            }catch(Exception e){runOnUiThread(()->error("Обновление yt-dlp",e));}
            finally{runOnUiThread(()->{updateEngineButton.setEnabled(true);updateEngineButton.setText("Обновить компонент работы с YouTube");});}
        });
    }
    private String version(){
        try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}
        catch(Exception ignored){return "?";}
    }
    private void error(String title,Exception e){
        ThemeStyle.dialog(this).setTitle(title).setMessage(e.getMessage())
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
                    else if(manual)ThemeStyle.dialog(this).setMessage("Установлена последняя версия.")
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
            ThemeStyle.dialog(this).setTitle("Установить загруженное обновление?")
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
