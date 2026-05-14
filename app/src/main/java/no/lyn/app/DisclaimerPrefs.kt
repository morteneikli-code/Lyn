package no.lyn.app

import android.content.Context
import androidx.core.content.edit

/**
 * Tracks whether the user has acknowledged the one-time disclaimer screen.
 *
 * The key is versioned (`_v1`) so we can re-trigger acceptance if we later make a
 * substantive change to the wording — e.g. broaden the scope or sharpen the
 * liability language. Bump to `_v2` and existing users will see the new screen once.
 */
object DisclaimerPrefs {
    private const val PREFS = "disclaimer_prefs"
    private const val KEY_ACCEPTED = "disclaimer_accepted_v1"

    fun isAccepted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ACCEPTED, false)

    fun accept(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_ACCEPTED, true) }
    }
}
