package com.tunjid.fingergestures;


import android.content.Intent;

import com.tunjid.fingergestures.activities.PopupActivity;
import com.tunjid.fingergestures.billing.PurchasesVerifier;
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;

import java.util.List;

import static com.tunjid.fingergestures.App.transformApp;
import static com.tunjid.fingergestures.App.withApp;

public class PopUpGestureConsumer implements GestureConsumer {

    public static final String ACTION_ACCESSIBILITY_BUTTON = "com.tunjid.fingergestures.action.accessibilityButton";
    public static final String ACTION_SHOW_POPUP = "PopUpGestureConsumer shows popup";
    public static final String EXTRA_SHOWS_ACCESSIBILITY_BUTTON = "extra shows accessibility button";
    private static final String ACCESSIBILITY_BUTTON_ENABLED = "accessibility button enabled";
    private static final String ACCESSIBILITY_BUTTON_SINGLE_CLICK = "accessibility button single click";
    private static final String SAVED_ACTIONS = "accessibility button apps";
    private static final String ANIMATES_POPUP = "animates popup";

    private final SetManager<Integer> setManager;

    private static PopUpGestureConsumer instance;

    public static PopUpGestureConsumer getInstance() {
        if (instance == null) instance = new PopUpGestureConsumer();
        return instance;
    }

    @Override
    public void onGestureActionTriggered(int gestureAction) {
        withApp(app -> app.broadcast(new Intent(ACTION_SHOW_POPUP)));
    }

    @Override
    public boolean accepts(int gesture) {
        return gesture == SHOW_POPUP;
    }

    private PopUpGestureConsumer() {
        setManager = new SetManager<>(Integer::compare, this::canAddToSet, Integer::valueOf, String::valueOf);
    }

    public boolean hasAccessibilityButton() {
        return transformApp(app -> app.getPreferences().getBoolean(ACCESSIBILITY_BUTTON_ENABLED, false), false);
    }

    public boolean isSingleClick() {
        return transformApp(app -> app.getPreferences().getBoolean(ACCESSIBILITY_BUTTON_SINGLE_CLICK, false), false);
    }

    public boolean shouldAnimatePopup() {
        return transformApp(app -> app.getPreferences().getBoolean(ANIMATES_POPUP, true), true);
    }

    public boolean addToSet(@GestureConsumer.GestureAction int action) {
        return setManager.addToSet(String.valueOf(action), SAVED_ACTIONS);
    }

    public void removeFromSet(@GestureConsumer.GestureAction int action) {
        setManager.removeFromSet(String.valueOf(action), SAVED_ACTIONS);
    }

    public void setAnimatesPopup(boolean visible) {
        withApp(app -> app.getPreferences().edit().putBoolean(ANIMATES_POPUP, visible).apply());
    }

    public void setSingleClick(boolean isSingleClick) {
        withApp(app -> app.getPreferences().edit().putBoolean(ACCESSIBILITY_BUTTON_SINGLE_CLICK, isSingleClick).apply());
    }

    public void showPopup() {
        if (isSingleClick()) getList().stream()
                .findFirst()
                .ifPresent(GestureMapper.getInstance()::performAction);
        else withApp(app -> {
            Intent intent = new Intent(app, PopupActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            app.startActivity(intent);
        });
    }

    public List<Integer> getList() {
        return setManager.getItems(SAVED_ACTIONS);
    }

    public void enableAccessibilityButton(boolean enabled) {
        withApp(app -> {
            app.getPreferences().edit().putBoolean(ACCESSIBILITY_BUTTON_ENABLED, enabled).apply();
            app.broadcast(new Intent(ACTION_ACCESSIBILITY_BUTTON)
                    .putExtra(EXTRA_SHOWS_ACCESSIBILITY_BUTTON, enabled));
        });
    }

    private boolean canAddToSet(String preferenceName) {
        return setManager.getSet(preferenceName).size() < 2 || PurchasesVerifier.getInstance().isPremiumNotTrial();
    }
}
