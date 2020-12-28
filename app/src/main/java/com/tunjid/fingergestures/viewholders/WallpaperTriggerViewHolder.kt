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

import android.app.TimePickerDialog
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import com.tunjid.androidx.core.content.colorAt
import com.tunjid.androidx.core.content.drawableAt
import com.tunjid.androidx.core.graphics.drawable.withTint
import com.tunjid.androidx.recyclerview.viewbinding.BindingViewHolder
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderDelegate
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderFrom
import com.tunjid.fingergestures.models.uiState
import com.tunjid.fingergestures.models.updatePartial
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.WallpaperSelection
import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.databinding.ViewholderWallpaperTriggerBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.HOUR_OF_DAY
import java.util.Calendar.MINUTE

private var BindingViewHolder<ViewholderWallpaperTriggerBinding>.item by viewHolderDelegate<Item.WallpaperTrigger>()

fun ViewGroup.wallpaperTrigger() = viewHolderFrom(ViewholderWallpaperTriggerBinding::inflate).apply {
    binding.start.setOnClickListener { selectTime(WallpaperSelection.Day) }
    binding.end.setOnClickListener { selectTime(WallpaperSelection.Night) }
    binding.cancelAutoWallpaper.setOnClickListener { item.cancelAutoWallpaper() }
}

fun BindingViewHolder<ViewholderWallpaperTriggerBinding>.bind(item: Item.WallpaperTrigger) = binding.run {
    this@bind.item = item
    start.setCompoundDrawablesWithIntrinsicBounds(null, getTopDrawable(WallpaperSelection.Day), null, null)
    end.setCompoundDrawablesWithIntrinsicBounds(null, getTopDrawable(WallpaperSelection.Night), null, null)
    start.text = getCalendarString(item.dayStatus.calendar)
    end.text = getCalendarString(item.nightStatus.calendar)
    start.setTextColor(getTextColor(WallpaperSelection.Day))
    end.setTextColor(getTextColor(WallpaperSelection.Night))
}

private fun BindingViewHolder<ViewholderWallpaperTriggerBinding>.showTimePicker(calendar: Calendar, onTimeSetListener: TimePickerDialog.OnTimeSetListener) {
    if (App.hasStoragePermission)
        TimePickerDialog(itemView.context, onTimeSetListener, calendar.get(HOUR_OF_DAY), calendar.get(MINUTE), false).show()
    else itemView::uiState.updatePartial { copy(snackbarText = itemView.context.getString(R.string.enable_storage_settings)) }
}

private fun getCalendarString(calendar: Calendar): String = dateFormat.format(calendar.time)

private fun BindingViewHolder<ViewholderWallpaperTriggerBinding>.getTextColor(selection: WallpaperSelection): Int =
    itemView.context.colorAt(when {
        when (selection) {
            WallpaperSelection.Day -> item.dayStatus.willChange
            WallpaperSelection.Night -> item.nightStatus.willChange
        } -> R.color.toggle_text
        else -> R.color.dark_grey
    })

private fun BindingViewHolder<ViewholderWallpaperTriggerBinding>.getTopDrawable(selection: WallpaperSelection): Drawable? =
    itemView.context.drawableAt(when (selection) {
        WallpaperSelection.Day -> R.drawable.ic_day_24dp
        WallpaperSelection.Night -> R.drawable.ic_night_24dp
    })?.withTint(getTextColor(selection))

private fun BindingViewHolder<ViewholderWallpaperTriggerBinding>.selectTime(selection: WallpaperSelection) =
    showTimePicker(when (selection) {
        WallpaperSelection.Day -> item.dayStatus.calendar
        WallpaperSelection.Night -> item.nightStatus.calendar
    }) { _, hour, minute -> item.selectTime(selection, hour, minute) }

private val dateFormat = SimpleDateFormat("h:mm a", Locale.US)
