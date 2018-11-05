package com.wireguard.android.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

class CoroutinesWorker {
    private val singleThreadDispatcher = newSingleThreadContext("singleThreadDispatcher")

    suspend fun <T> async(block: suspend CoroutineScope.() -> T): Deferred<T> {
        return GlobalScope.async(Dispatchers.Main, CoroutineStart.DEFAULT) { block() }
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