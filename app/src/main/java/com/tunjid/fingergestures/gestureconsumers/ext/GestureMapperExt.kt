/*
 * Copyright (c) 2017, 2018, 2019 Adetunji Dahunsi.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tunjid.fingergestures.gestureconsumers.ext

import com.tunjid.fingergestures.gestureconsumers.*
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Timed
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue


private data class Batch (
    val last: Timed<out Signal>,
    val third :Timed<out Signal>,
    val prev:  Timed<out Signal>,
    val head:  Timed<out Signal>,
)

private sealed class Signal {
    object Default : Signal()
    data class Item(val direction: GestureDirection, val id: String = UUID.randomUUID().toString().take(4)) : Signal()
}

val GestureDirection.isDouble get() = kind == Kind.Double

val GestureDirection.double: GestureDirection
    get() = this.match(this)

private fun GestureDirection.match(updated: GestureDirection): GestureDirection =
    if (updated != this) updated
    else when (updated) {
        GestureDirection.Up -> GestureDirection.DoubleUp
        GestureDirection.Down -> GestureDirection.DoubleDown
        GestureDirection.Left -> GestureDirection.DoubleLeft
        GestureDirection.Right -> GestureDirection.DoubleRight
        else -> updated
    }

interface GestureProcessor {
    val delayMillis: Long
    val scheduler: Scheduler
    val timeUnit get() = TimeUnit.MILLISECONDS

    val GestureDirection.hasDoubleAction: Boolean

    val Flowable<GestureDirection>.processed: Flowable<GestureDirection>
        get() = timestamp(scheduler)
            .buffer(signal)
            .map(::coalesced)
            .concatMap { Flowable.fromIterable(it) }

    private val emptySignal get() = Timed(Signal.Default, 0L, timeUnit)

    private val Flowable<GestureDirection>.signal: Flowable<Any>
        get() = timestamp(scheduler)
            .scan(Batch(emptySignal, emptySignal, emptySignal, emptySignal)) buffer@{ batch, timedGesture ->
                batch.copy(
                    last = batch.third,
                    third = batch.prev,
                    prev = batch.head,
                    head = Timed(Signal.Item(timedGesture.value()), timedGesture.time(), timedGesture.unit())
                )
            }
            .skip(1)
            .concatMap { Flowable.just(it).delay(2, timeUnit, scheduler) }
            .switchMap { batch ->
                val (_, _, prevTimed, headTimed) = batch
                val prev = prevTimed.value()
                val head = headTimed.value()

                // Default emissions
                if (head !is Signal.Item) return@switchMap head.signal(immediate = true)

                // Isolated emissions
                val headHasDouble = head.direction.hasDoubleAction
                if (prev !is Signal.Item || !headTimed.isCloseTo(prevTimed))
                    return@switchMap head.signal(immediate = !headHasDouble)

                // Batch emissions
                if (batch.areClose || batch.sameTrio) return@switchMap head.signal(immediate = true)
                if (headHasDouble) return@switchMap head.signal(immediate = false)

                head.signal(immediate = true)
            }

    private fun Any.signal(immediate: Boolean) = when (immediate) {
        true -> Flowable.just(this)
        else -> Flowable.timer(delayMillis, timeUnit, scheduler)
    }

    private fun coalesced(list: List<Timed<GestureDirection>>): List<GestureDirection> =
        list.foldIndexed(listOf<Timed<GestureDirection>>()) fold@{ index, directions, current ->
            // Do not match double swipes on batch requests
            if (directions.isEmpty() || list.size > 2) return@fold directions + current

            val prev = directions.last()
            if (!prev.value().hasDoubleAction) return@fold directions + current

            val next = list.getOrNull(index + 1)
            val plausibleDouble = prev.value().match(current.value())

            if (plausibleDouble.isDouble && next == null) directions.dropLast(1)
                .plus(Timed(plausibleDouble, current.time(), current.unit()))
            else directions + current
        }.map(Timed<GestureDirection>::value)

    private fun Timed<*>.isCloseTo(timed: Timed<*>): Boolean =
        (time() - timed.time()).absoluteValue <= delayMillis

    private val Batch.sameTrio
        get() = head.value() == prev.value()
            && prev.value() == third.value()

    private val Batch.areClose
        get() = head.isCloseTo(prev)
            && prev.isCloseTo(third)
            && third.isCloseTo(last)
}

