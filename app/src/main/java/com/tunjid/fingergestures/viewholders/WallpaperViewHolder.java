package com.tunjid.fingergestures.viewholders;

import android.app.WallpaperManager;
import android.view.View;
import android.widget.ImageView;

import com.tunjid.fingergestures.R;

public class WallpaperViewHolder extends AppViewHolder {

   private ImageView main;
   private ImageView alt;

    public WallpaperViewHolder(View itemView) {
        super(itemView);
        main = itemView.findViewById(R.id.main_wallpaper);
        alt = itemView.findViewById(R.id.alt_wallpaper);
    }

    @Override
    public void bind() {
        super.bind();

        WallpaperManager wallpaperManager = itemView.getContext().getSystemService(WallpaperManager.class);
        if (wallpaperManager == null) return;

        main.setImageDrawable(wallpaperManager.getDrawable());
        wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM);
        wallpaperManager.getWallpaperInfo();
    }
}
