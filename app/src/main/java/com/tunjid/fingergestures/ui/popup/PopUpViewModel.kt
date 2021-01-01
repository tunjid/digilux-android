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

package com.tunjid.fingergestures.ui.popup

import androidx.lifecycle.ViewModel
import com.tunjid.fingergestures.managers.BackgroundManager
import com.tunjid.fingergestures.gestureconsumers.PopUpGestureConsumer
import com.tunjid.fingergestures.gestureconsumers.GestureMapper
import com.tunjid.fingergestures.models.PopUp
import com.tunjid.fingergestures.models.toPopUpActions
import com.tunjid.fingergestures.toLiveData
import io.reactivex.rxkotlin.Flowables
import javax.inject.Inject

data class State(
    val animatesPopUp: Boolean,
    val sliderColor: Int,
    val backgroundColor: Int,
    val popUpActions: List<PopUp> = listOf(),
)

sealed class Input {
    data class Perform(val popUp: PopUp) : Input()
}

class PopUpViewModel @Inject constructor(
    backgroundManager: BackgroundManager,
    popUpGestureConsumer: PopUpGestureConsumer,
    private val gestureMapper: GestureMapper
) : ViewModel() {

    val state = Flowables.combineLatest(
        popUpGestureConsumer.animatePopUpPreference.monitor,
        backgroundManager.sliderColorPreference.monitor,
        backgroundManager.backgroundColorPreference.monitor,
        popUpGestureConsumer.popUpActions
            .toPopUpActions(backgroundManager.sliderColorPreference.monitor),
        ::State
    ).toLiveData()

    fun accept(input: Input) = when (input) {
        is Input.Perform -> gestureMapper.performAction(input.popUp.value)
    }
}