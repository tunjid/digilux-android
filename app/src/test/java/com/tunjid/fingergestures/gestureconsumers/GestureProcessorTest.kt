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

package com.tunjid.fingergestures.gestureconsumers

import com.tunjid.fingergestures.gestureconsumers.GestureDirection
import com.tunjid.fingergestures.gestureconsumers.GestureProcessor
import io.reactivex.Flowable
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class GestureProcessorTest : GestureProcessor {

    override var scheduler = TestScheduler()
    private var observer = TestSubscriber<GestureDirection>()

    override val delayMillis: Long
        get() = 500L

    override val GestureDirection.hasDoubleAction: Boolean
        get() = this == GestureDirection.Down

    @Before
    fun setup() {
        scheduler = TestScheduler()
        observer = TestSubscriber()
    }

    @Test
    fun `recognizes double gestures if direction supports it`() {
        val backing = Flowable.just(
            GestureDirection.Down,
            GestureDirection.Down,
        )

        backing.processed.subscribe(observer)
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS)

        observer.assertValues(GestureDirection.DoubleDown)
    }

    @Test
    fun `does not double up if direction does not support it`() {
        val backing = Flowable.just(
            GestureDirection.Up,
            GestureDirection.Up,
            GestureDirection.Up,
            GestureDirection.Up,
            GestureDirection.Down,
        )

        backing.processed.subscribe(observer)
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS)

        observer.assertValues(
            GestureDirection.Up,
            GestureDirection.Up,
            GestureDirection.Up,
            GestureDirection.Up,
            GestureDirection.Down
        )

        println(observer.values())
    }

    @Test
    fun `ignores double gestures if swipes are persistent`() {
        val backing = Flowable.just(
            GestureDirection.Up,
            GestureDirection.Down,
            GestureDirection.Down,
            GestureDirection.Down,
            GestureDirection.Down,
        )

        backing.processed.subscribe(observer)
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS)

        observer.assertValues(
            GestureDirection.Up,
            GestureDirection.Down,
            GestureDirection.Down,
            GestureDirection.Down,
            GestureDirection.Down
        )
    }

    @Test
    fun `recognizes spaces between gestures`() {
        val backing = Flowable.just(
            GestureDirection.Up,
        ).concatWith(
            Flowable.just(
                GestureDirection.Down,
                GestureDirection.Down,
            ).delay(600, TimeUnit.MILLISECONDS, scheduler)
        ).concatWith(
            Flowable.just(
                GestureDirection.Left,
            ).delay(800, TimeUnit.MILLISECONDS, scheduler)
        ).concatWith(Flowable.never())

        backing.processed.subscribe(observer)
        scheduler.advanceTimeBy(4000, TimeUnit.SECONDS)

        observer.assertValues(
            GestureDirection.Up,
            GestureDirection.DoubleDown,
            GestureDirection.Left
        )
    }
}
