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
import com.tunjid.fingergestures.BackgroundManager
import com.tunjid.fingergestures.gestureconsumers.PopUpGestureConsumer
import com.tunjid.fingergestures.gestureconsumers.GestureDirection
import com.tunjid.fingergestures.gestureconsumers.GestureMapper
import com.tunjid.fingergestures.models.Action
import com.tunjid.fingergestures.models.Unique
import com.tunjid.fingergestures.models.toPopUpActions
import com.tunjid.fingergestures.toLiveData
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import javax.inject.Inject

data class ActionState(
    val needsPremium: Unique<Boolean> = Unique(false),
    val availableActions: List<Action> = listOf(),
)

sealed class ActionInput {
    data class Add(val action: Action) : ActionInput()
    data class MapGesture(val direction: GestureDirection, val action: Action) : ActionInput()
}

class ActionViewModel @Inject constructor(
    private val gestureMapper: GestureMapper,
    backgroundManager: BackgroundManager,
    popUpGestureConsumer: PopUpGestureConsumer,
) : ViewModel() {

    private val editor = popUpGestureConsumer.setManager
        .editorFor(PopUpGestureConsumer.Preference.SavedActions)

    private val processor = PublishProcessor.create<Boolean>()

    val state = Flowable.combineLatest(
        processor.startWith(false).map(::Unique),
        gestureMapper.supportedActions.toPopUpActions(backgroundManager.sliderColorPreference.monitor),
        ::ActionState
    ).toLiveData()

    fun accept(input: ActionInput) = when (input) {
        is ActionInput.Add -> {
            val added = editor + input.action.value
            if (!added) processor.onNext(true)
            else Unit
        }
        is ActionInput.MapGesture -> gestureMapper.mapGestureToAction(input.direction, input.action.value)
    }
}