package com.tunjid.fingergestures.viewholders;

import android.app.TimePickerDialog;
import android.view.View;
import android.widget.TextView;

import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.adapters.AppAdapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static com.tunjid.fingergestures.BackgroundManager.ALT_WALLPAPER_PICK_CODE;
import static com.tunjid.fingergestures.BackgroundManager.MAIN_WALLPAPER_PICK_CODE;
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

        start.setOnClickListener(view -> showTimePicker(backgroundManager.getMainWallpaperCalendar(MAIN_WALLPAPER_PICK_CODE),
                (dialog, hour, minute) -> {
                    backgroundManager.setWallpaperChangeTime(MAIN_WALLPAPER_PICK_CODE, hour, minute);
                    bind();
                }));

        end.setOnClickListener(view -> showTimePicker(backgroundManager.getMainWallpaperCalendar(ALT_WALLPAPER_PICK_CODE),
                (dialog, hour, minute) -> {
                    backgroundManager.setWallpaperChangeTime(ALT_WALLPAPER_PICK_CODE, hour, minute);
                    bind();
                }));
    }

    @Override
    public void bind() {
        super.bind();
        start.setText(getCalendarString(backgroundManager.getMainWallpaperCalendar(MAIN_WALLPAPER_PICK_CODE)));
        end.setText(getCalendarString(backgroundManager.getMainWallpaperCalendar(ALT_WALLPAPER_PICK_CODE)));
    }

    private void showTimePicker(Calendar calendar, TimePickerDialog.OnTimeSetListener listener) {
        new TimePickerDialog(itemView.getContext(), listener, calendar.get(HOUR_OF_DAY), calendar.get(MINUTE), false).show();
    }

    private String getCalendarString(Calendar calendar) {
        return dateFormat.format(calendar.getTime());
    }
}
