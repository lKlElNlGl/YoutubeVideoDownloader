package com.lklengl.youtubevideodownloader;

import java.net.URI;

public final class DownloadOptions {
    private DownloadOptions() {}
    // yt-dlp retains the original title and Unicode; 180 UTF-8 bytes avoids filesystem limits.
    public static final String OUTPUT_TEMPLATE = "%(title).180B.%(ext)s";
    public static final String[] QUALITIES = {"best", "2160", "1440", "1080", "720", "480", "360"};
    public static String format(boolean audio, String quality) {
        if (audio) return "bestaudio/best";
        if ("best".equals(quality)) return "bv*+ba/b";
        if (!isQuality(quality)) throw new IllegalArgumentException("Недопустимое качество");
        return "bv*[height<=" + quality + "]+ba/b[height<=" + quality + "]";
    }
    /** WebM output requires WebM-compatible video and audio streams. */
    public static String formatForExport(boolean audio,String quality,String export){
        if(!"webm".equals(export))return format(audio,quality);
        if("best".equals(quality))return "bv*[ext=webm]+ba[ext=webm]/b[ext=webm]";
        if(!isQuality(quality))throw new IllegalArgumentException("Недопустимое качество");
        return "bv*[ext=webm][height<="+quality+"]+ba[ext=webm]/b[ext=webm][height<="+quality+"]";
    }
    public static boolean isQuality(String quality) {
        if (quality == null || !quality.matches("[1-9][0-9]{1,3}")) return "best".equals(quality);
        try { int h=Integer.parseInt(quality); return h>=100 && h<=4320; }
        catch(NumberFormatException ignored) { return false; }
    }
    public static String validateUrl(String value) {
        if (value == null) throw new IllegalArgumentException("Введите ссылку");
        String url = value.trim();
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null ||
                !(host.equalsIgnoreCase("youtube.com") || host.toLowerCase().endsWith(".youtube.com") ||
                  host.equalsIgnoreCase("youtu.be") || host.equalsIgnoreCase("m.youtube.com"))) {
                throw new IllegalArgumentException("Нужна HTTPS-ссылка на YouTube");
            }
            return url;
        } catch (java.net.URISyntaxException ex) {
            throw new IllegalArgumentException("Некорректная ссылка");
        }
    }
    public static String validateProxy(String value) {
        String url = value == null ? "" : value.trim();
        if (url.isEmpty()) return "";
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (uri.getHost() == null || uri.getPort() < 1 || uri.getPort() > 65535 ||
                !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)))
                throw new IllegalArgumentException("Укажите HTTP(S)-прокси с портом");
            return url;
        } catch (java.net.URISyntaxException ex) {
            throw new IllegalArgumentException("Некорректный адрес прокси");
        }
    }
}
