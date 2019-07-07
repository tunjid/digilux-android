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
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.BackgroundManager
import com.tunjid.fingergestures.BackgroundManager.Companion.DAY_WALLPAPER_PICK_CODE
import com.tunjid.fingergestures.BackgroundManager.Companion.NIGHT_WALLPAPER_PICK_CODE
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.activities.MainActivity
import com.tunjid.fingergestures.adapters.AppAdapter

class WallpaperViewHolder(
        itemView: View,
        appAdapterListener: AppAdapter.AppAdapterListener
) : AppViewHolder(itemView, appAdapterListener) {

    private val backgroundManager: BackgroundManager = BackgroundManager.instance
    private val current: ImageView = itemView.findViewById(R.id.current_wallpaper)
    private val day: ImageView = itemView.findViewById(R.id.main_wallpaper)
    private val night: ImageView = itemView.findViewById(R.id.alt_wallpaper)

    init {

        day.setOnClickListener { adapterListener.pickWallpaper(DAY_WALLPAPER_PICK_CODE) }
        night.setOnClickListener { adapterListener.pickWallpaper(NIGHT_WALLPAPER_PICK_CODE) }
        itemView.findViewById<View>(R.id.share_day_wallpaper).setOnClickListener { requestEdit(DAY_WALLPAPER_PICK_CODE) }
        itemView.findViewById<View>(R.id.share_night_wallpaper).setOnClickListener { requestEdit(NIGHT_WALLPAPER_PICK_CODE) }

        setAspectRatio(current)
        setAspectRatio(day)
        setAspectRatio(night)
    }

    @SuppressLint("MissingPermission")
    override fun bind() {
        super.bind()
        if (App.hasStoragePermission) {
            val context = itemView.context

            val wallpaperManager = context.getSystemService(WallpaperManager::class.java) ?: return

            current.setImageDrawable(wallpaperManager.drawable)
            wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)
            wallpaperManager.wallpaperInfo
        } else {
            adapterListener.requestPermission(MainActivity.STORAGE_CODE)
        }
        loadImage(DAY_WALLPAPER_PICK_CODE, day)
        loadImage(NIGHT_WALLPAPER_PICK_CODE, night)
    }

    private fun setAspectRatio(imageView: ImageView) {
        val params = imageView.layoutParams as ConstraintLayout.LayoutParams
        params.dimensionRatio = backgroundManager.screenDimensionRatio
    }

    private fun requestEdit(@BackgroundManager.WallpaperSelection selection: Int) {
        val context = itemView.context
        val file = backgroundManager.getWallpaperFile(selection, context)

        if (!file.exists()) {
            adapterListener.showSnackbar(R.string.error_wallpaper_not_created)
            return
        }

        val uri = FileProvider.getUriForFile(context, context.packageName + ".com.tunjid.fingergestures.wallpaperprovider", file)
        val editIntent = Intent(Intent.ACTION_EDIT)

        editIntent.setDataAndType(uri, "image/*")
        editIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

        context.startActivity(Intent.createChooser(editIntent,
                context.getString(R.string.choose_edit_source_message),
                backgroundManager.getWallpaperEditPendingIntent(context).intentSender))
    }

    private fun loadImage(@BackgroundManager.WallpaperSelection selection: Int, imageView: ImageView) {
        val context = imageView.context
        val file = backgroundManager.getWallpaperFile(selection, context)
        if (!file.exists()) return

        Picasso.with(context).load(file)
                .memoryPolicy(MemoryPolicy.NO_CACHE)
                .fit()
                .noFade()
                .centerCrop()
                .into(imageView)
    }
}
