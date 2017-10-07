package com.tunjid.fingergestures.activities;

import android.os.Bundle;

import com.tunjid.androidbootstrap.core.view.ViewHider;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.baseclasses.FingerGestureActivity;
import com.tunjid.fingergestures.fragments.HomeFragment;

public class MainActivity extends FingerGestureActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        fab = findViewById(R.id.fab);
        fabHider = ViewHider.of(fab).setDirection(ViewHider.BOTTOM).build();

        if (savedInstanceState == null) showFragment(HomeFragment.newInstance());
    }
}
