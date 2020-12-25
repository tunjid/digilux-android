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

package com.tunjid.fingergestures.activities

import android.content.Intent
import android.graphics.PorterDuff.Mode.SRC_IN
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer.Companion.CURRENT_BRIGHTNESS_BYTE

class BrightnessActivity : TimedActivity(), SeekBar.OnSeekBarChangeListener {

    private var brightnessByte: Int = 0

    private lateinit var seekBar: SeekBar
    private lateinit var seekBarText: TextView
    private lateinit var seekBarBackground: ViewGroup

    private lateinit var brightnessGestureConsumer: BrightnessGestureConsumer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_brightness)

        brightnessGestureConsumer = BrightnessGestureConsumer.instance

        val sliderColor = backgroundManager.sliderColorPreference.value
        val sliderBackgroundColor = backgroundManager.backgroundColorPreference.value

        val layout = findViewById<ConstraintLayout>(R.id.constraint_layout)
        seekBarBackground = findViewById(R.id.seekbar_background)
        seekBar = findViewById(R.id.seekbar)
        seekBarText = findViewById(R.id.seekbar_text)
        val settingsIcon = findViewById<ImageView>(R.id.go_to_settings)

        seekBarText.setTextColor(sliderColor)
        seekBar.thumb.setColorFilter(sliderColor, SRC_IN)
        seekBar.progressDrawable.setColorFilter(sliderColor, SRC_IN)
        settingsIcon.setImageDrawable(backgroundManager.tint(R.drawable.ic_settings_white_24dp, sliderColor))
        seekBarBackground.background = backgroundManager.tint(R.drawable.color_indicator, sliderBackgroundColor)

        val set = ConstraintSet()
        set.clone(layout)
        set.setVerticalBias(seekBarBackground.id, brightnessGestureConsumer.positionPreference.value / 100f)
        set.applyTo(layout)

        layout.setOnClickListener { finish() }
        settingsIcon.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (brightnessGestureConsumer.animateSliderPreference.value)
            overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up)
    }

    override fun finish() {
        super.finish()

        val animateSlide = brightnessGestureConsumer.animateSliderPreference.value
        if (animateSlide)
            overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_down)
        else
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onProgressChanged(seekBar: SeekBar, percentage: Int, fromUser: Boolean) {
        var value = percentage
        updateEndTime()

        if (!fromUser) return
        if (value == 100) value--

        brightnessByte = brightnessGestureConsumer.percentToByte(value)
        brightnessGestureConsumer.saveBrightness(brightnessByte)

        if (seekBarText.visibility != View.VISIBLE) return

        TransitionManager.beginDelayedTransition(seekBarBackground, AutoTransition())
        seekBarText.visibility = View.GONE
    }

    private fun handleIntent(intent: Intent) {
        TransitionManager.beginDelayedTransition(seekBarBackground, AutoTransition())

        brightnessByte = intent.getIntExtra(CURRENT_BRIGHTNESS_BYTE, 0)
        val percentage = brightnessGestureConsumer.byteToPercentage(brightnessByte)

        seekBar.setProgress(percentage, true)
        seekBar.setOnSeekBarChangeListener(this)

        val showDimmer = brightnessGestureConsumer.shouldShowDimmer()
        seekBarText.visibility = if (showDimmer) View.VISIBLE else View.GONE

        if (showDimmer) {
            val dimmerPercent = brightnessGestureConsumer.screenDimmerPercentPreference.value * 100f
            seekBarText.text = getString(R.string.screen_dimmer_value, dimmerPercent)
        }

        waitToFinish()
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        brightnessGestureConsumer.removeDimmer()
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {}
}
