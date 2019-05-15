/*
 * Copyright (c) 2017, 2018, 2019 Adetunji Dahunsi.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
