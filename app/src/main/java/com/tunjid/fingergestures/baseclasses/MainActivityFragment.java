package com.tunjid.fingergestures.baseclasses;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseFragment;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.activities.MainActivity;
import com.tunjid.fingergestures.billing.PurchasesManager;
import com.tunjid.fingergestures.fragments.AppFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.disposables.CompositeDisposable;

import static androidx.recyclerview.widget.DividerItemDecoration.VERTICAL;

public abstract class MainActivityFragment extends BaseFragment {

    protected CompositeDisposable disposables = new CompositeDisposable();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        disposables.clear();
        super.onDestroyView();
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

    public void showBottomSheetFragment(MainActivityFragment fragment) {
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager == null) return;

        fragmentManager.beginTransaction().replace(R.id.bottom_sheet, fragment).commit();
        toggleBottomSheet(true);
    }

    protected void toggleBottomSheet(boolean show) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) activity.toggleBottomSheet(show);
    }

    @Nullable
    protected AppFragment getCurrentAppFragment() {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity == null) return null;

        return (AppFragment) activity.getCurrentFragment();
    }

    protected RecyclerView.ItemDecoration divider() {
        Context context = requireContext();

        DividerItemDecoration itemDecoration = new DividerItemDecoration(context, VERTICAL);
        Drawable decoration = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_dark);

        if (decoration != null) itemDecoration.setDrawable(decoration);

        return itemDecoration;
    }
}
