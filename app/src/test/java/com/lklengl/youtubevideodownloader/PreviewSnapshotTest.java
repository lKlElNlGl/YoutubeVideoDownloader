package com.lklengl.youtubevideodownloader;

import org.junit.Test;
import static org.junit.Assert.*;

public class PreviewSnapshotTest {
    @Test public void videoPreviewIsAssociatedWithItsOriginalUrl(){
        PreviewSnapshot data=PreviewSnapshot.video("https://youtu.be/abc123","Название видео","10:30","1080p, 720p");
        assertTrue(data.matches("https://youtu.be/abc123"));
        assertFalse(data.matches("https://youtu.be/other"));
        assertEquals("Название видео",data.title);
        assertEquals("10:30",data.duration);
        assertEquals("1080p, 720p",data.resolutions);
        assertFalse(data.playlist);
    }
    @Test public void playlistPreviewRetainsSummary(){
        PreviewSnapshot data=PreviewSnapshot.playlist("https://www.youtube.com/playlist?list=PL123","Избранное",12);
        assertTrue(data.playlist);
        assertEquals(12,data.videoCount);
        assertEquals("Избранное",data.title);
    }
    @Test public void emptyUrlNeverMatchesSavedPreview(){
        PreviewSnapshot data=PreviewSnapshot.video("https://youtu.be/abc123","Title","2:20","720p");
        assertFalse(data.matches(null));
        assertFalse(data.matches(""));
    }
}
