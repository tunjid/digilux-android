package com.tunjid.fingergestures.fragments;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.BuildConfig;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.HomeAdapter;
import com.tunjid.fingergestures.baseclasses.FingerGestureFragment;
import com.tunjid.fingergestures.billing.BillingManager;
import com.tunjid.fingergestures.billing.PurchasesManager;

import static android.support.v7.widget.DividerItemDecoration.VERTICAL;
import static com.android.billingclient.api.BillingClient.BillingResponse.ITEM_ALREADY_OWNED;
import static com.android.billingclient.api.BillingClient.BillingResponse.OK;
import static com.android.billingclient.api.BillingClient.BillingResponse.SERVICE_DISCONNECTED;
import static com.android.billingclient.api.BillingClient.BillingResponse.SERVICE_UNAVAILABLE;

public class HomeFragment extends FingerGestureFragment
        implements
        HomeAdapter.HomeAdapterListener {

    private static final int SETTINGS_CODE = 200;
    private static final int ACCESSIBILITY_CODE = 300;
    private static final String RX_JAVA_LINK = "https://github.com/ReactiveX/RxJava";
    private static final String CLOLOR_PICKER_LINK = "https://github.com/QuadFlask/colorpicker";
    private static final String ANDROID_BOOTSTRAP_LINK = "https://github.com/tunjid/android-bootstrap";
    private static final String GET_SET_ICON_LINK = "http://www.myiconfinder.com/getseticons";

    private boolean fromSettings;
    private boolean fromAccessibility;

    private final TextLink[] infolist;
    private AdView adView;
    private View accessibility;
    private View settings;
    private RecyclerView recyclerView;
    private BillingManager billingManager;

    {
        Context context = App.getInstance();
        infolist = new TextLink[]{new TextLink(context.getString(R.string.get_set_icon), GET_SET_ICON_LINK),
                new TextLink(context.getString(R.string.rxjava), RX_JAVA_LINK),
                new TextLink(context.getString(R.string.color_picker), CLOLOR_PICKER_LINK),
                new TextLink(context.getString(R.string.android_bootstrap), ANDROID_BOOTSTRAP_LINK)};
    }

    public static HomeFragment newInstance() {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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
        recyclerView.setAdapter(new HomeAdapter(this));
        recyclerView.addItemDecoration(itemDecoration);

        accessibility = root.findViewById(R.id.accessibility_permissions);
        settings = root.findViewById(R.id.settings_permissions);
        adView = root.findViewById(R.id.adView);

        accessibility.setOnClickListener(v -> startActivity(App.accessibilityIntent()));
        settings.setOnClickListener(v -> startActivity(App.settingsIntent()));

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
                    TransitionManager.beginDelayedTransition(root, new AutoTransition());
                    adView.setVisibility(View.VISIBLE);
                }
            });
        }

        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FloatingActionButton fab = getFab();
        fab.setImageResource(R.drawable.ic_settings_white_24dp);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        billingManager = new BillingManager(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!fromSettings && !App.canWriteToSettings()) askForSettings();
        else if (!fromAccessibility && !App.isAccessibilityServiceEnabled()) askForAccessibility();

        dismissPermissionsBar();

        fromSettings = false;
        fromAccessibility = false;

        recyclerView.getAdapter().notifyDataSetChanged();
        if (!PurchasesManager.getInstance().hasAds()) hideAds();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView = null;
        accessibility = null;
        settings = null;
        adView = null;
    }

    @Override
    public void onDestroy() {
        billingManager.destroy();
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_home, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.info:
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.open_source_libraries)
                        .setItems(infolist, (a, b) -> showLink(infolist[b]))
                        .show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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

    @Override
    public void purchase(String sku) {
        billingManager.initiatePurchaseFlow(sku)
                .subscribe(launchStatus -> {
                    switch (launchStatus) {
                        case OK:
                            break;
                        case SERVICE_UNAVAILABLE:
                        case SERVICE_DISCONNECTED:
                            showSnackbar(R.string.billing_not_connected);
                            break;
                        case ITEM_ALREADY_OWNED:
                            showSnackbar(R.string.billing_you_own_this);
                            break;
                        default:
                            showSnackbar(R.string.billing_generic_error);
                            break;
                    }
                }, throwable -> showSnackbar(R.string.billing_generic_error));
    }

    @Override
    protected boolean showsFab() {
        return false;
    }

    private void askForSettings() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.permission_required)
                .setMessage(R.string.settings_permission_request)
                .setPositiveButton(R.string.yes, (dialog, b) -> startActivityForResult(App.settingsIntent(), SETTINGS_CODE))
                .setNegativeButton(R.string.no, (dialog, b) -> onPermissionDialogDismissed(settings))
                .setOnCancelListener(dialog -> onPermissionDialogDismissed(settings))
                .show();
    }

    private void askForAccessibility() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.permission_required)
                .setMessage(R.string.accessibility_permissions_request)
                .setPositiveButton(R.string.yes, (dialog, b) -> startActivityForResult(App.accessibilityIntent(), ACCESSIBILITY_CODE))
                .setNegativeButton(R.string.no, (dialog, b) -> onPermissionDialogDismissed(accessibility))
                .setOnCancelListener(dialog -> onPermissionDialogDismissed(accessibility))
                .show();
    }

    private void onPermissionDialogDismissed(View prompt) {
        ViewGroup root = getRoot();
        if (root == null) return;

        TransitionManager.beginDelayedTransition(root, new AutoTransition());
        prompt.setVisibility(View.VISIBLE);
    }

    private void dismissPermissionsBar() {
        ViewGroup root = getRoot();
        if (root == null) return;

        TransitionManager.beginDelayedTransition(root, new AutoTransition());
        accessibility.setVisibility(View.GONE);
        settings.setVisibility(View.GONE);
    }

    private void showLink(TextLink textLink) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(textLink.link));
        startActivity(browserIntent);
    }

    private void hideAds() {
        if (adView.getVisibility() == View.GONE) return;

        ViewGroup root = getRoot();
        if (root == null) return;

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
        android.transition.TransitionManager.beginDelayedTransition(root, hideTransition);
        adView.setVisibility(View.GONE);
    }

    @Nullable
    private ViewGroup getRoot() {
        return (ViewGroup) getView();
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
