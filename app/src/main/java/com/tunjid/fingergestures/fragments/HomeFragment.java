package com.tunjid.fingergestures.fragments;


import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tunjid.fingergestures.FingerGestureService;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.HomeAdapter;
import com.tunjid.fingergestures.baseclasses.FingerGestureFragment;

import static android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES;
import static android.support.design.widget.Snackbar.LENGTH_SHORT;
import static android.support.v7.widget.DividerItemDecoration.VERTICAL;

public class HomeFragment extends FingerGestureFragment
implements HomeAdapter.HomeAdapterListener{

    private static final int SETTINGS_CODE = 200;
    private static final int ACCESSIBILITY_CODE = 300;

    private boolean fromSettings;
    private boolean fromAccessibility;

    public static HomeFragment newInstance() {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        RecyclerView recyclerView = root.findViewById(R.id.options_list);
        Context context = getContext();

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new HomeAdapter(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(context, VERTICAL));

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

        if (!fromSettings && !Settings.System.canWrite(getContext())) askForSettings();
        else if (!fromAccessibility && !isAccessibilityServiceEnabled()) askForAccessibility();

        fromSettings = false;
        fromAccessibility = false;
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case SETTINGS_CODE:
                fromSettings = true;
                showSnackbar(Settings.System.canWrite(getContext())
                        ? R.string.settings_permission_granted
                        : R.string.settings_permission_denied);
                break;
            case ACCESSIBILITY_CODE:
                fromAccessibility = true;
                showSnackbar(isAccessibilityServiceEnabled()
                        ? R.string.accessibility_permission_granted
                        : R.string.accessibility_permission_denied);
                break;
        }
    }

    @Override
    protected boolean showsFab() {
        return false;
    }

    @NonNull
    private Intent settingsIntent() {
        return new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getContext().getPackageName()));
    }

    @NonNull
    private Intent accessibilityIntent() {
        return new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
    }

    private void askForSettings() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.permission_required)
                .setMessage(R.string.settings_permission_request)
                .setPositiveButton(R.string.yes, (dialog, b) -> startActivityForResult(settingsIntent(), SETTINGS_CODE))
                .setNegativeButton(R.string.no, (dialog, b) -> dialog.dismiss())
                .show();
    }

    private void askForAccessibility() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.permission_required)
                .setMessage(R.string.accessibility_permissions_request)
                .setPositiveButton(R.string.yes, (dialog, b) -> startActivityForResult(accessibilityIntent(), ACCESSIBILITY_CODE))
                .setNegativeButton(R.string.no, (dialog, b) -> dialog.dismiss())
                .show();
    }

    private void showSnackbar(@StringRes int resource) {
        ViewGroup root = (ViewGroup) getView();
        if (root == null) return;
        Snackbar.make(root, resource, LENGTH_SHORT).show();
    }

    public boolean isAccessibilityServiceEnabled() {
        Context context = getContext();
        ContentResolver contentResolver = context.getContentResolver();
        ComponentName expectedComponentName = new ComponentName(context, FingerGestureService.class);
        String enabledServicesSetting = Settings.Secure.getString(contentResolver, ENABLED_ACCESSIBILITY_SERVICES);

        if (enabledServicesSetting == null) return false;

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);

        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);

            if (enabledService != null && enabledService.equals(expectedComponentName)) return true;
        }

        return false;
    }
}