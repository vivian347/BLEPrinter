package com.kviv.ble.di

import android.content.Context
import com.kviv.ble.data.AndroidBleController
import com.kviv.ble.domain.BleController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {
    @Provides
    @Singleton
    fun provideBleController(@ApplicationContext context: Context): BleController {
        return AndroidBleController(context)
    }
}