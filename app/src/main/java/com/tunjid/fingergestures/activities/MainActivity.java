package com.tunjid.fingergestures.activities;

import android.Manifest;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
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
import android.view.WindowInsets;
import android.widget.TextSwitcher;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.tunjid.androidbootstrap.core.abstractclasses.BaseFragment;
import com.tunjid.androidbootstrap.material.animator.FabExtensionAnimator;
import com.tunjid.androidbootstrap.view.animator.ViewHider;
import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.TrialView;
import com.tunjid.fingergestures.baseclasses.FingerGestureActivity;
import com.tunjid.fingergestures.billing.PurchasesVerifier;
import com.tunjid.fingergestures.fragments.AppFragment;
import com.tunjid.fingergestures.models.TextLink;
import com.tunjid.fingergestures.models.UiState;
import com.tunjid.fingergestures.viewholders.DiffViewHolder;
import com.tunjid.fingergestures.viewmodels.AppViewModel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.disposables.CompositeDisposable;

import static android.content.Intent.ACTION_SEND;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
import static android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
import static android.view.animation.AnimationUtils.loadAnimation;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN;
import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_INDEFINITE;
import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT;
import static com.tunjid.androidbootstrap.view.util.ViewUtil.getLayoutParams;
import static com.tunjid.fingergestures.App.accessibilityIntent;
import static com.tunjid.fingergestures.App.accessibilityServiceEnabled;
import static com.tunjid.fingergestures.App.doNotDisturbIntent;
import static com.tunjid.fingergestures.App.hasStoragePermission;
import static com.tunjid.fingergestures.App.nullCheck;
import static com.tunjid.fingergestures.App.settingsIntent;
import static com.tunjid.fingergestures.App.withApp;
import static com.tunjid.fingergestures.BackgroundManager.ACTION_EDIT_WALLPAPER;
import static com.tunjid.fingergestures.BackgroundManager.ACTION_NAV_BAR_CHANGED;
import static com.tunjid.fingergestures.services.FingerGestureService.ACTION_SHOW_SNACK_BAR;
import static com.tunjid.fingergestures.services.FingerGestureService.EXTRA_SHOW_SNACK_BAR;

public class MainActivity extends FingerGestureActivity {

    public static final int STORAGE_CODE = 100;
    public static final int SETTINGS_CODE = 200;
    public static final int ACCESSIBILITY_CODE = 300;
    public static final int DO_NOT_DISTURB_CODE = 400;

