package com.tunjid.fingergestures;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Pair;

import com.tunjid.fingergestures.gestureconsumers.GestureConsumer;

import java.io.File;
import java.io.FileInputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.function.IntConsumer;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.palette.graphics.Palette;
import io.reactivex.Single;

import static android.app.AlarmManager.INTERVAL_DAY;
import static android.app.AlarmManager.RTC_WAKEUP;
import static android.app.WallpaperManager.FLAG_SYSTEM;
import static androidx.core.content.ContextCompat.getColor;
import static com.tunjid.fingergestures.App.EMPTY;
import static com.tunjid.fingergestures.App.transformApp;
import static com.tunjid.fingergestures.App.withApp;
import static io.reactivex.Single.error;
import static io.reactivex.Single.fromCallable;
import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.computation;

public class BackgroundManager {

    private static final int MAX_SLIDER_DURATION = 5000;
    private static final int DEF_SLIDER_DURATION_PERCENT = 60;

    private static final String SLIDER_DURATION = "slider duration";
    private static final String BACKGROUND_COLOR = "background color";
    private static final String SLIDER_COLOR = "slider color";

    private static final String ERROR_NEED_PERMISSION = "Need permission";
    private static final String ERROR_NO_WALLPAPER_MANAGER = "No Wallpaper manager";
    private static final String ERROR_NO_DRAWABLE_FOUND = "No Drawable found";
    private static final String ERROR_NOT_A_BITMAP = "Not a Bitmap";

    private static final String DAY_WALLPAPER_NAME = "day";
    private static final String DAY_WALLPAPER_SET = "day wallpaper set";
    private static final String DAY_WALLPAPER_HOUR = "day wallpaper hour";
    private static final String DAY_WALLPAPER_MINUTE = "day wallpaper minute";

    private static final String NIGHT_WALLPAPER_NAME = "night";
    private static final String NIGHT_WALLPAPER_SET = "night wallpaper set";
    private static final String NIGHT_WALLPAPER_HOUR = "night wallpaper hour";
    private static final String NIGHT_WALLPAPER_MINUTE = "night wallpaper minute";

    private static final String EXTRA_CHOSEN_COMPONENT = "android.intent.extra.CHOSEN_COMPONENT";
    private static final String EXTRA_CHANGE_WALLPAPER = "com.tunjid.fingergestures.extra.changeWallpaper";
    private static final String ACTION_CHANGE_WALLPAPER = "com.tunjid.fingergestures.action.changeWallpaper";
    public static final String ACTION_EDIT_WALLPAPER = "com.tunjid.fingergestures.action.editWallpaper";

    private final String[] wallpaperTargets;

    public static final int DAY_WALLPAPER_PICK_CODE = 0;
    public static final int NIGHT_WALLPAPER_PICK_CODE = 1;
    private static final int INVALID_WALLPAPER_PICK_CODE = -1;


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({INVALID_WALLPAPER_PICK_CODE, DAY_WALLPAPER_PICK_CODE, NIGHT_WALLPAPER_PICK_CODE})
    public @interface WallpaperSelection {}

    @SuppressLint("StaticFieldLeak")
    private static BackgroundManager instance;

    public static BackgroundManager getInstance() {
        if (instance == null) instance = new BackgroundManager();
        return instance;
    }

    private BackgroundManager() {
        wallpaperTargets = new String[]{
                transformApp(app -> app.getString(R.string.day_wallpaper), EMPTY),
                transformApp(app -> app.getString(R.string.night_wallpaper), EMPTY)};
    }

    @ColorInt
    public int getSliderColor() {
        return transformApp(app -> app.getPreferences().getInt(SLIDER_COLOR, getColor(app, R.color.colorAccent)), Color.WHITE);
    }

    @ColorInt
    public int getBackgroundColor() {
        return transformApp(app -> app.getPreferences().getInt(BACKGROUND_COLOR, getColor(app, R.color.colorPrimary)), Color.LTGRAY);
    }

    @IntRange(from = GestureConsumer.ZERO_PERCENT, to = GestureConsumer.HUNDRED_PERCENT)
    public int getSliderDurationPercentage() {
        return transformApp(app -> app.getPreferences().getInt(SLIDER_DURATION, DEF_SLIDER_DURATION_PERCENT), GestureConsumer.FIFTY_PERCENT);
    }

    public int getSliderDurationMillis() {
        return durationPercentageToMillis(getSliderDurationPercentage());
    }

    public void setSliderColor(@ColorInt int color) {
        withApp(app -> app.getPreferences().edit().putInt(SLIDER_COLOR, color).apply());
    }

    public void setBackgroundColor(@ColorInt int color) {
        withApp(app -> app.getPreferences().edit().putInt(BACKGROUND_COLOR, color).apply());
    }

