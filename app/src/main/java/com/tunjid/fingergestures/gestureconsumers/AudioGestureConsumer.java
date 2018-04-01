package com.tunjid.fingergestures.gestureconsumers;

import android.annotation.SuppressLint;
import android.media.AudioManager;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;

import com.tunjid.fingergestures.R;

import static android.media.AudioManager.ADJUST_LOWER;
import static android.media.AudioManager.ADJUST_RAISE;

public class AudioGestureConsumer implements GestureConsumer {

    private static final int DEF_INCREMENT_VALUE = 20;
    private static final int STREAM_TYPE_MEDIA = AudioManager.STREAM_MUSIC;
    private static final int STREAM_TYPE_RING = AudioManager.STREAM_RING;
    private static final int STREAM_TYPE_ALARM = AudioManager.STREAM_ALARM;
    private static final int STREAM_TYPE_DEFAULT = AudioManager.USE_DEFAULT_STREAM_TYPE;
    private static final int STREAM_TYPE_ALL = Integer.MAX_VALUE;
    private static final int MANAGED_BY_SYSTEM = -1;

    private static final String INCREMENT_VALUE = "audio increment value";
    private static final String AUDIO_STREAM_TYPE = "audio stream type";

    @IntDef({STREAM_TYPE_MEDIA, STREAM_TYPE_RING, STREAM_TYPE_ALARM, STREAM_TYPE_ALL, STREAM_TYPE_DEFAULT})
    @interface AudioStream {}

    @SuppressLint("StaticFieldLeak")
    private static AudioGestureConsumer instance;

    public static AudioGestureConsumer getInstance() {
        if (instance == null) instance = new AudioGestureConsumer();
        return instance;
    }

    private AudioGestureConsumer() {}

    @Override
    @SuppressLint("SwitchIntDef")
    public boolean accepts(@GestureAction int gesture) {
        return gesture == INCREASE_AUDIO || gesture == REDUCE_AUDIO;
    }

    @Override
    @SuppressLint("SwitchIntDef")
    public void onGestureActionTriggered(@GestureAction int gestureAction) {
        adjustAudio(gestureAction == INCREASE_AUDIO);
    }

    public boolean canSetVolumeDelta() {
        return getStreamType() != STREAM_TYPE_DEFAULT;
    }

    @IntRange(from = ZERO_PERCENT, to = HUNDRED_PERCENT)
    public int getVolumeDelta() {
        return requestApp(app -> app.getPreferences().getInt(INCREMENT_VALUE, DEF_INCREMENT_VALUE), DEF_INCREMENT_VALUE);
    }

    public void setVolumeDelta(@IntRange(from = ZERO_PERCENT, to = HUNDRED_PERCENT) int volumeDelta) {
        requestApp(app -> app.getPreferences().edit().putInt(INCREMENT_VALUE, volumeDelta).apply());
    }

    public void setStreamType(@IdRes int resId) {
        @AudioStream int streamType = getStreamTypeFromId(resId);
        requestApp(app -> app.getPreferences().edit().putInt(AUDIO_STREAM_TYPE, streamType).apply());
    }

    private void setStreamVolume(boolean increase, AudioManager audioManager, int streamType) {
        int currentVolume = audioManager.getStreamVolume(streamType);
        int newVolume = increase ? increase(currentVolume, streamType, audioManager) : reduce(currentVolume, streamType, audioManager);
        audioManager.setStreamVolume(streamType, newVolume, 0);
    }

    private int reduce(int currentValue, int stream, AudioManager audioManager) {
        return Math.max(currentValue - normalizePercentageForStream(getVolumeDelta(), stream, audioManager), 0);
    }

    private int increase(int currentValue, int stream, AudioManager audioManager) {
        return Math.min(currentValue + normalizePercentageForStream(getVolumeDelta(), stream, audioManager), audioManager.getStreamMaxVolume(stream));
    }

