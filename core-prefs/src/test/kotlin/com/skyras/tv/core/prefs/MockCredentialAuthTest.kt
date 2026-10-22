package com.skyras.tv.core.prefs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MockCredentialAuthTest {

    @Test
    fun rejectsBlankUsername() {
        val result = MockCredentialAuth.authenticate("  ", "secret", null)
        assertTrue(result is SignInOutcome.Failure)
    }

    @Test
    fun rejectsShortPassword() {
        val result = MockCredentialAuth.authenticate("travis", "ab", null)
        assertTrue(result is SignInOutcome.Failure)
    }

    @Test
    fun demoUserWithoutUrlUsesBuiltInCatalog() {
        val result = MockCredentialAuth.authenticate("demo", "demo", "  ")
        val success = result as SignInOutcome.Success
        assertEquals("demo", success.username)
        assertTrue(success.useDemoCatalog)
    }

    @Test
    fun customUserWithPlaylistUrlSucceeds() {
        val result = MockCredentialAuth.authenticate(
            username = "travis",
            password = "skyras",
            playlistUrl = "https://example.com/live.m3u"
        )
        val success = result as SignInOutcome.Success
        assertEquals("travis", success.username)
        assertEquals("https://example.com/live.m3u", success.m3uUrl)
        assertEquals(false, success.useDemoCatalog)
    }

    @Test
    fun rejectsNonHttpPlaylist() {
        val result = MockCredentialAuth.authenticate("travis", "skyras", "ftp://nope")
        assertTrue(result is SignInOutcome.Failure)
    }
}
