package me.melijn.siteapi.threading

import kotlinx.coroutines.runBlocking


class Task(private val func: suspend () -> Unit) : KTRunnable {

    override suspend fun run() {
        try {
            func()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

class DeferredNTask<T>(private val func: suspend () -> T?) : DeferredNKTRunnable<T> {

    override suspend fun run(): T? {
        return try {
            func()
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }
}


class DeferredTask<T>(private val func: suspend () -> T) : DeferredKTRunnable<T> {

    override suspend fun run(): T {
        return func()
    }
}

class RunnableTask(private val func: suspend () -> Unit) : Runnable {

    override fun run() {
        runBlocking {
            try {
                func()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}

class TaskInline(private inline val func: () -> Unit) : Runnable {

    override fun run() {
        try {
            func()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

@FunctionalInterface
interface KTRunnable {
    suspend fun run()
}

@FunctionalInterface
interface DeferredNKTRunnable<T> {
    suspend fun run(): T?
}

@FunctionalInterface
interface DeferredKTRunnable<T> {
    suspend fun run(): T
}