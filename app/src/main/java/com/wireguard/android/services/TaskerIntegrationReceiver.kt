/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wireguard.android.Application
import com.wireguard.android.BuildConfig
import com.wireguard.android.model.Tunnel
import com.wireguard.android.model.TunnelManager
import timber.log.Timber

class TaskerIntegrationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || intent.action == null)
            return

        val manager = Application.tunnelManager
        val isSelfPackage = intent.`package` == BuildConfig.APPLICATION_ID
        val taskerEnabled = !Application.appPrefs.allowTaskerIntegration || Application.appPrefs.taskerIntegrationSecret.isEmpty()
        val tunnelName: String? = intent.getStringExtra(TunnelManager.TUNNEL_NAME_INTENT_EXTRA)
        val integrationSecret: String? = intent.getStringExtra(TunnelManager.INTENT_INTEGRATION_SECRET_EXTRA)

        var state: Tunnel.State? = null
        Timber.tag("IntentReceiver")
        when (intent.action) {
            "${BuildConfig.APPLICATION_ID}.SET_TUNNEL_UP" -> {
                state = Tunnel.State.UP
            }
            "${BuildConfig.APPLICATION_ID}.SET_TUNNEL_DOWN" -> {
                state = Tunnel.State.DOWN
            }
            else -> Timber.d("Invalid intent action: ${intent.action}")
        }

        if (taskerEnabled && !isSelfPackage) {
            Timber.e("Tasker integration is disabled! Not allowing tunnel state change to pass through.")
            return
        }

        if (tunnelName != null && state != null) {
            if (isSelfPackage) {
                toggleTunnelState(tunnelName, state, manager)
                return
            }
            when (integrationSecret) {
                Application.appPrefs.taskerIntegrationSecret -> toggleTunnelState(tunnelName, state, manager)
                else -> Timber.e("Intent integration secret mis-match! Exiting...")
            }
        } else if (tunnelName == null) {
            Timber.d("Intent parameter ${TunnelManager.TUNNEL_NAME_INTENT_EXTRA} not set!")
        }
    }

    private fun toggleTunnelState(tunnelName: String, state: Tunnel.State, manager: TunnelManager) {
        Timber.d("Setting $tunnelName's state to $state")
        manager.getTunnels().thenAccept { tunnels ->
            val tunnel = tunnels[tunnelName]
            tunnel?.let {
                manager.setTunnelState(it, state)
            }
        }
    }
}
