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
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.BackgroundManager
import com.tunjid.fingergestures.BackgroundManager.Companion.DAY_WALLPAPER_PICK_CODE
import com.tunjid.fingergestures.BackgroundManager.Companion.NIGHT_WALLPAPER_PICK_CODE
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.AppAdapterListener
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.HOUR_OF_DAY
import java.util.Calendar.MINUTE

class WallpaperTriggerViewHolder(
        itemView: View,
        appAdapterListener: AppAdapterListener
) : AppViewHolder(itemView, appAdapterListener) {

    private val backgroundManager: BackgroundManager = BackgroundManager.instance
    private val start: TextView = itemView.findViewById(R.id.start)
    private val end: TextView = itemView.findViewById(R.id.end)

    init {
        start.setOnClickListener {
            showTimePicker(backgroundManager.getMainWallpaperCalendar(DAY_WALLPAPER_PICK_CODE),
                    TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                        backgroundManager.setWallpaperChangeTime(DAY_WALLPAPER_PICK_CODE, hour, minute)
                        bind()
                    })
        }

        end.setOnClickListener {
            showTimePicker(backgroundManager.getMainWallpaperCalendar(NIGHT_WALLPAPER_PICK_CODE),
                    TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                        backgroundManager.setWallpaperChangeTime(NIGHT_WALLPAPER_PICK_CODE, hour, minute)
                        bind()
                    }
            )
        }

        itemView.findViewById<View>(R.id.cancel_auto_wallpaper).setOnClickListener {
            backgroundManager.cancelAutoWallpaper()
            bind()
        }
    }

    override fun bind() {
        super.bind()
        start.setCompoundDrawablesWithIntrinsicBounds(null, getTopDrawable(DAY_WALLPAPER_PICK_CODE), null, null)
        end.setCompoundDrawablesWithIntrinsicBounds(null, getTopDrawable(NIGHT_WALLPAPER_PICK_CODE), null, null)
        start.text = getCalendarString(backgroundManager.getMainWallpaperCalendar(DAY_WALLPAPER_PICK_CODE))
        end.text = getCalendarString(backgroundManager.getMainWallpaperCalendar(NIGHT_WALLPAPER_PICK_CODE))
        start.setTextColor(getTextColor(DAY_WALLPAPER_PICK_CODE))
        end.setTextColor(getTextColor(NIGHT_WALLPAPER_PICK_CODE))
    }

    private fun showTimePicker(calendar: Calendar, onTimeSetListener: TimePickerDialog.OnTimeSetListener) {
        if (App.hasStoragePermission)
            TimePickerDialog(itemView.context, onTimeSetListener, calendar.get(HOUR_OF_DAY), calendar.get(MINUTE), false).show()
        else
            listener.showSnackbar(R.string.enable_storage_settings)
    }

    private fun getCalendarString(calendar: Calendar): String = dateFormat.format(calendar.time)

    private fun getTextColor(@BackgroundManager.WallpaperSelection selection: Int): Int {
        return ContextCompat.getColor(itemView.context, if (backgroundManager.willChangeWallpaper(selection))
            R.color.toggle_text
        else
            R.color.dark_grey)
    }

    private fun getTopDrawable(@BackgroundManager.WallpaperSelection selection: Int): Drawable {
        @DrawableRes val drawableRes = if (selection == DAY_WALLPAPER_PICK_CODE) R.drawable.ic_day_24dp else R.drawable.ic_night_24dp
        return backgroundManager.tint(drawableRes, getTextColor(selection))
    }

    companion object {

        private val dateFormat = SimpleDateFormat("h:mm a", Locale.US)
    }
}
