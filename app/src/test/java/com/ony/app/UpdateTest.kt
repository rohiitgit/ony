package com.ony.app

import org.junit.Assert.*
import org.junit.Test

class UpdateTest {

    private val validJson = """
        {"versionName":"0.1.42","versionCode":42,"tag":"v0.1.42","downloadUrl":"https://github.com/rohiitgit/ony/releases/download/v0.1.42/Ony-0.1.42.apk","releaseUrl":"https://github.com/rohiitgit/ony/releases/tag/v0.1.42","pageUrl":"https://rohiitgit.github.io/ony/download/"}
    """.trimIndent()

    @Test fun parseManifest_readsAllFields() {
        val m = UpdateCheck.parseManifest(validJson)
        assertNotNull(m)
        assertEquals("0.1.42", m!!.versionName)
        assertEquals(42L, m.versionCode)
        assertEquals("v0.1.42", m.tag)
        assertEquals("https://github.com/rohiitgit/ony/releases/download/v0.1.42/Ony-0.1.42.apk", m.downloadUrl)
        assertEquals("https://github.com/rohiitgit/ony/releases/tag/v0.1.42", m.releaseUrl)
        assertEquals("https://rohiitgit.github.io/ony/download/", m.pageUrl)
    }

    @Test fun parseManifest_defaultsVersionNameFromCode() {
        val m = UpdateCheck.parseManifest("""{"versionCode":7}""")
        assertNotNull(m)
        assertEquals("0.1.7", m!!.versionName)
        assertNull(m.tag)
        assertNull(m.downloadUrl)
    }

    @Test fun parseManifest_rejectsMissingOrInvalid() {
        assertNull(UpdateCheck.parseManifest(""))
        assertNull(UpdateCheck.parseManifest("{}"))
        assertNull(UpdateCheck.parseManifest("""{"versionCode":0}"""))
        assertNull(UpdateCheck.parseManifest("""{"versionCode":-1}"""))
        assertNull(UpdateCheck.parseManifest("not json"))
        assertNull(UpdateCheck.parseManifest("""{"versionName":"0.1.5"}"""))
    }

    @Test fun isUpdateAvailable_onlyWhenNewer() {
        val m = UpdateCheck.parseManifest("""{"versionCode":10}""")!!
        assertTrue(UpdateCheck.isUpdateAvailable(5, m))
        assertFalse(UpdateCheck.isUpdateAvailable(10, m))
        assertFalse(UpdateCheck.isUpdateAvailable(11, m))
    }

    @Test fun shouldCheck_respectsThrottle() {
        assertTrue(UpdateCheck.shouldCheck(0, 1000, 60_000))
        assertTrue(UpdateCheck.shouldCheck(1000, 61_000, 60_000))
        assertFalse(UpdateCheck.shouldCheck(1000, 60_999, 60_000))
        assertTrue(UpdateCheck.shouldCheck(5000, 1000, 60_000)) // clock went backwards
        assertTrue(UpdateCheck.shouldCheck(1000, 2000, 0))
        assertTrue(UpdateCheck.shouldCheck(1000, 2000, -1))
    }

    @Test fun shouldShowPrompt_onlyForNewerAndNotDismissed() {
        val m = UpdateCheck.parseManifest("""{"versionCode":10}""")!!
        assertTrue(UpdateCheck.shouldShowPrompt(5, m, 0))
        assertFalse(UpdateCheck.shouldShowPrompt(10, m, 0))
        assertFalse(UpdateCheck.shouldShowPrompt(5, m, 10))
        assertFalse(UpdateCheck.shouldShowPrompt(5, m, 15))
        assertTrue(UpdateCheck.shouldShowPrompt(5, m, 9))
    }

    @Test fun shouldNotify_onlyForNewerAndNotNotified() {
        val m = UpdateCheck.parseManifest("""{"versionCode":12}""")!!
        assertTrue(UpdateCheck.shouldNotify(5, m, 0))
        assertFalse(UpdateCheck.shouldNotify(12, m, 0))
        assertFalse(UpdateCheck.shouldNotify(5, m, 12))
        assertFalse(UpdateCheck.shouldNotify(5, m, 13))
        assertTrue(UpdateCheck.shouldNotify(5, m, 11))
    }

    @Test fun bestActionUrl_prefersDirectApk() {
        val m = UpdateCheck.parseManifest(validJson)!!
        assertEquals("https://github.com/rohiitgit/ony/releases/download/v0.1.42/Ony-0.1.42.apk", UpdateCheck.bestActionUrl(m))
    }

    @Test fun bestActionUrl_fallsBack() {
        assertEquals("https://example.com/release", UpdateCheck.bestActionUrl(UpdateManifest("0.1.1", 1, releaseUrl = "https://example.com/release")))
        assertEquals("https://example.com/page", UpdateCheck.bestActionUrl(UpdateManifest("0.1.1", 1, pageUrl = "https://example.com/page")))
        assertNull(UpdateCheck.bestActionUrl(UpdateManifest("0.1.1", 1)))
    }
}
