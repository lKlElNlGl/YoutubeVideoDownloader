package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;
public class ProgressPolicyTest {
 @Test public void aNewAudioStreamCannotMoveTheTaskBarBackwards(){
  assertEquals(63,ProgressPolicy.displayPercent(63,57));
  assertEquals(64,ProgressPolicy.displayPercent(63,64));
 }
 @Test public void onlyCompletedTaskDisplaysOneHundred(){
  assertEquals(99,ProgressPolicy.displayPercent(99,100));
  assertEquals(99,ProgressPolicy.displayPercent(60,100));
  assertEquals(100,ProgressPolicy.completed());
 }
}