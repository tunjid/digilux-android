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
import android.os.Parcelable
import androidx.fragment.app.Fragment
import com.android.billingclient.api.BillingClient
import com.tunjid.androidx.recyclerview.diff.Differentiable
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.managers.WallpaperSelection
import com.tunjid.fingergestures.managers.PurchasesManager
import com.tunjid.fingergestures.gestureconsumers.GestureAction
import com.tunjid.fingergestures.ui.main.Item
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables
import kotlinx.parcelize.Parcelize
import java.util.*

data class AppState(
    val shilling: Shilling,
    val purchasesState: PurchasesManager.State,
    val links: List<TextLink> = listOf(),
    val broadcasts: Optional<Broadcast.Prompt> = Optional.empty(),
    val uiInteraction: Input.UiInteraction,
    val permissionState: PermissionState = PermissionState(),
    val billingState: BillingState = BillingState(),
    val items: List<Item> = listOf(),
)

sealed class Input {
    sealed class Permission : Input() {
        sealed class Request(val prompt: Int) : Permission(), Parcelable {
            @Parcelize
            object Storage : Request(R.string.wallpaper_permission_request)

            @Parcelize
            object Settings : Request(R.string.settings_permission_request)

            @Parcelize
            object Accessibility : Request(R.string.accessibility_permissions_request)

            @Parcelize
            object DoNotDisturb : Request(R.string.do_not_disturb_permissions_request)
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
        data class PurchaseResult(val messageRes: Int) : UiInteraction()
        data class WallpaperPick(val selection: WallpaperSelection) : UiInteraction()
    }

    sealed class Billing : Input() {
        data class Client(val client: BillingClient?) : Billing()
        data class Purchase(val sku: PurchasesManager.Sku) : Billing()
    }

    object StartTrial : Input()
    object Shill : Input()
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

data class BillingState(
    val client: BillingClient? = null,
    val prompt: Unique<Int>? = null,
    val cart: Unique<Pair<BillingClient, PurchasesManager.Sku>>? = null
)

data class Package(val app: ApplicationInfo) : Differentiable {
    override val diffId: String get() = app.packageName

    override fun areContentsTheSame(other: Differentiable): Boolean =
        (other as? Package)?.let { it.diffId == diffId } ?: super.areContentsTheSame(other)
}

data class Action(val value: GestureAction, val iconColor: Int) : Differentiable {
    override val diffId: String get() = value.toString()
    override fun areContentsTheSame(other: Differentiable): Boolean =
        (other as? Action)?.let { it.value == value } ?: super.areContentsTheSame(other)
}

data class Brightness(val value: Int) : Differentiable {
    override val diffId: String get() = value.toString()
    override fun areContentsTheSame(other: Differentiable): Boolean =
        (other as? Brightness)?.let { it.value == value } ?: super.areContentsTheSame(other)
}

data class UiUpdate(
    val titleRes: Int = R.string.blank_string,
    val iconRes: Int = R.drawable.ic_add_24dp,
    val fabVisible: Boolean = false
)

@Suppress("unused")
val Any?.ignore
    get() = Unit

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

fun Flowable<List<GestureAction>>.toPopUpActions(colorSource: Flowable<Int>) = Flowables.combineLatest(
    this,
    colorSource
) { gestures, color -> gestures.map { Action(it, color) } }