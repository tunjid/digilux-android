package com.tunjid.fingergestures;


import android.annotation.TargetApi;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
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
import android.util.DisplayMetrics;

import java.io.File;
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

public class WallpaperUtils {

    private static final String ERROR_NEED_PERMISSION = "Need permission";
    private static final String ERROR_NO_WALLPAPER_MANAGER = "No Wallpaper manager";
    private static final String ERROR_NO_DRAWABLE_FOUND = "No Drawable found";
    private static final String ERROR_NOT_A_BITMAP = "Not a Bitmap";
    private static final String MAIN_WALLPAPER_HOUR = "main wallpaper hour";
    private static final String MAIN_WALLPAPER_MINUTE = "main wallpaper minute";
    private static final String ALT_WALLPAPER_HOUR = "alt wallpaper hour";
    private static final String ALT_WALLPAPER_MINUTE = "alt wallpaper minute";

    public static final int MAIN_WALLPAPER_PICK_CODE = 0;
    public static final int ALT_WALLPAPER_PICK_CODE = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MAIN_WALLPAPER_PICK_CODE, ALT_WALLPAPER_PICK_CODE})
    public @interface WallpaperSelection {}

    public static int[] getScreenAspectRatio() {
        DisplayMetrics displayMetrics = App.getInstance().getResources().getDisplayMetrics();
        return new int[]{displayMetrics.widthPixels, displayMetrics.heightPixels};
    }

    public static String getScreenDimensionRatio() {
        int[] dimensions = getScreenAspectRatio();
        return "H," + dimensions[0] + ":" + dimensions[1];
    }

    public static File getWallpaperFile(@WallpaperSelection int selection) {
        return new File(App.getInstance().getFilesDir(), getFileName(selection));
    }

    private static String getFileName(@WallpaperSelection int selection) {
        return selection == MAIN_WALLPAPER_PICK_CODE ? "main" : "alt";
    }

    public static Drawable tint(@DrawableRes int drawableRes, int color) {
        Context context = App.getInstance();
        Drawable normalDrawable = ContextCompat.getDrawable(context, drawableRes);
        Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
        DrawableCompat.setTint(wrapDrawable, color);

        return wrapDrawable;
    }

    public static Single<Palette> extractPalette() {
        if (!App.hasStoragePermission()) return error(new Exception(ERROR_NEED_PERMISSION));

        WallpaperManager wallpaperManager = App.getInstance().getSystemService(WallpaperManager.class);
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

    public static void setLightTme(@WallpaperSelection int selection, int hourOfDay, int minute) {
        int hour = hourOfDay;
        if (hourOfDay > 12 && selection == MAIN_WALLPAPER_PICK_CODE) hour -= 12;
        if (hourOfDay < 12 && selection == ALT_WALLPAPER_PICK_CODE) hour += 12;

        getPreferences().edit()
                .putInt(selection == MAIN_WALLPAPER_PICK_CODE ? MAIN_WALLPAPER_HOUR : ALT_WALLPAPER_HOUR, hour)
                .putInt(selection == MAIN_WALLPAPER_PICK_CODE ? MAIN_WALLPAPER_MINUTE : ALT_WALLPAPER_MINUTE, minute)
                .apply();
    }

    public static Calendar getMainWallpaperCalendar(@WallpaperSelection int selection) {
        SharedPreferences preferences = getPreferences();

        int defHour = selection == MAIN_WALLPAPER_PICK_CODE ? 7 : 19;
        int hour = preferences.getInt(selection == MAIN_WALLPAPER_PICK_CODE ? MAIN_WALLPAPER_HOUR : ALT_WALLPAPER_HOUR, defHour);
        int minute = preferences.getInt(selection == MAIN_WALLPAPER_PICK_CODE ? MAIN_WALLPAPER_MINUTE : ALT_WALLPAPER_MINUTE, 0);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    @TargetApi(Build.VERSION_CODES.O_MR1)
    private static List<Palette.Swatch> getLiveWallpaperWatches(WallpaperManager wallpaperManager) {
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

    private static boolean isLiveWallpaper(WallpaperManager wallpaperManager) {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1 && wallpaperManager.getWallpaperInfo() != null;
    }

    private static SharedPreferences getPreferences() {
        return App.getInstance().getPreferences();
    }
}
