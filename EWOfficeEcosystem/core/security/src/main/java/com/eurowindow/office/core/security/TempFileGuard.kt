package com.eurowindow.office.core.security

import java.io.File

/**
 * S3 Remediation: Tracks and deterministically scrubs temporary working copies, PDF print artifacts,
 * and clipboard stage files inside the application's private cache directory while guaranteeing
 * that user-owned source files outside cacheDir are never deleted.
 */
object TempFileGuard {

    data class CleanupReport(
        val deletedFiles: List<String>,
        val retainedFiles: List<String>,
        val bytesReclaimed: Long
    )

    @JvmStatic
    fun scrubAppCacheDirectory(
        cacheDir: File,
        explicitTempFile: File? = null,
        protectedUserFiles: Set<File> = emptySet()
    ): CleanupReport {
        val deleted = mutableListOf<String>()
        val retained = mutableListOf<String>()
        var bytesFreed = 0L

        val canonicalCacheRoot = cacheDir.canonicalFile
        val canonicalProtected = protectedUserFiles.map { it.canonicalFile }.toSet()

        fun tryDelete(target: File) {
            if (!target.exists() || !target.isFile) return
            val canonicalTarget = target.canonicalFile
            // Never delete a protected user file or anything outside cacheDir
            if (canonicalTarget in canonicalProtected) {
                retained.add(canonicalTarget.name)
                return
            }
            if (!canonicalTarget.path.startsWith(canonicalCacheRoot.path)) {
                retained.add(canonicalTarget.name)
                return
            }
            val len = canonicalTarget.length()
            if (canonicalTarget.delete()) {
                deleted.add(canonicalTarget.name)
                bytesFreed += len
            } else {
                retained.add(canonicalTarget.name)
            }
        }

        if (explicitTempFile != null) {
            tryDelete(explicitTempFile)
        }

        val children = cacheDir.listFiles() ?: emptyArray()
        for (child in children) {
            if (child.isFile) {
                val name = child.name
                val isEphemeralArtifact =
                    name == "print.pdf" ||
                    name.startsWith("lok_clip_") ||
                    name.startsWith("ew_work_") ||
                    name.endsWith(".tmp") ||
                    name.endsWith(".docx") ||
                    name.endsWith(".xlsx") ||
                    name.endsWith(".ods") ||
                    name.endsWith(".odt") ||
                    name.endsWith(".csv") ||
                    name.endsWith(".pdf")
                if (isEphemeralArtifact) {
                    tryDelete(child)
                } else {
                    retained.add(name)
                }
            }
        }

        return CleanupReport(
            deletedFiles = deleted.sorted(),
            retainedFiles = retained.sorted(),
            bytesReclaimed = bytesFreed
        )
    }
}
