package com.tunjid.fingergestures.gestureconsumers;


public class GestureUtils {

    public static int normalizePercetageToByte(int percentage) {
        return (int) (BrightnessGestureConsumer.MAX_BRIGHTNESS * (percentage / 100F));
    }

    static float normalizePercetageToFraction(int percentage) {
        return percentage / 100F;
    }
}
