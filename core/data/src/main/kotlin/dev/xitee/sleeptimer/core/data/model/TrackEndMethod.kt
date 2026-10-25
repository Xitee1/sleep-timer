package dev.xitee.sleeptimer.core.data.model

/**
 * How the remaining time of the playing song is read for "let the song finish"
 * (see [UserSettings.trackEndMaxWaitMinutes]).
 *
 * - [None] — no method chosen yet; the feature stays inactive and the timer stops
 *   playback right away. Default, so no permission is requested up front.
 * - [NotificationAccess] — `MediaSessionManager.getActiveSessions` through the app's
 *   notification listener, which the user enables in the system settings.
 * - [Shizuku] — the same API queried inside the Shizuku shell process, whose shell
 *   uid holds `MEDIA_CONTENT_CONTROL`.
 */
enum class TrackEndMethod {
    None,
    NotificationAccess,
    Shizuku,
    ;

    companion object {
        val Default: TrackEndMethod = None

        fun fromStorage(value: String?): TrackEndMethod =
            entries.firstOrNull { it.name == value } ?: Default
    }
}
