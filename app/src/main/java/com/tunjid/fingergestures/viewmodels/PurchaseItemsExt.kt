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

import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.gestureconsumers.GestureMapper
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables

val Inputs.purchaseItems: Flowable<List<Item>>
    get() = with(dependencies.purchasesManager) {
        Flowables.combineLatest(
            GestureMapper.instance.doubleSwipePreference.monitor,
            state
        ) { doubleSwipe, state ->
            listOf(
                Item.AdFree(
                    sortKey = AppViewModel.AD_FREE,
                    tab = Tab.Gestures,
                    input = this@purchaseItems,
                    notAdFree = state.notAdFree,
                    notPremium = !state.isPremium,
                    hasAds = state.hasAds,
                ),
                Item.Toggle(
                    tab = Tab.Gestures,
                    sortKey = AppViewModel.LOCKED_CONTENT,
                    titleRes = R.string.set_locked_content,
                    isChecked = state.hasLockedContent,
                    consumer = lockedContentPreference.setter
                ),
                Item.Slider(
                    tab = Tab.Brightness,
                    sortKey = AppViewModel.DOUBLE_SWIPE_SETTINGS,
                    titleRes = R.string.adjust_double_swipe_settings,
                    infoRes = 0,
                    isEnabled = state.isPremium,
                    value = doubleSwipe,
                    consumer = GestureMapper.instance.doubleSwipePreference.setter,
                    function = GestureMapper.instance::getSwipeDelayText
                ),
            )
        }
    }