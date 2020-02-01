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

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.models.Brightness


class DiscreteItemViewHolder(
        itemView: View,
        listener: (Brightness) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private var discreteValue: Brightness? = null
    private val textView: TextView = (itemView as TextView).apply {
        setOnClickListener { discreteValue?.let(listener) }
    }

    fun bind(brightness: Brightness) {
        this.discreteValue = brightness
        val context = itemView.context
        textView.text = context.getString(R.string.discrete_brightness_text, brightness.value)
    }
}
