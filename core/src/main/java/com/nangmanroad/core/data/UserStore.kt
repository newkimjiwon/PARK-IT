package com.nangmanroad.core.data

import android.content.Context

/**
 * The signed-in user's own state, kept on the head unit until account sync exists.
 * "Phone outbox" holds the spots and products the driver asked to open on the phone app.
 */
class UserStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("user_state", Context.MODE_PRIVATE)

    var savedSpots: Set<String>
        get() = read(KEY_SAVED)
        set(value) = write(KEY_SAVED, value)

    var likedSpots: Set<String>
        get() = read(KEY_LIKED)
        set(value) = write(KEY_LIKED, value)

    var followedAuthors: Set<String>
        get() = prefs.getStringSet(KEY_FOLLOWING, null)?.toSet() ?: DEFAULT_FOLLOWING
        set(value) = write(KEY_FOLLOWING, value)

    var helpfulTips: Set<String>
        get() = read(KEY_HELPFUL)
        set(value) = write(KEY_HELPFUL, value)

    var phoneOutbox: Set<String>
        get() = read(KEY_OUTBOX)
        set(value) = write(KEY_OUTBOX, value)

    private fun read(key: String): Set<String> = prefs.getStringSet(key, null)?.toSet() ?: emptySet()

    private fun write(key: String, value: Set<String>) {
        prefs.edit().putStringSet(key, value).apply()
    }

    private companion object {
        const val KEY_SAVED = "saved_spots"
        const val KEY_LIKED = "liked_spots"
        const val KEY_FOLLOWING = "followed_authors"
        const val KEY_HELPFUL = "helpful_tips"
        const val KEY_OUTBOX = "phone_outbox"

        /** A new account starts out following one creator so the 팔로잉 tab is not empty. */
        val DEFAULT_FOLLOWING = setOf("u-romance")
    }
}
