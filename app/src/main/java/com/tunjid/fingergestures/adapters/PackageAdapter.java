package com.tunjid.fingergestures.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseRecyclerViewAdapter;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.viewholders.PackageViewHolder;

import java.util.List;
import java.util.function.Supplier;


public class PackageAdapter extends DiffAdapter<PackageViewHolder, PackageAdapter.PackageClickListener> {

    private final boolean isHorizontal;

    public PackageAdapter(boolean isHorizontal, Supplier<List<String>> listSupplier, PackageClickListener listener) {
        super(listSupplier, listener);
        setHasStableIds(true);
        this.isHorizontal = isHorizontal;
    }

    @Override
    public PackageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutRes = isHorizontal ? R.layout.viewholder_package_horizontal : R.layout.viewholder_package_vertical;
        return new PackageViewHolder(LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false), adapterListener);
    }

    @Override
    public void onBindViewHolder(PackageViewHolder holder, int position) {
        holder.bind(list.get(position));
    }

    public interface PackageClickListener extends BaseRecyclerViewAdapter.AdapterListener {
        void onPackageClicked(String packageName);
    }
}
