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


import android.annotation.TargetApi
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.tunjid.androidx.core.content.drawableAt
import com.tunjid.fingergestures.*
import com.tunjid.fingergestures.di.AppBroadcaster
import com.tunjid.fingergestures.di.AppBroadcasts
import com.tunjid.fingergestures.di.AppContext
import com.tunjid.fingergestures.di.AppDisposable
import com.tunjid.fingergestures.gestureconsumers.GestureAction.PopUpShow
import com.tunjid.fingergestures.managers.PurchasesManager
import com.tunjid.fingergestures.models.Broadcast
import com.tunjid.fingergestures.ui.popup.PopupDialogActivity
import io.reactivex.rxkotlin.addTo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PopUpGestureConsumer @Inject constructor(
    @AppContext private val context: Context,
    reactivePreferences: ReactivePreferences,
    appDisposable: AppDisposable,
    broadcasts: AppBroadcasts,
    private val broadcaster: AppBroadcaster,
    private val purchasesManager: PurchasesManager
) : GestureConsumer {

    enum class Preference(override val preferenceName: String) : SetPreference {
        SavedActions(preferenceName = "accessibility button apps");
    }

    val accessibilityButtonEnabledPreference: ReactivePreference<Boolean> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = "accessibility button enabled",
        default = false,
        onSet = { enabled ->
            broadcaster(Broadcast.Service.AccessibilityButtonChanged(enabled))
        }
    )
    val accessibilityButtonSingleClickPreference: ReactivePreference<Boolean> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = "accessibility button single click",
        default = false
    )
    val animatePopUpPreference: ReactivePreference<Boolean> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = "animates popup",
        default = true
    )
    val bubblePopUpPreference: ReactivePreference<Boolean> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = "popup bubbles",
        default = false
    )
    val positionPreference: ReactivePreference<Int> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = "popup position",
        default = 50
    )
    val setManager: SetManager<Preference, GestureAction> = SetManager(
        reactivePreferences = reactivePreferences,
        keys = Preference.values().toList(),
        sorter = compareBy(GestureAction::id),
        addFilter = this::canAddToSet,
        stringMapper = GestureAction::deserialize,
        objectMapper = GestureAction::serialized)

    @TargetApi(Build.VERSION_CODES.Q)
    val hasBubbleApi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val popUpActions = setManager.itemsFlowable(Preference.SavedActions)

    val percentageFormatter = { percent: Int -> context.getString(R.string.position_percent, percent) }

    private val list: List<GestureAction>
        by setManager.itemsFlowable(Preference.SavedActions).asProperty(listOf(), appDisposable::add)

    private val showsInBubble by bubblePopUpPreference.monitor.asProperty(false, appDisposable::add)

    init {
        NotificationManagerCompat.from(context).createNotificationChannel(
            NotificationChannelCompat.Builder(popUpBubbleChannel, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.popup_bubble_channel_name))
                .setDescription(context.getString(R.string.popup_bubble_channel_description))
                .build()
        )
        broadcasts.filterIsInstance<Broadcast.ShowPopUp>()
            .subscribe { showPopup() }
            .addTo(appDisposable)
    }

    override fun onGestureActionTriggered(gestureAction: GestureAction) = showPopup()

    override fun accepts(gesture: GestureAction): Boolean = gesture == PopUpShow

    private fun canAddToSet(preferenceName: Preference): Boolean =
        setManager.getSet(preferenceName).size < 2 || purchasesManager.currentState.isPremiumNotTrial

    @TargetApi(Build.VERSION_CODES.Q)
    private fun showPopup() = when {
        accessibilityButtonSingleClickPreference.value -> list
            .firstOrNull()
            ?.let(Broadcast::Gesture)
            ?.let(broadcaster)
            ?: Unit
        hasBubbleApi && showsInBubble -> bubbleNotification()
        else -> context.startActivity(PopupDialogActivity.intent(context, false))
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun bubbleNotification() {
        val bitmap = context.drawableAt(R.drawable.ic_logo)?.toBitmap() ?: return
        val icon = IconCompat.createWithAdaptiveBitmap(bitmap)
        val person = Person.Builder()
            .setName(context.getString(R.string.popup_bubble_notification_title))
            .setIcon(icon)
            .setImportant(true)
            .build()
        val intent = PopupDialogActivity.intent(context, true)

        ShortcutManagerCompat.pushDynamicShortcut(context, ShortcutInfoCompat.Builder(context, popUpBubbleShortcutId)
            .setLongLived(true)
            .setIntent(intent)
            .setShortLabel(context.getString(R.string.popup_bubble_notification_title))
            .setIcon(icon)
            .setPerson(person)
            .build())

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(popUpBubbleRequestCode, NotificationCompat.Builder(context, popUpBubbleChannel)
                .setShortcutId(popUpBubbleShortcutId)
                .setContentTitle(context.getString(R.string.popup_bubble_notification_title))
                .setLargeIcon(bitmap)
                .setSmallIcon(icon)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setStyle(NotificationCompat.MessagingStyle(person)
                    .setGroupConversation(false)
                    .addMessage(
                        context.getString(R.string.popup_bubble_notification_message),
                        System.currentTimeMillis(),
                        person
                    )
                )
                .addPerson(person)
                .setShowWhen(true)
                .setContentIntent(intent.toPendingIntent())
                .setBubbleMetadata(NotificationCompat.BubbleMetadata
                    .Builder()
                    .setIcon(icon)
                    .setAutoExpandBubble(true)
                    .setSuppressNotification(true)
                    .setIntent(PopupDialogActivity.intent(context, true).toPendingIntent())
                    .setDesiredHeight(context.resources.getDimensionPixelSize(R.dimen.popup_bubble_size))
                    .build()
                )
                .build())
        }
    }

    private fun Intent.toPendingIntent() = PendingIntent.getActivity(
        context,
        popUpBubbleRequestCode,
        this,
        PendingIntent.FLAG_UPDATE_CURRENT
    )
}

private const val popUpBubbleChannel = "Popup Bubbles"
private const val popUpBubbleShortcutId = "Popup Bubbles Shortcut Id"
private const val popUpBubbleRequestCode = 7