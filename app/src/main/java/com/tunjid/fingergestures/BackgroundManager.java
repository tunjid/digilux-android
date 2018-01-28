package com.tunjid.fingergestures;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.graphics.Palette;
import android.util.DisplayMetrics;
import android.util.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.function.IntConsumer;

import io.reactivex.Single;

import static android.app.AlarmManager.INTERVAL_DAY;
import static android.app.AlarmManager.RTC_WAKEUP;
import static android.app.WallpaperManager.FLAG_SYSTEM;
import static android.text.TextUtils.isEmpty;
import static io.reactivex.Single.error;
import static io.reactivex.Single.fromCallable;
import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.computation;

public class BackgroundManager {

    private static final String ERROR_NEED_PERMISSION = "Need permission";
    private static final String ERROR_NO_WALLPAPER_MANAGER = "No Wallpaper manager";
    private static final String ERROR_NO_DRAWABLE_FOUND = "No Drawable found";
    private static final String ERROR_NOT_A_BITMAP = "Not a Bitmap";

    private static final String DAY_WALLPAPER_NAME = "day";
    private static final String DAY_WALLPAPER_HOUR = "day wallpaper hour";
    private static final String DAY_WALLPAPER_MINUTE = "day wallpaper minute";

    private static final String NIGHT_WALLPAPER_NAME = "night";
    private static final String NIGHT_WALLPAPER_HOUR = "night wallpaper hour";
    private static final String NIGHT_WALLPAPER_MINUTE = "night wallpaper minute";

    private static final String EXTRA_CHANGE_WALLPAPER = "com.tunjid.fingergestures.extra.changeWallpaper";
    private static final String ACTION_CHANGE_WALLPAPER = "com.tunjid.fingergestures.action.changeWallpaper";
    private final String[] wallpaperTargets;

