package dev.xitee.sleeptimer.feature.timer.widgetconfig

import javax.inject.Qualifier

/**
 * Qualifies the [android.content.ComponentName] of the app's AppWidgetProvider.
 * The provider class lives in :app, so this module receives the component via
 * injection instead of hard-coding the class name — same pattern as
 * `@DeviceAdminComponent` in :core:service.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WidgetProviderComponent
