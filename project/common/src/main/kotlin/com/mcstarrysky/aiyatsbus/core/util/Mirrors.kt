/*
 *  Copyright (C) 2022-2024 SummerIceBearStudio
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.mcstarrysky.aiyatsbus.core.util

import com.mcstarrysky.aiyatsbus.core.AIYATSBUS_PREFIX
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.util.replaceWithOrder
import taboolib.module.chat.colored
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

fun <T> mirrorNow(id: String, func: (Mirror.MirrorStatus) -> T): T {
    val time = System.nanoTime()
    val status = Mirror.MirrorStatus()
    return func(status).also {
        if (status.isCancelled) return it
        Mirror.mirrorData.computeIfAbsent(id) { Mirror.MirrorData() }.finish(time)
    }
}

fun <T> mirrorFuture(id: String, func: Mirror.MirrorFuture<T>.() -> Unit): CompletableFuture<T> {
    val mf = Mirror.MirrorFuture<T>().also(func)
    mf.future.thenApply {
        Mirror.mirrorData.computeIfAbsent(id) { Mirror.MirrorData() }.finish(mf.time)
    }
    return mf.future
}

object Mirror {

    val mirrorData = ConcurrentHashMap<String, MirrorData>()

    fun report(sender: ProxyCommandSender, func: MirrorSettings.() -> Unit = {}): MirrorCollect {
        val options = MirrorSettings().also(func)
        val collect = MirrorCollect(options, "/", "/")
        mirrorData.forEach { mirror ->
            var point = collect
            mirror.key.split(":").forEach {
                point = point.sub.computeIfAbsent(it) { _ -> MirrorCollect(options, mirror.key, it) }
            }
        }
        collect.print(sender, collect.getTotal(), 0)
        return collect
    }

    class MirrorSettings {

        var childFormat = "$AIYATSBUS_PREFIX &8{0}&f{1} &8count({2})&c avg({3}ms) &7{4}ms ~ {5}ms &8··· &7{6}%"
        var parentFormat = "$AIYATSBUS_PREFIX &8{0}&7{1} &8count({2})&c avg({3}ms) &7{4}ms ~ {5}ms &8··· &7{6}%"
    }

    class MirrorFuture<T> {

        internal val time = System.nanoTime()
        internal val future = CompletableFuture<T>()

        fun finish(any: T) {
            future.complete(any)
        }
    }

    class MirrorCollect(val opt: MirrorSettings, val key: String, val path: String, val sub: MutableMap<String, MirrorCollect> = TreeMap()) {

        fun getTotal(): BigDecimal {
            var total = mirrorData[key]?.timeTotal ?: BigDecimal.ZERO
            sub.values.forEach {
                total = total.add(it.getTotal())
            }
            return total
        }

        fun print(sender: ProxyCommandSender, all: BigDecimal, space: Int) {
            val prefix = "${"···".repeat(space)}${if (space > 0) " " else ""}"
            val total = getTotal()
            val data = mirrorData[key]
            if (data != null) {
                val count = data.count
                val avg = data.getAverage()
                val min = data.getLowest()
                val max = data.getHighest()
                val format = if (sub.isEmpty()) opt.childFormat else opt.parentFormat
                sender.sendMessage(format.replaceWithOrder(prefix, path, count, avg, min, max, percent(all, total)).colored())
            }
            sub.values.map {
                it to percent(all, it.getTotal())
            }.sortedByDescending {
                it.second
            }.forEach {
                it.first.print(sender, all, if (data != null) space + 1 else space)
            }
        }

        fun percent(all: BigDecimal, total: BigDecimal): Double {
            return if (all.toDouble() == 0.0) 0.0 else total.divide(all, 2, RoundingMode.HALF_UP).multiply(BigDecimal("100")).toDouble()
        }
    }

    class MirrorStatus {

        var isCancelled = false
    }

    class MirrorData {

        internal var count = 0L
        internal var time = 0L
        internal var timeTotal = BigDecimal.ZERO
        internal var timeLatest = BigDecimal.ZERO
        internal var timeLowest = BigDecimal.ZERO
        internal var timeHighest = BigDecimal.ZERO

        init {
            reset()
        }

        fun define(): MirrorData {
            time = System.nanoTime()
            return this
        }

        fun finish(): MirrorData {
            return finish(time)
        }

        fun finish(startTime: Long): MirrorData {
            val stopTime = System.nanoTime()
            timeLatest = BigDecimal((stopTime - startTime) / 1000000.0).setScale(2, RoundingMode.HALF_UP)
            timeTotal = timeTotal.add(timeLatest)
            if (timeLatest.compareTo(timeHighest) == 1) {
                timeHighest = timeLatest
            }
            if (timeLatest.compareTo(timeLowest) == -1) {
                timeLowest = timeLatest
            }
            count++
            return this
        }

        fun reset(): MirrorData {
            count = 0
            timeTotal = BigDecimal.ZERO
            timeLatest = BigDecimal.ZERO
            timeLowest = BigDecimal.ZERO
            timeHighest = BigDecimal.ZERO
            return this
        }

        fun getTotal(): Double {
            return timeTotal.toDouble()
        }

        fun getLatest(): Double {
            return timeLatest.toDouble()
        }

        fun getHighest(): Double {
            return timeHighest.toDouble()
        }

        fun getLowest(): Double {
            return timeLowest.toDouble()
        }

        fun getAverage(): Double {
            return if (count == 0L) 0.0 else timeTotal.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP).toDouble()
        }
    }
}