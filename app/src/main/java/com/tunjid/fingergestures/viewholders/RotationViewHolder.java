package com.tunjid.fingergestures.viewholders;

import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.adapters.PackageAdapter;
import com.tunjid.fingergestures.fragments.PackageFragment;
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer;

import static android.support.v7.widget.LinearLayoutManager.HORIZONTAL;
import static com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer.EXCLUDED_APPS;
import static com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer.ROTATION_APPS;

public class RotationViewHolder extends AppViewHolder {

    private RecyclerView rotationList;
    private RecyclerView excludedList;

    public RotationViewHolder(View itemView, AppAdapter.AppAdapterListener listener) {
        super(itemView, listener);

        RotationGestureConsumer gestureConsumer = RotationGestureConsumer.getInstance();

        rotationList = itemView.findViewById(R.id.rotation_list);
        excludedList = itemView.findViewById(R.id.excluded_list);

        rotationList.setLayoutManager(new LinearLayoutManager(itemView.getContext(), HORIZONTAL, false));
        rotationList.setAdapter(new PackageAdapter(true, gestureConsumer.getList(ROTATION_APPS),getPackageClickListener(ROTATION_APPS)));

        excludedList.setLayoutManager(new LinearLayoutManager(itemView.getContext(), HORIZONTAL, false));
        excludedList.setAdapter(PackageAdapter.noTextInstance(gestureConsumer.getList(EXCLUDED_APPS),getPackageClickListener(EXCLUDED_APPS)));

        itemView.findViewById(R.id.add_rotation).setOnClickListener(view -> adapterListener.showBottomSheetFragment(PackageFragment.newInstance(ROTATION_APPS)));
        itemView.findViewById(R.id.add_excluded).setOnClickListener(view -> adapterListener.showBottomSheetFragment(PackageFragment.newInstance(EXCLUDED_APPS)));
    }

    @Override
    public void bind() {
        super.bind();
        rotationList.getAdapter().notifyDataSetChanged();
        excludedList.getAdapter().notifyDataSetChanged();
    }

    private PackageAdapter.PackageClickListener getPackageClickListener(@RotationGestureConsumer.PersistedSet String preferenceName) {
        RotationGestureConsumer gestureConsumer = RotationGestureConsumer.getInstance();

        return packageName -> new AlertDialog.Builder(itemView.getContext())
                .setTitle(R.string.auto_rotate_remove)
                .setPositiveButton(R.string.yes, ((dialog, which) -> {
                    gestureConsumer.removeFromSet(packageName, preferenceName);
                    rotationList.getAdapter().notifyDataSetChanged();
                }))
                .setNegativeButton(R.string.no, ((dialog, which) -> dialog.dismiss()))
                .show();
    }
}
