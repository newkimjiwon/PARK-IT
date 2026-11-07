package com.nangmanroad.core.sync

import android.content.Context

/** Keeps this device's sync session across restarts. */
class SessionStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("sync_session", Context.MODE_PRIVATE)

    fun load(): Session? = prefs.getString(KEY_SESSION, null)?.let { runCatching { Api.json.decodeFromString(Session.serializer(), it) }.getOrNull() }

    fun save(session: Session?) {
        prefs.edit().apply {
            if (session == null) remove(KEY_SESSION) else putString(KEY_SESSION, Api.json.encodeToString(Session.serializer(), session))
        }.apply()
    }

    private companion object {
        const val KEY_SESSION = "session"
    }
}
