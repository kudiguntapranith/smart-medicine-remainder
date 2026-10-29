package com.smartmedicine.view;

import com.smartmedicine.controller.NavigationController;
import com.smartmedicine.model.DoseRecord;
import com.smartmedicine.model.User;
import com.smartmedicine.service.AppContext;
import com.smartmedicine.service.DashboardStats;
import com.smartmedicine.util.Config;
import com.smartmedicine.util.DateTimeUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Home dashboard: today's counters, next reminder and today's doses with Taken / Missed actions. */
public final class DashboardView implements Page {
    private final User user;
    private final VBox root = new VBox(16);
    private final Label total = new Label("0"), taken = new Label("0"), pending = new Label("0"), missed = new Label("0");
    private final Label next = new Label();
    private final DoseTable table;

    public DashboardView(User user, NavigationController nav) {
        this.user = user;
        root.getStyleClass().add("page");
        root.setPadding(new Insets(24));

        Label welcome = UiUtil.title("Welcome, " + user.fullName() + "!");
        Label date = new Label();
        date.getStyleClass().add("subtitle");
        date.setText("Today is " + DateTimeUtil.formatLongDate(LocalDate.now()));

        Button add = new Button("+ Add Medicine");
        add.getStyleClass().add("btn-primary");
        add.setOnAction(e -> nav.navigate(NavigationController.Target.ADD_MEDICINE));
        Button history = new Button("View History");
        history.getStyleClass().add("btn-secondary");
        history.setOnAction(e -> nav.navigate(NavigationController.Target.HISTORY));
        Button alarms = new Button("Alarms");
        alarms.getStyleClass().add("btn-secondary");
        alarms.setOnAction(e -> nav.navigate(NavigationController.Target.ALARMS));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(10, new VBox(2, welcome, date), spacer, add, alarms, history);
        top.setAlignment(Pos.CENTER_LEFT);

        HBox stats = new HBox(14,
                UiUtil.statCard("Scheduled today", total, "#0f172a"),
                UiUtil.statCard("Taken", taken, "#16a34a"),
                UiUtil.statCard("Pending", pending, "#d97706"),
                UiUtil.statCard("Missed", missed, "#dc2626"));

        next.getStyleClass().add("section-title");
        VBox nextBox = new VBox(next);
        nextBox.getStyleClass().add("info-box");

        Label listTitle = new Label("Today's medicines");
        listTitle.getStyleClass().add("section-title");
        table = new DoseTable(false, true, false, d -> mark(d, true), d -> mark(d, false));
        root.getChildren().addAll(top, stats, nextBox, listTitle, table.node());
    }

    @Override public Parent root() { return root; }

    @Override public void refresh() {
        UiUtil.async(() -> AppContext.doses().getTodayDoses(user.id()), this::render);
    }

    private void render(List<DoseRecord> doses) {
        DashboardStats s = DashboardStats.of(doses, LocalDateTime.now(), Config.GRACE_MINUTES);
        total.setText(String.valueOf(s.total()));
        taken.setText(String.valueOf(s.taken()));
        pending.setText(String.valueOf(s.pending()));
        missed.setText(String.valueOf(s.missed()));
        next.setText(s.next() == null ? "\u23F0 No more upcoming reminders today"
                : "\u23F0 Next reminder: " + s.next().medicineName() + " (" + s.next().dosage() + ") at "
                + DateTimeUtil.formatTime(s.next().scheduledTime()));
        table.setItems(doses);
    }

    private void mark(DoseRecord dose, boolean asTaken) {
        UiUtil.async(() -> {
            if (asTaken) AppContext.doses().markTaken(user.id(), dose.id());
            else AppContext.doses().markMissed(user.id(), dose.id());
            return Boolean.TRUE;
        }, ok -> { refresh(); AppContext.reminder().reschedule(); });
    }
}
