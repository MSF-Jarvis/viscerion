/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.di

import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import com.wireguard.android.Application
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
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.Reusable
import java.io.File
import java.util.concurrent.Executor
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
annotation class ApplicationContext

@Qualifier
annotation class ApplicationHandler

@Singleton
@Component(modules = [ApplicationModule::class])
interface AppComponent {
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
class ApplicationModule(application: Application) {
    @get:ApplicationContext
    @get:Singleton
    @get:Provides
    val context: Context = application.applicationContext

    @get:Reusable
    @get:Provides
    val executor: Executor = AsyncTask.SERIAL_EXECUTOR

    @get:ApplicationHandler
    @get:Reusable
    @get:Provides
    val handler: Handler = Handler(Looper.getMainLooper())

    @get:Reusable
    @get:Provides
    val rootShell: RootShell = RootShell(context)

    @get:Reusable
    @get:Provides
    val toolsInstaller: ToolsInstaller = ToolsInstaller(context, rootShell)

    @get:Reusable
    @get:Provides
    val tunnelManager: TunnelManager = TunnelManager(context, getConfigStore(context), getPreferences(context))

    @Reusable
    @Provides
    fun getBackend(
        @ApplicationContext context: Context,
        rootShell: RootShell,
        toolsInstaller: ToolsInstaller,
        preferences: ApplicationPreferences
    ): Backend {
        return if (File("/sys/module/wireguard").exists() && !preferences.forceUserspaceBackend) WgQuickBackend(
            context,
            rootShell,
            toolsInstaller
        ) else GoBackend(context, preferences)
    }

    @Reusable
    @Provides
    fun getBackendType(backend: Backend): Class<Backend> = backend.javaClass

    @Reusable
    @Provides
    fun getConfigStore(@ApplicationContext context: Context): ConfigStore = FileConfigStore(context)

    @Reusable
    @Provides
    fun getPreferences(@ApplicationContext context: Context): ApplicationPreferences = ApplicationPreferences(context)

    @Reusable
    @Provides
    fun getAsyncWorker(executor: Executor, @ApplicationHandler handler: Handler): AsyncWorker =
        AsyncWorker(executor, handler)

    @Singleton
    @Provides
    fun getBackendAsync(backend: Backend): BackendAsync {
        val backendAsync = BackendAsync()
        backendAsync.complete(backend)
        return backendAsync
    }
}
