package com.tunjid.fingergestures;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import static android.provider.Settings.Secure.*;
import static android.support.design.widget.Snackbar.LENGTH_SHORT;

public class MainActivity extends AppCompatActivity {

    private static final int SETTINGS_CODE = 200;
    private static final int ACCESSIBILITY_CODE = 300;

    private boolean fromSettings;
    private boolean fromAccessibility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!fromSettings && !Settings.System.canWrite(this)) askForSettings();
        else if (!fromAccessibility && !isAccessibilityServiceEnabled()) askForAccessibility();

        fromSettings = false;
        fromAccessibility = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        // finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case SETTINGS_CODE:
                fromSettings = true;
                showSnackbar(Settings.System.canWrite(this)
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

    @NonNull
    private Intent settingsIntent() {
        return new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
    }

    @NonNull
    private Intent accessibilityIntent() {
        return new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
    }

    private void askForSettings() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(R.string.settings_permission_request)
                .setPositiveButton(R.string.yes, (dialog, b) -> startActivityForResult(settingsIntent(), SETTINGS_CODE))
                .setNegativeButton(R.string.no, (dialog, b) -> dialog.dismiss())
                .show();
    }

    private void askForAccessibility() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(R.string.accessibility_permissions_request)
                .setPositiveButton(R.string.yes, (dialog, b) -> startActivityForResult(accessibilityIntent(), ACCESSIBILITY_CODE))
                .setNegativeButton(R.string.no, (dialog, b) -> dialog.dismiss())
                .show();
    }

    private void showSnackbar(@StringRes int resource) {
        Snackbar.make(findViewById(R.id.container), resource, LENGTH_SHORT).show();
    }

    public boolean isAccessibilityServiceEnabled() {
        ComponentName expectedComponentName = new ComponentName(this, FingerGestureService.class);
        String enabledServicesSetting = Settings.Secure.getString(getContentResolver(), ENABLED_ACCESSIBILITY_SERVICES);

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
