package com.smartmedicine.view;

import com.smartmedicine.model.DoseRecord;
import com.smartmedicine.util.DateTimeUtil;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.stage.Window;

/** The popup shown when a reminder is due. Must be called on the JavaFX Application Thread. */
public final class AlarmDialog {
    private AlarmDialog() { }

    public static void show(Window owner, DoseRecord dose, Runnable onTaken) {
        try { java.awt.Toolkit.getDefaultToolkit().beep(); } catch (Throwable ignored) { /* sound is optional */ }
        ButtonType taken = new ButtonType("Mark as Taken", ButtonBar.ButtonData.OK_DONE);
        ButtonType later = new ButtonType("Keep Pending", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.getButtonTypes().setAll(taken, later);
        alert.setTitle("Medicine Reminder");
        alert.setHeaderText("\u23F0 Time to take " + dose.medicineName());
        alert.setContentText("Dosage: " + dose.dosage() + "\nScheduled: " + DateTimeUtil.formatTime(dose.scheduledTime())
                + "\n\nIf you choose 'Keep Pending' the dose stays pending until you mark it.");
        if (owner != null) alert.initOwner(owner);
        alert.setOnShown(e -> ((Stage) alert.getDialogPane().getScene().getWindow()).setAlwaysOnTop(true));
        alert.showAndWait().ifPresent(result -> { if (result == taken) onTaken.run(); });
    }
}
