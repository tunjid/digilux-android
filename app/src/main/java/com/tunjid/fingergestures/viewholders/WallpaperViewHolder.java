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

package com.tunjid.fingergestures.viewholders;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import android.view.View;
import android.widget.ImageView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.activities.MainActivity;
import com.tunjid.fingergestures.adapters.AppAdapter;

import java.io.File;

import static com.tunjid.fingergestures.BackgroundManager.DAY_WALLPAPER_PICK_CODE;
import static com.tunjid.fingergestures.BackgroundManager.NIGHT_WALLPAPER_PICK_CODE;

public class WallpaperViewHolder extends AppViewHolder {

    private ImageView current;
    private ImageView day;
    private ImageView night;
    private BackgroundManager backgroundManager;

    public WallpaperViewHolder(View itemView, AppAdapter.AppAdapterListener appAdapterListener) {
        super(itemView, appAdapterListener);
        current = itemView.findViewById(R.id.current_wallpaper);
        day = itemView.findViewById(R.id.main_wallpaper);
        night = itemView.findViewById(R.id.alt_wallpaper);
        backgroundManager = BackgroundManager.getInstance();

        day.setOnClickListener(view -> adapterListener.pickWallpaper(DAY_WALLPAPER_PICK_CODE));
        night.setOnClickListener(view -> adapterListener.pickWallpaper(NIGHT_WALLPAPER_PICK_CODE));
        itemView.findViewById(R.id.share_day_wallpaper).setOnClickListener(view -> requestEdit(DAY_WALLPAPER_PICK_CODE));
        itemView.findViewById(R.id.share_night_wallpaper).setOnClickListener(view -> requestEdit(NIGHT_WALLPAPER_PICK_CODE));

        setAspectRatio(current);
        setAspectRatio(day);
        setAspectRatio(night);
    }

    @Override
    @SuppressLint("MissingPermission")
    public void bind() {
        super.bind();
        if (App.Companion.getHasStoragePermission()) {
            Context context = itemView.getContext();

            WallpaperManager wallpaperManager = context.getSystemService(WallpaperManager.class);
            if (wallpaperManager == null) return;

            current.setImageDrawable(wallpaperManager.getDrawable());
            wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM);
            wallpaperManager.getWallpaperInfo();
        }
        else {
            adapterListener.requestPermission(MainActivity.Companion.getSTORAGE_CODE());
        }
        loadImage(DAY_WALLPAPER_PICK_CODE, day);
        loadImage(NIGHT_WALLPAPER_PICK_CODE, night);
    }

    private void setAspectRatio(ImageView imageView) {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) imageView.getLayoutParams();
        params.dimensionRatio = backgroundManager.getScreenDimensionRatio();
    }

    private void requestEdit(@BackgroundManager.WallpaperSelection int selection) {
        Context context = itemView.getContext();
        File file = backgroundManager.getWallpaperFile(selection, context);

        if (!file.exists()) {
            adapterListener.showSnackbar(R.string.error_wallpaper_not_created);
            return;
        }

        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".com.tunjid.fingergestures.wallpaperprovider", file);
        Intent editIntent = new Intent(Intent.ACTION_EDIT);

        editIntent.setDataAndType(uri, "image/*");
        editIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(Intent.createChooser(editIntent,
                context.getString(R.string.choose_edit_source_message),
                backgroundManager.getWallpaperEditPendingIntent(context).getIntentSender()));
    }

    private void loadImage(@BackgroundManager.WallpaperSelection int selection, ImageView imageView) {
        Context context = imageView.getContext();
        File file = backgroundManager.getWallpaperFile(selection, context);
        if (!file.exists()) return;

        Picasso.with(context).load(file)
                .memoryPolicy(MemoryPolicy.NO_CACHE)
                .fit()
                .noFade()
                .centerCrop()
                .into(imageView);
    }
}
