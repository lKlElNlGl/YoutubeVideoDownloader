package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;
public class EngineRecoveryPolicyTest {
    @Test public void outdatedEngineWithHttp403SuggestsRefresh(){
        String err="WARNING: Your yt-dlp version (2025.11.12) is older than 90 days!\nERROR: unable to download video data: HTTP Error 403: Forbidden";
        assertTrue(EngineRecoveryPolicy.shouldUpdate(err));
    }
    @Test public void extractorSignatureFailureSuggestsRefresh(){
        assertTrue(EngineRecoveryPolicy.shouldUpdate("ERROR: [youtube] nsig extraction failed"));
    }
    @Test public void sslAndDnsFailuresAreNotMistakenForEngineUpdates(){
        assertFalse(EngineRecoveryPolicy.shouldUpdate("WARNING: Your yt-dlp version (2025.11.12) is older than 90 days!\nSSL: UNEXPECTED_EOF_WHILE_READING"));
        assertFalse(EngineRecoveryPolicy.shouldUpdate("Network is unreachable; No address associated with hostname"));
        assertFalse(EngineRecoveryPolicy.shouldUpdate("HTTP Error 403: Forbidden"));
    }
    @Test public void networkErrorsAreRussianForPreview(){
        assertEquals("Не удалось подключиться к YouTube. Проверьте интернет и повторите попытку.",
            EngineRecoveryPolicy.shortMessage("SSL handshake operation timed out"));
        assertEquals("Не удалось подключиться к YouTube. Проверьте интернет и повторите попытку.",
            EngineRecoveryPolicy.shortMessage("No address associated with hostname"));
    }
    @Test public void errorsWithOldComponentSuggestManualAction(){
        assertEquals("Не удалось загрузить видео. Проверьте обновление компонента YouTube.",
            EngineRecoveryPolicy.shortMessage("WARNING: Your yt-dlp version (2025.11.12) is older than 90 days!\nERROR: HTTP Error 403: Forbidden"));
    }
    @Test public void checkingLimitAndRetries(){
        long day=24L*60*60*1000;
        assertTrue(EngineRecoveryPolicy.isCheckDue(day+1,0));
        assertFalse(EngineRecoveryPolicy.isCheckDue(day+10,day));
        assertTrue(EngineRecoveryPolicy.isCheckDue(2*day+1,day));
        assertTrue(EngineRecoveryPolicy.shouldRetry(true,false));
        assertFalse(EngineRecoveryPolicy.shouldRetry(true,true));
        assertFalse(EngineRecoveryPolicy.shouldRetry(false,false));
    }
}
