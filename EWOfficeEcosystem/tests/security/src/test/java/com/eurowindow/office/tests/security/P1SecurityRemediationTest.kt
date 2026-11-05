package com.eurowindow.office.tests.security

import com.eurowindow.office.core.security.BroadcastIsolationPolicy
import com.eurowindow.office.core.security.EWOfflineUrlPolicy
import com.eurowindow.office.core.security.TempFileGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class P1SecurityRemediationTest {

    @Test
    fun s1_offlineUrlPolicy_allowsCoolAndAssetSchemesOnly() {
        assertTrue(EWOfflineUrlPolicy.isAllowedUrl("cool://localhost/cool/mobilesocket/soc1/open"))
        assertTrue(EWOfflineUrlPolicy.isAllowedUrl("cool://localhost/cool/media?Tag=img1"))
        assertTrue(EWOfflineUrlPolicy.isAllowedUrl("file:///android_asset/dist/cool.html?file_path=file:///data"))
        assertTrue(EWOfflineUrlPolicy.isAllowedUrl("data:image/png;base64,iVBORw0KGgo="))
        assertTrue(EWOfflineUrlPolicy.isAllowedUrl("blob:null/1234-5678"))
        assertTrue(EWOfflineUrlPolicy.isAllowedUrl("javascript:window.postMobileMessage('SAVE')"))

        // Must strictly block external network and arbitrary file/content schemes
        assertFalse(EWOfflineUrlPolicy.isAllowedUrl("http://example.com/telemetry"))
        assertFalse(EWOfflineUrlPolicy.isAllowedUrl("https://collaboraoffice.com/check-update"))
        assertFalse(EWOfflineUrlPolicy.isAllowedUrl("ws://127.0.0.1:9980/cool"))
        assertFalse(EWOfflineUrlPolicy.isAllowedUrl("wss://cloud.example.com/socket"))
        assertFalse(EWOfflineUrlPolicy.isAllowedUrl("file:///sdcard/Download/secret.docx"))
        assertFalse(EWOfflineUrlPolicy.isAllowedUrl("file:///data/data/com.eurowindow.documents/databases/ew_documents.db"))
        assertFalse(EWOfflineUrlPolicy.isAllowedUrl("file:///android_asset/../../etc/hosts"))
    }

    @Test
    fun s3_tempFileGuard_deletesEphemeralCacheAndPreservesUserSourceFiles() {
        val tempRoot = Files.createTempDirectory("ew_s3_test").toFile()
        try {
            val cacheDir = File(tempRoot, "cache").apply { mkdirs() }
            val userSourceDir = File(tempRoot, "user_storage").apply { mkdirs() }

            val userSourceDocx = File(userSourceDir, "HopDong_Eurowindow_Original.docx").apply {
                writeText("USER_OWNED_SOURCE_DOCUMENT_CONTENT")
            }
            val tempWorkingCopy = File(cacheDir, "HopDong_Eurowindow_Original.docx").apply {
                writeText("TEMP_WORKING_COPY")
            }
            val printPdf = File(cacheDir, "print.pdf").apply {
                writeText("PDF_EXPORT_CACHE")
            }
            val clipFile = File(cacheDir, "lok_clip_001.html").apply {
                writeText("<p>Clipboard fragment</p>")
            }

            val report = TempFileGuard.scrubAppCacheDirectory(
                cacheDir = cacheDir,
                explicitTempFile = tempWorkingCopy,
                protectedUserFiles = setOf(userSourceDocx)
            )

            assertFalse("Temp working copy must be deleted", tempWorkingCopy.exists())
            assertFalse("Print PDF temp artifact must be deleted", printPdf.exists())
            assertFalse("Clipboard temp artifact must be deleted", clipFile.exists())
            assertTrue("User-owned source file must NEVER be deleted", userSourceDocx.exists())
            assertEquals("USER_OWNED_SOURCE_DOCUMENT_CONTENT", userSourceDocx.readText())
            assertEquals(3, report.deletedFiles.size)
            assertTrue(report.bytesReclaimed > 0)
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun s4_broadcastAndNamespaceIsolation_verifiesZeroSharedStateBetweenApps() {
        assertTrue(BroadcastIsolationPolicy.verifyNoSharedNamespaces())
        val doc = BroadcastIsolationPolicy.DOCUMENTS_DESCRIPTOR
        val sheet = BroadcastIsolationPolicy.SHEETS_DESCRIPTOR

        assertEquals("com.eurowindow.documents", doc.applicationId)
        assertEquals("com.eurowindow.sheets", sheet.applicationId)
        assertFalse(doc.fileProviderAuthority == sheet.fileProviderAuthority)
        assertFalse(doc.clipboardAuthority == sheet.clipboardAuthority)
        assertFalse(doc.databaseName == sheet.databaseName)
        assertFalse(doc.dataStoreName == sheet.dataStoreName)
        assertFalse(doc.fontConfigCachePath == sheet.fontConfigCachePath)
    }
}
