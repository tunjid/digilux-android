package com.tunjid.fingergestures.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextSwitcher;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseFragment;
import com.tunjid.androidbootstrap.view.animator.FabExtensionAnimator;
import com.tunjid.androidbootstrap.view.animator.ViewHider;
import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.TrialView;
import com.tunjid.fingergestures.baseclasses.FingerGestureActivity;
import com.tunjid.fingergestures.billing.PurchasesManager;
import com.tunjid.fingergestures.fragments.AppFragment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.processors.PublishProcessor;

import static android.content.Intent.ACTION_SEND;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN;
import static com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE;
import static com.google.android.material.snackbar.Snackbar.LENGTH_SHORT;
import static android.view.animation.AnimationUtils.loadAnimation;
import static com.tunjid.fingergestures.BackgroundManager.ACTION_EDIT_WALLPAPER;
import static com.tunjid.fingergestures.adapters.AppAdapter.ADAPTIVE_BRIGHTNESS;
import static com.tunjid.fingergestures.adapters.AppAdapter.ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS;
import static com.tunjid.fingergestures.adapters.AppAdapter.AD_FREE;
import static com.tunjid.fingergestures.adapters.AppAdapter.ANIMATES_POPUP;
import static com.tunjid.fingergestures.adapters.AppAdapter.ANIMATES_SLIDER;
import static com.tunjid.fingergestures.adapters.AppAdapter.AUDIO_DELTA;
import static com.tunjid.fingergestures.adapters.AppAdapter.AUDIO_SLIDER_SHOW;
import static com.tunjid.fingergestures.adapters.AppAdapter.AUDIO_STREAM_TYPE;
import static com.tunjid.fingergestures.adapters.AppAdapter.DISCRETE_BRIGHTNESS;
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
import static com.tunjid.fingergestures.adapters.AppAdapter.SUPPORT;
import static com.tunjid.fingergestures.adapters.AppAdapter.USE_LOGARITHMIC_SCALE;
import static com.tunjid.fingergestures.adapters.AppAdapter.WALLPAPER_PICKER;
import static com.tunjid.fingergestures.adapters.AppAdapter.WALLPAPER_TRIGGER;
import static com.tunjid.fingergestures.services.FingerGestureService.ACTION_SHOW_SNACK_BAR;
import static com.tunjid.fingergestures.services.FingerGestureService.EXTRA_SHOW_SNACK_BAR;
import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;

public class MainActivity extends FingerGestureActivity {

