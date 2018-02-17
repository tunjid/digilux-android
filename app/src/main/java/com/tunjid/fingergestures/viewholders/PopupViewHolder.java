package com.tunjid.fingergestures.viewholders;

import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.PopUpManager;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.ActionAdapter;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.fragments.ActionFragment;
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer;

import static android.support.v7.widget.LinearLayoutManager.HORIZONTAL;
import static com.tunjid.fingergestures.activities.MainActivity.SETTINGS_CODE;

public class PopupViewHolder extends AppViewHolder {

    private RecyclerView recyclerView;

    public PopupViewHolder(View itemView, AppAdapter.AppAdapterListener listener) {
        super(itemView, listener);

        PopUpManager buttonManager = PopUpManager.getInstance();

        recyclerView = itemView.findViewById(R.id.item_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext(), HORIZONTAL, false));
        recyclerView.setAdapter(new ActionAdapter(true, true, buttonManager.getList(), this::onActionClicked));

        itemView.findViewById(R.id.add).setOnClickListener(view -> {
            if (App.canWriteToSettings())
                adapterListener.showBottomSheetFragment(ActionFragment.actionInstance());
            else
                new AlertDialog.Builder(itemView.getContext()).setMessage(R.string.permission_required).show();
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
        recyclerView.getAdapter().notifyDataSetChanged();
        if (!App.canWriteToSettings()) adapterListener.requestPermission(SETTINGS_CODE);
    }

    private void onActionClicked(@GestureConsumer.GestureAction int action) {
        PopUpManager buttonManager = PopUpManager.getInstance();

        AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());

        if (!App.canWriteToSettings()) builder.setMessage(R.string.permission_required);

        else if (!buttonManager.hasAccessibilityButton()) builder.setMessage(R.string.popup_prompt);

        else builder.setTitle(R.string.popup_remove)
                    .setPositiveButton(R.string.yes, ((dialog, which) -> {
                        buttonManager.removeFromSet(action);
                        recyclerView.getAdapter().notifyDataSetChanged();
                    }))
                    .setNegativeButton(R.string.no, ((dialog, which) -> dialog.dismiss()));

        builder.show();
    }
}
