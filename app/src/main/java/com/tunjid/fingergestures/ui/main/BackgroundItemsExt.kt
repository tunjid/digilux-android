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

package com.tunjid.fingergestures.ui.main

import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.managers.PaletteStatus
import com.tunjid.fingergestures.managers.WallpaperSelection
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables

val Inputs.backgroundItems: Flowable<List<Item>>
    get() = with(dependencies.backgroundManager) {
        Flowables.combineLatest(
            dependencies.purchasesManager.state,
            sliderDurationPreference.monitor,
            backgroundColorPreference.monitor,
            sliderColorPreference.monitor,
            nightWallpaperStatus,
            dayWallpaperStatus,
            paletteFlowable,
        ) { purchasesState, sliderDuration, backgroundColor, sliderColor, nightStatus, dayStatus, paletteStatus ->
            listOf(
                Item.Slider(
                    tab = Tab.Display,
                    sortKey = ItemSorter.SLIDER_DURATION,
                    titleRes = R.string.adjust_slider_duration,
                    infoRes = 0,
                    value = sliderDuration,
                    isEnabled = true,
                    consumer = sliderDurationPreference.setter,
                    function = ::getSliderDurationText
                ),
                Item.ColorAdjuster(
                    tab = Tab.Display,
                    sortKey = ItemSorter.SLIDER_COLOR,
                    backgroundColor = backgroundColor,
                    sliderColor = sliderColor,
                    canPickColorFromWallpaper = purchasesState.isPremium,
                    backgroundColorSetter = backgroundColorPreference.setter,
                    sliderColorSetter = sliderColorPreference.setter,
                    palette = when (paletteStatus) {
                        is PaletteStatus.Available -> paletteStatus.palette
                        is PaletteStatus.Unavailable -> null
                    },
                    input = this@backgroundItems,
                ),
                Item.WallpaperTrigger(
                    tab = Tab.Display,
                    sortKey = ItemSorter.WALLPAPER_TRIGGER,
                    dayStatus = dayStatus,
                    nightStatus = nightStatus,
                    selectTime = setWallpaperChangeTime,
                    cancelAutoWallpaper = cancelAutoWallpaper,
                    input = this@backgroundItems,
                ),
                Item.WallpaperPick(
                    tab = Tab.Display,
                    sortKey = ItemSorter.WALLPAPER_PICKER,
                    dayFile = getWallpaperFile(WallpaperSelection.Day),
                    nightFile = getWallpaperFile(WallpaperSelection.Night),
                    screenDimensionRatio = screenDimensionRatio,
                    paletteStatus = paletteStatus,
                    editWallPaperPendingIntent = wallpaperEditPendingIntent,
                    input = this@backgroundItems,
                ),
            )
        }
    }