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

package com.tunjid.fingergestures.resultcontracts

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.models.Input

class PermissionRequestContract : ActivityResultContract<Input.Permission.Request, Input.Permission.Action.Changed?>() {
    // Super hacky, but I really don't want to have 3 contracts in main activity
    private var request: Input.Permission.Request? = null

    override fun createIntent(context: Context, input: Input.Permission.Request): Intent {
        request = input
        return when (input) {
            Input.Permission.Request.DoNotDisturb ->
                ActivityResultContracts.StartActivityForResult().createIntent(context, App.doNotDisturbIntent)
            Input.Permission.Request.Accessibility ->
                ActivityResultContracts.StartActivityForResult().createIntent(context, App.accessibilityIntent)
            Input.Permission.Request.Settings ->
                ActivityResultContracts.StartActivityForResult().createIntent(context, App.settingsIntent)
            Input.Permission.Request.Storage -> {
                ActivityResultContracts.RequestPermission().createIntent(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Input.Permission.Action.Changed? {
        return request?.let(Input.Permission.Action::Changed)
    }
}