    private static final int DEFAULT_SYSTEM_UI_FLAGS = SYSTEM_UI_FLAG_LAYOUT_STABLE
            | SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STORAGE_CODE, SETTINGS_CODE, ACCESSIBILITY_CODE, DO_NOT_DISTURB_CODE})
    public @interface PermissionRequest {}

    private static final String[] STORAGE_PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE};

    public int topInset;
    public int bottomInset;

    private boolean insetsApplied;

    private View topInsetView;
    private View bottomInsetView;
    private Toolbar toolbar;
    private ViewGroup constraintLayout;
    private TextSwitcher shillSwitcher;
    private MaterialButton permissionText;
    private BottomSheetBehavior bottomSheetBehavior;

    private FabExtensionAnimator fabExtensionAnimator;
    private CompositeDisposable disposables;

    private AppViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = ViewModelProviders.of(this).get(AppViewModel.class);
        disposables = new CompositeDisposable();

        toolbar = findViewById(R.id.toolbar);
        topInsetView = findViewById(R.id.top_inset);
        bottomInsetView = findViewById(R.id.bottom_inset);
        shillSwitcher = findViewById(R.id.upgrade_prompt);
        permissionText = findViewById(R.id.permission_view);
        constraintLayout = findViewById(R.id.constraint_layout);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        fabHider = ViewHider.of(permissionText).setDirection(ViewHider.BOTTOM).build();
        barHider = ViewHider.of(toolbar).setDirection(ViewHider.TOP).build();
        fabExtensionAnimator = new FabExtensionAnimator(permissionText);

        fabHider.hide();

        permissionText.setBackgroundTintList(getFabTint());
        permissionText.setOnClickListener(view -> viewModel.onPermissionClicked(this::onPermissionClicked));
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));
        bottomNavigationView.setOnNavigationItemSelectedListener(this::onOptionsItemSelected);
        constraintLayout.setOnApplyWindowInsetsListener((view, insets) -> consumeSystemInsets(insets));

        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(DEFAULT_SYSTEM_UI_FLAGS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorBackground));
        window.setNavigationBarColor(getNavBarColor());

        setSupportActionBar(toolbar);
        toggleBottomSheet(false);

        Intent startIntent = getIntent();
        boolean isPickIntent = startIntent != null && ACTION_SEND.equals(startIntent.getAction());

        if (savedInstanceState == null && isPickIntent) handleIntent(startIntent);
        else if (savedInstanceState == null) showAppFragment(viewModel.gestureItems);

        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentViewCreated(@NonNull FragmentManager fm,
                                              @NonNull Fragment f,
                                              @NonNull View v,
                                              @Nullable Bundle savedInstanceState) {
                if (f instanceof AppFragment)
                    updateBottomNav((AppFragment) f, bottomNavigationView);
            }
        }, false);

        setUpSwitcher();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (PurchasesVerifier.getInstance().hasAds()) shill();
        else hideAds();

        if (!accessibilityServiceEnabled()) requestPermission(ACCESSIBILITY_CODE);

        invalidateOptionsMenu();
        subscribeToBroadcasts();
        disposables.add(viewModel.uiState().subscribe(this::onStateChanged, Throwable::printStackTrace));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_start_trial);
        boolean isTrialVisible = !PurchasesVerifier.getInstance().isPremiumNotTrial();

        if (item != null) item.setVisible(isTrialVisible);
        if (isTrialVisible && item != null) item.setActionView(new TrialView(this, item));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_start_trial:
                PurchasesVerifier purchasesManager = PurchasesVerifier.getInstance();
                boolean isTrialRunning = purchasesManager.isTrialRunning();

                withSnackbar(snackbar -> {
                    snackbar.setText(purchasesManager.getTrialPeriodText());
                    snackbar.setDuration(isTrialRunning ? LENGTH_SHORT : LENGTH_INDEFINITE);
                    if (!isTrialRunning) snackbar.setAction(android.R.string.yes, view -> {
                        purchasesManager.startTrial();
                        recreate();
                    });

                    snackbar.show();
                });
                break;
            case R.id.action_directions:
                showAppFragment(viewModel.gestureItems);
                return true;
            case R.id.action_slider:
                showAppFragment(viewModel.brightnessItems);
                return true;
            case R.id.action_audio:
                showAppFragment(viewModel.audioItems);
                return true;
            case R.id.action_accessibility_popup:
                showAppFragment(viewModel.popupItems);
                return true;
            case R.id.action_wallpaper:
                showAppFragment(viewModel.appearanceItems);
                return true;
            case R.id.info:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.open_source_libraries)
                        .setItems(viewModel.state.links, (dialog, index) -> showLink(viewModel.state.links[index]))
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
        if (disposables != null) disposables.clear();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        DiffViewHolder.onActivityDestroyed();

        shillSwitcher = null;
        constraintLayout = null;
        permissionText = null;
        bottomSheetBehavior = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        viewModel.onPermissionChange(requestCode).ifPresent(this::showSnackbar);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != STORAGE_CODE || grantResults.length == 0 || grantResults[0] != PERMISSION_GRANTED)
            return;

        viewModel.onPermissionChange(requestCode).ifPresent(this::showSnackbar);
        nullCheck(getCurrentFragment(), AppFragment::notifyDataSetChanged);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    public AppFragment getCurrentFragment() {
        return (AppFragment) super.getCurrentFragment();
    }

    @Override
    public boolean showFragment(BaseFragment fragment) {
        viewModel.checkPermissions();
        return super.showFragment(fragment);
    }

    public void requestPermission(@PermissionRequest int permission) {
        viewModel.requestPermission(permission);
    }

    public void toggleBottomSheet(boolean show) {
        bottomSheetBehavior.setState(show ? STATE_COLLAPSED : STATE_HIDDEN);
    }

    private void showAppFragment(int[] items) {
        showFragment(AppFragment.newInstance(items));
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();

        if (!ACTION_SEND.equals(action) || type == null || !type.startsWith("image/")) return;

        if (!hasStoragePermission()) {
            showSnackbar(R.string.enable_storage_settings);
            showAppFragment(viewModel.gestureItems);
            return;
        }

        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri == null) return;

        AppFragment toShow = AppFragment.newInstance(viewModel.appearanceItems);
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
        showPermissionDialog(R.string.settings_permission_request, () -> startActivityForResult(settingsIntent(), SETTINGS_CODE));
    }

    private void askForAccessibility() {
        showPermissionDialog(R.string.accessibility_permissions_request, () -> startActivityForResult(accessibilityIntent(), ACCESSIBILITY_CODE));
    }

    private void askForDoNotDisturb() {
        showPermissionDialog(R.string.do_not_disturb_permissions_request, () -> startActivityForResult(doNotDisturbIntent(), DO_NOT_DISTURB_CODE));
    }

    private void showPermissionDialog(@StringRes int stringRes, Runnable yesAction) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(stringRes)
                .setPositiveButton(R.string.yes, (dialog, b) -> yesAction.run())
                .setNegativeButton(R.string.no, (dialog, b) -> dialog.dismiss())
                .show();
    }

    private void onPermissionClicked(int permissionRequest) {
        if (permissionRequest == DO_NOT_DISTURB_CODE) askForDoNotDisturb();
        else if (permissionRequest == ACCESSIBILITY_CODE) askForAccessibility();
        else if (permissionRequest == SETTINGS_CODE) askForSettings();
        else if (permissionRequest == STORAGE_CODE) askForStorage();
    }

    private void updateBottomNav(@NonNull AppFragment fragment, BottomNavigationView bottomNavigationView) {
        viewModel.updateBottomNav(Arrays.hashCode(fragment.getItems())).ifPresent(bottomNavigationView::setSelectedItemId);
    }

    private void showLink(TextLink textLink) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(textLink.link));
        startActivity(browserIntent);
    }

    private void shill() {
        disposables.add(viewModel.shill().subscribe(shillSwitcher::setText, Throwable::printStackTrace));
    }

    private void hideAds() {
        viewModel.calmIt();
        if (shillSwitcher.getVisibility() == View.GONE) return;

        Transition hideTransition = getTransition();
        hideTransition.addListener(new TransitionListenerAdapter() {
            public void onTransitionEnd(Transition transition) { showSnackbar(R.string.billing_thanks); }
        });
        TransitionManager.beginDelayedTransition(constraintLayout, hideTransition);
        shillSwitcher.setVisibility(View.GONE);
    }

    private void setUpSwitcher() {
        shillSwitcher.setFactory(() -> {
            View view = LayoutInflater.from(this).inflate(R.layout.text_switch, shillSwitcher, false);
            view.setOnClickListener(clicked -> viewModel.shillMoar());
            return view;
        });

        shillSwitcher.setInAnimation(loadAnimation(this, android.R.anim.slide_in_left));
        shillSwitcher.setOutAnimation(loadAnimation(this, android.R.anim.slide_out_right));
    }

    private void onStateChanged(UiState uiState) {
        permissionText.post(() -> fabExtensionAnimator.updateGlyphs(uiState.glyphState));
        permissionText.post(uiState.fabVisible ? fabHider::show : fabHider::hide);
    }

    private void subscribeToBroadcasts() {
        withApp(app -> disposables.add(app.broadcasts()
                .filter(this::intentMatches)
                .subscribe(this::onBroadcastReceived, error -> {
                    error.printStackTrace();
                    subscribeToBroadcasts(); // Resubscribe on error
                })));
    }

    private void onBroadcastReceived(Intent intent) {
        String action = intent.getAction();
        if (ACTION_EDIT_WALLPAPER.equals(action))
            showSnackbar(R.string.error_wallpaper_google_photos);
        else if (ACTION_SHOW_SNACK_BAR.equals(action))
            showSnackbar(intent.getIntExtra(EXTRA_SHOW_SNACK_BAR, R.string.generic_error));
        else if (ACTION_NAV_BAR_CHANGED.equals(action))
            getWindow().setNavigationBarColor(getNavBarColor());
    }

    private boolean intentMatches(Intent intent) {
        String action = intent.getAction();
        return ACTION_EDIT_WALLPAPER.equals(action)
                || ACTION_SHOW_SNACK_BAR.equals(action)
                || ACTION_NAV_BAR_CHANGED.equals(action);
    }

    private int getNavBarColor() {
        return ContextCompat.getColor(this, BackgroundManager.getInstance().usesColoredNav()
                ? R.color.colorPrimary
                : R.color.black);
    }

    private ColorStateList getFabTint() {
        return ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimary));
    }

    private Transition getTransition() {
        return new AutoTransition().excludeTarget(RecyclerView.class, true);
    }

    private WindowInsets consumeSystemInsets(WindowInsets insets) {
        if (insetsApplied) return insets;

        topInset = insets.getSystemWindowInsetTop();
        int leftInset = insets.getSystemWindowInsetLeft();
        int rightInset = insets.getSystemWindowInsetRight();
        bottomInset = insets.getSystemWindowInsetBottom();

        getLayoutParams(toolbar).topMargin = topInset;
        getLayoutParams(topInsetView).height = topInset;
        getLayoutParams(bottomInsetView).height = bottomInset;
        constraintLayout.setPadding(leftInset, 0, rightInset, 0);

        insetsApplied = true;
        return insets;
    }
}
