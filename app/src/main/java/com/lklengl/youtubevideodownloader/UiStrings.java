package com.lklengl.youtubevideodownloader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** English is the default; user preference is independent from the device locale. */
public final class UiStrings {
    public static final String[] LANGUAGE_NAMES={"English","Русский"};
    private static final Map<String,String> ENGLISH;
    static {
        Map<String,String> strings=new HashMap<>();
        strings.put("Загрузка", "Downloads");
        strings.put("Очередь", "Queue");
        strings.put("Настройки", "Settings");
        strings.put("Светлая", "Light");
        strings.put("Тёмная", "Dark");
        strings.put("Как в системе", "System default");
        strings.put("Язык", "Language");
        strings.put("Внешний вид", "Appearance");
        strings.put("Тема оформления", "Theme");
        strings.put("Сетевые параметры", "Network settings");
        strings.put("Использовать HTTP/HTTPS-прокси", "Use HTTP/HTTPS proxy");
        strings.put("Сохранение файлов", "File storage");
        strings.put("Папка сохранения", "Save folder");
        strings.put("Изменить папку", "Change folder");
        strings.put("Вернуть папку по умолчанию", "Restore default folder");
        strings.put("Открыть папку с файлами", "Open downloads folder");
        strings.put("Обновления", "Updates");
        strings.put("Загрузка с YouTube", "YouTube downloads");
        strings.put("Обновить компонент работы с YouTube", "Update YouTube component");
        strings.put("Приложение", "Application");
        strings.put("Проверить новую версию", "Check for updates");
        strings.put("О приложении", "About");
        strings.put("СКАЧИВАНИЕ ВИДЕО И МУЗЫКИ", "VIDEO AND MUSIC DOWNLOADER");
        strings.put("Ссылка на YouTube", "YouTube link");
        strings.put("Ссылка на видео или плейлист", "Video or playlist URL");
        strings.put("Очистить ссылку", "Clear URL");
        strings.put("Вставить ссылку", "Paste link");
        strings.put("Информация о видео / плейлисте", "Video / playlist information");
        strings.put("Подробности", "Details");
        strings.put("ИНФОРМАЦИЯ О ВИДЕО", "VIDEO INFORMATION");
        strings.put("ИНФОРМАЦИЯ О ПЛЕЙЛИСТЕ", "PLAYLIST INFORMATION");
        strings.put("Открыть список видео", "Open video list");
        strings.put("Обновить информацию", "Refresh information");
        strings.put("Параметры загрузки", "Download options");
        strings.put("Формат", "Format");
        strings.put("Качество видео", "Video quality");
        strings.put("Скачивание", "Downloading");
        strings.put("Скачать без очереди", "Download immediately");
        strings.put("Добавить в очередь", "Add to queue");
        strings.put("Очистить завершённые", "Clear completed");
        strings.put("Отмена", "Cancel");
        strings.put("Отменить", "Cancel");
        strings.put("Закрыть", "Close");
        strings.put("ОК", "OK");
        strings.put("Добавить", "Add");
        strings.put("Удалить", "Delete");
        strings.put("Плейлист", "Playlist");
        strings.put("Видео в плейлисте", "Videos in playlist");
        strings.put("Выбрать все", "Select all");
        strings.put("Выбрать всё", "Select all");
        strings.put("Качество для отмеченных", "Quality for selected");
        strings.put("Уже добавлено", "Already queued");
        strings.put("Уже скачано", "Already downloaded");
        strings.put("Длительность неизвестна", "Duration unknown");
        strings.put("Назад", "Back");
        strings.put("Обновить плейлист", "Refresh playlist");
        strings.put("Качество для этого видео", "Quality for this video");
        strings.put("Плейлист обновлён", "Playlist updated");
        strings.put("Не удалось обновить плейлист", "Could not refresh playlist");
        strings.put("Выберите видео", "Select videos");
        strings.put("Повторное добавление", "Add again");
        strings.put("Максимальное", "Best available");
        strings.put("Очередь пуста", "Queue is empty");
        strings.put("Текущая загрузка", "Current download");
        strings.put("Следующие в очереди", "Up next");
        strings.put("Завершённые", "Completed");
        strings.put("Ошибки и отменённые", "Failed and cancelled");
        strings.put("На паузе", "Paused");
        strings.put("Готово", "Done");
        strings.put("Ожидает", "Waiting");
        strings.put("Выполняется", "Running");
        strings.put("Ошибка", "Error");
        strings.put("Отменено", "Cancelled");
        strings.put("Открыть файл", "Open file");
        strings.put("Открыть с помощью…", "Open with…");
        strings.put("Открыть папку", "Open folder");
        strings.put("Повторить загрузку", "Retry download");
        strings.put("Продолжить", "Resume");
        strings.put("Удалить запись", "Delete entry");
        strings.put("Приостановить загрузку", "Pause download");
        strings.put("Продолжить загрузку", "Resume download");
        strings.put("Поставить на паузу", "Pause download");
        strings.put("Действия с загрузкой", "Download actions");
        strings.put("Файл", "File");
        strings.put("Файл не найден", "File not found");
        strings.put("Удалить файл", "Delete file");
        strings.put("Удалить скачанный файл?", "Delete downloaded file?");
        strings.put("Прокрутите вниз", "Scroll down");
        strings.put("Подробности ошибки", "Error details");
        strings.put("Обновление уже выполняется.", "Update already in progress.");
        strings.put("Компонент обновлён.", "Component updated.");
        strings.put("Проверка…", "Checking…");
        strings.put("Установлена последняя версия.", "Already up to date.");
        strings.put("Проверка обновлений", "Checking for updates");
        strings.put("Позже", "Later");
        strings.put("Установить", "Install");
        strings.put("Проверить новую версию приложения", "Check for app updates");
        strings.put("Установка обновления", "Install update");
        strings.put("Запрос отменён", "Request cancelled");
        strings.put("Не удалось открыть файл", "Could not open file");
        strings.put("Невозможно добавить загрузку", "Could not add download");
        strings.put("Добавлено в очередь", "Added to queue");
        strings.put("Очистить", "Clear");
        strings.put("Очистить ссылку и информацию о видео?", "Clear the URL and video information?");
        strings.put("Нет подключения к интернету. Проверьте сеть и повторите попытку.", "No internet connection. Check your network and try again.");
        strings.put("Время ожидания истекло. Проверьте интернет и повторите попытку.", "Connection timed out. Check your internet connection and try again.");
        strings.put("Получаем информацию с YouTube…", "Fetching information from YouTube…");
        strings.put("Ожидаем ответ сервера YouTube…", "Waiting for YouTube…");
        strings.put("YouTube отвечает дольше обычного, запрос продолжается.", "YouTube is taking longer than usual. Still waiting.");
        strings.put("Большой плейлист может потребовать больше времени.", "Large playlists may take longer.");
        strings.put("Продолжаем получать сведения о видео…", "Still fetching video information…");
        strings.put("Запрашиваем список видео с YouTube…", "Requesting playlist videos from YouTube…");
        strings.put("Обрабатывается информация о плейлисте…", "Processing playlist information…");
        strings.put("Большой плейлист может загружаться дольше обычного.", "Large playlists may take longer than usual.");
        strings.put("Продолжаем получать сведения о видео…", "Still loading video information…");
        strings.put("Плейлист не сохранён. Получите список повторно.", "Playlist not saved. Fetch the list again.");
        strings.put("Аудио", "Audio");
        strings.put("Определяется…", "Detecting…");
        strings.put("Без названия", "Untitled");
        strings.put("Не указано", "Not specified");
        strings.put("Видео MP4", "Video MP4");
        strings.put("Аудио MP3 · 192 кбит/с", "Audio MP3 · 192 kbps");
        strings.put("Видео WebM", "Video WebM");
        strings.put("Видео MKV", "Video MKV");
        strings.put("Аудио M4A", "Audio M4A");
        strings.put("Отметить все", "Select all");
        strings.put("Применить", "Apply");
        strings.put("Все", "All");
        strings.put("Скачанные", "Downloaded");
        strings.put("Нескачанные", "Not downloaded");
        strings.put("Только отмеченные", "Selected only");
        strings.put("Только неотмеченные", "Unselected only");
        strings.put("Сначала отмеченные", "Selected first");
        strings.put("Только уже добавленные", "Already queued only");
        strings.put("Только не добавленные", "Not queued yet");
        strings.put("Качество для отмеченных", "Quality for selected");
        ENGLISH=Collections.unmodifiableMap(strings);
    }
    private UiStrings(){}
    public static String normalizeLanguage(String value) {
        return "ru".equals(value)?"ru":"en";
    }
    public static String translate(String language,String source) {
        if(source==null)return "";
        if("ru".equals(normalizeLanguage(language))) {
            for(Map.Entry<String,String> entry:ENGLISH.entrySet())
                if(entry.getValue().equals(source))return entry.getKey();
            return source;
        }
        String translated=ENGLISH.get(source);
        if(translated!=null)return translated;
        for(Map.Entry<String,String> entry:ENGLISH.entrySet())
            if(entry.getValue().equals(source))return source;
        return source;
    }
    public static String translate(android.content.Context context,String source) {
        String code=context.getSharedPreferences("design",android.content.Context.MODE_PRIVATE)
            .getString("language","en");
        return translate(code,source);
    }
    /** Translate labels and formatted status strings without touching queue identifiers. */
    public static String display(android.content.Context context,String source) {
        String result=translate(context,source);
        if(!result.equals(source)||"ru".equals(context.getSharedPreferences("design",0).getString("language","en")))return result;
        for(Map.Entry<String,String> e:ENGLISH.entrySet()){
            String old=e.getKey();
            if(source.startsWith(old)&&source.length()>old.length()&&
                (old.endsWith(" ")||old.endsWith("…")||old.endsWith(": ")))
                return e.getValue()+source.substring(old.length());
        }
        return source;
    }
}