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
}
