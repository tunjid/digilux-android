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

package com.tunjid.fingergestures.models

import android.content.pm.ApplicationInfo
import com.tunjid.androidx.recyclerview.diff.Differentiable
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.activities.MainActivity
import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer
import com.tunjid.fingergestures.viewmodels.AppViewModel

data class AppState(
        val links: List<TextLink> = listOf(),
        val brightnessValues: List<Brightness> = listOf(),
        val popUpActions: List<Action> = listOf(),
        val availableActions: List<Action> = listOf(),
        val installedApps: List<Package> = listOf(),
        val permissionsQueue: List<Int> = listOf(),
        val items: List<Item> = listOf(),
)

data class Package(val app: ApplicationInfo) : Differentiable {
    override val diffId: String get() = app.packageName

    override fun areContentsTheSame(other: Differentiable): Boolean =
            (other as? Package)?.let { it.diffId == diffId } ?: super.areContentsTheSame(other)
}

data class Action(@GestureConsumer.GestureAction val value: Int) : Differentiable {
    override val diffId: String get() = value.toString()
    override fun areContentsTheSame(other: Differentiable): Boolean =
            (other as? Action)?.let { it.value == value } ?: super.areContentsTheSame(other)
}

data class Brightness(val value: String) : Differentiable {
    override val diffId: String get() = value
    override fun areContentsTheSame(other: Differentiable): Boolean =
            (other as? Brightness)?.let { it.value == value } ?: super.areContentsTheSame(other)
}

data class UiUpdate(
        val titleRes: Int = R.string.blank_string,
        val iconRes: Int = R.drawable.ic_add_24dp,
        val fabVisible: Boolean = false
)

sealed class Shilling {
    object Calm : Shilling()
    data class Quip(val message: String) : Shilling()
}

val AppState.uiUpdate
    get() = when (permissionsQueue.lastOrNull()) {
        MainActivity.DO_NOT_DISTURB_CODE -> UiUpdate(
                titleRes = R.string.enable_do_not_disturb,
                iconRes = R.drawable.ic_volume_loud_24dp
        )
        MainActivity.ACCESSIBILITY_CODE -> UiUpdate(
                titleRes = R.string.enable_accessibility,
                iconRes = R.drawable.ic_human_24dp
        )
        MainActivity.SETTINGS_CODE -> UiUpdate(
                titleRes = R.string.enable_write_settings,
                iconRes = R.drawable.ic_settings_white_24dp
        )
        MainActivity.STORAGE_CODE -> UiUpdate(
                titleRes = R.string.enable_storage_settings,
                iconRes = R.drawable.ic_storage_24dp
        )
        else -> UiUpdate()
    }.copy(fabVisible = permissionsQueue.isNotEmpty())