/*
 * Copyright © 2018 Samuel Holland <samuel@sholland.org>
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.util.ExceptionLoggers
import timber.log.Timber

class BootShutdownReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Timber.tag(TAG)
        if (intent.action == null)
            return
        Application.backendAsync.thenAccept { backend ->
            if (backend !is WgQuickBackend)
                return@thenAccept
            val action = intent.action
            val tunnelManager = Application.getTunnelManager()
            if (Intent.ACTION_BOOT_COMPLETED == action) {
                Timber.i("Broadcast receiver restoring state (boot)")
                tunnelManager.restoreState(false).whenComplete(ExceptionLoggers.D)
            } else if (Intent.ACTION_SHUTDOWN == action) {
                Timber.i("Broadcast receiver saving state (shutdown)")
                tunnelManager.saveState()
            }
        }
    }

    companion object {
        private val TAG = "WireGuard/" + BootShutdownReceiver::class.java.simpleName
    }
}
