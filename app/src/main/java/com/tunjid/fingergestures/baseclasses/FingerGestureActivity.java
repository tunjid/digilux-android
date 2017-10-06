package com.tunjid.fingergestures.baseclasses;


import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseActivity;
import com.tunjid.androidbootstrap.core.view.ViewHider;

public abstract class FingerGestureActivity extends BaseActivity {

    protected ViewHider fabHider;
    protected FloatingActionButton fab;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }

    public FloatingActionButton getFab() {
        return fab;
    }

    public void toggleFab(boolean visible) {
        if (visible) fabHider.show();
        else fabHider.hide();
    }
}
