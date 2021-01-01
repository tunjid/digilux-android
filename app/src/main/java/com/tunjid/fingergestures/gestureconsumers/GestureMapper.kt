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
import com.tunjid.fingergestures.di.AppBroadcasts
import com.tunjid.fingergestures.di.AppContext
import com.tunjid.fingergestures.di.AppDisposable
import com.tunjid.fingergestures.di.GestureConsumers
import com.tunjid.fingergestures.managers.PurchasesManager
import com.tunjid.fingergestures.models.Broadcast
import io.reactivex.Flowable
import io.reactivex.Scheduler
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

private val GestureAction.isSupported: Boolean
    get() = when (this) {
        GestureAction.GlobalLockScreen,
        GestureAction.GlobalTakeScreenshot -> App.isPieOrHigher
        else -> true
    }

@Singleton
class GestureMapper @Inject constructor(
    @AppContext private val context: Context,
    reactivePreferences: ReactivePreferences,
    private val purchasesManager: PurchasesManager,
    private val consumers: GestureConsumers,
    broadcasts: AppBroadcasts,
    appDisposable: AppDisposable,
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
                    singleActionName = context.getString(singleAction.nameRes),
                    doubleActionName = context.getString(doubleAction.nameRes),
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

    private val gestures = PublishProcessor.create<GestureDirection>()
    private val currentState by state.asProperty(
        default = State(),
        disposableHandler = appDisposable::add
    )

    val supportedActions: Flowable<List<GestureAction>>
        get() = Flowable.just(
            GestureAction.values()
                .toList()
                .filter(GestureAction::isSupported)
        )

    init {
        broadcasts.filterIsInstance<Broadcast.Gesture>()
            .map(Broadcast.Gesture::gesture)
            .subscribe(this@GestureMapper::performAction)
            .addTo(appDisposable)

        object : GestureProcessor {
            override val scheduler: Scheduler = Schedulers.io()

            override val delayMillis: Long
                get() = currentState.doubleSwipeDelayMillis

            override val GestureDirection.hasDoubleAction: Boolean
                get() = currentState.pairFor(this).doubleAction != GestureAction.DoNothing

            init {
                gestures.processed
                    .subscribe(::performAction)
                    .addTo(appDisposable)
            }
        }
    }

    fun mapGestureToAction(direction: GestureDirection, action: GestureAction) {
        directionPreferencesMap.getValue(direction).value = action.id
    }

    fun getSwipeDelayText(percentage: Int): String =
        context.getString(when {
            purchasesManager.currentState.isPremium -> R.string.double_swipe_delay
            else -> R.string.go_premium_text
        }, delayPercentageToMillis(percentage))

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

    companion object {
        private const val MAX_DOUBLE_SWIPE_DELAY = 1000L
        private const val DEF_DOUBLE_SWIPE_DELAY_PERCENTAGE = 50
    }
}
