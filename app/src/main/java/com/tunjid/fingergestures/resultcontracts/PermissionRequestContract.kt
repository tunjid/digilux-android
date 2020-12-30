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
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import com.tunjid.androidx.core.delegates.intentExtras
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.models.Input

private var Intent.lastRequest by intentExtras<Input.Permission.Request>()

class PermissionRequestContract(
    private val cacheProvider: () -> Intent
) : ActivityResultContract<Input.Permission.Request, Input.Permission.Action.Changed>() {

    override fun createIntent(context: Context, input: Input.Permission.Request): Intent {
        cacheProvider().lastRequest = input
        return when (input) {
            Input.Permission.Request.DoNotDisturb ->
                ActivityResultContracts.StartActivityForResult().createIntent(context, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            Input.Permission.Request.Accessibility ->
                ActivityResultContracts.StartActivityForResult().createIntent(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Input.Permission.Request.Settings ->
                ActivityResultContracts.StartActivityForResult().createIntent(context, Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + context.packageName)
                ))
            Input.Permission.Request.Storage -> {
                ActivityResultContracts.RequestPermission().createIntent(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Input.Permission.Action.Changed =
        Input.Permission.Action.Changed(cacheProvider().lastRequest)
}