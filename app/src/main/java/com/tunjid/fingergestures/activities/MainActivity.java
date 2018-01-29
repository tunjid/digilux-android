package com.tunjid.fingergestures.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.tunjid.androidbootstrap.core.view.ViewHider;
import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.BuildConfig;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.baseclasses.FingerGestureActivity;
import com.tunjid.fingergestures.billing.PurchasesManager;
import com.tunjid.fingergestures.fragments.AppFragment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayDeque;
import java.util.Deque;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.tunjid.fingergestures.adapters.AppAdapter.ADAPTIVE_BRIGHTNESS;
import static com.tunjid.fingergestures.adapters.AppAdapter.ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS;
import static com.tunjid.fingergestures.adapters.AppAdapter.AD_FREE;
import static com.tunjid.fingergestures.adapters.AppAdapter.DOUBLE_SWIPE_SETTINGS;
import static com.tunjid.fingergestures.adapters.AppAdapter.MAP_DOWN_ICON;
import static com.tunjid.fingergestures.adapters.AppAdapter.MAP_LEFT_ICON;
import static com.tunjid.fingergestures.adapters.AppAdapter.MAP_RIGHT_ICON;
import static com.tunjid.fingergestures.adapters.AppAdapter.MAP_UP_ICON;
import static com.tunjid.fingergestures.adapters.AppAdapter.PADDING;
import static com.tunjid.fingergestures.adapters.AppAdapter.REVIEW;
import static com.tunjid.fingergestures.adapters.AppAdapter.SCREEN_DIMMER;
import static com.tunjid.fingergestures.adapters.AppAdapter.SHOW_SLIDER;
import static com.tunjid.fingergestures.adapters.AppAdapter.SLIDER_COLOR;
import static com.tunjid.fingergestures.adapters.AppAdapter.SLIDER_DELTA;
import static com.tunjid.fingergestures.adapters.AppAdapter.SLIDER_DURATION;
import static com.tunjid.fingergestures.adapters.AppAdapter.SLIDER_POSITION;
import static com.tunjid.fingergestures.adapters.AppAdapter.WALLPAPER_PICKER;
import static com.tunjid.fingergestures.adapters.AppAdapter.WALLPAPER_TRIGGER;

public class MainActivity extends FingerGestureActivity {

