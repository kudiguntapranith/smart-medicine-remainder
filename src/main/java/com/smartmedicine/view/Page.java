package com.smartmedicine.view;

import javafx.scene.Parent;

/** A screen that can be shown in the main window. refresh() reloads its data. */
public interface Page {
    Parent root();
    void refresh();
}
