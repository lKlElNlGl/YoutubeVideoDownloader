package com.lklengl.youtubevideodownloader;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
/** Refreshes downloaded media metadata locally, without contacting the provider. */
public final class LocalMetadata {
 private LocalMetadata(){}
 public static boolean exists(Context context,String uri){
  if(uri==null||uri.isEmpty())return false;
  try(ParcelFileDescriptor fd=context.getContentResolver().openFileDescriptor(Uri.parse(uri),"r")){
   return fd!=null;
  }catch(Exception ignored){return false;}
 }
 private static int number(String text,int fallback){
  try{return text==null?fallback:Integer.parseInt(text);}catch(Exception ex){return fallback;}
 }
 public static boolean refresh(Context context,DownloadQueue.Item entry){
  if(!DownloadQueue.DONE.equals(entry.status)||!exists(context,entry.contentUri))return false;
  int duration=-1,height=0;
  MediaMetadataRetriever retriever=new MediaMetadataRetriever();
  try{
   retriever.setDataSource(context,Uri.parse(entry.contentUri));
   long millis=number(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION),-1);
   if(millis>=0)duration=(int)Math.min(Integer.MAX_VALUE,millis/1000);
   height=number(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT),0);
  }catch(Exception ignored){}finally{try{retriever.release();}catch(Exception ignored){}}
  DownloadQueue.metadata(context,entry.id,duration,height,null);
  if(height>0)PlaylistHistory.markDownloaded(context,entry.url,entry.contentUri,height);
  return true;
 }
}