    public static final int DAY_WALLPAPER_PICK_CODE = 0;
    public static final int NIGHT_WALLPAPER_PICK_CODE = 1;
    private static final int INVALID_WALLPAPER_PICK_CODE = -1;


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({INVALID_WALLPAPER_PICK_CODE, DAY_WALLPAPER_PICK_CODE, NIGHT_WALLPAPER_PICK_CODE})
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
        wallpaperTargets = new String[]{app.getString(R.string.day_wallpaper), app.getString(R.string.night_wallpaper)};
    }

    public int[] getScreenAspectRatio() {
        DisplayMetrics displayMetrics = app.getResources().getDisplayMetrics();
        return new int[]{displayMetrics.widthPixels, displayMetrics.heightPixels};
    }

    public void requestWallPaperConstant(@StringRes int titleRes, Context context, IntConsumer consumer) {
        new AlertDialog.Builder(context)
                .setTitle(titleRes)
                .setItems(wallpaperTargets, (dialog, index) -> consumer.accept(index == 0
                        ? DAY_WALLPAPER_PICK_CODE
                        : NIGHT_WALLPAPER_PICK_CODE))
                .show();
    }

    public String getScreenDimensionRatio() {
        int[] dimensions = getScreenAspectRatio();
        return "H," + dimensions[0] + ":" + dimensions[1];
    }

    public File getWallpaperFile(@WallpaperSelection int selection) {
        return new File(app.getFilesDir(), getFileName(selection));
    }

    private String getFileName(@WallpaperSelection int selection) {
        return selection == DAY_WALLPAPER_PICK_CODE ? DAY_WALLPAPER_NAME : NIGHT_WALLPAPER_NAME;
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
            if (!swatches.isEmpty())
                return fromCallable(() -> Palette.from(swatches)).subscribeOn(computation()).observeOn(mainThread());
        }

        Drawable drawable = wallpaperManager.getDrawable();
        if (drawable == null) return error(new Exception(ERROR_NO_DRAWABLE_FOUND));
        if (!(drawable instanceof BitmapDrawable)) return error(new Exception(ERROR_NOT_A_BITMAP));

        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        return fromCallable(() -> Palette.from(bitmap).generate()).subscribeOn(computation()).observeOn(mainThread());
    }

    public void setWallpaperChangeTime(@WallpaperSelection int selection, int hourOfDay, int minute) {
        if (hourOfDay > 12 && selection == DAY_WALLPAPER_PICK_CODE) hourOfDay -= 12;
        if (hourOfDay < 12 && selection == NIGHT_WALLPAPER_PICK_CODE) hourOfDay += 12;

        getPreferences().edit()
                .putInt(selection == DAY_WALLPAPER_PICK_CODE ? DAY_WALLPAPER_HOUR : NIGHT_WALLPAPER_HOUR, hourOfDay)
                .putInt(selection == DAY_WALLPAPER_PICK_CODE ? DAY_WALLPAPER_MINUTE : NIGHT_WALLPAPER_MINUTE, minute)
                .apply();

        Calendar calendar = calendarForTime(hourOfDay, minute);

        AlarmManager alarmManager = app.getSystemService(AlarmManager.class);
        if (alarmManager == null) return;

        PendingIntent alarmIntent = getWallpaperPendingIntent(selection);
        alarmManager.setRepeating(RTC_WAKEUP, calendar.getTimeInMillis(), INTERVAL_DAY, alarmIntent);
    }

    public void cancelAutoWallpaper() {
        AlarmManager alarmManager = app.getSystemService(AlarmManager.class);
        if (alarmManager == null) return;

        PendingIntent day = getWallpaperPendingIntent(DAY_WALLPAPER_PICK_CODE);
        PendingIntent night = getWallpaperPendingIntent(NIGHT_WALLPAPER_PICK_CODE);

        Pair<Integer, Integer> dayTimePair = getDefaultTime(DAY_WALLPAPER_PICK_CODE);
        Pair<Integer, Integer> nightTimePair = getDefaultTime(NIGHT_WALLPAPER_PICK_CODE);

        setWallpaperChangeTime(NIGHT_WALLPAPER_PICK_CODE, nightTimePair.first, nightTimePair.second);
        setWallpaperChangeTime(DAY_WALLPAPER_PICK_CODE, dayTimePair.first, dayTimePair.second);
        alarmManager.cancel(night);
        alarmManager.cancel(day);
        night.cancel();
        day.cancel();
    }

    public Calendar getMainWallpaperCalendar(@WallpaperSelection int selection) {
        Pair<Integer, Integer> timePair = getDefaultTime(selection);
        SharedPreferences preferences = getPreferences();

        int hour = preferences.getInt(selection == DAY_WALLPAPER_PICK_CODE ? DAY_WALLPAPER_HOUR : NIGHT_WALLPAPER_HOUR, timePair.first);
        int minute = preferences.getInt(selection == DAY_WALLPAPER_PICK_CODE ? DAY_WALLPAPER_MINUTE : NIGHT_WALLPAPER_MINUTE, timePair.second);

        return calendarForTime(hour, minute);
    }

    private PendingIntent getWallpaperPendingIntent(@WallpaperSelection int selection) {
        return PendingIntent.getBroadcast(app, selection, getWallPaperChangeIntent(selection), PendingIntent.FLAG_UPDATE_CURRENT);
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
        int selection = selectionFromIntent(intent);
        if (selection == INVALID_WALLPAPER_PICK_CODE) return;

        File wallpaperFile = getWallpaperFile(selection);
        WallpaperManager wallpaperManager = app.getSystemService(WallpaperManager.class);
        if (wallpaperManager == null || !wallpaperFile.exists()) return;

        try { wallpaperManager.setStream(new FileInputStream(wallpaperFile));}
        catch (Exception e) {e.printStackTrace();}
    }

    @WallpaperSelection
    private int selectionFromIntent(Intent intent) {
        String action = intent.getAction();
        return !isEmpty(action) && action.equals(ACTION_CHANGE_WALLPAPER)
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

    public boolean willChangeWallpaper(@WallpaperSelection int selection) {
        Intent intent = new Intent(app, WallpaperBroadcastReceiver.class);
        intent.setAction(ACTION_CHANGE_WALLPAPER);
        intent.putExtra(EXTRA_CHANGE_WALLPAPER, selection);

        return PendingIntent.getBroadcast(app, selection, getWallPaperChangeIntent(selection), PendingIntent.FLAG_NO_CREATE) != null;
    }

    private SharedPreferences getPreferences() {
        return app.getPreferences();
    }

    private Pair<Integer, Integer> getDefaultTime(@WallpaperSelection int selection) {
        return new Pair<>(selection == DAY_WALLPAPER_PICK_CODE ? 7 : 19, 0);
    }

    private Intent getWallPaperChangeIntent(@WallpaperSelection int selection) {
        Intent intent = new Intent(app, WallpaperBroadcastReceiver.class);
        intent.setAction(ACTION_CHANGE_WALLPAPER);
        intent.putExtra(EXTRA_CHANGE_WALLPAPER, selection);

        return intent;
    }
}
