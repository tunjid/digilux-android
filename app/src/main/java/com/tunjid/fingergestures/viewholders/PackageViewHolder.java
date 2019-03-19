package com.tunjid.fingergestures.viewholders;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.tunjid.androidbootstrap.recyclerview.InteractiveViewHolder;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.PackageAdapter;


public class PackageViewHolder extends InteractiveViewHolder<PackageAdapter.PackageClickListener> {

    private String packageName;

    private ImageView imageView;
    @Nullable private TextView textView;

    public PackageViewHolder(View itemView, PackageAdapter.PackageClickListener clickListener) {
        super(itemView, clickListener);
        imageView = itemView.findViewById(R.id.icon);
        textView = itemView.findViewById(R.id.text);

        itemView.setOnClickListener(view -> adapterListener.onPackageClicked(packageName));
    }

    public void bind(ApplicationInfo info) {
        if (info == null) return;
        PackageManager packageManager = itemView.getContext().getPackageManager();

        packageName = info.packageName;
        imageView.setImageDrawable(packageManager.getApplicationIcon(info));
        if (textView != null) textView.setText(packageManager.getApplicationLabel(info));
    }
}
