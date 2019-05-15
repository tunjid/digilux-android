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

package com.tunjid.fingergestures.viewholders;

import android.app.TimePickerDialog;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.adapters.AppAdapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static com.tunjid.fingergestures.BackgroundManager.NIGHT_WALLPAPER_PICK_CODE;
import static com.tunjid.fingergestures.BackgroundManager.DAY_WALLPAPER_PICK_CODE;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;

public class WallpaperTriggerViewHolder extends AppViewHolder {

    private static final DateFormat dateFormat = new SimpleDateFormat("h:mm a", Locale.US);

    private TextView start;
    private TextView end;
    private BackgroundManager backgroundManager;

    public WallpaperTriggerViewHolder(View itemView, AppAdapter.AppAdapterListener appAdapterListener) {
        super(itemView, appAdapterListener);

        start = itemView.findViewById(R.id.start);
        end = itemView.findViewById(R.id.end);
        backgroundManager = BackgroundManager.getInstance();

        start.setOnClickListener(view -> showTimePicker(backgroundManager.getMainWallpaperCalendar(DAY_WALLPAPER_PICK_CODE),
                (dialog, hour, minute) -> {
                    backgroundManager.setWallpaperChangeTime(DAY_WALLPAPER_PICK_CODE, hour, minute);
                    bind();
                }));

        end.setOnClickListener(view -> showTimePicker(backgroundManager.getMainWallpaperCalendar(NIGHT_WALLPAPER_PICK_CODE),
                (dialog, hour, minute) -> {
                    backgroundManager.setWallpaperChangeTime(NIGHT_WALLPAPER_PICK_CODE, hour, minute);
                    bind();
                }));

        itemView.findViewById(R.id.cancel_auto_wallpaper).setOnClickListener(view -> {
            backgroundManager.cancelAutoWallpaper();
            bind();
        });
    }

    @Override
    public void bind() {
        super.bind();
        start.setCompoundDrawablesWithIntrinsicBounds(null, getTopDrawable(DAY_WALLPAPER_PICK_CODE), null, null);
        end.setCompoundDrawablesWithIntrinsicBounds(null, getTopDrawable(NIGHT_WALLPAPER_PICK_CODE), null, null);
        start.setText(getCalendarString(backgroundManager.getMainWallpaperCalendar(DAY_WALLPAPER_PICK_CODE)));
        end.setText(getCalendarString(backgroundManager.getMainWallpaperCalendar(NIGHT_WALLPAPER_PICK_CODE)));
        start.setTextColor(getTextColor(DAY_WALLPAPER_PICK_CODE));
        end.setTextColor(getTextColor(NIGHT_WALLPAPER_PICK_CODE));
    }

    private void showTimePicker(Calendar calendar, TimePickerDialog.OnTimeSetListener listener) {
        if (App.hasStoragePermission())
            new TimePickerDialog(itemView.getContext(), listener, calendar.get(HOUR_OF_DAY), calendar.get(MINUTE), false).show();
        else adapterListener.showSnackbar(R.string.enable_storage_settings);
    }

    private String getCalendarString(Calendar calendar) {
        return dateFormat.format(calendar.getTime());
    }

    private int getTextColor(@BackgroundManager.WallpaperSelection int selection) {
        return ContextCompat.getColor(itemView.getContext(), backgroundManager.willChangeWallpaper(selection)
                ? R.color.toggle_text
                : R.color.dark_grey);
    }

    private Drawable getTopDrawable(@BackgroundManager.WallpaperSelection int selection) {
        @DrawableRes int drawableRes = selection == DAY_WALLPAPER_PICK_CODE ? R.drawable.ic_day_24dp : R.drawable.ic_night_24dp;
        return backgroundManager.tint(drawableRes, getTextColor(selection));
    }
}
