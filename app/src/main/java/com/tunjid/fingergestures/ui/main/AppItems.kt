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
import com.tunjid.fingergestures.di.AppDependencies
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

object ItemSorter {
    private const val PADDING = -1
    const val SLIDER_DELTA = PADDING + 1
    const val SLIDER_POSITION = SLIDER_DELTA + 1
    const val SLIDER_DURATION = SLIDER_POSITION + 1
    const val SLIDER_COLOR = SLIDER_DURATION + 1
    const val SCREEN_DIMMER = SLIDER_COLOR + 1
    const val USE_LOGARITHMIC_SCALE = SCREEN_DIMMER + 1
    const val SHOW_SLIDER = USE_LOGARITHMIC_SCALE + 1
    const val ADAPTIVE_BRIGHTNESS = SHOW_SLIDER + 1
    const val ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS = ADAPTIVE_BRIGHTNESS + 1
    const val DOUBLE_SWIPE_SETTINGS = ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS + 1
    const val MAP_UP_ICON = DOUBLE_SWIPE_SETTINGS + 1
    const val MAP_DOWN_ICON = MAP_UP_ICON + 1
    const val MAP_LEFT_ICON = MAP_DOWN_ICON + 1
    const val MAP_RIGHT_ICON = MAP_LEFT_ICON + 1
    const val AD_FREE = MAP_RIGHT_ICON + 1
    const val REVIEW = AD_FREE + 1
    const val WALLPAPER_PICKER = REVIEW + 1
    const val WALLPAPER_TRIGGER = WALLPAPER_PICKER + 1
    const val ROTATION_LOCK = WALLPAPER_TRIGGER + 1
    const val EXCLUDED_ROTATION_LOCK = ROTATION_LOCK + 1
    const val ENABLE_WATCH_WINDOWS = EXCLUDED_ROTATION_LOCK + 1
    const val POPUP_ACTION = ENABLE_WATCH_WINDOWS + 1
    const val ENABLE_ACCESSIBILITY_BUTTON = POPUP_ACTION + 1
    const val ACCESSIBILITY_SINGLE_CLICK = ENABLE_ACCESSIBILITY_BUTTON + 1
    const val ANIMATES_SLIDER = ACCESSIBILITY_SINGLE_CLICK + 1
    const val ANIMATES_POPUP = ANIMATES_SLIDER + 1
    const val DISCRETE_BRIGHTNESS = ANIMATES_POPUP + 1
    const val AUDIO_DELTA = DISCRETE_BRIGHTNESS + 1
    const val AUDIO_STREAM_TYPE = AUDIO_DELTA + 1
    const val AUDIO_SLIDER_SHOW = AUDIO_STREAM_TYPE + 1
    const val NAV_BAR_COLOR = AUDIO_SLIDER_SHOW + 1
    const val LOCKED_CONTENT = NAV_BAR_COLOR + 1
    const val SUPPORT = LOCKED_CONTENT + 1
    const val ROTATION_HISTORY = SUPPORT + 1
}

private val tabItems = listOf(
    Tab.Gestures to intArrayOf(
        ItemSorter.MAP_UP_ICON, ItemSorter.MAP_DOWN_ICON, ItemSorter.MAP_LEFT_ICON, ItemSorter.MAP_RIGHT_ICON,
        ItemSorter.AD_FREE, ItemSorter.SUPPORT, ItemSorter.REVIEW, ItemSorter.LOCKED_CONTENT
    ),
    Tab.Brightness to intArrayOf(
        ItemSorter.SLIDER_DELTA, ItemSorter.DISCRETE_BRIGHTNESS, ItemSorter.SCREEN_DIMMER, ItemSorter.USE_LOGARITHMIC_SCALE,
        ItemSorter.SHOW_SLIDER, ItemSorter.ADAPTIVE_BRIGHTNESS, ItemSorter.ANIMATES_SLIDER, ItemSorter.ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS,
        ItemSorter.DOUBLE_SWIPE_SETTINGS
    ),
    Tab.Audio to intArrayOf(
        ItemSorter.AUDIO_DELTA, ItemSorter.AUDIO_STREAM_TYPE, ItemSorter.AUDIO_SLIDER_SHOW
    ),
    Tab.Shortcuts to intArrayOf(
        ItemSorter.ENABLE_ACCESSIBILITY_BUTTON, ItemSorter.ACCESSIBILITY_SINGLE_CLICK,
        ItemSorter.ANIMATES_POPUP, ItemSorter.ENABLE_WATCH_WINDOWS, ItemSorter.POPUP_ACTION,
        ItemSorter.ROTATION_LOCK, ItemSorter.EXCLUDED_ROTATION_LOCK, ItemSorter.ROTATION_HISTORY
    ),
    Tab.Display to intArrayOf(
        ItemSorter.SLIDER_POSITION, ItemSorter.SLIDER_DURATION, ItemSorter.NAV_BAR_COLOR,
        ItemSorter.SLIDER_COLOR, ItemSorter.WALLPAPER_PICKER, ItemSorter.WALLPAPER_TRIGGER
    )
).toMap()
