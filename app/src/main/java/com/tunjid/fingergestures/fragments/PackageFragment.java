package com.tunjid.fingergestures.fragments;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.functions.collections.Lists;
import com.tunjid.androidbootstrap.recyclerview.ListManager;
import com.tunjid.androidbootstrap.recyclerview.ListManagerBuilder;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.PackageAdapter;
import com.tunjid.fingergestures.baseclasses.MainActivityFragment;
import com.tunjid.fingergestures.billing.PurchasesManager;
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer;
import com.tunjid.fingergestures.viewholders.PackageViewHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

import static androidx.recyclerview.widget.DividerItemDecoration.VERTICAL;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.EXCLUDED_ROTATION_LOCK;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.ROTATION_LOCK;
import static com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer.ROTATION_APPS;
import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static java.util.Objects.requireNonNull;

public class PackageFragment extends MainActivityFragment implements PackageAdapter.PackageClickListener {

    private static final String ARG_PERSISTED_SET = "PERSISTED_SET";

    private View progressBar;
    private ListManager<PackageViewHolder, Void> listManager;
    private final List<ApplicationInfo> packageNames = new ArrayList<>();

    public static PackageFragment newInstance(@RotationGestureConsumer.PersistedSet String preferenceName) {
        PackageFragment fragment = new PackageFragment();
        Bundle args = new Bundle();

        args.putString(ARG_PERSISTED_SET, preferenceName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public String getStableTag() {
        return getClass().getSimpleName();
    }

    @Nullable
    @Override
    @SuppressWarnings("ConstantConditions")
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_packages, container, false);
        Context context = inflater.getContext();

        DividerItemDecoration itemDecoration = new DividerItemDecoration(context, VERTICAL);
        Drawable decoration = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_dark);

        if (decoration != null) itemDecoration.setDrawable(decoration);

        progressBar = root.findViewById(R.id.progress_bar);
        listManager = new ListManagerBuilder<PackageViewHolder, Void>()
                .withRecyclerView(root.findViewById(R.id.options_list))
                .withAdapter(new PackageAdapter(false, () -> packageNames, this))
                .withLinearLayoutManager()
                .addDecoration(itemDecoration)
                .build();

        String persistedSet = getArguments().getString(ARG_PERSISTED_SET);
        root.<Toolbar>findViewById(R.id.title_bar).setTitle(RotationGestureConsumer.getInstance().getAddText(persistedSet));

        populateList(context);

        return root;
    }


    @Override
    public void onPackageClicked(String packageName) {
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

        AppFragment fragment = getCurrentAppFragment();
        if (fragment == null) return;

        fragment.notifyItemChanged(ROTATION_APPS.equals(persistedSet) ? ROTATION_LOCK : EXCLUDED_ROTATION_LOCK);
    }

    @Override
    public void onDestroyView() {
        listManager.clear();
        listManager = null;
        super.onDestroyView();
    }

    private void populateList(Context context) {
        disposables.add(Single.fromCallable(() -> context.getPackageManager().getInstalledApplications(0).stream()
                .filter(applicationInfo -> (applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 || (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
                .sorted(RotationGestureConsumer.getInstance().getApplicationInfoComparator())
                .collect(Collectors.toList()))
                .subscribeOn(Schedulers.io())
                .observeOn(mainThread())
                .subscribe(list -> {
                    ViewGroup root = (ViewGroup) getView();
                    if (root == null) return;

                    Lists.replace(packageNames, list);

                    progressBar.setVisibility(View.GONE);
                    listManager.withRecyclerView(rv -> ((PackageAdapter) requireNonNull(rv.getAdapter())).calculateDiff());
                    TransitionManager.beginDelayedTransition(root, new AutoTransition());
                }, Throwable::printStackTrace));
    }
}
