package com.smartmedicine.view;

import com.smartmedicine.controller.NavigationController;
import com.smartmedicine.controller.NavigationController.Target;
import com.smartmedicine.model.DoseRecord;
import com.smartmedicine.model.User;
import com.smartmedicine.service.AppContext;
import java.util.EnumMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Main window: left sidebar navigation + page area. Starts the reminder scheduler for the logged-in user. */
public final class MainView {
    private final BorderPane root = new BorderPane();
    private final User user;
    private final NavigationController nav = new NavigationController(root);
    private final Map<Target, Button> buttons = new EnumMap<>(Target.class);

    public MainView(User user, Runnable onLogout) {
        this.user = user;
        nav.register(Target.HOME, new DashboardView(user, nav));
        nav.register(Target.ADD_MEDICINE, new AddMedicineView(user));
        nav.register(Target.ALARMS, new AlarmsView(user));
        nav.register(Target.HISTORY, new HistoryView(user));

        Label brand = new Label("\uD83D\uDC8A Smart Medicine");
        brand.getStyleClass().add("brand");
        Label who = new Label("\uD83D\uDC64 " + user.fullName());
        who.setStyle("-fx-text-fill: #99f6e4;");
        who.setWrapText(true);
        VBox side = new VBox(8, brand, who);
        side.getStyleClass().add("sidebar");
        side.setPrefWidth(210);
        side.setAlignment(Pos.TOP_LEFT);
        side.setPadding(new Insets(20, 12, 20, 12));
        addNav(side, Target.HOME, "\uD83C\uDFE0  Home");
        addNav(side, Target.ADD_MEDICINE, "\u2795  Add Medicine");
        addNav(side, Target.ALARMS, "\u23F0  Alarms");
        addNav(side, Target.HISTORY, "\uD83D\uDCCB  History");
        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        Button logout = new Button("\uD83D\uDEAA  Logout");
        logout.getStyleClass().add("nav-btn");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(e -> onLogout.run());
        side.getChildren().addAll(grow, logout);

        root.setLeft(side);
        nav.setOnChange(t -> buttons.forEach((k, b) -> {
            b.getStyleClass().remove("nav-btn-active");
            if (k == t) b.getStyleClass().add("nav-btn-active");
        }));
        nav.navigate(Target.HOME);
        AppContext.reminder().start(user.id(), this::onAlarm, nav::refreshCurrent);
    }

    public Parent root() { return root; }

    private void addNav(VBox side, Target target, String text) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-btn");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> nav.navigate(target));
        buttons.put(target, b);
        side.getChildren().add(b);
    }

    private void onAlarm(DoseRecord dose) {
        AlarmDialog.show(root.getScene() == null ? null : root.getScene().getWindow(), dose, () ->
                UiUtil.async(() -> {
                    AppContext.doses().markTaken(user.id(), dose.id());
                    return Boolean.TRUE;
                }, ok -> { nav.refreshCurrent(); AppContext.reminder().reschedule(); }));
    }
}
