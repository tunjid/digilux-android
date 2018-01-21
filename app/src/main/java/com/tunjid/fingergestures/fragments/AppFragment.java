package com.tunjid.fingergestures.fragments;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;
import com.tunjid.androidbootstrap.core.abstractclasses.BaseFragment;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.WallpaperUtils;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.baseclasses.MainActivityFragment;

import java.io.File;
import java.util.Arrays;

import static android.support.v7.widget.DividerItemDecoration.VERTICAL;

public class AppFragment extends MainActivityFragment
        implements
        AppAdapter.AppAdapterListener {

    private static final String ARGS_ITEM = "items";
    private static final String IMAGE_SELECTION = "image/*";

    private int[] items;
    private RecyclerView recyclerView;

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
        Context context = inflater.getContext();
        DividerItemDecoration itemDecoration = new DividerItemDecoration(context, VERTICAL);
        Drawable decoration = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_dark);
        if (decoration != null) itemDecoration.setDrawable(decoration);

        recyclerView = root.findViewById(R.id.options_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new AppAdapter(items, this));
        recyclerView.addItemDecoration(itemDecoration);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (Math.abs(dy) < 3) return;
                toggleToolbar(dy < 0);
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Activity activity = getActivity();
        if (activity == null) return;

        if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(activity, R.string.cancel, Toast.LENGTH_SHORT).show();
            return;
        }

        if (requestCode == WallpaperUtils.MAIN_WALLPAPER_PICK_CODE || requestCode == WallpaperUtils.ALT_WALLPAPER_PICK_CODE) {
            int[] aspectRatio = WallpaperUtils.getScreenAspectRatio();
            File file = WallpaperUtils.getWallpaperFile(requestCode);
            Uri uri = Uri.fromFile(file);
            CropImage.activity(data.getData())
                    .setOutputUri(uri)
                    .setFixAspectRatio(true)
                    .setAspectRatio(aspectRatio[0], aspectRatio[1])
                    .setMinCropWindowSize(100, 100)
                    .setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                    .start(activity, this);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView = null;
    }

    @Override
    protected boolean showsFab() {
        return false;
    }

    @Override
    public void pickWallpaper(@WallpaperUtils.WallpaperSelection int selection) {
        Intent intent = new Intent();
        intent.setType(IMAGE_SELECTION);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, ""), selection);
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

    public void refresh() {
        if (recyclerView != null) recyclerView.getAdapter().notifyDataSetChanged();
    }
}
