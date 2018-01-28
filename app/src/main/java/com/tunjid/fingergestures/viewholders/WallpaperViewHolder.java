package com.tunjid.fingergestures.viewholders;

import android.app.WallpaperManager;
import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.ImageView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.activities.MainActivity;
import com.tunjid.fingergestures.adapters.AppAdapter;

import java.io.File;

public class WallpaperViewHolder extends AppViewHolder {

    private ImageView current;
    private ImageView main;
    private ImageView alt;
    private BackgroundManager backgroundManager;

    public WallpaperViewHolder(View itemView, AppAdapter.AppAdapterListener appAdapterListener) {
        super(itemView, appAdapterListener);
        current = itemView.findViewById(R.id.current_wallpaper);
        main = itemView.findViewById(R.id.main_wallpaper);
        alt = itemView.findViewById(R.id.alt_wallpaper);
        backgroundManager = BackgroundManager.getInstance();

        main.setOnClickListener(view -> adapterListener.pickWallpaper(BackgroundManager.MAIN_WALLPAPER_PICK_CODE));
        alt.setOnClickListener(view -> adapterListener.pickWallpaper(BackgroundManager.ALT_WALLPAPER_PICK_CODE));

        setAspectRatio(current);
        setAspectRatio(main);
        setAspectRatio(alt);
    }

    @Override
    public void bind() {
        super.bind();
        if (App.hasStoragePermission()) {
            Context context = itemView.getContext();

            WallpaperManager wallpaperManager = context.getSystemService(WallpaperManager.class);
            if (wallpaperManager == null) return;

            current.setImageDrawable(wallpaperManager.getDrawable());
            wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM);
            wallpaperManager.getWallpaperInfo();
        }
        else {
            adapterListener.requestPermission(MainActivity.STORAGE_CODE);
        }
        loadImage(BackgroundManager.MAIN_WALLPAPER_PICK_CODE, main);
        loadImage(BackgroundManager.ALT_WALLPAPER_PICK_CODE, alt);
    }

    private void setAspectRatio(ImageView imageView) {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) imageView.getLayoutParams();
        params.dimensionRatio = backgroundManager.getScreenDimensionRatio();
    }

    private void loadImage(@BackgroundManager.WallpaperSelection int selection, ImageView imageView) {
        File file = backgroundManager.getWallpaperFile(selection);
        if (!file.exists()) return;

        Picasso.with(itemView.getContext()).load(file)
                .memoryPolicy(MemoryPolicy.NO_CACHE)
                .fit()
                .centerCrop()
                .into(imageView);
    }
}
