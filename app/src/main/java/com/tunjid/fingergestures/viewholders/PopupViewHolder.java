package com.tunjid.fingergestures.viewholders;

import android.view.View;
import android.widget.TextView;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.PopUpGestureConsumer;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.ActionAdapter;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.adapters.DiffAdapter;
import com.tunjid.fingergestures.fragments.ActionFragment;
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.tunjid.fingergestures.activities.MainActivity.SETTINGS_CODE;

public class PopupViewHolder extends AppViewHolder {

    private RecyclerView recyclerView;

    public PopupViewHolder(View itemView, AppAdapter.AppAdapterListener listener) {
        super(itemView, listener);

        PopUpGestureConsumer buttonManager = PopUpGestureConsumer.getInstance();

        recyclerView = itemView.findViewById(R.id.item_list);
        recyclerView.setLayoutManager(new GridLayoutManager(itemView.getContext(), 3));
        recyclerView.setAdapter(new ActionAdapter(true, true, buttonManager::getList, this::onActionClicked));

        itemView.findViewById(R.id.add).setOnClickListener(view -> {
            if (!App.canWriteToSettings())
                new AlertDialog.Builder(itemView.getContext()).setMessage(R.string.permission_required).show();

            else if (!buttonManager.hasAccessibilityButton())
                new AlertDialog.Builder(itemView.getContext()).setMessage(R.string.popup_prompt).show();

            else
                adapterListener.showBottomSheetFragment(ActionFragment.actionInstance());
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
        ((DiffAdapter) recyclerView.getAdapter()).calculateDiff();
        if (!App.canWriteToSettings()) adapterListener.requestPermission(SETTINGS_CODE);
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
