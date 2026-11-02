package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;
public class FriendlyDownloadErrorTest {
 @Test public void rawYtDlpNetworkFailureHasReadableSummary(){
  String raw="WARNING [youtube] [Errno 101] Network is unreachable. ERROR: Giving up after 3 retries";
  assertEquals("Не удалось подключиться к YouTube. Проверьте интернет и настройки прокси.",
   QueuePolicy.errorSummary(raw));
 }
 @Test public void accessChallengeIsNotShownAsWallOfLog(){
  assertEquals("YouTube запрашивает подтверждение доступа. Попробуйте повторить позже.",
   QueuePolicy.errorSummary("ERROR: Sign in to confirm you're not a bot"));
 }
}