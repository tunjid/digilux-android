package com.tunjid.fingergestures;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.io.File;
import java.io.FileInputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.reactivex.Single;

import static android.app.WallpaperManager.FLAG_SYSTEM;
import static io.reactivex.Single.error;
import static io.reactivex.Single.fromCallable;
import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.computation;

public class BackgroundManager {

    private static final String ERROR_NEED_PERMISSION = "Need permission";
    private static final String ERROR_NO_WALLPAPER_MANAGER = "No Wallpaper manager";
    private static final String ERROR_NO_DRAWABLE_FOUND = "No Drawable found";
    private static final String ERROR_NOT_A_BITMAP = "Not a Bitmap";
    private static final String MAIN_WALLPAPER_HOUR = "main wallpaper hour";
    private static final String MAIN_WALLPAPER_MINUTE = "main wallpaper minute";
    private static final String ALT_WALLPAPER_HOUR = "alt wallpaper hour";
    private static final String ALT_WALLPAPER_MINUTE = "alt wallpaper minute";
    private static final String EXTRA_CHANGE_WALLPAPER = "com.tunjid.fingergestures.extra.changeWallpaper";
    private static final String ACTION_CHANGE_WALLPAPER = "com.tunjid.fingergestures.action.changeWallpaper";

    private static final int INVALID_WALLPAPER_PICK_CODE = -1;
    public static final int MAIN_WALLPAPER_PICK_CODE = 0;
    public static final int ALT_WALLPAPER_PICK_CODE = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({INVALID_WALLPAPER_PICK_CODE, MAIN_WALLPAPER_PICK_CODE, ALT_WALLPAPER_PICK_CODE})
    public @interface WallpaperSelection {}

    private final App app;

    @SuppressLint("StaticFieldLeak")
    private static BackgroundManager instance;

    public static BackgroundManager getInstance() {
        if (instance == null) instance = new BackgroundManager();
        return instance;
    }

    private BackgroundManager() {
        app = App.getInstance();
    }

    public int[] getScreenAspectRatio() {
        DisplayMetrics displayMetrics = App.getInstance().getResources().getDisplayMetrics();
        return new int[]{displayMetrics.widthPixels, displayMetrics.heightPixels};
    }

    public String getScreenDimensionRatio() {
        int[] dimensions = getScreenAspectRatio();
        return "H," + dimensions[0] + ":" + dimensions[1];
    }

    public File getWallpaperFile(@WallpaperSelection int selection) {
        return new File(App.getInstance().getFilesDir(), getFileName(selection));
    }

    private String getFileName(@WallpaperSelection int selection) {
        return selection == MAIN_WALLPAPER_PICK_CODE ? "main" : "alt";
    }

    public Drawable tint(@DrawableRes int drawableRes, int color) {
        Drawable normalDrawable = ContextCompat.getDrawable(app, drawableRes);
        Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
        DrawableCompat.setTint(wrapDrawable, color);

        return wrapDrawable;
    }

    public Single<Palette> extractPalette() {
        if (!App.hasStoragePermission()) return error(new Exception(ERROR_NEED_PERMISSION));

        WallpaperManager wallpaperManager = app.getSystemService(WallpaperManager.class);
        if (wallpaperManager == null) return error(new Exception(ERROR_NO_WALLPAPER_MANAGER));

        if (isLiveWallpaper(wallpaperManager)) {
            List<Palette.Swatch> swatches = getLiveWallpaperWatches(wallpaperManager);
            if (!swatches.isEmpty()) return fromCallable(() -> Palette.from(swatches))
                    .subscribeOn(computation()).observeOn(mainThread());
        }

        Drawable drawable = wallpaperManager.getDrawable();
        if (drawable == null) return error(new Exception(ERROR_NO_DRAWABLE_FOUND));
        if (!(drawable instanceof BitmapDrawable)) return error(new Exception(ERROR_NOT_A_BITMAP));

        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        return fromCallable(() -> Palette.from(bitmap).generate()).subscribeOn(computation()).observeOn(mainThread());
    }

