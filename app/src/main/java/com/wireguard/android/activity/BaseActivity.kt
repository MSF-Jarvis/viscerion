/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.activity

import android.os.Bundle
import androidx.annotation.Nullable
import androidx.databinding.CallbackRegistry
import androidx.databinding.CallbackRegistry.NotifierCallback
import com.wireguard.android.Application
import com.wireguard.android.model.Tunnel

/**
 * Base class for activities that need to remember the currently-selected tunnel.
 */

abstract class BaseActivity : ThemeChangeAwareActivity() {

    private val selectionChangeRegistry = SelectionChangeRegistry()
    @Nullable
    @get:Nullable
    var selectedTunnel: Tunnel? = null
        set(@Nullable tunnel) {
            val oldTunnel = this.selectedTunnel
            if (oldTunnel == tunnel)
                return
            field = tunnel
            onSelectedTunnelChanged(oldTunnel, tunnel)
            selectionChangeRegistry.notifyCallbacks(oldTunnel, 0, tunnel)
        }

    fun addOnSelectedTunnelChangedListener(listener: OnSelectedTunnelChangedListener) {
        selectionChangeRegistry.add(listener)
    }

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        // Restore the saved tunnel if there is one; otherwise grab it from the arguments.
        val savedTunnelName: String? = when {
            savedInstanceState != null -> savedInstanceState.getString(KEY_SELECTED_TUNNEL)
            intent != null -> intent.getStringExtra(KEY_SELECTED_TUNNEL)
            else -> null
        }

        savedTunnelName?.let {
            Application.tunnelManager.completableTunnels.thenAccept { tunnels ->
                selectedTunnel = tunnels[it]
            }
        }

        // The selected tunnel must be set before the superclass method recreates fragments.
        super.onCreate(savedInstanceState)

        supportActionBar?.elevation = 0f
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        selectedTunnel?.let {
            outState?.putString(KEY_SELECTED_TUNNEL, it.name)
        }
        super.onSaveInstanceState(outState)
    }

    protected abstract fun onSelectedTunnelChanged(@Nullable oldTunnel: Tunnel?, @Nullable newTunnel: Tunnel?)

    fun removeOnSelectedTunnelChangedListener(
        listener: OnSelectedTunnelChangedListener
    ) {
        selectionChangeRegistry.remove(listener)
    }

    interface OnSelectedTunnelChangedListener {
        fun onSelectedTunnelChanged(@Nullable oldTunnel: Tunnel?, @Nullable newTunnel: Tunnel?)
    }

    private class SelectionChangeNotifier : NotifierCallback<OnSelectedTunnelChangedListener, Tunnel, Tunnel>() {
        override fun onNotifyCallback(
            listener: OnSelectedTunnelChangedListener,
            oldTunnel: Tunnel?,
            ignored: Int,
            newTunnel: Tunnel?
        ) {
            listener.onSelectedTunnelChanged(oldTunnel, newTunnel)
        }
    }

    private class SelectionChangeRegistry :
        CallbackRegistry<OnSelectedTunnelChangedListener, Tunnel, Tunnel>(SelectionChangeNotifier())

    companion object {
        private const val KEY_SELECTED_TUNNEL = "selected_tunnel"
    }
}
