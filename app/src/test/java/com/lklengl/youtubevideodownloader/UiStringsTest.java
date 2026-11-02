package com.lklengl.youtubevideodownloader;

import org.junit.Test;
import static org.junit.Assert.*;

public class UiStringsTest {
    @Test public void englishIsDefaultForFreshInstallOrInvalidPreference() {
        assertEquals("en", UiStrings.normalizeLanguage(null));
        assertEquals("en", UiStrings.normalizeLanguage(""));
        assertEquals("en", UiStrings.normalizeLanguage("de"));
    }
    @Test public void savedRussianPreferenceIsPreserved() {
        assertEquals("ru", UiStrings.normalizeLanguage("ru"));
        assertEquals("en", UiStrings.normalizeLanguage("en"));
    }
    @Test public void translatesUiLabelsInBothDirections() {
        assertEquals("Downloads", UiStrings.translate("en", "Загрузка"));
        assertEquals("Queue", UiStrings.translate("en", "Очередь"));
        assertEquals("Settings", UiStrings.translate("en", "Настройки"));
        assertEquals("Настройки", UiStrings.translate("ru", "Settings"));
    }
    @Test public void technicalIdentifiersAreNeverTranslated() {
        assertEquals("https://youtube.com/watch?v=abc", UiStrings.translate("en", "https://youtube.com/watch?v=abc"));
        assertEquals("mp4", UiStrings.translate("ru", "mp4"));
    }
    @Test public void languageChoicesAreAlwaysNamedInNativeLanguage() {
        assertArrayEquals(new String[]{"English", "Русский"}, UiStrings.LANGUAGE_NAMES);
    }
    @Test public void formatsDynamicCountersWithoutRussianFragments() {
        assertEquals("Selected: 8 of 554", UiStrings.displayLanguage("en", "Выбрано: 8 из 554"));
        assertEquals("Added to queue: 8", UiStrings.displayLanguage("en", "Добавлено в очередь: 8"));
    }
    @Test public void russianModeKeepsOriginalDynamicLabels() {
        assertEquals("Выбрано: 8 из 554", UiStrings.displayLanguage("ru", "Выбрано: 8 из 554"));
    }    @Test public void formatsTransferUnitsAsEnglish() {
        assertEquals("Downloaded: 20.5 MB/s", UiStrings.displayLanguage("en","Скачано: 20.5 МБ/с"));
    }
}
