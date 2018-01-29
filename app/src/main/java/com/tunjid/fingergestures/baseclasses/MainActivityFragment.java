package com.tunjid.fingergestures.baseclasses;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.View;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseFragment;
import com.tunjid.fingergestures.activities.MainActivity;
import com.tunjid.fingergestures.billing.PurchasesManager;

public abstract class MainActivityFragment extends BaseFragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    protected void toggleToolbar(boolean visible) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) activity.toggleToolbar(visible);
    }

    public void showSnackbar(@StringRes int resource) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) activity.showSnackbar(resource);
    }

    public void purchase(@PurchasesManager.SKU String sku) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) activity.purchase(sku);
    }

    public void requestPermission(@MainActivity.PermissionRequest int permission) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) activity.requestPermission(permission);
    }
}
