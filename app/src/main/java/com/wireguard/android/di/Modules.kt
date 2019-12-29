/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.di

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import com.wireguard.android.configStore.ConfigStore
import com.wireguard.android.configStore.FileConfigStore
import com.wireguard.android.di.factory.BackendFactory
import com.wireguard.android.di.factory.CompletableBackendFactory
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.AsyncWorker
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.ToolsInstaller
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single<ConfigStore> { FileConfigStore(androidContext(), androidContext().filesDir) }
    single { AsyncWorker(AsyncTask.SERIAL_EXECUTOR, Handler(Looper.getMainLooper())) }
    single { RootShell(androidContext()) }
    single { PreferenceManager.getDefaultSharedPreferences(androidContext()) }
    single { ApplicationPreferences(get()) }
    single { TunnelManager(get(), get(), androidContext(), get(), get()) }
    single { BackendFactory.getBackend(androidContext(), get(), get(), get()) }
    single { CompletableBackendFactory.getBackendAsync(get()) }
    single { ToolsInstaller(androidContext(), get()) }
}
