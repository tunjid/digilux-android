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
