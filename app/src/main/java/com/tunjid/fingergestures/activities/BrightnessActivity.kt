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
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.tunjid.androidx.core.content.drawableAt
import com.tunjid.androidx.core.graphics.drawable.withTint
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.databinding.ActivityBrightnessBinding
import com.tunjid.fingergestures.filter
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer.Companion.CURRENT_BRIGHTNESS_BYTE
import com.tunjid.fingergestures.mapDistinct
import com.tunjid.fingergestures.viewmodels.BrightnessViewModel
import com.tunjid.fingergestures.viewmodels.In
import com.tunjid.fingergestures.viewmodels.State

class BrightnessActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener {
    private val viewModel by viewModels<BrightnessViewModel>()
    private val binding by lazy { ActivityBrightnessBinding.inflate(layoutInflater) }
    private val ownerAndRegistry = lifecycleOwnerAndRegistry()
    private val dialogLifecycleOwner = ownerAndRegistry.first
    private val dialogLifecycleRegistry = ownerAndRegistry.second

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        dialogLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        window.apply {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val controls = binding.controls
        controls.seekbar.setOnSeekBarChangeListener(this)

        viewModel.state.apply {
            mapDistinct(State::sliderColor)
                .observe(dialogLifecycleOwner) { sliderColor ->
                    controls.seekbarText.setTextColor(sliderColor)
                    controls.seekbar.thumb.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(sliderColor, BlendModeCompat.SRC_IN)
                    controls.seekbar.progressDrawable.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(sliderColor, BlendModeCompat.SRC_IN)
                    controls.goToSettings.setImageDrawable(drawableAt(R.drawable.ic_settings_white_24dp)
                        ?.withTint(sliderColor))
                }
            mapDistinct(State::backgroundColor)
                .observe(dialogLifecycleOwner) { backgroundColor ->
                    controls.seekbarBackground.background = drawableAt(R.drawable.color_indicator)
                        ?.withTint(backgroundColor)
                }
            mapDistinct(State::verticalBias)
                .observe(dialogLifecycleOwner) { verticalBias ->
                    val layout = binding.constraintLayout

                    val set = ConstraintSet()
                    set.clone(layout)
                    set.setVerticalBias(controls.seekbarBackground.id, verticalBias)
                    set.applyTo(layout)
                }
            mapDistinct(State::showDimmerText)
                .observe(dialogLifecycleOwner, controls.seekbarText::isVisible::set)

            filter(State::showDimmerText)
                .mapDistinct(State::dimmerPercent)
                .observe(dialogLifecycleOwner) {
                    binding.controls.seekbarText.text = getString(R.string.screen_dimmer_value, it)
                }
            mapDistinct(State::initialProgress)
                .observe(dialogLifecycleOwner) {
                    binding.controls.seekbar.setProgress(it, true)
                }
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

    override fun onDestroy() {
        dialogLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        when (viewModel.state.value?.animateSlide) {
            true -> overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_down)
            else -> overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, percentage: Int, fromUser: Boolean) {
        if (!fromUser) return

        viewModel.accept(In.Change(percentage))

        if (binding.controls.seekbarText.visibility != View.VISIBLE) return

        TransitionManager.beginDelayedTransition(binding.controls.seekbarBackground, AutoTransition())
        binding.controls.seekbarText.isVisible = false
    }

    private fun handleIntent(intent: Intent) {
        TransitionManager.beginDelayedTransition(binding.controls.seekbarBackground, AutoTransition())
        val brightnessByte = intent.getIntExtra(CURRENT_BRIGHTNESS_BYTE, 0)
        viewModel.accept(In.Start(brightnessByte))
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) = viewModel.accept(In.RemoveDimmer)

    override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
}

private fun lifecycleOwnerAndRegistry() = with(object : LifecycleOwner {
    val registry = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle = registry
}) { this to registry }