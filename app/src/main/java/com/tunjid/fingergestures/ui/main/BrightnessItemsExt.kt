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

package com.tunjid.fingergestures.ui.main

import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.ui.main.Item
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer
import com.tunjid.fingergestures.models.Brightness
import io.reactivex.Flowable

val Inputs.brightnessItems: Flowable<List<Item>>
    get() = with(dependencies.gestureConsumers.brightness) {
        state.map {
            listOf(
                Item.Slider(
                    tab = Tab.Brightness,
                    sortKey = ItemSorter.SLIDER_DELTA,
                    titleRes = R.string.adjust_slider_delta,
                    infoRes = 0,
                    consumer = percentagePreference.setter,
                    value = it.increment.value,
                    isEnabled = it.increment.enabled,
                    function = ::getAdjustDeltaText
                ),
                Item.Slider(
                    tab = Tab.Display,
                    sortKey = ItemSorter.SLIDER_POSITION,
                    titleRes = R.string.adjust_slider_position,
                    infoRes = 0,
                    consumer = positionPreference.setter,
                    value = it.position.value,
                    isEnabled = it.position.enabled,
                    function = percentageFormatter
                ),
                Item.Slider(
                    tab = Tab.Brightness,
                    sortKey = ItemSorter.ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS,
                    titleRes = R.string.adjust_adaptive_threshold,
                    infoRes = R.string.adjust_adaptive_threshold_description,
                    consumer = adaptiveBrightnessThresholdPreference.setter,
                    value = it.adaptive.value,
                    isEnabled = it.adaptive.enabled,
                    function = ::getAdaptiveBrightnessThresholdText
                ),
                Item.Toggle(
                    tab = Tab.Brightness,
                    sortKey = ItemSorter.ADAPTIVE_BRIGHTNESS,
                    titleRes = R.string.adaptive_brightness,
                    isChecked = it.restoresAdaptiveBrightnessOnDisplaySleep,
                    consumer = adaptiveBrightnessPreference.setter,
                ),
                Item.Toggle(
                    tab = Tab.Brightness,
                    sortKey = ItemSorter.USE_LOGARITHMIC_SCALE,
                    titleRes = R.string.use_logarithmic_scale,
                    isChecked = it.usesLogarithmicScale,
                    consumer = logarithmicBrightnessPreference.setter
                ),
                Item.Toggle(
                    tab = Tab.Brightness,
                    sortKey = ItemSorter.SHOW_SLIDER,
                    titleRes = R.string.show_slider,
                    isChecked = it.shouldShowSlider,
                    consumer = showSliderPreference.setter,
                ),
                Item.Toggle(
                    tab = Tab.Brightness,
                    sortKey = ItemSorter.ANIMATES_SLIDER,
                    titleRes = R.string.slider_animate,
                    isChecked = it.shouldAnimateSlider,
                    consumer = animateSliderPreference.setter
                ),
                Item.DiscreteBrightness(
                    tab = Tab.Brightness,
                    sortKey = ItemSorter.DISCRETE_BRIGHTNESS,
                    editor = discreteBrightnessManager.editorFor(BrightnessGestureConsumer.Preference.DiscreteBrightnesses),
                    brightnesses = it.discreteBrightnesses
                        .sorted()
                        .map(::Brightness),
                    input = this@brightnessItems,
                ),
                Item.ScreenDimmer(
                    tab = Tab.Brightness,
                    sortKey = ItemSorter.SLIDER_DELTA,
                    dimmerState = it.dimmerState,
                    consumer = screenDimmerEnabledPreference.setter,
                    input = this@brightnessItems,
                ),
            )
        }
    }