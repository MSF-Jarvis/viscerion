package com.wireguard.android.util

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext

class CoroutinesWorker {
    private val singleThreadDispatcher = newSingleThreadContext("singleThreadDispatcher")

    suspend fun <T> async(block: suspend CoroutineScope.() -> T): Deferred<T> {
        return GlobalScope.async(Dispatchers.Main, CoroutineStart.DEFAULT, { block() })
    }

    suspend fun <T> asyncAwait(block: suspend CoroutineScope.() -> T): T {
        return async(block).await()
    }

    suspend fun <T> asyncExecutor(block: () -> T, response: (T) -> Unit): Job {
        return GlobalScope.launch(Dispatchers.Main) {
            val deferred = async(singleThreadDispatcher) { block.invoke() }
            response.invoke(deferred.await())
        }
    }
}