    public static final int STORAGE_CODE = 100;
    public static final int SETTINGS_CODE = 200;
    public static final int ACCESSIBILITY_CODE = 300;
    public static final int DO_NOT_DISTURB_CODE = 400;


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STORAGE_CODE, SETTINGS_CODE, ACCESSIBILITY_CODE, DO_NOT_DISTURB_CODE})
    public @interface PermissionRequest {}

    private static final String RX_JAVA_LINK = "https://github.com/ReactiveX/RxJava";
    private static final String COLOR_PICKER_LINK = "https://github.com/QuadFlask/colorpicker";
    private static final String ANDROID_BOOTSTRAP_LINK = "https://github.com/tunjid/android-bootstrap";
    private static final String GET_SET_ICON_LINK = "http://www.myiconfinder.com/getseticons";
    private static final String IMAGE_CROPPER_LINK = "https://github.com/ArthurHub/Android-Image-Cropper";
    private static final String MATERIAL_DESIGN_ICONS_LINK = "https://materialdesignicons.com/";

    private static final String[] STORAGE_PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE};

    private ViewGroup constraintLayout;
    private TextSwitcher switcher;
    private MaterialButton permissionText;
    private BottomSheetBehavior bottomSheetBehavior;

    private FabExtensionAnimator fabExtensionAnimator;
    private PublishProcessor<String> publishProcessor;
    private CompositeDisposable disposables;
    private String[] quips;

    private final TextLink[] links;
    private final AtomicInteger quipCounter = new AtomicInteger(-1);
    private final Deque<Integer> permissionsStack = new ArrayDeque<>();

    private final int[] GESTURE_ITEMS = {PADDING, MAP_UP_ICON, MAP_DOWN_ICON, MAP_LEFT_ICON,
            MAP_RIGHT_ICON, AD_FREE, SUPPORT, REVIEW, PADDING};

    private final int[] BRIGHTNESS_ITEMS = {PADDING, SLIDER_DELTA, DISCRETE_BRIGHTNESS,
            SCREEN_DIMMER, USE_LOGARITHMIC_SCALE, SHOW_SLIDER, ADAPTIVE_BRIGHTNESS, ANIMATES_SLIDER,
            ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS, DOUBLE_SWIPE_SETTINGS, PADDING};

    private final int[] AUDIO_ITEMS = {PADDING, AUDIO_DELTA, AUDIO_STREAM_TYPE, AUDIO_SLIDER_SHOW,
            PADDING};

    private final int[] APPEARANCE_ITEMS = {PADDING, SLIDER_POSITION, SLIDER_DURATION,
            SLIDER_COLOR, WALLPAPER_PICKER, WALLPAPER_TRIGGER, ENABLE_WATCH_WINDOWS,
            ENABLE_ACCESSIBILITY_BUTTON, ANIMATES_POPUP, ROTATION_LOCK, EXCLUDED_ROTATION_LOCK,
            POPUP_ACTION, PADDING};

    {
        Context context = App.getInstance();
        links = context == null
                ? new TextLink[0]
                : new TextLink[]{new TextLink(context.getString(R.string.get_set_icon), GET_SET_ICON_LINK),
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

        disposables = new CompositeDisposable();

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorBackground));

        Toolbar toolbar = findViewById(R.id.toolbar);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        switcher = findViewById(R.id.upgrade_prompt);
        permissionText = findViewById(R.id.permission_view);
        constraintLayout = findViewById(R.id.constraint_layout);

        fabHider = ViewHider.of(permissionText).setDirection(ViewHider.BOTTOM).build();
        barHider = ViewHider.of(toolbar).setDirection(ViewHider.TOP).build();
        fabExtensionAnimator = new FabExtensionAnimator(permissionText);

        fabHider.hide();

        permissionText.setBackgroundTintList(getFabTint());
        permissionText.setOnClickListener(view -> onPermissionClicked());
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));
        bottomNavigationView.setOnNavigationItemSelectedListener(this::onOptionsItemSelected);

        setSupportActionBar(toolbar);
        toggleBottomSheet(false);

        Intent startIntent = getIntent();
        boolean isPickIntent = startIntent != null && ACTION_SEND.equals(startIntent.getAction());

        if (savedInstanceState == null && isPickIntent) handleIntent(startIntent);
        else if (savedInstanceState == null) showFragment(AppFragment.newInstance(GESTURE_ITEMS));

        setUpSwitcher();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (PurchasesManager.getInstance().hasAds()) shill();
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

                Snackbar snackbar = Snackbar.make(coordinator, purchasesManager.getTrialPeriodText(), isTrialRunning ? LENGTH_SHORT : LENGTH_INDEFINITE);
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
                showFragment(AppFragment.newInstance(BRIGHTNESS_ITEMS));
                return true;
            case R.id.action_audio:
                showFragment(AppFragment.newInstance(AUDIO_ITEMS));
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
        if (disposables != null) disposables.clear();
        switcher = null;
        constraintLayout = null;
        permissionText = null;
        bottomSheetBehavior = null;
        permissionsStack.clear();
        super.onDestroy();
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
            case DO_NOT_DISTURB_CODE:
                showSnackbar((shouldRemove = App.hasDoNotDisturbAccess())
                        ? R.string.do_not_disturb_permission_granted
                        : R.string.do_not_disturb_permission_denied);
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
        if (fragment != null) fragment.notifyDataSetChanged();
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
        permissionsStack.remove(permission);
        permissionsStack.push(permission);
        onPermissionAdded();
    }

    public void toggleBottomSheet(boolean show) {
        bottomSheetBehavior.setState(show ? STATE_COLLAPSED : STATE_HIDDEN);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();

        if (!ACTION_SEND.equals(action) || type == null || !type.startsWith("image/")) return;

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

    private void askForDoNotDisturb() {
        showPermissionDialog(R.string.do_not_disturb_permissions_request, () -> startActivityForResult(App.doNotDisturbIntent(), DO_NOT_DISTURB_CODE));
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
        FabExtensionAnimator.GlyphState glyphState = null;

        if (permissionRequest == DO_NOT_DISTURB_CODE)
            glyphState = glyphState(R.string.enable_do_not_disturb, R.drawable.ic_volume_loud_24dp);
        else if (permissionRequest == ACCESSIBILITY_CODE)
            glyphState = glyphState(R.string.enable_accessibility, R.drawable.ic_human_24dp);
        else if (permissionRequest == SETTINGS_CODE)
            glyphState = glyphState(R.string.enable_write_settings, R.drawable.ic_settings_white_24dp);
        else if (permissionRequest == STORAGE_CODE)
            glyphState = glyphState(R.string.enable_storage_settings, R.drawable.ic_storage_24dp);

        if (glyphState != null) fabExtensionAnimator.updateGlyphs(glyphState);
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

        if (permissionRequest == DO_NOT_DISTURB_CODE) askForDoNotDisturb();
        else if (permissionRequest == ACCESSIBILITY_CODE) askForAccessibility();
        else if (permissionRequest == SETTINGS_CODE) askForSettings();
        else if (permissionRequest == STORAGE_CODE) askForStorage();
    }

    private void showLink(TextLink textLink) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(textLink.link));
        startActivity(browserIntent);
    }

    private void shill() {
        disposables.clear();

        publishProcessor = PublishProcessor.create();

        disposables.add(Flowable.interval(10, TimeUnit.SECONDS)
                .map(value -> getNextQuip())
                .observeOn(mainThread())
                .subscribe(publishProcessor::onNext, Throwable::printStackTrace));

        disposables.add(publishProcessor.subscribe(switcher::setText, Throwable::printStackTrace));
    }

    private void hideAds() {
        if (switcher.getVisibility() == View.GONE) return;

        Transition hideTransition = getTransition();
        hideTransition.addListener(new TransitionListenerAdapter() {
            public void onTransitionEnd(Transition transition) {
                showSnackbar(R.string.billing_thanks);
            }
        });
        TransitionManager.beginDelayedTransition(constraintLayout, hideTransition);
        switcher.setVisibility(View.GONE);
    }

    private void setUpSwitcher() {
        quips = getResources().getStringArray(R.array.upsell_text);
        switcher.setFactory(() -> {
            View view = LayoutInflater.from(this).inflate(R.layout.text_switch, switcher, false);
            view.setOnClickListener(clicked -> {
                if (publishProcessor != null) publishProcessor.onNext(getNextQuip());
            });
            return view;
        });

        switcher.setInAnimation(loadAnimation(this, android.R.anim.slide_in_left));
        switcher.setOutAnimation(loadAnimation(this, android.R.anim.slide_out_right));
    }

    private void togglePermissionText(boolean show) {
        if (show) fabHider.show();
        else fabHider.hide();
    }

    private ColorStateList getFabTint() {
        return ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimary));
    }

    private FabExtensionAnimator.GlyphState glyphState(@StringRes int text, @DrawableRes int icon) {
        return FabExtensionAnimator.newState(getText(text), ContextCompat.getDrawable(this, icon));
    }

    private Transition getTransition() {
        return new AutoTransition().excludeTarget(RecyclerView.class, true);
    }

    private String getNextQuip() {
        if (quipCounter.incrementAndGet() >= quips.length) quipCounter.set(0);
        return quips[quipCounter.get()];
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
