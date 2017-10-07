package com.tunjid.fingergestures.baseclasses;


import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseActivity;
import com.tunjid.androidbootstrap.core.view.ViewHider;
import com.tunjid.fingergestures.R;

import static android.support.design.widget.Snackbar.LENGTH_SHORT;

public abstract class FingerGestureActivity extends BaseActivity {

    protected ViewHider fabHider;
    protected FloatingActionButton fab;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }

    public void showSnackbar(@StringRes int resource) {
        ViewGroup root = findViewById(R.id.container);
        if (root == null) return;
        Snackbar.make(root, resource, LENGTH_SHORT).show();
    }

    public FloatingActionButton getFab() {
        return fab;
    }

    public void toggleFab(boolean visible) {
        if (visible) fabHider.show();
        else fabHider.hide();
    }
}
