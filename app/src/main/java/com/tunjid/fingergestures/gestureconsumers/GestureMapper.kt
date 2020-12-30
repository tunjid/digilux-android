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
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.ReactivePreference
import com.tunjid.fingergestures.ReactivePreferences
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.di.AppBroadcasts
import com.tunjid.fingergestures.di.AppContext
import com.tunjid.fingergestures.di.GestureConsumers
import com.tunjid.fingergestures.gestureconsumers.ext.GestureProcessor
import com.tunjid.fingergestures.gestureconsumers.ext.double
import com.tunjid.fingergestures.gestureconsumers.ext.isDouble
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
) : FingerprintGestureController.FingerprintGestureCallback(), GestureProcessor {

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
        key = "double swipe delay",
        default = DEF_DOUBLE_SWIPE_DELAY_PERCENTAGE
    )

    private val directionPreferencesMap = GestureDirection.values().map { gestureDirection ->
        gestureDirection to ReactivePreference(
            reactivePreferences = reactivePreferences,
            key = gestureDirection.direction,
            default = when (gestureDirection) {
                GestureDirection.Up -> GestureAction.NOTIFICATION_UP
                GestureDirection.Down -> GestureAction.NOTIFICATION_DOWN
                GestureDirection.Left -> GestureAction.REDUCE_BRIGHTNESS
                GestureDirection.Right -> GestureAction.INCREASE_BRIGHTNESS
                else -> GestureAction.DO_NOTHING
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
                        context.getString(GestureAction.fromId(it).resource)
                    },
                    directionPreferencesMap.getValue(this.double).monitor.map {
                        context.getString(when {
                            isPremium -> GestureAction.fromId(it)
                            else -> GestureAction.DO_NOTHING
                        }.resource)
                    }
                )
            }.map { (singleName, doubleName) ->
                GesturePair(
                    singleGestureName = context.getString(this.stringRes),
                    doubleGestureName = context.getString(double.stringRes),
                    singleActionName = singleName,
                    doubleActionName = doubleName
                )
            }

    val directionPreferencesFlowable: Flowable<State>
        get() = Flowable.combineLatest(
            GestureDirection.Left.pairedState,
            GestureDirection.Up.pairedState,
            GestureDirection.Right.pairedState,
            GestureDirection.Down.pairedState,
            ::State
        )

    private val actionIds: IntArray
    private val gestureProcessor = PublishProcessor.create<GestureDirection>()
    private val disposables = CompositeDisposable()

    val actions: List<GestureAction>
        get() = actionIds.map(::actionForResource)
            .filter(::isSupportedAction)

    override val scheduler: Scheduler
        get() = Schedulers.io()

    override val delayMillis: Long
        get() = delayPercentageToMillis(doubleSwipePreference.value).toLong()

    override val GestureDirection.hasDoubleAction: Boolean
        get() = directionToAction(this.double) != GestureAction.DO_NOTHING

    init {
        actionIds = getActionIds()
        broadcasts.filterIsInstance<Broadcast.Gesture>()
            .map(Broadcast.Gesture::gesture)
            .subscribe(this@GestureMapper::performAction)
            .addTo(disposables)

        gestureProcessor.processed
            .subscribe(::performAction)
            .addTo(disposables)
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
        gestureProcessor.onNext(rawToDirection(raw))
    }

    fun performAction(action: GestureAction) {
        consumers.all
            .firstOrNull { it.accepts(action) }
            ?.onGestureActionTriggered(action)
    }

    private fun performAction(direction: GestureDirection) =
        performAction(directionToAction(direction))

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
        if (action == GestureAction.GLOBAL_LOCK_SCREEN || action == GestureAction.GLOBAL_TAKE_SCREENSHOT) App.isPieOrHigher else true

    private fun directionToAction(direction: GestureDirection): GestureAction {
        if (direction.isDouble && purchasesManager.isNotPremium) return GestureAction.DO_NOTHING

        val id = directionPreferencesMap.getValue(direction).value
        return GestureAction.fromId(id)
    }

    private fun actionForResource(resource: Int): GestureAction = when (resource) {
        R.string.do_nothing -> GestureAction.DO_NOTHING
        R.string.increase_brightness -> GestureAction.INCREASE_BRIGHTNESS
        R.string.reduce_brightness -> GestureAction.REDUCE_BRIGHTNESS
        R.string.maximize_brightness -> GestureAction.MAXIMIZE_BRIGHTNESS
        R.string.minimize_brightness -> GestureAction.MINIMIZE_BRIGHTNESS
        R.string.increase_audio -> GestureAction.INCREASE_AUDIO
        R.string.reduce_audio -> GestureAction.REDUCE_AUDIO
        R.string.notification_up -> GestureAction.NOTIFICATION_UP
        R.string.notification_down -> GestureAction.NOTIFICATION_DOWN
        R.string.toggle_notifications -> GestureAction.NOTIFICATION_TOGGLE
        R.string.toggle_flashlight -> GestureAction.TOGGLE_FLASHLIGHT
        R.string.toggle_dock -> GestureAction.TOGGLE_DOCK
        R.string.toggle_auto_rotate -> GestureAction.TOGGLE_AUTO_ROTATE
        R.string.global_home -> GestureAction.GLOBAL_HOME
        R.string.global_back -> GestureAction.GLOBAL_BACK
        R.string.global_recents -> GestureAction.GLOBAL_RECENTS
        R.string.global_split_screen -> GestureAction.GLOBAL_SPLIT_SCREEN
        R.string.global_power_dialog -> GestureAction.GLOBAL_POWER_DIALOG
        R.string.global_lock_screen -> GestureAction.GLOBAL_LOCK_SCREEN
        R.string.global_take_screenshot -> GestureAction.GLOBAL_TAKE_SCREENSHOT
        R.string.show_popup -> GestureAction.SHOW_POPUP
        else -> GestureAction.DO_NOTHING
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
        private const val MAX_DOUBLE_SWIPE_DELAY = 1000
        private const val DEF_DOUBLE_SWIPE_DELAY_PERCENTAGE = 50
    }
}
