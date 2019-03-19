package com.tunjid.fingergestures.models;

import android.app.Application;
import android.content.pm.ApplicationInfo;

import com.tunjid.fingergestures.R;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class AppState {

    private static final String RX_JAVA_LINK = "https://github.com/ReactiveX/RxJava";
    private static final String COLOR_PICKER_LINK = "https://github.com/QuadFlask/colorpicker";
    private static final String ANDROID_BOOTSTRAP_LINK = "https://github.com/tunjid/android-bootstrap";
    private static final String GET_SET_ICON_LINK = "http://www.myiconfinder.com/getseticons";
    private static final String IMAGE_CROPPER_LINK = "https://github.com/ArthurHub/Android-Image-Cropper";
    private static final String MATERIAL_DESIGN_ICONS_LINK = "https://materialdesignicons.com/";

    public final TextLink[] links;
    public final List<String> brightnessValues;
    public final List<Integer> popUpActions;
    public final List<Integer> availableActions;
    public final List<ApplicationInfo> installedApps;
    public final List<ApplicationInfo> rotationApps;
    public final List<ApplicationInfo> excludedRotationApps;
    public final Queue<Integer> permissionsQueue;

    public AppState(Application application) {
        brightnessValues = new ArrayList<>();

        popUpActions = new ArrayList<>();
        availableActions = new ArrayList<>();

        installedApps = new ArrayList<>();
        rotationApps = new ArrayList<>();
        excludedRotationApps = new ArrayList<>();

        permissionsQueue = new ArrayDeque<>();

        links = new TextLink[]{
                new TextLink(application.getString(R.string.get_set_icon), GET_SET_ICON_LINK),
                new TextLink(application.getString(R.string.rxjava), RX_JAVA_LINK),
                new TextLink(application.getString(R.string.color_picker), COLOR_PICKER_LINK),
                new TextLink(application.getString(R.string.image_cropper), IMAGE_CROPPER_LINK),
                new TextLink(application.getString(R.string.material_design_icons), MATERIAL_DESIGN_ICONS_LINK),
                new TextLink(application.getString(R.string.android_bootstrap), ANDROID_BOOTSTRAP_LINK)};
    }
}
