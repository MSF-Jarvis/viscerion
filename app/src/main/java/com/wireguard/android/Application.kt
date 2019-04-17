/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.configStore.FileConfigStore
import com.wireguard.android.di.applicationModules
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.AsyncWorker
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.updateAppTheme
import java9.util.concurrent.CompletableFuture
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.inject
import org.koin.core.logger.Level
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference

class Application : android.app.Application() {
    private lateinit var rootShell: RootShell
    private lateinit var tunnelManager: TunnelManager
    private var backend: Backend? = null
    private val futureBackend = CompletableFuture<Backend>()

    init {
        weakSelf = WeakReference(this)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(
                TunnelManager.NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_wgquick_title),
                NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationChannel.description = getString(R.string.notification_channel_wgquick_desc)
        notificationChannel.setShowBadge(false)
        notificationChannel.setSound(null, null)
        notificationChannel.enableVibration(false)
        notificationManager.createNotificationChannel(notificationChannel)
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(level = Level.DEBUG)
            androidContext(this@Application)
            modules(applicationModules)
        }

        if (BuildConfig.DEBUG)
            Timber.plant(Timber.DebugTree())

        updateAppTheme()

        tunnelManager = TunnelManager(FileConfigStore(applicationContext))

        val asyncWorker by inject<AsyncWorker>()
        asyncWorker.supplyAsync { backend }.thenAccept { backend ->
            futureBackend.complete(backend)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel()
    }

    companion object : KoinComponent {

        private lateinit var weakSelf: WeakReference<Application>
        val backendAsync by lazy { get().futureBackend }
        val rootShell by inject<RootShell>()
        val appPrefs by inject<ApplicationPreferences>()

        fun get(): Application {
            return weakSelf.get() as Application
        }

        val backend: Backend
            get() {
                val app = get()
                synchronized(app.futureBackend) {
                    if (app.backend == null) {
                        var backend: Backend? = null
                        if (File("/sys/module/wireguard").exists()) {
                            try {
                                if (appPrefs.forceUserspaceBackend)
                                    throw Exception("Forcing userspace backend on user request.")
                                app.rootShell.start()
                                backend = WgQuickBackend(app.applicationContext)
                            } catch (ignored: Exception) {
                            }
                        }
                        if (backend == null)
                            backend = GoBackend(app.applicationContext)
                        app.backend = backend
                    }
                }
                return app.backend as Backend
            }
    }
}
