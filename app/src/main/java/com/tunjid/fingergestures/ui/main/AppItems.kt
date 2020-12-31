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
import com.tunjid.fingergestures.ui.main.Item
import com.tunjid.fingergestures.di.AppDependencies
import com.tunjid.fingergestures.models.Input
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables
import java.util.*

enum class Tab(val resource: Int) {
    Gestures(R.id.action_directions),
    Brightness(R.id.action_slider),
    Audio(R.id.action_audio),
    Shortcuts(R.id.action_accessibility_popup),
    Display(R.id.action_wallpaper);
}

interface Inputs {
    val dependencies: AppDependencies

    fun accept(input: Input)

    val items: Flowable<List<Item>>
        get() = Flowables.combineLatest(
            linkItems,
            brightnessItems,
            backgroundItems,
            rotationItems,
            popUpItems,
            audioItems,
            purchaseItems,
            gestureItems,
        ) { simple, brightness, background, rotation, popUp, audio, purchase, gestures ->

            (simple + brightness + background + rotation + popUp + audio + purchase + gestures)
                .groupBy(Item::tab)
                .entries
                .map { (tab, items) ->
                    listOf(
                        Item.Padding(
                            sortKey = Int.MIN_VALUE,
                            tab = tab,
                            diffId = "Top"
                        ),
                        Item.Padding(
                            sortKey = Int.MAX_VALUE,
                            tab = tab,
                            diffId = "Bottom"
                        )
                    ) + items
                }
                .flatten()
                .sortedWith(COMPARATOR)
        }
}

private val COMPARATOR: Comparator<Item> = compareBy(Item::tab, {
    when (val index = it.sortList.indexOf(it.sortKey)) {
        in Int.MIN_VALUE until 0 -> it.sortKey
        else -> index
    }
})

private val Item.sortList get() = tabItems.getValue(tab)

private val tabItems = listOf(
    Tab.Gestures to intArrayOf(
        MainViewModel.MAP_UP_ICON, MainViewModel.MAP_DOWN_ICON, MainViewModel.MAP_LEFT_ICON, MainViewModel.MAP_RIGHT_ICON,
        MainViewModel.AD_FREE, MainViewModel.SUPPORT, MainViewModel.REVIEW, MainViewModel.LOCKED_CONTENT
    ),
    Tab.Brightness to intArrayOf(
        MainViewModel.SLIDER_DELTA, MainViewModel.DISCRETE_BRIGHTNESS, MainViewModel.SCREEN_DIMMER, MainViewModel.USE_LOGARITHMIC_SCALE,
        MainViewModel.SHOW_SLIDER, MainViewModel.ADAPTIVE_BRIGHTNESS, MainViewModel.ANIMATES_SLIDER, MainViewModel.ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS,
        MainViewModel.DOUBLE_SWIPE_SETTINGS
    ),
    Tab.Audio to intArrayOf(
        MainViewModel.AUDIO_DELTA, MainViewModel.AUDIO_STREAM_TYPE, MainViewModel.AUDIO_SLIDER_SHOW
    ),
    Tab.Shortcuts to intArrayOf(
        MainViewModel.ENABLE_ACCESSIBILITY_BUTTON, MainViewModel.ACCESSIBILITY_SINGLE_CLICK,
        MainViewModel.ANIMATES_POPUP, MainViewModel.ENABLE_WATCH_WINDOWS, MainViewModel.POPUP_ACTION,
        MainViewModel.ROTATION_LOCK, MainViewModel.EXCLUDED_ROTATION_LOCK, MainViewModel.ROTATION_HISTORY
    ),
    Tab.Display to intArrayOf(
        MainViewModel.SLIDER_POSITION, MainViewModel.SLIDER_DURATION, MainViewModel.NAV_BAR_COLOR,
        MainViewModel.SLIDER_COLOR, MainViewModel.WALLPAPER_PICKER, MainViewModel.WALLPAPER_TRIGGER
    )
).toMap()