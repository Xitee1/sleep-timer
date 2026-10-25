package dev.xitee.sleeptimer.core.service.media

import android.service.notification.NotificationListenerService

/**
 * Exists only so the user can grant notification access, which unlocks
 * `MediaSessionManager.getActiveSessions` for reading the playing song's length.
 * No callbacks are overridden — notifications are never read.
 *
 * Declared in `:app`'s manifest and instantiated by the system, so it stays Hilt-free.
 */
class MediaNotificationListener : NotificationListenerService()
