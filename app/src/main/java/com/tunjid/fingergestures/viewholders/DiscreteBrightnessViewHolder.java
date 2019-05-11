package com.tunjid.fingergestures.viewholders;

import android.content.Context;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.tunjid.androidbootstrap.recyclerview.ListManager;
import com.tunjid.androidbootstrap.recyclerview.ListManagerBuilder;
import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.adapters.DiscreteBrightnessAdapter;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;

import java.util.List;
import java.util.function.Supplier;

import androidx.appcompat.app.AlertDialog;

import static com.tunjid.fingergestures.activities.MainActivity.SETTINGS_CODE;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.SLIDER_DELTA;

public class DiscreteBrightnessViewHolder extends DiffViewHolder<String> {

    public DiscreteBrightnessViewHolder(View itemView, List<String> items, AppAdapter.AppAdapterListener listener) {
        super(itemView, items, listener);

        TextView title = itemView.findViewById(R.id.title);
        title.setText(R.string.discrete_brightness_title);
        title.setOnClickListener(view -> new AlertDialog.Builder(itemView.getContext())
                .setMessage(R.string.discrete_brightness_description)
                .show());

        itemView.findViewById(R.id.add).setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
            if (App.canWriteToSettings()) requestDiscreteValue(builder);
            else builder.setMessage(R.string.permission_required).show();
        });
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
    Supplier<List<String>> getListSupplier() {
        return BrightnessGestureConsumer.getInstance()::getDiscreteBrightnessValues;
    }

    @Override
    ListManager<?, Void> createListManager(View itemView) {
        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(itemView.getContext());
        layoutManager.setJustifyContent(JustifyContent.FLEX_START);
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setAlignItems(AlignItems.CENTER);

        return new ListManagerBuilder<DiscreteItemViewHolder, Void>()
                .withAdapter(new DiscreteBrightnessAdapter(items, discreteValue -> {
                    BrightnessGestureConsumer.getInstance().removeDiscreteBrightnessValue(discreteValue);
                    adapterListener.notifyItemChanged(SLIDER_DELTA);
                    bind();
                }))
                .withRecyclerView(itemView.findViewById(R.id.item_list))
                .withCustomLayoutManager(layoutManager)
                .build();
    }

    private void onDiscreteValueEntered(DialogInterface dialogInterface, EditText editText) {
        String discreteValue = editText.getText().toString();
        int value = -1;

        try {value = Integer.valueOf(discreteValue);}
        catch (Exception e) {e.printStackTrace();}

        if (isValidValue(value)) {
            BrightnessGestureConsumer.getInstance().addDiscreteBrightnessValue(discreteValue);
            adapterListener.notifyItemChanged(SLIDER_DELTA);
            bind();
        }

        dialogInterface.dismiss();
    }

    private void requestDiscreteValue(AlertDialog.Builder builder) {
        Context context = itemView.getContext();

        FrameLayout container = new FrameLayout(context);
        EditText editText = new EditText(context);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = params.rightMargin = context.getResources().getDimensionPixelSize(R.dimen.single_and_half_margin);

        container.addView(editText, params);

        AlertDialog alertDialog = builder.setTitle(R.string.discrete_brightness_hint)
                .setView(container)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.ok, (dialog, which) -> onDiscreteValueEntered(dialog, editText))
                .create();

        editText.setImeOptions(EditorInfo.IME_ACTION_SEND);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setFilters(new InputFilter[]{new LengthFilter(2)});
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND)
                onDiscreteValueEntered(alertDialog, editText);
            return true;
        });

        if (alertDialog.getWindow() != null)
            alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alertDialog.show();
    }

    private boolean isValidValue(int value) {
        boolean invalid = value < 1 || value > 99;
        if (invalid) adapterListener.showSnackbar(R.string.discrete_brightness_error);
        return !invalid;
    }
}
