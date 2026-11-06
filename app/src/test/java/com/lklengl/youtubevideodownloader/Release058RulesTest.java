package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class Release058RulesTest {
 @Test public void availabilityRemovesUnsupportedHeights(){
  assertEquals(Arrays.asList("best","1080","720"),Release058Rules.available(Arrays.asList(720,1080,720,240)));
 }
 @Test public void emptyMetadataUsesOldFallback(){
  assertEquals(Arrays.asList(PlaylistQualityPolicy.VALUES),Release058Rules.fallback());
 }
 @Test public void cachedMetadataAllowsSelectionDuringRefresh(){
  assertTrue(Release058Rules.canSelect(true,false,false));
  assertFalse(Release058Rules.canSelect(false,false,false));
  assertTrue(Release058Rules.canSelect(false,true,false));
  assertTrue(Release058Rules.canSelect(false,false,true));
 }
 @Test public void cancellationExpiresAfterFifteenSeconds(){
  assertTrue(Release058Rules.showCancel(1000,15999));
  assertFalse(Release058Rules.showCancel(1000,16000));
 }
 @Test public void resetIncludesSortCheckbox(){
  assertTrue(Release058Rules.hasActiveFilters(0,0,0,true));
  assertFalse(Release058Rules.hasActiveFilters(0,0,0,false));
 }
}