    public static final int STORAGE_CODE = 100;
    public static final int SETTINGS_CODE = 200;
    public static final int ACCESSIBILITY_CODE = 300;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STORAGE_CODE, SETTINGS_CODE, ACCESSIBILITY_CODE})
    public @interface PermissionRequest {}

    private static final String RX_JAVA_LINK = "https://github.com/ReactiveX/RxJava";
    private static final String COLOR_PICKER_LINK = "https://github.com/QuadFlask/colorpicker";
    private static final String ANDROID_BOOTSTRAP_LINK = "https://github.com/tunjid/android-bootstrap";
    private static final String GET_SET_ICON_LINK = "http://www.myiconfinder.com/getseticons";
    private static final String[] STORAGE_PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE};

    private AdView adView;
    private TextView permissionText;
    private ConstraintLayout constraintLayout;

    private final TextLink[] links;
    private final Deque<Integer> permissionsStack = new ArrayDeque<>();

    private final int[] GESTURE_ITEMS = {PADDING, MAP_UP_ICON, MAP_DOWN_ICON, MAP_LEFT_ICON, MAP_RIGHT_ICON, AD_FREE, REVIEW};
    private final int[] WALLPAPER_ITEMS = {PADDING, SLIDER_COLOR, WALLPAPER_PICKER, WALLPAPER_TRIGGER};
    private final int[] SLIDER_ITEMS = {PADDING, SLIDER_DELTA, SLIDER_POSITION, SLIDER_DURATION, SCREEN_DIMMER,
            SHOW_SLIDER, ADAPTIVE_BRIGHTNESS, ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS, DOUBLE_SWIPE_SETTINGS};

    {
        Context context = App.getInstance();
        links = new TextLink[]{new TextLink(context.getString(R.string.get_set_icon), GET_SET_ICON_LINK),
                new TextLink(context.getString(R.string.rxjava), RX_JAVA_LINK),
                new TextLink(context.getString(R.string.color_picker), COLOR_PICKER_LINK),
                new TextLink(context.getString(R.string.android_bootstrap), ANDROID_BOOTSTRAP_LINK)};
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorBackground));

        Toolbar toolbar = findViewById(R.id.toolbar);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        adView = findViewById(R.id.adView);
        constraintLayout = findViewById(R.id.constraint_layout);

        barHider = ViewHider.of(toolbar).setDirection(ViewHider.TOP).build();

        permissionText = findViewById(R.id.permission_view);
        permissionText.setOnClickListener(view -> onPermissionClicked());

        bottomNavigationView.setOnNavigationItemSelectedListener(this::onOptionsItemSelected);
        setSupportActionBar(toolbar);

        Intent startIntent = getIntent();
        boolean isPickIntent = startIntent != null && Intent.ACTION_SEND.equals(startIntent.getAction());

        if (savedInstanceState == null && isPickIntent) handleIntent(startIntent);
        else if (savedInstanceState == null) showFragment(AppFragment.newInstance(GESTURE_ITEMS));
    }

    @Override
    protected void onResume() {
        super.onResume();
        adView.resume();

        AdRequest.Builder builder = new AdRequest.Builder();
        if (BuildConfig.DEBUG) builder.addTestDevice("4853CDD3A8952349497550F27CC60ED3");

        boolean hasAds = PurchasesManager.getInstance().hasAds();
        adView.setVisibility(hasAds ? View.INVISIBLE : View.GONE);

        if (hasAds) {
            adView.loadAd(builder.build());
            adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    if (adView == null) return;
                    TransitionManager.beginDelayedTransition(constraintLayout, new AutoTransition());
                    adView.setVisibility(View.VISIBLE);
                }
            });
        }
        else hideAds();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_start_trial:
                if (PurchasesManager.getInstance().startTrial()) recreate();
                break;
            case R.id.action_directions:
                showFragment(AppFragment.newInstance(GESTURE_ITEMS));
                return true;
            case R.id.action_slider:
                showFragment(AppFragment.newInstance(SLIDER_ITEMS));
                return true;
            case R.id.action_wallpaper:
                showFragment(AppFragment.newInstance(WALLPAPER_ITEMS));
                return true;
            case R.id.info:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.open_source_libraries)
                        .setItems(links, (dialog, index) -> showLink(links[index]))
                        .show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onPause() {
        super.onPause();
        adView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adView != null) adView.destroy();
        adView = null;
        permissionsStack.clear();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        boolean shouldRemove = false;
        switch (requestCode) {
            case SETTINGS_CODE:
                showSnackbar((shouldRemove = App.canWriteToSettings())
                        ? R.string.settings_permission_granted
                        : R.string.settings_permission_denied);

                break;
            case ACCESSIBILITY_CODE:
                showSnackbar((shouldRemove = App.accessibilityServiceEnabled())
                        ? R.string.accessibility_permission_granted
                        : R.string.accessibility_permission_denied);
                break;
        }
        if (shouldRemove) permissionsStack.remove(requestCode);
        dismissPermissionsBar();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != STORAGE_CODE || grantResults.length == 0 || grantResults[0] != PERMISSION_GRANTED)
            return;

        permissionsStack.remove(requestCode);
        dismissPermissionsBar();
        AppFragment fragment = (AppFragment) getCurrentFragment();
        if (fragment != null) fragment.refresh();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    public void requestPermission(@PermissionRequest int permission) {
        if (permissionsStack.contains(permission)) permissionsStack.remove(permission);
        permissionsStack.push(permission);
        onPermissionAdded();
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();

        if (!Intent.ACTION_SEND.equals(action) || TextUtils.isEmpty(type) || !type.startsWith("image/"))
            return;

        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri == null) return;

        AppFragment toShow = AppFragment.newInstance(WALLPAPER_ITEMS);
        final String tag = toShow.getStableTag();

        showFragment(toShow);

        BackgroundManager.getInstance().requestWallPaperConstant(R.string.choose_target, this, selection -> {
            AppFragment shown = (AppFragment) getSupportFragmentManager().findFragmentByTag(tag);
            if (shown != null && shown.isVisible()) shown.cropImage(imageUri, selection);
            else showSnackbar(R.string.error_wallpaper);
        });
    }

    private void askForStorage() {
        showPermissionDialog(R.string.wallpaper_permission_request, () -> requestPermissions(STORAGE_PERMISSIONS, STORAGE_CODE));
    }

    private void askForSettings() {
        showPermissionDialog(R.string.settings_permission_request, () -> startActivityForResult(App.settingsIntent(), SETTINGS_CODE));
    }

    private void askForAccessibility() {
        showPermissionDialog(R.string.accessibility_permissions_request, () -> startActivityForResult(App.accessibilityIntent(), ACCESSIBILITY_CODE));
    }

    private void showPermissionDialog(@StringRes int stringRes, Runnable yesAction) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(stringRes)
                .setPositiveButton(R.string.yes, (dialog, b) -> yesAction.run())
                .setNegativeButton(R.string.no, (dialog, b) -> dialog.dismiss())
                .show();
    }

    private void onPermissionAdded() {
        if (permissionsStack.isEmpty()) return;
        int permissionRequest = permissionsStack.peek();
        int text = 0;

        if (permissionRequest == ACCESSIBILITY_CODE) text = R.string.enable_accessibility;
        else if (permissionRequest == SETTINGS_CODE) text = R.string.enable_write_settings;
        else if (permissionRequest == STORAGE_CODE) text = R.string.enable_storage_settings;

        if (text != 0) permissionText.setText(text);
        TransitionManager.beginDelayedTransition(constraintLayout, new AutoTransition());
        permissionText.setVisibility(View.VISIBLE);
    }

    private void dismissPermissionsBar() {
        if (!permissionsStack.isEmpty()) {
            onPermissionAdded();
            return;
        }
        TransitionManager.beginDelayedTransition(constraintLayout, new AutoTransition());
        permissionText.setVisibility(View.GONE);
    }

    private void onPermissionClicked() {
        if (permissionsStack.isEmpty()) {
            dismissPermissionsBar();
            return;
        }
        int permissionRequest = permissionsStack.peek();

        if (permissionRequest == ACCESSIBILITY_CODE) askForAccessibility();
        else if (permissionRequest == SETTINGS_CODE) askForSettings();
        else if (permissionRequest == STORAGE_CODE) askForStorage();
    }

    private void showLink(TextLink textLink) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(textLink.link));
        startActivity(browserIntent);
    }

    private void hideAds() {
        if (adView.getVisibility() == View.GONE) return;

        Transition hideTransition = new AutoTransition();
        hideTransition.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(@NonNull Transition transition) {

            }

            @Override
            public void onTransitionEnd(@NonNull Transition transition) {
                showSnackbar(R.string.billing_thanks);
            }

            @Override
            public void onTransitionCancel(@NonNull Transition transition) {

            }

            @Override
            public void onTransitionPause(@NonNull Transition transition) {

            }

            @Override
            public void onTransitionResume(@NonNull Transition transition) {

            }
        });
        android.transition.TransitionManager.beginDelayedTransition(constraintLayout, hideTransition);
        adView.setVisibility(View.GONE);
    }

    private static class TextLink implements CharSequence {
        private final CharSequence text;
        private final String link;

        TextLink(CharSequence text, String link) {
            this.text = text;
            this.link = link;
        }

        @Override
        @NonNull
        public String toString() {
            return text.toString();
        }

        @Override
        public int length() {
            return text.length();
        }

        @Override
        public char charAt(int index) {
            return text.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return text.subSequence(start, end);
        }
    }
}
