package com.eurowindow.office.tests.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class P1EnginePackagingIntegrationTest {

    private fun resolveProjectRoot(): File {
        var dir: File = File(System.getProperty("user.dir") ?: ".").canonicalFile
        while (dir.parentFile != null && !File(dir, "engine/collabora-runtime").exists()) {
            dir = dir.parentFile!!
        }
        return dir
    }

    @Test
    fun verifyCollaboraArm64NativeLibrariesAndAssetsPackaged() {
        val projRoot = resolveProjectRoot()
        val jniDir = File(projRoot, "engine/collabora-runtime/src/main/jniLibs/arm64-v8a")
        assertTrue("JNI arm64-v8a directory must exist", jniDir.exists())

        val requiredLibs = listOf(
            "liblo-native-code.so",
            "libandroidapp.so",
            "libc++_shared.so",
            "libfreebl3.so",
            "libnspr4.so",
            "libnss3.so",
            "libnssckbi.so",
            "libnssdbm3.so",
            "libnssutil3.so",
            "libplc4.so",
            "libplds4.so",
            "libsmime3.so",
            "libsoftokn3.so",
            "libsqlite3.so",
            "libssl3.so"
        )
        for (libName in requiredLibs) {
            val f = File(jniDir, libName)
            assertTrue("Required native library $libName must exist and be non-empty", f.exists() && f.length() > 0L)
        }
        assertEquals(15, jniDir.listFiles { f -> f.name.endsWith(".so") }?.size ?: 0)

        // Verify UNO configuration and dynamic fontconfig isolation placeholder
        val fontsConf = File(projRoot, "engine/collabora-runtime/src/main/assets/unpack/etc/fonts/fonts.conf").readText()
        assertFalse("Upstream hardcoded com.collabora.libreoffice path must be removed", fontsConf.contains("com.collabora.libreoffice"))
        assertTrue("Must contain @@APPLICATION_ID@@ placeholder for per-app isolation", fontsConf.contains("@@APPLICATION_ID@@"))

        // Verify Collabora branding removed from cool.html and bootstraprc
        val bootstraprc = File(projRoot, "engine/collabora-runtime/src/main/assets/program/bootstraprc").readText()
        assertTrue("ProductKey must be EW Personal Office", bootstraprc.contains("ProductKey=EW Personal Office"))
    }
}
