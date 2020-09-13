package me.melijn.siteapi.threading

import kotlinx.coroutines.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object TaskManager {

    private var taskCount = 0
    private var repeatCount = 0

    private val executorService: ExecutorService = Executors.newCachedThreadPool {
        Thread(it, "[Task-Pool-${taskCount++}]")
    }

    val dispatcher = executorService.asCoroutineDispatcher()
    val scheduledExecutorService: ScheduledExecutorService = Executors.newScheduledThreadPool(15) {
        Thread(it, "[Repeat-Pool-${repeatCount++}")
    }

    fun async(block: suspend CoroutineScope.() -> Unit) = CoroutineScope(dispatcher).launch {
        Task {
            block.invoke(this)
        }.run()
    }

    fun <T> taskValueAsync(block: suspend CoroutineScope.() -> T): Deferred<T> = CoroutineScope(dispatcher).async {
        DeferredTask {
            block.invoke(this)
        }.run()
    }

    fun <T> taskValueNAsync(block: suspend CoroutineScope.() -> T?): Deferred<T?> = CoroutineScope(dispatcher).async {
        DeferredNTask {
            block.invoke(this)
        }.run()
    }

    inline fun asyncInline(crossinline block: CoroutineScope.() -> Unit) = CoroutineScope(dispatcher).launch {
        TaskInline {
            block.invoke(this)
        }.run()
    }

    inline fun asyncAfter(afterMillis: Long, crossinline func: () -> Unit) {
        scheduledExecutorService.schedule(TaskInline { func() }, afterMillis, TimeUnit.MILLISECONDS)
    }
}
