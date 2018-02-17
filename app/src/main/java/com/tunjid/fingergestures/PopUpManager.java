package com.tunjid.fingergestures;


import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.tunjid.fingergestures.billing.PurchasesManager;
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class PopUpManager {

    public static final String ACTION_ACCESSIBILITY_BUTTON = "com.tunjid.fingergestures.action.accessibilityButton";
    public static final String EXTRA_SHOWS_ACCESSIBILITY_BUTTON = "extra shows accessibility button";
    private static final String ACCESSIBILITY_BUTTON_ENABLED = "accessibility button enabled";
    private static final String SAVED_ACTIONS = "accessibility button apps";

    private SetManager<Integer> setManager;

    private static PopUpManager instance;

    public static PopUpManager getInstance() {
        if (instance == null) instance = new PopUpManager();
        return instance;
    }

    private PopUpManager() {
        Comparator<Integer> sorter = Integer::compare;

        Function<String, Boolean> addFilter = preferenceName -> {
            Set<String> set = setManager.getSet(preferenceName);
            return !(set.size() > 2 && PurchasesManager.getInstance().isPremiumNotTrial());
        };

        setManager = new SetManager<>(sorter, addFilter, Integer::valueOf, String::valueOf);
    }

    public boolean hasAccessibilityButton() {
        return App.getInstance().getPreferences().getBoolean(ACCESSIBILITY_BUTTON_ENABLED, false);
    }

    public boolean addToSet(@GestureConsumer.GestureAction int action) {
        return setManager.addToSet(String.valueOf(action), SAVED_ACTIONS);
    }

    public void removeFromSet(@GestureConsumer.GestureAction int action) {
        setManager.removeFromSet(String.valueOf(action), SAVED_ACTIONS);
    }

    public List<String> getList() {
        return setManager.getList(SAVED_ACTIONS);
    }

    public void enableAccessibilityButton(boolean enabled) {
        App.getInstance().getPreferences().edit().putBoolean(ACCESSIBILITY_BUTTON_ENABLED, enabled).apply();

        Intent intent = new Intent(ACTION_ACCESSIBILITY_BUTTON);
        intent.putExtra(EXTRA_SHOWS_ACCESSIBILITY_BUTTON, enabled);

        LocalBroadcastManager.getInstance(App.getInstance()).sendBroadcast(intent);
    }
}
