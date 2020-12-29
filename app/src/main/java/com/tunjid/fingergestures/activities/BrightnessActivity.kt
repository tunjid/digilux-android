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
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.dynamicanimation.animation.SpringForce
import androidx.dynamicanimation.animation.springAnimationOf
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import com.tunjid.androidx.core.content.drawableAt
import com.tunjid.androidx.core.graphics.drawable.withTint
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.activities.main.activeOnCreateLifecycleOwner
import com.tunjid.fingergestures.databinding.ActivityBrightnessBinding
import com.tunjid.fingergestures.di.viewModelFactory
import com.tunjid.fingergestures.filter
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer.Companion.CURRENT_BRIGHTNESS_BYTE
import com.tunjid.fingergestures.map
import com.tunjid.fingergestures.mapDistinct
import com.tunjid.fingergestures.viewmodels.BrightnessViewModel
import com.tunjid.fingergestures.viewmodels.In
import com.tunjid.fingergestures.viewmodels.State

class BrightnessActivity : AppCompatActivity() {
    private val viewModel by viewModelFactory<BrightnessViewModel>()
    private val binding by lazy { ActivityBrightnessBinding.inflate(layoutInflater) }
    private val dialogLifecycleOwner = activeOnCreateLifecycleOwner()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window.apply {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val controls = binding.controls
        val sliderSpring = springAnimationOf(
            controls.slider::setValue,
            controls.slider::getValue
        ).setSpring(SpringForce(100f)
            .setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY)
        )

        controls.slider.labelBehavior = LabelFormatter.LABEL_GONE
        controls.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.accept(In.Change(value.toInt()))
        }
        controls.slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) = viewModel.accept(In.RemoveDimmer)

            override fun onStopTrackingTouch(slider: Slider) = Unit
        })

        viewModel.state.apply {
            mapDistinct(State::sliderColor)
                .observe(dialogLifecycleOwner) { sliderColor ->
                    controls.slider.thumbTintList = ColorStateList.valueOf(sliderColor)
                    controls.slider.trackTintList = ColorStateList.valueOf(sliderColor)
                    controls.goToSettings.setImageDrawable(drawableAt(R.drawable.ic_settings_white_24dp)
                        ?.withTint(sliderColor))
                }
            mapDistinct(State::backgroundColor)
                .observe(dialogLifecycleOwner) { backgroundColor ->
                    controls.sliderBackground.background = drawableAt(R.drawable.color_indicator)
                        ?.withTint(backgroundColor)
                }
            mapDistinct(State::verticalBias)
                .observe(dialogLifecycleOwner) { verticalBias ->
                    val layout = binding.constraintLayout

                    val set = ConstraintSet()
                    set.clone(layout)
                    set.setVerticalBias(controls.sliderBackground.id, verticalBias)
                    set.applyTo(layout)
                }
            mapDistinct(State::showDimmerText)
                .observe(dialogLifecycleOwner, {
                    TransitionManager.beginDelayedTransition(binding.controls.sliderBackground, AutoTransition())
                    controls.sliderText.isVisible = it
                })

            filter(State::showDimmerText)
                .mapDistinct(State::dimmerPercent)
                .observe(dialogLifecycleOwner) {
                    binding.controls.sliderText.text = getString(R.string.screen_dimmer_value, it)
                }
            mapDistinct(State::initialProgress)
                .map(Int::toFloat)
                .observe(dialogLifecycleOwner, sliderSpring::animateToFinalPosition)

            filter(State::completed)
                .observe(dialogLifecycleOwner) { finish() }
        }

        binding.constraintLayout.setOnClickListener { finish() }
        controls.goToSettings.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.state.value?.animateSlide == true)
            overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up)
    }

    override fun finish() {
        super.finish()
        when (viewModel.state.value?.animateSlide) {
            true -> overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_down)
            else -> overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun handleIntent(intent: Intent) {
        val brightnessByte = intent.getIntExtra(CURRENT_BRIGHTNESS_BYTE, 0)
        viewModel.accept(In.Start(brightnessByte))
    }
}
