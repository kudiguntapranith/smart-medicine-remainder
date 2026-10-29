package com.smartmedicine.view;

import com.smartmedicine.model.Frequency;
import com.smartmedicine.model.Medicine;
import com.smartmedicine.model.User;
import com.smartmedicine.service.AppContext;
import com.smartmedicine.util.DateTimeUtil;
import com.smartmedicine.util.ValidationException;
import com.smartmedicine.util.Validator;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Add / edit form on the left, list of saved medicines (edit, deactivate, reactivate) on the right. */
public final class AddMedicineView implements Page {
    private final User user;
    private final HBox root = new HBox(20);
    private final Label formTitle = new Label("Add Medicine");
    private final TextField name = new TextField();
    private final TextField dosage = new TextField();
    private final ComboBox<Frequency> frequency = new ComboBox<>(FXCollections.observableArrayList(Frequency.values()));
    private final ObservableList<String> times = FXCollections.observableArrayList();
    private final ListView<String> timesList = new ListView<>(times);
    private final TextField timeInput = new TextField();
    private final DatePicker start = new DatePicker(LocalDate.now());
    private final DatePicker end = new DatePicker();
    private final CheckBox ongoing = new CheckBox("Ongoing (no end date)");
    private final TextArea notes = new TextArea();
    private final Label message = new Label();
    private final Button save = new Button("Save Medicine");
    private final TableView<Medicine> table = new TableView<>();
    private final Button toggleActive = new Button("Deactivate");
    private Long editingId;
    private boolean loading;

    public AddMedicineView(User user) {
        this.user = user;
        root.getStyleClass().add("page");
        root.setPadding(new Insets(24));
        root.getChildren().addAll(buildForm(), buildList());
        setDefaultTimes(Frequency.ONCE_DAILY);
        frequency.setValue(Frequency.ONCE_DAILY);
    }

    @Override public Parent root() { return root; }

    @Override public void refresh() { reloadMedicines(); }

    private VBox buildForm() {
        formTitle.getStyleClass().add("page-title");
        name.setPromptText("e.g. Paracetamol");
        dosage.setPromptText("e.g. 1 tablet or 5 ml");
        frequency.setMaxWidth(Double.MAX_VALUE);
        frequency.valueProperty().addListener((o, a, f) -> { if (!loading && f != null) setDefaultTimes(f); });
        timesList.setPrefHeight(100);
        timeInput.setPromptText("HH:mm (24-hour), e.g. 08:30");
        Button addTime = new Button("Add time");
        addTime.getStyleClass().add("btn-secondary");
        addTime.setOnAction(e -> addTime());
        timeInput.setOnAction(e -> addTime());
        Button removeTime = new Button("Remove selected");
        removeTime.getStyleClass().add("btn-secondary");
        removeTime.setOnAction(e -> {
            String sel = timesList.getSelectionModel().getSelectedItem();
            if (sel != null) times.remove(sel);
        });
        ongoing.selectedProperty().addListener((o, a, b) -> { end.setDisable(b); if (b) end.setValue(null); });
        notes.setPromptText("Optional notes (e.g. after meals)");
        notes.setPrefRowCount(3);
        message.setWrapText(true);

        save.getStyleClass().add("btn-primary");
        save.setOnAction(e -> save());
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("btn-secondary");
        cancel.setOnAction(e -> clearForm());

        VBox form = new VBox(8, formTitle,
                new Label("Medicine name *"), name, new Label("Dosage *"), dosage,
                new Label("Frequency *"), frequency,
                new Label("Reminder times * (24-hour)"), timesList,
                new HBox(8, timeInput, addTime, removeTime),
                new HBox(12, new VBox(4, new Label("Start date *"), start), new VBox(4, new Label("End date"), end)),
                ongoing, new Label("Notes"), notes, message, new HBox(10, save, cancel));
        form.getStyleClass().add("card");
        form.setPrefWidth(460);
        form.setMinWidth(430);
        return form;
    }

