package com.tunjid.fingergestures;


import android.support.annotation.IntDef;
import android.util.DisplayMetrics;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class WallpaperUtils {

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
}
