package com.lklengl.youtubevideodownloader;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLException;

/** Translate update download exceptions into friendly Russian messages. */
public final class UpdateErrorMessages {
    private static final Pattern HTTP=Pattern.compile("(?i)\\b(?:github\\s+)?http\\s+(?:error\\s+)?([1-5][0-9]{2})\\b");
    private UpdateErrorMessages(){}
    public static String userMessage(Throwable error){
        Throwable cursor=error;
        for(int i=0;i<10&&cursor!=null;i++,cursor=cursor.getCause()){
            if(cursor instanceof UnknownHostException)
                return "Нет подключения к интернету. Проверьте сеть и повторите попытку.";
            if(cursor instanceof SocketTimeoutException)
                return "Время ожидания истекло. Проверьте интернет и повторите попытку.";
            if(cursor instanceof SSLException)
                return "Не удалось установить защищённое соединение. Проверьте интернет и повторите попытку.";
            if(cursor instanceof SocketException||cursor instanceof ConnectException)
                return "Соединение прервано. Проверьте интернет и повторите попытку.";
            String message=cursor.getMessage();
            if(message==null)continue;
            Matcher match=HTTP.matcher(message);
            if(match.find())return "Не удалось скачать обновление (HTTP "+match.group(1)+"). Повторите попытку позже.";
            String lower=message.toLowerCase(Locale.ROOT);
            if(lower.contains("connection abort")||lower.contains("connection reset")||lower.contains("broken pipe"))
                return "Соединение прервано. Проверьте интернет и повторите попытку.";
            if(lower.contains("timed out")||lower.contains("timeout"))
                return "Время ожидания истекло. Проверьте интернет и повторите попытку.";
            if(lower.contains("network is unreachable")||lower.contains("unable to resolve"))
                return "Нет подключения к интернету. Проверьте сеть и повторите попытку.";
        }
        return "Не удалось скачать обновление. Повторите попытку.";
    }
}
