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

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.SkuDetails
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.theartofdev.edmodo.cropper.CropImage
import com.tunjid.fingergestures.*
import com.tunjid.fingergestures.managers.PurchasesManager
import com.tunjid.fingergestures.managers.WallpaperSelection
import com.tunjid.fingergestures.managers.getWallpaperFile
import com.tunjid.fingergestures.managers.screenAspectRatio
import com.tunjid.fingergestures.models.*
import com.tunjid.fingergestures.ui.BottomSheetController
import java.util.*

interface MainApp : LifecycleOwner, ActivityResultCaller, BottomSheetController {
    val permissionContract: ActivityResultLauncher<Input.Permission.Request>
    val wallpaperPickContract: ActivityResultLauncher<WallpaperSelection>
    val cropWallpaperContract: ActivityResultLauncher<Intent>

    val activity: Activity
    var uiState: UiState
    val inputs: Inputs
}

fun MainApp.observe(state: LiveData<State>) = with(state) {
    mapDistinct(State::fabState)
        .observe(this@observe, ::onStateChanged)

    mapDistinct(State::permissionState)
        .mapDistinct(PermissionState::active)
        .nonNull()
        .map(Unique<Input.Permission.Request>::item)
        .filterUnhandledEvents()
        .observe(this@observe, ::onPermissionClicked)

    mapDistinct(State::permissionState)
        .mapDistinct(PermissionState::prompt)
        .nonNull()
        .map(Unique<Int>::item)
        .filterUnhandledEvents()
        .observe(this@observe, ::showSnackbar)

    mapDistinct(State::uiInteraction)
        .filterUnhandledEvents()
        .observe(this@observe, ::onUiInteraction)

    mapDistinct(State::broadcasts)
        .filter(Optional<Broadcast.Prompt>::isPresent)
        .map(Optional<Broadcast.Prompt>::get)
        .filterUnhandledEvents()
        .observe(this@observe, ::onBroadcastReceived)

    mapDistinct(State::billingState)
        .mapDistinct(BillingState::cart)
        .nonNull()
        .map(Unique<Pair<BillingClient, SkuDetails>>::item)
        .filterUnhandledEvents()
        .observe(this@observe, ::purchase)

    mapDistinct(State::purchasesState)
        .filterUnhandledEvents()
        .observe(this@observe) {
            ::uiState.updatePartial { copy(toolbarInvalidated = true) }
        }
    mapDistinct(State::shilling)
        .observe(this@observe) {
            ::uiState.updatePartial { copy(shilling = it) }
        }
}

fun MainApp.cropImage(result: Pair<WallpaperSelection, Uri>) {
    val (selection, source) = result

    val aspectRatio = activity.screenAspectRatio
    val file = activity.getWallpaperFile(selection)

    val destination = Uri.fromFile(file)

    cropWallpaperContract.launch(CropImage.activity(source)
        .setOutputUri(destination)
        .setFixAspectRatio(true)
        .setAspectRatio(aspectRatio[0], aspectRatio[1])
        .setMinCropWindowSize(100, 100)
        .setOutputCompressFormat(Bitmap.CompressFormat.PNG)
        .getIntent(activity))
}

private fun MainApp.getString(@StringRes resId: Int, vararg formatArgs: Any?) = when {
    formatArgs.isEmpty() -> activity.getString(resId)
    else -> activity.getString(resId, *formatArgs)
}

private fun MainApp.onPermissionClicked(permissionRequest: Input.Permission.Request) =
    showPermissionDialog(permissionRequest.prompt) {
        permissionContract.launch(permissionRequest)
    }

private fun MainApp.onUiInteraction(it: Input.UiInteraction) =
    when (it) {
        is Input.UiInteraction.ShowSheet -> bottomSheetNavigator.push(it.fragment).let { }
        is Input.UiInteraction.GoPremium -> MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.go_premium_title)
            .setMessage(getString(R.string.go_premium_body, getString(it.description)))
            .setPositiveButton(R.string.continue_text) { _, _ -> inputs.accept(Input.Billing.Purchase(PurchasesManager.Sku.Premium)) }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .show()
            .let { }
        is Input.UiInteraction.WallpaperPick -> when {
            activity.hasStoragePermission -> wallpaperPickContract.launch(it.selection)
            else -> ::uiState.updatePartial { copy(snackbarText = getString(R.string.enable_storage_settings)) }
        }
        is Input.UiInteraction.PurchaseResult -> ::uiState.updatePartial { copy(snackbarText = getString(it.messageRes)) }
        Input.UiInteraction.Default -> Unit
    }

private fun MainApp.onStateChanged(fabState: FabState) {
    uiState = uiState.copy(
        fabIcon = fabState.iconRes,
        fabText = getString(fabState.titleRes),
        fabShows = fabState.fabVisible
    )
}

private fun MainApp.onBroadcastReceived(broadcast: Broadcast.Prompt) {
    ::uiState.updatePartial { copy(snackbarText = broadcast.message) }
}

private fun MainApp.showSnackbar(@StringRes resource: Int) = ::uiState.updatePartial {
    copy(snackbarText = getString(resource))
}

private fun MainApp.purchase(cart: Pair<BillingClient, SkuDetails>) {
    val (client, skuDetails) = cart
    val result = client.launchBillingFlow(activity, BillingFlowParams.newBuilder()
        .setSkuDetails(skuDetails)
        .build())

    when (result.responseCode) {
        BillingClient.BillingResponseCode.OK -> Unit
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> showSnackbar(R.string.billing_not_connected)
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> showSnackbar(R.string.billing_you_own_this)
        else -> showSnackbar(R.string.generic_error)
    }
}

private fun MainApp.showPermissionDialog(@StringRes stringRes: Int, yesAction: () -> Unit) {
    MaterialAlertDialogBuilder(activity)
        .setTitle(R.string.permission_required)
        .setMessage(stringRes)
        .setPositiveButton(R.string.yes) { _, _ -> yesAction.invoke() }
        .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
        .show()
}
