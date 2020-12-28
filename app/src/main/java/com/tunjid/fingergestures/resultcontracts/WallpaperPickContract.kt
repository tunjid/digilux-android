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

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import com.tunjid.androidx.core.delegates.intentExtras
import com.tunjid.fingergestures.WallpaperSelection

private var Intent.lastSelection by intentExtras<WallpaperSelection>()

class WallpaperPickContract(
    private val cacheProvider: () -> Intent
) : ActivityResultContract<WallpaperSelection, Pair<WallpaperSelection, Uri>?>() {

    override fun createIntent(context: Context, input: WallpaperSelection): Intent {
        cacheProvider().lastSelection = input
        return ActivityResultContracts.StartActivityForResult().createIntent(context, Intent.createChooser(Intent()
            .setType("image/*")
            .setAction(Intent.ACTION_GET_CONTENT), ""))
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Pair<WallpaperSelection, Uri>? =
        intent?.data?.let { cacheProvider().lastSelection to it }
}