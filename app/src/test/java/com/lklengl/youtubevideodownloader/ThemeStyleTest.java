package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;
public class ThemeStyleTest {
    @Test public void themeNamesInAgreedOrder() {
        assertArrayEquals(new String[]{"Светлая","Тёмная","Как в системе"},ThemeStyle.MODES);
    }
    @Test public void resolvesDarkBySystem() {
        assertTrue(ThemeStyle.isDark(2,true));
        assertFalse(ThemeStyle.isDark(2,false));
        assertTrue(ThemeStyle.isDark(1,false));
        assertFalse(ThemeStyle.isDark(0,true));
    }
}
