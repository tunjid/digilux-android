package com.tunjid.fingergestures.viewmodels;

import android.app.Application;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.activities.MainActivity;
import com.tunjid.fingergestures.models.State;
import com.tunjid.fingergestures.models.TextLink;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.processors.PublishProcessor;

import static com.tunjid.fingergestures.activities.MainActivity.ACCESSIBILITY_CODE;
import static com.tunjid.fingergestures.activities.MainActivity.DO_NOT_DISTURB_CODE;
import static com.tunjid.fingergestures.activities.MainActivity.SETTINGS_CODE;
import static com.tunjid.fingergestures.activities.MainActivity.STORAGE_CODE;
import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;

public class AppViewModel extends AndroidViewModel {

    public static final int PADDING = -1;
    public static final int SLIDER_DELTA = PADDING + 1;
    public static final int SLIDER_POSITION = SLIDER_DELTA + 1;
    public static final int SLIDER_DURATION = SLIDER_POSITION + 1;
    public static final int SLIDER_COLOR = SLIDER_DURATION + 1;
    public static final int SCREEN_DIMMER = SLIDER_COLOR + 1;
    public static final int USE_LOGARITHMIC_SCALE = SCREEN_DIMMER + 1;
    public static final int SHOW_SLIDER = USE_LOGARITHMIC_SCALE + 1;
    public static final int ADAPTIVE_BRIGHTNESS = SHOW_SLIDER + 1;
    public static final int ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS = ADAPTIVE_BRIGHTNESS + 1;
    public static final int DOUBLE_SWIPE_SETTINGS = ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS + 1;
    public static final int MAP_UP_ICON = DOUBLE_SWIPE_SETTINGS + 1;
    public static final int MAP_DOWN_ICON = MAP_UP_ICON + 1;
    public static final int MAP_LEFT_ICON = MAP_DOWN_ICON + 1;
    public static final int MAP_RIGHT_ICON = MAP_LEFT_ICON + 1;
    public static final int AD_FREE = MAP_RIGHT_ICON + 1;
    public static final int REVIEW = AD_FREE + 1;
    public static final int WALLPAPER_PICKER = REVIEW + 1;
    public static final int WALLPAPER_TRIGGER = WALLPAPER_PICKER + 1;
    public static final int ROTATION_LOCK = WALLPAPER_TRIGGER + 1;
    public static final int EXCLUDED_ROTATION_LOCK = ROTATION_LOCK + 1;
    public static final int ENABLE_WATCH_WINDOWS = EXCLUDED_ROTATION_LOCK + 1;
    public static final int POPUP_ACTION = ENABLE_WATCH_WINDOWS + 1;
    public static final int ENABLE_ACCESSIBILITY_BUTTON = POPUP_ACTION + 1;
    public static final int ACCESSIBILITY_SINGLE_CLICK = ENABLE_ACCESSIBILITY_BUTTON + 1;
    public static final int ANIMATES_SLIDER = ACCESSIBILITY_SINGLE_CLICK + 1;
    public static final int ANIMATES_POPUP = ANIMATES_SLIDER + 1;
    public static final int DISCRETE_BRIGHTNESS = ANIMATES_POPUP + 1;
    public static final int AUDIO_DELTA = DISCRETE_BRIGHTNESS + 1;
    public static final int AUDIO_STREAM_TYPE = AUDIO_DELTA + 1;
    public static final int AUDIO_SLIDER_SHOW = AUDIO_STREAM_TYPE + 1;
    public static final int SUPPORT = AUDIO_SLIDER_SHOW + 1;

    public final int[] gestureItems = {PADDING, MAP_UP_ICON, MAP_DOWN_ICON, MAP_LEFT_ICON,
            MAP_RIGHT_ICON, AD_FREE, SUPPORT, REVIEW, PADDING};

    public final int[] brightnessItems = {PADDING, SLIDER_DELTA, DISCRETE_BRIGHTNESS,
            SCREEN_DIMMER, USE_LOGARITHMIC_SCALE, SHOW_SLIDER, ADAPTIVE_BRIGHTNESS, ANIMATES_SLIDER,
            ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS, DOUBLE_SWIPE_SETTINGS, PADDING};

