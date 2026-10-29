package com.smartmedicine.controller;

import com.smartmedicine.model.User;
import com.smartmedicine.service.AppContext;
import com.smartmedicine.view.LoginView;
import com.smartmedicine.view.MainView;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Owns the window and switches between the login screen and the main application. */
public final class AppController {
    private final Stage stage;

    public AppController(Stage stage) {
        this.stage = stage;
        stage.setTitle("Smart Medicine Reminder");
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.setOnCloseRequest(e -> AppContext.reminder().stop());
    }

    public void showLogin() {
        AppContext.setUser(null);
        show(new LoginView(this::showMain).root());
    }

    public void showMain(User user) {
        AppContext.setUser(user);
        show(new MainView(user, this::logout).root());
    }

    private void logout() {
        AppContext.reminder().stop();
        showLogin();
    }

    private void show(Parent root) {
        double w = stage.getScene() == null ? 1150 : stage.getScene().getWidth();
        double h = stage.getScene() == null ? 740 : stage.getScene().getHeight();
        Scene scene = new Scene(root, w, h);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
}
