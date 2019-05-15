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

package com.tunjid.fingergestures;


import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tunjid.fingergestures.App.withApp;

public class SetManager<T> {

    private final Comparator<T> sorter;
    private final Function<String, T> stringMapper;
    private final Function<T, String> objectMapper;
    private final Function<String, Boolean> addFilter;

    public SetManager(Comparator<T> sorter,
                      Function<String, Boolean> addFilter,
                      Function<String, T> stringMapper,
                      Function<T, String> objectMapper) {
        this.sorter = sorter;
        this.addFilter = addFilter;
        this.stringMapper = stringMapper;
        this.objectMapper = objectMapper;
    }

    public boolean addToSet(String value, String preferencesName) {
        if (!addFilter.apply(preferencesName)) return false;

        Set<String> set = getSet(preferencesName);
        set.add(value);
        saveSet(set, preferencesName);

        return true;
    }

    public void removeFromSet(String packageName, String preferencesName) {
        Set<String> set = getSet(preferencesName);
        set.remove(packageName);
        saveSet(set, preferencesName);
    }

    public List<String> getList(String preferenceName) {
       return stream(preferenceName).collect(Collectors.toList());
    }

    public List<T> getItems(String preferenceName) {
        return stream(preferenceName).map(stringMapper).collect(Collectors.toList());
    }

    private Stream<String> stream(String preferenceName) {
        return getSet(preferenceName)
                .stream()
                .map(stringMapper)
                .filter(Objects::nonNull)
                .sorted(sorter)
                .map(objectMapper);
    }

    public Set<String> getSet(String preferencesName) {
        Set<String> defaultValue = new HashSet<>();
        return App.transformApp(app -> new HashSet<>(app.getPreferences().getStringSet(preferencesName, defaultValue)), defaultValue);
    }

    private void saveSet(Set<String> set, String preferencesName) {
        withApp(app -> app.getPreferences().edit().putStringSet(preferencesName, set).apply());
    }
}
