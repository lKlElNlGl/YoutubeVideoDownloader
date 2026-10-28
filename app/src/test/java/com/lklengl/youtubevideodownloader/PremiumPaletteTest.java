package com.lklengl.youtubevideodownloader;

import org.junit.Test;
import static org.junit.Assert.*;

/** Regression tests for the agreed dark-theme palette. */
public final class PremiumPaletteTest {
    @Test public void buttonsUseTheExactApprovedGradientEndpoints(){
        assertEquals(0xFFFC2A3F,PremiumPalette.GRADIENT_TOP_LEFT);
        assertEquals(0xFFFE1F2C,PremiumPalette.GRADIENT_BOTTOM_RIGHT);
    }
    @Test public void dropdownSurfaceSeparatesVisiblyFromCards(){
        assertNotEquals(PremiumPalette.DARK_CARD,PremiumPalette.DARK_POPUP);
        assertTrue(channelDifference(PremiumPalette.DARK_CARD,PremiumPalette.DARK_POPUP)>=36);
        assertNotEquals(PremiumPalette.DARK_POPUP,PremiumPalette.DARK_SELECTED);
    }
    private static int channelDifference(int x,int y){
        return Math.abs(((x>>16)&255)-((y>>16)&255))+
            Math.abs(((x>>8)&255)-((y>>8)&255))+
            Math.abs((x&255)-(y&255));
    }
}
