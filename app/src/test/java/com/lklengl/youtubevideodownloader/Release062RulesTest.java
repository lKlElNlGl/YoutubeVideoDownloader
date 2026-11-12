package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class Release062RulesTest {
 @Test public void videoThenAudioSortedDescendingWithOnlyOneDivider(){
  List<String> c=Release062Rules.individualChoices(null,"mp4","720");
  assertEquals(Arrays.asList("mp4|best","mp4|2160","mp4|1440","mp4|1080",
    "mp4|720","mp4|480","mp4|360","mp3|a:best","mp3|a:320",
    "mp3|a:256","mp3|a:192","mp3|a:128"),c);
  assertEquals(7,Release062Rules.dividerIndex(c));
 }
 @Test public void audioOnlyOrVideoOnlyNeverHasDivider(){
  List<String> audio=Release062Rules.individualChoices(
    Arrays.asList("audio-only"),"mp3","a:best");
  assertEquals(Arrays.asList("mp3|a:best","mp3|a:320","mp3|a:256",
    "mp3|a:192","mp3|a:128"),audio);
  assertEquals(-1,Release062Rules.dividerIndex(audio));
  List<String> video=Release062Rules.individualChoices(
    Arrays.asList("best","720","480","video-only"),"mp4","720");
  assertEquals(Arrays.asList("mp4|best","mp4|720","mp4|480"),video);
  assertEquals(-1,Release062Rules.dividerIndex(video));
 }
 @Test public void dividerIsNotSelectableAndIndicesMapCorrectly(){
  assertEquals(-1,Release062Rules.popupIndexToChoice(7,7));
  assertEquals(7,Release062Rules.popupIndexToChoice(8,7));
  assertEquals(8,Release062Rules.choiceIndexToPopup(7,7));
  assertEquals(3,Release062Rules.choiceIndexToPopup(3,7));
  assertEquals(2,Release062Rules.popupIndexToChoice(2,-1));
 }
 @Test public void verifiedOptionsDoNotDiscardManuallyChosenLimit(){
  List<String> c=Release062Rules.individualChoices(
    Arrays.asList("best","720","480"),"mp4","1080");
  assertEquals(Arrays.asList("mp4|best","mp4|1080","mp4|720","mp4|480",
    "mp3|a:best","mp3|a:320","mp3|a:256","mp3|a:192","mp3|a:128"),c);
  assertEquals(4,Release062Rules.dividerIndex(c));
 }
 @Test public void expandedRowsShowFullUnitsInBothLanguages(){
  assertEquals("Видео MP4 · Максимальное",Release062Rules.fullChoice("mp4|best",false));
  assertEquals("Video MP4 · Maximum",Release062Rules.fullChoice("mp4|best",true));
  assertEquals("Аудио MP3 · 320 кбит/с",Release062Rules.fullChoice("mp3|a:320",false));
  assertEquals("Audio MP3 · 192 kbps",Release062Rules.fullChoice("mp3|a:192",true));
 }
 @Test public void stretchOverscrollMovesListCardDownWithTheHeader(){
  assertEquals(146,Release062Rules.stretchedCardTop(0,0,126,-20));
  assertEquals(126,Release062Rules.stretchedCardTop(0,0,126,0));
  assertEquals(66,Release062Rules.stretchedCardTop(0,-60,126,0));
  assertEquals(0,Release062Rules.stretchedCardTop(1,-60,126,0));
 }
 @Test public void qualityProgressUsesThreeFixedLinesAndPriorityButtonShowsCount(){
  assertEquals("Определено\nкачеств:\n35 из 326",Release062Rules.qualityProgress(35,326,false));
  assertEquals("Checked\nqualities:\n35 of 326",Release062Rules.qualityProgress(35,326,true));
  assertEquals("Скачать без очереди (4)",Release062Rules.priorityLabel(4,false));
  assertEquals("Download immediately (4)",Release062Rules.priorityLabel(4,true));
 }
}
