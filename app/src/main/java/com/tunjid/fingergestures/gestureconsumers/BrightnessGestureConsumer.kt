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
import android.provider.Settings.System.SCREEN_BRIGHTNESS
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.BrightnessLookup
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.SetManager
import com.tunjid.fingergestures.activities.BrightnessActivity
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.normalizePercentageToFraction
import java.math.BigDecimal
import java.util.*
import java.util.Comparator.naturalOrder
import java.util.Comparator.reverseOrder
import kotlin.math.max
import kotlin.math.min

class BrightnessGestureConsumer private constructor() : GestureConsumer {

    private val discreteBrightnessManager: SetManager<Int> = SetManager(
            Comparator(Int::compareTo),
            { true },
            Integer::valueOf,
            Int::toString)

    var incrementPercentage: Int
        @IntRange(from = GestureConsumer.ZERO_PERCENT.toLong(), to = GestureConsumer.HUNDRED_PERCENT.toLong())
        get() = App.transformApp({ app -> app.preferences.getInt(INCREMENT_VALUE, DEF_INCREMENT_VALUE) }, DEF_INCREMENT_VALUE)
        set(@IntRange(from = GestureConsumer.ZERO_PERCENT.toLong(), to = GestureConsumer.HUNDRED_PERCENT.toLong())
            incrementValue) {
            App.withApp { app -> app.preferences.edit().putInt(INCREMENT_VALUE, incrementValue).apply() }
        }

    var positionPercentage: Int
        @IntRange(from = GestureConsumer.ZERO_PERCENT.toLong(), to = GestureConsumer.HUNDRED_PERCENT.toLong())
        get() = App.transformApp({ app -> app.preferences.getInt(SLIDER_POSITION, DEF_POSITION_VALUE) }, DEF_POSITION_VALUE)
        set(@IntRange(from = GestureConsumer.ZERO_PERCENT.toLong(), to = GestureConsumer.HUNDRED_PERCENT.toLong())
            positionPercentage) {
            App.withApp { app -> app.preferences.edit().putInt(SLIDER_POSITION, positionPercentage).apply() }
        }

    var adaptiveBrightnessThreshold: Int
        @IntRange(from = GestureConsumer.ZERO_PERCENT.toLong(), to = GestureConsumer.HUNDRED_PERCENT.toLong())
        get() = App.transformApp({ app -> app.preferences.getInt(ADAPTIVE_BRIGHTNESS_THRESHOLD, DEF_ADAPTIVE_BRIGHTNESS_THRESHOLD) }, DEF_ADAPTIVE_BRIGHTNESS_THRESHOLD)
        set(@IntRange(from = GestureConsumer.ZERO_PERCENT.toLong(), to = GestureConsumer.HUNDRED_PERCENT.toLong())
            threshold) {
            App.withApp { app -> app.preferences.edit().putInt(ADAPTIVE_BRIGHTNESS_THRESHOLD, threshold).apply() }
        }

    val screenDimmerDimPercent: Float
        get() = App.transformApp({ app -> app.preferences.getFloat(SCREEN_DIMMER_DIM_PERCENT, DEF_DIM_PERCENT) }, DEF_DIM_PERCENT)

    var isDimmerEnabled: Boolean
        get() = (hasOverlayPermission()
                && PurchasesManager.getInstance().isPremium
                && App.transformApp({ app -> app.preferences.getBoolean(SCREEN_DIMMER_ENABLED, false) }, false))
        set(enabled) {
            App.withApp { app -> app.preferences.edit().putBoolean(SCREEN_DIMMER_ENABLED, enabled).apply() }
            if (!enabled) removeDimmer()
        }

    val discreteBrightnessValues: List<String>
        get() = discreteBrightnessManager.getList(DISCRETE_BRIGHTNESS_SET)

