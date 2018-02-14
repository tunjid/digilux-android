package com.tunjid.fingergestures.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseRecyclerViewAdapter;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.viewholders.PackageViewHolder;

import java.util.List;


public class PackageAdapter extends BaseRecyclerViewAdapter<PackageViewHolder, PackageAdapter.PackageClickListener> {

    private final boolean isHorizontal;
    private final List<String> packageNames;

    public PackageAdapter(boolean isHorizontal, List<String> packageNames, PackageClickListener listener) {
        super(listener);
        setHasStableIds(true);
        this.isHorizontal = isHorizontal;
        this.packageNames = packageNames;
    }

    @Override
    public PackageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutRes = isHorizontal ? R.layout.viewholder_package_horizontal : R.layout.viewholder_package_vertical;
        return new PackageViewHolder(LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false), adapterListener);
    }

    @Override
    public void onBindViewHolder(PackageViewHolder holder, int position) {
        holder.bind(packageNames.get(position));
    }

    @Override
    public int getItemCount() {
        return packageNames.size();
    }

    @Override
    public long getItemId(int position) {
        return packageNames.get(position).hashCode();
    }

    public interface PackageClickListener extends BaseRecyclerViewAdapter.AdapterListener {
        void onPackageClicked(String packageName);
    }

    public static PackageAdapter noTextInstance(List<String> packageNames, PackageClickListener listener) {
        return new PackageAdapter(true, packageNames, listener) {
            @Override
            public PackageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new PackageViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_package_excluded, parent, false), adapterListener);
            }
        };
    }
}
