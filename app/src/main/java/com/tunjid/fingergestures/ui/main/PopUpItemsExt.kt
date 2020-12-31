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

package com.tunjid.fingergestures.ui.main

import com.tunjid.fingergestures.gestureconsumers.PopUpGestureConsumer
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.models.toPopUpActions
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables

val Inputs.popUpItems: Flowable<List<Item>>
    get() = with(dependencies.gestureConsumers.popUp) {
        Flowables.combineLatest(
            setManager.itemsFlowable(PopUpGestureConsumer.Preference.SavedActions)
                .toPopUpActions(dependencies.backgroundManager.sliderColorPreference.monitor),
            accessibilityButtonSingleClickPreference.monitor,
            accessibilityButtonEnabledPreference.monitor,
            animatePopUpPreference.monitor,
        ) { savedActions, isSingleClick, accessibilityButtonEnabled, animatePopup ->
            listOf(
                Item.Toggle(
                    tab = Tab.Shortcuts,
                    sortKey = MainViewModel.ENABLE_ACCESSIBILITY_BUTTON,
                    titleRes = R.string.popup_enable,
                    isChecked = accessibilityButtonEnabled,
                    consumer = accessibilityButtonEnabledPreference.setter
                ),
                Item.Toggle(
                    tab = Tab.Shortcuts,
                    sortKey = MainViewModel.ACCESSIBILITY_SINGLE_CLICK,
                    titleRes = R.string.popup_single_click,
                    isChecked = isSingleClick,
                    consumer = accessibilityButtonSingleClickPreference.setter
                ),
                Item.Toggle(
                    tab = Tab.Shortcuts,
                    sortKey = MainViewModel.ANIMATES_POPUP,
                    titleRes = R.string.popup_animate_in,
                    isChecked = animatePopup,
                    consumer = animatePopUpPreference.setter
                ),
                Item.PopUp(
                    tab = Tab.Shortcuts,
                    sortKey = MainViewModel.POPUP_ACTION,
                    items = savedActions,
                    editor = setManager.editorFor(PopUpGestureConsumer.Preference.SavedActions),
                    accessibilityButtonEnabled = accessibilityButtonEnabled,
                    input = this@popUpItems
                )
            )
        }
    }