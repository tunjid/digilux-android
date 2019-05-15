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

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.tunjid.androidbootstrap.recyclerview.InteractiveViewHolder;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.DiscreteBrightnessAdapter;


public class DiscreteItemViewHolder extends InteractiveViewHolder<DiscreteBrightnessAdapter.BrightnessValueClickListener> {

    private String discreteValue;
    private TextView textView;

    public DiscreteItemViewHolder(View itemView, DiscreteBrightnessAdapter.BrightnessValueClickListener clickListener) {
        super(itemView, clickListener);
        textView = (TextView) itemView;

        itemView.setOnClickListener(view -> adapterListener.onDiscreteBrightnessClicked(discreteValue));
    }

    public void bind(String discreteValue) {
        this.discreteValue = discreteValue;
        Context context = itemView.getContext();
        textView.setText(context.getString(R.string.discrete_brightness_text, discreteValue));
    }
}
