package com.lklengl.youtubevideodownloader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.text.TextUtils;
import android.graphics.drawable.Drawable;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Full-screen, virtualized playlist selector, with pinned footer and persistent selections. */
public final class PlaylistActivity extends Activity {
 public static final String URL="playlist_url", QUALITY="quality", FORMAT="output_format",
  PROXY="proxy", FOLDER="folder", PRIORITY="priority";
 private String source,quality,format,proxy,folder;
 private boolean priority,loading=false;
 private PlaylistRepository.Playlist playlist;
 private boolean[] selected;
 private ListView list;
 private BaseAdapter adapter;
 private TextView counter,selectAll,heading,subheading;
 private Button addButton,allButton,refreshButton;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private int dp(float n){return ThemeStyle.dp(this,n);}
 private TextView txt(String value,int size){
  TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(ThemeStyle.text(this));
  return t;
 }
 private LinearLayout column(){
  LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;
 }
 private Button button(String label,boolean red){
  Button b=new Button(this);b.setText(label);ThemeStyle.styleButton(b,red);return b;
 }
 @Override public void onCreate(Bundle state){
  ThemeStyle.initialize(this);super.onCreate(state);
  source=getIntent().getStringExtra(URL);
  quality=getIntent().getStringExtra(QUALITY);
  format=getIntent().getStringExtra(FORMAT);
  proxy=getIntent().getStringExtra(PROXY);
  folder=getIntent().getStringExtra(FOLDER);
  priority=getIntent().getBooleanExtra(PRIORITY,false);
  if(source==null||!PlaylistSupport.isPlaylist(source)){finish();return;}
  playlist=PlaylistCache.load(this,source);
  if(playlist==null){
   Toast.makeText(this,"Список плейлиста не сохранён. Получите информацию повторно.",Toast.LENGTH_LONG).show();
   finish();return;
  }
  createScreen();
  refreshData(false);
 }
 private Set<String> previouslyAdded(){
  Set<String> urls=new HashSet<>();
  for(DownloadQueue.Item j:DownloadQueue.all(this))urls.add(j.url);
  return urls;
 }
 private void refreshData(boolean fresh){
  if(fresh){playlist=PlaylistCache.load(this,source);if(playlist==null)return;}
  Set<String> added=previouslyAdded();
  List<String> urls=new ArrayList<>();
  for(PlaylistRepository.Entry e:playlist.entries)urls.add(e.url);
  List<Boolean> saved=PlaylistCache.readSelection(this,source);
  List<Boolean> flags=saved==null?PlaylistSelectionPolicy.initial(urls,added):
   PlaylistSelectionPolicy.resume(saved,urls,added);
  selected=new boolean[flags.size()];
  for(int i=0;i<flags.size();i++)selected[i]=flags.get(i);
  heading.setText(playlist.title);
  long duration=0;boolean known=true;
  for(PlaylistRepository.Entry e:playlist.entries){if(e.duration<0)known=false;else duration+=e.duration;}
  subheading.setText(playlist.entries.size()+" видео"+(known?" · "+QueueRules.readableDuration((int)Math.min(Integer.MAX_VALUE,duration)):""));
  adapter.notifyDataSetChanged();updateFooter();
 }
 private void createScreen(){
  LinearLayout root=column();root.setPadding(dp(12),dp(8),dp(12),dp(8));
  root.setBackgroundColor(ThemeStyle.background(this));setContentView(root);
  LinearLayout toolbar=new LinearLayout(this);toolbar.setGravity(Gravity.CENTER_VERTICAL);
  root.addView(toolbar,new LinearLayout.LayoutParams(-1,dp(47)));
  TextView back=txt("‹",32);back.setGravity(Gravity.CENTER);
  toolbar.addView(back,new LinearLayout.LayoutParams(dp(38),dp(46)));
  back.setContentDescription("Назад");back.setOnClickListener(v->finish());
  TextView title=txt("Плейлист",18);title.setTypeface(null,Typeface.BOLD);
  toolbar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
  TextView update=txt("⟳",27);update.setGravity(Gravity.CENTER);
  update.setContentDescription("Обновить список с YouTube");
  toolbar.addView(update,new LinearLayout.LayoutParams(dp(44),dp(44)));
  update.setOnClickListener(v->updatePlaylist());
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
  LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);
  hp.setMargins(0,dp(4),0,dp(9));root.addView(header,hp);
  ImageView cover=new ImageView(this);
  header.addView(cover,new LinearLayout.LayoutParams(dp(92),dp(74)));
  if(!playlist.entries.isEmpty()){
   DownloadQueue.Item first=new DownloadQueue.Item();
   first.id="playlist-"+playlist.entries.get(0).id;first.url=playlist.entries.get(0).url;
   QueueThumbnails.show(this,first,cover,dp(8));
  }
  LinearLayout details=column();details.setPadding(dp(10),0,0,0);
  header.addView(details,new LinearLayout.LayoutParams(0,-2,1));
  heading=txt("",15);heading.setTypeface(null,Typeface.BOLD);heading.setMaxLines(2);
  heading.setEllipsize(TextUtils.TruncateAt.END);details.addView(heading);
  subheading=txt("",12);subheading.setTextColor(ThemeStyle.subdued(this));details.addView(subheading);
  allButton=button("↓  Добавить весь плейлист",true);
  root.addView(allButton,new LinearLayout.LayoutParams(-1,dp(47)));
  allButton.setOnClickListener(v->{Arrays.fill(selected,true);adapter.notifyDataSetChanged();updateFooter();addSelected();});
  selectAll=txt("☑  Выбрать все",13);
  selectAll.setGravity(Gravity.CENTER_VERTICAL);
  selectAll.setPadding(dp(7),dp(7),dp(7),dp(7));
  root.addView(selectAll,new LinearLayout.LayoutParams(-1,dp(43)));
  selectAll.setOnClickListener(v->{
   boolean every=true;for(boolean b:selected)if(!b){every=false;break;}
   Arrays.fill(selected,!every);persistSelection();adapter.notifyDataSetChanged();updateFooter();
  });
  list=new ListView(this);
  list.setDividerHeight(dp(1));list.setDivider(new android.graphics.drawable.ColorDrawable(ThemeStyle.border(this)));
  list.setBackgroundColor(ThemeStyle.background(this));
  list.setCacheColorHint(Color.TRANSPARENT);
  list.setVerticalScrollBarEnabled(true);
  root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
  adapter=new BaseAdapter(){
   @Override public int getCount(){return playlist.entries.size();}
   @Override public Object getItem(int i){return playlist.entries.get(i);}
   @Override public long getItemId(int i){return i;}
   @Override public View getView(int pos,View convert,android.view.ViewGroup parent){
    LinearLayout row=new LinearLayout(PlaylistActivity.this);row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(dp(2),dp(5),dp(2),dp(5));row.setMinimumHeight(dp(66));
    CheckBox check=new CheckBox(PlaylistActivity.this);
    check.setButtonTintList(android.content.res.ColorStateList.valueOf(selected[pos]?ThemeStyle.RED:ThemeStyle.subdued(PlaylistActivity.this)));
    check.setChecked(selected[pos]);row.addView(check,new LinearLayout.LayoutParams(dp(39),dp(44)));
    ImageView image=new ImageView(PlaylistActivity.this);
    row.addView(image,new LinearLayout.LayoutParams(dp(57),dp(48)));
    PlaylistRepository.Entry entry=playlist.entries.get(pos);
    DownloadQueue.Item item=new DownloadQueue.Item();item.id="playlist-"+entry.id;item.url=entry.url;
    QueueThumbnails.show(PlaylistActivity.this,item,image,dp(7));
    LinearLayout copy=column();copy.setPadding(dp(8),0,0,0);
    row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
    TextView name=txt(entry.title,13);name.setSingleLine(true);name.setEllipsize(TextUtils.TruncateAt.END);
    copy.addView(name);
    TextView duration=txt(entry.duration>=0?QueueRules.readableDuration(entry.duration):"Длительность неизвестна",11);
    duration.setTextColor(ThemeStyle.subdued(PlaylistActivity.this));copy.addView(duration);
    if(previouslyAddedCache.contains(entry.url)){
     TextView already=txt("Уже добавлено в очередь",10);
     already.setTextColor(0xFF4EC67B);copy.addView(already);
    }
    row.setOnClickListener(v->toggle(pos));
    check.setOnClickListener(v->toggle(pos));
    return row;
   }
  };
  list.setAdapter(adapter);
  LinearLayout bottom=column();
  bottom.setPadding(dp(3),dp(9),dp(3),dp(4));
  bottom.setBackground(ThemeStyle.rounded(ThemeStyle.cardColor(this),ThemeStyle.border(this),dp(12)));
  LinearLayout.LayoutParams bottomParams=new LinearLayout.LayoutParams(-1,-2);
  bottomParams.setMargins(0,dp(6),0,0);root.addView(bottom,bottomParams);
  counter=txt("",12);counter.setPadding(dp(8),0,0,dp(3));bottom.addView(counter);
  addButton=button("↓  Добавить в очередь",true);
  bottom.addView(addButton,new LinearLayout.LayoutParams(-1,dp(49)));
  addButton.setOnClickListener(v->addSelected());
  list.setOnScrollListener(new android.widget.AbsListView.OnScrollListener(){
   @Override public void onScrollStateChanged(android.widget.AbsListView v,int state){}
   @Override public void onScroll(android.widget.AbsListView v,int first,int visible,int total){
    if(visible>0)getPreferences(MODE_PRIVATE).edit().putInt("scroll_"+source.hashCode(),first).apply();
   }
  });
  list.post(()->list.setSelection(Math.max(0,getPreferences(MODE_PRIVATE).getInt("scroll_"+source.hashCode(),0))));
 }
 private Set<String> previouslyAddedCache=new HashSet<>();
 private void toggle(int position){
  selected[position]=!selected[position];persistSelection();adapter.notifyDataSetChanged();updateFooter();
 }
 private void persistSelection(){PlaylistCache.saveSelection(this,source,selected);}
 private void updateFooter(){
  previouslyAddedCache=previouslyAdded();
  int chosen=0;for(boolean yes:selected)if(yes)chosen++;
  counter.setText("Выбрано: "+chosen+" из "+selected.length);
  addButton.setText("↓  Добавить в очередь ("+chosen+")");
  addButton.setEnabled(chosen>0);
  selectAll.setText(chosen==selected.length?"☑  Снять выбор":"☐  Выбрать все ("+selected.length+")");
 }
 private void addSelected(){
  int count=0,duplicates=0;
  Set<String> added=previouslyAdded();
  for(int i=0;i<selected.length;i++)if(selected[i]){
   count++;if(added.contains(playlist.entries.get(i).url))duplicates++;
  }
  if(count==0){Toast.makeText(this,"Выберите видео",Toast.LENGTH_SHORT).show();return;}
  if(duplicates>0){
   int existing=duplicates;
   ThemeStyle.dialog(this).setTitle("Повторные видео")
    .setMessage("Уже добавлено: "+existing+". Добавить их повторно?")
    .setNegativeButton("Отмена",null)
    .setPositiveButton("Добавить",(d,w)->reallyAdd()).show();
  }else reallyAdd();
 }
 private void reallyAdd(){
  try{
   List<Boolean> chosen=new ArrayList<>();
   for(boolean b:selected)chosen.add(b);
   int added=DownloadQueue.enqueuePlaylist(this,playlist,chosen,
    "mp3".equals(format)||"m4a".equals(format),quality,proxy,folder,priority,format);
   for(int i=0;i<selected.length;i++)if(selected[i])selected[i]=false;
   persistSelection();updateFooter();adapter.notifyDataSetChanged();
   DownloadService.kick(this);
   Toast.makeText(this,"Добавлено в очередь: "+added,Toast.LENGTH_LONG).show();
  }catch(Exception ex){ThemeStyle.dialog(this).setMessage(ex.getMessage()).setPositiveButton("ОК",null).show();}
 }
 private void updatePlaylist(){
  if(loading)return;
  loading=true;
  ThemeStyle.dialog(this).setTitle("Обновление плейлиста")
   .setMessage("Запрашиваем список с YouTube. Сохранённый список останется доступным.")
   .setPositiveButton("ОК",null).show();
  worker.execute(()->{
   try{
    PlaylistRepository.Playlist updated=PlaylistRepository.fetch(getApplicationContext(),source,proxy);
    PlaylistCache.save(getApplicationContext(),source,updated);
    runOnUiThread(()->{
     loading=false;refreshData(true);
     Toast.makeText(this,"Список обновлён",Toast.LENGTH_SHORT).show();
    });
   }catch(Exception ex){
    runOnUiThread(()->{loading=false;ThemeStyle.dialog(this).setTitle("Не удалось обновить")
      .setMessage(ex.getMessage()).setPositiveButton("ОК",null).show();});
   }
  });
 }
 @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
