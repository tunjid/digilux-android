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
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.recyclerview.InteractiveAdapter;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer;
import com.tunjid.fingergestures.viewholders.ActionViewHolder;

import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;


public class ActionAdapter extends DiffAdapter<ActionViewHolder, ActionAdapter.ActionClickListener, Integer> {

    private final boolean isHorizontal;
    private final boolean showsText;

    public ActionAdapter(boolean isHorizontal, boolean showsText, List<Integer> list, ActionClickListener listener) {
        super(list, listener);
        this.isHorizontal = isHorizontal;
        this.showsText = showsText;
    }

    @NonNull
    @Override
    public ActionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        @LayoutRes int layoutRes = isHorizontal ? R.layout.viewholder_action_horizontal : R.layout.viewholder_action_vertical;
        return new ActionViewHolder(showsText, LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false), adapterListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ActionViewHolder holder, int position) {
        holder.bind(list.get(position));
    }

    public interface ActionClickListener extends InteractiveAdapter.AdapterListener {
        void onActionClicked(@GestureConsumer.GestureAction int actionRes);
    }
}
