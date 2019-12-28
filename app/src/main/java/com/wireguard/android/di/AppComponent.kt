/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.di

import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.configStore.ConfigStore
import com.wireguard.android.configStore.FileConfigStore
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.AsyncWorker
import com.wireguard.android.util.BackendAsync
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.ToolsInstaller
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.Reusable
import java.io.File
import java.util.concurrent.Executor
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class])
interface AppComponent {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance applicationContext: Context): AppComponent
    }

    val backend: Backend
    val backendAsync: BackendAsync
    val asyncWorker: AsyncWorker
    val backendType: Class<Backend>
    val toolsInstaller: ToolsInstaller
    val tunnelManager: TunnelManager
    val rootShell: RootShell
    val preferences: ApplicationPreferences
}

@Module
object ApplicationModule {
    @get:Reusable
    @get:Provides
    val executor: Executor = AsyncTask.SERIAL_EXECUTOR

    @get:Reusable
    @get:Provides
    val handler: Handler = Handler(Looper.getMainLooper())

    @Reusable
    @Provides
    fun getSharedPrefs(context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    @Reusable
    @Provides
    fun getConfigStore(context: Context): ConfigStore = FileConfigStore(context, context.filesDir)

    @Reusable
    @Provides
    fun getBackend(
        context: Context,
        rootShell: RootShell,
        toolsInstaller: ToolsInstaller,
        preferences: ApplicationPreferences
    ): Backend {
        return if (File("/sys/module/wireguard").exists() && !preferences.forceUserspaceBackend) WgQuickBackend(
            context,
            toolsInstaller,
            rootShell
        ) else GoBackend(context, preferences)
    }

    @Reusable
    @Provides
    fun getBackendType(backend: Backend): Class<Backend> = backend.javaClass

    @Singleton
    @Provides
    fun getBackendAsync(backend: Backend): BackendAsync {
        val backendAsync = BackendAsync()
        backendAsync.complete(backend)
        return backendAsync
    }
}
