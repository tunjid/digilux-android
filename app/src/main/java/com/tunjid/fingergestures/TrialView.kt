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

package com.tunjid.fingergestures


import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.Window.FEATURE_OPTIONS_PANEL
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.tunjid.androidx.core.content.unwrapActivity
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.billing.TrialStatus
import com.tunjid.fingergestures.databinding.TrialViewBinding
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.Disposable

@SuppressLint("ViewConstructor")
class TrialView(context: Context, menuItem: MenuItem) : FrameLayout(context) {

    private val binding = TrialViewBinding.inflate(LayoutInflater.from(context), this, true)
    private var disposable: Disposable? = null

    init {
        val clickListener = { _: View ->
            context.unwrapActivity?.onMenuItemSelected(FEATURE_OPTIONS_PANEL, menuItem)
            Unit
        }

        setOnClickListener(clickListener)
        binding.icon.setOnClickListener(clickListener)

        disposable = PurchasesManager.instance.state
            .observeOn(mainThread())
            .subscribe(::update, Throwable::printStackTrace)

        CheatSheet.setup(this, menuItem.title)
    }

    override fun onDetachedFromWindow() {
        if (disposable != null) disposable!!.dispose()
        super.onDetachedFromWindow()
    }

    private fun update(state: PurchasesManager.State) {
        binding.icon.isVisible = !state.isOnTrial
        binding.text.isVisible = state.isOnTrial

        if (state.trialStatus is TrialStatus.Trial) binding.text.text = state.trialStatus.countDown.toString()

    }
}
