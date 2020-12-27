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

package com.tunjid.fingergestures.viewholders

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Intent
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import com.tunjid.androidx.recyclerview.viewbinding.BindingViewHolder
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderDelegate
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderFrom
import com.tunjid.androidx.uidrivers.uiState
import com.tunjid.androidx.uidrivers.updatePartial
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.WallpaperSelection
import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.databinding.ViewholderWallpaperPickBinding
import com.tunjid.fingergestures.viewmodels.Input
import java.io.File

private var BindingViewHolder<ViewholderWallpaperPickBinding>.item by viewHolderDelegate<Item.WallpaperPick>()

fun ViewGroup.wallpaperPick() = viewHolderFrom(ViewholderWallpaperPickBinding::inflate).apply {
    binding.mainWallpaper.setOnClickListener { item.input.accept(Input.UiInteraction.WallpaperPick(WallpaperSelection.Day)) }
    binding.altWallpaper.setOnClickListener { item.input.accept(Input.UiInteraction.WallpaperPick(WallpaperSelection.Night)) }
    binding.shareDayWallpaper.setOnClickListener { requestEdit(WallpaperSelection.Day) }
    binding.shareNightWallpaper.setOnClickListener { requestEdit(WallpaperSelection.Night) }
}

@SuppressLint("MissingPermission")
fun BindingViewHolder<ViewholderWallpaperPickBinding>.bind(item: Item.WallpaperPick) = binding.run {
    this@bind.item = item

    setAspectRatio(currentWallpaper)
    setAspectRatio(mainWallpaper)
    setAspectRatio(altWallpaper)

    if (App.hasStoragePermission) {
        val context = itemView.context
        val wallpaperManager = context.getSystemService(WallpaperManager::class.java) ?: return@run

        currentWallpaper.setImageDrawable(wallpaperManager.drawable)
        wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)
        wallpaperManager.wallpaperInfo
    } else item.input.accept(Input.Permission.Request.Storage)

    item.dayFile?.let(mainWallpaper::loadImage)
    item.nightFile?.let(altWallpaper::loadImage)
}

private fun BindingViewHolder<ViewholderWallpaperPickBinding>.setAspectRatio(imageView: ImageView) {
    val params = imageView.layoutParams as ConstraintLayout.LayoutParams
    params.dimensionRatio = item.screenDimensionRatio
}

private fun BindingViewHolder<ViewholderWallpaperPickBinding>.requestEdit(selection: WallpaperSelection) {
    val file = when (selection) {
        WallpaperSelection.Day -> item.dayFile
        WallpaperSelection.Night -> item.nightFile
    }
        ?.takeIf(File::exists)
        ?: return itemView::uiState.updatePartial {
            copy(snackbarText = itemView.context.getString(R.string.error_wallpaper_not_created))
        }


    val context = itemView.context
    val uri = FileProvider.getUriForFile(context, context.packageName + ".com.tunjid.fingergestures.wallpaperprovider", file)
    val editIntent = Intent(Intent.ACTION_EDIT)

    editIntent.setDataAndType(uri, "image/*")
    editIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

    context.startActivity(Intent.createChooser(
        editIntent,
        context.getString(R.string.choose_edit_source_message),
        item.editWallPaperPendingIntent.intentSender
    ))
}

private fun ImageView.loadImage(file: File) {
    if (file.exists()) Picasso.get().load(file)
        .memoryPolicy(MemoryPolicy.NO_CACHE)
        .fit()
        .noFade()
        .centerCrop()
        .into(this)
}
