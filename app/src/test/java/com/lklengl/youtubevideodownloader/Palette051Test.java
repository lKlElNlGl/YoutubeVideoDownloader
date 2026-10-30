package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;
public class Palette051Test {
 @Test public void approvedRedButtonAlwaysRemainsIdenticalTo050(){
  assertEquals(0xFFFC2A3F,Palette050.PRIMARY_TOP_LEFT);
  assertEquals(0xFFFE1F2C,Palette050.PRIMARY_BOTTOM_RIGHT);
  assertEquals(0xFFA92A21,Palette050.PRIMARY_BORDER_DARK);
  assertEquals(0xFFFF9CA4,Palette050.PRIMARY_BORDER_LIGHT);
  assertEquals(Palette050.PRIMARY_TOP_LEFT,PremiumPalette.GRADIENT_TOP_LEFT);
  assertEquals(Palette050.PRIMARY_BOTTOM_RIGHT,PremiumPalette.GRADIENT_BOTTOM_RIGHT);
 }
 @Test public void newCardIsDarkerButOldPaletteRemainsAccessible(){
  assertEquals(0xFF272E39,Palette050.NIGHT_CARD);
  assertEquals(0xFF1F2632,Palette051.CARD);
  assertEquals(0xFF303A4A,Palette051.BUTTON);
 }
 @Test public void surfaceBackgroundAndReadableTextRemain050(){
  assertEquals(0xFF0D1119,Palette050.NIGHT_BACKGROUND);
  assertEquals(0xFFF4F5F8,Palette050.NIGHT_TEXT);
  assertEquals(0xFFF5F6F9,Palette050.DAY_BACKGROUND);
 }
}
