package com.tunjid.fingergestures.baseclasses;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentManager;
import android.view.View;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseFragment;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.activities.MainActivity;
import com.tunjid.fingergestures.billing.PurchasesManager;
import com.tunjid.fingergestures.fragments.AppFragment;

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

    public void toggleBottomSheet(boolean show) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) activity.toggleBottomSheet(show);
    }

    public void showBottomSheetFragment(MainActivityFragment fragment) {
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager == null) return;

        fragmentManager.beginTransaction().replace(R.id.bottom_sheet, fragment).commit();
        toggleBottomSheet(true);
    }

    @Nullable
    public AppFragment getCurrentAppFragment() {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity == null) return null;

        return (AppFragment) activity.getCurrentFragment();
    }
}
