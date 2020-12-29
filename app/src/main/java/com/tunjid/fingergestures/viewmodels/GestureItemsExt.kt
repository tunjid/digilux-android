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

import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.gestureconsumers.GestureMapper
import com.tunjid.fingergestures.viewmodels.main.Inputs
import com.tunjid.fingergestures.viewmodels.main.Tab
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables

val Inputs.gestureItems: Flowable<List<Item>>
    get() = Flowables.combineLatest(
        dependencies.gestureMapper.directionPreferencesFlowable,
        dependencies.purchasesManager.state.map(PurchasesManager.State::isPremium)
    ) { state, isPremium ->
        listOf(
            Item.Mapper(
                sortKey = AppViewModel.MAP_UP_ICON,
                tab = Tab.Gestures,
                direction = GestureMapper.UP_GESTURE,
                doubleDirection = GestureMapper.DOUBLE_UP_GESTURE,
                gesturePair = state.up,
                canUseDoubleSwipes = isPremium,
                input = this@gestureItems
            ),
            Item.Mapper(
                sortKey = AppViewModel.MAP_DOWN_ICON,
                tab = Tab.Gestures,
                direction = GestureMapper.DOWN_GESTURE,
                doubleDirection = GestureMapper.DOUBLE_DOWN_GESTURE,
                gesturePair = state.down,
                canUseDoubleSwipes = isPremium,
                input = this@gestureItems
            ),
            Item.Mapper(
                sortKey = AppViewModel.MAP_LEFT_ICON,
                tab = Tab.Gestures,
                direction = GestureMapper.LEFT_GESTURE,
                doubleDirection = GestureMapper.DOUBLE_LEFT_GESTURE,
                gesturePair = state.left,
                canUseDoubleSwipes = isPremium,
                input = this@gestureItems
            ),
            Item.Mapper(
                sortKey = AppViewModel.MAP_RIGHT_ICON,
                tab = Tab.Gestures,
                direction = GestureMapper.RIGHT_GESTURE,
                doubleDirection = GestureMapper.DOUBLE_RIGHT_GESTURE,
                gesturePair = state.right,
                canUseDoubleSwipes = isPremium,
                input = this@gestureItems
            ),
        )
    }