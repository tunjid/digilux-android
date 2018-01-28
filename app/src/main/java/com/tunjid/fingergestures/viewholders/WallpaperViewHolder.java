package com.tunjid.fingergestures.viewholders;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.FileProvider;
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
        itemView.findViewById(R.id.edit_wallpaper).setOnClickListener(view ->
                backgroundManager.requestWallPaperConstant(R.string.choose_edit_source, itemView.getContext(), this::requestEdit));

        setAspectRatio(current);
        setAspectRatio(day);
        setAspectRatio(night);
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
        loadImage(DAY_WALLPAPER_PICK_CODE, day);
        loadImage(NIGHT_WALLPAPER_PICK_CODE, night);
    }

    private void setAspectRatio(ImageView imageView) {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) imageView.getLayoutParams();
        params.dimensionRatio = backgroundManager.getScreenDimensionRatio();
    }

    private void requestEdit(@BackgroundManager.WallpaperSelection int selection) {
        Context context = itemView.getContext();
        File file = backgroundManager.getWallpaperFile(selection);

        if (!file.exists()) {
            adapterListener.showSnackbar(R.string.error_wallpaper_not_created);
            return;
        }

        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".com.tunjid.fingergestures.wallpaperprovider", file);
        Intent editIntent = new Intent(Intent.ACTION_EDIT);

        editIntent.setDataAndType(uri, "image/*");
        editIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(Intent.createChooser(editIntent, null));
    }

    private void loadImage(@BackgroundManager.WallpaperSelection int selection, ImageView imageView) {
        File file = backgroundManager.getWallpaperFile(selection);
        if (!file.exists()) return;

        Picasso.with(itemView.getContext()).load(file)
                .memoryPolicy(MemoryPolicy.NO_CACHE)
                .fit()
                .noFade()
                .centerCrop()
                .into(imageView);
    }
}
