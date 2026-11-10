package com.lklengl.youtubevideodownloader;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;

public class PlaylistMediaPolicyTest {
 @Test public void compactLabelsStayReadableAndLocalized(){
  assertEquals("MP4 · Макс.",PlaylistMediaPolicy.compact("mp4","best",false));
  assertEquals("MP4 · Max",PlaylistMediaPolicy.compact("mp4","best",true));
  assertEquals("MP4 · 1080p",PlaylistMediaPolicy.compact("mp4","1080",false));
  assertEquals("MP3 · 192k",PlaylistMediaPolicy.compact("mp3","a:192",true));
  assertEquals("MP3 · Макс.",PlaylistMediaPolicy.compact("mp3","a:best",false));
 }
 @Test public void audioQualityIsNotMistakenForVideoHeight(){
  assertTrue(PlaylistMediaPolicy.allowed("mp3","a:320"));
  assertTrue(PlaylistMediaPolicy.allowed("mp4","1080"));
  assertFalse(PlaylistMediaPolicy.allowed("mp3","1080"));
  assertFalse(PlaylistMediaPolicy.allowed("mp4","a:192"));
  assertEquals("a:best",PlaylistMediaPolicy.defaultQuality("mp3"));
  assertEquals("best",PlaylistMediaPolicy.defaultQuality("mp4"));
 }
 @Test public void realVideoOptionsNeverAutoUpscale(){
  assertEquals("720",PlaylistMediaPolicy.effectiveVideo("1080",Arrays.asList("best","720","480")));
  assertEquals("480",PlaylistMediaPolicy.effectiveVideo("480",Arrays.asList("best","720","480")));
  assertEquals("360",PlaylistMediaPolicy.effectiveVideo("360",Arrays.asList("best","720","480")));
  assertEquals("best",PlaylistMediaPolicy.effectiveVideo("best",Arrays.asList("best","720")));
 }
 @Test public void audioTranscodeCapRespectsSourceAndRequest(){
  assertEquals(192,PlaylistMediaPolicy.mp3Kbps("a:320",128,"opus"));
  assertEquals(128,PlaylistMediaPolicy.mp3Kbps("a:128",192,"aac"));
  assertEquals(192,PlaylistMediaPolicy.mp3Kbps("a:best",0,""));
  assertEquals(256,PlaylistMediaPolicy.mp3Kbps("a:320",192,"opus"));
 }
}
