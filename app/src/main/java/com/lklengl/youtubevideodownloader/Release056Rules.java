package com.lklengl.youtubevideodownloader;
/** Pixel-exact visibility thresholds and display-only labels for v0.5.6. */
public final class Release056Rules {
 private Release056Rules(){}
 public static String folderDisplay(String documentId) {
  if(documentId==null)return "";
  return documentId.startsWith("primary:")?documentId.substring("primary:".length()):documentId;
 }
 public static boolean showUp(boolean canScrollUp,int firstRowTop,int tabsBottom) {
  return canScrollUp&&firstRowTop<=tabsBottom;
 }
 public static boolean showDown(boolean canScrollDown,int cleanButtonBottom,int viewportBottom) {
  return canScrollDown&&cleanButtonBottom>=viewportBottom;
 }
 public static int qualityWidth(int textPixels,int adornmentsPixels,int availablePixels) {
  return Math.min(availablePixels,Math.max(0,textPixels+adornmentsPixels));
 }
}
