package com.smartmedicine.view;

import com.smartmedicine.model.DisplayStatus;
import com.smartmedicine.model.DoseRecord;
import com.smartmedicine.service.DoseStatusCalculator;
import com.smartmedicine.util.Config;
import com.smartmedicine.util.DateTimeUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Reusable table of dose records with colored status badges and optional action buttons. */
public final class DoseTable {
    private final TableView<DoseRecord> table = new TableView<>();
    private final boolean historyMode;

    public DoseTable(boolean showDate, boolean showActions, boolean historyMode,
                     Consumer<DoseRecord> onTaken, Consumer<DoseRecord> onMissed) {
        this.historyMode = historyMode;
        if (showDate) addText("Date", d -> DateTimeUtil.formatDate(d.scheduledDate()));
        addText("Time", d -> DateTimeUtil.formatTime(d.scheduledTime()));
        addText("Medicine", DoseRecord::medicineName);
        addText("Dosage", DoseRecord::dosage);
        table.getColumns().add(statusColumn());
        if (showActions) table.getColumns().add(actionColumn(onTaken, onMissed));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No doses to show."));
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    public TableView<DoseRecord> node() { return table; }

    public void setItems(List<DoseRecord> items) { table.setItems(FXCollections.observableArrayList(items)); }

    private void addText(String title, Function<DoseRecord, String> getter) {
        TableColumn<DoseRecord, String> col = new TableColumn<>(title);
        col.setSortable(false);
        col.setCellValueFactory(cd -> new ReadOnlyStringWrapper(getter.apply(cd.getValue())));
        table.getColumns().add(col);
    }

    private TableColumn<DoseRecord, DoseRecord> statusColumn() {
        TableColumn<DoseRecord, DoseRecord> col = new TableColumn<>("Status");
        col.setSortable(false);
        col.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(DoseRecord d, boolean empty) {
                super.updateItem(d, empty);
                if (empty || d == null) { setGraphic(null); return; }
                DisplayStatus s = DoseStatusCalculator.effective(d, LocalDateTime.now(), Config.GRACE_MINUTES);
                Label badge = new Label(historyMode ? s.historyLabel() : s.label());
                badge.getStyleClass().addAll("badge", historyMode ? s.historyCssClass() : s.cssClass());
                setGraphic(badge);
            }
        });
        return col;
    }

    private TableColumn<DoseRecord, DoseRecord> actionColumn(Consumer<DoseRecord> onTaken, Consumer<DoseRecord> onMissed) {
        TableColumn<DoseRecord, DoseRecord> col = new TableColumn<>("Actions");
        col.setSortable(false);
        col.setMinWidth(250);
        col.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        col.setCellFactory(c -> new TableCell<>() {
            private final Button taken = new Button("Mark as Taken");
            private final Button missed = new Button("Mark Missed");
            private final HBox box = new HBox(6, taken, missed);
            {
                taken.getStyleClass().add("btn-success");
                missed.getStyleClass().add("btn-danger");
                taken.setOnAction(e -> onTaken.accept(getItem()));
                missed.setOnAction(e -> onMissed.accept(getItem()));
            }
            @Override protected void updateItem(DoseRecord d, boolean empty) {
                super.updateItem(d, empty);
                if (empty || d == null) { setGraphic(null); return; }
                DisplayStatus s = DoseStatusCalculator.effective(d, LocalDateTime.now(), Config.GRACE_MINUTES);
                taken.setDisable(s == DisplayStatus.TAKEN);
                missed.setDisable(s == DisplayStatus.MISSED || s == DisplayStatus.UPCOMING);
                setGraphic(box);
            }
        });
        return col;
    }
}
