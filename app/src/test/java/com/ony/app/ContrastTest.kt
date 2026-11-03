package com.ony.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every text colour must stay readable on its background, in light and dark (WCAG AA, 4.5:1). */
class ContrastTest {

    private fun ratio(a: Color, b: Color): Double {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun check(name: String, p: OnyPalette) {
        val pairs = listOf(
            "text on page" to (p.onPage to p.page),
            "muted text on page" to (p.pageMuted to p.page),
            "text on block" to (p.onBlock to p.block),
            "muted text on block" to (p.blockMuted to p.block),
            "text on raised block" to (p.onBlock to p.blockRaised),
            "stat values on block" to (p.statValue to p.block),
            "text on lavender" to (p.onAccent to p.accent),
            "lavender on block" to (p.accent to p.block),
        )
        pairs.forEach { (what, colors) ->
            val r = ratio(colors.first, colors.second)
            assertTrue("$name: $what is ${"%.2f".format(r)}:1", r >= 4.5)
        }
    }

    @Test fun lightPaletteIsReadable() = check("light", LightPalette)

    @Test fun darkPaletteIsReadable() = check("dark", DarkPalette)

    // Blocks must still read as blocks against the page in dark mode.
    @Test fun darkBlocksStandOutFromThePage() {
        assertTrue(ratio(DarkPalette.block, DarkPalette.page) >= 1.25)
    }
}
