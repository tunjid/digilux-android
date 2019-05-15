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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.recyclerview.InteractiveAdapter;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.viewholders.DiscreteItemViewHolder;

import java.util.List;

import androidx.annotation.NonNull;


public class DiscreteBrightnessAdapter extends DiffAdapter<DiscreteItemViewHolder, DiscreteBrightnessAdapter.BrightnessValueClickListener, String> {

    public DiscreteBrightnessAdapter(List<String> items, BrightnessValueClickListener listener) {
        super(items, listener);
    }

    @NonNull
    @Override
    public DiscreteItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_chip, parent, false);
        return new DiscreteItemViewHolder(itemView, adapterListener);
    }

    @Override
    public void onBindViewHolder(@NonNull DiscreteItemViewHolder holder, int position) {
        holder.bind(list.get(position));
    }

    public interface BrightnessValueClickListener extends InteractiveAdapter.AdapterListener {
        void onDiscreteBrightnessClicked(String discreteValue);
    }
}
