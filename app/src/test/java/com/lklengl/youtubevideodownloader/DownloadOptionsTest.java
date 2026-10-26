package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;

public class DownloadOptionsTest {
    @Test public void youtubeTitleIsOutputName() {
        assertEquals("%(title).180B.%(ext)s", DownloadOptions.OUTPUT_TEMPLATE);
    }
    @Test public void qualityLimit() { assertEquals("bv*[height<=720]+ba/b[height<=720]", DownloadOptions.format(false, "720")); }
    @Test public void bestQuality() { assertEquals("bv*+ba/b", DownloadOptions.format(false, "best")); }
    @Test public void audioFormat() { assertEquals("bestaudio/best", DownloadOptions.format(true, "1080")); }
    @Test(expected=IllegalArgumentException.class) public void rejectsBadQuality() { DownloadOptions.format(false, "9999"); }
    @Test public void acceptsYouTubeUrl() { assertEquals("https://youtu.be/test", DownloadOptions.validateUrl(" https://youtu.be/test ")); }
    @Test(expected=IllegalArgumentException.class) public void rejectsOtherHosts() { DownloadOptions.validateUrl("https://youtube.com.evil.example/video"); }
    @Test(expected=IllegalArgumentException.class) public void rejectsHttp() { DownloadOptions.validateUrl("http://youtube.com/watch?v=x"); }
    @Test public void acceptsProxy() { assertEquals("http://127.0.0.1:8888",DownloadOptions.validateProxy(" http://127.0.0.1:8888 ")); }
    @Test(expected=IllegalArgumentException.class) public void rejectsBadProxy() { DownloadOptions.validateProxy("file:///etc/passwd"); }
}