    private VBox buildList() {
        Label title = new Label("My medicines");
        title.getStyleClass().add("section-title");
        addColumn("Name", Medicine::name);
        addColumn("Dosage", Medicine::dosage);
        addColumn("Times", Medicine::timesLabel);
        addColumn("Period", Medicine::periodLabel);
        addColumn("Status", m -> m.active() ? "Active" : "Inactive");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No medicines yet. Add your first one on the left."));
        VBox.setVgrow(table, Priority.ALWAYS);

        Button edit = new Button("Edit selected");
        edit.getStyleClass().add("btn-primary");
        edit.setOnAction(e -> {
            Medicine m = table.getSelectionModel().getSelectedItem();
            if (m == null) UiUtil.info("Select a medicine first."); else loadForEdit(m);
        });
        toggleActive.getStyleClass().add("btn-danger");
        toggleActive.setOnAction(e -> toggleActive());
        table.getSelectionModel().selectedItemProperty().addListener((o, a, m) ->
                toggleActive.setText(m != null && !m.active() ? "Activate" : "Deactivate / Delete"));
        toggleActive.setText("Deactivate / Delete");
        Label hint = new Label("Deleting a medicine deactivates it: reminders stop, but past history is kept.");
        hint.getStyleClass().add("subtitle");
        hint.setWrapText(true);

        VBox box = new VBox(10, title, table, new HBox(10, edit, toggleActive), hint);
        box.getStyleClass().add("card");
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private void addColumn(String title, java.util.function.Function<Medicine, String> getter) {
        TableColumn<Medicine, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new ReadOnlyStringWrapper(getter.apply(cd.getValue())));
        table.getColumns().add(c);
    }

    // ---------- form logic ----------

    private void setDefaultTimes(Frequency f) {
        if (f == Frequency.CUSTOM) return;
        times.setAll(f.defaultTimes().stream().map(DateTimeUtil::formatTime24).toList());
    }

    private void addTime() {
        try {
            String t = DateTimeUtil.formatTime24(Validator.parseTime(timeInput.getText()));
            if (times.contains(t)) throw new ValidationException("That time is already in the list.");
            times.add(t);
            FXCollections.sort(times);
            timeInput.clear();
            message.setText("");
        } catch (ValidationException e) {
            showError(e.getMessage());
        }
    }

    private void save() {
        List<LocalTime> parsed = new ArrayList<>();
        try {
            for (String t : times) parsed.add(Validator.parseTime(t));
        } catch (ValidationException ex) {
            showError(ex.getMessage());
            return;
        }
        String n = name.getText(), d = dosage.getText(), nt = notes.getText();
        Frequency f = frequency.getValue();
        LocalDate s = start.getValue(), e = ongoing.isSelected() ? null : end.getValue();
        Long id = editingId;
        save.setDisable(true);
        UiUtil.async(() -> {
            if (id == null) AppContext.medicines().add(user.id(), n, d, f, s, e, nt, parsed);
            else AppContext.medicines().update(user.id(), id, n, d, f, s, e, nt, parsed);
            return Boolean.TRUE;
        }, ok -> {
            save.setDisable(false);
            clearForm();
            message.getStyleClass().remove("error-label");
            message.setText("Saved. Reminders are scheduled.");
            reloadMedicines();
        }, t -> {
            save.setDisable(false);
            if (t instanceof ValidationException) showError(t.getMessage()); else UiUtil.error(t);
        });
    }

    private void showError(String text) {
        if (!message.getStyleClass().contains("error-label")) message.getStyleClass().add("error-label");
        message.setText(text);
    }

    private void loadForEdit(Medicine m) {
        loading = true;
        editingId = m.id();
        formTitle.setText("Edit Medicine");
        save.setText("Update Medicine");
        name.setText(m.name());
        dosage.setText(m.dosage());
        frequency.setValue(m.frequency());
        times.setAll(m.times().stream().map(DateTimeUtil::formatTime24).toList());
        start.setValue(m.startDate());
        ongoing.setSelected(m.endDate() == null);
        end.setValue(m.endDate());
        notes.setText(m.notes() == null ? "" : m.notes());
        message.setText("");
        loading = false;
    }

    private void clearForm() {
        loading = true;
        editingId = null;
        formTitle.setText("Add Medicine");
        save.setText("Save Medicine");
        name.clear();
        dosage.clear();
        frequency.setValue(Frequency.ONCE_DAILY);
        setDefaultTimes(Frequency.ONCE_DAILY);
        start.setValue(LocalDate.now());
        ongoing.setSelected(false);
        end.setValue(null);
        notes.clear();
        timeInput.clear();
        message.setText("");
        loading = false;
    }

    private void reloadMedicines() {
        UiUtil.async(() -> AppContext.medicines().list(user.id()),
                list -> table.setItems(FXCollections.observableArrayList(list)));
    }

    private void toggleActive() {
        Medicine m = table.getSelectionModel().getSelectedItem();
        if (m == null) { UiUtil.info("Select a medicine first."); return; }
        boolean activate = !m.active();
        UiUtil.async(() -> {
            if (activate) AppContext.medicines().reactivate(user.id(), m.id());
            else AppContext.medicines().deactivate(user.id(), m.id());
            return Boolean.TRUE;
        }, ok -> { if (editingId != null && editingId == m.id()) clearForm(); reloadMedicines(); });
    }
}
