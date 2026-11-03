package com.ony.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test fun autoFollowsThePhone() {
        assertEquals(true, ThemeMode.AUTO.isDark(systemDark = true))
        assertEquals(false, ThemeMode.AUTO.isDark(systemDark = false))
    }

    @Test fun lightAndDarkIgnoreThePhone() {
        assertEquals(false, ThemeMode.LIGHT.isDark(systemDark = true))
        assertEquals(true, ThemeMode.DARK.isDark(systemDark = false))
    }

    @Test fun unknownSavedValueFallsBackToAuto() {
        assertEquals(ThemeMode.AUTO, ThemeMode.fromSaved("SEPIA"))
        assertEquals(ThemeMode.AUTO, ThemeMode.fromSaved(null))
        assertEquals(ThemeMode.DARK, ThemeMode.fromSaved("DARK"))
    }
}
