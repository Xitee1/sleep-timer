package dev.xitee.sleeptimer.di

import android.content.ComponentName
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.xitee.sleeptimer.core.service.screen.DeviceAdminComponent
import dev.xitee.sleeptimer.receiver.SleepTimerDeviceAdminReceiver

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Single source of truth for the device-admin receiver's ComponentName. The
     * receiver class lives in this module, so lower modules must receive the
     * component via injection instead of hard-coding the class name.
     */
    @Provides
    @DeviceAdminComponent
    fun provideDeviceAdminComponent(@ApplicationContext context: Context): ComponentName =
        ComponentName(context, SleepTimerDeviceAdminReceiver::class.java)
}
