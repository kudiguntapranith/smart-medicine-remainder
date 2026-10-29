package com.smartmedicine.view;

import com.smartmedicine.util.DataAccessException;
import com.smartmedicine.util.ValidationException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Small UI helpers shared by all screens. */
public final class UiUtil {
    private UiUtil() { }

    /** Runs {@code work} on a background thread; {@code onSuccess} runs on the JavaFX Application Thread. */
    public static <T> void async(Supplier<T> work, Consumer<T> onSuccess) {
        async(work, onSuccess, UiUtil::error);
    }

    public static <T> void async(Supplier<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override protected T call() { return work.get(); }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException()));
        Thread t = new Thread(task, "db-task");
        t.setDaemon(true);
        t.start();
    }

    public static void error(Throwable t) {
        String msg;
        if (t instanceof ValidationException || t instanceof DataAccessException) msg = t.getMessage();
        else msg = "Something went wrong: " + t.getMessage();
        runOnFx(() -> new Alert(Alert.AlertType.ERROR, msg).showAndWait());
    }

    public static void info(String message) {
        runOnFx(() -> new Alert(Alert.AlertType.INFORMATION, message).showAndWait());
    }

    public static void runOnFx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run(); else Platform.runLater(r);
    }

    /** White rounded "stat" card with a big value and a caption. */
    public static VBox statCard(String caption, Label value, String color) {
        value.getStyleClass().add("stat-value");
        value.setStyle("-fx-text-fill: " + color + ";");
        Label cap = new Label(caption);
        cap.getStyleClass().add("stat-label");
        VBox box = new VBox(4, value, cap);
        box.getStyleClass().add("card");
        box.setMinWidth(150);
        return box;
    }

    public static Label title(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("page-title");
        return l;
    }
}
