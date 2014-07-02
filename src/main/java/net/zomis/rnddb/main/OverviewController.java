package net.zomis.rnddb.main;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import net.zomis.rnddb.RndScanner;
import net.zomis.rnddb.entities.RndLevelset;
import net.zomis.rnddb.host.RndDbSource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class OverviewController {

	private static final Logger logger = LogManager.getLogger(OverviewController.class);
	private final RndDbSource	db;
	private final Preferences prefs = Preferences.userNodeForPackage(getClass());
	private final File userDirectory;
	
	@FXML
	private TreeView<RndLevelset> localTree;

	@FXML
	private TreeView<RndLevelset> remoteTree;
	private final Stage stage;
	
	private File getDirectory() {
		final String USER_DIR = "USER_DIR";
		String storedPath = prefs.get(USER_DIR, "");
		if (verifyRnDDir(storedPath))
			return new File(storedPath);
		
		DirectoryChooser fileChooser = new DirectoryChooser();
		fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
		fileChooser.setTitle("Find Rocks'n'Diamonds Directory");
		File dir = fileChooser.showDialog(stage);
		if (dir == null)
			return null;
		
		if (verifyRnDDir(dir.getAbsolutePath())) {
			prefs.put(USER_DIR, dir.getAbsolutePath());
		}
		
		return dir;
	}
	
	
	private boolean verifyRnDDir(String fileName) {
		File file = new File(fileName);
		if (!file.exists())
			return false;
		
		File setupConf = new File(file, "levelsetup.conf");
		return setupConf.exists();
	}


	private OverviewController(RndDbSource db) {
		this.db = db;

		Pane root;
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("rnddbMain.fxml"));
			loader.setController(this);
			root = loader.load();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		stage = new Stage();
		stage.setTitle("My New Stage Title");
		stage.setScene(new Scene(root, 450, 450));
		stage.show();
		stage.setOnCloseRequest(e -> db.close());
		
		RndLevelset rootSet = new RndLevelset();
		rootSet.setName("ROOT");
        TreeItem<RndLevelset> rootItem = new TreeItem<>(rootSet);
        rootItem.setExpanded(true);
        rootItem.setGraphic(new CheckBox());
        localTree.setRoot(rootItem);
		
		userDirectory = getDirectory();
		System.out.println(userDirectory);
		this.scanAll(null);
		
//		Button save = new Button("Save to DB");
//		bottom.getChildren().add(save);
//		save.setOnAction(e -> saveNodeToDB(localTree.getRoot()));
//		
//		Button showAll = new Button("Show existing in DB");
//		bottom.getChildren().add(showAll);
//		showAll.setOnAction(this::showDBcontents);
		
	}

	public static void start(RndDbSource db) {
		new OverviewController(db);
	}

	@FXML
	private void download(ActionEvent event) {
		showDBcontents(event);
	}
	
	@FXML
	private void upload(ActionEvent event) {
		this.saveNodeToDB(localTree.getRoot());
	}
	
	private void showDBcontents(ActionEvent event) {
		db.getAllLevelSets().forEach(level -> System.out.println(level));
	}
	
	private void saveNodeToDB(TreeItem<RndLevelset> root) {
//		ZipFile zip = new ZipFile("");
//		File.createTempFile("rnddb", "zip");
		
		root.getChildren().stream().filter(node -> !node.isLeaf()).forEach(this::saveNodeToDB);
		
		root.getChildren().stream().filter(node -> node.isLeaf()).filter(node -> ((CheckBox) node.getGraphic()).isSelected()).forEach(leaf -> {
			leaf.getValue().readFiles();
			leaf.getValue().calcMD5();
			db.saveLevelSet(leaf.getValue());	
		});
		
	}

	private void scanAll(ActionEvent event) {
		logger.info("Scanning " + userDirectory);
		Map<RndLevelset, TreeItem<RndLevelset>> nodes = new HashMap<>();
		RndScanner.scan(RndScanner.getLevelsDir(userDirectory), lset -> {
			TreeItem<RndLevelset> item = new TreeItem<>(lset);
			item.setGraphic(lset.isLevelGroup() ? new Label("GRP") : new CheckBox());
			nodes.put(lset, item);
			TreeItem<RndLevelset> parent = lset.getParent() != null ? nodes.get(lset.getParent()) : localTree.getRoot();
			parent.getChildren().add(item);
		});
	}
	
}
