package net.zomis.rnddb.main;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

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
import net.zomis.rnddb.host.RndDbClient;
import net.zomis.rnddb.host.RndDbSource;
import net.zomis.rnddb.host.RootPathFinder;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class OverviewController implements RootPathFinder {

	private static final Logger logger = LogManager.getLogger(OverviewController.class);
	private static final String USER_DIR = "USER_DIR";
	
	private final RndDbSource	remote;
	private final RndDbSource	local;
	private final Preferences prefs = Preferences.userNodeForPackage(getClass());
	private File userDirectory;
	
	@FXML
	private TreeView<RndLevelset> localTree;

	@FXML
	private TreeView<RndLevelset> remoteTree;
	private final Stage stage;
	
	private File getDirectory() {
		String storedPath = prefs.get(USER_DIR, "");
		if (verifyRnDDir(storedPath))
			return new File(storedPath);
		
		DirectoryChooser fileChooser = new DirectoryChooser();
//		fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
		fileChooser.setTitle("Find Rocks'n'Diamonds Directory");
		File dir = fileChooser.showDialog(stage);
		if (dir == null) {
			return null;
		}
		
		if (verifyRnDDir(dir.getAbsolutePath())) {
			prefs.put(USER_DIR, dir.getAbsolutePath());
		}
		
		return dir;
	}
	
	@FXML
	private void chooseBaseDirectory(ActionEvent event) {
		prefs.remove(USER_DIR);
		getDirectory();
	}
	
	
	private boolean verifyRnDDir(String fileName) {
		File file = new File(fileName);
		if (!file.exists()) {
			return false;
		}
		
		File setupConf = new File(file, "levelsetup.conf");
		return setupConf.exists();
	}

	enum Mode {
		LOCAL_ONLY,// See actual files on left, database on right
		AS_SERVER, // See actual files on left, database on right
		CONNECTED; // See actual files (+db?) on left, remote on right
	}
	
	private OverviewController(RndDbSource local, RndDbSource db) {
		this.remote = db;
		this.local = local;
		
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
		stage.setTitle("Rocks'n'Diamonds Database");
		stage.setScene(new Scene(root, 450, 450));
		stage.show();
		stage.setOnCloseRequest(e -> db.close());
		
		RndLevelset rootSet = new RndLevelset();
		rootSet.setName("Local");
        TreeItem<RndLevelset> rootItem = new TreeItem<>(rootSet);
        rootItem.setExpanded(true);
        rootItem.setGraphic(new CheckBox());
        localTree.setRoot(rootItem);
		
		RndLevelset remoteRootSet = new RndLevelset();
		remoteRootSet.setName("Remote");
        TreeItem<RndLevelset> remoteRootItem = new TreeItem<>(remoteRootSet);
        remoteRootItem.setExpanded(true);
        remoteRootItem.setGraphic(new CheckBox());
        remoteTree.setRoot(remoteRootItem);
		
		userDirectory = getDirectory();
		logger.info("User Directory is " + userDirectory);
		this.scanAll(null);
	}

	public static OverviewController start(RndDbSource local, RndDbSource db) {
		return new OverviewController(local, db);
	}

	@FXML
	private void download(ActionEvent event) {
		if (remoteTree.getRoot().getChildren().isEmpty()) {
			showDBcontents(event);
		}
		else {
			logger.info("Downloading if possible");
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e1) {
			}
			
			if (remote instanceof RndDbClient) {
				RndDbClient client = (RndDbClient) remote;
				try {
					for (TreeItem<RndLevelset> item : remoteTree.getSelectionModel().getSelectedItems()) {
						client.downloadLevelSet(userDirectory, item.getValue());
					}
				}
				catch (InterruptedException | IOException e) {
					logger.error("Error downloading selected: " + remoteTree.getSelectionModel().getSelectedItems(), e);
				}
			}
		}
	}
	
	@FXML
	private void upload(ActionEvent event) {
		this.saveNodeToDB(localTree.getRoot());
	}
	
	private void showDBcontents(ActionEvent event) {
		remote.getAllLevelSets().forEach(level -> logger.debug("DB: " + level));
		populateRemoteTree(remote);
	}
	
	private void saveNodeToDB(TreeItem<RndLevelset> root) {
		// TODO: Also need to save `levelinfo.conf` of all parent levelsets!
		root.getChildren().stream().filter(node -> !node.isLeaf()).forEach(this::saveNodeToDB);
		
		root.getChildren().stream().filter(node -> node.isLeaf()).filter(node -> ((CheckBox) node.getGraphic()).isSelected()).forEach(leaf -> {
			RndLevelset parent = leaf.getValue().getParent();
			while (parent != null) {
				parent.readFiles();
				parent.calcMD5();
				remote.saveLevelSet(parent);
				
				parent = parent.getParent();
			}
			leaf.getValue().readFiles();
			leaf.getValue().calcMD5();
			remote.saveLevelSet(leaf.getValue());
		});
	}

	private void scanAll(ActionEvent event) {
		logger.info("Scanning " + userDirectory);
		Map<RndLevelset, TreeItem<RndLevelset>> nodes = new HashMap<>();
		RndScanner.scanLevels(userDirectory, lset -> {
			TreeItem<RndLevelset> item = new TreeItem<>(lset);
			item.setGraphic(lset.isLevelGroup() ? new Label("GRP") : new CheckBox());
			nodes.put(lset, item);
			TreeItem<RndLevelset> parent = lset.getParent() != null ? nodes.get(lset.getParent()) : localTree.getRoot();
			parent.getChildren().add(item);
		});
	}
	
	private void populateRemoteTree(RndDbSource source) {
		List<RndLevelset> allSets = source.getAllLevelSets();
		RndLevelset nullParent = remoteTree.getRoot().getValue();
		Map<RndLevelset, List<RndLevelset>> childs = allSets.stream().collect(Collectors.groupingBy(set -> Optional.ofNullable(set.getParent()).orElse(nullParent)));
		Map<RndLevelset, TreeItem<RndLevelset>> nodes = new HashMap<>();
		nodes.put(nullParent, remoteTree.getRoot());
		createRecursively(childs.get(nullParent), childs, nodes);
	}

	private void createRecursively(List<RndLevelset> list, Map<RndLevelset, List<RndLevelset>> childs, Map<RndLevelset, TreeItem<RndLevelset>> nodes) {
		Consumer<RndLevelset> create = lset -> {
			TreeItem<RndLevelset> item = new TreeItem<>(lset);
			item.setGraphic(lset.isLevelGroup() ? new Label("GRP") : new CheckBox());
			nodes.put(lset, item);
			TreeItem<RndLevelset> parent = lset.getParent() != null ? nodes.get(lset.getParent()) : remoteTree.getRoot();
			parent.getChildren().add(item);
		};
		
		logger.info("Scanning " + list);
		
		if (list == null) {
			logger.warn("List is null.");
			return;
		}
		
		list.stream().filter(set -> !set.isLevelGroup()).forEach(create);
		
		list.stream().filter(set -> set.isLevelGroup()).forEach(set -> {
			create.accept(set);
			createRecursively(childs.get(set), childs, nodes);
		});
	}

	@Override
	public File getRootPath() {
		return this.userDirectory;
	}
	
	
}
