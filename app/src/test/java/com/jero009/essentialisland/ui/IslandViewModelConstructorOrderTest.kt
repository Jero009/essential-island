/*
 * Essential Island (2026)
 * Based on SmartIsland © Animesh Gupta — github.com/agupta07505 (GPL-3.0)
 * Do not remove or alter this notice.
 */

package com.jero009.essentialisland.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the IslandViewModel property-order bug: the init-block collectors launch on Main.immediate and run
 * while the constructor is still executing, so any flow they combine must be declared above init.
 * `appsLauncherOpen` used to sit below init, and the empty-island guard and auto-collapse-pause collectors
 * died on every startup with "NullPointerException: Flow.collect on a null object reference".
 */
class IslandViewModelConstructorOrderTest {

    @Test
    fun appsLauncherOpenIsDeclaredBeforeTheInitBlock() {
        val source = locate("IslandViewModel.kt")
        val decl = source.indexOfFirst { it.contains("val appsLauncherOpen = MutableStateFlow(false)") }
        val init = source.indexOfFirst { it.trim() == "init {" }
        assertTrue(
            "IslandViewModel.kt not found from ${File(".").absolutePath}",
            decl >= 0 && init >= 0
        )
        assertTrue(
            "appsLauncherOpen must be declared before init { (decl line ${decl + 1}, init line ${init + 1}): " +
                "init collectors run on Main.immediate during construction and combine it while it is still null",
            decl < init
        )
    }

    private fun locate(name: String): List<String> {
        val roots = generateSequence(File(".").absoluteFile) { it.parentFile }.take(6)
        for (root in roots) {
            for (prefix in listOf("src/main/java", "app/src/main/java")) {
                val candidate = File(File(root, prefix), "com/jero009/essentialisland/ui/$name")
                if (candidate.isFile) return candidate.readText().lines()
            }
        }
        return emptyList()
    }
}
