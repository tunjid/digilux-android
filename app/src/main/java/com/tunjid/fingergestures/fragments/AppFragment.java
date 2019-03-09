package com.tunjid.fingergestures.fragments;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.theartofdev.edmodo.cropper.CropImage;
import com.tunjid.androidbootstrap.core.abstractclasses.BaseFragment;
import com.tunjid.androidbootstrap.recyclerview.ListManager;
import com.tunjid.androidbootstrap.recyclerview.ListManagerBuilder;
import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.baseclasses.MainActivityFragment;
import com.tunjid.fingergestures.viewholders.AppViewHolder;
import com.tunjid.fingergestures.viewmodels.AppViewModel;

import java.io.File;
import java.util.Arrays;
import java.util.stream.IntStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;

import static com.tunjid.fingergestures.BackgroundManager.DAY_WALLPAPER_PICK_CODE;
import static com.tunjid.fingergestures.BackgroundManager.NIGHT_WALLPAPER_PICK_CODE;
import static java.lang.Math.abs;

public class AppFragment extends MainActivityFragment
        implements
        AppAdapter.AppAdapterListener {

    private static final String ARGS_ITEM = "items";
    private static final String IMAGE_SELECTION = "image/*";

    private int[] items;
    private ListManager<AppViewHolder, Void> listManager;

    public static AppFragment newInstance(int[] items) {
        AppFragment fragment = new AppFragment();
        Bundle args = new Bundle();

        args.putIntArray(ARGS_ITEM, items);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public String getStableTag() {
        return Arrays.toString(getArguments().getIntArray(ARGS_ITEM));
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        items = getArguments().getIntArray(ARGS_ITEM);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_home, container, false);

        listManager = new ListManagerBuilder<AppViewHolder, Void>()
                .withRecyclerView(root.findViewById(R.id.options_list))
                .withAdapter(new AppAdapter(items, this))
                .withLinearLayoutManager()
                .addDecoration(divider())
                .addScrollListener((dx, dy) -> { if (abs(dy) > 3) toggleToolbar(dy < 0); })
                .build();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        notifyDataSetChanged();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        toggleToolbar(true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Activity activity = getActivity();
        if (activity == null) return;

        if (resultCode != Activity.RESULT_OK) {
            showSnackbar(R.string.cancel_wallpaper);
        }
        else if (requestCode == DAY_WALLPAPER_PICK_CODE || requestCode == NIGHT_WALLPAPER_PICK_CODE) {
            cropImage(data.getData(), requestCode);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listManager.clear();
        listManager = null;
    }

    @Override
    public void pickWallpaper(@BackgroundManager.WallpaperSelection int selection) {
        if (!App.hasStoragePermission()) showSnackbar(R.string.enable_storage_settings);
        else startActivityForResult(Intent.createChooser(new Intent()
                .setType(IMAGE_SELECTION)
                .setAction(Intent.ACTION_GET_CONTENT), ""), selection);
    }

    public int[] getItems() {
        return items;
    }

    public void notifyItemChanged(@AppViewModel.AdapterIndex int position) {
        int index = IntStream.range(0, items.length).filter(i -> items[i] == position).findFirst().orElse(-1);
        if (index != -1) listManager.notifyItemChanged(index);
    }

    public void notifyDataSetChanged() {
        listManager.notifyDataSetChanged();
    }

    @Nullable
    @Override
    @SuppressLint("CommitTransaction")
    @SuppressWarnings("ConstantConditions")
    public FragmentTransaction provideFragmentTransaction(BaseFragment fragmentTo) {
        return getFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.fade_in, android.R.anim.fade_out);
    }

    public void cropImage(Uri source, @BackgroundManager.WallpaperSelection int selection) {
        BackgroundManager backgroundManager = BackgroundManager.getInstance();
        int[] aspectRatio = backgroundManager.getScreenAspectRatio();
        if (aspectRatio == null) return;

        Activity activity = getActivity();
        if (activity == null) return;

        File file = backgroundManager.getWallpaperFile(selection, activity);
        Uri destination = Uri.fromFile(file);

        CropImage.activity(source)
                .setOutputUri(destination)
                .setFixAspectRatio(true)
                .setAspectRatio(aspectRatio[0], aspectRatio[1])
                .setMinCropWindowSize(100, 100)
                .setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                .start(activity, this);
    }
}
