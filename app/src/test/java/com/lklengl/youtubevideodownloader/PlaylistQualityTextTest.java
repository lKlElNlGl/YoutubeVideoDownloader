package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaylistQualityTextTest {
 @Test public void completedFileDisplaysActualResolution(){
  assertEquals("1080p",PlaylistQualityText.label(PlaylistHistory.DOWNLOADED,"best",1080,"720"));
 }
 @Test public void queuedVideoDisplaysQualityChosenAtEnqueueTime(){
  assertEquals("720p",PlaylistQualityText.label(PlaylistHistory.QUEUED,"720",0,"best"));
 }
 @Test public void manualChoiceOverridesExistingDownloadLabel(){
  assertEquals("720p",PlaylistQualityText.label(PlaylistHistory.DOWNLOADED,"best",1080,"720",true));
  assertEquals("Максимальное",PlaylistQualityText.label(PlaylistHistory.DOWNLOADED,"best",1080,"best",true));
 }
 @Test public void missingRecordedHeightDoesNotInventResolution(){
  assertEquals("Качество неизвестно",PlaylistQualityText.label(PlaylistHistory.DOWNLOADED,"best",0,"best"));
 }
}