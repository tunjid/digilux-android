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

import android.view.View;
import android.widget.TextView;

import com.tunjid.androidbootstrap.recyclerview.ListManager;
import com.tunjid.androidbootstrap.recyclerview.ListManagerBuilder;
import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.PopUpGestureConsumer;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.ActionAdapter;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.fragments.ActionFragment;
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer;

import java.util.List;
import java.util.function.Supplier;

import androidx.appcompat.app.AlertDialog;

import static com.tunjid.fingergestures.activities.MainActivity.SETTINGS_CODE;

public class PopupViewHolder extends DiffViewHolder<Integer> {

    public PopupViewHolder(View itemView, List<Integer> items, AppAdapter.AppAdapterListener listener) {
        super(itemView, items, listener);

        itemView.findViewById(R.id.add).setOnClickListener(view -> {
            if (!App.canWriteToSettings())
                new AlertDialog.Builder(itemView.getContext()).setMessage(R.string.permission_required).show();

            else if (!PopUpGestureConsumer.getInstance().hasAccessibilityButton())
                new AlertDialog.Builder(itemView.getContext()).setMessage(R.string.popup_prompt).show();

            else
                adapterListener.showBottomSheetFragment(ActionFragment.popUpInstance());
        });

        TextView title = itemView.findViewById(R.id.title);

        title.setText(R.string.popup_title);
        title.setOnClickListener(view -> new AlertDialog.Builder(itemView.getContext())
                .setMessage(R.string.popup_description)
                .show());
    }

    @Override
    public void bind() {
        super.bind();

        diff();
        if (!App.canWriteToSettings()) adapterListener.requestPermission(SETTINGS_CODE);
    }

    @Override
    String getSizeCacheKey() {
        return getClass().getSimpleName();
    }

    @Override
    Supplier<List<Integer>> getListSupplier() {
        return PopUpGestureConsumer.getInstance()::getList;
    }

    @Override
    ListManager<?, Void> createListManager(View itemView) {
        return new ListManagerBuilder<ActionViewHolder, Void>()
                .withAdapter(new ActionAdapter(true, true, items, this::onActionClicked))
                .withRecyclerView(itemView.findViewById(R.id.item_list))
                .withGridLayoutManager(3)
                .build();
    }

    private void onActionClicked(@GestureConsumer.GestureAction int action) {
        PopUpGestureConsumer buttonManager = PopUpGestureConsumer.getInstance();

        AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());

        if (!App.canWriteToSettings()) builder.setMessage(R.string.permission_required);

        else if (!buttonManager.hasAccessibilityButton()) builder.setMessage(R.string.popup_prompt);

        else builder.setTitle(R.string.popup_remove)
                    .setPositiveButton(R.string.yes, ((dialog, which) -> {
                        buttonManager.removeFromSet(action);
                        bind();
                    }))
                    .setNegativeButton(R.string.no, ((dialog, which) -> dialog.dismiss()));

        builder.show();
    }
}
