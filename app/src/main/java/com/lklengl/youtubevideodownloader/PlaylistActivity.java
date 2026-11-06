package com.lklengl.youtubevideodownloader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Full-screen playlist selector: independently editable formats, stable recycled thumbnails and real refresh state. */
public final class PlaylistActivity extends Activity {
 public static final String URL="playlist_url", QUALITY="quality", FORMAT="output_format",
  PROXY="proxy", FOLDER="folder", PRIORITY="priority";
 private boolean priority;
 private String source,quality,format,proxy,folder;
 private PlaylistRepository.Playlist playlist;
 private PlaylistResolutionWorker formatsWorker;
 private boolean[] selected;
 private List<String> qualities;
 private ListView list;
 private BaseAdapter adapter;
 private TextView heading,subheading,owner,progressText,refreshControl,sectionCount;
 private CheckBox selectAll;
 private ImageView filterIcon,bulkQualityIcon;
 private View filterDot;
 private final List<Integer> visibleRows=new ArrayList<>();
 private int[] videoStatus=new int[0];
 private int selectionFilter=0,downloadFilter=0,queueFilter=0;
 private boolean selectedFirst=false,animatingSelection=false;
 private static final Map<String,int[]> scrollSession=new HashMap<>();
 private int activeTitle=-1,durationWidth=0;
 private boolean scrollRestoring=true;
 private float swipeStartX,swipeStartY;
 private boolean swipeEligible;
 private final Map<String,String> queuedQualities=new HashMap<>();
 private LinearLayout progressPanel,rootLayout;
 private FrameLayout headerClip;
 private int headerExpandedHeight=-1,headerCollapsePx=0,priorListFirst=-1,priorListTop=0;
 private boolean returningToTop=false;
 private ProgressBar refreshSpinner;
 private Button cancelRefreshButton,addButton;
 private FrameLayout listFrame;
 private ImageView listArrowUp,listArrowDown;
 private final Handler arrowHandler=new Handler(Looper.getMainLooper());
 private long refreshStart;
 private boolean observedRunning=false;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final String[] waitingMessages={
  "Запрашиваем список видео с YouTube…",
  "Ожидаем ответа YouTube. Сохранённый список доступен.",
  "Обрабатывается информация о плейлисте…",
  "Большой плейлист может загружаться дольше обычного.",
  "Продолжаем получать сведения о видео…"
 };
 private final Runnable tick=new Runnable(){
  @Override public void run(){
   PlaylistRefreshManager.Snapshot state=PlaylistRefreshManager.current();
   boolean running=source!=null&&source.equals(state.url)&&state.running();
   if(running){
    if(cancelExpiry!=null){handler.removeCallbacks(cancelExpiry);cancelExpiry=null;}
    cancelledRevision=-1;
    progressPanel.setMinimumHeight(0);
    observedRunning=true;refreshStart=state.startedMs;
    progressPanel.setVisibility(View.VISIBLE);refreshControl.setEnabled(false);
    refreshSpinner.setVisibility(View.VISIBLE);cancelRefreshButton.setVisibility(View.VISIBLE);
    long seconds=Math.max(0,(SystemClock.elapsedRealtime()-refreshStart)/1000L);
    int index=seconds<12?0:1+(int)((seconds-12)/8%(waitingMessages.length-1));
    progressText.setText(UiStrings.display(PlaylistActivity.this,
      "Обновление плейлиста · "+String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60)+
      "\n"+(state.message.isEmpty()?waitingMessages[index]:state.message)));
   }else{
    boolean same=source!=null&&source.equals(state.url);
    long cancelledAt=PlaylistRefreshManager.cancellationTimeMs();
    boolean cancelled=same&&state.stage==PlaylistRefreshManager.Stage.CANCELLED&&
       Release058Rules.showCancel(cancelledAt,SystemClock.elapsedRealtime());
    refreshControl.setEnabled(true);
    if(cancelled&&cancelledRevision!=state.revision){
     cancelledRevision=state.revision;
     android.transition.TransitionManager.beginDelayedTransition(
       rootLayout,new android.transition.AutoTransition().setDuration(600));
     progressPanel.setMinimumHeight(progressPanel.getHeight()+dp(18));
     progressPanel.setVisibility(View.VISIBLE);
     refreshSpinner.setVisibility(View.GONE);cancelRefreshButton.setVisibility(View.GONE);
     progressText.setText(UiStrings.display(PlaylistActivity.this,
          "Запрос обновления информации отменён"));
     final long revision=state.revision;
     cancelExpiry=()->{
      if(revision!=cancelledRevision)return;
      expiredCancelledRevision=revision;
      android.transition.TransitionManager.beginDelayedTransition(
       rootLayout,new android.transition.AutoTransition().setDuration(600));
      progressPanel.setVisibility(View.GONE);progressPanel.setMinimumHeight(0);
      cancelExpiry=null;
     };
     handler.postDelayed(cancelExpiry,Math.max(0,15000-(SystemClock.elapsedRealtime()-cancelledAt)));
    }else if(!cancelled&&progressPanel.getVisibility()!=View.GONE){
     if(cancelledRevision!=state.revision||
       expiredCancelledRevision!=state.revision){
      android.transition.TransitionManager.beginDelayedTransition(
        rootLayout,new android.transition.AutoTransition().setDuration(600));
      progressPanel.setVisibility(View.GONE);progressPanel.setMinimumHeight(0);
     }
    }
    if(observedRunning&&same){
     if(state.stage==PlaylistRefreshManager.Stage.DONE){
      refreshData(true);
      PlaylistResolutionWorker.takeRefreshFlag(PlaylistActivity.this,source);
      formatsWorker.start(playlist,true);
      promoteVisibleFormats();
      if(PlaylistRefreshManager.claimNotice(source,state.revision))
       RefreshNotice.show(PlaylistActivity.this,true);
     }else if(state.stage==PlaylistRefreshManager.Stage.ERROR){
      if(PlaylistRefreshManager.claimNotice(source,state.revision))
       RefreshNotice.show(PlaylistActivity.this,false);
     }
    }
    observedRunning=false;
   }
   handler.postDelayed(this,600);
  }
 };
 private long cancelledRevision=-1,expiredCancelledRevision=-1;
 private Runnable cancelExpiry;
 private int dp(float v){return ThemeStyle.dp(this,v);}
 private TextView txt(String t,int size){
  TextView view=new TextView(this);view.setText(UiStrings.display(this,t));view.setTextSize(size);
  view.setTextColor(ThemeStyle.text(this));return view;
 }
 private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
 private LinearLayout panel(){LinearLayout l=column();
  l.setPadding(dp(12),dp(10),dp(12),dp(10));
  l.setBackground(ThemeStyle.rounded(ThemeStyle.cardColor(this),ThemeStyle.border(this),dp(15)));
  return l;
 }
 private LinearLayout.LayoutParams margin(int top,int bottom){
  LinearLayout.LayoutParams l=new LinearLayout.LayoutParams(-1,-2);
  l.setMargins(0,dp(top),0,dp(bottom));return l;
 }
 @Override public void onCreate(Bundle state){
  ThemeStyle.initialize(this);super.onCreate(state);
  source=getIntent().getStringExtra(URL);quality=getIntent().getStringExtra(QUALITY);
  format=getIntent().getStringExtra(FORMAT);proxy=getIntent().getStringExtra(PROXY);
  folder=getIntent().getStringExtra(FOLDER);
  priority=getIntent().getBooleanExtra(PRIORITY,false);
  if(source==null||!PlaylistSupport.isPlaylist(source)){finish();return;}
  if(!PlaylistQualityPolicy.allowed(quality))quality="best";
  if(proxy==null)proxy="";
  playlist=PlaylistCache.load(this,source);
  if(playlist==null){
   Toast.makeText(this,UiStrings.display(PlaylistActivity.this, "Плейлист не сохранён. Получите список повторно."),Toast.LENGTH_LONG).show();
   finish();return;
  }
  android.content.SharedPreferences display=getSharedPreferences("playlist_display",0);
  selectionFilter=display.getInt("selection",0);
  downloadFilter=display.getInt("download",0);
  queueFilter=display.getInt("queue",0);
  selectedFirst=display.getBoolean("selectedFirst",false);
  formatsWorker=new PlaylistResolutionWorker(this,proxy,(id,failed)->{
   if(isFinishing()||isDestroyed()||adapter==null)return;
   adapter.notifyDataSetChanged();updateFooter();
  });
  buildScreen();ThemeStyle.applySystemBarInsets(this);refreshData(false);
  boolean force=PlaylistResolutionWorker.takeRefreshFlag(this,source);
  formatsWorker.start(playlist,force);
  list.post(()->{promoteVisibleFormats();adapter.notifyDataSetChanged();});
 }
 /** Back swipe begins in free screen space; inputs and vertical scrolling win. */
 private boolean touchesInteractive(View v,float x,float y){
  if(v==null||v.getVisibility()!=View.VISIBLE)return false;
  if(v instanceof CompoundButton||v instanceof Spinner||v instanceof Button||
     v instanceof EditText||(v instanceof ImageView&&v.isClickable())||
      v==filterIcon||v==refreshControl||v==listArrowUp||v==listArrowDown)
   if(hit(v,x,y))return true;
  if(v instanceof ViewGroup){
   ViewGroup group=(ViewGroup)v;
   for(int i=0;i<group.getChildCount();i++)
    if(touchesInteractive(group.getChildAt(i),x,y))return true;
  }
  return false;
 }
 private boolean hit(View view,float x,float y){
  int[] pos=new int[2];view.getLocationOnScreen(pos);
  return x>=pos[0]&&y>=pos[1]&&x<pos[0]+view.getWidth()&&y<pos[1]+view.getHeight();
 }
 @Override public boolean dispatchTouchEvent(MotionEvent event){
  if(event.getActionMasked()==MotionEvent.ACTION_DOWN){
   swipeStartX=event.getRawX();swipeStartY=event.getRawY();
   swipeEligible=!touchesInteractive(getWindow().getDecorView(),swipeStartX,swipeStartY);
  }else if(event.getActionMasked()==MotionEvent.ACTION_UP){
   float dx=event.getRawX()-swipeStartX,dy=event.getRawY()-swipeStartY;
   if(swipeEligible&&Release057Rules.backSwipe(swipeStartX,swipeStartY,
       event.getRawX(),dx/getResources().getDisplayMetrics().density,
       dy/getResources().getDisplayMetrics().density)){
    MotionEvent cancel=MotionEvent.obtain(event);cancel.setAction(MotionEvent.ACTION_CANCEL);
    super.dispatchTouchEvent(cancel);cancel.recycle();finish();return true;
   }
   swipeEligible=false;
  }else if(event.getActionMasked()==MotionEvent.ACTION_CANCEL)swipeEligible=false;
  return super.dispatchTouchEvent(event);
 }
 private Set<String> previouslyAdded(){
  Set<String> urls=new HashSet<>();
  for(int i=0;i<playlist.entries.size();i++)
   if(i<videoStatus.length&&videoStatus[i]!=PlaylistHistory.NEW)
    urls.add(playlist.entries.get(i).url);
  return urls;
 }
 private ColorStateList checkTint(){
  return new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},
   new int[]{ThemeStyle.RED,ThemeStyle.isDark(this)?0xFFF4F5F8:0xFF596477});
 }
 private void refreshStatuses(){
  PlaylistHistory.syncFromQueue(this);
  List<DownloadQueue.Item> items=DownloadQueue.all(this);
  queuedQualities.clear();
  for(DownloadQueue.Item item:items)queuedQualities.put(item.url,item.quality);
  videoStatus=new int[playlist.entries.size()];
  for(int i=0;i<videoStatus.length;i++)
   videoStatus[i]=PlaylistHistory.status(this,playlist.entries.get(i).url,items);
 }
 private void rebuildVisible(boolean reSort){
  List<Integer> next=PlaylistFilterPolicy.visible(selected,videoStatus,selectionFilter,downloadFilter,queueFilter,reSort&&selectedFirst);
  if(reSort || visibleRows.isEmpty()){visibleRows.clear();visibleRows.addAll(next);}
  else{
   visibleRows.removeIf(i->!next.contains(i));
   for(int i:next)if(!visibleRows.contains(i))visibleRows.add(i);
  }
  if(adapter!=null)adapter.notifyDataSetChanged();
  updateFilterIcon();updateFooter();
 }
 private void updateFilterIcon(){
  if(filterIcon==null)return;
  boolean active=selectionFilter!=0||downloadFilter!=0||queueFilter!=0||selectedFirst;
  filterIcon.setImageDrawable(new UiIcons("sliders",ThemeStyle.text(this),dp(20)));
  filterIcon.setBackground(ThemeStyle.rounded(ThemeStyle.buttonFill(this),
    ThemeStyle.buttonStroke(this),dp(9)));
  if(filterDot!=null)filterDot.setVisibility(active?View.VISIBLE:View.GONE);
 }
 private void refreshData(boolean fresh){
  if(fresh){
   playlist=PlaylistCache.load(this,source);
   if(playlist==null)return;
  }
  refreshStatuses();
  Set<String> already=previouslyAdded();
  List<String> urls=new ArrayList<>();
  for(PlaylistRepository.Entry e:playlist.entries)urls.add(e.url);
  Map<String,Boolean> byId=PlaylistCache.readSelectionById(this,source);
  List<String> ids=new ArrayList<>();
  Set<String> alreadyIds=new HashSet<>();
  for(PlaylistRepository.Entry e:playlist.entries){
   ids.add(e.id);
   if(already.contains(e.url))alreadyIds.add(e.id);
  }
  List<Boolean> flags;
  if(byId!=null)flags=PlaylistSelectionPolicy.byId(ids,byId,alreadyIds);
  else{
   List<Boolean> legacy=PlaylistCache.readSelection(this,source);
   flags=legacy==null?PlaylistSelectionPolicy.initial(urls,already):
     PlaylistSelectionPolicy.resume(legacy,urls,already);
  }
  selected=new boolean[flags.size()];
  for(int i=0;i<flags.size();i++){
   // Pending formats never permit a prechecked item.
   selected[i]=flags.get(i)&&formatsWorker.ready(playlist.entries.get(i).id);
  }
  PlaylistCache.saveSelectionById(this,source,playlist,selected);
  qualities=PlaylistCache.readQualities(this,source,playlist,quality);
  heading.setText(playlist.title);
  owner.setText(playlist.uploader);
  owner.setVisibility(TextUtils.isEmpty(playlist.uploader)?View.GONE:View.VISIBLE);
  long seconds=0;boolean allKnown=true;
  for(PlaylistRepository.Entry e:playlist.entries){
   if(e.duration<0)allKnown=false;else seconds+=e.duration;
  }
  subheading.setText(UiStrings.display(PlaylistActivity.this, playlist.entries.size()+" видео"+
   (allKnown?" · "+QueueRules.readableDuration((int)Math.min(Integer.MAX_VALUE,seconds)):"")));
  durationWidth=dp(44);
  android.graphics.Paint timePaint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
  timePaint.setTextSize(getResources().getDisplayMetrics().scaledDensity*11);
  for(PlaylistRepository.Entry e:playlist.entries)if(e.duration>=0)
   durationWidth=Math.max(durationWidth,(int)Math.ceil(timePaint.measureText(
    QueueRules.readableDuration(e.duration)))+dp(6));
  durationWidth=Math.min(durationWidth,dp(95));
  rebuildVisible(true);
 }
 private void buildScreen(){
  LinearLayout root=column();rootLayout=root;
  root.setBackgroundColor(ThemeStyle.background(this));
  root.setPadding(dp(13),dp(8),dp(13),dp(8));setContentView(root);
  LinearLayout toolbar=new LinearLayout(this);toolbar.setGravity(Gravity.CENTER_VERTICAL);
  root.addView(toolbar,new LinearLayout.LayoutParams(-1,dp(47)));
  TextView back=txt("‹",32);back.setGravity(Gravity.CENTER);
  toolbar.addView(back,new LinearLayout.LayoutParams(dp(44),dp(45)));
  back.setOnClickListener(v->finish());back.setContentDescription(UiStrings.display(PlaylistActivity.this, "Назад"));
  TextView title=txt("Плейлист",19);title.setTypeface(null,Typeface.BOLD);
  toolbar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
  refreshControl=txt("⟳",30);refreshControl.setGravity(Gravity.CENTER);
  refreshControl.setTextColor(ThemeStyle.RED);
  toolbar.addView(refreshControl,new LinearLayout.LayoutParams(dp(44),dp(44)));
  refreshControl.setContentDescription(UiStrings.display(PlaylistActivity.this, "Обновить плейлист"));
  refreshControl.setOnClickListener(v->updatePlaylist());

  progressPanel=panel();
  LinearLayout statusRow=new LinearLayout(this);statusRow.setGravity(Gravity.CENTER_VERTICAL);
  progressPanel.addView(statusRow);
  refreshSpinner=new ProgressBar(this);
  refreshSpinner.setIndeterminateTintList(ColorStateList.valueOf(ThemeStyle.RED));
  statusRow.addView(refreshSpinner,new LinearLayout.LayoutParams(dp(33),dp(33)));
  progressText=txt("",12);progressText.setTextColor(ThemeStyle.subdued(this));
  LinearLayout.LayoutParams progressTextParams=new LinearLayout.LayoutParams(0,-2,1);
  progressTextParams.setMargins(dp(10),0,0,0);statusRow.addView(progressText,progressTextParams);
  cancelRefreshButton=new Button(this);
  cancelRefreshButton.setText(UiStrings.display(this,"Отмена"));
  ThemeStyle.styleButton(cancelRefreshButton,false);
  cancelRefreshButton.setTextSize(12);
  statusRow.addView(cancelRefreshButton,new LinearLayout.LayoutParams(dp(78),dp(38)));
  cancelRefreshButton.setOnClickListener(v->cancelRefresh());
  root.addView(progressPanel,margin(1,6));progressPanel.setVisibility(View.GONE);

  LinearLayout header=panel();
  headerClip=new FrameLayout(this);
  headerClip.setClipChildren(true);headerClip.setClipToPadding(true);
  headerClip.addView(header,new FrameLayout.LayoutParams(-1,-2));
  root.addView(headerClip,margin(2,9));
  headerClip.post(()->{if(headerExpandedHeight<0)headerExpandedHeight=headerClip.getHeight();});
  LinearLayout hero=new LinearLayout(this);hero.setGravity(Gravity.CENTER_VERTICAL);
  header.addView(hero);
  ImageView cover=new ImageView(this);
  hero.addView(cover,new LinearLayout.LayoutParams(dp(88),dp(76)));
  if(!playlist.entries.isEmpty()){
   PlaylistRepository.Entry first=playlist.entries.get(0);
   DownloadQueue.Item preview=new DownloadQueue.Item();
   preview.id="playlist-"+first.id;preview.url=first.url;
   QueueThumbnails.show(this,preview,cover,dp(9));
  }
  LinearLayout descriptions=column();
  descriptions.setPadding(dp(10),0,0,0);
  hero.addView(descriptions,new LinearLayout.LayoutParams(0,-2,1));
  heading=txt("",15);heading.setTypeface(null,Typeface.BOLD);
  heading.setMaxLines(3);heading.setEllipsize(TextUtils.TruncateAt.END);
  descriptions.addView(heading);
  owner=txt("",12);owner.setTextColor(ThemeStyle.subdued(this));descriptions.addView(owner);
  subheading=txt("",12);subheading.setTextColor(ThemeStyle.subdued(this));descriptions.addView(subheading);

  LinearLayout listCard=column();
  listCard.setPadding(dp(10),dp(8),dp(10),dp(6));
  listCard.setClipChildren(false);listCard.setClipToPadding(false);
  listCard.setBackground(ThemeStyle.rounded(ThemeStyle.cardColor(this),ThemeStyle.border(this),dp(14)));
  root.addView(listCard,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout section=new LinearLayout(this);section.setGravity(Gravity.CENTER_VERTICAL);
  listCard.addView(section,margin(2,0));
  TextView sectionTitle=txt("Видео в плейлисте",16);sectionTitle.setTypeface(null,Typeface.BOLD);
  section.post(()->sectionTitle.setTranslationY(
   sectionCount.getTop()+sectionCount.getBaseline()-sectionTitle.getTop()-sectionTitle.getBaseline()-dp(2)));
  section.addView(sectionTitle,new LinearLayout.LayoutParams(0,-2,1));
  sectionCount=txt("",12);sectionCount.setTextColor(ThemeStyle.subdued(this));section.addView(sectionCount);sectionCount.setTranslationY(-dp(2));
  View rule=new View(this);rule.setBackgroundColor(ThemeStyle.border(this));
  listCard.addView(rule,new LinearLayout.LayoutParams(-1,dp(1)));

  LinearLayout selectRow=new LinearLayout(this);
  selectRow.setGravity(Gravity.CENTER_VERTICAL);
  listCard.addView(selectRow,new LinearLayout.LayoutParams(-1,dp(46)));
  selectAll=new CheckBox(this);
  selectAll.setText(UiStrings.display(this,"Выбрать всё"));
  selectAll.setTextColor(ThemeStyle.text(this));selectAll.setTextSize(13);
  UiCheckboxStyle.apply(selectAll);UiCheckboxStyle.applyLabelGap(selectAll);
  selectRow.addView(selectAll,new LinearLayout.LayoutParams(0,dp(44),1));
  selectAll.setOnClickListener(v->{
   boolean all=true;for(int i:visibleRows)if(formatsWorker.ready(playlist.entries.get(i).id)&&!selected[i]){all=false;break;}
   for(int i:new ArrayList<>(visibleRows))
    if(formatsWorker.ready(playlist.entries.get(i).id))selected[i]=!all;
   persistSelection();
   if(selectionFilter!=0)rebuildVisible(false);
   else{adapter.notifyDataSetChanged();updateFooter();}
  });
  filterIcon=new ImageView(this);filterIcon.setScaleType(ImageView.ScaleType.CENTER);
  filterIcon.setContentDescription(UiStrings.display(this,"Настройки списка видео"));
  FrameLayout filterFrame=new FrameLayout(this);
  bulkQualityIcon=new ImageView(this);
  bulkQualityIcon.setScaleType(ImageView.ScaleType.CENTER);
  android.graphics.drawable.Drawable approved=getDrawable(R.drawable.ic_quality_480_symmetric).mutate();
  approved.setTint(ThemeStyle.text(this));
  bulkQualityIcon.setImageDrawable(approved);
  bulkQualityIcon.setBackground(ThemeStyle.rounded(ThemeStyle.buttonFill(this),
   ThemeStyle.buttonStroke(this),dp(9)));
  bulkQualityIcon.setContentDescription(UiStrings.display(this,"Качество для отмеченных"));
  LinearLayout.LayoutParams batchButton=new LinearLayout.LayoutParams(dp(42),dp(38));
  batchButton.rightMargin=dp(6);selectRow.addView(bulkQualityIcon,batchButton);
  bulkQualityIcon.setOnClickListener(v->showBulkQuality());
  selectRow.addView(filterFrame,new LinearLayout.LayoutParams(dp(42),dp(38)));
  filterFrame.addView(filterIcon,new FrameLayout.LayoutParams(-1,-1));
  filterDot=new View(this);
  android.graphics.drawable.GradientDrawable dotShape=new android.graphics.drawable.GradientDrawable();
  dotShape.setColor(ThemeStyle.RED);dotShape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
  filterDot.setBackground(dotShape);
  FrameLayout.LayoutParams dotPos=new FrameLayout.LayoutParams(dp(7),dp(7),Gravity.TOP|Gravity.END);
  dotPos.setMargins(0,dp(3),dp(3),0);filterFrame.addView(filterDot,dotPos);
  filterFrame.setOnClickListener(v->showListSettings());
  updateFilterIcon();
  list=new ListView(this);
  list.setDivider(new android.graphics.drawable.InsetDrawable(
    new android.graphics.drawable.ColorDrawable(ThemeStyle.border(this)),dp(10),0,dp(10),0));
  list.setDividerHeight(dp(1));list.setCacheColorHint(Color.TRANSPARENT);
  list.setBackgroundColor(ThemeStyle.cardColor(this));
  list.setVerticalScrollBarEnabled(true);
  listFrame=new FrameLayout(this);
  LinearLayout.LayoutParams listSpan=new LinearLayout.LayoutParams(-1,0,1);
  listSpan.setMargins(-dp(10),0,-dp(10),0);
  listCard.addView(listFrame,listSpan);
  listFrame.addView(list,new FrameLayout.LayoutParams(-1,-1));
  TextView empty=txt("Нет видео, соответствующих фильтру",13);
  empty.setGravity(Gravity.CENTER);
  listFrame.addView(empty,new FrameLayout.LayoutParams(-1,-1));
  list.setEmptyView(empty);
  adapter=new BaseAdapter(){
   @Override public int getCount(){return visibleRows.size();}
   @Override public Object getItem(int i){return playlist.entries.get(visibleRows.get(i));}
   @Override public long getItemId(int i){return visibleRows.get(i);}
   @Override public boolean hasStableIds(){return true;}
   @Override public View getView(int pos,View old,android.view.ViewGroup parent){
    Row h;
    if(old==null){
     h=new Row();
     LinearLayout row=new LinearLayout(PlaylistActivity.this);
     row.setGravity(Gravity.CENTER_VERTICAL);
     row.setPadding(dp(11),dp(4),dp(11),dp(4));row.setMinimumHeight(dp(67));
     h.check=new CheckBox(PlaylistActivity.this);
      UiCheckboxStyle.apply(h.check);
     row.addView(h.check,new LinearLayout.LayoutParams(dp(37),dp(43)));
     h.image=new ImageView(PlaylistActivity.this);
     row.addView(h.image,new LinearLayout.LayoutParams(dp(64),dp(53)));
     LinearLayout description=column();
     description.setPadding(dp(9),0,0,0);
     row.addView(description,new LinearLayout.LayoutParams(0,-2,1));
     h.name=txt("",13);h.name.setTypeface(null,Typeface.BOLD);
     h.name.setSingleLine(true);h.name.setEllipsize(TextUtils.TruncateAt.END);
     description.addView(h.name);
     LinearLayout detail=new LinearLayout(PlaylistActivity.this);
     detail.setGravity(Gravity.CENTER_VERTICAL);
     h.detail=detail;
     description.addView(detail);
     h.duration=txt("",11);h.duration.setTextColor(ThemeStyle.subdued(PlaylistActivity.this));
     h.duration.setSingleLine(true);h.duration.setEllipsize(TextUtils.TruncateAt.END);
     h.duration.setGravity(Gravity.CENTER_VERTICAL);
     detail.addView(h.duration,new LinearLayout.LayoutParams(durationWidth,dp(28)));
     h.qualityFrame=new FrameLayout(PlaylistActivity.this);
     h.qualityFrame.setBackground(ThemeStyle.rounded(ThemeStyle.field(PlaylistActivity.this),
       ThemeStyle.buttonStroke(PlaylistActivity.this),dp(6)));
     LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(dp(80),dp(20));
     qp.setMargins(dp(1),0,0,0);detail.addView(h.qualityFrame,qp);
     final Row holder=h;
     h.qualityValue=txt("",12);
     h.qualityValue.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL);
     h.qualityValue.setIncludeFontPadding(false);
     h.qualityValue.setSingleLine(true);
     h.qualityValue.setEllipsize(TextUtils.TruncateAt.END);
     h.qualityValue.setPadding(0,0,dp(18),0);
     h.qualityFrame.addView(h.qualityValue,new FrameLayout.LayoutParams(-1,-1));
     h.quality=spinner(PlaylistQualityPolicy.LABELS,()->holder.qualityLabel);
     h.quality.setBackgroundColor(Color.TRANSPARENT);
     h.quality.setPadding(dp(3),0,dp(18),0);
     h.quality.setDropDownWidth(dp(190));
     h.qualityFrame.addView(h.quality,new FrameLayout.LayoutParams(-1,-1));
     h.quality.setOnTouchListener((v,event)->{
      if(event.getActionMasked()==MotionEvent.ACTION_DOWN)holder.qualityUserInteraction=true;
      return false;
     });
     ImageView arrow=new ImageView(PlaylistActivity.this);
     arrow.setImageDrawable(new UiIcons("chevron",ThemeStyle.text(PlaylistActivity.this),dp(13)));
     arrow.setPadding(dp(4),0,dp(4),0);
     h.qualityFrame.addView(arrow,new FrameLayout.LayoutParams(dp(18),-1,Gravity.END));
     h.qualityLoader=new ProgressBar(PlaylistActivity.this,null,
       android.R.attr.progressBarStyleSmall);
     h.qualityLoader.setIndeterminateTintList(ColorStateList.valueOf(ThemeStyle.text(PlaylistActivity.this)));
     LinearLayout.LayoutParams loaderParams=new LinearLayout.LayoutParams(dp(20),dp(20));
     loaderParams.leftMargin=dp(6);
     detail.addView(h.qualityLoader,loaderParams);
      ((RotatingSpinner)h.quality).setArrow(arrow);
      // Closed spinner is 20dp; popup menu rows stay at 46dp.
     arrow.setOnClickListener(v->{
      if(!holder.quality.isEnabled())return;
      holder.qualityUserInteraction=true;holder.quality.performClick();
     });
     h.already=txt("",10);
     description.addView(h.already);
     row.setTag(h);h.root=row;old=row;
     row.setOnClickListener(v->{activeTitle=h.position;adapter.notifyDataSetChanged();});
     h.check.setOnClickListener(v->{activeTitle=h.position;toggle(h.position,h);adapter.notifyDataSetChanged();});
      h.quality.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
      @Override public void onNothingSelected(AdapterView<?> parent){}
      @Override public void onItemSelected(AdapterView<?> parent,View view,int chosen,long id){
       // Programmatic row binding and bulk updates do not count as user changes.
       if(!holder.qualityUserInteraction)return;
       if(holder.position<0||holder.position>=qualities.size()||chosen<0||
          chosen>=holder.qualityOptions.size())return;
       String value=holder.qualityOptions.get(chosen);
       if(value.equals(qualities.get(holder.position)))return;
       holder.qualityUserInteraction=false;
       qualities.set(holder.position,value);
       markQualityOverride(holder.position);
       persistQualities();updateRowQuality(holder);
      }
     });
    }else h=(Row)old.getTag();
    h.position=visibleRows.get(pos);
     PlaylistRepository.Entry entry=playlist.entries.get(h.position);
    String imageKey="playlist-"+entry.id;
    if(!imageKey.equals(h.imageId)||h.image.getDrawable() instanceof UiIcons){
     h.imageId=imageKey;
     DownloadQueue.Item imageItem=new DownloadQueue.Item();
     imageItem.id=imageKey;imageItem.url=entry.url;
     QueueThumbnails.show(PlaylistActivity.this,imageItem,h.image,dp(7));
    }
    h.check.setChecked(formatsWorker.ready(entry.id)&&selected[h.position]);
    h.root.setBackgroundColor(rowHighlight(activeTitle==h.position));
    h.name.setText(entry.title);
     TitleMarquee.show(h.name,activeTitle==h.position);
     TitleMarquee.bind(h.name,()->{activeTitle=h.position;adapter.notifyDataSetChanged();});
    h.duration.setText(UiStrings.display(PlaylistActivity.this, entry.duration<0?"Длительность неизвестна":QueueRules.readableDuration(entry.duration)));
    h.qualityUserInteraction=false;
    boolean waiting=!formatsWorker.ready(entry.id);
    List<String> allowed=formatsWorker.choices(entry.id);
    if(!allowed.equals(h.qualityOptions)){
     h.qualityOptions=new ArrayList<>(allowed);
     replaceRowQualityChoices(h);
    }
    int choice=h.qualityOptions.indexOf(qualities.get(h.position));
    h.quality.setSelection(waiting?0:Math.max(0,choice));
    h.quality.setEnabled(!waiting);
    h.qualityLoader.setVisibility(waiting?View.VISIBLE:View.GONE);
    h.qualityValue.setTextColor(waiting?ThemeStyle.subdued(PlaylistActivity.this):ThemeStyle.text(PlaylistActivity.this));
    h.check.setEnabled(!waiting);h.check.setAlpha(waiting?.55f:1f);
    updateRowQuality(h);
     int status=h.position<videoStatus.length?videoStatus[h.position]:PlaylistHistory.NEW;
     h.already.setVisibility(status==PlaylistHistory.NEW?View.GONE:View.VISIBLE);
     h.already.setText(UiStrings.display(PlaylistActivity.this,
       status==PlaylistHistory.DOWNLOADED?"Уже скачано":"Уже добавлено в очередь"));
     h.already.setTextColor(status==PlaylistHistory.DOWNLOADED?0xFF4EC67B:0xFFE5AD38);
    return old;
   }
  };
  final int[] savedScroll=scrollSession.get(source);
  list.setAdapter(adapter);
  listArrowUp=playlistArrow(true);listArrowDown=playlistArrow(false);
  list.setOnScrollListener(new android.widget.AbsListView.OnScrollListener(){
   private boolean moving=false;
   @Override public void onScrollStateChanged(android.widget.AbsListView v,int state){
    moving=state!=android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE;
   }
   @Override public void onScroll(android.widget.AbsListView v,int first,int visible,int total){
    promoteVisibleFormats();
    if(list.getChildCount()>0){
     int top=list.getChildAt(0).getTop();
     if(moving&&!returningToTop&&!scrollRestoring&&priorListFirst>=0){
      int delta=(first-priorListFirst)*dp(70)+priorListTop-top;
      if(delta>0&&headerCollapsePx<headerExpandedHeight)
       setHeaderCollapse(headerCollapsePx+delta);
      else if(delta<0&&first==0&&!list.canScrollVertically(-1))
       setHeaderCollapse(headerCollapsePx+delta);
     }
     priorListFirst=first;priorListTop=top;
    }
    if(!scrollRestoring&&visible>0&&list.getChildCount()>0)
     scrollSession.put(source,new int[]{first,list.getChildAt(0).getTop()});
    list.post(PlaylistActivity.this::updateListArrows);
   }
  });
  list.post(()->{
   if(savedScroll!=null){
    list.setSelectionFromTop(savedScroll[0],savedScroll[1]);
    if(savedScroll[0]>0)setHeaderCollapse(headerExpandedHeight);
   }
   list.post(()->{scrollRestoring=false;updateListArrows();});
  });

  LinearLayout bottom=column();root.addView(bottom,margin(7,0));
  addButton=new Button(this);addButton.setText(UiStrings.display(PlaylistActivity.this, "Добавить в очередь"));
  ThemeStyle.styleButton(addButton,true);
  ThemeStyle.icon(addButton,"plus",18);
  bottom.addView(addButton,new LinearLayout.LayoutParams(-1,dp(46)));
  addButton.setOnClickListener(v->addSelected());
 }
 /** Hide the cover as the list scrolls; the toolbar and list heading stay fixed. */
 private void setHeaderCollapse(int pixels){
  if(headerClip==null||headerExpandedHeight<=0)return;
  int next=Math.max(0,Math.min(headerExpandedHeight,pixels));
  if(headerCollapsePx==next)return;
  headerCollapsePx=next;
  LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)headerClip.getLayoutParams();
  p.height=headerExpandedHeight-next;
  float fraction=(headerExpandedHeight-next)/(float)headerExpandedHeight;
  p.topMargin=Math.round(dp(2)*fraction);
  p.bottomMargin=Math.round(dp(9)*fraction);
  headerClip.setLayoutParams(p);
  // Translate upward inside the clipping viewport, rather than cutting the cover from below.
  if(headerClip.getChildCount()>0)headerClip.getChildAt(0).setTranslationY(-next);
  updateListArrows();
 }
 private void scrollToPlaylistTop(){
  if(list==null)return;
  returningToTop=true;
  list.smoothScrollToPositionFromTop(0,0,600);
  if(headerExpandedHeight<=0){returningToTop=false;return;}
  android.animation.ValueAnimator anim=android.animation.ValueAnimator.ofInt(headerCollapsePx,0);
  anim.setDuration(600);
  anim.addUpdateListener(a->setHeaderCollapse((Integer)a.getAnimatedValue()));
  anim.addListener(new android.animation.AnimatorListenerAdapter(){
   @Override public void onAnimationEnd(android.animation.Animator a){
    returningToTop=false;priorListFirst=-1;updateListArrows();
   }
  });anim.start();
 }
 /** Floating queue-style arrows control only the playlist ListView. */
 private android.graphics.drawable.GradientDrawable arrowSkin(boolean pressed){
  int fill=ThemeStyle.buttonFill(this);
  fill=(fill&0x00FFFFFF)|(pressed?0xFF000000:0xD9000000);
  return ThemeStyle.rounded(fill,ThemeStyle.buttonStroke(this),dp(11));
 }
 private ImageView playlistArrow(boolean up){
  ImageView button=new ImageView(this);
  button.setImageDrawable(new UiIcons(up?"arrow-outline-up":"arrow-outline-down",ThemeStyle.text(this),dp(21)));
  button.setScaleType(ImageView.ScaleType.CENTER);
  button.setBackground(arrowSkin(false));button.setElevation(dp(6));
  button.setContentDescription(UiStrings.display(this,up?"Прокрутить вверх":"Прокрутить вниз"));
  FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(78),dp(38),
   Gravity.CENTER_HORIZONTAL|(up?Gravity.TOP:Gravity.BOTTOM));
  if(up)p.topMargin=dp(8);else p.bottomMargin=dp(8);
  listFrame.addView(button,p);button.setVisibility(View.GONE);button.setAlpha(0f);
  final boolean[] held={false};
  final Runnable edge=()->{
   held[0]=true;
   if(list.getCount()==0)return;
   if(up)scrollToPlaylistTop();
   else list.smoothScrollToPositionFromTop(list.getCount()-1,0,600);
  };
  button.setOnTouchListener((v,event)->{
   switch(event.getActionMasked()){
    case MotionEvent.ACTION_DOWN:
     held[0]=false;button.setBackground(arrowSkin(true));
     arrowHandler.postDelayed(edge,600);return true;
    case MotionEvent.ACTION_UP:
     button.setBackground(arrowSkin(false));arrowHandler.removeCallbacks(edge);
     if(!held[0]){
      if(up){
       if(Release058Rules.playlistArrowShouldJumpToTop(false,!list.canScrollVertically(-1)))
        scrollToPlaylistTop();
       else list.smoothScrollBy(-list.getHeight(),600);
      }else list.smoothScrollBy(list.getHeight(),600);
     }
     return true;
    case MotionEvent.ACTION_CANCEL:
     arrowHandler.removeCallbacks(edge);button.setBackground(arrowSkin(false));return true;
   }
   return true;
  });
  return button;
 }
 private void fadeListArrow(ImageView button,boolean show){
  if(button==null||Boolean.valueOf(show).equals(button.getTag()))return;
  button.setTag(show);button.animate().cancel();
  if(show){
   button.setVisibility(View.VISIBLE);button.setAlpha(0f);
   button.animate().alpha(1f).setDuration(300).start();
  }else button.animate().alpha(0f).setDuration(300).withEndAction(()->{
    if(Boolean.FALSE.equals(button.getTag()))button.setVisibility(View.GONE);
  }).start();
 }
 private void updateListArrows(){
  if(list==null)return;
  fadeListArrow(listArrowUp,headerExpandedHeight>0&&headerCollapsePx>=headerExpandedHeight);
  fadeListArrow(listArrowDown,list.canScrollVertically(1));
 }
 private int rowHighlight(boolean active){
  int base=ThemeStyle.cardColor(this);
  if(!active)return base;
  int tint=ThemeStyle.isDark(this)?Palette050.NIGHT_POPUP:ThemeStyle.field(this);
  float amount=.30f;
  return Color.rgb(
   Math.round(Color.red(base)*(1-amount)+Color.red(tint)*amount),
   Math.round(Color.green(base)*(1-amount)+Color.green(tint)*amount),
   Math.round(Color.blue(base)*(1-amount)+Color.blue(tint)*amount));
 }
 private static final class Row{
  LinearLayout root;CheckBox check;ImageView image;TextView name,duration,already;
  Spinner quality;FrameLayout qualityFrame;LinearLayout detail;
  ProgressBar qualityLoader;
  List<String> qualityOptions=Collections.emptyList();
  TextView qualityValue;boolean qualityUserInteraction=false;String qualityLabel="";
  String imageId="";int position=-1;
 }
 private Spinner spinner(String[] labels){return spinner(labels,null);}
 private Spinner spinner(String[] labels,java.util.function.Supplier<String> shown){
  Spinner s=new RotatingSpinner(this);
  ArrayAdapter<String> a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,UiStrings.translateChoices(this,labels)){
   @Override public View getView(int p,View old,android.view.ViewGroup parent){
    TextView t=(TextView)super.getView(p,old,parent);
    t.setTextColor(ThemeStyle.text(PlaylistActivity.this));t.setTextSize(12);
    if(shown!=null){t.setText(shown.get());t.setGravity(Gravity.CENTER_VERTICAL);t.setIncludeFontPadding(false);t.setTextColor(Color.TRANSPARENT);}
    return t;
   }
   @Override public View getDropDownView(int p,View old,android.view.ViewGroup parent){
    TextView t=(TextView)super.getDropDownView(p,old,parent);
    t.setTextColor(ThemeStyle.text(PlaylistActivity.this));t.setTextSize(13);
    t.setPadding(dp(12),0,dp(12),0);
     t.setGravity(Gravity.CENTER_VERTICAL);
     t.setLayoutParams(new AbsListView.LayoutParams(-1,dp(ThemeStyle.POPUP_ITEM_HEIGHT_DP)));
    t.setBackground(ThemeStyle.popupRowBackground(PlaylistActivity.this,
      p==s.getSelectedItemPosition(),p,getCount()));
    return t;
   }
  };
  a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
  s.setAdapter(a);s.setPopupBackgroundDrawable(ThemeStyle.popupBackground(this));
  return s;
 }
 /** Restrict popup options to this video's confirmed heights. */
 private void replaceRowQualityChoices(Row row){
  String[] labels=new String[row.qualityOptions.size()];
  for(int i=0;i<labels.length;i++)
   labels[i]=UiStrings.display(this,PlaylistQualityPolicy.LABELS[
     PlaylistQualityPolicy.index(row.qualityOptions.get(i))]);
  final RotatingSpinner spinner=(RotatingSpinner)row.quality;
  ArrayAdapter<String> choices=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,labels){
   @Override public View getView(int index,View recycled,ViewGroup parent){
    TextView t=(TextView)super.getView(index,recycled,parent);
    t.setTextColor(Color.TRANSPARENT);t.setTextSize(12);
    t.setGravity(Gravity.CENTER_VERTICAL);t.setIncludeFontPadding(false);return t;
   }
   @Override public View getDropDownView(int index,View recycled,ViewGroup parent){
    TextView t=(TextView)super.getDropDownView(index,recycled,parent);
    t.setTextColor(ThemeStyle.text(PlaylistActivity.this));t.setTextSize(13);
    t.setPadding(dp(12),0,dp(12),0);t.setGravity(Gravity.CENTER_VERTICAL);
    t.setLayoutParams(new AbsListView.LayoutParams(-1,dp(ThemeStyle.POPUP_ITEM_HEIGHT_DP)));
    t.setBackground(ThemeStyle.popupRowBackground(PlaylistActivity.this,
      index==spinner.getSelectedItemPosition(),index,getCount()));
    return t;
   }
  };
  row.qualityUserInteraction=false;
  spinner.setAdapter(choices);
  spinner.setPopupBackgroundDrawable(ThemeStyle.popupBackground(this));
 }
 private void promoteVisibleFormats(){
  if(formatsWorker==null||list==null||playlist==null)return;
  List<PlaylistRepository.Entry> visible=new ArrayList<>();
  int first=list.getFirstVisiblePosition();
  for(int i=first;i<first+list.getChildCount()&&i<visibleRows.size();i++)
   visible.add(playlist.entries.get(visibleRows.get(i)));
  formatsWorker.prioritize(visible);
 }
 private String qualityOverrideKey(int index){
  return source.hashCode()+"_"+playlist.entries.get(index).id;
 }
 private boolean hasQualityOverride(int index){
  return getSharedPreferences("playlist_quality_display_v055",0)
    .getBoolean(qualityOverrideKey(index),false);
 }
 private void markQualityOverride(int index){
  getSharedPreferences("playlist_quality_display_v055",0).edit()
    .putBoolean(qualityOverrideKey(index),true).apply();
 }
 private void updateRowQuality(Row row){
  if(row.position<0||row.position>=qualities.size())return;
  PlaylistRepository.Entry entry=playlist.entries.get(row.position);
  row.qualityLabel=UiStrings.display(this,PlaylistQualityText.label(
    row.position<videoStatus.length?videoStatus[row.position]:PlaylistHistory.NEW,
    queuedQualities.containsKey(entry.url)?queuedQualities.get(entry.url):"best",
    PlaylistHistory.actualHeight(this,entry.url),
    qualities.get(row.position),hasQualityOverride(row.position)));
  // Draw closed-state text ourselves: OEM Spinner padding otherwise truncates it.
  row.qualityValue.setText(formatsWorker.ready(entry.id)?row.qualityLabel:"Максимальное");
  int textWidth=(int)Math.ceil(row.qualityValue.getPaint().measureText(row.qualityValue.getText().toString()));
  int available=row.detail.getWidth()-durationWidth-dp(1)-
    (formatsWorker.ready(entry.id)?0:dp(26));
  if(row.detail.getWidth()==0){
   row.detail.post(()->{
    if(row.root.isAttachedToWindow())updateRowQuality(row);
   });
   available=getResources().getDisplayMetrics().widthPixels-dp(165)-durationWidth;
  }
  int wanted=Release057Rules.qualityWidth(textWidth,dp(3+18+7),
    Math.max(dp(50),available));
  if(row.qualityFrame.getLayoutParams().width!=wanted){
   row.qualityFrame.getLayoutParams().width=wanted;
   row.qualityFrame.requestLayout();
  }
 }
 private void toggle(int position,Row row){
  if(position<0||position>=selected.length||animatingSelection||
     !formatsWorker.ready(playlist.entries.get(position).id))return;
  selected[position]=!selected[position];persistSelection();
  row.check.setChecked(selected[position]);updateFooter();
  boolean disappears=selectionFilter==1&&!selected[position]||
    selectionFilter==2&&selected[position];
  if(!disappears)return;
  // Animate height so adjacent list items slide upward instead of jumping.
  animatingSelection=true;
  final View view=row.root;
  final int initial=Math.max(view.getHeight(),dp(65));
  android.animation.ValueAnimator fade=android.animation.ValueAnimator.ofInt(initial,0);
  fade.setDuration(220);
  fade.addUpdateListener(animation->{
   android.view.ViewGroup.LayoutParams params=view.getLayoutParams();
   params.height=(Integer)animation.getAnimatedValue();
   view.setLayoutParams(params);
   view.setAlpha(1f-animation.getAnimatedFraction());
  });
  fade.addListener(new android.animation.AnimatorListenerAdapter(){
   @Override public void onAnimationEnd(android.animation.Animator animation){
    view.setAlpha(1f);
    android.view.ViewGroup.LayoutParams params=view.getLayoutParams();
    params.height=-2;view.setLayoutParams(params);
    animatingSelection=false;
    rebuildVisible(false);
   }
  });
  fade.start();
 }
 private void persistSelection(){
  PlaylistCache.saveSelectionById(this,source,playlist,selected);
  PlaylistCache.saveSelection(this,source,selected);
 }
 private void persistQualities(){PlaylistCache.saveQualities(this,source,playlist,qualities);}
 private void updateFooter(){
  if(selected==null||selectAll==null||addButton==null)return;
  int count=0;for(boolean flag:selected)if(flag)count++;
  sectionCount.setText(UiStrings.display(this,"Выбрано: "+count+" из "+selected.length));
  addButton.setText(UiStrings.display(this,"Добавить в очередь")+" ("+count+")");
  addButton.setEnabled(count>0);
  boolean all=true;int enabled=0;
  for(int i:visibleRows)if(formatsWorker.ready(playlist.entries.get(i).id)){
   enabled++;if(!selected[i])all=false;
  }
  selectAll.setChecked(enabled>0&&all);
  selectAll.setEnabled(enabled>0);selectAll.setAlpha(enabled>0?1f:.55f);
 }
 private void addCompactSpinner(LinearLayout content,Spinner spinner){
  FrameLayout frame=new FrameLayout(this);
  frame.setBackground(ThemeStyle.rounded(ThemeStyle.field(this),ThemeStyle.buttonStroke(this),dp(9)));
  content.addView(frame,new LinearLayout.LayoutParams(-1,dp(46)));
  spinner.setBackgroundColor(Color.TRANSPARENT);
  spinner.setPadding(dp(12),0,dp(32),0);
  frame.addView(spinner,new FrameLayout.LayoutParams(-1,-1));
  ImageView arrow=new ImageView(this);
  arrow.setImageDrawable(new UiIcons("chevron",ThemeStyle.text(this),dp(13)));
  arrow.setPadding(dp(9),0,dp(9),0);
  FrameLayout.LayoutParams location=new FrameLayout.LayoutParams(dp(32),-1,Gravity.END);
  frame.addView(arrow,location);
  ((RotatingSpinner)spinner).setArrow(arrow);
  arrow.setOnClickListener(v->spinner.performClick());
 }
 private Spinner settingsChoice(LinearLayout content,String title,String[] choices,int value){
  TextView heading=txt(title,13);
  heading.setTextColor(ThemeStyle.subdued(this));
  LinearLayout.LayoutParams labelSpace=margin(8,4);
  content.addView(heading,labelSpace);
  Spinner spinner=spinner(choices);
  spinner.setSelection(Math.max(0,Math.min(choices.length-1,value)));
  addCompactSpinner(content,spinner);

  return spinner;
 }
 private void showListSettings(){
  ScrollView scroll=new ScrollView(this);
  LinearLayout content=column();content.setPadding(dp(17),dp(6),dp(17),dp(9));
  scroll.addView(content);
  TextView title=txt("Фильтр отображения загрузок",15);
  title.setTypeface(null,Typeface.BOLD);content.addView(title,margin(7,8));
  Spinner choose=settingsChoice(content,"Отобразить отмеченные/неотмеченные загрузки",
    new String[]{"Все","Только отмеченные","Только неотмеченные"},selectionFilter);
  Spinner downloads=settingsChoice(content,"Отобразить скачанные/нескачанные загрузки",
    new String[]{"Все","Скачанные","Нескачанные"},downloadFilter);
  Spinner queued=settingsChoice(content,"Отобразить находящиеся/отсутствующие в очереди загрузки",
    new String[]{"Все","Уже добавленные","Ещё не добавленные"},queueFilter);
  CheckBox sort=new CheckBox(this);
  UiCheckboxStyle.apply(sort);UiCheckboxStyle.applyLabelGap(sort);
  sort.setText(UiStrings.display(this,"Сначала отмеченные"));
  sort.setTextColor(ThemeStyle.text(this));sort.setTextSize(13);
  sort.setChecked(selectedFirst);content.addView(sort,margin(12,4));
  TextView reset=txt("Сбросить фильтры",13);
  reset.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
  reset.setPadding(0,dp(10),dp(1),dp(1));content.addView(reset,margin(5,3));
  Runnable updateReset=()->{
   boolean changed=Release058Rules.hasActiveFilters(
    choose.getSelectedItemPosition(),downloads.getSelectedItemPosition(),
    queued.getSelectedItemPosition(),sort.isChecked());
   reset.setEnabled(changed);
   reset.setTextColor(changed?ThemeStyle.text(this):ThemeStyle.subdued(this));
   reset.setAlpha(changed?1f:.55f);
  };
  AdapterView.OnItemSelectedListener changes=new AdapterView.OnItemSelectedListener(){
   @Override public void onItemSelected(AdapterView<?> parent,View view,int pos,long id){updateReset.run();}
   @Override public void onNothingSelected(AdapterView<?> parent){updateReset.run();}
  };
  choose.setOnItemSelectedListener(changes);
  downloads.setOnItemSelectedListener(changes);
  queued.setOnItemSelectedListener(changes);
  sort.setOnCheckedChangeListener((button,checked)->updateReset.run());
  reset.setOnClickListener(v->{
   choose.setSelection(0);downloads.setSelection(0);queued.setSelection(0);
   sort.setChecked(false);updateReset.run();
  });
  updateReset.run();
  ThemeStyle.dialog(this)
   .setTitle(UiStrings.display(this,"Настройки списка видео"))
   .setView(scroll)
   .setNegativeButton(UiStrings.display(this,"Отмена"),null)
   .setPositiveButton(UiStrings.display(this,"Применить"),(d,w)->{
    selectionFilter=choose.getSelectedItemPosition();
    downloadFilter=downloads.getSelectedItemPosition();
    queueFilter=queued.getSelectedItemPosition();selectedFirst=sort.isChecked();
    getSharedPreferences("playlist_display",0).edit()
      .putInt("selection",selectionFilter).putInt("download",downloadFilter)
      .putInt("queue",queueFilter).putBoolean("selectedFirst",selectedFirst).apply();
    int first=list.getFirstVisiblePosition();
    int anchor=first>=0&&first<visibleRows.size()?visibleRows.get(first):0;
    int offset=list.getChildCount()>0?list.getChildAt(0).getTop():0;
    scrollRestoring=true;
    rebuildVisible(true);
    int restored=Release057Rules.anchorPosition(visibleRows,anchor);
    list.post(()->{
     if(!visibleRows.isEmpty())list.setSelectionFromTop(restored,offset);
     list.post(()->{scrollRestoring=false;updateListArrows();});
    });
   }).show();
 }
 private void showBulkQuality(){
  LinearLayout content=column();content.setPadding(dp(17),dp(6),dp(17),dp(12));
  Spinner bulk=spinner(PlaylistQualityPolicy.LABELS);
  bulk.setSelection(PlaylistQualityPolicy.index(quality));
  addCompactSpinner(content,bulk);
  AlertDialog dialog=ThemeStyle.dialog(this)
    .setTitle(UiStrings.display(this,"Качество для отмеченных"))
    .setView(content)
    .setNegativeButton(UiStrings.display(this,"Отмена"),null)
    .setPositiveButton(UiStrings.display(this,"Применить"),null)
    .create();
  dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(click->{
   int index=bulk.getSelectedItemPosition(),count=0;
   for(boolean checked:selected)if(checked)count++;
   if(count==0){
    Toast.makeText(this,UiStrings.display(this,"Выберите видео"),Toast.LENGTH_SHORT).show();return;
   }
   if(index<0||index>=PlaylistQualityPolicy.VALUES.length)return;
   qualities=PlaylistQualityPolicy.applyToSelected(qualities,selected,PlaylistQualityPolicy.VALUES[index]);
   for(int i=0;i<selected.length;i++)if(selected[i]){
    List<String> available=formatsWorker.cached(playlist.entries.get(i).id);
    if(available!=null)qualities.set(i,Release058Rules.closestSupported(qualities.get(i),available));
    markQualityOverride(i);
   }
   quality=PlaylistQualityPolicy.VALUES[index];
   persistQualities();adapter.notifyDataSetChanged();
   Toast.makeText(this,UiStrings.display(this,"Качество отмеченных обновлено"),Toast.LENGTH_SHORT).show();
   dialog.dismiss();
  }));
  dialog.show();
 }
 private void addSelected(){
  int count=0,duplicates=0;
  Set<String> already=previouslyAdded();
  for(int i=0;i<selected.length;i++)if(selected[i]){
   count++;if(already.contains(playlist.entries.get(i).url))duplicates++;
  }
  if(count==0){Toast.makeText(this,UiStrings.display(PlaylistActivity.this, "Выберите видео"),Toast.LENGTH_SHORT).show();return;}
  if(duplicates>0){
   ThemeStyle.dialog(this).setTitle(UiStrings.display(PlaylistActivity.this, "Повторное добавление"))
    .setMessage(UiStrings.display(PlaylistActivity.this, "Уже добавлено: "+duplicates+". Добавить их повторно?"))
    .setNegativeButton(UiStrings.display(PlaylistActivity.this, "Отмена"),null)
    .setPositiveButton(UiStrings.display(PlaylistActivity.this, "Добавить"),(d,w)->reallyAdd()).show();
  }else reallyAdd();
 }
 private void reallyAdd(){
  try{
   List<Boolean> flags=new ArrayList<>();for(boolean flag:selected)flags.add(flag);
   int count=DownloadQueue.enqueuePlaylist(this,playlist,flags,
    "mp3".equals(format)||"m4a".equals(format),qualities,proxy,folder,priority,format,source);
   // A successfully enqueued selection is consumed; retain each video's quality.
   Arrays.fill(selected,false);
   persistSelection();
   refreshStatuses();rebuildVisible(true);DownloadService.kick(this);
   QueueNotice.show(this,QueueNotice.message(count,priority),()->{
     android.content.Intent intent=new android.content.Intent(this,MainActivity.class);
     intent.addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
        android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
     intent.putExtra("open_queue_top",true);startActivity(intent);
    });
  }catch(Exception ex){
   ThemeStyle.dialog(this).setMessage(UiStrings.display(PlaylistActivity.this, ex.getMessage())).setPositiveButton(UiStrings.display(PlaylistActivity.this, "ОК"),null).show();
  }
 }
 private void updatePlaylist(){
  if(!PlaylistRefreshManager.start(getApplicationContext(),source,proxy)){
   Toast.makeText(this,UiStrings.display(this,"Другой плейлист уже обновляется"),Toast.LENGTH_LONG).show();
   return;
  }
  handler.removeCallbacks(tick);tick.run();
 }
 private void cancelRefresh(){PlaylistRefreshManager.cancel(source);}
 @Override protected void onResume(){
  super.onResume();
  if(progressPanel!=null){
    refreshStatuses();rebuildVisible(false);
    handler.removeCallbacks(tick);tick.run();
   }
 }
 @Override protected void onPause(){
  if(!scrollRestoring&&list!=null&&list.getChildCount()>0)
   scrollSession.put(source,new int[]{list.getFirstVisiblePosition(),list.getChildAt(0).getTop()});
  handler.removeCallbacks(tick);super.onPause();
 }
 @Override protected void onDestroy(){
  handler.removeCallbacks(tick);
  if(cancelExpiry!=null)handler.removeCallbacks(cancelExpiry);
  if(formatsWorker!=null)formatsWorker.close();
  super.onDestroy();
 }

}
