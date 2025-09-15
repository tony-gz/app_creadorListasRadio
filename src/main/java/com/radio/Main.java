package com.radio;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/main_view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600); // tamaño inicial
        stage.setTitle("Generador de Listas ZaraRadio");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args); // Lanza la aplicación JavaFX
    }
}
