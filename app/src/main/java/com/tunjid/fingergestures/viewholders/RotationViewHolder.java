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

public class RotationViewHolder extends AppViewHolder {

    private RecyclerView recyclerView;

    public RotationViewHolder(View itemView, AppAdapter.AppAdapterListener listener) {
        super(itemView, listener);

        recyclerView = itemView.findViewById(R.id.package_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext(), HORIZONTAL, false));
        recyclerView.setAdapter(new PackageAdapter(true, RotationGestureConsumer.getInstance().getPackageNames(),
                packageName -> new AlertDialog.Builder(itemView.getContext())
                        .setTitle(R.string.auto_rotate_remove)
                        .setPositiveButton(R.string.yes, ((dialog, which) -> {
                            RotationGestureConsumer.getInstance().removeRotationApp(packageName);
                            recyclerView.getAdapter().notifyDataSetChanged();
                        }))
                        .setNegativeButton(R.string.no, ((dialog, which) -> dialog.dismiss()))
                        .show()));

        itemView.findViewById(R.id.add).setOnClickListener(view -> adapterListener.showBottomSheetFragment(PackageFragment.newInstance()));
    }

    @Override
    public void bind() {
        super.bind();
        recyclerView.getAdapter().notifyDataSetChanged();
    }
}
