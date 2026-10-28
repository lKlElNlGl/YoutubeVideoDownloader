package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaylistSupportTest {
    @Test public void recognizesPlaylistLinks() {
        assertTrue(PlaylistSupport.isPlaylist("https://www.youtube.com/playlist?list=PLabc123"));
        assertTrue(PlaylistSupport.isPlaylist("https://www.youtube.com/watch?v=abc&list=PLabc123"));
        assertFalse(PlaylistSupport.isPlaylist("https://youtu.be/abc"));
    }
    @Test public void normalizesYouTubeEntries() {
        assertEquals("https://www.youtube.com/watch?v=ab_12-X",PlaylistSupport.videoUrl("ab_12-X"));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsUntrustedPlaylistEntry() {
        PlaylistSupport.videoUrl("https://evil.invalid/a");
    }
    @Test public void selectedItemCount() {
        assertEquals("2 из 5",PlaylistSupport.selectionCount(2,5));
    }
}
