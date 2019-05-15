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
