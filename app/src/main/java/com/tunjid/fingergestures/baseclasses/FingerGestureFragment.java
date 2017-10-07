package com.tunjid.fingergestures.baseclasses;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.view.View;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseFragment;

public abstract class FingerGestureFragment extends BaseFragment {

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toggleFab(showsFab());
    }

    protected void toggleFab(boolean visible) {
        ((FingerGestureActivity) getActivity()).toggleFab(visible);
    }

    protected void showSnackbar(@StringRes int resource) {
        ((FingerGestureActivity) getActivity()).showSnackbar(resource);
    }

    protected FloatingActionButton getFab() {
        return ((FingerGestureActivity) getActivity()).getFab();
    }

    protected abstract boolean showsFab();
}
