package dev.xitee.sleeptimer.core.service.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    // MediaVolumeController, TimerNotificationManager, and ScreenLockHelper
    // are all @Inject constructable @Singletons, so no explicit @Provides needed.
}
