package com.tunjid.fingergestures.viewholders;

import android.content.pm.ApplicationInfo;
import android.view.View;
import android.widget.TextView;

import com.tunjid.androidbootstrap.recyclerview.ListManagerBuilder;
import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.adapters.PackageAdapter;
import com.tunjid.fingergestures.fragments.PackageFragment;
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer;

import java.util.List;
import java.util.function.Supplier;

import androidx.appcompat.app.AlertDialog;

import static com.tunjid.fingergestures.activities.MainActivity.SETTINGS_CODE;
import static com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer.ROTATION_APPS;

public class RotationViewHolder extends DiffViewHolder<ApplicationInfo> {

    private final String persistedSet;

    public RotationViewHolder(View itemView,
                              @RotationGestureConsumer.PersistedSet String persistedSet,
                              List<ApplicationInfo> items,
                              AppAdapter.AppAdapterListener listener) {
        super(itemView, items, listener);
        this.persistedSet = persistedSet;

        listManager = new ListManagerBuilder<PackageViewHolder, Void>()
                .withAdapter(new PackageAdapter(true, items, getPackageClickListener(persistedSet)))
                .withRecyclerView(itemView.findViewById(R.id.item_list))
                .withGridLayoutManager(3)
                .build();

        itemView.findViewById(R.id.add).setOnClickListener(view -> {
            if (!App.canWriteToSettings())
                new AlertDialog.Builder(itemView.getContext()).setMessage(R.string.permission_required).show();

            else if (!RotationGestureConsumer.getInstance().canAutoRotate())
                new AlertDialog.Builder(itemView.getContext()).setMessage(R.string.auto_rotate_prompt).show();

            else
                adapterListener.showBottomSheetFragment(PackageFragment.newInstance(persistedSet));
        });

        TextView title = itemView.findViewById(R.id.title);
        boolean isRotationList = ROTATION_APPS.equals(persistedSet);

        title.setText(isRotationList ? R.string.auto_rotate_apps : R.string.auto_rotate_apps_excluded);
        title.setOnClickListener(view -> new AlertDialog.Builder(itemView.getContext())
                .setMessage(isRotationList ? R.string.auto_rotate_description : R.string.auto_rotate_ignored_description)
                .show());
    }

    @Override
    public void bind() {
        super.bind();

        diff();
        if (!App.canWriteToSettings()) adapterListener.requestPermission(SETTINGS_CODE);
    }

    @Override
    protected String diffHash(ApplicationInfo item) { return item.packageName; }

    @Override
    String getSizeCacheKey() {
        return persistedSet;
    }

    @Override
    Supplier<List<ApplicationInfo>> getListSupplier() {
        return () -> RotationGestureConsumer.getInstance().getList(persistedSet);
    }

    private PackageAdapter.PackageClickListener getPackageClickListener(@RotationGestureConsumer.PersistedSet String preferenceName) {
        return packageName -> {
            RotationGestureConsumer gestureConsumer = RotationGestureConsumer.getInstance();
            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());

            if (!App.canWriteToSettings()) builder.setMessage(R.string.permission_required);

            else if (!gestureConsumer.canAutoRotate())
                builder.setMessage(R.string.auto_rotate_prompt);

            else if (!gestureConsumer.isRemovable(packageName))
                builder.setMessage(R.string.auto_rotate_cannot_remove);

            else builder.setTitle(gestureConsumer.getRemoveText(preferenceName))
                        .setPositiveButton(R.string.yes, ((dialog, which) -> {
                            gestureConsumer.removeFromSet(packageName, preferenceName);
                            bind();
                        }))
                        .setNegativeButton(R.string.no, ((dialog, which) -> dialog.dismiss()));

            builder.show();
        };
    }
}
