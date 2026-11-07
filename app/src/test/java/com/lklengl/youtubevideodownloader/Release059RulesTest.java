package com.lklengl.youtubevideodownloader;

import org.junit.Test;
import static org.junit.Assert.*;

/** Regression specifications for the v0.5.9 playlist interface. */
public class Release059RulesTest {
 @Test public void playlistInformationScrollsNormallyBeforeControlsPin(){
  assertEquals(90,Release059Rules.stickyControlsY(0,90));
  assertEquals(20,Release059Rules.stickyControlsY(-70,90));
  assertEquals(0,Release059Rules.stickyControlsY(-90,90));
  assertEquals(0,Release059Rules.stickyControlsY(-180,90));
 }
 @Test public void activeRowIsSlightlyBrighterInDarkModeAndMuchClearerInLightMode(){
  assertEquals(0xFF2D3542,Release059Rules.highlightColor(0xFF272E39,0xFF3B4457,true));
  assertEquals(0xFFF0F1F3,Release059Rules.highlightColor(0xFFFFFFFF,0xFFE6E7EB,false));
 }
 @Test public void compactQualityMovesTextRightWithoutGrowingTheField(){
  assertEquals(7,Release059Rules.qualityLeftInsetDp(28,2,16));
  assertEquals(3,Release059Rules.qualityRightInsetDp(28,2,16));
 }
}
