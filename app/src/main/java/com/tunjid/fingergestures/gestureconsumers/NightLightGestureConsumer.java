package com.tunjid.fingergestures.gestureconsumers;

import android.annotation.SuppressLint;
import android.app.UiModeManager;
import android.content.Context;
import android.widget.Toast;

import com.tunjid.fingergestures.Application;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static android.app.UiModeManager.MODE_NIGHT_YES;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.NIGHT_MODE_OFF;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.NIGHT_MODE_ON;

public class NightLightGestureConsumer implements GestureConsumer {

    @SuppressLint("StaticFieldLeak")
    private static NightLightGestureConsumer instance;

    private final Context app;
    private final UiModeManager modeManager;
    private final Set<Integer> gestures;

    static NightLightGestureConsumer getInstance() {
        if (instance == null) instance = new NightLightGestureConsumer();
        return instance;
    }

    private NightLightGestureConsumer() {
        app = Application.getContext();
        modeManager = app.getSystemService(UiModeManager.class);
        gestures = new HashSet<>();
        gestures.add(NIGHT_MODE_ON);
        gestures.add(NIGHT_MODE_OFF);
    }

    @Override
    @SuppressLint("SwitchIntDef")
    public void onGestureActionTriggered(@GestureUtils.GestureAction int gestureAction) {
        switch (gestureAction) {
            case NIGHT_MODE_ON:
            case NIGHT_MODE_OFF:
                boolean activated = gestureAction == NIGHT_MODE_ON;
                try {
                    @SuppressLint("WrongConstant")
                    Class<?> displayClass = Class.forName("com.android.internal.app.NightDisplayController");
                    Constructor<?> cons = displayClass.getConstructor(Context.class);
                    Object sbservice = cons.newInstance(app);

                    Method setActivated;
                    setActivated = displayClass.getMethod("setActivated");
                    setActivated.invoke(sbservice, activated);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(app, "Unsupported", Toast.LENGTH_SHORT).show();
                }
                modeManager.setNightMode(MODE_NIGHT_YES);
                Toast.makeText(app, activated ? "Night mode on" : "Night mode off", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public Set<Integer> gestures() {
        return gestures;
    }
}

