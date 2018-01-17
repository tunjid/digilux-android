package com.tunjid.fingergestures.fragments;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseFragment;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.baseclasses.FingerGestureFragment;

import java.util.Arrays;

import static android.support.v7.widget.DividerItemDecoration.VERTICAL;

public class AppFragment extends FingerGestureFragment
        implements
        AppAdapter.HomeAdapterListener {

    private static final String ARGS_ITEM = "items";

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
    public String getStableTag() {
        return Arrays.toString(getArguments().getIntArray(ARGS_ITEM));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        items = getArguments().getIntArray(ARGS_ITEM);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_home, container, false);
        Context context = getContext();
        DividerItemDecoration itemDecoration = new DividerItemDecoration(context, VERTICAL);
        itemDecoration.setDrawable(ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_dark));

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
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FloatingActionButton fab = getFab();
        fab.setImageResource(R.drawable.ic_settings_white_24dp);
    }

    @Override
    public void onResume() {
        super.onResume();
        recyclerView.getAdapter().notifyDataSetChanged();
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

    @Nullable
    @Override
    @SuppressLint("CommitTransaction")
    public FragmentTransaction provideFragmentTransaction(BaseFragment fragmentTo) {
        return getFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
