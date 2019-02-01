/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.util.ExceptionLoggers
import com.wireguard.android.util.thenAccept
import timber.log.Timber

class BootShutdownReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        intent.action ?: return
        Application.backendAsync.thenAccept { backend ->
            if (backend !is WgQuickBackend)
                return
            val action = intent.action
            val tunnelManager = Application.tunnelManager
            when (action) {
                Intent.ACTION_BOOT_COMPLETED -> {
                    Timber.i("Broadcast receiver restoring state (boot)")
                    tunnelManager.restoreState(false).whenComplete(ExceptionLoggers.D)
                }
                Intent.ACTION_SHUTDOWN -> {
                    Timber.i("Broadcast receiver saving state (shutdown)")
                    tunnelManager.saveState()
                }
                else -> {
                    Timber.i("Invalid intent action received.")
                }
            }
        }
    }
}
