package com.lklengl.youtubevideodownloader;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public final class QualityChoicesTest {
    @Test public void dynamicResolutionsAreDescendingWithoutDuplicates(){
        QualityChoices c=QualityChoices.fromDetected("1080p, 854p, 1280p, 1080p, 640p");
        assertEquals(Arrays.asList("best","1280","1080","854","640"),c.values());
        assertEquals(Arrays.asList("Максимальное","1280p","1080p","854p","640p"),c.labels());
    }
    @Test public void unavailableOrInvalidMetadataUsesFallback(){
        assertEquals("best",QualityChoices.fromDetected("Не указано").values().get(0));
        assertEquals(DownloadOptions.QUALITIES.length,QualityChoices.fromDetected("").values().size());
    }
    @Test public void discoveredResolutionCanBePassedToYtDlp(){
        assertTrue(DownloadOptions.format(false,"854").contains("height<=854"));
        assertTrue(DownloadOptions.format(false,"1280").contains("height<=1280"));
    }
    @Test public void invalidResolutionsAreRejected(){
        assertFalse(DownloadOptions.isQuality("-10"));
        assertFalse(DownloadOptions.isQuality("1920]"));
        assertFalse(DownloadOptions.isQuality("99999"));
    }
    @Test public void selectingMaximumIsAlwaysFirst(){
        assertEquals("best",QualityChoices.fromDetected("720p, 360p").values().get(0));
    }
}
