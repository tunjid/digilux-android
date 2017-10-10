package com.tunjid.fingergestures.gestureconsumers;

import java.util.Set;

public interface GestureConsumer {

    void onGestureActionTriggered(@GestureUtils.GestureAction int gestureAction);

    Set<Integer> gestures();

    default boolean accepts(@GestureUtils.GestureAction int gesture) {
        return gestures().contains(gesture);
    }
}
