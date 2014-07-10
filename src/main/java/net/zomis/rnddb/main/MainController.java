package net.zomis.rnddb.main;

import java.io.File;
import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javax.persistence.EntityManagerFactory;

import net.zomis.rnddb.host.RndDatabaseManager;
import net.zomis.rnddb.host.RndDbClient;
import net.zomis.rnddb.host.RndDbServer;
import net.zomis.rnddb.host.RndDbSource;
import net.zomis.rnddb.host.RootPathFinder;
import net.zomis.rnddb.test.MockSource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class MainController {
	private static final int DEFAULT_PORT = 7332;
	private static final Logger logger = LogManager.getLogger(Main.class);
	private EntityManagerFactory emf;
	private RndDbSource db;

	@FXML
	private TextField serverAddress;
	
	@FXML
	private void localOnly(ActionEvent event) {
		logger.info("Local");
		emf = DatabaseConfig.localhostEmbedded();
		db = new RndDatabaseManager(emf);
		OverviewController.start(db, db);
		close(event);
	}
	
	private void close(ActionEvent event) {
		Node source = (Node) event.getSource();
		Stage stage = (Stage) source.getScene().getWindow();
		stage.close();
	}

	@FXML
	private void host(ActionEvent event) {
		try {
			emf = DatabaseConfig.fromFile(new File("connection.properties"));
			
			RedirectRootPathFinder changer = new RedirectRootPathFinder();
			db = new RndDbServer(DEFAULT_PORT, changer, new RndDatabaseManager(emf));
			OverviewController overview = OverviewController.start(db, db);
			changer.redirect = overview;
			close(event);
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
			close(event);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@FXML
	private void test(ActionEvent event) {
		OverviewController.start(new MockSource(), new MockSource()).getRoot();
		close(event);
	}
	
}
