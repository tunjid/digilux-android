package com.tunjid.fingergestures.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.tunjid.androidbootstrap.core.abstractclasses.BaseFragment;
import com.tunjid.androidbootstrap.core.view.ViewHider;
import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.BuildConfig;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.TrialView;
import com.tunjid.fingergestures.baseclasses.FingerGestureActivity;
import com.tunjid.fingergestures.billing.PurchasesManager;
import com.tunjid.fingergestures.fragments.AppFragment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayDeque;
import java.util.Deque;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.support.design.widget.BottomSheetBehavior.STATE_COLLAPSED;
import static android.support.design.widget.BottomSheetBehavior.STATE_HIDDEN;
import static android.support.design.widget.Snackbar.LENGTH_INDEFINITE;
import static android.support.design.widget.Snackbar.LENGTH_SHORT;
import static com.tunjid.fingergestures.BackgroundManager.ACTION_EDIT_WALLPAPER;
import static com.tunjid.fingergestures.adapters.AppAdapter.ADAPTIVE_BRIGHTNESS;
import static com.tunjid.fingergestures.adapters.AppAdapter.ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS;
import static com.tunjid.fingergestures.adapters.AppAdapter.AD_FREE;
import static com.tunjid.fingergestures.adapters.AppAdapter.DOUBLE_SWIPE_SETTINGS;
import static com.tunjid.fingergestures.adapters.AppAdapter.ENABLE_ACCESSIBILITY_BUTTON;
import static com.tunjid.fingergestures.adapters.AppAdapter.ENABLE_WATCH_WINDOWS;
import static com.tunjid.fingergestures.adapters.AppAdapter.EXCLUDED_ROTATION_LOCK;
import static com.tunjid.fingergestures.adapters.AppAdapter.MAP_DOWN_ICON;
import static com.tunjid.fingergestures.adapters.AppAdapter.MAP_LEFT_ICON;
import static com.tunjid.fingergestures.adapters.AppAdapter.MAP_RIGHT_ICON;
import static com.tunjid.fingergestures.adapters.AppAdapter.MAP_UP_ICON;
import static com.tunjid.fingergestures.adapters.AppAdapter.PADDING;
import static com.tunjid.fingergestures.adapters.AppAdapter.POPUP_ACTION;
import static com.tunjid.fingergestures.adapters.AppAdapter.REVIEW;
import static com.tunjid.fingergestures.adapters.AppAdapter.ROTATION_LOCK;
import static com.tunjid.fingergestures.adapters.AppAdapter.SCREEN_DIMMER;
import static com.tunjid.fingergestures.adapters.AppAdapter.SHOW_SLIDER;
import static com.tunjid.fingergestures.adapters.AppAdapter.SLIDER_COLOR;
import static com.tunjid.fingergestures.adapters.AppAdapter.SLIDER_DELTA;
import static com.tunjid.fingergestures.adapters.AppAdapter.SLIDER_DURATION;
import static com.tunjid.fingergestures.adapters.AppAdapter.SLIDER_POSITION;
import static com.tunjid.fingergestures.adapters.AppAdapter.WALLPAPER_PICKER;
import static com.tunjid.fingergestures.adapters.AppAdapter.WALLPAPER_TRIGGER;
import static com.tunjid.fingergestures.services.FingerGestureService.ACTION_SHOW_SNACK_BAR;
import static com.tunjid.fingergestures.services.FingerGestureService.EXTRA_SHOW_SNACK_BAR;

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
    private static final String IMAGE_CROPPER_LINK = "https://github.com/ArthurHub/Android-Image-Cropper";
    private static final String MATERIAL_DESIGN_ICONS_LINK = "https://materialdesignicons.com/";

    private static final String[] STORAGE_PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE};

    private AdView adView;
    private TextView permissionText;
    private ViewGroup container;
    private BottomSheetBehavior bottomSheetBehavior;

    private final TextLink[] links;
    private final Deque<Integer> permissionsStack = new ArrayDeque<>();

    private final int[] GESTURE_ITEMS = {PADDING, MAP_UP_ICON, MAP_DOWN_ICON, MAP_LEFT_ICON, MAP_RIGHT_ICON, AD_FREE, REVIEW};
    private final int[] APPEARANCE_ITEMS = {PADDING, SLIDER_COLOR, WALLPAPER_PICKER, WALLPAPER_TRIGGER, ROTATION_LOCK, EXCLUDED_ROTATION_LOCK, POPUP_ACTION};
    private final int[] SLIDER_ITEMS = {PADDING, SLIDER_DELTA, SLIDER_POSITION, SLIDER_DURATION, SCREEN_DIMMER,
            SHOW_SLIDER, ADAPTIVE_BRIGHTNESS, ENABLE_WATCH_WINDOWS, ENABLE_ACCESSIBILITY_BUTTON,
            ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS, DOUBLE_SWIPE_SETTINGS};

    {
        Context context = App.getInstance();
        links = new TextLink[]{new TextLink(context.getString(R.string.get_set_icon), GET_SET_ICON_LINK),
                new TextLink(context.getString(R.string.rxjava), RX_JAVA_LINK),
                new TextLink(context.getString(R.string.color_picker), COLOR_PICKER_LINK),
                new TextLink(context.getString(R.string.image_cropper), IMAGE_CROPPER_LINK),
                new TextLink(context.getString(R.string.material_design_icons), MATERIAL_DESIGN_ICONS_LINK),
                new TextLink(context.getString(R.string.android_bootstrap), ANDROID_BOOTSTRAP_LINK)};
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_EDIT_WALLPAPER.equals(action))
                showSnackbar(R.string.error_wallpaper_google_photos);
            else if (ACTION_SHOW_SNACK_BAR.equals(action))
                showSnackbar(intent.getIntExtra(EXTRA_SHOW_SNACK_BAR, R.string.generic_error));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!PurchasesManager.getInstance().isPremiumNotTrial())
            MobileAds.initialize(this, getString(R.string.ad_id));

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorBackground));

        Toolbar toolbar = findViewById(R.id.toolbar);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        adView = findViewById(R.id.adView);
        container = findViewById(R.id.container);

        barHider = ViewHider.of(toolbar).setDirection(ViewHider.TOP).build();

        permissionText = findViewById(R.id.permission_view);
        permissionText.setOnClickListener(view -> onPermissionClicked());
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));

        bottomNavigationView.setOnNavigationItemSelectedListener(this::onOptionsItemSelected);
        setSupportActionBar(toolbar);
        toggleBottomSheet(false);

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
                    TransitionManager.beginDelayedTransition(container, getTransition());
                    adView.setVisibility(View.VISIBLE);
                }
            });
        }
        else hideAds();

        if (!App.accessibilityServiceEnabled()) requestPermission(ACCESSIBILITY_CODE);
        invalidateOptionsMenu();

        IntentFilter filter = new IntentFilter(ACTION_EDIT_WALLPAPER);
        filter.addAction(ACTION_SHOW_SNACK_BAR);

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
        registerReceiver(receiver, filter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_start_trial);
        boolean isTrialVisible = !PurchasesManager.getInstance().isPremiumNotTrial();

        if (item != null) item.setVisible(isTrialVisible);
        if (isTrialVisible && item != null) item.setActionView(new TrialView(this, item));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_start_trial:
                PurchasesManager purchasesManager = PurchasesManager.getInstance();
                boolean isTrialRunning = purchasesManager.isTrialRunning();

                Snackbar snackbar = Snackbar.make(container, purchasesManager.getTrialPeriodText(), isTrialRunning ? LENGTH_SHORT : LENGTH_INDEFINITE);
                if (!isTrialRunning) snackbar.setAction(android.R.string.yes, view -> {
                    purchasesManager.startTrial();
                    recreate();
                });

                snackbar.show();
                break;
            case R.id.action_directions:
                showFragment(AppFragment.newInstance(GESTURE_ITEMS));
                return true;
            case R.id.action_slider:
                showFragment(AppFragment.newInstance(SLIDER_ITEMS));
                return true;
            case R.id.action_wallpaper:
                showFragment(AppFragment.newInstance(APPEARANCE_ITEMS));
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
    public void onBackPressed() {
        if (bottomSheetBehavior.getState() != STATE_HIDDEN) toggleBottomSheet(false);
        else super.onBackPressed();
    }

    @Override
    protected void onPause() {
        adView.pause();
        unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.gc();
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

    @Override
    public boolean showFragment(BaseFragment fragment) {
        if (permissionsStack.isEmpty()) dismissPermissionsBar();
        return super.showFragment(fragment);
    }

    public void requestPermission(@PermissionRequest int permission) {
        if (permissionsStack.contains(permission)) permissionsStack.remove(permission);
        permissionsStack.push(permission);
        onPermissionAdded();
    }

    public void toggleBottomSheet(boolean show) {
        bottomSheetBehavior.setState(show ? STATE_COLLAPSED : STATE_HIDDEN);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();

        if (!Intent.ACTION_SEND.equals(action) || TextUtils.isEmpty(type) || !type.startsWith("image/"))
            return;

        if (!App.hasStoragePermission()) {
            showSnackbar(R.string.enable_storage_settings);
            showFragment(AppFragment.newInstance(GESTURE_ITEMS));
            return;
        }

        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri == null) return;

        AppFragment toShow = AppFragment.newInstance(APPEARANCE_ITEMS);
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
        togglePermissionText(true);
    }

    private void dismissPermissionsBar() {
        if (permissionsStack.isEmpty()) togglePermissionText(false);
        else onPermissionAdded();
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

        Transition hideTransition = getTransition();
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
        android.transition.TransitionManager.beginDelayedTransition(container, hideTransition);
        adView.setVisibility(View.GONE);
    }

    private void togglePermissionText(boolean show) {
        TransitionManager.beginDelayedTransition(container, getTransition());
        ViewGroup.LayoutParams params = permissionText.getLayoutParams();
        params.height = show ? getResources().getDimensionPixelSize(R.dimen.triple_margin) : 0;
        ((ViewGroup) permissionText.getParent()).invalidate();
    }

    private Transition getTransition() {
        return new AutoTransition().excludeTarget(RecyclerView.class, true);
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
