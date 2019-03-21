/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wireguard.android.R
import com.wireguard.android.databinding.LogViewerActivityBinding

class LiveLogViewerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var logcatThread: Thread
    private val logcatDataset: ArrayList<LogEntry> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: LogViewerActivityBinding =
                DataBindingUtil.setContentView(this, R.layout.log_viewer_activity)
        viewManager = LinearLayoutManager(this)
        viewAdapter = LogEntryAdapter(logcatDataset)
        recyclerView = binding.logviewer.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
        logcatThread = Thread(Runnable {
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-b", "all", "-v", "threadtime", "*:V"))
            process.inputStream.bufferedReader().apply {
                var line: String?
                while (true) {
                    line = readLine()
                    if (line == null)
                        break
                    logcatDataset.add(LogEntry(line))
                    runOnUiThread { viewAdapter.notifyDataSetChanged() }
                }
            }
        })
        logcatThread.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::logcatThread.isInitialized && logcatThread.isAlive) {
            logcatThread.interrupt()
        }
    }

    class LogEntryAdapter(private val myDataset: ArrayList<LogEntry>) :
            PagedListAdapter<LogEntry, LogEntryAdapter.ViewHolder>(diffCallback) {

        class ViewHolder(val textView: TextView, var isSingleLine: Boolean = true) :
                RecyclerView.ViewHolder(textView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): LogEntryAdapter.ViewHolder {
            val textView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.log_viewer_entry, parent, false) as TextView
            return ViewHolder(textView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.apply {
                setSingleLine()
                text = myDataset[position].entry
                setOnClickListener {
                    setSingleLine(!holder.isSingleLine)
                    holder.isSingleLine = !holder.isSingleLine
                }
            }
        }

        override fun getItemCount() = myDataset.size
    }

    data class LogEntry(val entry: String)

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<LogEntry>() {
            override fun areContentsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean {
                return oldItem.entry == newItem.entry
            }

            override fun areItemsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean {
                return false
            }
        }
    }
}
