package com.smartmedicine.view;

import com.smartmedicine.model.DoseRecord;
import com.smartmedicine.model.DoseStatus;
import com.smartmedicine.model.Frequency;
import com.smartmedicine.model.Medicine;
import com.smartmedicine.model.User;
import com.smartmedicine.service.AppContext;
import com.smartmedicine.service.DaySummary;
import com.smartmedicine.service.DoseService;
import com.smartmedicine.util.Config;
import com.smartmedicine.util.DateTimeUtil;
import com.smartmedicine.util.ValidationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** Last 30 days of doses with filters (date range, medicine, status) and a daily summary. */
public final class HistoryView implements Page {
    private static final Medicine ALL_MEDICINES = new Medicine(0, 0, "All medicines", "", Frequency.CUSTOM,
            LocalDate.now(), null, null, true, List.of());
    private static final String ALL_STATUS = "All statuses";

    private final User user;
    private final VBox root = new VBox(14);
    private final DatePicker from = new DatePicker();
    private final DatePicker to = new DatePicker();
    private final ComboBox<Medicine> medicine = new ComboBox<>();
    private final ComboBox<String> status = new ComboBox<>(
            FXCollections.observableArrayList(ALL_STATUS, "Taken", "Missed", "Pending"));
    private final Label error = new Label();
    private final Label counts = new Label();
    private final DoseTable table = new DoseTable(true, false, true, null, null);
    private final TableView<DaySummary> summary = new TableView<>();

    public HistoryView(User user) {
        this.user = user;
        root.getStyleClass().add("page");
        root.setPadding(new Insets(24));
        medicine.setConverter(new StringConverter<>() {
            @Override public String toString(Medicine m) { return m == null ? "" : m.name(); }
            @Override public Medicine fromString(String s) { return null; }
        });
        medicine.getItems().add(ALL_MEDICINES);
        medicine.setValue(ALL_MEDICINES);
        status.setValue(ALL_STATUS);
        resetDates();

        Button apply = new Button("Apply filters");
        apply.getStyleClass().add("btn-primary");
        apply.setOnAction(e -> load());
        Button reset = new Button("Reset");
        reset.getStyleClass().add("btn-secondary");
        reset.setOnAction(e -> { resetDates(); medicine.setValue(ALL_MEDICINES); status.setValue(ALL_STATUS); load(); });
        error.getStyleClass().add("error-label");

        HBox filters = new HBox(12, new VBox(4, new Label("From"), from), new VBox(4, new Label("To"), to),
                new VBox(4, new Label("Medicine"), medicine), new VBox(4, new Label("Status"), status),
                new VBox(4, new Label(" "), new HBox(8, apply, reset)));
        filters.getStyleClass().add("card");

        summary.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        summary.setPlaceholder(new Label("No data."));
        sumColumn("Date", s -> DateTimeUtil.formatDate(s.date()));
        sumColumn("Taken", s -> String.valueOf(s.taken()));
        sumColumn("Missed", s -> String.valueOf(s.missed()));
        sumColumn("Pending", s -> String.valueOf(s.pending()));
        VBox.setVgrow(summary, Priority.ALWAYS);

        Label t1 = new Label("Dose history (last " + Config.HISTORY_DAYS + " days)");
        t1.getStyleClass().add("section-title");
        VBox left = new VBox(8, t1, counts, table.node());
        HBox.setHgrow(left, Priority.ALWAYS);
        Label t2 = new Label("Daily summary");
        t2.getStyleClass().add("section-title");
        VBox right = new VBox(8, t2, summary);
        right.setPrefWidth(340);
        right.setMinWidth(300);
        HBox content = new HBox(16, left, right);
        VBox.setVgrow(content, Priority.ALWAYS);

        root.getChildren().addAll(UiUtil.title("Medicine History"), filters, error, content);
    }

    @Override public Parent root() { return root; }

    @Override public void refresh() {
        UiUtil.async(() -> AppContext.medicines().list(user.id()), meds -> {
            Medicine selected = medicine.getValue();
            List<Medicine> items = new ArrayList<>();
            items.add(ALL_MEDICINES);
            items.addAll(meds);
            medicine.setItems(FXCollections.observableArrayList(items));
            Medicine keep = items.stream().filter(m -> selected != null && m.id() == selected.id()).findFirst()
                    .orElse(ALL_MEDICINES);
            medicine.setValue(keep);
            load();
        });
    }

    private void load() {
        error.setText("");
        LocalDate f = from.getValue(), t = to.getValue();
        Medicine m = medicine.getValue();
        Long medicineId = (m == null || m.id() == 0) ? null : m.id();
        DoseStatus st = switch (String.valueOf(status.getValue())) {
            case "Taken" -> DoseStatus.TAKEN;
            case "Missed" -> DoseStatus.MISSED;
            case "Pending" -> DoseStatus.PENDING;
            default -> null;
        };
        UiUtil.async(() -> AppContext.doses().getHistory(user.id(), f, t, medicineId, st), this::render,
                ex -> { if (ex instanceof ValidationException) error.setText(ex.getMessage()); else UiUtil.error(ex); });
    }

    private void render(List<DoseRecord> records) {
        table.setItems(records);
        List<DaySummary> days = DoseService.summarize(records, LocalDateTime.now(), Config.GRACE_MINUTES);
        summary.setItems(FXCollections.observableArrayList(days));
        int taken = days.stream().mapToInt(DaySummary::taken).sum();
        int missed = days.stream().mapToInt(DaySummary::missed).sum();
        counts.setText(records.size() + " doses shown  |  Taken: " + taken + "  |  Missed: " + missed);
    }

    private void resetDates() {
        from.setValue(LocalDate.now().minusDays(Config.HISTORY_DAYS - 1L));
        to.setValue(LocalDate.now());
    }

    private void sumColumn(String title, java.util.function.Function<DaySummary, String> getter) {
        TableColumn<DaySummary, String> c = new TableColumn<>(title);
        c.setSortable(false);
        c.setCellValueFactory(cd -> new ReadOnlyStringWrapper(getter.apply(cd.getValue())));
        summary.getColumns().add(c);
    }
}
