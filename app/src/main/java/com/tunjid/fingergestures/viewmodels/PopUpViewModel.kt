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

data class PopUpState(
    val popUpActions: List<Action> = listOf(),
)

sealed class PopUpInput {
    data class Perform(val action: Action) : PopUpInput()
}

class PopUpViewModel : ViewModel() {
    private val popUpGestureConsumer = PopUpGestureConsumer.instance
    private val gestureMapper = GestureMapper.instance

    val state = popUpGestureConsumer.popUpActions
        .listMap(::Action)
        .map(::PopUpState)
        .toLiveData()

    fun accept(input: PopUpInput) = when (input) {
        is PopUpInput.Perform -> gestureMapper.performAction(input.action.value)
    }
}