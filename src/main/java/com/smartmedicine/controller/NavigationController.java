package com.smartmedicine.controller;

import com.smartmedicine.view.Page;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import javafx.scene.layout.BorderPane;

/** Switches the page shown in the center of the main window. */
public final class NavigationController {
    public enum Target { HOME, ADD_MEDICINE, ALARMS, HISTORY }

    private final BorderPane host;
    private final Map<Target, Page> pages = new EnumMap<>(Target.class);
    private Target current;
    private Consumer<Target> onChange = t -> { };

    public NavigationController(BorderPane host) { this.host = host; }

    public void register(Target target, Page page) { pages.put(target, page); }
    public void setOnChange(Consumer<Target> listener) { this.onChange = listener; }

    public void navigate(Target target) {
        current = target;
        Page page = pages.get(target);
        host.setCenter(page.root());
        page.refresh();
        onChange.accept(target);
    }

    /** Reloads the visible page (used by the reminder scheduler and after changes). */
    public void refreshCurrent() {
        if (current != null) pages.get(current).refresh();
    }
}
