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

package com.tunjid.fingergestures.gestureconsumers


import android.content.Context
import android.content.Intent
import com.tunjid.fingergestures.ReactivePreference
import com.tunjid.fingergestures.ReactivePreferences
import com.tunjid.fingergestures.SetManager
import com.tunjid.fingergestures.SetPreference
import com.tunjid.fingergestures.ui.popup.PopupActivity
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.di.AppBroadcaster
import com.tunjid.fingergestures.di.AppContext
import com.tunjid.fingergestures.gestureconsumers.GestureAction.PopUpShow
import com.tunjid.fingergestures.models.Broadcast
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PopUpGestureConsumer @Inject constructor(
    @AppContext private val context: Context,
    reactivePreferences: ReactivePreferences,
    private val broadcaster: AppBroadcaster,
    private val purchasesManager: PurchasesManager
) : GestureConsumer {

    enum class Preference(override val preferenceName: String) : SetPreference {
        SavedActions(preferenceName = "accessibility button apps");
    }

    val accessibilityButtonEnabledPreference: ReactivePreference<Boolean> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = ACCESSIBILITY_BUTTON_ENABLED,
        default = false,
        onSet = { enabled ->
            broadcaster(Broadcast.Service.AccessibilityButtonChanged(enabled))
        }
    )
    val accessibilityButtonSingleClickPreference: ReactivePreference<Boolean> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = ACCESSIBILITY_BUTTON_SINGLE_CLICK,
        default = false
    )
    val animatePopUpPreference: ReactivePreference<Boolean> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = ANIMATES_POPUP,
        default = true
    )

    val setManager: SetManager<Preference, GestureAction> = SetManager(
        reactivePreferences = reactivePreferences,
        keys = Preference.values().toList(),
        sorter = compareBy(GestureAction::id),
        addFilter = this::canAddToSet,
        stringMapper = GestureAction::deserialize,
        objectMapper = GestureAction::serialized)

    val popUpActions = setManager.itemsFlowable(Preference.SavedActions)

    private val list: List<GestureAction>
        get() = setManager.getItems(Preference.SavedActions)

    override fun onGestureActionTriggered(gestureAction: GestureAction) =
        broadcaster(Broadcast.Service.ShowPopUp)

    override fun accepts(gesture: GestureAction): Boolean = gesture == PopUpShow

    fun showPopup() = when {
        accessibilityButtonSingleClickPreference.value -> list
            .firstOrNull()
            ?.let(Broadcast::Gesture)
            ?.let(broadcaster)
            ?: Unit
        else -> context.startActivity(Intent(context, PopupActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    private fun canAddToSet(preferenceName: Preference): Boolean =
        setManager.getSet(preferenceName).size < 2 || purchasesManager.currentState.isPremiumNotTrial

    companion object {
        private const val ACCESSIBILITY_BUTTON_ENABLED = "accessibility button enabled"
        private const val ACCESSIBILITY_BUTTON_SINGLE_CLICK = "accessibility button single click"
        private const val ANIMATES_POPUP = "animates popup"
    }
}
