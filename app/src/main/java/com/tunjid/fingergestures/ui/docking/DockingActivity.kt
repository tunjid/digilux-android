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

package com.tunjid.fingergestures.ui.docking

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.ui.dialogLifecycleOwner
import com.tunjid.fingergestures.databinding.ActivityDockingBinding
import com.tunjid.fingergestures.di.viewModelFactory
import com.tunjid.fingergestures.mapDistinct
import java.util.concurrent.TimeUnit.MILLISECONDS

class DockingActivity : AppCompatActivity() {
    private var wasMultiWindowMode: Boolean = false

    private val binding by lazy { ActivityDockingBinding.inflate(layoutInflater) }
    private val viewModel by viewModelFactory<DockingViewModel>()
    private val dialogLifecycleOwner by lazy { dialogLifecycleOwner() }

    private val dockingAnimationDuration: Int
        get() = resources.getInteger(R.integer.docking_animation_duration)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wasMultiWindowMode = savedInstanceState != null && savedInstanceState.getBoolean(CONFIGURATION_CHANGE_KEY)
        setContentView(binding.root)

        binding.logo.isVisible = isInMultiWindowMode

        viewModel.state.apply {
            mapDistinct(State::backgroundColor)
                .observe(dialogLifecycleOwner, binding.constraintLayout::setBackgroundColor)
        }

        handleIntent(false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(true)
    }

    override fun onResume() {
        super.onResume()
        overridePendingTransition(R.anim.slide_in_down_quick, R.anim.slide_out_up)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(CONFIGURATION_CHANGE_KEY, isInMultiWindowMode)
        super.onSaveInstanceState(outState)
    }

    private fun handleIntent(fromNewIntent: Boolean) {
        if (isInMultiWindowMode && !fromNewIntent) return
        if (wasMultiWindowMode)
            finish()
        else
            App.delay(dockingAnimationDuration.toLong(), MILLISECONDS, viewModel::toggleDock)
    }

    companion object {

        const val CONFIGURATION_CHANGE_KEY = "isConfigurationChange"
    }
}
