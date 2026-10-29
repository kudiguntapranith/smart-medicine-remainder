package com.smartmedicine.view;

import com.smartmedicine.model.User;
import com.smartmedicine.service.AppContext;
import com.smartmedicine.util.ValidationException;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** First screen: login, with a switch to registration. */
public final class LoginView {
    private final Consumer<User> onLoggedIn;
    private final StackPane root = new StackPane();
    private final Label heading = new Label();
    private final TextField username = new TextField();
    private final TextField fullName = new TextField();
    private final PasswordField password = new PasswordField();
    private final PasswordField confirm = new PasswordField();
    private final Label error = new Label();
    private final Button submit = new Button();
    private final Button toggle = new Button();
    private boolean registerMode;

    public LoginView(Consumer<User> onLoggedIn) {
        this.onLoggedIn = onLoggedIn;
        Label brand = new Label("\uD83D\uDC8A  Smart Medicine Reminder");
        brand.getStyleClass().add("page-title");
        Label sub = new Label("Never miss a dose.");
        sub.getStyleClass().add("subtitle");
        heading.getStyleClass().add("section-title");

        username.setPromptText("Username");
        fullName.setPromptText("Full name");
        password.setPromptText("Password");
        confirm.setPromptText("Confirm password");
        error.getStyleClass().add("error-label");
        error.setWrapText(true);
        submit.getStyleClass().add("btn-primary");
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setDefaultButton(true);
        submit.setOnAction(e -> submit());
        toggle.getStyleClass().add("link-btn");
        toggle.setOnAction(e -> setMode(!registerMode));

        VBox card = new VBox(12, brand, sub, heading, username, fullName, password, confirm, error, submit, toggle);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(400);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(32));
        root.getChildren().add(card);
        root.getStyleClass().add("login-bg");
        StackPane.setAlignment(card, Pos.CENTER);
        setMode(false);
    }

    public Parent root() { return root; }

    private void setMode(boolean register) {
        registerMode = register;
        heading.setText(register ? "Create your account" : "Sign in");
        submit.setText(register ? "Register" : "Login");
        toggle.setText(register ? "Already have an account? Login" : "New user? Register here");
        fullName.setVisible(register); fullName.setManaged(register);
        confirm.setVisible(register); confirm.setManaged(register);
        error.setText("");
        password.clear();
        confirm.clear();
    }

    private void submit() {
        error.setText("");
        submit.setDisable(true);
        String u = username.getText(), n = fullName.getText(), p = password.getText(), c = confirm.getText();
        boolean register = registerMode;
        UiUtil.async(() -> register ? AppContext.auth().register(u, n, p, c) : AppContext.auth().login(u, p),
                user -> { submit.setDisable(false); onLoggedIn.accept(user); },
                t -> {
                    submit.setDisable(false);
                    if (t instanceof ValidationException) error.setText(t.getMessage());
                    else error.setText(t.getMessage() == null ? "Unexpected error." : t.getMessage());
                });
    }
}
