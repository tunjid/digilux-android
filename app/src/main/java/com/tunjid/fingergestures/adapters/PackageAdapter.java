package com.tunjid.fingergestures.adapters;

import android.content.pm.ApplicationInfo;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.recyclerview.InteractiveAdapter;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.viewholders.PackageViewHolder;

import java.util.List;

import androidx.annotation.NonNull;


public class PackageAdapter extends DiffAdapter<PackageViewHolder, PackageAdapter.PackageClickListener, ApplicationInfo> {

    private final boolean isHorizontal;

    public PackageAdapter(boolean isHorizontal, List<ApplicationInfo> list, PackageClickListener listener) {
        super(list, listener);
        this.isHorizontal = isHorizontal;
    }

    @NonNull
    @Override
    public PackageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = isHorizontal ? R.layout.viewholder_package_horizontal : R.layout.viewholder_package_vertical;
        return new PackageViewHolder(LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false), adapterListener);
    }

    @Override
    public void onBindViewHolder(@NonNull PackageViewHolder holder, int position) {
        holder.bind(list.get(position));
    }

    public interface PackageClickListener extends InteractiveAdapter.AdapterListener {
        void onPackageClicked(String packageName);
    }
}
