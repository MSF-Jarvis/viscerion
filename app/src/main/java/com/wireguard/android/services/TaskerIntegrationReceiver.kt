/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.services

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import com.wireguard.android.BuildConfig
import com.wireguard.android.di.getInjector
import com.wireguard.android.model.Tunnel
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.providers.OneTapWidget
import com.wireguard.android.util.ApplicationPreferences
import javax.inject.Inject
import timber.log.Timber

class TaskerIntegrationReceiver : BroadcastReceiver() {
    @Inject lateinit var manager: TunnelManager
    @Inject lateinit var prefs: ApplicationPreferences
    @Inject lateinit var handler: Handler

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.action == null) {
            return
        }
        getInjector(context).inject(this)

        if (intent.action == ACTION_FIRE_SETTING) {
            intent.action = intent.getStringExtra(TunnelManager.TUNNEL_STATE_INTENT_EXTRA)
        }

        val isSelfPackage = intent.`package` == BuildConfig.APPLICATION_ID
        val taskerEnabled = !prefs.allowTaskerIntegration || prefs.taskerIntegrationSecret.isEmpty()
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
                toggleTunnelState(context, tunnelName, state, manager)
                return
            }
            when (integrationSecret) {
                prefs.taskerIntegrationSecret -> toggleTunnelState(context, tunnelName, state, manager)
                else -> Timber.e("Intent integration secret mis-match! Exiting...")
            }
        } else if (tunnelName == null) {
            Timber.d("Intent parameter ${TunnelManager.TUNNEL_NAME_INTENT_EXTRA} not set!")
        }
    }

    private fun toggleTunnelState(context: Context, tunnelName: String, state: Tunnel.State, manager: TunnelManager) {
        Timber.d("Setting $tunnelName's state to $state")
        manager.getTunnels().thenAccept { tunnels ->
            val tunnel = tunnels[tunnelName]
            tunnel?.let {
                manager.setTunnelState(it, state)
            }
        }
        handler.postDelayed({
            context.sendBroadcast(Intent(context, OneTapWidget::class.java).apply {
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, OneTapWidget::class.java))
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            })
        }, 1000L)
    }

    companion object {
        private const val ACTION_FIRE_SETTING = "com.twofortyfouram.locale.intent.action.FIRE_SETTING"
    }
}
