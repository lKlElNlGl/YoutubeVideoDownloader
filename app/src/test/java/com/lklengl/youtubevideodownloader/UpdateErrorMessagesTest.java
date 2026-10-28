package com.lklengl.youtubevideodownloader;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.io.IOException;
import javax.net.ssl.SSLException;
import org.junit.Test;
import static org.junit.Assert.*;

public class UpdateErrorMessagesTest {
    @Test public void connectionAbortIsShownInPlainRussian(){
        assertEquals("Соединение прервано. Проверьте интернет и повторите попытку.",
            UpdateErrorMessages.userMessage(new SocketException("Software caused connection abort")));
    }
    @Test public void noInternetHasClearRussianMessage(){
        assertEquals("Нет подключения к интернету. Проверьте сеть и повторите попытку.",
            UpdateErrorMessages.userMessage(new UnknownHostException("github.com")));
    }
    @Test public void timeoutAndSslErrorsDoNotLeakJavaMessages(){
        assertEquals("Время ожидания истекло. Проверьте интернет и повторите попытку.",
            UpdateErrorMessages.userMessage(new SocketTimeoutException("Read timed out")));
        assertEquals("Не удалось установить защищённое соединение. Проверьте интернет и повторите попытку.",
            UpdateErrorMessages.userMessage(new SSLException("UNEXPECTED_EOF_WHILE_READING")));
    }
    @Test public void unexpectedAndHttpErrorsAreUnderstood(){
        assertEquals("Не удалось скачать обновление (HTTP 503). Повторите попытку позже.",
            UpdateErrorMessages.userMessage(new IOException("GitHub HTTP 503")));
        assertEquals("Не удалось скачать обновление. Повторите попытку.",
            UpdateErrorMessages.userMessage(new IOException("the server delivered some opaque error")));
    }
}
