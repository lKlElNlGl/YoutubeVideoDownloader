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
 private String source,quality,format,proxy,folder;
 private volatile String activeRefreshId;
 private boolean priority,loading=false,ignoreBulk=true;
 private long refreshStart;
 private PlaylistRepository.Playlist playlist;
 private boolean[] selected;
 private List<String> qualities;
 private ListView list;
 private BaseAdapter adapter;
 private TextView heading,subheading,owner,progressText,refreshControl,sectionCount;
 private CheckBox selectAll;
 private ImageView filterIcon;
 private final List<Integer> visibleRows=new ArrayList<>();
 private int[] videoStatus=new int[0];
 private int selectionFilter=0,downloadFilter=0,queueFilter=0;
 private boolean selectedFirst=false,animatingSelection=false;
 private LinearLayout progressPanel;
 private Button addButton;
 
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final String[] waitingMessages={
  "Запрашиваем список видео с YouTube…",
  "Ожидаем ответа YouTube. Сохранённый список доступен.",
  "Обрабатывается информация о плейлисте…",
  "Большой плейлист может загружаться дольше обычного.",
  "Продолжаем получать сведения о видео…"
 };
 private final Runnable tick=new Runnable(){
  @Override public void run(){
   if(activeRefreshId==null)return;
   long seconds=(SystemClock.elapsedRealtime()-refreshStart)/1000L;
   int index=seconds<12?0:1+(int)((seconds-12)/8%(waitingMessages.length-1));
   progressText.setText(UiStrings.display(PlaylistActivity.this, "Обновление плейлиста · "+String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60)
    +"\n"+waitingMessages[index]));
   handler.postDelayed(this,1000);
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
  filterIcon.setImageDrawable(new UiIcons("sliders",active?ThemeStyle.RED:ThemeStyle.text(this),dp(21)));
  filterIcon.setBackground(ThemeStyle.rounded(ThemeStyle.buttonFill(this),
    active?ThemeStyle.RED:ThemeStyle.buttonStroke(this),dp(9)));
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
  List<Boolean> saved=PlaylistCache.readSelection(this,source);
  List<Boolean> flags=saved==null?PlaylistSelectionPolicy.initial(urls,already):
   PlaylistSelectionPolicy.resume(saved,urls,already);
  selected=new boolean[flags.size()];
  for(int i=0;i<flags.size();i++)selected[i]=flags.get(i);
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
  selectAll.setButtonTintList(checkTint());
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
  selectRow.addView(filterIcon,new LinearLayout.LayoutParams(dp(42),dp(38)));
  filterIcon.setOnClickListener(v->showListSettings());
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
      h.check.setButtonTintList(checkTint());
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
     h.quality.setPadding(dp(7),dp(3),dp(5),dp(3));
      h.quality.setMinHeight(dp(27));
      h.quality.setCompoundDrawablesWithIntrinsicBounds(null,null,
        new UiIcons("chevron",ThemeStyle.subdued(PlaylistActivity.this),dp(12)),null);
      h.quality.setCompoundDrawablePadding(dp(4));
      h.quality.setBackground(ThemeStyle.rounded(ThemeStyle.buttonFill(PlaylistActivity.this),
        ThemeStyle.buttonStroke(PlaylistActivity.this),dp(6)));
      LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(-2,dp(29));
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
    h.quality.setText(UiStrings.display(PlaylistActivity.this, PlaylistQualityPolicy.LABELS[PlaylistQualityPolicy.index(qualities.get(h.position))]));
    int status=h.position<videoStatus.length?videoStatus[h.position]:PlaylistHistory.NEW;
     h.already.setVisibility(status==PlaylistHistory.NEW?View.GONE:View.VISIBLE);
     h.already.setText(UiStrings.display(PlaylistActivity.this,
       status==PlaylistHistory.DOWNLOADED?"Уже скачано":"Уже добавлено"));
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
 private Set<String> previouslyAddedCache=new HashSet<>();
 private void toggle(int position,Row row){
  if(position<0||position>=selected.length)return;
  selected[position]=!selected[position];persistSelection();
  row.check.setChecked(selected[position]);
  updateFooter();
 }
 private void persistSelection(){PlaylistCache.saveSelection(this,source,selected);}
 private void persistQualities(){PlaylistCache.saveQualities(this,source,playlist,qualities);}
 private void updateFooter(){
  previouslyAddedCache=previouslyAdded();
  int count=0;for(boolean flag:selected)if(flag)count++;
  counter.setText(UiStrings.display(PlaylistActivity.this, "Выбрано: "+count+" из "+selected.length));
  addButton.setText(UiStrings.display(this,"Добавить в очередь")+" ("+count+")");
  addButton.setEnabled(count>0);
  selectAll.setText(UiStrings.display(PlaylistActivity.this, count==selected.length?"☑  Снять выбор":"☐  Выбрать все ("+selected.length+")"));
 }
 private void chooseQuality(int position){
  if(position<0||position>=qualities.size())return;
  AlertDialog dialog=ThemeStyle.dialog(this).setTitle(UiStrings.display(PlaylistActivity.this, "Качество для этого видео"))
   .setSingleChoiceItems(UiStrings.translateChoices(this,PlaylistQualityPolicy.LABELS),
    PlaylistQualityPolicy.index(qualities.get(position)),(d,which)->{
     qualities.set(position,PlaylistQualityPolicy.VALUES[which]);
     persistQualities();
     for(int i=0;i<list.getChildCount();i++){
      int p=list.getFirstVisiblePosition()+i;
      if(p==position){
       Row h=(Row)list.getChildAt(i).getTag();
       h.quality.setText(UiStrings.display(PlaylistActivity.this, PlaylistQualityPolicy.LABELS[which]+" ⌄"));break;
      }
     }
     d.dismiss();
   }).setNegativeButton(UiStrings.display(PlaylistActivity.this, "Отмена"),null).create();
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
    "mp3".equals(format)||"m4a".equals(format),qualities,proxy,folder,priority,format);
   Arrays.fill(selected,false);persistSelection();updateFooter();
   adapter.notifyDataSetChanged();DownloadService.kick(this);
   Toast.makeText(this,UiStrings.display(PlaylistActivity.this, "Добавлено в очередь: "+count),Toast.LENGTH_LONG).show();
  }catch(Exception ex){
   ThemeStyle.dialog(this).setMessage(UiStrings.display(PlaylistActivity.this, ex.getMessage())).setPositiveButton(UiStrings.display(PlaylistActivity.this, "ОК"),null).show();
  }
 }
 private void updatePlaylist(){
  if(loading)return;
  loading=true;refreshStart=SystemClock.elapsedRealtime();
  String id=UUID.randomUUID().toString();activeRefreshId=id;
  progressPanel.setVisibility(View.VISIBLE);refreshControl.setEnabled(false);
  handler.removeCallbacks(tick);tick.run();
  worker.execute(()->{
   try{
    PlaylistRepository.Playlist updated=PlaylistRepository.fetch(getApplicationContext(),source,proxy,id);
    if(!id.equals(activeRefreshId))return;
    PlaylistCache.save(getApplicationContext(),source,updated);
    runOnUiThread(()->{
     if(!id.equals(activeRefreshId)||isFinishing()||isDestroyed())return;
     finishRefresh();refreshData(true);
     Toast.makeText(this,UiStrings.display(PlaylistActivity.this, "Плейлист обновлён"),Toast.LENGTH_SHORT).show();
    });
   }catch(Exception ex){
    runOnUiThread(()->{
     if(!id.equals(activeRefreshId)||isFinishing()||isDestroyed())return;
     finishRefresh();
     ThemeStyle.dialog(this).setTitle(UiStrings.display(PlaylistActivity.this, "Не удалось обновить плейлист"))
      .setMessage(UiStrings.display(PlaylistActivity.this, ex.getMessage())).setPositiveButton(UiStrings.display(PlaylistActivity.this, "ОК"),null).show();
    });
   }
  });
 }
 private void finishRefresh(){
  activeRefreshId=null;loading=false;handler.removeCallbacks(tick);
  progressPanel.setVisibility(View.GONE);refreshControl.setEnabled(true);
 }
 private void cancelRefresh(){
  String id=activeRefreshId;if(id==null)return;
  finishRefresh();
  try{com.yausername.youtubedl_android.YoutubeDL.getInstance().destroyProcessById(id);}
  catch(Exception ignored){}
 }
 @Override protected void onDestroy(){
  cancelRefresh();worker.shutdownNow();handler.removeCallbacks(tick);super.onDestroy();
 }
}
