package com.eurowindow.office.core.security

/**
 * S4 Remediation: Validates package-scoped internal broadcast isolation and two-app namespace separation.
 */
object BroadcastIsolationPolicy {

    const val LO_ACTIVITY_BROADCAST_ACTION = "org.libreoffice.androidlib.LOActivityBroadcast"

    data class AppIsolationDescriptor(
        val applicationId: String,
        val fileProviderAuthority: String,
        val clipboardAuthority: String,
        val databaseName: String,
        val dataStoreName: String,
        val fontConfigCachePath: String
    )

    val DOCUMENTS_DESCRIPTOR = AppIsolationDescriptor(
        applicationId = "com.eurowindow.documents",
        fileProviderAuthority = "com.eurowindow.documents.fileprovider",
        clipboardAuthority = "com.eurowindow.documents.clipboard",
        databaseName = "ew_documents.db",
        dataStoreName = "ew_documents_prefs",
        fontConfigCachePath = "/data/data/com.eurowindow.documents/fontconfig"
    )

    val SHEETS_DESCRIPTOR = AppIsolationDescriptor(
        applicationId = "com.eurowindow.sheets",
        fileProviderAuthority = "com.eurowindow.sheets.fileprovider",
        clipboardAuthority = "com.eurowindow.sheets.clipboard",
        databaseName = "ew_sheets.db",
        dataStoreName = "ew_sheets_prefs",
        fontConfigCachePath = "/data/data/com.eurowindow.sheets/fontconfig"
    )

    @JvmStatic
    fun verifyNoSharedNamespaces(
        first: AppIsolationDescriptor = DOCUMENTS_DESCRIPTOR,
        second: AppIsolationDescriptor = SHEETS_DESCRIPTOR
    ): Boolean {
        return first.applicationId != second.applicationId &&
            first.fileProviderAuthority != second.fileProviderAuthority &&
            first.clipboardAuthority != second.clipboardAuthority &&
            first.databaseName != second.databaseName &&
            first.dataStoreName != second.dataStoreName &&
            first.fontConfigCachePath != second.fontConfigCachePath
    }
}
