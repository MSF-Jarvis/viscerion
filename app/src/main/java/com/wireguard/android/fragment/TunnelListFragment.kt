/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.fragment

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.integration.android.IntentIntegrator
import com.wireguard.android.R
import com.wireguard.android.configStore.FileConfigStore.Companion.CONFIGURATION_FILE_SUFFIX
import com.wireguard.android.databinding.ObservableKeyedRecyclerViewAdapter
import com.wireguard.android.databinding.TunnelListFragmentBinding
import com.wireguard.android.databinding.TunnelListItemBinding
import com.wireguard.android.model.Tunnel
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.ui.AddTunnelsSheet
import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.AsyncWorker
import com.wireguard.android.util.ExceptionLoggers
import com.wireguard.android.util.KotlinCompanions
import com.wireguard.android.widget.MultiselectableRelativeLayout
import com.wireguard.android.widget.fab.FloatingActionButtonRecyclerViewScrollListener
import com.wireguard.config.Config
import java9.util.concurrent.CompletableFuture
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class TunnelListFragment : BaseFragment() {

    private val actionModeListener = ActionModeListener()
    private val asyncWorker by inject<AsyncWorker>()
    private val tunnelManager by inject<TunnelManager>()
    private val prefs by inject<ApplicationPreferences>()
    private var actionMode: ActionMode? = null
    private var binding: TunnelListFragmentBinding? = null

    private fun importTunnel(configText: String) {
        try {
            // Ensure the config text is parseable before proceeding…
            Config.parse(ByteArrayInputStream(configText.toByteArray(StandardCharsets.UTF_8)))

            // Config text is valid, now create the tunnel…
            ConfigNamingDialogFragment.newInstance(configText).show(requireFragmentManager(), null)
        } catch (exception: Exception) {
            onTunnelImportFinished(emptyList(), listOf<Throwable>(exception))
        }
    }

    private fun importTunnel(uri: Uri?) {
        val activity = activity
        if (activity == null || uri == null)
            return
        val contentResolver = activity.contentResolver

        val futureTunnels = ArrayList<CompletableFuture<Tunnel>>()
        val throwables = ArrayList<Throwable>()
        asyncWorker.supplyAsync {
            val columns = arrayOf(OpenableColumns.DISPLAY_NAME)
            var name = ""
            @Suppress("Recycle")
            contentResolver.query(uri, columns, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0))
                    name = cursor.getString(0)
            }
            if (name.isEmpty())
                name = Uri.decode(uri.lastPathSegment)
            var idx = name.lastIndexOf('/')
            if (idx >= 0) {
                if (idx >= name.length - 1)
                    throw IllegalArgumentException("Illegal file name: $name")
                name = name.substring(idx + 1)
            }
            val isZip = name.toLowerCase().endsWith(".zip")
            if (name.toLowerCase().endsWith(CONFIGURATION_FILE_SUFFIX))
                name = name.substring(0, name.length - CONFIGURATION_FILE_SUFFIX.length)
            else if (!isZip)
                throw IllegalArgumentException("File must be .conf or .zip")

            if (isZip) {
                ZipInputStream(contentResolver.openInputStream(uri)).use { zip ->
                    val reader = BufferedReader(InputStreamReader(zip, StandardCharsets.UTF_8))
                    var entry: ZipEntry?
                    while (true) {
                        entry = zip.nextEntry
                        if (entry == null)
                            break
                        name = entry.name
                        idx = name.lastIndexOf('/')
                        if (idx >= 0) {
                            if (idx >= name.length - 1)
                                continue
                            name = name.substring(name.lastIndexOf('/') + 1)
                        }
                        if (name.toLowerCase().endsWith(CONFIGURATION_FILE_SUFFIX))
                            name = name.substring(0, name.length - CONFIGURATION_FILE_SUFFIX.length)
                        else
                            continue
                        val config: Config? = try {
                            Config.parse(reader)
                        } catch (e: Exception) {
                            throwables.add(e)
                            null
                        }

                        if (config != null)
                            futureTunnels.add(tunnelManager.create(name, config).toCompletableFuture())
                    }
                }
            } else {
                futureTunnels.add(
                        tunnelManager.create(
                                name,
                                Config.parse(contentResolver.openInputStream(uri))
                        ).toCompletableFuture()
                )
            }

            if (futureTunnels.isEmpty()) {
                if (throwables.size == 1)
                    throw throwables[0]
                else if (throwables.isEmpty())
                    throw IllegalArgumentException("No configurations found")
            }

            CompletableFuture.allOf(*futureTunnels.toTypedArray())
        }.whenComplete { future, exception ->
            if (exception != null) {
                onTunnelImportFinished(emptyList(), listOf(exception))
            } else {
                future.whenComplete { _, _ ->
                    val tunnels = ArrayList<Tunnel>(futureTunnels.size)
                    for (futureTunnel in futureTunnels) {
                        val tunnel: Tunnel? = try {
                            futureTunnel.getNow(null)
                        } catch (e: Exception) {
                            throwables.add(e)
                            null
                        }

                        tunnel?.let {
                            tunnels.add(it)
                        }
                    }
                    onTunnelImportFinished(tunnels, throwables)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_IMPORT -> {
                if (resultCode == Activity.RESULT_OK)
                    data?.data?.also { uri ->
                        Timber.tag("TunnelImport").i("Import uri: $uri")
                        importTunnel(uri)
                    }
                return
            }
            IntentIntegrator.REQUEST_CODE -> {
                IntentIntegrator.parseActivityResult(requestCode, resultCode, data)?.contents?.let {
                    importTunnel(it)
                }
                return
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val bottomSheet = AddTunnelsSheet(this)

        binding = TunnelListFragmentBinding.inflate(inflater, container, false)
        binding?.apply {
            createFab.setOnClickListener { bottomSheet.show(requireFragmentManager(), "BOTTOM_SHEET") }
            tunnelList.addOnScrollListener(FloatingActionButtonRecyclerViewScrollListener(createFab))
            executePendingBindings()
        }
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun viewForTunnel(tunnel: Tunnel, tunnels: List<Tunnel>): MultiselectableRelativeLayout? {
        var view: MultiselectableRelativeLayout? = null
        binding?.let {
            view =
                    it.tunnelList.findViewHolderForAdapterPosition(tunnels.indexOf(tunnel))?.itemView as? MultiselectableRelativeLayout
        }
        return view
    }

    override fun onSelectedTunnelChanged(oldTunnel: Tunnel?, newTunnel: Tunnel?) {
        if (binding == null)
            return
        tunnelManager.getTunnels().thenAccept { tunnels ->
            newTunnel?.let {
                viewForTunnel(it, tunnels)?.setSingleSelected(true)
            }
            oldTunnel?.let {
                viewForTunnel(it, tunnels)?.setSingleSelected(false)
            }
        }
    }

    private fun onTunnelDeletionFinished(count: Int, throwable: Throwable?) {
        val message: String
        if (throwable == null) {
            message = resources.getQuantityString(R.plurals.delete_success, count, count)
        } else {
            val error = ExceptionLoggers.unwrapMessage(throwable)
            message = resources.getQuantityString(R.plurals.delete_error, count, count, error)
            Timber.e(throwable)
        }
        binding?.let {
            Snackbar.make(it.mainContainer, message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun onTunnelImportFinished(tunnels: List<Tunnel>, throwables: Collection<Throwable>) {
        var message = ""

        for (throwable in throwables) {
            val error = ExceptionLoggers.unwrapMessage(throwable)
            message = getString(R.string.import_error, error)
            Timber.e(throwable)
        }

        if (tunnels.size == 1 && throwables.isEmpty())
            message = getString(R.string.import_success, tunnels[0].name)
        else if (tunnels.isEmpty() && throwables.size == 1)
        else if (throwables.isEmpty())
            message = resources.getQuantityString(
                    R.plurals.import_total_success,
                    tunnels.size, tunnels.size
            )
        else if (!throwables.isEmpty())
            message = resources.getQuantityString(
                    R.plurals.import_partial_success,
                    tunnels.size + throwables.size,
                    tunnels.size, tunnels.size + throwables.size
            )/* Use the exception message from above. */

        if (prefs.exclusions.isNotEmpty()) {
            val excludedApps = prefs.exclusionsArray
            tunnels.forEach { tunnel ->
                val oldConfig = tunnel.getConfig()
                oldConfig?.let {
                    it.`interface`.excludedApplications.addAll(excludedApps)
                    tunnel.setConfig(it)
                }
            }
        }

        binding?.let {
            if (message.isNotEmpty())
                Snackbar.make(it.mainContainer, message, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putIntegerArrayList("CHECKED_ITEMS", actionModeListener.getCheckedItems())
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        savedInstanceState?.let { bundle ->
            val checkedItems = bundle.getIntegerArrayList("CHECKED_ITEMS")
            checkedItems?.let {
                it.forEach { checkedItem ->
                    actionModeListener.setItemChecked(checkedItem, true)
                }
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if (binding == null)
            return
        binding?.fragment = this
        tunnelManager.getTunnels().thenAccept { binding?.tunnels = it }
        binding?.rowConfigurationHandler =
                object : ObservableKeyedRecyclerViewAdapter.RowConfigurationHandler<TunnelListItemBinding, Tunnel> {
                    override fun onConfigureRow(binding: TunnelListItemBinding, tunnel: Tunnel, position: Int) {
                        binding.fragment = this@TunnelListFragment
                        binding.root.setOnClickListener {
                            if (actionMode == null) {
                                selectedTunnel = tunnel
                            } else {
                                actionModeListener.toggleItemChecked(position)
                            }
                        }
                        binding.root.setOnLongClickListener {
                            actionModeListener.toggleItemChecked(position)
                            true
                        }

                        if (actionMode != null)
                            (binding.root as MultiselectableRelativeLayout).setMultiSelected(
                                    actionModeListener.checkedItems.contains(position)
                            )
                        else
                            (binding.root as MultiselectableRelativeLayout).setSingleSelected(selectedTunnel == tunnel)
                    }
                }
    }

    private inner class ActionModeListener : ActionMode.Callback {
        val checkedItems = HashSet<Int>()

        private var resources: Resources? = null

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.menu_action_delete -> {
                    val copyCheckedItems = HashSet(checkedItems)
                    tunnelManager.getTunnels().thenAccept { tunnels ->
                        val tunnelsToDelete = ArrayList<Tunnel>()
                        for (position in copyCheckedItems)
                            tunnelsToDelete.add(tunnels[position])

                        val futures = KotlinCompanions.streamForDeletion(tunnelsToDelete)
                        CompletableFuture.allOf(*futures)
                                .thenApply { futures.size }
                                .whenComplete { count, throwable ->
                                    onTunnelDeletionFinished(count, throwable)
                                }
                    }
                    binding?.createFab?.extend()
                    checkedItems.clear()
                    mode.finish()
                    return true
                }
                R.id.menu_action_select_all -> {
                    tunnelManager.getTunnels().thenAccept { tunnels ->
                        for (i in tunnels.indices) {
                            setItemChecked(i, true)
                        }
                    }
                    return true
                }
                else -> return false
            }
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode
            activity?.let {
                resources = it.resources
            }
            mode.menuInflater.inflate(R.menu.tunnel_list_action_mode, menu)
            binding?.let {
                it.tunnelList.adapter?.notifyDataSetChanged()
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
            resources = null
            checkedItems.clear()
            binding?.let {
                it.tunnelList.adapter?.notifyDataSetChanged()
            }
        }

        internal fun toggleItemChecked(position: Int) {
            setItemChecked(position, !checkedItems.contains(position))
        }

        internal fun getCheckedItems(): ArrayList<Int> {
            return ArrayList(checkedItems)
        }

        internal fun setItemChecked(position: Int, checked: Boolean) {
            if (checked) {
                checkedItems.add(position)
            } else {
                checkedItems.remove(position)
            }

            val adapter = binding?.tunnelList?.adapter

            if (actionMode == null && checkedItems.isNotEmpty()) {
                (activity as AppCompatActivity).startSupportActionMode(this)
            } else if (checkedItems.isEmpty()) {
                actionMode?.finish()
            }

            adapter?.notifyItemChanged(position)

            updateTitle(actionMode)
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            updateTitle(mode)
            return false
        }

        private fun updateTitle(mode: ActionMode?) {
            if (mode == null) {
                return
            }

            val count = checkedItems.size
            mode.title = if (count == 0) "" else resources?.getQuantityString(R.plurals.delete_title, count, count)
        }
    }

    companion object {
        const val REQUEST_IMPORT = 1
    }
}