    public void setSliderDurationPercentage(@IntRange(from = GestureConsumer.ZERO_PERCENT, to = GestureConsumer.HUNDRED_PERCENT) int duration) {
        withApp(app -> app.getPreferences().edit().putInt(SLIDER_DURATION, duration).apply());
    }

    public String getSliderDurationText(@IntRange(from = GestureConsumer.ZERO_PERCENT, to = GestureConsumer.HUNDRED_PERCENT) int duration) {
        int millis = durationPercentageToMillis(duration);
        float seconds = millis / 1000F;
        return transformApp(app -> app.getString(R.string.duration_value, seconds), EMPTY);
    }

    @Nullable
    public int[] getScreenAspectRatio() {
        DisplayMetrics displayMetrics = transformApp(app -> app.getResources().getDisplayMetrics());
        return displayMetrics == null ? null : new int[]{displayMetrics.widthPixels, displayMetrics.heightPixels};
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
        return dimensions == null ? "H, 16:9" : "H," + dimensions[0] + ":" + dimensions[1];
    }

    public File getWallpaperFile(@WallpaperSelection int selection, @NonNull Context context) {
        Context app = context.getApplicationContext();
        return new File(app.getFilesDir(), getFileName(selection));
    }

    private String getFileName(@WallpaperSelection int selection) {
        return selection == DAY_WALLPAPER_PICK_CODE ? DAY_WALLPAPER_NAME : NIGHT_WALLPAPER_NAME;
    }

    @NonNull
    public Drawable tint(@DrawableRes int drawableRes, int color) {
        Drawable normalDrawable = transformApp(app -> ContextCompat.getDrawable(app, drawableRes));
        if (normalDrawable == null) return new ColorDrawable(color);

        Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
        DrawableCompat.setTint(wrapDrawable, color);

        return wrapDrawable;
    }

