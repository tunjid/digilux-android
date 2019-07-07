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

package com.tunjid.fingergestures.fragments;


import android.content.Context;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.tunjid.androidbootstrap.recyclerview.ListManager;
import com.tunjid.androidbootstrap.recyclerview.ListManagerBuilder;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.PackageAdapter;
import com.tunjid.fingergestures.baseclasses.MainActivityFragment;
import com.tunjid.fingergestures.billing.PurchasesManager;
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer;
import com.tunjid.fingergestures.viewholders.PackageViewHolder;
import com.tunjid.fingergestures.viewmodels.AppViewModel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;

import static com.tunjid.fingergestures.App.nullCheck;
import static com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer.ROTATION_APPS;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.EXCLUDED_ROTATION_LOCK;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.ROTATION_LOCK;

public class PackageFragment extends MainActivityFragment {

    private static final String ARG_PERSISTED_SET = "PERSISTED_SET";

    private AppViewModel viewModel;

    public static PackageFragment newInstance(@RotationGestureConsumer.PersistedSet String preferenceName) {
        PackageFragment fragment = new PackageFragment();
        Bundle args = new Bundle();

        args.putString(ARG_PERSISTED_SET, preferenceName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        viewModel = ViewModelProviders.of(requireActivity()).get(AppViewModel.class);
    }

    @Nullable
    @Override
    @SuppressWarnings("ConstantConditions")
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_packages, container, false);

        Toolbar toolbar = root.findViewById(R.id.title_bar);
        ProgressBar progressBar = root.findViewById(R.id.progress_bar);
        ListManager<PackageViewHolder, Void> listManager = new ListManagerBuilder<PackageViewHolder, Void>()
                .withAdapter(new PackageAdapter(false, viewModel.state.installedApps, this::onPackageClicked))
                .withRecyclerView(root.findViewById(R.id.options_list))
                .withLinearLayoutManager()
                .addDecoration(divider())
                .build();

        String persistedSet = getArguments().getString(ARG_PERSISTED_SET);
        toolbar.setTitle(RotationGestureConsumer.getInstance().getAddText(persistedSet));

        disposables.add(viewModel.updatedApps().subscribe(result -> {
            TransitionManager.beginDelayedTransition(root, new AutoTransition());
            progressBar.setVisibility(View.GONE);
            listManager.onDiff(result);
        }, Throwable::printStackTrace));

        return root;
    }

    private void onPackageClicked(String packageName) {
        Bundle args = getArguments();
        if (args == null) {
            showSnackbar(R.string.generic_error);
            return;
        }

        @RotationGestureConsumer.PersistedSet
        String persistedSet = args.getString(ARG_PERSISTED_SET);

        if (persistedSet == null) {
            showSnackbar(R.string.generic_error);
            return;
        }

        boolean added = RotationGestureConsumer.getInstance().addToSet(packageName, persistedSet);

        if (!added) {
            Context context = requireContext();
            new AlertDialog.Builder(context)
                    .setTitle(R.string.go_premium_title)
                    .setMessage(context.getString(R.string.go_premium_body, context.getString(R.string.auto_rotate_description)))
                    .setPositiveButton(R.string.continue_text, (dialog, which) -> purchase(PurchasesManager.PREMIUM_SKU))
                    .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }

        toggleBottomSheet(false);
        Companion.nullCheck(getCurrentAppFragment(), fragment -> fragment.notifyItemChanged(ROTATION_APPS.equals(persistedSet) ? ROTATION_LOCK : EXCLUDED_ROTATION_LOCK));
    }
}
