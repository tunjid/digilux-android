package com.tunjid.fingergestures.viewholders;

import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.adapters.PackageAdapter;
import com.tunjid.fingergestures.fragments.PackageFragment;
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer;

import static android.support.v7.widget.LinearLayoutManager.HORIZONTAL;
import static com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer.ROTATION_APPS;

public class RotationViewHolder extends AppViewHolder {

    private RecyclerView rotationList;

    public RotationViewHolder(View itemView, @RotationGestureConsumer.PersistedSet String persistedSet, AppAdapter.AppAdapterListener listener) {
        super(itemView, listener);

        RotationGestureConsumer gestureConsumer = RotationGestureConsumer.getInstance();

        rotationList = itemView.findViewById(R.id.rotation_list);
        rotationList.setLayoutManager(new LinearLayoutManager(itemView.getContext(), HORIZONTAL, false));
        rotationList.setAdapter(new PackageAdapter(true, gestureConsumer.getList(persistedSet), getPackageClickListener(persistedSet)));

        itemView.<TextView>findViewById(R.id.title).setText(ROTATION_APPS.equals(persistedSet) ? R.string.auto_rotate_apps : R.string.auto_rotate_apps_excluded);
        itemView.findViewById(R.id.add_rotation).setOnClickListener(view -> adapterListener.showBottomSheetFragment(PackageFragment.newInstance(persistedSet)));
    }

    @Override
    public void bind() {
        super.bind();
        rotationList.getAdapter().notifyDataSetChanged();
    }

    private PackageAdapter.PackageClickListener getPackageClickListener(@RotationGestureConsumer.PersistedSet String preferenceName) {
        RotationGestureConsumer gestureConsumer = RotationGestureConsumer.getInstance();

        return packageName -> new AlertDialog.Builder(itemView.getContext())
                .setTitle(gestureConsumer.getRemoveText(preferenceName))
                .setPositiveButton(R.string.yes, ((dialog, which) -> {
                    gestureConsumer.removeFromSet(packageName, preferenceName);
                    rotationList.getAdapter().notifyDataSetChanged();
                }))
                .setNegativeButton(R.string.no, ((dialog, which) -> dialog.dismiss()))
                .show();
    }
}
