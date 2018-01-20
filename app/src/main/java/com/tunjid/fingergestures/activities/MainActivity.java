package com.tunjid.fingergestures.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.tunjid.androidbootstrap.core.view.ViewHider;
import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.BuildConfig;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.baseclasses.FingerGestureActivity;
import com.tunjid.fingergestures.billing.PurchasesManager;
import com.tunjid.fingergestures.fragments.AppFragment;

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

public class MainActivity extends FingerGestureActivity {

    private static final int SETTINGS_CODE = 200;
    private static final int ACCESSIBILITY_CODE = 300;

    private static final String RX_JAVA_LINK = "https://github.com/ReactiveX/RxJava";
    private static final String COLOR_PICKER_LINK = "https://github.com/QuadFlask/colorpicker";
    private static final String ANDROID_BOOTSTRAP_LINK = "https://github.com/tunjid/android-bootstrap";
    private static final String GET_SET_ICON_LINK = "http://www.myiconfinder.com/getseticons";

    private boolean fromSettings;
    private boolean fromAccessibility;

    private final TextLink[] links;
    private View accessibility;
    private View settings;
    private AdView adView;
    private ConstraintLayout constraintLayout;

    private final int[] GESTURE_ITEMS = {PADDING, MAP_UP_ICON, MAP_DOWN_ICON, MAP_LEFT_ICON, MAP_RIGHT_ICON, AD_FREE, REVIEW};
    private final int[] SLIDER_ITEMS = {PADDING, SLIDER_DELTA, SLIDER_POSITION, SLIDER_DURATION, SCREEN_DIMMER,
            SHOW_SLIDER, ADAPTIVE_BRIGHTNESS, ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS, DOUBLE_SWIPE_SETTINGS};
    private final int[] WALLPAPER_ITEMS = {PADDING, SLIDER_COLOR, WALLPAPER_PICKER};

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
        window.setStatusBarColor(ContextCompat.getColor(this,R.color.colorBackground));

        Toolbar toolbar = findViewById(R.id.toolbar);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        fab = findViewById(R.id.fab);
        adView = findViewById(R.id.adView);
        constraintLayout = findViewById(R.id.constraint_layout);

        fabHider = ViewHider.of(fab).setDirection(ViewHider.BOTTOM).build();
        barHider = ViewHider.of(toolbar).setDirection(ViewHider.TOP).build();

        accessibility = findViewById(R.id.accessibility_permissions);
        settings = findViewById(R.id.settings_permissions);

        accessibility.setOnClickListener(v -> startActivity(App.accessibilityIntent()));
        settings.setOnClickListener(v -> startActivity(App.settingsIntent()));

        bottomNavigationView.setOnNavigationItemSelectedListener(this::onOptionsItemSelected);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) showFragment(AppFragment.newInstance(GESTURE_ITEMS));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!fromSettings && !App.canWriteToSettings()) askForSettings();
        else if (!fromAccessibility && !App.isAccessibilityServiceEnabled()) askForAccessibility();

        dismissPermissionsBar();

        fromSettings = false;
        fromAccessibility = false;

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
        getMenuInflater().inflate(R.menu.fragment_home, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case SETTINGS_CODE:
                fromSettings = true;
                showSnackbar(App.canWriteToSettings()
                        ? R.string.settings_permission_granted
                        : R.string.settings_permission_denied);
                break;
            case ACCESSIBILITY_CODE:
                fromAccessibility = true;
                showSnackbar(App.isAccessibilityServiceEnabled()
                        ? R.string.accessibility_permission_granted
                        : R.string.accessibility_permission_denied);
                break;
        }
    }

    private void askForSettings() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(R.string.settings_permission_request)
                .setPositiveButton(R.string.yes, (dialog, b) -> startActivityForResult(App.settingsIntent(), SETTINGS_CODE))
                .setNegativeButton(R.string.no, (dialog, b) -> onPermissionDialogDismissed(settings))
                .setOnCancelListener(dialog -> onPermissionDialogDismissed(settings))
                .show();
    }

    private void askForAccessibility() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(R.string.accessibility_permissions_request)
                .setPositiveButton(R.string.yes, (dialog, b) -> startActivityForResult(App.accessibilityIntent(), ACCESSIBILITY_CODE))
                .setNegativeButton(R.string.no, (dialog, b) -> onPermissionDialogDismissed(accessibility))
                .setOnCancelListener(dialog -> onPermissionDialogDismissed(accessibility))
                .show();
    }

    private void onPermissionDialogDismissed(View prompt) {
        TransitionManager.beginDelayedTransition(constraintLayout, new AutoTransition());
        prompt.setVisibility(View.VISIBLE);
    }

    private void dismissPermissionsBar() {
        TransitionManager.beginDelayedTransition(constraintLayout, new AutoTransition());
        accessibility.setVisibility(View.GONE);
        settings.setVisibility(View.GONE);
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
