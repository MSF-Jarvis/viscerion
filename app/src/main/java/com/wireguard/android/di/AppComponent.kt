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
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
annotation class ApplicationHandler

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
    @get:JvmStatic
    val executor: Executor = AsyncTask.SERIAL_EXECUTOR

    @get:ApplicationHandler
    @get:Reusable
    @get:Provides
    @get:JvmStatic
    val handler: Handler = Handler(Looper.getMainLooper())

    @Reusable
    @Provides
    @JvmStatic
    fun getRootShell(context: Context): RootShell = RootShell(context)

    @Reusable
    @Provides
    @JvmStatic
    fun getSharedPrefs(context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    @Reusable
    @Provides
    @JvmStatic
    fun getToolsInstaller(
        context: Context,
        rootShell: RootShell
    ): ToolsInstaller = ToolsInstaller(context, rootShell)

    @Reusable
    @Provides
    @JvmStatic
    fun getConfigStore(context: Context): ConfigStore = FileConfigStore(context, context.filesDir)

    @Reusable
    @Provides
    @JvmStatic
    fun getTunnelManager(
        context: Context,
        configStore: ConfigStore,
        prefs: ApplicationPreferences
    ): TunnelManager = TunnelManager(context, configStore, prefs)

    @Reusable
    @Provides
    @JvmStatic
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
    @JvmStatic
    fun getBackendType(backend: Backend): Class<Backend> = backend.javaClass

    @Reusable
    @Provides
    @JvmStatic
    fun getPreferences(sharedPrefs: SharedPreferences): ApplicationPreferences = ApplicationPreferences(sharedPrefs)

    @Reusable
    @Provides
    @JvmStatic
    fun getAsyncWorker(executor: Executor, @ApplicationHandler handler: Handler): AsyncWorker =
        AsyncWorker(executor, handler)

    @Singleton
    @Provides
    @JvmStatic
    fun getBackendAsync(backend: Backend): BackendAsync {
        val backendAsync = BackendAsync()
        backendAsync.complete(backend)
        return backendAsync
    }
}
