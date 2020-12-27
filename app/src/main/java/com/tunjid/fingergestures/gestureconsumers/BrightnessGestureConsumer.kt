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
import com.tunjid.fingergestures.*
import com.tunjid.fingergestures.activities.BrightnessActivity
import com.tunjid.fingergestures.billing.PurchasesManager
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables
import java.math.BigDecimal
import java.util.*
import java.util.Comparator.naturalOrder
import java.util.Comparator.reverseOrder
import kotlin.math.max
import kotlin.math.min

class BrightnessGestureConsumer private constructor() : GestureConsumer {

    data class SliderPair(
        val value: Int,
        val enabled: Boolean,
    )

    data class DimmerState(
        val enabled: Boolean,
        val visible: Boolean,
        val checked: Boolean
    )

    data class State(
        val increment: SliderPair,
        val position: SliderPair,
        val adaptive: SliderPair,
        val dimmerState: DimmerState,
        val restoresAdaptiveBrightnessOnDisplaySleep: Boolean,
        val usesLogarithmicScale: Boolean,
        val shouldShowSlider: Boolean,
        val shouldAnimateSlider: Boolean,
        val discreteBrightnesses: List<Int>,
    )

    enum class Preference(override val preferenceName: String) : ListPreference {
        DiscreteBrightnesses(preferenceName = "discrete brightness values");
    }

    val percentagePreference: ReactivePreference<Int> = ReactivePreference(
        preferencesName = INCREMENT_VALUE,
        default = DEF_INCREMENT_VALUE
    )
    val positionPreference: ReactivePreference<Int> = ReactivePreference(
        preferencesName = SLIDER_POSITION,
        default = DEF_POSITION_VALUE
    )
    val adaptiveBrightnessThresholdPreference: ReactivePreference<Int> = ReactivePreference(
        preferencesName = ADAPTIVE_BRIGHTNESS_THRESHOLD,
        default = DEF_ADAPTIVE_BRIGHTNESS_THRESHOLD
    )
    val adaptiveBrightnessPreference: ReactivePreference<Boolean> = ReactivePreference(
        preferencesName = ADAPTIVE_BRIGHTNESS,
        default = true
    )
    val logarithmicBrightnessPreference: ReactivePreference<Boolean> = ReactivePreference(
        preferencesName = LOGARITHMIC_SCALE,
        default = App.isPieOrHigher
    )
    val showSliderPreference: ReactivePreference<Boolean> = ReactivePreference(
        preferencesName = SLIDER_VISIBLE,
        default = true
    )
    val animateSliderPreference: ReactivePreference<Boolean> = ReactivePreference(
        preferencesName = ANIMATES_SLIDER,
        default = true
    )
    val screenDimmerPercentPreference: ReactivePreference<Float> = ReactivePreference(
        preferencesName = SCREEN_DIMMER_DIM_PERCENT,
        default = DEF_DIM_PERCENT
    )
    val screenDimmerEnabledPreference: ReactivePreference<Boolean> = ReactivePreference(
        preferencesName = SCREEN_DIMMER_ENABLED,
        default = false,
        onSet = { enabled -> if (!enabled) removeDimmer() }
    )

    val discreteBrightnessManager: SetManager<Preference, Int> = SetManager(
        keys = Preference.values().toList(),
        sorter = Comparator(Int::compareTo),
        addFilter = { true },
        stringMapper = Integer::valueOf,
        objectMapper = Int::toString)

    var isDimmerEnabled: Boolean
        get() = (hasOverlayPermission()
            && PurchasesManager.instance.isPremium
            && App.transformApp({ app -> app.preferences.getBoolean(SCREEN_DIMMER_ENABLED, false) }, false))
        set(enabled) {
            App.withApp { app -> app.preferences.edit().putBoolean(SCREEN_DIMMER_ENABLED, enabled).apply() }
            if (!enabled) removeDimmer()
        }

    private val supportsAmbientThreshold = Flowables.combineLatest(
        PurchasesManager.instance.state,
        adaptiveBrightnessPreference.monitor
    ) { purchaseState, restores -> purchaseState.isPremium && restores && hasBrightnessSensor() }

