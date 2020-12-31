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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import androidx.annotation.IntRange
import com.jakewharton.rx.replayingShare
import com.tunjid.androidx.core.delegates.intentExtras
import com.tunjid.fingergestures.*
import com.tunjid.fingergestures.activities.BrightnessActivity
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.di.AppBroadcaster
import com.tunjid.fingergestures.di.AppContext
import com.tunjid.fingergestures.di.AppDisposable
import com.tunjid.fingergestures.models.Broadcast
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables
import java.math.BigDecimal
import java.util.*
import java.util.Comparator.naturalOrder
import java.util.Comparator.reverseOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class BrightnessGestureConsumer @Inject constructor(
    @AppContext private val context: Context,
    reactivePreferences: ReactivePreferences,
    private val broadcaster: AppBroadcaster,
    private val purchasesManager: PurchasesManager,
    appDisposable: AppDisposable
) : GestureConsumer {

    data class SliderPair(
        val value: Int,
        val enabled: Boolean,
    )

    data class DimmerState(
        val enabled: Boolean = false,
        val visible: Boolean = false,
        val checked: Boolean = false,
        val percentage: Float = 0f,
    )

    data class State(
        val increment: SliderPair = SliderPair(value = DEF_INCREMENT_VALUE, enabled = true),
        val position: SliderPair = SliderPair(value = DEF_POSITION_VALUE, enabled = true),
        val adaptive: SliderPair = SliderPair(value = DEF_ADAPTIVE_BRIGHTNESS_THRESHOLD, enabled = true),
        val dimmerState: DimmerState = DimmerState(),
        val restoresAdaptiveBrightnessOnDisplaySleep: Boolean = true,
        val usesLogarithmicScale: Boolean = App.isPieOrHigher,
        val shouldShowSlider: Boolean = true,
        val shouldAnimateSlider: Boolean = true,
        val discreteBrightnesses: List<Int> = listOf(),
    )

    enum class Preference(override val preferenceName: String) : SetPreference {
        DiscreteBrightnesses(preferenceName = "discrete brightness values");
    }

    val percentagePreference: ReactivePreference<Int> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = "increment value",
        default = DEF_INCREMENT_VALUE
    )
    val positionPreference: ReactivePreference<Int> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = "slider position",
        default = DEF_POSITION_VALUE
    )
    val adaptiveBrightnessThresholdPreference: ReactivePreference<Int> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = "adaptive brightness threshold",
        default = DEF_ADAPTIVE_BRIGHTNESS_THRESHOLD
    )
    val adaptiveBrightnessPreference: ReactivePreference<Boolean> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = "adaptive brightness",
        default = true
    )
    val logarithmicBrightnessPreference: ReactivePreference<Boolean> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = "logarithmic scale",
        default = App.isPieOrHigher
    )
    val showSliderPreference: ReactivePreference<Boolean> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = "slider visible",
        default = true
    )
    val animateSliderPreference: ReactivePreference<Boolean> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = "animates slider",
        default = true
    )
    val screenDimmerPercentPreference: ReactivePreference<Float> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = "screen dimmer dim percent",
        default = DEF_DIM_PERCENT
    )
    val screenDimmerEnabledPreference: ReactivePreference<Boolean> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = "screen dimmer enabled",
        default = false,
        onSet = { enabled -> if (!enabled) removeDimmer() }
    )
    val discreteBrightnessManager: SetManager<Preference, Int> = SetManager(
        reactivePreferences = reactivePreferences,
        keys = Preference.values().toList(),
        sorter = Comparator(Int::compareTo),
        addFilter = { true },
        stringMapper = Integer::valueOf,
        objectMapper = Int::toString
    )

    private val isDimmerEnabled: Boolean
        get() = hasOverlayPermission
            && purchasesManager.currentState.isPremium
            && currentState.dimmerState.checked

    private val hasOverlayPermission: Boolean
        get() = Settings.canDrawOverlays(context)

    private val supportsAmbientThreshold = Flowables.combineLatest(
        purchasesManager.state,
        adaptiveBrightnessPreference.monitor
    ) { purchaseState, restores -> purchaseState.isPremium && restores && hasBrightnessSensor() }

    val state: Flowable<State> = Flowables.combineLatest(
        Flowables.combineLatest(
            discreteBrightnessManager.itemsFlowable(Preference.DiscreteBrightnesses),
            purchasesManager.state,
            percentagePreference.monitor,

            ) { discreteBrightnesses, purchaseState, percentage ->
            SliderPair(value = percentage, enabled = purchaseState.isPremium || discreteBrightnesses.isEmpty())
        },
        Flowables.combineLatest(
            positionPreference.monitor,
            Flowable.just(true),
            ::SliderPair,
        ),
        Flowables.combineLatest(
            adaptiveBrightnessThresholdPreference.monitor,
            supportsAmbientThreshold,
            ::SliderPair,
        ),
        Flowables.combineLatest(
            purchasesManager.state,
            screenDimmerEnabledPreference.monitor,
            screenDimmerPercentPreference.monitor
        ) { purchaseState, dimmerEnabled, dimmerPercent ->
            DimmerState(
                enabled = purchaseState.isPremium,
                // TODO: Make reactive
                visible = hasOverlayPermission,
                checked = dimmerEnabled,
                percentage = dimmerPercent
            )
        },
        adaptiveBrightnessPreference.monitor,
        logarithmicBrightnessPreference.monitor,
        showSliderPreference.monitor,
        animateSliderPreference.monitor,
        discreteBrightnessManager.itemsFlowable(Preference.DiscreteBrightnesses),
        ::State,
    ).replayingShare()

    private val currentState by state.asProperty(State(), appDisposable::add)

    override fun onGestureActionTriggered(gestureAction: GestureAction) {
        var byteValue: Int
        val originalValue: Int

        byteValue = try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            MAX_BRIGHTNESS.toInt()
        }

        originalValue = byteValue

        when (gestureAction) {
            GestureAction.IncreaseBrightness -> byteValue = increase(byteValue)
            GestureAction.ReduceBrightness -> byteValue = reduce(byteValue)
            GestureAction.MaximizeBrightness -> byteValue = MAX_BRIGHTNESS.toInt()
            GestureAction.MinimizeBrightness -> byteValue = MIN_BRIGHTNESS.toInt()
            else -> Unit
        }

        val intent = Intent(context, BrightnessActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        if (engagedDimmer(gestureAction, originalValue)) {
            byteValue = originalValue
            // Read it directly, can't optimize by checking current
            val percent = screenDimmerPercentPreference.value
            intent.action = ACTION_SCREEN_DIMMER_CHANGED
            intent.screenDimmerPercent = percent
            broadcaster(Broadcast.Service.ScreenDimmerChanged(percent))
        } else if (shouldRemoveDimmerOnChange(gestureAction)) removeDimmer()

        saveBrightness(byteValue)

        intent.putExtra(CURRENT_BRIGHTNESS_BYTE, byteValue)

        if (showSliderPreference.value) context.startActivity(intent)
    }

    @SuppressLint("SwitchIntDef")
    override fun accepts(gesture: GestureAction): Boolean = when (gesture) {
        GestureAction.IncreaseBrightness,
        GestureAction.ReduceBrightness,
        GestureAction.MaximizeBrightness,
        GestureAction.MinimizeBrightness -> true
        else -> false
    }

    val percentageFormatter = { percent: Int -> context.getString(R.string.position_percent, percent) }

    fun saveBrightness(byteValue: Int) {
        if (!context.canWriteToSettings) return

        val contentResolver = context.contentResolver ?: return

        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, byteValue)
    }

    fun onScreenTurnedOn() {
        if (!context.canWriteToSettings) return

        val threshold = adaptiveThresholdToLux(adaptiveBrightnessThresholdPreference.value)
        val restoresAdaptiveBrightness = adaptiveBrightnessPreference.value
        val toggleAndLeave = restoresAdaptiveBrightness && !purchasesManager.currentState.isPremium

        if (!restoresAdaptiveBrightness) return

        if (threshold == 0 || toggleAndLeave) {
            toggleAdaptiveBrightness(true)
            return
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return

        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) ?: return

        sensorManager.registerListener(object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                sensorManager.unregisterListener(this, lightSensor)

                val values = event.values
                val lux = if (values != null && values.isNotEmpty()) values[0] else -1F
                val restoredBrightness = lux >= threshold

                toggleAdaptiveBrightness(restoredBrightness)
                if (restoredBrightness) removeDimmer()
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }, lightSensor, SensorManager.SENSOR_DELAY_UI)
    }

    private fun engagedDimmer(gestureAction: GestureAction, byteValue: Int): Boolean =
        when {
            !isDimmerEnabled -> false
            byteValue == MIN_BRIGHTNESS.toInt() && gestureAction == GestureAction.ReduceBrightness -> {
                increaseScreenDimmer()
                true
            }
            gestureAction == GestureAction.IncreaseBrightness && currentState.dimmerState.percentage > MIN_DIM_PERCENT -> {
                reduceScreenDimmer()
                true
            }
            else -> false
        }

    private fun reduceScreenDimmer() {
        val current = currentState.dimmerState.percentage
        val changed = current - GestureConsumer.normalizePercentageToFraction(percentagePreference.value)
        screenDimmerPercentPreference.value = max(roundDown(changed), MIN_DIM_PERCENT)
    }

    private fun increaseScreenDimmer() {
        val current = currentState.dimmerState.percentage
        val changed = current + GestureConsumer.normalizePercentageToFraction(percentagePreference.value)
        screenDimmerPercentPreference.value = min(roundDown(changed), MAX_DIM_PERCENT)
    }

    private fun reduce(byteValue: Int): Int =
        if (noDiscreteBrightness()) max(adjustByte(byteValue, false), MIN_BRIGHTNESS.toInt())
        else findDiscreteBrightnessValue(byteValue, false)

    private fun increase(byteValue: Int): Int =
        if (noDiscreteBrightness()) min(adjustByte(byteValue, true), MAX_BRIGHTNESS.toInt())
        else findDiscreteBrightnessValue(byteValue, true)

    private fun adjustByte(byteValue: Int, increase: Boolean): Int {
        val delta = percentagePreference.value
        var asPercentage = byteToPercentage(byteValue)

        asPercentage = if (increase) min(asPercentage + delta, 100)
        else max(asPercentage - delta, 0)

        return percentToByte(asPercentage)
    }

    fun shouldShowDimmer(): Boolean = currentState.dimmerState.percentage != MIN_DIM_PERCENT

    fun getAdjustDeltaText(percentage: Int): String = context.let { app ->
        if (purchasesManager.currentState.isPremium && !noDiscreteBrightness())
            app.getString(R.string.delta_percent_premium, percentage)
        else app.getString(R.string.delta_percent, percentage)
    }

    fun getAdaptiveBrightnessThresholdText(@IntRange(from = GestureConsumer.ZERO_PERCENT.toLong(), to = GestureConsumer.HUNDRED_PERCENT.toLong())
                                           percent: Int): String {
        if (!hasBrightnessSensor())
            return context.getString(R.string.unavailable_brightness_sensor)

        if (!purchasesManager.currentState.isPremium)
            return context.getString(R.string.go_premium_text)

        if (!adaptiveBrightnessPreference.value)
            return context.getString(R.string.adjust_adaptive_threshold_prompt)

        val lux = adaptiveThresholdToLux(percent)
        val descriptor = context.getString(when {
            lux < 50 -> R.string.adaptive_threshold_low
            lux < 1000 -> R.string.adaptive_threshold_medium
            else -> R.string.adaptive_threshold_high
        })

        return context.getString(R.string.adaptive_threshold, lux, descriptor)
    }

    fun removeDimmer() {
        screenDimmerPercentPreference.value = MIN_DIM_PERCENT
        broadcaster(Broadcast.Service.ScreenDimmerChanged(MIN_DIM_PERCENT))
    }

    fun percentToByte(percentage: Int): Int =
        if (logarithmicBrightnessPreference.value) BrightnessLookup.lookup(percentage, false)
        else (percentage * MAX_BRIGHTNESS / 100).toInt()

    fun byteToPercentage(byteValue: Int): Int =
        if (logarithmicBrightnessPreference.value) BrightnessLookup.lookup(byteValue, true)
        else (byteValue * 100 / MAX_BRIGHTNESS).toInt()

    private fun adaptiveThresholdToLux(percentage: Int): Int =
        (percentage * MAX_ADAPTIVE_THRESHOLD / 100f).toInt()

    private fun findDiscreteBrightnessValue(byteValue: Int, increasing: Boolean): Int {
        val evaluated = if (byteValue < FUZZ_THRESHOLD) byteValue else if (increasing) byteValue + 2 else byteValue - 2
        val alternative = (if (increasing) MAX_BRIGHTNESS else MIN_BRIGHTNESS).toInt()
        val comparator = if (increasing) naturalOrder() else reverseOrder<Int>()
        val filter = { integer: Int -> if (increasing) integer > evaluated else integer < evaluated }

        return discreteBrightnessManager.getSet(Preference.DiscreteBrightnesses).stream()
            .map { this.stringPercentageToBrightnessInt(it) }
            .filter(filter::invoke).min(comparator).orElse(alternative)
    }

    private fun toggleAdaptiveBrightness(on: Boolean) {
        val brightnessMode =
            if (on) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL

        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, brightnessMode)
    }

    private fun stringPercentageToBrightnessInt(stringPercent: String): Int {
        val percentage = java.lang.Float.valueOf(stringPercent)
        return percentToByte(percentage.toInt())
    }

    private fun hasBrightnessSensor(): Boolean {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return false

        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        return lightSensor != null
    }

    private fun shouldRemoveDimmerOnChange(gestureAction: GestureAction): Boolean =
        (gestureAction == GestureAction.MinimizeBrightness || gestureAction == GestureAction.MaximizeBrightness
            || shouldShowDimmer() && !purchasesManager.currentState.isPremium)

    private fun noDiscreteBrightness(): Boolean =
        discreteBrightnessManager.getSet(Preference.DiscreteBrightnesses).isEmpty()

    companion object {

        private const val FUZZ_THRESHOLD = 15
        private const val MIN_BRIGHTNESS = 0f
        private const val MIN_DIM_PERCENT = 0f
        private const val MAX_DIM_PERCENT = .8f
        private const val MAX_BRIGHTNESS = 255f
        private const val DEF_DIM_PERCENT = MIN_DIM_PERCENT
        private const val MAX_ADAPTIVE_THRESHOLD = 1200
        private const val DEF_INCREMENT_VALUE = 20
        private const val DEF_POSITION_VALUE = 50
        private const val DEF_ADAPTIVE_BRIGHTNESS_THRESHOLD = 50

        const val CURRENT_BRIGHTNESS_BYTE = "brightness value"
        const val ACTION_SCREEN_DIMMER_CHANGED = "show screen dimmer"

        private fun roundDown(d: Float): Float {
            var bd = BigDecimal(d.toString())
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_DOWN)
            return bd.toFloat()
        }
    }
}

var Intent.screenDimmerPercent by intentExtras<Float?>()