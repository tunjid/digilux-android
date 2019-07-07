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
import android.widget.Switch
import androidx.annotation.StringRes
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R

class ToggleViewHolder(itemView: View,
                       @StringRes titleRes: Int,
                       supplier: () -> Boolean,
                       consumer: (Boolean) -> Unit) : AppViewHolder(itemView) {

    init {
        val toggle = itemView.findViewById<Switch>(R.id.toggle)
        toggle.setText(titleRes)
        toggle.setOnClickListener { consumer.invoke(toggle.isChecked) }
        disposables.add(App.backgroundToMain(supplier).subscribe((toggle::setChecked), Throwable::printStackTrace))
    }
}
