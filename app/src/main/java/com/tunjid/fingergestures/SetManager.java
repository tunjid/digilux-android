package com.tunjid.fingergestures;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SetManager<T> {

    private final App app;
    private final Comparator<T> sorter;
    private final Function<String, T> stringMapper;
    private final Function<T, String> objectMapper;
    private final Function<String, Boolean> addFilter;
    private final Map<String, List<String>> listMap;

    public SetManager(Comparator<T> sorter, Function<String, Boolean> addFilter,
                      Function<String, T> stringMapper, Function<T, String> objectMapper) {

        app = App.getInstance();
        listMap = new HashMap<>();

        this.sorter = sorter;
        this.addFilter = addFilter;
        this.stringMapper = stringMapper;
        this.objectMapper = objectMapper;
    }

    public boolean addToSet(String value, String preferencesName) {
        Set<String> set = getSet(preferencesName);

        if (!addFilter.apply(preferencesName)) return false;

        set.add(value);

        saveSet(set, preferencesName);
        resetList(preferencesName);

        return true;
    }

    public void removeFromSet(String packageName, String preferencesName) {
        Set<String> set = getSet(preferencesName);
        set.remove(packageName);

        saveSet(set, preferencesName);
        resetList(preferencesName);
    }

    public List<String> getList(String preferenceName) {
        if (!listMap.containsKey(preferenceName)) resetList(preferenceName);
        return listMap.get(preferenceName);
    }

    public Set<String> getSet(String preferencesName) {
        return new HashSet<>(app.getPreferences().getStringSet(preferencesName, new HashSet<>()));
    }

    private void saveSet(Set<String> set, String preferencesName) {
        app.getPreferences().edit().putStringSet(preferencesName, set).apply();
    }

    private void resetList(String preferencesName) {
        List<String> list = listMap.computeIfAbsent(preferencesName, k -> new ArrayList<>());

        list.clear();
        list.addAll(getSet(preferencesName).stream()
                .map(stringMapper)
                .filter(Objects::nonNull)
                .sorted(sorter)
                .map(objectMapper)
                .collect(Collectors.toList()));
    }
}
