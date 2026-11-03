package com.lklengl.youtubevideodownloader;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaylistFolderRulesTest {
 @Test public void sanitizesInvalidCharactersWithoutChangingCyrillic(){
  assertEquals("Моя музыка 2026",PlaylistFolderRules.safeName("  Моя:музыка/2026?  "));
 }
 @Test public void avoidsEmptyAndDotOnlyDirectoryNames(){
  assertEquals("Плейлист",PlaylistFolderRules.safeName(" ..\\/:*? "));
 }
 @Test public void preservesLegiblePlaylistName(){
  assertEquals("Любимые клипы",PlaylistFolderRules.safeName("Любимые клипы"));
 }
 @Test public void resolvesNameCollisions(){
  Set<String> existing=new HashSet<>(Arrays.asList("Любимые клипы","Любимые клипы (2)"));
  assertEquals("Любимые клипы (3)",PlaylistFolderRules.uniqueName("Любимые клипы",existing));
 }
}
