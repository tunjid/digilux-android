package com.tunjid.fingergestures;


import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tunjid.fingergestures.App.requireApp;

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
       return getSet(preferenceName).stream()
                .map(stringMapper)
                .filter(Objects::nonNull)
                .sorted(sorter)
                .map(objectMapper)
                .collect(Collectors.toList());
    }

    public Set<String> getSet(String preferencesName) {
        Set<String> defaultValue = new HashSet<>();
        return requireApp(app -> new HashSet<>(app.getPreferences().getStringSet(preferencesName, defaultValue)), defaultValue);
    }

    private void saveSet(Set<String> set, String preferencesName) {
        requireApp(app -> app.getPreferences().edit().putStringSet(preferencesName, set).apply());
    }
}
