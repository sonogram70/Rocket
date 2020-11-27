package com.github.devil0414.rocket.task

import com.github.devil0414.rocket.util.Tick
import java.util.*
import kotlin.math.max

class RocketTask internal constructor(
    private val scheduler: RocketScheduler, val runnable: Runnable, delay: Long
) : Comparable<RocketTask> {

    companion object {
        internal const val ERROR = 0L
        internal const val NO_REPEATING = -1L
        internal const val CANCEL = -2L
        internal const val DONE = -3L
    }

    internal var nextRun: Long = Tick.currentTicks + max(0L, delay)

    internal var period: Long = 0L

    val isScheduled: Boolean
        get() = period.let { it != ERROR && it > CANCEL }

    val isCancelled
        get() = period == CANCEL

    val isDone
        get() = period == DONE

    internal fun execute() {
        runnable.runCatching { run() }
    }

    fun cancel() {
        if (!isScheduled) return

        period = CANCEL

        //256 tick 이상이면 큐에서 즉시 제거, 아닐경우 자연스럽게 제거
        val remainTicks = nextRun - Tick.currentTicks

        if (remainTicks > 0xFF)
            scheduler.remove(this)
    }

    override fun compareTo(other: RocketTask): Int {
        return nextRun.compareTo(other.nextRun)
    }
}

class RocketScheduler : Runnable {
    private val queue = PriorityQueue<RocketTask>()

    fun runTask(runnable: Runnable, delay: Long): RocketTask {
        RocketTask(this, runnable, delay).apply {
            this.period = RocketTask.NO_REPEATING
            queue.offer(this)
            return this
        }
    }

    fun runTaskTimer(runnable: Runnable, delay: Long, period: Long): RocketTask {
        RocketTask(this, runnable, delay).apply {
            this.period = max(1L, period)
            queue.offer(this)
            return this
        }
    }

    override fun run() {
        val current = Tick.currentTicks

        while (queue.isNotEmpty()) {
            val task = queue.peek()

            if (task.nextRun > current)
                break

            queue.remove()

            if (task.isScheduled) {

                task.run {
                    execute()
                    if (period > 0) {
                        nextRun = current + period
                        queue.offer(task)
                    } else {
                        period == RocketTask.DONE
                    }
                }
            }
        }
    }

    internal fun cancelAll() {
        val queue = this.queue
        queue.forEach { it.period = RocketTask.CANCEL }
        queue.clear()
    }

    fun remove(task: RocketTask) {
        queue.remove(task)
    }
}