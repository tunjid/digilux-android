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

package com.tunjid.fingergestures.viewholders

import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.Switch

import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.AppAdapter
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer

import android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION

class ScreenDimmerViewHolder(itemView: View, listener: AppAdapter.AppAdapterListener) : AppViewHolder(itemView, listener) {

    private val purchasesManager: PurchasesManager = PurchasesManager.getInstance()
    private val brightnessGestureConsumer: BrightnessGestureConsumer = BrightnessGestureConsumer.getInstance()
    private val goToSettings: Button = itemView.findViewById(R.id.go_to_settings)
    private val overLayToggle: Switch = itemView.findViewById<Switch>(R.id.toggle).apply {
        setOnCheckedChangeListener { _, isChecked -> brightnessGestureConsumer.isDimmerEnabled = isChecked }
    }

    override fun bind() {
        super.bind()
        val isPremium = purchasesManager.isPremium
        val hasOverlayPermission = brightnessGestureConsumer.hasOverlayPermission()

        goToSettings.visibility = if (hasOverlayPermission) View.GONE else View.VISIBLE
        goToSettings.setOnClickListener { this.goToSettings(it) }

        overLayToggle.isEnabled = isPremium
        overLayToggle.visibility = if (hasOverlayPermission) View.VISIBLE else View.GONE
        overLayToggle.setText(if (isPremium) R.string.screen_dimmer_toggle else R.string.go_premium_text)
        overLayToggle.isChecked = brightnessGestureConsumer.isDimmerEnabled

        if (!isPremium) itemView.setOnClickListener { this.goToSettings(it) }
    }

    private fun goToSettings(view: View) {
        if (purchasesManager.isNotPremium) return  goPremium(R.string.premium_prompt_dimmer)
        view.context.startActivity(Intent(ACTION_MANAGE_OVERLAY_PERMISSION))
    }
}
