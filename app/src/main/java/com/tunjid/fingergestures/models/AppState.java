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
