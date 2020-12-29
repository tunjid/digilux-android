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

package com.tunjid.fingergestures.gestureconsumers

enum class GestureAction(val id: Int) {
    INCREASE_BRIGHTNESS(0),
    REDUCE_BRIGHTNESS(1),
    MAXIMIZE_BRIGHTNESS(2),
    MINIMIZE_BRIGHTNESS(3),

    // int NIGHT_MODE_ON = 4; DO NOT REMOVE FOR LEGACY REASONS
    // int NIGHT_MODE_OFF = 5; DO NOT REMOVE FOR LEGACY REASONS
    NOTIFICATION_UP(6),
    NOTIFICATION_DOWN(7),
    DO_NOTHING(8),
    TOGGLE_FLASHLIGHT(9),
    TOGGLE_DOCK(10),
    TOGGLE_AUTO_ROTATE(11),
    NOTIFICATION_TOGGLE(12),
    GLOBAL_HOME(13),
    GLOBAL_BACK(14),
    GLOBAL_RECENTS(15),
    GLOBAL_SPLIT_SCREEN(16),
    GLOBAL_POWER_DIALOG(17),
    SHOW_POPUP(18),
    INCREASE_AUDIO(19),
    REDUCE_AUDIO(20),
    GLOBAL_LOCK_SCREEN(21),
    GLOBAL_TAKE_SCREENSHOT(22);

    val serialized get() = id.toString()

    companion object {
        private val map = values().map { it.id to it }.toMap()
        fun fromId(id: Int) = map.getValue(id)
        fun deserialize(id: String) = fromId(id.toInt())
    }
}

interface GestureConsumer {

    fun onGestureActionTriggered(gestureAction: GestureAction)

    fun accepts(gesture: GestureAction): Boolean

    companion object {

        const val ZERO_PERCENT = 0
        const val FIFTY_PERCENT = 50
        const val HUNDRED_PERCENT = 100

        fun normalizePercentageToFraction(percentage: Int): Float = percentage / 100f
    }
}
