package net.zomis.rnddb.main;
	
import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.apache.log4j.PropertyConfigurator;


public class Main extends Application {
    @Override
    public void start(final Stage primaryStage) throws IOException {
        primaryStage.setTitle("Rocks'n'Diamonds Database");
        Parent root = FXMLLoader.load(MainController.class.getResource("rnddbStart.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }
    
	public static void main(String[] args) {
		PropertyConfigurator.configure(Main.class.getResource("log4j.properties"));
		launch(args);
	}
}
