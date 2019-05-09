package com.tunjid.fingergestures.viewholders;

import android.view.View;
import android.widget.TextView;

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

        listManager = new ListManagerBuilder<ActionViewHolder, Void>()
                .withAdapter(new ActionAdapter(true, true, items, this::onActionClicked))
                .withRecyclerView(itemView.findViewById(R.id.item_list))
                .withGridLayoutManager(3)
                .build();

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
