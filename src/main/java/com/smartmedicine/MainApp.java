package com.smartmedicine;

import com.smartmedicine.controller.AppController;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        new AppController(stage).showLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