    public final int[] audioItems = {PADDING, AUDIO_DELTA, AUDIO_STREAM_TYPE, AUDIO_SLIDER_SHOW,
            PADDING};

    public final int[] popupItems = {PADDING, ENABLE_ACCESSIBILITY_BUTTON,
            ACCESSIBILITY_SINGLE_CLICK, ANIMATES_POPUP, ENABLE_WATCH_WINDOWS, ROTATION_LOCK,
            EXCLUDED_ROTATION_LOCK, POPUP_ACTION, PADDING};

    public final int[] appearanceItems = {PADDING, SLIDER_POSITION, SLIDER_DURATION,
            SLIDER_COLOR, WALLPAPER_PICKER, WALLPAPER_TRIGGER, PADDING};

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SLIDER_DELTA, SLIDER_POSITION, SLIDER_DURATION, SLIDER_COLOR, SCREEN_DIMMER,
            SHOW_SLIDER, USE_LOGARITHMIC_SCALE, ADAPTIVE_BRIGHTNESS, ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS,
            DOUBLE_SWIPE_SETTINGS, MAP_UP_ICON, MAP_DOWN_ICON, MAP_LEFT_ICON, MAP_RIGHT_ICON,
            AD_FREE, REVIEW, WALLPAPER_PICKER, WALLPAPER_TRIGGER, ROTATION_LOCK,
            EXCLUDED_ROTATION_LOCK, ENABLE_WATCH_WINDOWS, POPUP_ACTION, ENABLE_ACCESSIBILITY_BUTTON,
            ACCESSIBILITY_SINGLE_CLICK, ANIMATES_SLIDER, ANIMATES_POPUP, DISCRETE_BRIGHTNESS,
            AUDIO_DELTA, AUDIO_STREAM_TYPE, AUDIO_SLIDER_SHOW, SUPPORT})
    public @interface AdapterIndex {}

    private static final String RX_JAVA_LINK = "https://github.com/ReactiveX/RxJava";
    private static final String COLOR_PICKER_LINK = "https://github.com/QuadFlask/colorpicker";
    private static final String ANDROID_BOOTSTRAP_LINK = "https://github.com/tunjid/android-bootstrap";
    private static final String GET_SET_ICON_LINK = "http://www.myiconfinder.com/getseticons";
    private static final String IMAGE_CROPPER_LINK = "https://github.com/ArthurHub/Android-Image-Cropper";
    private static final String MATERIAL_DESIGN_ICONS_LINK = "https://materialdesignicons.com/";

    public final TextLink[] links;

    private State state;
    private final String[] quips;
    private final AtomicInteger quipCounter;
    private final CompositeDisposable disposable;
    private final PublishProcessor<State> stateProcessor;
    private final PublishProcessor<String> shillProcessor;
    private final Queue<Integer> permissionsQueue;

    public AppViewModel(@NonNull Application application) {
        super(application);

        state = new State(application);
        disposable = new CompositeDisposable();
        permissionsQueue = new ArrayDeque<>();
        quipCounter = new AtomicInteger(-1);

        stateProcessor = PublishProcessor.create();
        shillProcessor = PublishProcessor.create();

        quips = application.getResources().getStringArray(R.array.upsell_text);
        links = new TextLink[]{
                new TextLink(application.getString(R.string.get_set_icon), GET_SET_ICON_LINK),
                new TextLink(application.getString(R.string.rxjava), RX_JAVA_LINK),
                new TextLink(application.getString(R.string.color_picker), COLOR_PICKER_LINK),
                new TextLink(application.getString(R.string.image_cropper), IMAGE_CROPPER_LINK),
                new TextLink(application.getString(R.string.material_design_icons), MATERIAL_DESIGN_ICONS_LINK),
                new TextLink(application.getString(R.string.android_bootstrap), ANDROID_BOOTSTRAP_LINK)};
    }

    @Override protected void onCleared() {
        super.onCleared();
        disposable.clear();
        permissionsQueue.clear();
    }

    public Flowable<State> state() {
        return stateProcessor;
    }

    public Flowable<String> shill() {
        disposable.clear();
        startShilling();
        return shillProcessor;
    }

    public void shillMoar() {
        shillProcessor.onNext(getNextQuip());
    }

    public void calmIt() {
        disposable.clear();
    }

    public void checkPermissions() {
        if (permissionsQueue.isEmpty()) stateProcessor.onNext(state = state.visibility(false));
        else onPermissionAdded();
    }

    public void requestPermission(@MainActivity.PermissionRequest int permission) {
        if (!permissionsQueue.contains(permission)) permissionsQueue.add(permission);
        onPermissionAdded();
    }

    public void onPermissionClicked(Consumer<Integer> consumer) {
        if (permissionsQueue.isEmpty()) checkPermissions();
        else consumer.accept(permissionsQueue.peek());
    }

    public Optional<Integer> onPermissionChange(int requestCode) {
        boolean shouldRemove;
        Optional<Integer> result;
        switch (requestCode) {
            case STORAGE_CODE:
                result = Optional.of((shouldRemove = App.hasStoragePermission())
                        ? R.string.storage_permission_granted
                        : R.string.storage_permission_denied);
                break;
            case SETTINGS_CODE:
                result = Optional.of((shouldRemove = App.canWriteToSettings())
                        ? R.string.settings_permission_granted
                        : R.string.settings_permission_denied);
                break;
            case ACCESSIBILITY_CODE:
                result = Optional.of((shouldRemove = App.accessibilityServiceEnabled())
                        ? R.string.accessibility_permission_granted
                        : R.string.accessibility_permission_denied);
                break;
            case DO_NOT_DISTURB_CODE:
                result = Optional.of((shouldRemove = App.hasDoNotDisturbAccess())
                        ? R.string.do_not_disturb_permission_granted
                        : R.string.do_not_disturb_permission_denied);
                break;
            default:
                return Optional.empty();
        }
        if (shouldRemove) permissionsQueue.remove(requestCode);
        checkPermissions();
        return result;
    }

    public Optional<Integer> updateBottomNav(int hash) {
        permissionsQueue.clear();
        int id = hash == Arrays.hashCode(gestureItems)
                ? R.id.action_directions
                : hash == Arrays.hashCode(brightnessItems)
                ? R.id.action_slider
                : hash == Arrays.hashCode(audioItems)
                ? R.id.action_audio
                : hash == Arrays.hashCode(popupItems)
                ? R.id.action_accessibility_popup
                : hash == Arrays.hashCode(appearanceItems)
                ? R.id.action_wallpaper
                : 0;

        return id == 0 ? Optional.empty() : Optional.of(id);
    }

    private void onPermissionAdded() {
        if (permissionsQueue.isEmpty()) return;
        int permissionRequest = permissionsQueue.peek();

        switch (permissionRequest) {
            default:
                return;
            case DO_NOT_DISTURB_CODE:
                state = state.glyph(R.string.enable_do_not_disturb, R.drawable.ic_volume_loud_24dp);
                break;
            case ACCESSIBILITY_CODE:
                state = state.glyph(R.string.enable_accessibility, R.drawable.ic_human_24dp);
                break;
            case SETTINGS_CODE:
                state = state.glyph(R.string.enable_write_settings, R.drawable.ic_settings_white_24dp);
                break;
            case STORAGE_CODE:
                state = state.glyph(R.string.enable_storage_settings, R.drawable.ic_storage_24dp);
                break;
        }

        stateProcessor.onNext(state);
    }

    private void startShilling() {
        disposable.add(Flowable.interval(10, TimeUnit.SECONDS)
                .map(__ -> getNextQuip())
                .observeOn(mainThread())
                .subscribe(shillProcessor::onNext, Throwable::printStackTrace));
    }

    private String getNextQuip() {
        if (quipCounter.incrementAndGet() >= quips.length) quipCounter.set(0);
        return quips[quipCounter.get()];
    }
}
