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

package com.tunjid.fingergestures.di

import com.tunjid.fingergestures.BackgroundManager
import com.tunjid.fingergestures.gestureconsumers.PopUpGestureConsumer
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.gestureconsumers.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
data class AppDependencies @Inject constructor(
    val gestureMapper: GestureMapper,
    val purchasesManager: PurchasesManager,
    val backgroundManager: BackgroundManager,
    val gestureConsumers: GestureConsumers,
)

@Singleton
data class GestureConsumers @Inject constructor(
    val nothing: NothingGestureConsumer,
    val brightness: BrightnessGestureConsumer,
    val notification: NotificationGestureConsumer,
    val flashlight: FlashlightGestureConsumer,
    val docking: DockingGestureConsumer,
    val rotation: RotationGestureConsumer,
    val globalAction: GlobalActionGestureConsumer,
    val popUp: PopUpGestureConsumer,
    val audio: AudioGestureConsumer
) {
    val all = listOf(
        nothing, brightness, notification, flashlight,
        docking, rotation, globalAction, popUp, audio
    )
}