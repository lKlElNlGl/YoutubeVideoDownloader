package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;
public class FolderRulesTest {
    @Test public void returnToDefaultOnlyAppearsForCustomLocation(){
        assertFalse(FolderRules.isCustom(null));
        assertFalse(FolderRules.isCustom(""));
        assertTrue(FolderRules.isCustom("content://com.android.externalstorage.documents/tree/primary%3AMovies"));
    }
}