    private void adjustAudio(boolean increase) {
        requestApp(app -> {
            AudioManager audioManager = app.getSystemService(AudioManager.class);
            if (audioManager == null) return;

            int streamType = getStreamType();
            switch (streamType) {
                default:
                case STREAM_TYPE_DEFAULT:
                    audioManager.adjustSuggestedStreamVolume(increase ? ADJUST_RAISE : ADJUST_LOWER, streamType, 0);
                    break;
                case STREAM_TYPE_MEDIA:
                case STREAM_TYPE_ALARM:
                case STREAM_TYPE_RING:
                    setStreamVolume(increase, audioManager, streamType);
                    break;
                case STREAM_TYPE_ALL:
                    setStreamVolume(increase, audioManager, STREAM_TYPE_MEDIA);
                    setStreamVolume(increase, audioManager, STREAM_TYPE_RING);
                    setStreamVolume(increase, audioManager, STREAM_TYPE_ALARM);
                    break;
            }
        });
    }

    public String getChangeText(int percentage) {
        return requestApp(app -> {
            AudioManager audioManager = app.getSystemService(AudioManager.class);
            if (audioManager == null) return null;

            int normalized = normalizePercentageForStream(percentage, getStreamType(), audioManager);
            int maxSteps = getMaxSteps(audioManager);

            return maxSteps == MANAGED_BY_SYSTEM
                    ? app.getString(R.string.audio_stream_text_system)
                    : app.getString(R.string.audio_stream_text, normalized, maxSteps);
        }, "");
    }

    private int normalizePercentageForStream(int percentage, int streamType, AudioManager audioManager) {
        int actualStreamType;
        switch (streamType) {
            case STREAM_TYPE_MEDIA:
            case STREAM_TYPE_RING:
            case STREAM_TYPE_ALARM:
                actualStreamType = streamType;
                break;
            default:
                actualStreamType = STREAM_TYPE_MEDIA;
                break;
        }

        int streamMax = audioManager.getStreamMaxVolume(actualStreamType);
        return Math.max(percentage * streamMax / 100, 1);
    }

    private int getStreamType() {
        return requestApp(app -> app.getPreferences().getInt(AUDIO_STREAM_TYPE, STREAM_TYPE_DEFAULT), STREAM_TYPE_DEFAULT);
    }

    @AudioStream
    private int getStreamWithLargestMax(AudioManager audioManager) {
        int max = Math.max(audioManager.getStreamMaxVolume(STREAM_TYPE_ALARM), audioManager.getStreamMaxVolume(STREAM_TYPE_RING));
        return Math.max(max, audioManager.getStreamMaxVolume(STREAM_TYPE_MEDIA));
    }

    private int getMaxSteps(AudioManager audioManager) {
        int streamType = getStreamType();
        switch (streamType) {
            case STREAM_TYPE_MEDIA:
            case STREAM_TYPE_RING:
            case STREAM_TYPE_ALARM:
                return audioManager.getStreamMaxVolume(streamType);
            case STREAM_TYPE_ALL:
                return getStreamWithLargestMax(audioManager);
            default:
                return MANAGED_BY_SYSTEM;
        }
    }

    @AudioStream
    private int getStreamTypeFromId(@IdRes int resId) {
        switch (resId) {
            default:
            case R.id.stream_default:
                return STREAM_TYPE_DEFAULT;
            case R.id.stream_media:
                return STREAM_TYPE_MEDIA;
            case R.id.stream_ring:
                return STREAM_TYPE_RING;
            case R.id.stream_alarm:
                return STREAM_TYPE_ALARM;
            case R.id.stream_all:
                return STREAM_TYPE_ALL;
        }
    }

    @IdRes
    public int getCheckedId() {
        switch (getStreamType()) {
            default:
            case STREAM_TYPE_DEFAULT:
                return R.id.stream_default;
            case STREAM_TYPE_MEDIA:
                return R.id.stream_media;
            case STREAM_TYPE_RING:
                return R.id.stream_ring;
            case STREAM_TYPE_ALARM:
                return R.id.stream_alarm;
            case STREAM_TYPE_ALL:
                return R.id.stream_all;
        }
    }
}

