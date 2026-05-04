package org.psz80.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("GrupoVMPS - UI Mock");

        // instanciar o controller
        Controller controller = new Controller();
        Scene scene = new Scene(controller.getRoot(), 1000, 700);
        try {
            scene.getStylesheets().add(getClass().getResource("/styles/ui.css").toExternalForm());
        } catch (NullPointerException ex) {
            System.err.println("UI stylesheet not found: styles/ui.css");
        }
        stage.setScene(scene);
        stage.setMinWidth(1280);
        stage.setMinHeight(720);

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}