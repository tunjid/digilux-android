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

package com.tunjid.fingergestures.ui.brightness

import androidx.lifecycle.ViewModel
import com.tunjid.fingergestures.managers.BackgroundManager
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer
import com.tunjid.fingergestures.toLiveData
import com.tunjid.fingergestures.filterIsInstance
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import io.reactivex.rxkotlin.Flowables
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class State(
    val completed: Boolean,
    val animateSlide: Boolean,
    val showDimmerText: Boolean,
    val verticalBias: Float,
    val dimmerPercent: Float,
    val initialProgress: Int,
    val sliderColor: Int,
    val backgroundColor: Int
)

sealed class Input {
    data class Change(val percentage: Int) : Input()
    data class Start(val brightnessByte: Int) : Input()
    object RemoveDimmer : Input()
}

class BrightnessViewModel @Inject constructor(
    private val brightnessGestureConsumer: BrightnessGestureConsumer,
    private val backgroundManager: BackgroundManager,
) : ViewModel() {

    private var brightnessByte: Int = 0

    private val processor = PublishProcessor.create<Input>()

    val state = Flowables.combineLatest(
        processor.switchMap {
            Flowable.timer(backgroundManager.sliderDurationMillis.toLong(), TimeUnit.MILLISECONDS)
        }
            .map { true }
            .startWith(false),
        brightnessGestureConsumer.animateSliderPreference.monitor,
        brightnessGestureConsumer.screenDimmerPercentPreference.monitor
            .map { it != 0f },
        brightnessGestureConsumer.positionPreference.monitor
            .map { it / 100f },
        brightnessGestureConsumer.screenDimmerPercentPreference.monitor
            .map { it * 100f },
        processor.filterIsInstance<Input.Start>()
            .map(Input.Start::brightnessByte)
            .map(brightnessGestureConsumer::byteToPercentage),
        backgroundManager.sliderColorPreference.monitor,
        backgroundManager.backgroundColorPreference.monitor,
        ::State
    ).toLiveData()

    fun accept(input: Input) {
        processor.onNext(input)

        when (input) {
            is Input.Start -> Unit
            is Input.Change -> {
                var value = input.percentage
                if (value == 100) value--

                brightnessByte = brightnessGestureConsumer.percentToByte(value)
                brightnessGestureConsumer.saveBrightness(brightnessByte)
            }
            Input.RemoveDimmer -> brightnessGestureConsumer.removeDimmer()
        }
    }
}