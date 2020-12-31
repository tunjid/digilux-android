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

package com.tunjid.fingergestures.managers

import android.annotation.TargetApi
import android.app.WallpaperManager
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.palette.graphics.Palette
import com.tunjid.fingergestures.hasStoragePermission
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import java.util.ArrayList

val Context.wallpaperPalettes: Flowable<PaletteStatus> get() = Flowable.defer {
    if (!hasStoragePermission) return@defer Flowable.just(PaletteStatus.Unavailable("Need permission"))

    val wallpaperManager = getSystemService(WallpaperManager::class.java)
        ?: return@defer Flowable.just(PaletteStatus.Unavailable("No Wallpaper manager"))

    if (wallpaperManager.wallpaperIsLive) {
        val swatches = wallpaperManager.getLiveWallpaperWatches()
        if (swatches.isNotEmpty())
            return@defer Flowable.fromCallable { Palette.from(swatches) }
                .map(PaletteStatus::Available)
                .subscribeOn(Schedulers.computation())
    }

    val drawable = (wallpaperManager.drawable
        ?: return@defer Flowable.just(PaletteStatus.Unavailable("No Drawable found")))
        as? BitmapDrawable
        ?: return@defer Flowable.just(PaletteStatus.Unavailable("Not a Bitmap"))

    val bitmap = drawable.bitmap
    Flowable.fromCallable { Palette.from(bitmap).generate() }
        .map(PaletteStatus::Available)
        .subscribeOn(Schedulers.computation())
}

@TargetApi(Build.VERSION_CODES.O_MR1)
private fun WallpaperManager.getLiveWallpaperWatches(): List<Palette.Swatch> {
    val colors = getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
    val result = ArrayList<Palette.Swatch>()
    if (colors == null) return result

    val primary = colors.primaryColor
    val secondary = colors.secondaryColor
    val tertiary = colors.tertiaryColor

    result.add(Palette.Swatch(primary.toArgb(), 3))

    if (secondary != null) result.add(Palette.Swatch(secondary.toArgb(), 2))
    if (tertiary != null) result.add(Palette.Swatch(tertiary.toArgb(), 2))

    return result
}

private val WallpaperManager.wallpaperIsLive: Boolean
    get() = Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1 && wallpaperInfo != null