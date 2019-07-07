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

import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import android.view.View
import android.widget.SeekBar
import android.widget.TextView

import com.tunjid.fingergestures.R

import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

class SliderAdjusterViewHolder(itemView: View,
                               @StringRes titleRes: Int,
                               @StringRes infoRes: Int,
                               private val consumer: (Int) -> Unit,
                               private val valueSupplier: () -> Int,
                               private val enabledSupplier: () -> Boolean,
                               private val function: (Int) -> String) : AppViewHolder(itemView), SeekBar.OnSeekBarChangeListener {

    private val value: TextView = itemView.findViewById(R.id.value)
    private val seekBar: SeekBar

    init {
        val title = itemView.findViewById<TextView>(R.id.title)
        title.setText(titleRes)

        seekBar = itemView.findViewById(R.id.seekbar)
        seekBar.setOnSeekBarChangeListener(this)

        if (infoRes != 0) {
            title.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_info_outline_white_24dp, 0)
            itemView.setOnClickListener { AlertDialog.Builder(itemView.context).setMessage(infoRes).show() }
        }
    }

    constructor(itemView: View,
                @StringRes titleRes: Int,
                consumer: (Int) -> Unit,
                valueSupplier: () -> Int,
                enabledSupplier: () -> Boolean,
                function: (Int) -> String) : this(itemView, titleRes, 0, consumer, valueSupplier, enabledSupplier, function)

    override fun bind() {
        super.bind()
        val enabled = enabledSupplier.invoke()

        seekBar.isEnabled = enabled
        seekBar.progress = valueSupplier.invoke()

        value.isEnabled = enabled
        value.text = function.invoke(valueSupplier.invoke())
    }

    override fun onProgressChanged(seekBar: SeekBar, percentage: Int, fromUser: Boolean) {
        if (!fromUser) return
        consumer.invoke(percentage)
        value.text = function.invoke(percentage)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {

    }
}
