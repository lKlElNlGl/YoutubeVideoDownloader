package com.lklengl.youtubevideodownloader;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;
public class QueueRulesTest {
    @Test public void onlyWaitingAndRunningCountTowardsCapacity(){
        DownloadQueue.Item a=new DownloadQueue.Item();a.status=DownloadQueue.WAITING;
        DownloadQueue.Item b=new DownloadQueue.Item();b.status=DownloadQueue.RUNNING;
        DownloadQueue.Item c=new DownloadQueue.Item();c.status=DownloadQueue.DONE;
        DownloadQueue.Item d=new DownloadQueue.Item();d.status=DownloadQueue.ERROR;
        assertEquals(2,QueueRules.activeCount(Arrays.asList(a,b,c,d)));
    }
    @Test public void durationsAreReadable() {
        assertEquals("1:01",QueueRules.readableDuration(61));
        assertEquals("1:01:01",QueueRules.readableDuration(3661));
    }
    @Test public void usesVideoTitleNotInternalIdForFilename() {
        assertEquals("%(title).180B.%(ext)s",QueueRules.filenameTemplate());
    }
}
