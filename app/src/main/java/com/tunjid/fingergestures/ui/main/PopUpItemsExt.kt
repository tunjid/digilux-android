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

import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.di.AppDependencies
import com.tunjid.fingergestures.gestureconsumers.GestureAction
import com.tunjid.fingergestures.gestureconsumers.GestureMapper
import com.tunjid.fingergestures.gestureconsumers.PopUpGestureConsumer
import com.tunjid.fingergestures.models.toPopUpActions
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables

val Inputs.popUpItems: Flowable<List<Item>>
    get() = with(dependencies.gestureConsumers.popUp) {
        Flowables.combineLatest(
            setManager.itemsFor(PopUpGestureConsumer.Preference.SavedActions)
                .toPopUpActions(dependencies.backgroundManager.sliderColorPreference.monitor),
            accessibilityButtonSingleClickPreference.monitor,
            accessibilityButtonEnabledPreference.monitor,
            animatePopUpPreference.monitor,
            bubblePopUpPreference.monitor,
            positionPreference.monitor,
            dependencies.hasPopUp
        ) { savedActions, isSingleClick, accessibilityButtonEnabled, animatePopup, isInBubble, popUpPosition, hasPopUp ->
            listOfNotNull(
                Item.Toggle(
                    tab = Tab.PopUp,
                    sortKey = ItemSorter.ENABLE_ACCESSIBILITY_BUTTON,
                    titleRes = R.string.popup_enable,
                    isChecked = accessibilityButtonEnabled,
                    onChanged = accessibilityButtonEnabledPreference.setter
                ),
                Item.Toggle(
                    tab = Tab.PopUp,
                    sortKey = ItemSorter.ACCESSIBILITY_SINGLE_CLICK,
                    titleRes = R.string.popup_single_click,
                    isChecked = isSingleClick,
                    onChanged = accessibilityButtonSingleClickPreference.setter
                ),
                Item.Toggle(
                    tab = Tab.PopUp,
                    sortKey = ItemSorter.ANIMATES_POPUP,
                    titleRes = R.string.popup_animate_in,
                    isChecked = animatePopup,
                    onChanged = animatePopUpPreference.setter
                ),
                Item.Toggle(
                    tab = Tab.PopUp,
                    sortKey = ItemSorter.BUBBLES_POPUP,
                    titleRes = R.string.popup_is_in_bubble,
                    isChecked = isInBubble,
                    onChanged = bubblePopUpPreference.setter
                ).takeIf { hasBubbleApi },
                Item.Slider(
                    tab = Tab.PopUp,
                    sortKey = ItemSorter.POPUP_POSITION,
                    titleRes = R.string.adjust_pop_up_position,
                    infoRes = 0,
                    consumer = positionPreference.setter,
                    value = popUpPosition,
                    isEnabled = true,
                    function = percentageFormatter
                ),
                Item.PopUp(
                    tab = Tab.PopUp,
                    sortKey = ItemSorter.POPUP_ACTION,
                    items = savedActions,
                    editor = setManager.editorFor(PopUpGestureConsumer.Preference.SavedActions),
                    enabled = hasPopUp,
                    input = this@popUpItems
                )
            )
        }
    }

private val GestureMapper.GesturePair.hasPopUp get() = singleAction == GestureAction.PopUpShow || doubleAction == GestureAction.PopUpShow
private val GestureMapper.State.hasPopUp get() = listOf(left, up, right, down).any(GestureMapper.GesturePair::hasPopUp)
private val AppDependencies.hasPopUp
    get() = Flowables.combineLatest(
        gestureConsumers.popUp.accessibilityButtonEnabledPreference.monitor,
        gestureConsumers.popUp.bubblePopUpPreference.monitor,
        gestureMapper.state.map(GestureMapper.State::hasPopUp),
    )
        .map(Triple<Boolean, Boolean, Boolean>::toList)
        .map { options -> options.any { it } }
