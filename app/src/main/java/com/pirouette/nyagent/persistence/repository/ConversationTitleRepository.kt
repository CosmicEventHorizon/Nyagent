package com.pirouette.nyagent.persistence.repository

import android.content.Context

/** Persists conversation display titles separately from GUID-keyed story data. */
class ConversationTitleRepository(context: Context) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(conversationId: String): String? =
        preferences.getString(conversationId, null)?.takeIf { it.isNotBlank() }

    fun save(conversationId: String, title: String) {
        preferences.edit().putString(conversationId, title).apply()
    }

    fun delete(conversationId: String) {
        preferences.edit().remove(conversationId).apply()
    }

    private companion object {
        const val PREFS_NAME = "conversation_titles"
    }
}
