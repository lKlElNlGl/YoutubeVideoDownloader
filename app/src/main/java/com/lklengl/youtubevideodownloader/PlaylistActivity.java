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
 private boolean[] selected;
 private List<String> qualities;
 private ListView list;
 private BaseAdapter adapter;
 private TextView heading,subheading,owner,progressText,refreshControl,sectionCount;
 private CheckBox selectAll;
 private ImageView filterIcon;
 private View filterDot;
 private final List<Integer> visibleRows=new ArrayList<>();
 private int[] videoStatus=new int[0];
 private int selectionFilter=0,downloadFilter=0,queueFilter=0;
 private boolean selectedFirst=false,animatingSelection=false;
 private LinearLayout progressPanel;
 private Button addButton;
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
    observedRunning=true;refreshStart=state.startedMs;
    progressPanel.setVisibility(View.VISIBLE);refreshControl.setEnabled(false);
    long seconds=Math.max(0,(SystemClock.elapsedRealtime()-refreshStart)/1000L);
    int index=seconds<12?0:1+(int)((seconds-12)/8%(waitingMessages.length-1));
    progressText.setText(UiStrings.display(PlaylistActivity.this,
      "Обновление плейлиста · "+String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60)+
       "\n"+(state.message.isEmpty()?waitingMessages[index]:state.message)));
   }else{
    progressPanel.setVisibility(View.GONE);refreshControl.setEnabled(true);
    if(observedRunning&&source.equals(state.url)){
     if(state.stage==PlaylistRefreshManager.Stage.DONE){
      refreshData(true);
      Toast.makeText(PlaylistActivity.this,UiStrings.display(PlaylistActivity.this,
       "Плейлист обновлён"),Toast.LENGTH_SHORT).show();
     }else if(state.stage==PlaylistRefreshManager.Stage.ERROR){
      ThemeStyle.dialog(PlaylistActivity.this)
       .setTitle(UiStrings.display(PlaylistActivity.this,"Не удалось обновить плейлист"))
       .setMessage(UiStrings.display(PlaylistActivity.this,state.error))
       .setPositiveButton(UiStrings.display(PlaylistActivity.this,"ОК"),null).show();
     }
    }
    observedRunning=false;
   }
   handler.postDelayed(this,600);
  }
 };
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
  buildScreen();refreshData(false);
 }
 private Set<String> previouslyAdded(){
  Set<String> urls=new HashSet<>();
  for(PlaylistRepository.Entry entry:playlist.entries)
   if(PlaylistHistory.hasAdded(this,entry.url))urls.add(entry.url);
  return urls;
 }
 private ColorStateList checkTint(){
  return new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},
   new int[]{ThemeStyle.RED,ThemeStyle.isDark(this)?0xFFF4F5F8:0xFF596477});
 }
 private void refreshStatuses(){
  PlaylistHistory.syncFromQueue(this);
  videoStatus=new int[playlist.entries.size()];
  for(int i=0;i<videoStatus.length;i++)
   videoStatus[i]=PlaylistHistory.status(this,playlist.entries.get(i).url);
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
  for(int i=0;i<flags.size();i++)selected[i]=flags.get(i);
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
  rebuildVisible(true);
 }
 private void buildScreen(){
  LinearLayout root=column();
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
  ProgressBar spinner=new ProgressBar(this);
  spinner.setIndeterminateTintList(ColorStateList.valueOf(ThemeStyle.RED));
  statusRow.addView(spinner,new LinearLayout.LayoutParams(dp(33),dp(33)));
  progressText=txt("",12);progressText.setTextColor(ThemeStyle.subdued(this));
  LinearLayout.LayoutParams progressTextParams=new LinearLayout.LayoutParams(0,-2,1);
  progressTextParams.setMargins(dp(10),0,0,0);statusRow.addView(progressText,progressTextParams);
  TextView cancel=txt("Отмена",13);cancel.setTextColor(ThemeStyle.RED);
  cancel.setPadding(dp(12),dp(8),dp(8),dp(8));statusRow.addView(cancel);
  cancel.setOnClickListener(v->cancelRefresh());
  root.addView(progressPanel,margin(1,6));progressPanel.setVisibility(View.GONE);

  LinearLayout header=panel();root.addView(header,margin(2,9));
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
  listCard.setBackground(ThemeStyle.rounded(ThemeStyle.cardColor(this),ThemeStyle.border(this),dp(14)));
  root.addView(listCard,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout section=new LinearLayout(this);section.setGravity(Gravity.CENTER_VERTICAL);
  listCard.addView(section,margin(2,0));
  TextView sectionTitle=txt("Видео в плейлисте",16);sectionTitle.setTypeface(null,Typeface.BOLD);
  sectionTitle.setTranslationY(dp(3));
  section.addView(sectionTitle,new LinearLayout.LayoutParams(0,-2,1));
  sectionCount=txt("",12);sectionCount.setTextColor(ThemeStyle.subdued(this));section.addView(sectionCount);
  View rule=new View(this);rule.setBackgroundColor(ThemeStyle.border(this));
  listCard.addView(rule,new LinearLayout.LayoutParams(-1,dp(1)));

  LinearLayout selectRow=new LinearLayout(this);
  selectRow.setGravity(Gravity.CENTER_VERTICAL);
  listCard.addView(selectRow,new LinearLayout.LayoutParams(-1,dp(46)));
  selectAll=new CheckBox(this);
  selectAll.setText(UiStrings.display(this,"Выбрать всё"));
  selectAll.setTextColor(ThemeStyle.text(this));selectAll.setTextSize(13);
  UiCheckboxStyle.apply(selectAll);
  selectRow.addView(selectAll,new LinearLayout.LayoutParams(0,dp(44),1));
  selectAll.setOnClickListener(v->{
   boolean all=true;for(int i:visibleRows)if(!selected[i]){all=false;break;}
   for(int i:new ArrayList<>(visibleRows))selected[i]=!all;
   persistSelection();
   if(selectionFilter!=0)rebuildVisible(false);
   else{adapter.notifyDataSetChanged();updateFooter();}
  });
  filterIcon=new ImageView(this);filterIcon.setScaleType(ImageView.ScaleType.CENTER);
  filterIcon.setContentDescription(UiStrings.display(this,"Настройки списка видео"));
  FrameLayout filterFrame=new FrameLayout(this);
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
  list.setDivider(new android.graphics.drawable.ColorDrawable(ThemeStyle.border(this)));
  list.setDividerHeight(dp(1));list.setCacheColorHint(Color.TRANSPARENT);
  list.setBackgroundColor(ThemeStyle.cardColor(this));
  list.setVerticalScrollBarEnabled(true);
  FrameLayout listFrame=new FrameLayout(this);
  listCard.addView(listFrame,new LinearLayout.LayoutParams(-1,0,1));
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
     row.setPadding(dp(1),dp(4),dp(1),dp(4));row.setMinimumHeight(dp(67));
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
     h.duration=txt("",11);h.duration.setTextColor(ThemeStyle.subdued(PlaylistActivity.this));
     description.addView(h.duration);
     h.quality=txt("",11);
     h.quality.setTextColor(ThemeStyle.text(PlaylistActivity.this));
     h.quality.setPadding(dp(8),dp(3),dp(8),dp(3));
  h.quality.setSingleLine(true);
      h.quality.setMinHeight(dp(27));
      h.quality.setCompoundDrawablesWithIntrinsicBounds(null,null,
        new UiIcons("chevron",ThemeStyle.subdued(PlaylistActivity.this),dp(12)),null);
      h.quality.setCompoundDrawablePadding(dp(4));
      h.quality.setBackground(ThemeStyle.rounded(ThemeStyle.buttonFill(PlaylistActivity.this),
        ThemeStyle.buttonStroke(PlaylistActivity.this),dp(6)));
      LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(dp(158),dp(31));
      qp.setMargins(0,dp(3),0,0);description.addView(h.quality,qp);
      h.already=txt("",10);
     description.addView(h.already);
     row.setTag(h);h.root=row;old=row;
     row.setOnClickListener(v->toggle(h.position,h));
     h.check.setOnClickListener(v->toggle(h.position,h));
     h.quality.setOnClickListener(v->chooseQuality(h.position));
    }else h=(Row)old.getTag();
    h.position=visibleRows.get(pos);
     PlaylistRepository.Entry entry=playlist.entries.get(h.position);
    String imageKey="playlist-"+entry.id;
    if(!imageKey.equals(h.imageId)){
     h.imageId=imageKey;
     DownloadQueue.Item imageItem=new DownloadQueue.Item();
     imageItem.id=imageKey;imageItem.url=entry.url;
     QueueThumbnails.show(PlaylistActivity.this,imageItem,h.image,dp(7));
    }
    h.check.setChecked(selected[h.position]);
    h.name.setText(entry.title);
    h.duration.setText(UiStrings.display(PlaylistActivity.this, entry.duration<0?"Длительность неизвестна":QueueRules.readableDuration(entry.duration)));
    h.quality.setText(UiStrings.display(PlaylistActivity.this,PlaylistQualityText.label(
       h.position<videoStatus.length?videoStatus[h.position]:PlaylistHistory.NEW,
       PlaylistHistory.queuedQuality(PlaylistActivity.this,entry.url),
       PlaylistHistory.actualHeight(PlaylistActivity.this,entry.url),qualities.get(h.position))));
    int status=h.position<videoStatus.length?videoStatus[h.position]:PlaylistHistory.NEW;
     h.already.setVisibility(status==PlaylistHistory.NEW?View.GONE:View.VISIBLE);
     h.already.setText(UiStrings.display(PlaylistActivity.this,
       status==PlaylistHistory.DOWNLOADED?"Уже скачано":"Уже добавлено в очередь"));
     h.already.setTextColor(status==PlaylistHistory.DOWNLOADED?0xFF4EC67B:0xFFE5AD38);
    return old;
   }
  };
  list.setAdapter(adapter);
  list.setOnScrollListener(new android.widget.AbsListView.OnScrollListener(){
   @Override public void onScrollStateChanged(android.widget.AbsListView v,int state){}
   @Override public void onScroll(android.widget.AbsListView v,int first,int visible,int total){
    if(visible>0)getPreferences(MODE_PRIVATE).edit().putInt("scroll_"+source.hashCode(),first).apply();
   }
  });
  list.post(()->list.setSelection(Math.max(0,getPreferences(MODE_PRIVATE)
   .getInt("scroll_"+source.hashCode(),0))));

  LinearLayout bottom=column();root.addView(bottom,margin(7,0));
  addButton=new Button(this);addButton.setText(UiStrings.display(PlaylistActivity.this, "Добавить в очередь"));
  ThemeStyle.styleButton(addButton,true);
  ThemeStyle.icon(addButton,"plus",18);
  bottom.addView(addButton,new LinearLayout.LayoutParams(-1,dp(49)));
  addButton.setOnClickListener(v->addSelected());
 }
 private static final class Row{
  LinearLayout root;CheckBox check;ImageView image;TextView name,duration,quality,already;
  String imageId="";int position;
 }
 private Spinner spinner(String[] labels){
  Spinner s=new Spinner(this,Spinner.MODE_DROPDOWN);
  ArrayAdapter<String> a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,UiStrings.translateChoices(this,labels)){
   @Override public View getView(int p,View old,android.view.ViewGroup parent){
    TextView t=(TextView)super.getView(p,old,parent);
    t.setTextColor(ThemeStyle.text(PlaylistActivity.this));t.setTextSize(12);return t;
   }
   @Override public View getDropDownView(int p,View old,android.view.ViewGroup parent){
    TextView t=(TextView)super.getDropDownView(p,old,parent);
    t.setTextColor(ThemeStyle.text(PlaylistActivity.this));t.setTextSize(13);
    t.setPadding(dp(12),dp(9),dp(12),dp(9));
    t.setBackground(ThemeStyle.popupRowBackground(PlaylistActivity.this,
      p==s.getSelectedItemPosition(),p,getCount()));
    return t;
   }
  };
  a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
  s.setAdapter(a);s.setPopupBackgroundDrawable(ThemeStyle.popupBackground(this));
  return s;
 }
 private void toggle(int position,Row row){
  if(position<0||position>=selected.length||animatingSelection)return;
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
  boolean all=!visibleRows.isEmpty();
  for(int i:visibleRows)if(!selected[i]){all=false;break;}
  selectAll.setChecked(all);
 }
 private void addCompactSpinner(LinearLayout content,Spinner spinner){
  FrameLayout frame=new FrameLayout(this);
  frame.setBackground(ThemeStyle.rounded(ThemeStyle.field(this),ThemeStyle.buttonStroke(this),dp(9)));
  content.addView(frame,new LinearLayout.LayoutParams(-1,dp(44)));
  spinner.setBackgroundColor(Color.TRANSPARENT);
  spinner.setPadding(dp(12),0,dp(32),0);
  frame.addView(spinner,new FrameLayout.LayoutParams(-1,-1));
  ImageView arrow=new ImageView(this);
  arrow.setImageDrawable(new UiIcons("chevron",ThemeStyle.text(this),dp(13)));
  arrow.setPadding(dp(9),0,dp(9),0);
  FrameLayout.LayoutParams location=new FrameLayout.LayoutParams(dp(32),-1,Gravity.END);
  frame.addView(arrow,location);
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
  LinearLayout content=column();
  content.setPadding(dp(17),dp(6),dp(17),dp(9));
  scroll.addView(content);
  TextView filterHeading=txt("Фильтр отображения загрузок",15);
  filterHeading.setTypeface(null,Typeface.BOLD);content.addView(filterHeading,margin(7,8));
  Spinner choose=settingsChoice(content,"Отобразить отмеченные/неотмеченные загрузки",
    new String[]{"Все","Только отмеченные","Только неотмеченные"},selectionFilter);
  Spinner downloads=settingsChoice(content,"Отобразить загрузки со статусом",
    new String[]{"Все","Скачанные","Нескачанные"},downloadFilter);
  Spinner queued=settingsChoice(content,"Отобразить загрузки находящиеся/отсутствующие в очереди",
    new String[]{"Все","Уже добавленные","Ещё не добавленные"},queueFilter);
  View sectionBreak=new View(this);sectionBreak.setBackgroundColor(ThemeStyle.border(this));
  content.addView(sectionBreak,margin(13,12));
  TextView info=txt("Качество для отмеченных",13);
  info.setTypeface(null,Typeface.BOLD);
  content.addView(info,margin(7,4));
  Spinner bulk=spinner(PlaylistQualityPolicy.LABELS);
  bulk.setSelection(PlaylistQualityPolicy.index(quality));
  addCompactSpinner(content,bulk);
  TextView applyQuality=txt("Применить качество к отмеченным",13);
  applyQuality.setTextColor(ThemeStyle.isDark(this)?0xFFE77A80:ThemeStyle.RED);
  applyQuality.setGravity(Gravity.CENTER);
  content.addView(applyQuality,margin(4,8));
  applyQuality.setOnClickListener(v->{
   int index=bulk.getSelectedItemPosition();
   if(index<0||index>=PlaylistQualityPolicy.VALUES.length)return;
   qualities=PlaylistQualityPolicy.applyToSelected(qualities,selected,PlaylistQualityPolicy.VALUES[index]);
   quality=PlaylistQualityPolicy.VALUES[index];
   persistQualities();adapter.notifyDataSetChanged();
   Toast.makeText(this,UiStrings.display(this,"Качество отмеченных обновлено"),Toast.LENGTH_SHORT).show();
  });
  CheckBox sort=new CheckBox(this);
  UiCheckboxStyle.apply(sort);
  sort.setText(UiStrings.display(this,"Сначала отмеченные"));
  sort.setTextColor(ThemeStyle.text(this));sort.setTextSize(13);
  sort.setChecked(selectedFirst);
  content.addView(sort,margin(12,4));
  ThemeStyle.dialog(this)
    .setTitle(UiStrings.display(this,"Настройки списка видео"))
    .setView(scroll)
    .setNegativeButton(UiStrings.display(this,"Отмена"),null)
    .setPositiveButton(UiStrings.display(this,"Применить"),(d,w)->{
      selectionFilter=choose.getSelectedItemPosition();
      downloadFilter=downloads.getSelectedItemPosition();
      queueFilter=queued.getSelectedItemPosition();
      selectedFirst=sort.isChecked();
      getSharedPreferences("playlist_display",0).edit()
       .putInt("selection",selectionFilter)
       .putInt("download",downloadFilter)
       .putInt("queue",queueFilter)
       .putBoolean("selectedFirst",selectedFirst).apply();
      rebuildVisible(true);list.setSelection(0);
    }).show();
 }
 private void chooseQuality(int position){
  if(position<0||position>=qualities.size())return;
  TextView anchor=null;
  for(int i=0;i<list.getChildCount();i++){
   int visible=list.getFirstVisiblePosition()+i;
   if(visible<visibleRows.size()&&visibleRows.get(visible)==position){
    anchor=((Row)list.getChildAt(i).getTag()).quality;break;
   }
  }
  if(anchor==null)return;
  final TextView chosenView=anchor;
  LinearLayout options=column();
  String[] labels=UiStrings.translateChoices(this,PlaylistQualityPolicy.LABELS);
  int current=PlaylistQualityPolicy.index(qualities.get(position));
  android.widget.PopupWindow popup=new android.widget.PopupWindow(this);
  popup.setWidth(dp(185));popup.setHeight(dp(Math.min(340,labels.length*39+4)));
  popup.setBackgroundDrawable(ThemeStyle.popupBackground(this));
  popup.setFocusable(true);popup.setOutsideTouchable(true);popup.setElevation(dp(6));
  for(int n=0;n<labels.length;n++){
   final int index=n;
   LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);
   line.setPadding(dp(11),0,dp(12),0);
   line.setBackground(ThemeStyle.popupRowBackground(this,n==current,n,labels.length));
   TextView label=txt(labels[n],13);line.addView(label,new LinearLayout.LayoutParams(0,dp(39),1));
   if(n==current){
    ImageView check=new ImageView(this);
    check.setImageDrawable(new UiIcons("check",ThemeStyle.text(this),dp(15)));
    line.addView(check,new LinearLayout.LayoutParams(dp(17),dp(17)));
   }
   options.addView(line,new LinearLayout.LayoutParams(-1,dp(39)));
   line.setOnClickListener(v->{
    qualities.set(position,PlaylistQualityPolicy.VALUES[index]);
    persistQualities();chosenView.setText(labels[index]);popup.dismiss();
   });
  }
  ScrollView scroll=new ScrollView(this);scroll.addView(options);popup.setContentView(scroll);
  popup.showAsDropDown(anchor,0,0,Gravity.START);
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
    "mp3".equals(format)||"m4a".equals(format),qualities,proxy,folder,priority,format);
   Arrays.fill(selected,false);persistSelection();
   refreshStatuses();rebuildVisible(false);DownloadService.kick(this);
   QueueNotice.show(this,QueueNotice.message(count,priority));
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
  if(progressPanel!=null){handler.removeCallbacks(tick);tick.run();}
 }
 @Override protected void onPause(){handler.removeCallbacks(tick);super.onPause();}
 @Override protected void onDestroy(){handler.removeCallbacks(tick);super.onDestroy();}

}
