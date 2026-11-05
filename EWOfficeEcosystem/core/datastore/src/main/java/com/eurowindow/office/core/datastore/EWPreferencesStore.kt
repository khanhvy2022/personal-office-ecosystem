package com.eurowindow.office.core.datastore

import android.content.Context
import android.content.SharedPreferences

/**
 * Isolated per-application preferences store (`ew_documents_prefs` vs `ew_sheets_prefs`).
 */
class EWPreferencesStore(context: Context, val storeName: String) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(storeName, Context.MODE_PRIVATE)

    var isSpellcheckDisabledByDefault: Boolean
        get() = prefs.getBoolean("spellcheck_disabled_default", true)
        set(value) = prefs.edit().putBoolean("spellcheck_disabled_default", value).apply()

    var lastValidatedCorpusFile: String?
        get() = prefs.getString("last_validated_corpus", null)
        set(value) = prefs.edit().putString("last_validated_corpus", value).apply()
}
