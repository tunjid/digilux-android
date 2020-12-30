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
import android.content.Context
import com.jakewharton.rx.replayingShare
import com.tunjid.fingergestures.*
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.di.AppBroadcasts
import com.tunjid.fingergestures.di.AppContext
import com.tunjid.fingergestures.di.GestureConsumers
import com.tunjid.fingergestures.gestureconsumers.ext.GestureProcessor
import com.tunjid.fingergestures.gestureconsumers.ext.double
import com.tunjid.fingergestures.models.Broadcast
import com.tunjid.fingergestures.viewmodels.filterIsInstance
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
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

@Singleton
class GestureMapper @Inject constructor(
    @AppContext private val context: Context,
    reactivePreferences: ReactivePreferences,
    private val purchasesManager: PurchasesManager,
    private val consumers: GestureConsumers,
    broadcasts: AppBroadcasts
) : FingerprintGestureController.FingerprintGestureCallback() {

    data class GesturePair(
        val singleGestureName: String = "",
        val doubleGestureName: String = "",
        val singleActionName: String = "",
        val doubleActionName: String = "",
        val singleAction: GestureAction = GestureAction.DoNothing,
        val doubleAction: GestureAction = GestureAction.DoNothing,
    )

    data class State(
        val left: GesturePair = GesturePair(),
        val up: GesturePair = GesturePair(),
        val right: GesturePair = GesturePair(),
        val down: GesturePair = GesturePair(),
        val doubleSwipeDelayMillis: Long = MAX_DOUBLE_SWIPE_DELAY,
    )

    val doubleSwipePreference: ReactivePreference<Int> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = "double swipe delay",
        default = DEF_DOUBLE_SWIPE_DELAY_PERCENTAGE
    )

    private val directionPreferencesMap = GestureDirection.values().map { gestureDirection ->
        gestureDirection to ReactivePreference(
            reactivePreferences = reactivePreferences,
            key = gestureDirection.direction,
            default = when (gestureDirection) {
                GestureDirection.Up -> GestureAction.NotificationUp
                GestureDirection.Down -> GestureAction.NotificationDown
                GestureDirection.Left -> GestureAction.ReduceBrightness
                GestureDirection.Right -> GestureAction.IncreaseBrightness
                else -> GestureAction.DoNothing
            }.id
        )
    }
        .toMap()

    private val GestureDirection.pairedState
        get() = purchasesManager.state
            .map(PurchasesManager.State::isPremium)
            .distinctUntilChanged()
            .switchMap { isPremium ->
                Flowables.combineLatest(
                    directionPreferencesMap.getValue(this).monitor.map {
                        GestureAction.fromId(it)
                    },
                    directionPreferencesMap.getValue(this.double).monitor.map {
                        if (isPremium) GestureAction.fromId(it)
                        else GestureAction.DoNothing
                    }
                )
            }.map { (singleAction, doubleAction) ->
                GesturePair(
                    singleGestureName = context.getString(this.stringRes),
                    doubleGestureName = context.getString(double.stringRes),
                    singleActionName = context.getString(singleAction.resource),
                    doubleActionName = context.getString(doubleAction.resource),
                    singleAction = singleAction,
                    doubleAction = doubleAction,
                )
            }

    val state: Flowable<State> = Flowable.combineLatest(
        GestureDirection.Left.pairedState,
        GestureDirection.Up.pairedState,
        GestureDirection.Right.pairedState,
        GestureDirection.Down.pairedState,
        doubleSwipePreference.monitor.map(::delayPercentageToMillis),
        ::State
    ).replayingShare()

    private val actionIds: IntArray
    private val gestures = PublishProcessor.create<GestureDirection>()
    private val disposables = CompositeDisposable()
    private val currentState by state.asProperty(
        default = State(),
        disposableHandler = disposables::add
    )

    val actions: List<GestureAction>
        get() = actionIds.map(::actionForResource)
            .filter(::isSupportedAction)

    init {
        actionIds = getActionIds()
        broadcasts.filterIsInstance<Broadcast.Gesture>()
            .map(Broadcast.Gesture::gesture)
            .subscribe(this@GestureMapper::performAction)
            .addTo(disposables)

        object : GestureProcessor {
            override val scheduler: Scheduler = Schedulers.io()

            override val delayMillis: Long
                get() = currentState.doubleSwipeDelayMillis

            override val GestureDirection.hasDoubleAction: Boolean
                get() = currentState.pairFor(this).doubleAction != GestureAction.DoNothing

            init {
                gestures.processed
                    .subscribe(::performAction)
                    .addTo(disposables)
            }
        }
    }

    fun mapGestureToAction(direction: GestureDirection, action: GestureAction) {
        directionPreferencesMap.getValue(direction).value = action.id
    }

    fun getSwipeDelayText(percentage: Int): String =
        context.getString(when {
            purchasesManager.isNotPremium -> R.string.go_premium_text
            else -> R.string.double_swipe_delay
        }, delayPercentageToMillis(percentage))

    fun clear() = disposables.clear()

    override fun onGestureDetected(raw: Int) {
        super.onGestureDetected(raw)
        gestures.onNext(rawToDirection(raw))
    }

    fun performAction(action: GestureAction) {
        consumers.all
            .firstOrNull { it.accepts(action) }
            ?.onGestureActionTriggered(action)
    }

    private fun performAction(direction: GestureDirection) =
        performAction(currentState.actionFor(direction))

    private fun delayPercentageToMillis(percentage: Int): Long =
        (percentage * MAX_DOUBLE_SWIPE_DELAY / 100f).toLong()

    private fun rawToDirection(raw: Int): GestureDirection = when (raw) {
        FINGERPRINT_GESTURE_SWIPE_UP -> GestureDirection.Up
        FINGERPRINT_GESTURE_SWIPE_DOWN -> GestureDirection.Down
        FINGERPRINT_GESTURE_SWIPE_LEFT -> GestureDirection.Left
        FINGERPRINT_GESTURE_SWIPE_RIGHT -> GestureDirection.Right
        else -> GestureDirection.Up
    }

    private fun State.pairFor(direction: GestureDirection) = when (direction) {
        GestureDirection.Up -> up
        GestureDirection.Down -> down
        GestureDirection.Left -> left
        GestureDirection.Right -> right
        GestureDirection.DoubleUp -> up
        GestureDirection.DoubleDown -> down
        GestureDirection.DoubleLeft -> left
        GestureDirection.DoubleRight -> right
    }

    private fun State.actionFor(direction: GestureDirection) = when (direction) {
        GestureDirection.Up -> up.singleAction
        GestureDirection.Down -> down.singleAction
        GestureDirection.Left -> left.singleAction
        GestureDirection.Right -> right.singleAction
        GestureDirection.DoubleUp -> up.doubleAction
        GestureDirection.DoubleDown -> down.doubleAction
        GestureDirection.DoubleLeft -> left.doubleAction
        GestureDirection.DoubleRight -> right.doubleAction
    }

    private fun isSupportedAction(action: GestureAction): Boolean =
        if (action == GestureAction.GlobalLockScreen || action == GestureAction.GlobalTakeScreenshot) App.isPieOrHigher else true

    private fun actionForResource(resource: Int): GestureAction = when (resource) {
        R.string.do_nothing -> GestureAction.DoNothing
        R.string.increase_brightness -> GestureAction.IncreaseBrightness
        R.string.reduce_brightness -> GestureAction.ReduceBrightness
        R.string.maximize_brightness -> GestureAction.MaximizeBrightness
        R.string.minimize_brightness -> GestureAction.MinimizeBrightness
        R.string.increase_audio -> GestureAction.IncreaseAudio
        R.string.reduce_audio -> GestureAction.ReduceAudio
        R.string.notification_up -> GestureAction.NotificationUp
        R.string.notification_down -> GestureAction.NotificationDown
        R.string.toggle_notifications -> GestureAction.NotificationToggle
        R.string.toggle_flashlight -> GestureAction.FlashlightToggle
        R.string.toggle_dock -> GestureAction.DockToggle
        R.string.toggle_auto_rotate -> GestureAction.AutoRotateToggle
        R.string.global_home -> GestureAction.GlobalHome
        R.string.global_back -> GestureAction.GlobalBack
        R.string.global_recents -> GestureAction.GlobalRecents
        R.string.global_split_screen -> GestureAction.GlobalSplitScreen
        R.string.global_power_dialog -> GestureAction.GlobalPowerDialog
        R.string.global_lock_screen -> GestureAction.GlobalLockScreen
        R.string.global_take_screenshot -> GestureAction.GlobalTakeScreenshot
        R.string.show_popup -> GestureAction.PopUpShow
        else -> GestureAction.DoNothing
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
        private const val MAX_DOUBLE_SWIPE_DELAY = 1000L
        private const val DEF_DOUBLE_SWIPE_DELAY_PERCENTAGE = 50
    }
}
