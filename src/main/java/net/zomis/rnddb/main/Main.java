package net.zomis.rnddb.main;
	
import java.io.File;
import java.io.IOException;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import javax.persistence.EntityManagerFactory;

import net.zomis.rnddb.host.RndDatabaseManager;
import net.zomis.rnddb.host.RndDbClient;
import net.zomis.rnddb.host.RndDbServer;
import net.zomis.rnddb.host.RndDbSource;
import net.zomis.rnddb.host.RootPathFinder;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public class Main extends Application {
	private static final int DEFAULT_PORT = 7332;
	private static final Logger logger = LogManager.getLogger(Main.class);
	private EntityManagerFactory emf;
	private RndDbSource db;

	@FXML
	private TextField serverAddress;
	private Stage	window;
	
	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("Rocks'n'Databases");
		Pane root;
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("rnddbStart.fxml"));
			loader.setController(this);
			root = (GridPane) loader.load();
		}
		catch (IOException e1) {
			throw new RuntimeException(e1);
		}
		
		Scene scene = new Scene(root,400,400);
		scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		primaryStage.setScene(scene);
		primaryStage.show();
		this.window = primaryStage;
		
		logger.info("Started Main Frame");
	}
	
	@FXML
	private void localOnly(ActionEvent event) {
		emf = DatabaseConfig.localhostEmbedded();
		db = new RndDatabaseManager(emf);
		OverviewController.start(db, db);
		window.close();
	}
	
	@FXML
	private void host(ActionEvent event) {
		try {
			emf = DatabaseConfig.fromFile(new File("connection.properties"));
			
			RedirectRootPathFinder changer = new RedirectRootPathFinder();
			db = new RndDbServer(DEFAULT_PORT, changer, new RndDatabaseManager(emf));
			OverviewController overview = OverviewController.start(db, db);
			changer.redirect = overview;
			window.close();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private class RedirectRootPathFinder implements RootPathFinder {
		RootPathFinder redirect;
		
		@Override
		public File getRootPath() {
			return redirect.getRootPath();
		}
	};
	
	@FXML
	private void connect(ActionEvent event) {
		emf = DatabaseConfig.localhostEmbedded();
		String address = serverAddress.getText();
		String[] split = address.split(":");
		int connectPort = DEFAULT_PORT;
		if (split[0].isEmpty()) {
			split[0] = "127.0.0.1";
		}
		if (split.length > 1) {
			connectPort = Integer.parseInt(split[1]);
		}
		try {
			RndDatabaseManager local =  new RndDatabaseManager(emf);
			db = new RndDbClient(split[0], connectPort);
			OverviewController.start(local, db);
			window.close();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void main(String[] args) {
		PropertyConfigurator.configure(Main.class.getResource("log4j.properties"));
		launch(args);
	}
}
