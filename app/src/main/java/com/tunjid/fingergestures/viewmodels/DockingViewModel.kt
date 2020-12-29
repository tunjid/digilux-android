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

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.tunjid.fingergestures.BackgroundManager
import com.tunjid.fingergestures.di.AppBroadcaster
import com.tunjid.fingergestures.gestureconsumers.DockingGestureConsumer
import com.tunjid.fingergestures.toLiveData
import javax.inject.Inject

data class DockingState(
    val backgroundColor: Int
)

class DockingViewModel @Inject constructor(
    backgroundManager: BackgroundManager,
    private val broadcaster: AppBroadcaster,
) : ViewModel() {

    val state = backgroundManager.backgroundColorPreference.monitor
        .map(::DockingState)
        .toLiveData()

    fun toggleDock() = broadcaster(Intent(DockingGestureConsumer.ACTION_TOGGLE_DOCK))
}