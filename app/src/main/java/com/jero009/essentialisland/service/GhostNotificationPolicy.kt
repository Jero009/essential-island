/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.jero009.essentialisland.service

/**
 * Pure decision logic for the "silent ghost copy" behavior.
 *
 * When a notification classifies as island-only the listener cancels it from the system shade.
 * With [com.jero009.essentialisland.data.SmartIslandSettings.keepSilentCopyInShade] on, the service
 * immediately re-posts a silent copy ("ghost") of the cancelled notification under its own package:
 * the notification stays visible in the system notifications panel, but the system never actively
 * displays it — no heads-up, sound or vibration (a dedicated IMPORTANCE_LOW channel without alerts).
 *
 * Lifecycle, per the user's intent:
 *  - Ghosts are NOT removed when the original app withdraws the original notification; they are a
 *    "recent/pending" record the user clears by swiping the ghost away in the shade.
 *  - A re-posted or updated original re-uses the same deterministic tag, so the system replaces the
 *    ghost instead of duplicating it.
 *  - The only automatic cleanup is the per-app cap ([MAX_GHOSTS_PER_APP] newest per source app),
 *    applied when a new ghost is posted and re-checked on listener reconnect. Ghosts survive
 *    service restarts because they live in the system's notification stream.
 *
 * Kept free of Android types so the plain-JVM unit suite (see GhostNotificationPolicyTest) covers it.
 */
object GhostNotificationPolicy {

    /** Every ghost is posted with this id; the distinct [ghostTag] tells them apart. */
    const val GHOST_NOTIFICATION_ID = 0x51A1D
    const val CHANNEL_ID = "silent_ghost_copies"

    /** Extra on a ghost carrying the key of the original notification it mirrors. */
    const val EXTRA_ORIGINAL_KEY = "com.jero009.essentialisland.extra.GHOST_ORIGINAL_KEY"
    /** Extra on a ghost carrying the package of the original app (used to rebuild the registry on reconnect). */
    const val EXTRA_SOURCE_PACKAGE = "com.jero009.essentialisland.extra.GHOST_SOURCE_PACKAGE"

    /** At most this many ghosts are kept per source app; older ones are trimmed. */
    const val MAX_GHOSTS_PER_APP = 10

    /** A live ghost the service knows about; the registry maps tag -> record. */
    data class GhostRecord(
        val tag: String,
        val sourcePackage: String,
        val originalKey: String,
        val postTime: Long
    )

    /**
     * Deterministic tag for the ghost of [sbnKey] posted by [packageName]: a re-posted or updated
     * original keeps its key, so its ghost replaces the previous one instead of stacking up.
     */
    fun ghostTag(packageName: String, sbnKey: String): String =
        "$packageName:" + Integer.toHexString(sbnKey.hashCode())

    /**
     * Ghost vs plain cancel: a ghost copy is only posted when the shade copy is suppressed at all
     * ([hideFromNotificationShade]) AND the user wants the silent copy kept. Otherwise the
     * suppression leaves no trace, exactly as before.
     */
    fun shouldPostGhost(keepSilentCopyInShade: Boolean, hideFromNotificationShade: Boolean): Boolean =
        hideFromNotificationShade && keepSilentCopyInShade

    /**
     * Which records to cancel so at most [maxPerApp] newest ghosts per source app remain
     * (ties on postTime keep the earlier record). Records of different apps are independent.
     */
    fun ghostsToTrim(records: List<GhostRecord>, maxPerApp: Int = MAX_GHOSTS_PER_APP): List<GhostRecord> {
        if (maxPerApp <= 0) return records
        return records
            .groupBy { it.sourcePackage }
            .values
            .filter { it.size > maxPerApp }
            .flatMap { group -> group.sortedByDescending { it.postTime }.drop(maxPerApp) }
    }
}
