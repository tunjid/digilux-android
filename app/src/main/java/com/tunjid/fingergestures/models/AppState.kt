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

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.fragment.app.Fragment
import com.tunjid.androidx.recyclerview.diff.Differentiable
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.WallpaperSelection
import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer

data class AppState(
    val purchasesState: PurchasesManager.State,
    val links: List<TextLink> = listOf(),
    val popUpActions: List<Action> = listOf(),
    val availableActions: List<Action> = listOf(),
    val installedApps: List<Package> = listOf(),
    val broadcasts: Intent? = null,
    val uiInteraction: Input.UiInteraction,
    val permissionState: PermissionState = PermissionState(),
    val items: List<Item> = listOf(),
)

sealed class Input {
    sealed class Permission : Input() {
        sealed class Request(val code: Int) : Permission() {
            object Storage : Request(100)
            object Settings : Request(200)
            object Accessibility : Request(300)
            object DoNotDisturb : Request(400)
            companion object {
                private val values get() = listOf(Storage, Settings, Accessibility, DoNotDisturb)
                fun forCode(code: Int) = values.find { it.code == code }
            }
        }

        sealed class Action : Permission() {
            data class Clear(val time: Long = System.currentTimeMillis()) : Action()
            data class Clicked(val time: Long = System.currentTimeMillis()) : Action()
            data class Changed(val request: Request) : Action()
        }
    }

    sealed class UiInteraction : Input() {
        object Default : UiInteraction()
        data class ShowSheet(val fragment: Fragment) : UiInteraction()
        data class GoPremium(val description: Int) : UiInteraction()
        data class Purchase(val sku: PurchasesManager.Sku) : UiInteraction()
        data class WallpaperPick(val selection: WallpaperSelection) : UiInteraction()
    }
}

data class Unique<T>(
    val item: T,
    val time: Long = System.currentTimeMillis()
)

data class PermissionState(
    val queue: List<Input.Permission.Request> = listOf(),
    val active: Unique<Input.Permission.Request>? = null,
    val prompt: Unique<Int>? = null,
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
    get() = when (permissionState.queue.lastOrNull()) {
        Input.Permission.Request.DoNotDisturb -> UiUpdate(
            titleRes = R.string.enable_do_not_disturb,
            iconRes = R.drawable.ic_volume_loud_24dp
        )
        Input.Permission.Request.Accessibility -> UiUpdate(
            titleRes = R.string.enable_accessibility,
            iconRes = R.drawable.ic_human_24dp
        )
        Input.Permission.Request.Settings -> UiUpdate(
            titleRes = R.string.enable_write_settings,
            iconRes = R.drawable.ic_settings_white_24dp
        )
        Input.Permission.Request.Storage -> UiUpdate(
            titleRes = R.string.enable_storage_settings,
            iconRes = R.drawable.ic_storage_24dp
        )
        else -> UiUpdate()
    }.copy(fabVisible = permissionState.queue.isNotEmpty())