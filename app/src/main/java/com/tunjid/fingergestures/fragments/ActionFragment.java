package com.tunjid.fingergestures.fragments;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.recyclerview.ListManagerBuilder;
import com.tunjid.fingergestures.PopUpGestureConsumer;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.ActionAdapter;
import com.tunjid.fingergestures.baseclasses.MainActivityFragment;
import com.tunjid.fingergestures.billing.PurchasesManager;
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;
import com.tunjid.fingergestures.viewholders.ActionViewHolder;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;

import static androidx.recyclerview.widget.DividerItemDecoration.VERTICAL;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.MAP_DOWN_ICON;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.MAP_LEFT_ICON;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.MAP_RIGHT_ICON;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.MAP_UP_ICON;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.POPUP_ACTION;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.DOUBLE_DOWN_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.DOUBLE_LEFT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.DOUBLE_RIGHT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.DOUBLE_UP_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.DOWN_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.LEFT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.RIGHT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.UP_GESTURE;

public class ActionFragment extends MainActivityFragment implements ActionAdapter.ActionClickListener {

    private static final String ARG_DIRECTION = "DIRECTION";

    public static ActionFragment directionInstance(@GestureMapper.GestureDirection String direction) {
        ActionFragment fragment = new ActionFragment();
        Bundle args = new Bundle();

        args.putString(ARG_DIRECTION, direction);
        fragment.setArguments(args);
        return fragment;
    }

    public static ActionFragment actionInstance() {
        ActionFragment fragment = new ActionFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public String getStableTag() {
        return getClass().getSimpleName();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_actions, container, false);
        Context context = inflater.getContext();

        DividerItemDecoration itemDecoration = new DividerItemDecoration(context, VERTICAL);
        Drawable decoration = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_dark);

        if (decoration != null) itemDecoration.setDrawable(decoration);

        ActionAdapter adapter = new ActionAdapter(false, true, this::getItems, this);

        new ListManagerBuilder<ActionViewHolder, Void>()
                .withRecyclerView(root.findViewById(R.id.options_list))
                .withLinearLayoutManager()
                .withAdapter(adapter)
                .addDecoration(itemDecoration)
                .build();

        adapter.calculateDiff();

        root.<Toolbar>findViewById(R.id.title_bar).setTitle(R.string.pick_action);

        return root;
    }

    @Override
    public void onActionClicked(@GestureConsumer.GestureAction int action) {
        Bundle args = getArguments();
        if (args == null) {
            showSnackbar(R.string.generic_error);
            return;
        }

        @GestureMapper.GestureDirection
        String direction = args.getString(ARG_DIRECTION);

        boolean isActionInstance = direction == null;

        toggleBottomSheet(false);

        AppFragment fragment = getCurrentAppFragment();
        if (fragment == null) return;

        GestureMapper mapper = GestureMapper.getInstance();

        if (isActionInstance) {
            Context context = requireContext();
            if (PopUpGestureConsumer.getInstance().addToSet(action))
                fragment.notifyItemChanged(POPUP_ACTION);
            else new AlertDialog.Builder(context)
                    .setTitle(R.string.go_premium_title)
                    .setMessage(context.getString(R.string.go_premium_body, context.getString(R.string.popup_description)))
                    .setPositiveButton(R.string.continue_text, (dialog, which) -> purchase(PurchasesManager.PREMIUM_SKU))
                    .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .show();
        }
        else {
            mapper.mapGestureToAction(direction, action);
            fragment.notifyItemChanged(LEFT_GESTURE.equals(direction) || DOUBLE_LEFT_GESTURE.equals(direction)
                    ? MAP_LEFT_ICON
                    : UP_GESTURE.equals(direction) || DOUBLE_UP_GESTURE.equals(direction)
                    ? MAP_UP_ICON
                    : RIGHT_GESTURE.equals(direction) || DOUBLE_RIGHT_GESTURE.equals(direction)
                    ? MAP_RIGHT_ICON
                    : DOWN_GESTURE.equals(direction) || DOUBLE_DOWN_GESTURE.equals(direction)
                    ? MAP_DOWN_ICON : MAP_DOWN_ICON);
        }
    }

    private List<Integer> getItems() {
        return IntStream.of(GestureMapper.getInstance().getActions()).boxed().collect(Collectors.toList());
    }
}
