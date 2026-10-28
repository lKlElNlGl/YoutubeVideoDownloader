package com.lklengl.youtubevideodownloader;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
public class QueuePolicyTest {
    private DownloadQueue.Item item(String id,String status,String url){
        DownloadQueue.Item i=new DownloadQueue.Item();i.id=id;i.status=status;i.url=url;return i;
    }
    @Test public void detectDuplicateEvenWithDifferentYoutubeUrlForms(){
        DownloadQueue.Item waiting=item("1",DownloadQueue.WAITING,"https://youtube.com/shorts/aBc_123");
        assertTrue(QueuePolicy.hasActiveDuplicate(Arrays.asList(waiting),
            "https://youtu.be/aBc_123?si=xyz"));
        assertTrue(QueuePolicy.hasActiveDuplicate(Arrays.asList(waiting),
            "https://www.youtube.com/watch?v=aBc_123"));
        assertFalse(QueuePolicy.hasActiveDuplicate(Arrays.asList(waiting),
            "https://youtu.be/other456"));
    }
    @Test public void finishedVideosAreNotConsideredDuplicates(){
        assertFalse(QueuePolicy.hasActiveDuplicate(Arrays.asList(
            item("1",DownloadQueue.DONE,"https://youtu.be/abc")),"https://youtu.be/abc"));
    }
    @Test public void userFacingErrorsNeverIncludeFullYtDlpLogs(){
        String log="WARNING: Your yt-dlp version is older than 90 days!\n"
            +"WARNING: [youtube] SSL: UNEXPECTED_EOF_WHILE_READING\n"
            +"ERROR: unable to download video data: HTTP Error 403: Forbidden";
        assertEquals("Ошибка загрузки: HTTP 403",QueuePolicy.errorSummary(log));
        assertEquals("Ошибка сети (SSL)",QueuePolicy.errorSummary("SSL: UNEXPECTED_EOF_WHILE_READING"));
        assertTrue(QueuePolicy.errorSummary("very long message".repeat(70)).length()<=110);
    }
    @Test public void removeRunningRequiresDeferredDeletion(){
        assertTrue(QueuePolicy.needsCancellation(DownloadQueue.RUNNING));
        assertFalse(QueuePolicy.needsCancellation(DownloadQueue.WAITING));
        assertFalse(QueuePolicy.needsCancellation(DownloadQueue.DONE));
    }
}