    public void setWallpaperChangeTime(@WallpaperSelection int selection, int hourOfDay, int minute) {
        if (hourOfDay > 12 && selection == MAIN_WALLPAPER_PICK_CODE) hourOfDay -= 12;
        if (hourOfDay < 12 && selection == ALT_WALLPAPER_PICK_CODE) hourOfDay += 12;

        getPreferences().edit()
                .putInt(selection == MAIN_WALLPAPER_PICK_CODE ? MAIN_WALLPAPER_HOUR : ALT_WALLPAPER_HOUR, hourOfDay)
                .putInt(selection == MAIN_WALLPAPER_PICK_CODE ? MAIN_WALLPAPER_MINUTE : ALT_WALLPAPER_MINUTE, minute)
                .apply();

        // Set the alarm to start at approximately 2:00 p.m.
        Calendar calendar = calendarForTime(hourOfDay, minute);

        AlarmManager alarmManager = app.getSystemService(AlarmManager.class);
        if (alarmManager == null) return;

        PendingIntent alarmIntent = getWallpaperIntent(selection);
        // With setInexactRepeating(), you have to use one of the AlarmManager interval
        // constants--in this case, AlarmManager.INTERVAL_DAY.
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, alarmIntent);
    }

    public Calendar getMainWallpaperCalendar(@WallpaperSelection int selection) {
        SharedPreferences preferences = getPreferences();

        int defHour = selection == MAIN_WALLPAPER_PICK_CODE ? 7 : 19;
        int hour = preferences.getInt(selection == MAIN_WALLPAPER_PICK_CODE ? MAIN_WALLPAPER_HOUR : ALT_WALLPAPER_HOUR, defHour);
        int minute = preferences.getInt(selection == MAIN_WALLPAPER_PICK_CODE ? MAIN_WALLPAPER_MINUTE : ALT_WALLPAPER_MINUTE, 0);


        return calendarForTime(hour, minute);
    }

    private PendingIntent getWallpaperIntent(@WallpaperSelection int selection) {
        Intent intent = new Intent(app, WallpaperBroadcastReceiver.class);
        intent.setAction(ACTION_CHANGE_WALLPAPER);
        intent.putExtra(EXTRA_CHANGE_WALLPAPER, selection);

        return PendingIntent.getBroadcast(app, selection, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Calendar calendarForTime(int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    void onIntentReceived(Intent intent) {
        @WallpaperSelection
        int selection = selectionFromIntent(intent);

        if (selection == INVALID_WALLPAPER_PICK_CODE) return;
        WallpaperManager wallpaperManager = app.getSystemService(WallpaperManager.class);

        File wallpaperFile = getWallpaperFile(selection);
        if (wallpaperManager == null || !wallpaperFile.exists()) return;

        try { wallpaperManager.setStream(new FileInputStream(wallpaperFile));}
        catch (Exception e) {e.printStackTrace();}
    }

    @WallpaperSelection
    private int selectionFromIntent(Intent intent) {
        String action = intent.getAction();
        return !TextUtils.isEmpty(action) && action.equals(ACTION_CHANGE_WALLPAPER)
                ? intent.getIntExtra(EXTRA_CHANGE_WALLPAPER, INVALID_WALLPAPER_PICK_CODE)
                : INVALID_WALLPAPER_PICK_CODE;
    }

    @TargetApi(Build.VERSION_CODES.O_MR1)
    private List<Palette.Swatch> getLiveWallpaperWatches(WallpaperManager wallpaperManager) {
        WallpaperColors colors = wallpaperManager.getWallpaperColors(FLAG_SYSTEM);
        List<Palette.Swatch> result = new ArrayList<>();

        Color primary = colors.getPrimaryColor();
        Color secondary = colors.getSecondaryColor();
        Color tertiary = colors.getTertiaryColor();

        result.add(new Palette.Swatch(primary.toArgb(), 3));
        if (secondary != null) result.add(new Palette.Swatch(secondary.toArgb(), 2));
        if (tertiary != null) result.add(new Palette.Swatch(tertiary.toArgb(), 2));

        return result;
    }

    private boolean isLiveWallpaper(WallpaperManager wallpaperManager) {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1 && wallpaperManager.getWallpaperInfo() != null;
    }

    private SharedPreferences getPreferences() {
        return app.getPreferences();
    }

}
