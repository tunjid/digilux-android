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

package com.tunjid.fingergestures.models

import com.tunjid.fingergestures.gestureconsumers.GestureAction

sealed class Broadcast {
    sealed class Service: Broadcast() {
        object ExpandVolumeControls : Service()
        object ToggleDock : Service()
        data class AccessibilityButtonChanged(val enabled: Boolean) : Service()
        data class ScreenDimmerChanged(val percent: Float) : Service()
        data class GlobalAction(val action: Int) : Service()
        data class WatchesWindows(val enabled: Boolean) : Service()
        object ShadeDown : Service()
        object ShadeUp : Service()
        object ShadeToggle : Service()
    }
    data class Prompt(val message: String): Broadcast()
    data class Gesture(val gesture: GestureAction): Broadcast()
    object ShowPopUp : Broadcast()
    object AppResumed : Broadcast()
}