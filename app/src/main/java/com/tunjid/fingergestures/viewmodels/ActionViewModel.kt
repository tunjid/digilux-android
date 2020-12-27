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

import androidx.lifecycle.ViewModel
import com.tunjid.fingergestures.PopUpGestureConsumer
import com.tunjid.fingergestures.gestureconsumers.GestureMapper
import com.tunjid.fingergestures.models.Action
import com.tunjid.fingergestures.models.Unique
import com.tunjid.fingergestures.toLiveData
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor

data class ActionState(
    val needsPremium: Unique<Boolean> = Unique(false),
    val availableActions: List<Action> = listOf(),
)

sealed class ActionInput {
    data class Add(val action: Action) : ActionInput()
    data class MapGesture(val direction: String, val action: Action) : ActionInput()
}

class ActionViewModel : ViewModel() {

    private val popUpGestureConsumer = PopUpGestureConsumer.instance
    private val gestureMapper = GestureMapper.instance
    private val processor = PublishProcessor.create<Boolean>()

    val state = Flowable.combineLatest(
        processor.startWith(false).map(::Unique),
        Flowable.just(gestureMapper.actions).listMap(::Action),
        ::ActionState
    ).toLiveData()

    fun accept(input: ActionInput) = when (input) {
        is ActionInput.Add -> {
            val added = popUpGestureConsumer.setManager
                .addToSet(PopUpGestureConsumer.Preference.SavedActions, input.action.value)
            if (!added) processor.onNext(true)
            else Unit
        }
        is ActionInput.MapGesture -> gestureMapper.mapGestureToAction(input.direction, input.action.value)
    }
}