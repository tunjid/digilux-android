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
import android.accessibilityservice.FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_DOWN
import android.accessibilityservice.FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_LEFT
import android.accessibilityservice.FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_RIGHT
import android.accessibilityservice.FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_UP
import androidx.annotation.StringDef
import androidx.annotation.StringRes
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.PopUpGestureConsumer
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.ReactivePreference
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.DO_NOTHING
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.GLOBAL_BACK
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.GLOBAL_HOME
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.GLOBAL_LOCK_SCREEN
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.GLOBAL_POWER_DIALOG
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.GLOBAL_RECENTS
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.GLOBAL_SPLIT_SCREEN
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.GLOBAL_TAKE_SCREENSHOT
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.INCREASE_AUDIO
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.INCREASE_BRIGHTNESS
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.MAXIMIZE_BRIGHTNESS
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.MINIMIZE_BRIGHTNESS
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.NOTIFICATION_DOWN
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.NOTIFICATION_TOGGLE
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.NOTIFICATION_UP
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.REDUCE_AUDIO
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.REDUCE_BRIGHTNESS
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.SHOW_POPUP
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.TOGGLE_AUTO_ROTATE
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.TOGGLE_DOCK
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.TOGGLE_FLASHLIGHT
import io.reactivex.Flowable
import io.reactivex.Flowable.timer
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicReference

