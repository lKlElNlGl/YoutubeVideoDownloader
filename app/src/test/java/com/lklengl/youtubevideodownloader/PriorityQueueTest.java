package com.lklengl.youtubevideodownloader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class PriorityQueueTest {
    private DownloadQueue.Item item(String id,String status) {
        DownloadQueue.Item result=new DownloadQueue.Item();
        result.id=id;
        result.status=status;
        return result;
    }
    @Test public void urgentDownloadRunsBeforeAlreadyWaitingJobsWithoutInterruptingRunningJob(){
        List<DownloadQueue.Item> jobs=new ArrayList<>(Arrays.asList(
            item("active",DownloadQueue.RUNNING),
            item("older",DownloadQueue.WAITING),
            item("last",DownloadQueue.WAITING)));
        QueueRules.prependPriority(jobs,Arrays.asList(item("urgent",DownloadQueue.WAITING)));
        assertEquals("urgent",QueueRules.nextWaiting(jobs).id);
        assertEquals(DownloadQueue.RUNNING,jobs.stream().filter(j->"active".equals(j.id)).findFirst().get().status);
        assertEquals("older",jobs.get(2).id);
    }
    @Test public void batchPriorityPreservesPlaylistOrder(){
        List<DownloadQueue.Item> jobs=new ArrayList<>(Arrays.asList(item("old",DownloadQueue.WAITING)));
        QueueRules.prependPriority(jobs,Arrays.asList(item("first",DownloadQueue.WAITING),item("second",DownloadQueue.WAITING)));
        assertEquals("first",jobs.get(0).id);
        assertEquals("second",jobs.get(1).id);
        assertEquals("old",jobs.get(2).id);
    }
}
