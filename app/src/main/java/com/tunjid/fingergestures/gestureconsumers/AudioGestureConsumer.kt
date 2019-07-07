/*
 * Copyright (c) 2017, 2018, 2019 Adetunji Dahunsi.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tunjid.fingergestures.gestureconsumers

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS
import android.app.NotificationManager.INTERRUPTION_FILTER_ALL
import android.content.Intent
import android.media.AudioManager
import android.media.AudioManager.ADJUST_LOWER
import android.media.AudioManager.ADJUST_RAISE
import androidx.annotation.IdRes
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.math.max
import kotlin.math.min

class AudioGestureConsumer private constructor() : GestureConsumer {

    var volumeDelta: Int
        @IntRange(from = GestureConsumer.ZERO_PERCENT.toLong(), to = GestureConsumer.HUNDRED_PERCENT.toLong())

        get() = App.transformApp({ app -> app.preferences.getInt(INCREMENT_VALUE, DEF_INCREMENT_VALUE) }, DEF_INCREMENT_VALUE)
        set(@IntRange(
                from = GestureConsumer.ZERO_PERCENT.toLong(),
                to = GestureConsumer.HUNDRED_PERCENT.toLong()
        ) volumeDelta) = App.withApp { app -> app.preferences.edit().putInt(INCREMENT_VALUE, volumeDelta).apply() }


    private var streamType: Int
        get() = App.transformApp({ app -> app.preferences.getInt(AUDIO_STREAM_TYPE, STREAM_TYPE_DEFAULT) }, STREAM_TYPE_DEFAULT)
        set(@IdRes resId) {
            @AudioStream val streamType = getStreamTypeFromId(resId)
            App.withApp { app -> app.preferences.edit().putInt(AUDIO_STREAM_TYPE, streamType).apply() }
        }

    private val flags: Int
        get() {
            val shouldShowSlider = shouldShowSliders()
            if (shouldShowSlider && streamType == STREAM_TYPE_ALL) broadcastExpandVolumeIntent()
            return if (shouldShowSlider) AudioManager.FLAG_SHOW_UI else NO_FLAGS
        }

    val checkedId: Int
        @IdRes
        get() {
            return when (streamType) {
                STREAM_TYPE_DEFAULT -> R.id.stream_default
                STREAM_TYPE_MEDIA -> R.id.stream_media
                STREAM_TYPE_RING -> R.id.stream_ring
                STREAM_TYPE_ALARM -> R.id.stream_alarm
                STREAM_TYPE_ALL -> R.id.stream_all
                else -> R.id.stream_default
            }
        }


    @IntDef(STREAM_TYPE_MEDIA, STREAM_TYPE_RING, STREAM_TYPE_ALARM, STREAM_TYPE_ALL, STREAM_TYPE_DEFAULT)
    internal annotation class AudioStream

    @SuppressLint("SwitchIntDef")
    override fun accepts(@GestureConsumer.GestureAction gesture: Int): Boolean {
        return gesture == GestureConsumer.INCREASE_AUDIO || gesture == GestureConsumer.REDUCE_AUDIO
    }

    @SuppressLint("SwitchIntDef")
    override fun onGestureActionTriggered(@GestureConsumer.GestureAction gestureAction: Int) =
            adjustAudio(gestureAction == GestureConsumer.INCREASE_AUDIO)

    fun canSetVolumeDelta(): Boolean =
            App.hasDoNotDisturbAccess() && streamType != STREAM_TYPE_DEFAULT

    fun shouldShowSliders(): Boolean =
            App.transformApp({ app -> app.preferences.getBoolean(SHOWS_AUDIO_SLIDER, true) }, true)

    private fun setStreamVolume(increase: Boolean, audioManager: AudioManager, streamType: Int) {
        val currentVolume = audioManager.getStreamVolume(streamType)
        val newVolume = if (increase) increase(currentVolume, streamType, audioManager) else reduce(currentVolume, streamType, audioManager)

        val ringStream = STREAM_TYPE_RING == streamType
        val turnDnDOn = ringStream && currentVolume == 0 && !increase
        val turnDnDOff = ringStream && newVolume != 0 && increase

        if (turnDnDOn || turnDnDOff) App.withApp { app ->
            val notificationManager = app.getSystemService(NotificationManager::class.java) ?: return@withApp

            val filter = if (turnDnDOn) INTERRUPTION_FILTER_ALARMS else INTERRUPTION_FILTER_ALL
            if (notificationManager.currentInterruptionFilter == filter) return@withApp

            notificationManager.setInterruptionFilter(filter)
        }

        if (!turnDnDOn) audioManager.setStreamVolume(streamType, newVolume, flags)
    }

    fun setShowsSliders(visible: Boolean) =
            App.withApp { app -> app.preferences.edit().putBoolean(SHOWS_AUDIO_SLIDER, visible).apply() }

    private fun reduce(currentValue: Int, stream: Int, audioManager: AudioManager): Int =
            max(currentValue - normalizePercentageForStream(volumeDelta, stream, audioManager), MIN_VOLUME)

    private fun increase(currentValue: Int, stream: Int, audioManager: AudioManager): Int =
            min(currentValue + normalizePercentageForStream(volumeDelta, stream, audioManager), audioManager.getStreamMaxVolume(stream))

    private fun adjustAudio(increase: Boolean) {
        requireAppAndAudioManager({ app, audioManager ->
            if (!App.hasDoNotDisturbAccess()) return@requireAppAndAudioManager Void.TYPE
            when (val streamType = streamType) {
                STREAM_TYPE_DEFAULT -> audioManager.adjustSuggestedStreamVolume(if (increase) ADJUST_RAISE else ADJUST_LOWER, streamType, flags)
                STREAM_TYPE_MEDIA, STREAM_TYPE_ALARM, STREAM_TYPE_RING -> setStreamVolume(increase, audioManager, streamType)
                STREAM_TYPE_ALL -> {
                    setStreamVolume(increase, audioManager, STREAM_TYPE_MEDIA)
                    setStreamVolume(increase, audioManager, STREAM_TYPE_RING)
                    setStreamVolume(increase, audioManager, STREAM_TYPE_ALARM)
                }
                else -> audioManager.adjustSuggestedStreamVolume(if (increase) ADJUST_RAISE else ADJUST_LOWER, streamType, flags)
            }
            Void.TYPE
        }, Void.TYPE)
    }

    fun getChangeText(percentage: Int): String {
        return requireAppAndAudioManager({ app, audioManager ->
            if (!App.hasDoNotDisturbAccess()) return@requireAppAndAudioManager app.getString(R.string.enable_do_not_disturb)

            val normalized = normalizePercentageForStream(percentage, streamType, audioManager)
            val maxSteps = getMaxSteps(audioManager)

            if (maxSteps == MANAGED_BY_SYSTEM)
                app.getString(R.string.audio_stream_text_system)
            else
                app.getString(R.string.audio_stream_text, normalized, maxSteps)
        }, EMPTY_STRING)
    }

    fun getStreamTitle(@IdRes resId: Int): String {
        when (getStreamTypeFromId(resId)) {
            STREAM_TYPE_DEFAULT -> return App.transformApp({ app -> app.getString(R.string.audio_stream_default) }, EMPTY_STRING)
            STREAM_TYPE_MEDIA -> return requireAppAndAudioManager({ app, audioManager -> app.getString(R.string.audio_stream_media, audioManager.getStreamMaxVolume(STREAM_TYPE_MEDIA)) }, EMPTY_STRING)
            STREAM_TYPE_RING -> return requireAppAndAudioManager({ app, audioManager -> app.getString(R.string.audio_stream_ringtone, audioManager.getStreamMaxVolume(STREAM_TYPE_RING)) }, EMPTY_STRING)
            STREAM_TYPE_ALARM -> return requireAppAndAudioManager({ app, audioManager -> app.getString(R.string.audio_stream_alarm, audioManager.getStreamMaxVolume(STREAM_TYPE_ALARM)) }, EMPTY_STRING)
            STREAM_TYPE_ALL -> return requireAppAndAudioManager({ app, audioManager -> app.getString(R.string.audio_stream_all, getMaxSteps(audioManager)) }, EMPTY_STRING)
            else -> return App.transformApp({ app -> app.getString(R.string.audio_stream_default) }, EMPTY_STRING)
        }
    }

    private fun normalizePercentageForStream(percentage: Int, streamType: Int, audioManager: AudioManager): Int {
        val actualStreamType: Int = when (streamType) {
            STREAM_TYPE_MEDIA, STREAM_TYPE_RING, STREAM_TYPE_ALARM -> streamType
            else -> STREAM_TYPE_MEDIA
        }

        val streamMax = audioManager.getStreamMaxVolume(actualStreamType)
        return max(percentage * streamMax / 100, 1)
    }

    private fun broadcastExpandVolumeIntent() {
        val expandVolumeIntent = Intent(ACTION_EXPAND_VOLUME_CONTROLS)
        App.delay(EXPAND_VOLUME_DELAY, MILLISECONDS) { App.withApp { app -> app.broadcast(expandVolumeIntent) } }
    }

    @AudioStream
    private fun getStreamWithLargestMax(audioManager: AudioManager): Int {
        val max = max(audioManager.getStreamMaxVolume(STREAM_TYPE_ALARM), audioManager.getStreamMaxVolume(STREAM_TYPE_RING))
        return max(max, audioManager.getStreamMaxVolume(STREAM_TYPE_MEDIA))
    }

    private fun getMaxSteps(audioManager: AudioManager): Int = when (val streamType = streamType) {
        STREAM_TYPE_MEDIA, STREAM_TYPE_RING, STREAM_TYPE_ALARM -> audioManager.getStreamMaxVolume(streamType)
        STREAM_TYPE_ALL -> getStreamWithLargestMax(audioManager)
        else -> MANAGED_BY_SYSTEM
    }

    private fun <T> requireAppAndAudioManager(biFunction: (App, AudioManager) -> T, defaultValue: T): T =
            App.transformApp({ app ->
                val audioManager = app.getSystemService(AudioManager::class.java)
                if (audioManager != null) biFunction.invoke(app, audioManager) else defaultValue
            }, defaultValue)

    @AudioStream
    private fun getStreamTypeFromId(@IdRes resId: Int): Int {
        return when (resId) {
            R.id.stream_default -> STREAM_TYPE_DEFAULT
            R.id.stream_media -> STREAM_TYPE_MEDIA
            R.id.stream_ring -> STREAM_TYPE_RING
            R.id.stream_alarm -> STREAM_TYPE_ALARM
            R.id.stream_all -> STREAM_TYPE_ALL
            else -> STREAM_TYPE_DEFAULT
        }
    }

    companion object {

        private const val NO_FLAGS = 0
        private const val MIN_VOLUME = 0
        private const val DEF_INCREMENT_VALUE = 20
        private const val EXPAND_VOLUME_DELAY = 200L
        private const val STREAM_TYPE_MEDIA = AudioManager.STREAM_MUSIC
        private const val STREAM_TYPE_RING = AudioManager.STREAM_RING
        private const val STREAM_TYPE_ALARM = AudioManager.STREAM_ALARM
        private const val STREAM_TYPE_DEFAULT = AudioManager.USE_DEFAULT_STREAM_TYPE
        private const val STREAM_TYPE_ALL = Integer.MAX_VALUE
        private const val MANAGED_BY_SYSTEM = -1

        const val ACTION_EXPAND_VOLUME_CONTROLS = "AudioGestureConsumer expand volume"
        private const val INCREMENT_VALUE = "audio increment value"
        private const val AUDIO_STREAM_TYPE = "audio stream type"
        private const val SHOWS_AUDIO_SLIDER = "audio slider show"
        private const val EMPTY_STRING = ""

        val instance: AudioGestureConsumer  by lazy { AudioGestureConsumer() }

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