    public Single<Palette> extractPalette() {
        if (!App.hasStoragePermission()) return error(new Exception(ERROR_NEED_PERMISSION));

        WallpaperManager wallpaperManager = transformApp(app -> app.getSystemService(WallpaperManager.class));
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

    public void restoreWallpaperChange() {
        reInitializeWallpaperChange(DAY_WALLPAPER_PICK_CODE);
        reInitializeWallpaperChange(NIGHT_WALLPAPER_PICK_CODE);
    }

    public void setWallpaperChangeTime(@WallpaperSelection int selection, int hourOfDay, int minute) {
        setWallpaperChangeTime(selection, hourOfDay, minute, true);
    }

    public void cancelAutoWallpaper() {
        AlarmManager alarmManager = transformApp(app -> app.getSystemService(AlarmManager.class));
        if (alarmManager == null) return;

        PendingIntent day = getWallpaperPendingIntent(DAY_WALLPAPER_PICK_CODE);
        PendingIntent night = getWallpaperPendingIntent(NIGHT_WALLPAPER_PICK_CODE);
        if (day == null || night == null) return;

        Pair<Integer, Integer> dayTimePair = getDefaultTime(DAY_WALLPAPER_PICK_CODE);
        Pair<Integer, Integer> nightTimePair = getDefaultTime(NIGHT_WALLPAPER_PICK_CODE);

        setWallpaperChangeTime(NIGHT_WALLPAPER_PICK_CODE, nightTimePair.first, nightTimePair.second, false);
        setWallpaperChangeTime(DAY_WALLPAPER_PICK_CODE, dayTimePair.first, dayTimePair.second, false);
        alarmManager.cancel(night);
        alarmManager.cancel(day);
        night.cancel();
        day.cancel();
    }

    public Calendar getMainWallpaperCalendar(@WallpaperSelection int selection) {
        Pair<Integer, Integer> timePair = getDefaultTime(selection);
        SharedPreferences preferences = transformApp(App::getPreferences);
        if (preferences == null) return Calendar.getInstance();

        int hour = preferences.getInt(selection == DAY_WALLPAPER_PICK_CODE ? DAY_WALLPAPER_HOUR : NIGHT_WALLPAPER_HOUR, timePair.first);
        int minute = preferences.getInt(selection == DAY_WALLPAPER_PICK_CODE ? DAY_WALLPAPER_MINUTE : NIGHT_WALLPAPER_MINUTE, timePair.second);

        return calendarForTime(hour, minute);
    }

    @Nullable
    private PendingIntent getWallpaperPendingIntent(@WallpaperSelection int selection) {
        return transformApp(app -> PendingIntent.getBroadcast(app, selection, getWallPaperChangeIntent(app, selection), PendingIntent.FLAG_UPDATE_CURRENT));
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
        if (handledEditPick(intent)) return;

        int selection = selectionFromIntent(intent);
        if (selection == INVALID_WALLPAPER_PICK_CODE) return;

        File wallpaperFile = transformApp(app -> getWallpaperFile(selection, app));
        if (wallpaperFile == null) return;

        WallpaperManager wallpaperManager = transformApp(app -> app.getSystemService(WallpaperManager.class));
        if (wallpaperManager == null || !wallpaperFile.exists()) return;

        if (!App.hasStoragePermission()) return;
        try { wallpaperManager.setStream(new FileInputStream(wallpaperFile));}
        catch (Exception e) { e.printStackTrace(); }
    }

    @WallpaperSelection
    private int selectionFromIntent(Intent intent) {
        String action = intent.getAction();
        return action != null && action.equals(ACTION_CHANGE_WALLPAPER)
                ? intent.getIntExtra(EXTRA_CHANGE_WALLPAPER, INVALID_WALLPAPER_PICK_CODE)
                : INVALID_WALLPAPER_PICK_CODE;
    }

    private void reInitializeWallpaperChange(@WallpaperSelection int selection) {
        withApp(app -> {
            SharedPreferences preferences = app.getPreferences();
            boolean isDay = selection == DAY_WALLPAPER_PICK_CODE;
            boolean hasCalendar = preferences.getBoolean(isDay ? DAY_WALLPAPER_SET : NIGHT_WALLPAPER_SET, false);
            if (!hasCalendar) return;

            Calendar calendar = getMainWallpaperCalendar(selection);
            setWallpaperChangeTime(selection, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        });
    }

    private void setWallpaperChangeTime(@WallpaperSelection int selection, int hourOfDay, int minute, boolean adding) {
        int hour = hourOfDay > 12 && selection == DAY_WALLPAPER_PICK_CODE
                ? hourOfDay - 12
                : hourOfDay < 12 && selection == NIGHT_WALLPAPER_PICK_CODE
                ? hourOfDay + 12
                : hourOfDay;

        boolean isDayWallpaper = selection == DAY_WALLPAPER_PICK_CODE;

        withApp(app -> app.getPreferences().edit()
                .putInt(isDayWallpaper ? DAY_WALLPAPER_HOUR : NIGHT_WALLPAPER_HOUR, hour)
                .putInt(isDayWallpaper ? DAY_WALLPAPER_MINUTE : NIGHT_WALLPAPER_MINUTE, minute)
                .putBoolean(isDayWallpaper ? DAY_WALLPAPER_SET : NIGHT_WALLPAPER_SET, adding)
                .apply());

        Calendar calendar = calendarForTime(hourOfDay, minute);

        AlarmManager alarmManager = transformApp(app -> app.getSystemService(AlarmManager.class));
        if (alarmManager == null) return;

        PendingIntent alarmIntent = getWallpaperPendingIntent(selection);
        alarmManager.setRepeating(RTC_WAKEUP, calendar.getTimeInMillis(), INTERVAL_DAY, alarmIntent);
    }

    @TargetApi(Build.VERSION_CODES.O_MR1)
    private List<Palette.Swatch> getLiveWallpaperWatches(WallpaperManager wallpaperManager) {
        WallpaperColors colors = wallpaperManager.getWallpaperColors(FLAG_SYSTEM);
        List<Palette.Swatch> result = new ArrayList<>();
        if (colors == null) return result;

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
        return transformApp(app -> {
            Intent intent = new Intent(app, WallpaperBroadcastReceiver.class);
            intent.setAction(ACTION_CHANGE_WALLPAPER);
            intent.putExtra(EXTRA_CHANGE_WALLPAPER, selection);

            return PendingIntent.getBroadcast(app, selection, getWallPaperChangeIntent(app, selection), PendingIntent.FLAG_NO_CREATE) != null;
        }, false);
    }

    private Pair<Integer, Integer> getDefaultTime(@WallpaperSelection int selection) {
        return new Pair<>(selection == DAY_WALLPAPER_PICK_CODE ? 7 : 19, 0);
    }

    public PendingIntent getWallpaperEditPendingIntent(Context context) {
        Context app = context.getApplicationContext();
        Intent intent = new Intent(app, WallpaperBroadcastReceiver.class);
        intent.setAction(ACTION_EDIT_WALLPAPER);
        return PendingIntent.getBroadcast(app, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Intent getWallPaperChangeIntent(Context app, @WallpaperSelection int selection) {
        Intent intent = new Intent(app, WallpaperBroadcastReceiver.class);
        intent.setAction(ACTION_CHANGE_WALLPAPER);
        intent.putExtra(EXTRA_CHANGE_WALLPAPER, selection);

        return intent;
    }

    private boolean handledEditPick(Intent intent) {
        if (!ACTION_EDIT_WALLPAPER.equals(intent.getAction())) return false;

        ComponentName componentName = intent.getParcelableExtra(EXTRA_CHOSEN_COMPONENT);
        if (componentName == null) return false;

        boolean handled = componentName.getPackageName().equals("com.google.android.apps.photos");
        if (handled)
            withApp(app -> LocalBroadcastManager.getInstance(app).sendBroadcast(intent));

        return handled;
    }

    private int durationPercentageToMillis(int percentage) {
        return (int) (percentage * MAX_SLIDER_DURATION / 100F);
    }
}
