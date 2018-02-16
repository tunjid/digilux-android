package com.tunjid.fingergestures.fragments;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.ActionAdapter;
import com.tunjid.fingergestures.baseclasses.MainActivityFragment;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static android.support.v7.widget.DividerItemDecoration.VERTICAL;
import static com.tunjid.fingergestures.adapters.AppAdapter.MAP_DOWN_ICON;
import static com.tunjid.fingergestures.adapters.AppAdapter.MAP_LEFT_ICON;
import static com.tunjid.fingergestures.adapters.AppAdapter.MAP_RIGHT_ICON;
import static com.tunjid.fingergestures.adapters.AppAdapter.MAP_UP_ICON;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.DOUBLE_LEFT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.DOUBLE_RIGHT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.DOUBLE_UP_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.LEFT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.RIGHT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.UP_GESTURE;

public class ActionFragment extends MainActivityFragment implements ActionAdapter.ActionClickListener {

    private static final String ARG_DIRECTION = "DIRECTION";

    private RecyclerView recyclerView;
    private final List<Pair<Integer, Integer>> resources = new ArrayList<>();

    public static ActionFragment newInstance(@GestureMapper.GestureDirection String direction) {
        ActionFragment fragment = new ActionFragment();
        Bundle args = new Bundle();

        args.putString(ARG_DIRECTION, direction);
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
        buildItems();

        recyclerView = root.findViewById(R.id.options_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new ActionAdapter(resources, this));
        recyclerView.addItemDecoration(itemDecoration);

        root.<Toolbar>findViewById(R.id.title_bar).setTitle(R.string.pick_action);

        return root;
    }

    @Override
    public void onActionClicked(int actionRes) {
        Bundle args = getArguments();
        if (args == null) {
            showSnackbar(R.string.generic_error);
            return;
        }

        @GestureMapper.GestureDirection
        String direction = args.getString(ARG_DIRECTION);

        if (direction == null) {
            showSnackbar(R.string.generic_error);
            return;
        }

        toggleBottomSheet(false);

        AppFragment fragment = getCurrentAppFragment();
        if (fragment == null) return;

        GestureMapper.getInstance().mapGestureToAction(direction, actionRes);
        fragment.refresh(LEFT_GESTURE.equals(direction) || DOUBLE_LEFT_GESTURE.equals(direction)
                ? MAP_LEFT_ICON
                : UP_GESTURE.equals(direction) || DOUBLE_UP_GESTURE.equals(direction)
                ? MAP_UP_ICON
                : RIGHT_GESTURE.equals(direction) || DOUBLE_RIGHT_GESTURE.equals(direction)
                ? MAP_RIGHT_ICON
                : RIGHT_GESTURE.equals(direction) || DOUBLE_RIGHT_GESTURE.equals(direction)
                ? MAP_DOWN_ICON : MAP_DOWN_ICON);
    }

    @Override
    public void onDestroyView() {
        recyclerView = null;
        super.onDestroyView();
    }

    private void buildItems() {
        resources.addAll(IntStream.of(GestureMapper.getInstance().getActions())
                .boxed()
                .map(actionMapper)
                .collect(Collectors.toList()));
    }

    private Function<Integer, Pair<Integer, Integer>> actionMapper = stringRes -> {
        @DrawableRes int drawableRes = R.drawable.ic_add_24dp;
        switch (stringRes) {
            case R.string.increase_brightness:
                drawableRes = R.drawable.ic_brightness_medium_24dp;
                break;
            case R.string.reduce_brightness:
                drawableRes = R.drawable.ic_brightness_4_24dp;
                break;
            case R.string.maximize_brightness:
                drawableRes = R.drawable.ic_brightness_7_24dp;
                break;
            case R.string.minimize_brightness:
                drawableRes = R.drawable.ic_brightness_low_24dp;
                break;
            case R.string.notification_up:
                drawableRes = R.drawable.ic_boxed_arrow_up_24dp;
                break;
            case R.string.notification_down:
                drawableRes = R.drawable.ic_boxed_arrow_down_24dp;
                break;
            case R.string.toggle_flashlight:
                drawableRes = R.drawable.ic_brightness_flash_light_24dp;
                break;
            case R.string.toggle_dock:
                drawableRes = R.drawable.ic_arrow_collapse_down_24dp;
                break;
            case R.string.toggle_auto_rotate:
                drawableRes = R.drawable.ic_auto_rotate_24dp;
                break;
            case R.string.do_nothing:
                drawableRes = R.drawable.ic_blank_24dp;
                break;
        }
        return new Pair<>(drawableRes, stringRes);
    };
}
