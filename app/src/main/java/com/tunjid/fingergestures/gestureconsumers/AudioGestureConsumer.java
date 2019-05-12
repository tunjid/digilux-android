package com.tunjid.fingergestures.gestureconsumers;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Intent;
import android.media.AudioManager;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.R;

import java.util.function.BiFunction;

import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;

import static android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS;
import static android.app.NotificationManager.INTERRUPTION_FILTER_ALL;
import static android.media.AudioManager.ADJUST_LOWER;
import static android.media.AudioManager.ADJUST_RAISE;
import static com.tunjid.fingergestures.App.delay;
import static com.tunjid.fingergestures.App.hasDoNotDisturbAccess;
import static com.tunjid.fingergestures.App.withApp;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class AudioGestureConsumer implements GestureConsumer {

    private static final int NO_FLAGS = 0;
    private static final int MIN_VOLUME = 0;
    private static final int DEF_INCREMENT_VALUE = 20;
    private static final int EXPAND_VOLUME_DELAY = 200;
    private static final int STREAM_TYPE_MEDIA = AudioManager.STREAM_MUSIC;
    private static final int STREAM_TYPE_RING = AudioManager.STREAM_RING;
    private static final int STREAM_TYPE_ALARM = AudioManager.STREAM_ALARM;
    private static final int STREAM_TYPE_DEFAULT = AudioManager.USE_DEFAULT_STREAM_TYPE;
    private static final int STREAM_TYPE_ALL = Integer.MAX_VALUE;
    private static final int MANAGED_BY_SYSTEM = -1;

    public static final String ACTION_EXPAND_VOLUME_CONTROLS = "AudioGestureConsumer expand volume";
    private static final String INCREMENT_VALUE = "audio increment value";
    private static final String AUDIO_STREAM_TYPE = "audio stream type";
    private static final String SHOWS_AUDIO_SLIDER = "audio slider show";
    private static final String EMPTY_STRING = "";


    @IntDef({STREAM_TYPE_MEDIA, STREAM_TYPE_RING, STREAM_TYPE_ALARM, STREAM_TYPE_ALL, STREAM_TYPE_DEFAULT})
    @interface AudioStream {}

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
        return hasDoNotDisturbAccess() && getStreamType() != STREAM_TYPE_DEFAULT;
    }

    public boolean shouldShowSliders() {
        return App.transformApp(app -> app.getPreferences().getBoolean(SHOWS_AUDIO_SLIDER, true), true);
    }

    @IntRange(from = ZERO_PERCENT, to = HUNDRED_PERCENT)
    public int getVolumeDelta() {
        return App.transformApp(app -> app.getPreferences().getInt(INCREMENT_VALUE, DEF_INCREMENT_VALUE), DEF_INCREMENT_VALUE);
    }

    public void setVolumeDelta(@IntRange(from = ZERO_PERCENT,
            to = HUNDRED_PERCENT) int volumeDelta) {
        withApp(app -> app.getPreferences().edit().putInt(INCREMENT_VALUE, volumeDelta).apply());
    }

    public void setStreamType(@IdRes int resId) {
        @AudioStream int streamType = getStreamTypeFromId(resId);
        withApp(app -> app.getPreferences().edit().putInt(AUDIO_STREAM_TYPE, streamType).apply());
    }

    private void setStreamVolume(boolean increase, AudioManager audioManager, int streamType) {
        int currentVolume = audioManager.getStreamVolume(streamType);
        int newVolume = increase ? increase(currentVolume, streamType, audioManager) : reduce(currentVolume, streamType, audioManager);

        boolean ringStream = STREAM_TYPE_RING == streamType;
        boolean turnDnDOn = ringStream && currentVolume == 0 && !increase;
        boolean turnDnDOff = ringStream && newVolume != 0 && increase;

        if (turnDnDOn || turnDnDOff) withApp(app -> {
            NotificationManager notificationManager = app.getSystemService(NotificationManager.class);
            if (notificationManager == null) return;

            int filter = turnDnDOn ? INTERRUPTION_FILTER_ALARMS : INTERRUPTION_FILTER_ALL;
            if (notificationManager.getCurrentInterruptionFilter() == filter) return;

            notificationManager.setInterruptionFilter(filter);
        });

        if (!turnDnDOn) audioManager.setStreamVolume(streamType, newVolume, getFlags());
    }

    public void setShowsSliders(boolean visible) {
        withApp(app -> app.getPreferences().edit().putBoolean(SHOWS_AUDIO_SLIDER, visible).apply());
    }

    private int reduce(int currentValue, int stream, AudioManager audioManager) {
        return Math.max(currentValue - normalizePercentageForStream(getVolumeDelta(), stream, audioManager), MIN_VOLUME);
    }

    private int increase(int currentValue, int stream, AudioManager audioManager) {
        return Math.min(currentValue + normalizePercentageForStream(getVolumeDelta(), stream, audioManager), audioManager.getStreamMaxVolume(stream));
    }

    private void adjustAudio(boolean increase) {
        requireAppAndAudioManager((app, audioManager) -> {
            if (!hasDoNotDisturbAccess()) return Void.TYPE;
            int streamType = getStreamType();
            switch (streamType) {
                default:
                case STREAM_TYPE_DEFAULT:
                    audioManager.adjustSuggestedStreamVolume(increase ? ADJUST_RAISE : ADJUST_LOWER, streamType, getFlags());
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
            return Void.TYPE;
        }, Void.TYPE);
    }

    public String getChangeText(int percentage) {
        return requireAppAndAudioManager((app, audioManager) -> {
            if (!hasDoNotDisturbAccess()) return app.getString(R.string.enable_do_not_disturb);

            int normalized = normalizePercentageForStream(percentage, getStreamType(), audioManager);
            int maxSteps = getMaxSteps(audioManager);

            return maxSteps == MANAGED_BY_SYSTEM
                    ? app.getString(R.string.audio_stream_text_system)
                    : app.getString(R.string.audio_stream_text, normalized, maxSteps);
        }, EMPTY_STRING);
    }

    public String getStreamTitle(@IdRes int resId) {
        switch (getStreamTypeFromId(resId)) {
            default:
            case STREAM_TYPE_DEFAULT:
                return App.transformApp(app -> app.getString(R.string.audio_stream_default), EMPTY_STRING);
            case STREAM_TYPE_MEDIA:
                return requireAppAndAudioManager((app, audioManager) -> app.getString(R.string.audio_stream_media, audioManager.getStreamMaxVolume(STREAM_TYPE_MEDIA)), EMPTY_STRING);
            case STREAM_TYPE_RING:
                return requireAppAndAudioManager((app, audioManager) -> app.getString(R.string.audio_stream_ringtone, audioManager.getStreamMaxVolume(STREAM_TYPE_RING)), EMPTY_STRING);
            case STREAM_TYPE_ALARM:
                return requireAppAndAudioManager((app, audioManager) -> app.getString(R.string.audio_stream_alarm, audioManager.getStreamMaxVolume(STREAM_TYPE_ALARM)), EMPTY_STRING);
            case STREAM_TYPE_ALL:
                return requireAppAndAudioManager((app, audioManager) -> app.getString(R.string.audio_stream_all, getMaxSteps(audioManager)), EMPTY_STRING);
        }
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
        return App.transformApp(app -> app.getPreferences().getInt(AUDIO_STREAM_TYPE, STREAM_TYPE_DEFAULT), STREAM_TYPE_DEFAULT);
    }

    private int getFlags() {
        boolean shouldShowSlider = shouldShowSliders();
        if (shouldShowSlider && getStreamType() == STREAM_TYPE_ALL) broadcastExpandVolumeIntent();
        return shouldShowSlider ? AudioManager.FLAG_SHOW_UI : NO_FLAGS;
    }

    private void broadcastExpandVolumeIntent() {
        Intent expandVolumeIntent = new Intent(ACTION_EXPAND_VOLUME_CONTROLS);
        delay(EXPAND_VOLUME_DELAY, MILLISECONDS, () -> withApp(app -> app.broadcast(expandVolumeIntent)));
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

    private <T> T requireAppAndAudioManager(BiFunction<App, AudioManager, T> biFunction, T defaultValue) {
        return App.transformApp(app -> {
            AudioManager audioManager = app.getSystemService(AudioManager.class);
            return audioManager != null ? biFunction.apply(app, audioManager) : defaultValue;
        }, defaultValue);
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

//    public void setVolume(@IdRes int idRes, @IntRange(from = ZERO_PERCENT, to = HUNDRED_PERCENT) int percentage) {
//        AudioManager audioManager = App.requireApp(app -> app.getSystemService(AudioManager.class), null);
//        if (audioManager == null) return;
//
//        @AudioStream int streamType = getStreamTypeFromId(idRes);
//
//        switch (streamType) {
//            default:
//            case STREAM_TYPE_DEFAULT:
//                // No op
//                break;
//            case STREAM_TYPE_MEDIA:
//            case STREAM_TYPE_RING:
//            case STREAM_TYPE_ALARM:
//                setStreamVolume(percentage, audioManager, streamType);
//                break;
//            case STREAM_TYPE_ALL:
//                setStreamVolume(percentage, audioManager, STREAM_TYPE_MEDIA);
//                setStreamVolume(percentage, audioManager, STREAM_TYPE_RING);
//                setStreamVolume(percentage, audioManager, STREAM_TYPE_ALARM);
//                break;
//        }
//    }
//
//    private void setStreamVolume(@IntRange(from = ZERO_PERCENT, to = HUNDRED_PERCENT) int percentage, AudioManager audioManager, int streamType) {
//        int newVolume = normalizePercentageForStream(percentage, streamType, audioManager);
//        audioManager.setStreamVolume(streamType, newVolume, getFlags());
//    }