class GestureMapper private constructor() : FingerprintGestureController.FingerprintGestureCallback() {

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
            preferencesName = DOUBLE_SWIPE_DELAY,
            default = DEF_DOUBLE_SWIPE_DELAY_PERCENTAGE
    )

    private val directionPreferencesMap = allGestures.map { gestureDirection ->
        gestureDirection to ReactivePreference(
                preferencesName = gestureDirection,
                default = when (gestureDirection) {
                    UP_GESTURE -> NOTIFICATION_UP
                    DOWN_GESTURE -> NOTIFICATION_DOWN
                    LEFT_GESTURE -> REDUCE_BRIGHTNESS
                    RIGHT_GESTURE -> INCREASE_BRIGHTNESS
                    else -> DO_NOTHING
                }
        )
    }
            .toMap()

    val directionPreferencesFlowable: Flowable<State>
        get() = Flowable.combineLatest(
                directionPreferencesMap
                        .entries
                        .map { (gestureDirection, preference) ->
                            preference.monitor.map {
                                val resource = resourceForAction(
                                        if (isDouble(gestureDirection) && PurchasesManager.instance.isNotPremium) DO_NOTHING
                                        else it
                                )

                                gestureDirection to App.transformApp({ app -> app.getString(resource) }, "")
                            }
                        }) { items ->
            val map = (items.toList() as List<Pair<String, String>>).toMap()
            State(
                    left = GesturePair(
                            singleGestureName = getDirectionName(LEFT_GESTURE),
                            doubleGestureName = getDirectionName(DOUBLE_LEFT_GESTURE),
                            singleActionName = map.getValue(LEFT_GESTURE),
                            doubleActionName = map.getValue(DOUBLE_LEFT_GESTURE)
                    ),
                    up = GesturePair(
                            singleGestureName = getDirectionName(UP_GESTURE),
                            doubleGestureName = getDirectionName(DOUBLE_UP_GESTURE),
                            singleActionName = map.getValue(UP_GESTURE),
                            doubleActionName = map.getValue(DOUBLE_UP_GESTURE)
                    ),
                    right = GesturePair(
                            singleGestureName = getDirectionName(RIGHT_GESTURE),
                            doubleGestureName = getDirectionName(DOUBLE_RIGHT_GESTURE),
                            singleActionName = map.getValue(RIGHT_GESTURE),
                            doubleActionName = map.getValue(DOUBLE_RIGHT_GESTURE)
                    ),
                    down = GesturePair(
                            singleGestureName = getDirectionName(DOWN_GESTURE),
                            doubleGestureName = getDirectionName(DOUBLE_DOWN_GESTURE),
                            singleActionName = map.getValue(DOWN_GESTURE),
                            doubleActionName = map.getValue(DOUBLE_DOWN_GESTURE)
                    ),
            )
        }


    private val actionIds: IntArray
    private val consumers: Array<GestureConsumer> = arrayOf(
            NothingGestureConsumer.instance,
            BrightnessGestureConsumer.instance,
            NotificationGestureConsumer.instance,
            FlashlightGestureConsumer.instance,
            DockingGestureConsumer.instance,
            RotationGestureConsumer.instance,
            GlobalActionGestureConsumer.instance,
            PopUpGestureConsumer.instance,
            AudioGestureConsumer.instance)

    private val directionReference: AtomicReference<String> = AtomicReference()

    private var isSwipingDisposable: Disposable? = null
    private var doubleSwipeDisposable: Disposable? = null
    private var isOngoing: Boolean = false

    val actions: IntArray
        get() = actionIds.map(this::actionForResource)
                .filter(this::isSupportedAction)
                .toIntArray()

    var doubleSwipeDelay: Int by doubleSwipePreference.delegate

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(UP_GESTURE, DOWN_GESTURE, LEFT_GESTURE, RIGHT_GESTURE, DOUBLE_UP_GESTURE, DOUBLE_DOWN_GESTURE, DOUBLE_LEFT_GESTURE, DOUBLE_RIGHT_GESTURE)
    annotation class GestureDirection

    init {
        actionIds = getActionIds()
    }

    fun mapGestureToAction(@GestureDirection direction: String, @GestureConsumer.GestureAction action: Int) {
        directionPreferencesMap.getValue(direction).value = action
    }

    fun getMappedAction(@GestureDirection gestureDirection: String): String {
        @GestureConsumer.GestureAction
        val action = directionToAction(gestureDirection)
        val stringResource = resourceForAction(action)
        return App.transformApp({ app -> app.getString(stringResource) }, "")
    }

    @GestureDirection
    fun doubleDirection(@GestureDirection direction: String): String {
        return match(direction, direction)
    }

    fun getSwipeDelayText(percentage: Int): String {
        return App.transformApp({ app ->
            app.getString(if (PurchasesManager.instance.isNotPremium)
                R.string.go_premium_text
            else
                R.string.double_swipe_delay, delayPercentageToMillis(percentage))
        }, "")
    }

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
        if (hasPreviousSwipe && hasPendingAction && isDouble(originalDirection!!)) {
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

        doubleSwipeDisposable = timer(delayPercentageToMillis(doubleSwipeDelay).toLong(), MILLISECONDS)
                .subscribe({
                    val direction = directionReference.getAndSet(null) ?: return@subscribe
                    performAction(direction)
                }, this::onError)
    }

    fun performAction(@GestureConsumer.GestureAction action: Int) {
        val consumer = consumerForAction(action)
        consumer?.onGestureActionTriggered(action)
    }

    private fun performAction(@GestureDirection direction: String) {
        @GestureConsumer.GestureAction
        val action = directionToAction(direction)
        performAction(action)
    }

    private fun consumerForAction(@GestureConsumer.GestureAction action: Int): GestureConsumer? {
        for (consumer in consumers) if (consumer.accepts(action)) return consumer
        return null
    }

    private fun onError(throwable: Throwable) = throwable.printStackTrace()

    private fun resetIsOngoing() {
        isSwipingDisposable = App.delay(ONGOING_RESET_DELAY.toLong(), SECONDS) { isOngoing = false }
    }

    private fun delayPercentageToMillis(percentage: Int): Int =
            (percentage * MAX_DOUBLE_SWIPE_DELAY / 100f).toInt()

    @GestureDirection
    private fun rawToDirection(raw: Int): String = when (raw) {
        FINGERPRINT_GESTURE_SWIPE_UP -> UP_GESTURE
        FINGERPRINT_GESTURE_SWIPE_DOWN -> DOWN_GESTURE
        FINGERPRINT_GESTURE_SWIPE_LEFT -> LEFT_GESTURE
        FINGERPRINT_GESTURE_SWIPE_RIGHT -> RIGHT_GESTURE
        else -> UP_GESTURE
    }

    private fun isSupportedAction(@GestureConsumer.GestureAction action: Int): Boolean =
            if (action == GLOBAL_LOCK_SCREEN || action == GLOBAL_TAKE_SCREENSHOT) App.isPieOrHigher else true

    @GestureConsumer.GestureAction
    private fun directionToAction(@GestureDirection direction: String): Int {
        if (isDouble(direction) && PurchasesManager.instance.isNotPremium) return DO_NOTHING

        val gesture = directionPreferencesMap.getValue(direction).value
        if (gesture != UNASSIGNED_GESTURE) return gesture

        // Defaults
        return when (direction) {
            UP_GESTURE -> NOTIFICATION_UP
            DOWN_GESTURE -> NOTIFICATION_DOWN
            LEFT_GESTURE -> REDUCE_BRIGHTNESS
            RIGHT_GESTURE -> INCREASE_BRIGHTNESS
            else -> DO_NOTHING
        }
    }

    private fun match(@GestureDirection original: String?, @GestureDirection updated: String): String {
        if (updated != original) return updated

        return when (updated) {
            UP_GESTURE -> DOUBLE_UP_GESTURE
            DOWN_GESTURE -> DOUBLE_DOWN_GESTURE
            LEFT_GESTURE -> DOUBLE_LEFT_GESTURE
            RIGHT_GESTURE -> DOUBLE_RIGHT_GESTURE
            else -> updated
        }
    }

    private fun isDouble(direction: String): Boolean = when (direction) {
        DOUBLE_UP_GESTURE, DOUBLE_DOWN_GESTURE, DOUBLE_LEFT_GESTURE, DOUBLE_RIGHT_GESTURE -> true
        else -> false
    }

    @GestureConsumer.GestureAction
    private fun actionForResource(resource: Int): Int = when (resource) {
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

    @StringRes
    fun resourceForAction(@GestureConsumer.GestureAction action: Int): Int = when (action) {
        DO_NOTHING -> R.string.do_nothing
        INCREASE_BRIGHTNESS -> R.string.increase_brightness
        REDUCE_BRIGHTNESS -> R.string.reduce_brightness
        MAXIMIZE_BRIGHTNESS -> R.string.maximize_brightness
        MINIMIZE_BRIGHTNESS -> R.string.minimize_brightness
        INCREASE_AUDIO -> R.string.increase_audio
        REDUCE_AUDIO -> R.string.reduce_audio
        NOTIFICATION_UP -> R.string.notification_up
        NOTIFICATION_DOWN -> R.string.notification_down
        NOTIFICATION_TOGGLE -> R.string.toggle_notifications
        TOGGLE_FLASHLIGHT -> R.string.toggle_flashlight
        TOGGLE_DOCK -> R.string.toggle_dock
        TOGGLE_AUTO_ROTATE -> R.string.toggle_auto_rotate
        GLOBAL_HOME -> R.string.global_home
        GLOBAL_BACK -> R.string.global_back
        GLOBAL_RECENTS -> R.string.global_recents
        GLOBAL_SPLIT_SCREEN -> R.string.global_split_screen
        GLOBAL_POWER_DIALOG -> R.string.global_power_dialog
        GLOBAL_LOCK_SCREEN -> R.string.global_lock_screen
        GLOBAL_TAKE_SCREENSHOT -> R.string.global_take_screenshot
        SHOW_POPUP -> R.string.show_popup
        else -> R.string.do_nothing
    }

    fun getDirectionName(@GestureDirection direction: String): String = when (direction) {
        UP_GESTURE -> App.transformApp({ app -> app.getString(R.string.swipe_up) }, "")
        DOWN_GESTURE -> App.transformApp({ app -> app.getString(R.string.swipe_down) }, "")
        LEFT_GESTURE -> App.transformApp({ app -> app.getString(R.string.swipe_left) }, "")
        RIGHT_GESTURE -> App.transformApp({ app -> app.getString(R.string.swipe_right) }, "")
        DOUBLE_UP_GESTURE -> App.transformApp({ app -> app.getString(R.string.double_swipe_up) }, "")
        DOUBLE_DOWN_GESTURE -> App.transformApp({ app -> app.getString(R.string.double_swipe_down) }, "")
        DOUBLE_LEFT_GESTURE -> App.transformApp({ app -> app.getString(R.string.double_swipe_left) }, "")
        DOUBLE_RIGHT_GESTURE -> App.transformApp({ app -> app.getString(R.string.double_swipe_right) }, "")
        else -> ""
    }

    private fun getActionIds(): IntArray {
        return App.transformApp({ app ->
            val array = app.resources.obtainTypedArray(R.array.action_resources)
            val length = array.length()

            val ints = IntArray(length)
            for (i in 0 until length) ints[i] = array.getResourceId(i, R.string.do_nothing)

            array.recycle()

            ints
        }, IntArray(0))
    }

    companion object {

        private const val UNASSIGNED_GESTURE = -1
        private const val ONGOING_RESET_DELAY = 1
        private const val MAX_DOUBLE_SWIPE_DELAY = 1000
        private const val DEF_DOUBLE_SWIPE_DELAY_PERCENTAGE = 50

        const val UP_GESTURE = "up gesture"
        const val DOWN_GESTURE = "down gesture"
        const val LEFT_GESTURE = "left gesture"
        const val RIGHT_GESTURE = "right gesture"
        const val DOUBLE_UP_GESTURE = "double up gesture"
        const val DOUBLE_DOWN_GESTURE = "double down gesture"
        const val DOUBLE_LEFT_GESTURE = "double left gesture"
        const val DOUBLE_RIGHT_GESTURE = "double right gesture"
        private const val DOUBLE_SWIPE_DELAY = "double swipe delay"

        val instance: GestureMapper by lazy { GestureMapper() }

    }
}

private val allGestures = listOf(
        GestureMapper.UP_GESTURE,
        GestureMapper.DOWN_GESTURE,
        GestureMapper.LEFT_GESTURE,
        GestureMapper.RIGHT_GESTURE,
        GestureMapper.DOUBLE_UP_GESTURE,
        GestureMapper.DOUBLE_DOWN_GESTURE,
        GestureMapper.DOUBLE_LEFT_GESTURE,
        GestureMapper.DOUBLE_RIGHT_GESTURE
)