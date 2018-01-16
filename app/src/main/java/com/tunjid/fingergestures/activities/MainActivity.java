package com.tunjid.fingergestures.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.View;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.tunjid.androidbootstrap.core.view.ViewHider;
import com.tunjid.fingergestures.BuildConfig;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.baseclasses.FingerGestureActivity;
import com.tunjid.fingergestures.billing.PurchasesManager;
import com.tunjid.fingergestures.fragments.HomeFragment;

public class MainActivity extends FingerGestureActivity {

    private AdView adView;
    private ConstraintLayout constraintLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        fab = findViewById(R.id.fab);
        adView = findViewById(R.id.adView);
        constraintLayout = findViewById(R.id.constraint_layout);

        fabHider = ViewHider.of(fab).setDirection(ViewHider.BOTTOM).build();

        if (savedInstanceState == null) showFragment(HomeFragment.newInstance());
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
}
