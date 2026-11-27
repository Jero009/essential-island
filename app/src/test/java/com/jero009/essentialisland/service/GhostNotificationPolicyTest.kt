/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.jero009.essentialisland.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the "silent ghost copy" decision logic (ghost vs plain cancel, deterministic
 * dedupe tags, per-app cap) — no Android types involved, matching the plain unit-test style of the
 * app/src/test suite.
 */
class GhostNotificationPolicyTest {

    // ---- ghost vs plain cancel ----

    @Test
    fun testGhostPostedWhenBothTogglesOn() {
        assertTrue(GhostNotificationPolicy.shouldPostGhost(keepSilentCopyInShade = true, hideFromNotificationShade = true))
    }

    @Test
    fun testNoGhostWhenKeepSilentCopyOffIsPlainCancel() {
        // The suppression must leave no trace, exactly the pre-feature behavior.
        assertFalse(GhostNotificationPolicy.shouldPostGhost(keepSilentCopyInShade = false, hideFromNotificationShade = true))
    }

    @Test
    fun testNoGhostWhenShadeNotHidden() {
        // Nothing is suppressed at all, so there is nothing to mirror.
        assertFalse(GhostNotificationPolicy.shouldPostGhost(keepSilentCopyInShade = true, hideFromNotificationShade = false))
    }

    @Test
    fun testNoGhostWhenBothOff() {
        assertFalse(GhostNotificationPolicy.shouldPostGhost(keepSilentCopyInShade = false, hideFromNotificationShade = false))
    }

    // ---- deterministic dedupe tag ----

    @Test
    fun testTagIsDeterministicForSameKey() {
        val a = GhostNotificationPolicy.ghostTag("com.whatsapp", "0|com.whatsapp|1|null|1000")
        val b = GhostNotificationPolicy.ghostTag("com.whatsapp", "0|com.whatsapp|1|null|1000")
        assertEquals(a, b)
    }

    @Test
    fun testTagDiffersForDifferentKeys() {
        val a = GhostNotificationPolicy.ghostTag("com.whatsapp", "0|com.whatsapp|1|null|1000")
        val b = GhostNotificationPolicy.ghostTag("com.whatsapp", "0|com.whatsapp|2|null|1001")
        assertNotEquals(a, b)
    }

    @Test
    fun testTagCarriesSourcePackagePrefix() {
        val tag = GhostNotificationPolicy.ghostTag("com.example.chat", "key")
        assertTrue(tag.startsWith("com.example.chat:"))
    }

    @Test
    fun testRePostedOriginalReplacesInsteadOfDuplicating() {
        // A re-posted/updated notification keeps its key, so its ghost tag is stable: the system
        // notify() call with the same tag+id replaces the previous ghost.
        val original = GhostNotificationPolicy.ghostTag("com.example.chat", "0|com.example.chat|5|null|42")
        val reposted = GhostNotificationPolicy.ghostTag("com.example.chat", "0|com.example.chat|5|null|42")
        assertEquals(original, reposted)
    }

    // ---- per-app cap ----

    private fun records(pkg: String, count: Int, firstPostTime: Long = 1000L) =
        (0 until count).map { i ->
            GhostNotificationPolicy.GhostRecord(
                tag = GhostNotificationPolicy.ghostTag(pkg, "key$i"),
                sourcePackage = pkg,
                originalKey = "key$i",
                postTime = firstPostTime + i
            )
        }

    @Test
    fun testUnderTheCapNothingIsTrimmed() {
        val recs = records("com.whatsapp", GhostNotificationPolicy.MAX_GHOSTS_PER_APP)
        assertTrue(GhostNotificationPolicy.ghostsToTrim(recs).isEmpty())
    }

    @Test
    fun testOverTheCapOnlyTheOldestAreTrimmed() {
        val recs = records("com.whatsapp", GhostNotificationPolicy.MAX_GHOSTS_PER_APP + 3)
        val trimmed = GhostNotificationPolicy.ghostsToTrim(recs)
        assertEquals(3, trimmed.size)
        // The three oldest (smallest postTime) go; the newest ten stay.
        val trimmedTimes = trimmed.map { it.postTime }.sorted()
        assertEquals(listOf(1000L, 1001L, 1002L), trimmedTimes)
        val kept = recs - trimmed.toSet()
        assertEquals(GhostNotificationPolicy.MAX_GHOSTS_PER_APP, kept.size)
        assertTrue(kept.all { it.postTime > 1002L })
    }

    @Test
    fun testCapsArePerApp() {
        val recs = records("com.whatsapp", 12) + records("com.telegram", 12) + records("com.mail", 3)
        val trimmed = GhostNotificationPolicy.ghostsToTrim(recs)
        assertEquals(4, trimmed.size)
        assertEquals(2, trimmed.count { it.sourcePackage == "com.whatsapp" })
        assertEquals(2, trimmed.count { it.sourcePackage == "com.telegram" })
        assertEquals(0, trimmed.count { it.sourcePackage == "com.mail" })
    }

    @Test
    fun testEmptyRegistryTrimsNothing() {
        assertTrue(GhostNotificationPolicy.ghostsToTrim(emptyList()).isEmpty())
    }
}