    val state: Flowable<State> = Flowables.combineLatest(
        Flowables.combineLatest(
            discreteBrightnessManager.itemsFlowable(Preference.DiscreteBrightnesses),
            PurchasesManager.instance.state,
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
            PurchasesManager.instance.state,
            screenDimmerEnabledPreference.monitor,
        ) { purchaseState, dimmerEnabled ->
            DimmerState(
                enabled = purchaseState.isPremium,
                // TODO: Make reactive
                visible = hasOverlayPermission(),
                checked = dimmerEnabled
            )
        },
        adaptiveBrightnessPreference.monitor,
        logarithmicBrightnessPreference.monitor,
        showSliderPreference.monitor,
        animateSliderPreference.monitor,
        discreteBrightnessManager.itemsFlowable(Preference.DiscreteBrightnesses),
        ::State,
    ).replayingShare()

    override fun onGestureActionTriggered(gestureAction: GestureAction) {
        var byteValue: Int
        val originalValue: Int

        val app = App.instance ?: return

        byteValue = try {
            Settings.System.getInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            MAX_BRIGHTNESS.toInt()
        }

        originalValue = byteValue

        when (gestureAction) {
            GestureAction.INCREASE_BRIGHTNESS -> byteValue = increase(byteValue)
            GestureAction.REDUCE_BRIGHTNESS -> byteValue = reduce(byteValue)
            GestureAction.MAXIMIZE_BRIGHTNESS -> byteValue = MAX_BRIGHTNESS.toInt()
            GestureAction.MINIMIZE_BRIGHTNESS -> byteValue = MIN_BRIGHTNESS.toInt()
            else -> Unit
        }

        val intent = Intent(app, BrightnessActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        if (engagedDimmer(gestureAction, originalValue)) {
            byteValue = originalValue
            intent.action = ACTION_SCREEN_DIMMER_CHANGED
            intent.putExtra(SCREEN_DIMMER_DIM_PERCENT, screenDimmerPercentPreference.value)
            app.broadcast(intent)
        } else if (shouldRemoveDimmerOnChange(gestureAction)) removeDimmer()

        saveBrightness(byteValue)

        intent.putExtra(CURRENT_BRIGHTNESS_BYTE, byteValue)

        if (showSliderPreference.value) app.startActivity(intent)
    }

    @SuppressLint("SwitchIntDef")
    override fun accepts(gesture: GestureAction): Boolean = when (gesture) {
        GestureAction.INCREASE_BRIGHTNESS,
        GestureAction.REDUCE_BRIGHTNESS,
        GestureAction.MAXIMIZE_BRIGHTNESS,
        GestureAction.MINIMIZE_BRIGHTNESS -> true
        else -> false
    }

    fun saveBrightness(byteValue: Int) {
        if (!App.canWriteToSettings) return

        val contentResolver = App.transformApp((App::getContentResolver)) ?: return

        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, byteValue)
    }

