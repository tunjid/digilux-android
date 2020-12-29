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

package com.tunjid.fingergestures.viewmodels

import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.gestureconsumers.AudioGestureConsumer
import com.tunjid.fingergestures.viewmodels.main.Inputs
import com.tunjid.fingergestures.viewmodels.main.Tab
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables

val Inputs.audioItems: Flowable<List<Item>>
    get() = with(dependencies.gestureConsumers.audio) {
        Flowables.combineLatest(
            sliderPreference.monitor,
            incrementPreference.monitor,
            streamTypePreference.monitor
        ) { showSlider, audioValue, streamType ->
            listOf(
                Item.Toggle(
                    tab = Tab.Audio,
                    sortKey = AppViewModel.AUDIO_SLIDER_SHOW,
                    titleRes = R.string.audio_stream_slider_show,
                    isChecked = showSlider,
                    consumer = sliderPreference.setter,
                ),
                Item.Slider(
                    tab = Tab.Audio,
                    sortKey = AppViewModel.AUDIO_DELTA,
                    titleRes = R.string.audio_stream_delta,
                    infoRes = 0,
                    value = audioValue,
                    // TODO : make reactive
                    isEnabled = canSetVolumeDelta,
                    consumer = incrementPreference.setter,
                    function = ::getChangeText
                ),
                Item.AudioStream(
                    tab = Tab.Audio,
                    sortKey = AppViewModel.AUDIO_STREAM_TYPE,
                    titleFunction = ::getStreamTitle,
                    hasDoNotDisturbAccess = App.hasDoNotDisturbAccess,
                    consumer = streamTypePreference.setter,
                    stream = AudioGestureConsumer.Stream
                        .values()
                        .first { it.type == streamType },
                    input = this@audioItems
                )
            )
        }
    }