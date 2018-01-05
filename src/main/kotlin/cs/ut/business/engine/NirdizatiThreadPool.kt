package cs.ut.business.engine

import cs.ut.config.MasterConfiguration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


internal object NirdizatiThreadPool {
    private val threadPool: ThreadPoolExecutor

    init {
        val config = MasterConfiguration.threadPoolConfiguration
        threadPool = ThreadPoolExecutor(
                config.core,
                config.max,
                config.keepAlive.toLong(),
                TimeUnit.SECONDS,
                ArrayBlockingQueue<Runnable>(config.capacity)
        )
    }

    fun execute(runnable: Runnable) {
        threadPool.execute(runnable)
    }
}