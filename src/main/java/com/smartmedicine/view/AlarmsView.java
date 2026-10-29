package com.smartmedicine.view;

import com.smartmedicine.model.DoseRecord;
import com.smartmedicine.model.Medicine;
import com.smartmedicine.model.User;
import com.smartmedicine.service.AppContext;
import com.smartmedicine.service.DoseStatusCalculator;
import com.smartmedicine.util.Config;
import java.time.LocalDateTime;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Active alarms (upcoming and due doses of today) plus the configured reminder times. */
public final class AlarmsView implements Page {
    private final User user;
    private final VBox root = new VBox(14);
    private final DoseTable activeTable;
    private final ListView<String> configured = new ListView<>();

    public AlarmsView(User user) {
        this.user = user;
        root.getStyleClass().add("page");
        root.setPadding(new Insets(24));
        Label info = new Label("Reminders ring while this application is running (it must stay open; minimizing is fine). "
                + "A dose stays Pending when you ignore the alarm and becomes Missed " + Config.GRACE_MINUTES
                + " minutes after its scheduled time. An alarm never marks a dose as Taken by itself.");
        info.setWrapText(true);
        VBox infoBox = new VBox(info);
        infoBox.getStyleClass().add("info-box");

        Label t1 = new Label("Today's active alarms (upcoming and due)");
        t1.getStyleClass().add("section-title");
        activeTable = new DoseTable(false, true, false, d -> mark(d, true), d -> mark(d, false));
        activeTable.node().setPrefHeight(260);

        Label t2 = new Label("Configured reminder times");
        t2.getStyleClass().add("section-title");
        configured.setPlaceholder(new Label("No active medicines."));
        VBox.setVgrow(configured, Priority.ALWAYS);

        root.getChildren().addAll(UiUtil.title("Alarms & Reminders"), infoBox, t1, activeTable.node(), t2, configured);
    }

    @Override public Parent root() { return root; }

    @Override public void refresh() {
        UiUtil.async(() -> {
            List<DoseRecord> today = AppContext.doses().getTodayDoses(user.id());
            List<Medicine> meds = AppContext.medicines().list(user.id());
            return new Object[] {today, meds};
        }, data -> {
            @SuppressWarnings("unchecked") List<DoseRecord> today = (List<DoseRecord>) data[0];
            @SuppressWarnings("unchecked") List<Medicine> meds = (List<Medicine>) data[1];
            LocalDateTime now = LocalDateTime.now();
            activeTable.setItems(today.stream()
                    .filter(d -> DoseStatusCalculator.effective(d, now, Config.GRACE_MINUTES).isPending()).toList());
            configured.setItems(FXCollections.observableArrayList(meds.stream().filter(Medicine::active)
                    .map(m -> "\u23F0 " + m.name() + " (" + m.dosage() + ")  \u2192  " + m.timesLabel()).toList()));
        });
    }

    private void mark(DoseRecord dose, boolean taken) {
        UiUtil.async(() -> {
            if (taken) AppContext.doses().markTaken(user.id(), dose.id());
            else AppContext.doses().markMissed(user.id(), dose.id());
            return Boolean.TRUE;
        }, ok -> { refresh(); AppContext.reminder().reschedule(); });
    }
}
