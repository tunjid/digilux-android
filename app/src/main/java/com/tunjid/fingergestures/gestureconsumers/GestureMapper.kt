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

import android.accessibilityservice.FingerprintGestureController
import android.accessibilityservice.FingerprintGestureController.*
import android.annotation.SuppressLint
import android.content.Context
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.ReactivePreference
import com.tunjid.fingergestures.ReactivePreferences
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.di.AppBroadcasts
import com.tunjid.fingergestures.di.AppContext
import com.tunjid.fingergestures.di.GestureConsumers
import com.tunjid.fingergestures.gestureconsumers.GestureAction.*
import com.tunjid.fingergestures.models.Broadcast
import com.tunjid.fingergestures.viewmodels.filterIsInstance
import io.reactivex.Flowable
import io.reactivex.Flowable.timer
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

enum class Kind {
    Single,
    Double
}

enum class GestureDirection(
    val direction: String,
    val kind: Kind,
    val stringRes: Int
) {
    Up(direction = "up gesture", kind = Kind.Single, stringRes = R.string.swipe_up),
    Down(direction = "down gesture", kind = Kind.Single, stringRes = R.string.swipe_down),
    Left(direction = "left gesture", kind = Kind.Single, stringRes = R.string.swipe_left),
    Right(direction = "right gesture", kind = Kind.Single, stringRes = R.string.swipe_right),
    DoubleUp(direction = "double up gesture", kind = Kind.Double, stringRes = R.string.double_swipe_up),
    DoubleDown(direction = "double down gesture", kind = Kind.Double, stringRes = R.string.double_swipe_down),
    DoubleLeft(direction = "double left gesture", kind = Kind.Double, stringRes = R.string.double_swipe_left),
    DoubleRight(direction = "double right gesture", kind = Kind.Double, stringRes = R.string.double_swipe_right);
}

private val GestureDirection.isDouble get() = kind == Kind.Double