    fun onScreenTurnedOn() {
        if (!App.canWriteToSettings) return

        val threshold = adaptiveThresholdToLux(adaptiveBrightnessThresholdPreference.value)
        val restoresAdaptiveBrightness = adaptiveBrightnessPreference.value
        val toggleAndLeave = restoresAdaptiveBrightness && PurchasesManager.instance.isNotPremium

        if (!restoresAdaptiveBrightness) return

        if (threshold == 0 || toggleAndLeave) {
            toggleAdaptiveBrightness(true)
            return
        }

        val sensorManager = App.transformApp { app -> app.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
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
            byteValue == MIN_BRIGHTNESS.toInt() && gestureAction == GestureAction.REDUCE_BRIGHTNESS -> {
                increaseScreenDimmer()
                true
            }
            gestureAction == GestureAction.INCREASE_BRIGHTNESS && screenDimmerPercentPreference.value > MIN_DIM_PERCENT -> {
                reduceScreenDimmer()
                true
            }
            else -> false
        }

    private fun reduceScreenDimmer() {
        val current = screenDimmerPercentPreference.value
        val changed = current - GestureConsumer.normalizePercentageToFraction(percentagePreference.value)
        screenDimmerPercentPreference.value = max(roundDown(changed), MIN_DIM_PERCENT)
    }

    private fun increaseScreenDimmer() {
        val current = screenDimmerPercentPreference.value
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

    fun hasOverlayPermission(): Boolean = App.transformApp(Settings::canDrawOverlays, false)

    fun shouldShowDimmer(): Boolean = screenDimmerPercentPreference.value != MIN_DIM_PERCENT

    fun getAdjustDeltaText(percentage: Int): String = App.transformApp({ app ->
        if (PurchasesManager.instance.isPremium && !noDiscreteBrightness())
            app.getString(R.string.delta_percent_premium, percentage)
        else
            app.getString(R.string.delta_percent, percentage)
    }, EMPTY_STRING)

    fun getAdaptiveBrightnessThresholdText(@IntRange(from = GestureConsumer.ZERO_PERCENT.toLong(), to = GestureConsumer.HUNDRED_PERCENT.toLong())
                                           percent: Int): String {
        if (!hasBrightnessSensor())
            return App.transformApp({ app -> app.getString(R.string.unavailable_brightness_sensor) }, EMPTY_STRING)

        if (PurchasesManager.instance.isNotPremium)
            return App.transformApp({ app -> app.getString(R.string.go_premium_text) }, EMPTY_STRING)

        if (!adaptiveBrightnessPreference.value)
            return App.transformApp({ app -> app.getString(R.string.adjust_adaptive_threshold_prompt) }, EMPTY_STRING)

        val lux = adaptiveThresholdToLux(percent)

        return App.transformApp({ app ->
            val descriptor = app.getString(when {
                lux < 50 -> R.string.adaptive_threshold_low
                lux < 1000 -> R.string.adaptive_threshold_medium
                else -> R.string.adaptive_threshold_high
            })

            app.getString(R.string.adaptive_threshold, lux, descriptor)
        }, EMPTY_STRING)
    }

    fun removeDimmer() {
        screenDimmerPercentPreference.value = MIN_DIM_PERCENT
        val intent = Intent(ACTION_SCREEN_DIMMER_CHANGED)
        intent.putExtra(SCREEN_DIMMER_DIM_PERCENT, screenDimmerPercentPreference.value)
        App.withApp { app -> app.broadcast(intent) }
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

        App.withApp { app -> Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, brightnessMode) }
    }

    private fun stringPercentageToBrightnessInt(stringPercent: String): Int {
        val percentage = java.lang.Float.valueOf(stringPercent)
        return percentToByte(percentage.toInt())
    }

    private fun hasBrightnessSensor(): Boolean {
        val sensorManager = App.transformApp { app -> app.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
            ?: return false

        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        return lightSensor != null
    }

    private fun shouldRemoveDimmerOnChange(gestureAction: GestureAction): Boolean =
        (gestureAction == GestureAction.MINIMIZE_BRIGHTNESS || gestureAction == GestureAction.MAXIMIZE_BRIGHTNESS
            || shouldShowDimmer() && PurchasesManager.instance.isNotPremium)

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
        private const val INCREMENT_VALUE = "increment value"
        private const val SLIDER_POSITION = "slider position"
        private const val SLIDER_VISIBLE = "slider visible"
        private const val LOGARITHMIC_SCALE = "logarithmic scale"
        private const val ADAPTIVE_BRIGHTNESS = "adaptive brightness"
        private const val ADAPTIVE_BRIGHTNESS_THRESHOLD = "adaptive brightness threshold"
        private const val SCREEN_DIMMER_ENABLED = "screen dimmer enabled"
        private const val SCREEN_DIMMER_DIM_PERCENT = "screen dimmer dim percent"
        private const val ANIMATES_SLIDER = "animates slider"
        private const val EMPTY_STRING = ""

        val instance: BrightnessGestureConsumer by lazy { BrightnessGestureConsumer() }

        private fun roundDown(d: Float): Float {
            var bd = BigDecimal(d.toString())
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_DOWN)
            return bd.toFloat()
        }
    }
}
