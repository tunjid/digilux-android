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
import com.tunjid.fingergestures.gestureconsumers.GestureAction
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer
import com.tunjid.fingergestures.gestureconsumers.GestureAction.SHOW_POPUP
import com.tunjid.fingergestures.gestureconsumers.GestureMapper

class PopUpGestureConsumer private constructor() : GestureConsumer {

    enum class Preference(override val preferenceName: String) : ListPreference {
        SavedActions(preferenceName = "accessibility button apps");
    }

    val accessibilityButtonEnabledPreference: ReactivePreference<Boolean> = ReactivePreference(
        preferencesName = ACCESSIBILITY_BUTTON_ENABLED,
        default = false,
        onSet = { enabled ->
            App.instance?.broadcast(Intent(ACTION_ACCESSIBILITY_BUTTON)
                .putExtra(EXTRA_SHOWS_ACCESSIBILITY_BUTTON, enabled))
        }
    )
    val accessibilityButtonSingleClickPreference: ReactivePreference<Boolean> = ReactivePreference(
        preferencesName = ACCESSIBILITY_BUTTON_SINGLE_CLICK,
        default = false
    )
    val animatePopUpPreference: ReactivePreference<Boolean> = ReactivePreference(
        preferencesName = ANIMATES_POPUP,
        default = true
    )

    val setManager: SetManager<Preference, GestureAction> = SetManager(
        keys = Preference.values().toList(),
        sorter = compareBy(GestureAction::id),
        addFilter = this::canAddToSet,
        stringMapper = GestureAction::deserialize,
        objectMapper = GestureAction::serialized)

    val popUpActions = setManager.itemsFlowable(Preference.SavedActions)

    private val list: List<GestureAction>
        get() = setManager.getItems(Preference.SavedActions)

    override fun onGestureActionTriggered(gestureAction: GestureAction) {
        App.withApp { app -> app.broadcast(Intent(ACTION_SHOW_POPUP)) }
    }

    override fun accepts(gesture: GestureAction): Boolean = gesture == SHOW_POPUP

    fun showPopup() = when {
        accessibilityButtonSingleClickPreference.value -> list
            .firstOrNull()
            ?.let(GestureMapper.instance::performAction)
            ?: Unit
        else -> App.withApp { app ->
            val intent = Intent(app, PopupActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            app.startActivity(intent)
        }
    }

    private fun canAddToSet(preferenceName: Preference): Boolean =
        setManager.getSet(preferenceName).size < 2 || PurchasesManager.instance.isPremiumNotTrial

    companion object {

        const val ACTION_ACCESSIBILITY_BUTTON = "com.tunjid.fingergestures.action.accessibilityButton"
        const val ACTION_SHOW_POPUP = "PopUpGestureConsumer shows popup"
        const val EXTRA_SHOWS_ACCESSIBILITY_BUTTON = "extra shows accessibility button"
        private const val ACCESSIBILITY_BUTTON_ENABLED = "accessibility button enabled"
        private const val ACCESSIBILITY_BUTTON_SINGLE_CLICK = "accessibility button single click"
        private const val ANIMATES_POPUP = "animates popup"

        val instance: PopUpGestureConsumer by lazy { PopUpGestureConsumer() }
    }
}
