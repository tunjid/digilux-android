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

package com.tunjid.fingergestures


import android.content.Intent
import com.tunjid.fingergestures.activities.PopupActivity
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.SHOW_POPUP
import com.tunjid.fingergestures.gestureconsumers.GestureMapper

class PopUpGestureConsumer private constructor() : GestureConsumer {

    private val setManager: SetManager<Int> = SetManager(
            Comparator(Int::compareTo),
            this::canAddToSet,
            Integer::valueOf,
            Any::toString)

    var isSingleClick: Boolean
        get() = App.transformApp({ app -> app.preferences.getBoolean(ACCESSIBILITY_BUTTON_SINGLE_CLICK, false) }, false)
        set(isSingleClick) = App.withApp { app -> app.preferences.edit().putBoolean(ACCESSIBILITY_BUTTON_SINGLE_CLICK, isSingleClick).apply() }

    val list: List<Int>
        get() = setManager.getItems(SAVED_ACTIONS)

    val popUpActions = setManager.itemsFlowable(SAVED_ACTIONS)

    override fun onGestureActionTriggered(gestureAction: Int) {
        App.withApp { app -> app.broadcast(Intent(ACTION_SHOW_POPUP)) }
    }

    override fun accepts(gesture: Int): Boolean = gesture == SHOW_POPUP

    fun hasAccessibilityButton(): Boolean =
            App.transformApp({ app -> app.preferences.getBoolean(ACCESSIBILITY_BUTTON_ENABLED, false) }, false)

    fun shouldAnimatePopup(): Boolean =
            App.transformApp({ app -> app.preferences.getBoolean(ANIMATES_POPUP, true) }, true)

    fun addToSet(@GestureConsumer.GestureAction action: Int): Boolean =
            setManager.addToSet(action.toString(), SAVED_ACTIONS)

    fun removeFromSet(@GestureConsumer.GestureAction action: Int) =
            setManager.removeFromSet(action.toString(), SAVED_ACTIONS)

    fun setAnimatesPopup(visible: Boolean) {
        App.withApp { app -> app.preferences.edit().putBoolean(ANIMATES_POPUP, visible).apply() }
    }

    fun showPopup() = if (isSingleClick) list.stream()
            .findFirst()
            .ifPresent(GestureMapper.instance::performAction)
    else App.withApp { app ->
        val intent = Intent(app, PopupActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        app.startActivity(intent)
    }

    fun enableAccessibilityButton(enabled: Boolean) = App.withApp { app ->
        app.preferences.edit().putBoolean(ACCESSIBILITY_BUTTON_ENABLED, enabled).apply()
        app.broadcast(Intent(ACTION_ACCESSIBILITY_BUTTON)
                .putExtra(EXTRA_SHOWS_ACCESSIBILITY_BUTTON, enabled))
    }

    private fun canAddToSet(preferenceName: String): Boolean =
            setManager.getSet(preferenceName).size < 2 || PurchasesManager.instance.isPremiumNotTrial

    companion object {

        const val ACTION_ACCESSIBILITY_BUTTON = "com.tunjid.fingergestures.action.accessibilityButton"
        const val ACTION_SHOW_POPUP = "PopUpGestureConsumer shows popup"
        const val EXTRA_SHOWS_ACCESSIBILITY_BUTTON = "extra shows accessibility button"
        private const val ACCESSIBILITY_BUTTON_ENABLED = "accessibility button enabled"
        private const val ACCESSIBILITY_BUTTON_SINGLE_CLICK = "accessibility button single click"
        private const val SAVED_ACTIONS = "accessibility button apps"
        private const val ANIMATES_POPUP = "animates popup"

        val instance: PopUpGestureConsumer by lazy { PopUpGestureConsumer() }
    }
}
