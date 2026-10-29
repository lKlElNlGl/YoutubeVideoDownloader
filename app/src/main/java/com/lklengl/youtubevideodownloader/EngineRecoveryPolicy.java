package com.lklengl.youtubevideodownloader;

import java.util.Locale;

/** Pure classifier: protects against updating yt-dlp for SSL/DNS outages. */
public final class EngineRecoveryPolicy {
    private static final long DAY=24L*60*60*1000;
    private EngineRecoveryPolicy(){}
    public static boolean isCheckDue(long now,long previous){
        return previous<=0 || now<previous || now-previous>=DAY;
    }
    public static boolean shouldRetry(boolean updated,boolean alreadyRetried){
        return updated&&!alreadyRetried;
    }
    private static String lower(String message){
        return message==null?"":message.toLowerCase(Locale.ROOT);
    }
    private static boolean network(String s){
        return s.contains("ssl")||s.contains("certificate")||s.contains("network is unreachable")||
            s.contains("no address associated with hostname")||s.contains("name or service not known")||
            s.contains("temporary failure in name resolution")||s.contains("timed out")||
            s.contains("timeout")||s.contains("connection abort")||s.contains("connection reset")||
            s.contains("connection refused")||s.contains("unable to resolve")||
            s.contains("connection failed")||s.contains("dns")||
            s.contains("failed to establish a new connection");
    }
    public static boolean shouldUpdate(String message){
        String s=lower(message);
        if(network(s))return false;
        if(s.contains("nsig extraction failed")||s.contains("signature extraction failed")||
            s.contains("unable to extract")||s.contains("unable to download api page"))
            return true;
        boolean outdated=s.contains("yt-dlp version")&&s.contains("older than");
        return outdated && (s.contains("error:")||s.contains("http error")||
            s.contains("403")||s.contains("unable to download"));
    }
    public static String shortMessage(String message){
        String s=lower(message);
        if(network(s))return "Не удалось подключиться к YouTube. Проверьте интернет и повторите попытку.";
        if(shouldUpdate(message))return "Не удалось загрузить видео. Проверьте обновление компонента YouTube.";
        if(s.contains("http error 403")||s.contains("http 403"))
            return "YouTube отказал в доступе (HTTP 403). Попробуйте повторить позже.";
        if(s.contains("http error 429"))
            return "YouTube временно ограничил запросы (HTTP 429). Повторите позже.";
        return "Не удалось получить информацию о видео. Откройте подробности ошибки.";
    }
}
