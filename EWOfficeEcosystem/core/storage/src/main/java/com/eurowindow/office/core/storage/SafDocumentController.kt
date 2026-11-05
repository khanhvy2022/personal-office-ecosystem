package com.eurowindow.office.core.storage

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

/**
 * S2 Remediation: Pure Android Storage Access Framework (SAF) controller.
 * Requires zero MANAGE_EXTERNAL_STORAGE / READ_EXTERNAL_STORAGE / WRITE_EXTERNAL_STORAGE permissions.
 * Uses ACTION_OPEN_DOCUMENT, ACTION_CREATE_DOCUMENT, and takePersistableUriPermission.
 */
object SafDocumentController {

    const val MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val MIME_CSV = "text/csv"
    const val MIME_PDF = "application/pdf"

    fun buildOpenDocumentIntent(mimeTypes: Array<String>): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = if (mimeTypes.size == 1) mimeTypes[0] else "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
    }

    fun buildCreateDocumentIntent(mimeType: String, suggestedName: String): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, suggestedName)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
    }

    fun persistUriPermissionIfPossible(contentResolver: ContentResolver, uri: Uri): Boolean {
        return try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flags)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun resolveDisplayName(contentResolver: ContentResolver, uri: Uri, fallback: String): String {
        if (uri.scheme == "file") {
            return uri.lastPathSegment ?: fallback
        }
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else fallback
                } else {
                    fallback
                }
            } ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    /**
     * Materializes a bundled asset (e.g., blank template or test corpus file) into app-private filesDir/workspace
     * so it can be edited via a file:// or FileProvider URI without any external storage permissions.
     */
    fun materializeAssetToWorkspace(context: Context, assetPath: String, targetFileName: String): File {
        val workspaceDir = File(context.filesDir, "workspace").apply { mkdirs() }
        val outFile = File(workspaceDir, targetFileName)
        context.assets.open(assetPath).use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
                output.flush()
            }
        }
        return outFile
    }

    /**
     * Initializes a newly created SAF document URI with the contents of a bundled template asset.
     */
    fun initializeSafDocumentFromAsset(context: Context, targetUri: Uri, templateAssetPath: String): Boolean {
        return try {
            context.assets.open(templateAssetPath).use { input ->
                context.contentResolver.openOutputStream(targetUri, "wt")?.use { output ->
                    input.copyTo(output)
                    output.flush()
                } ?: return false
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