    override fun onGestureActionTriggered(@GestureConsumer.GestureAction gestureAction: Int) {
        var byteValue: Int
        val originalValue: Int

        val app = App.instance ?: return

        byteValue = try {
            Settings.System.getInt(app.contentResolver, SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            MAX_BRIGHTNESS.toInt()
        }

        originalValue = byteValue

        when (gestureAction) {
            GestureConsumer.INCREASE_BRIGHTNESS -> byteValue = increase(byteValue)
            GestureConsumer.REDUCE_BRIGHTNESS -> byteValue = reduce(byteValue)
            GestureConsumer.MAXIMIZE_BRIGHTNESS -> byteValue = MAX_BRIGHTNESS.toInt()
            GestureConsumer.MINIMIZE_BRIGHTNESS -> byteValue = MIN_BRIGHTNESS.toInt()
        }

        val intent = Intent(app, BrightnessActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        if (engagedDimmer(gestureAction, originalValue)) {
            byteValue = originalValue
            intent.action = ACTION_SCREEN_DIMMER_CHANGED
            intent.putExtra(SCREEN_DIMMER_DIM_PERCENT, screenDimmerDimPercent)
            app.broadcast(intent)
        } else if (shouldRemoveDimmerOnChange(gestureAction)) {
            removeDimmer()
        }

        saveBrightness(byteValue)

        intent.putExtra(CURRENT_BRIGHTNESS_BYTE, byteValue)

        if (shouldShowSlider()) app.startActivity(intent)
    }

    @SuppressLint("SwitchIntDef")
    override fun accepts(@GestureConsumer.GestureAction gesture: Int): Boolean {
        return when (gesture) {
            GestureConsumer.INCREASE_BRIGHTNESS, GestureConsumer.REDUCE_BRIGHTNESS, GestureConsumer.MAXIMIZE_BRIGHTNESS, GestureConsumer.MINIMIZE_BRIGHTNESS -> true
            else -> false
        }
    }

    fun saveBrightness(byteValue: Int) {
        if (!App.canWriteToSettings()) return

        val contentResolver = App.transformApp((App::getContentResolver)) ?: return

        Settings.System.putInt(contentResolver, SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL)
        Settings.System.putInt(contentResolver, SCREEN_BRIGHTNESS, byteValue)
    }

    fun onScreenTurnedOn() {
        if (!App.canWriteToSettings()) return

        val threshold = adaptiveThresholdToLux(adaptiveBrightnessThreshold)
        val restoresAdaptiveBrightness = restoresAdaptiveBrightnessOnDisplaySleep()
        val toggleAndLeave = restoresAdaptiveBrightness && PurchasesManager.getInstance().isNotPremium

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

    private fun engagedDimmer(@GestureConsumer.GestureAction gestureAction: Int, byteValue: Int): Boolean {
        if (!isDimmerEnabled) return false
        if (byteValue == MIN_BRIGHTNESS.toInt() && gestureAction == GestureConsumer.REDUCE_BRIGHTNESS) {
            increaseScreenDimmer()
            return true
        } else if (gestureAction == GestureConsumer.INCREASE_BRIGHTNESS && screenDimmerDimPercent > MIN_DIM_PERCENT) {
            reduceScreenDimmer()
            return true
        }
        return false
    }

    private fun reduceScreenDimmer() {
        val current = screenDimmerDimPercent
        val changed = current - normalizePercentageToFraction(incrementPercentage)
        setDimmerPercent(max(roundDown(changed), MIN_DIM_PERCENT))
    }

    private fun increaseScreenDimmer() {
        val current = screenDimmerDimPercent
        val changed = current + normalizePercentageToFraction(incrementPercentage)
        setDimmerPercent(min(roundDown(changed), MAX_DIM_PERCENT))
    }

    private fun reduce(byteValue: Int): Int {
        return if (noDiscreteBrightness()) max(adjustByte(byteValue, false), MIN_BRIGHTNESS.toInt())
        else findDiscreteBrightnessValue(byteValue, false)
    }

    private fun increase(byteValue: Int): Int {
        return if (noDiscreteBrightness()) min(adjustByte(byteValue, true), MAX_BRIGHTNESS.toInt())
        else findDiscreteBrightnessValue(byteValue, true)
    }

    private fun adjustByte(byteValue: Int, increase: Boolean): Int {
        val delta = incrementPercentage
        var asPercentage = byteToPercentage(byteValue)
        if (increase)
            asPercentage = Math.min(asPercentage + delta, 100)
        else
            asPercentage = Math.max(asPercentage - delta, 0)

        return percentToByte(asPercentage)
    }

    private fun setDimmerPercent(@FloatRange(from = GestureConsumer.ZERO_PERCENT.toDouble(), to = GestureConsumer.HUNDRED_PERCENT.toDouble())
                                 percentage: Float) {
        App.withApp { app -> app.preferences.edit().putFloat(SCREEN_DIMMER_DIM_PERCENT, percentage).apply() }
    }

    fun shouldRestoreAdaptiveBrightnessOnDisplaySleep(restore: Boolean) {
        App.withApp { app -> app.preferences.edit().putBoolean(ADAPTIVE_BRIGHTNESS, restore).apply() }
    }

    fun shouldUseLogarithmicScale(isLogarithmic: Boolean) {
        App.withApp { app -> app.preferences.edit().putBoolean(LOGARITHMIC_SCALE, isLogarithmic).apply() }
    }

    fun setSliderVisible(visible: Boolean) {
        App.withApp { app -> app.preferences.edit().putBoolean(SLIDER_VISIBLE, visible).apply() }
    }

    fun setAnimatesSlider(visible: Boolean) {
        App.withApp { app -> app.preferences.edit().putBoolean(ANIMATES_SLIDER, visible).apply() }
    }

    fun restoresAdaptiveBrightnessOnDisplaySleep(): Boolean {
        return App.transformApp({ app -> app.preferences.getBoolean(ADAPTIVE_BRIGHTNESS, false) }, false)
    }

    fun usesLogarithmicScale(): Boolean {
        return App.transformApp({ app -> app.preferences.getBoolean(LOGARITHMIC_SCALE, App.isPieOrHigher) }, App.isPieOrHigher)
    }

    fun supportsAmbientThreshold(): Boolean {
        return (PurchasesManager.getInstance().isPremium
                && restoresAdaptiveBrightnessOnDisplaySleep()
                && hasBrightnessSensor())
    }

    fun hasOverlayPermission(): Boolean {
        return App.transformApp(Settings::canDrawOverlays, false)
    }

    fun shouldShowDimmer(): Boolean {
        return screenDimmerDimPercent != MIN_DIM_PERCENT
    }

    fun shouldShowSlider(): Boolean {
        return App.transformApp({ app -> app.preferences.getBoolean(SLIDER_VISIBLE, true) }, true)
    }

    fun shouldAnimateSlider(): Boolean {
        return App.transformApp({ app -> app.preferences.getBoolean(ANIMATES_SLIDER, true) }, true)
    }

    fun canAdjustDelta(): Boolean {
        return noDiscreteBrightness() || PurchasesManager.getInstance().isPremium
    }

    fun addDiscreteBrightnessValue(discreteValue: String) {
        discreteBrightnessManager.addToSet(discreteValue, DISCRETE_BRIGHTNESS_SET)
    }

    fun removeDiscreteBrightnessValue(discreteValue: String) {
        discreteBrightnessManager.removeFromSet(discreteValue, DISCRETE_BRIGHTNESS_SET)
    }

    fun getAdjustDeltaText(percentage: Int): String {
        return App.transformApp({ app ->
            if (PurchasesManager.getInstance().isPremium && !noDiscreteBrightness())
                app.getString(R.string.delta_percent_premium, percentage)
            else
                app.getString(R.string.delta_percent, percentage)
        }, EMPTY_STRING)
    }

    fun getAdaptiveBrightnessThresholdText(@IntRange(from = GestureConsumer.ZERO_PERCENT.toLong(), to = GestureConsumer.HUNDRED_PERCENT.toLong())
                                           percent: Int): String {
        if (!hasBrightnessSensor())
            return App.transformApp({ app -> app.getString(R.string.unavailable_brightness_sensor) }, EMPTY_STRING)

        if (PurchasesManager.getInstance().isNotPremium)
            return App.transformApp({ app -> app.getString(R.string.go_premium_text) }, EMPTY_STRING)

        if (!restoresAdaptiveBrightnessOnDisplaySleep())
            return App.transformApp({ app -> app.getString(R.string.adjust_adaptive_threshold_prompt) }, EMPTY_STRING)

        val lux = adaptiveThresholdToLux(percent)

        return App.transformApp({ app ->
            val descriptor = app.getString(if (lux < 50)
                R.string.adaptive_threshold_low
            else if (lux < 1000)
                R.string.adaptive_threshold_medium
            else
                R.string.adaptive_threshold_high)

            app.getString(R.string.adaptive_threshold, lux, descriptor)
        }, EMPTY_STRING)
    }

    fun removeDimmer() {
        setDimmerPercent(MIN_DIM_PERCENT)
        val intent = Intent(ACTION_SCREEN_DIMMER_CHANGED)
        intent.putExtra(SCREEN_DIMMER_DIM_PERCENT, screenDimmerDimPercent)
        App.withApp { app -> app.broadcast(intent) }
    }

    fun percentToByte(percentage: Int): Int {
        return if (usesLogarithmicScale())
            BrightnessLookup.lookup(percentage, false)
        else
            (percentage * MAX_BRIGHTNESS / 100).toInt()
    }

    fun byteToPercentage(byteValue: Int): Int {
        return if (usesLogarithmicScale())
            BrightnessLookup.lookup(byteValue, true)
        else
            (byteValue * 100 / MAX_BRIGHTNESS).toInt()
    }

    private fun adaptiveThresholdToLux(percentage: Int): Int {
        return (percentage * MAX_ADAPTIVE_THRESHOLD / 100f).toInt()
    }

    private fun findDiscreteBrightnessValue(byteValue: Int, increasing: Boolean): Int {
        val evaluated = if (byteValue < FUZZ_THRESHOLD) byteValue else if (increasing) byteValue + 2 else byteValue - 2
        val alternative = (if (increasing) MAX_BRIGHTNESS else MIN_BRIGHTNESS).toInt()
        val comparator = if (increasing) naturalOrder() else reverseOrder<Int>()
        val filter = { integer: Int -> if (increasing) integer > evaluated else integer < evaluated }

        return discreteBrightnessManager.getSet(DISCRETE_BRIGHTNESS_SET).stream()
                .map { this.stringPercentageToBrightnessInt(it) }
                .filter(filter::invoke).min(comparator).orElse(alternative)
    }

    private fun toggleAdaptiveBrightness(on: Boolean) {
        val brightnessMode =
                if (on) SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                else SCREEN_BRIGHTNESS_MODE_MANUAL

        App.withApp { app -> Settings.System.putInt(app.contentResolver, SCREEN_BRIGHTNESS_MODE, brightnessMode) }
    }

    private fun stringPercentageToBrightnessInt(stringPercent: String): Int {
        val percentage = java.lang.Float.valueOf(stringPercent)
        return percentToByte(percentage.toInt())
    }

    private fun hasBrightnessSensor(): Boolean {
        val sensorManager = App.transformApp { app -> app.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
                ?: return false

        val lightSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_LIGHT)
        return lightSensor != null
    }

    private fun shouldRemoveDimmerOnChange(@GestureConsumer.GestureAction gestureAction: Int): Boolean {
        return (gestureAction == GestureConsumer.MINIMIZE_BRIGHTNESS || gestureAction == GestureConsumer.MAXIMIZE_BRIGHTNESS
                || shouldShowDimmer() && PurchasesManager.getInstance().isNotPremium)
    }

    private fun noDiscreteBrightness(): Boolean {
        return discreteBrightnessManager.getSet(DISCRETE_BRIGHTNESS_SET).isEmpty()
    }

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
        private const val DISCRETE_BRIGHTNESS_SET = "discrete brightness values"
        private const val EMPTY_STRING = ""

        val instance: BrightnessGestureConsumer by lazy { BrightnessGestureConsumer() }

        private fun roundDown(d: Float): Float {
            var bd = BigDecimal(d.toString())
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_DOWN)
            return bd.toFloat()
        }
    }
}
