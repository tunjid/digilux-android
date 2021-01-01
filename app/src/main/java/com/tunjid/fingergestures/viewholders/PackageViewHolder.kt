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

import android.content.pm.ApplicationInfo
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.models.Package

class PackageViewHolder(
        itemView: View,
        private val listener: (ApplicationInfo) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private var app: ApplicationInfo? = null

    private val imageView: ImageView = itemView.findViewById(R.id.icon)
    private val textView: TextView? = itemView.findViewById(R.id.text)

    init {
        itemView.setOnClickListener { app?.let(listener) }
    }

    fun bind(item: Package?) {
        if (item == null) return
        val packageManager = itemView.context.packageManager

        app = item.app
        imageView.setImageDrawable(packageManager.getApplicationIcon(item.app))
        if (textView != null) textView.text = packageManager.getApplicationLabel(item.app)
    }
}
