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
    public static boolean isQuality(String quality) {
        for (String item : QUALITIES) if (item.equals(quality)) return true;
        return false;
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