@SuppressLint("CheckResult")
@Singleton
class GestureMapper @Inject constructor(
    @AppContext private val context: Context,
    reactivePreferences: ReactivePreferences,
    private val purchasesManager: PurchasesManager,
    private val consumers: GestureConsumers,
    broadcasts: AppBroadcasts
) : FingerprintGestureController.FingerprintGestureCallback() {

    data class GesturePair(
        val singleGestureName: String,
        val doubleGestureName: String,
        val singleActionName: String,
        val doubleActionName: String,
    )

    data class State(
        val left: GesturePair,
        val up: GesturePair,
        val right: GesturePair,
        val down: GesturePair,
    )

    val doubleSwipePreference: ReactivePreference<Int> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = DOUBLE_SWIPE_DELAY,
        default = DEF_DOUBLE_SWIPE_DELAY_PERCENTAGE
    )

    private val directionPreferencesMap = GestureDirection.values().map { gestureDirection ->
        gestureDirection to ReactivePreference(
            reactivePreferences = reactivePreferences,
            key = gestureDirection.direction,
            default = when (gestureDirection) {
                GestureDirection.Up -> NOTIFICATION_UP
                GestureDirection.Down -> NOTIFICATION_DOWN
                GestureDirection.Left -> REDUCE_BRIGHTNESS
                GestureDirection.Right -> INCREASE_BRIGHTNESS
                else -> DO_NOTHING
            }.id
        )
    }
        .toMap()

    val directionPreferencesFlowable: Flowable<State>
        get() = Flowable.combineLatest(
            directionPreferencesMap
                .entries
                .map { (gestureDirection, preference) ->
                    preference.monitor.map {
                        val resource = (if (gestureDirection.isDouble && purchasesManager.isNotPremium) DO_NOTHING
                        else GestureAction.fromId(it)).resource

                        gestureDirection to context.getString(resource)
                    }
                }) { items ->
            val map = (items.toList() as List<Pair<GestureDirection, String>>).toMap()
            State(
                left = GesturePair(
                    singleGestureName = context.getString(GestureDirection.Left.stringRes),
                    doubleGestureName = context.getString(GestureDirection.DoubleLeft.stringRes),
                    singleActionName = map.getValue(GestureDirection.Left),
                    doubleActionName = map.getValue(GestureDirection.DoubleLeft)
                ),
                up = GesturePair(
                    singleGestureName = context.getString(GestureDirection.Up.stringRes),
                    doubleGestureName = context.getString(GestureDirection.DoubleUp.stringRes),
                    singleActionName = map.getValue(GestureDirection.Up),
                    doubleActionName = map.getValue(GestureDirection.DoubleUp)
                ),
                right = GesturePair(
                    singleGestureName = context.getString(GestureDirection.Right.stringRes),
                    doubleGestureName = context.getString(GestureDirection.DoubleRight.stringRes),
                    singleActionName = map.getValue(GestureDirection.Right),
                    doubleActionName = map.getValue(GestureDirection.DoubleRight)
                ),
                down = GesturePair(
                    singleGestureName = context.getString(GestureDirection.Down.stringRes),
                    doubleGestureName = context.getString(GestureDirection.DoubleDown.stringRes),
                    singleActionName = map.getValue(GestureDirection.Down),
                    doubleActionName = map.getValue(GestureDirection.DoubleDown)
                ),
            )
        }


    private val actionIds: IntArray

    private val directionReference: AtomicReference<GestureDirection> = AtomicReference()

    private var isSwipingDisposable: Disposable? = null
    private var doubleSwipeDisposable: Disposable? = null
    private var isOngoing: Boolean = false

    val actions: List<GestureAction>
        get() = actionIds.map(::actionForResource)
            .filter(::isSupportedAction)

    init {
        actionIds = getActionIds()
        broadcasts.filterIsInstance<Broadcast.Gesture>()
            .map(Broadcast.Gesture::gesture)
            .subscribe(this@GestureMapper::performAction)
    }

    fun mapGestureToAction( direction: GestureDirection, action: GestureAction) {
        directionPreferencesMap.getValue(direction).value = action.id
    }

    fun getMappedAction( gestureDirection: GestureDirection): String {
        val action = directionToAction(gestureDirection)
        val stringResource = action.resource
        return context.getString(stringResource)
    }

    fun doubleDirection(direction: GestureDirection): GestureDirection {
        return match(direction, direction)
    }

    fun getSwipeDelayText(percentage: Int): String =
        context.getString(when {
            purchasesManager.isNotPremium -> R.string.go_premium_text
            else -> R.string.double_swipe_delay
        }, delayPercentageToMillis(percentage))

    override fun onGestureDetected(raw: Int) {
        super.onGestureDetected(raw)

        val newDirection = rawToDirection(raw)
        val originalDirection = directionReference.get()

        val hasPreviousSwipe = originalDirection != null
        val hasPendingAction = doubleSwipeDisposable != null && !doubleSwipeDisposable!!.isDisposed
        val hasNoDoubleSwipe = directionToAction(doubleDirection(newDirection)) == DO_NOTHING

        // Keep responsiveness if user has not mapped gesture to a double swipe
        if (!hasPreviousSwipe && hasNoDoubleSwipe) {
            performAction(newDirection)
            return
        }

        // User has been swiping repeatedly in a certain direction
        if (isOngoing) {
            if (isSwipingDisposable != null) isSwipingDisposable!!.dispose()
            resetIsOngoing()
            performAction(newDirection)
            return
        }

        // Is canceling an existing double gesture to continue a single gesture
        if (hasPreviousSwipe && hasPendingAction && originalDirection!!.isDouble) {
            doubleSwipeDisposable!!.dispose()
            directionReference.set(null)
            isOngoing = true
            resetIsOngoing()
            performAction(newDirection)
            return
        }

        // Never completed a double gesture
        if (hasPreviousSwipe && originalDirection != newDirection) {
            if (hasPendingAction) doubleSwipeDisposable!!.dispose()
            directionReference.set(null)
            performAction(newDirection)
            return
        }

        directionReference.set(match(originalDirection, newDirection))

        if (hasPendingAction) doubleSwipeDisposable!!.dispose()

        doubleSwipeDisposable = timer(delayPercentageToMillis(doubleSwipePreference.value).toLong(), MILLISECONDS)
            .subscribe({
                val direction = directionReference.getAndSet(null) ?: return@subscribe
                performAction(direction)
            }, this::onError)
    }

    fun performAction(action: GestureAction) {
        val consumer = consumerForAction(action)
        consumer?.onGestureActionTriggered(action)
    }

    private fun performAction(direction: GestureDirection) {
        val action = directionToAction(direction)
        performAction(action)
    }

    private fun consumerForAction(action: GestureAction): GestureConsumer? {
        for (consumer in consumers.all) if (consumer.accepts(action)) return consumer
        return null
    }

    private fun onError(throwable: Throwable) = throwable.printStackTrace()

    private fun resetIsOngoing() {
        isSwipingDisposable = App.delay(ONGOING_RESET_DELAY.toLong(), SECONDS) { isOngoing = false }
    }

    private fun delayPercentageToMillis(percentage: Int): Int =
        (percentage * MAX_DOUBLE_SWIPE_DELAY / 100f).toInt()

    private fun rawToDirection(raw: Int): GestureDirection = when (raw) {
        FINGERPRINT_GESTURE_SWIPE_UP -> GestureDirection.Up
        FINGERPRINT_GESTURE_SWIPE_DOWN -> GestureDirection.Down
        FINGERPRINT_GESTURE_SWIPE_LEFT -> GestureDirection.Left
        FINGERPRINT_GESTURE_SWIPE_RIGHT -> GestureDirection.Right
        else -> GestureDirection.Up
    }

    private fun isSupportedAction(action: GestureAction): Boolean =
        if (action == GLOBAL_LOCK_SCREEN || action == GLOBAL_TAKE_SCREENSHOT) App.isPieOrHigher else true

    private fun directionToAction( direction: GestureDirection): GestureAction {
        if (direction.isDouble && purchasesManager.isNotPremium) return DO_NOTHING

        val id = directionPreferencesMap.getValue(direction).value
        return GestureAction.fromId(id)
    }

    private fun match(original: GestureDirection, updated: GestureDirection): GestureDirection {
        if (updated != original) return updated

        return when (updated) {
            GestureDirection.Up -> GestureDirection.DoubleUp
            GestureDirection.Down -> GestureDirection.DoubleDown
            GestureDirection.Left -> GestureDirection.DoubleLeft
            GestureDirection.Right -> GestureDirection.DoubleRight
            else -> updated
        }
    }

    private fun actionForResource(resource: Int): GestureAction = when (resource) {
        R.string.do_nothing -> DO_NOTHING
        R.string.increase_brightness -> INCREASE_BRIGHTNESS
        R.string.reduce_brightness -> REDUCE_BRIGHTNESS
        R.string.maximize_brightness -> MAXIMIZE_BRIGHTNESS
        R.string.minimize_brightness -> MINIMIZE_BRIGHTNESS
        R.string.increase_audio -> INCREASE_AUDIO
        R.string.reduce_audio -> REDUCE_AUDIO
        R.string.notification_up -> NOTIFICATION_UP
        R.string.notification_down -> NOTIFICATION_DOWN
        R.string.toggle_notifications -> NOTIFICATION_TOGGLE
        R.string.toggle_flashlight -> TOGGLE_FLASHLIGHT
        R.string.toggle_dock -> TOGGLE_DOCK
        R.string.toggle_auto_rotate -> TOGGLE_AUTO_ROTATE
        R.string.global_home -> GLOBAL_HOME
        R.string.global_back -> GLOBAL_BACK
        R.string.global_recents -> GLOBAL_RECENTS
        R.string.global_split_screen -> GLOBAL_SPLIT_SCREEN
        R.string.global_power_dialog -> GLOBAL_POWER_DIALOG
        R.string.global_lock_screen -> GLOBAL_LOCK_SCREEN
        R.string.global_take_screenshot -> GLOBAL_TAKE_SCREENSHOT
        R.string.show_popup -> SHOW_POPUP
        else -> DO_NOTHING
    }

    private fun getActionIds(): IntArray {
        val array = context.resources.obtainTypedArray(R.array.action_resources)
        val length = array.length()

        val ints = IntArray(length)
        for (i in 0 until length) ints[i] = array.getResourceId(i, R.string.do_nothing)

        array.recycle()

        return ints
    }

    companion object {
        private const val ONGOING_RESET_DELAY = 1
        private const val MAX_DOUBLE_SWIPE_DELAY = 1000
        private const val DEF_DOUBLE_SWIPE_DELAY_PERCENTAGE = 50
        private const val DOUBLE_SWIPE_DELAY = "double swipe delay"
    }
}
