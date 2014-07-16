package net.zomis.rnddb.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
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

	enum Mode {
		AS_SERVER,// See actual files on left, database on right
		CONNECTED, // See actual files on left, database on right
		LOCAL_ONLY; // See actual files (+db?) on left, remote on right
	}
	private static final Logger logger = LogManager.getLogger(OverviewController.class);

	private static final String USER_DIR = "USER_DIR";

	public static OverviewController start(RndDbSource local, RndDbSource db) {
		OverviewController controller = new OverviewController(local, db);
		controller.userDirectory = controller.getDirectory();
		logger.info("User Directory is " + controller.userDirectory);
		controller.scanAll();
		return controller;
	}

	public static OverviewController create(RndDbSource local, RndDbSource db) {
		OverviewController controller = new OverviewController(local, db);
		return controller;
	}

	private final RndDbSource	local;

	@FXML
	private TreeView<RndLevelset> localTree;

	private final Preferences prefs = Preferences.userNodeForPackage(getClass());

	private final RndDbSource	remote;
	@FXML
	private TreeView<RndLevelset> remoteTree;

	private Pane root;

	private final Stage stage;


	private File userDirectory;

	private OverviewController(RndDbSource local, RndDbSource db) {
		this.remote = db;
		this.local = local;

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
		rootSet.setPath("levels/");
		TreeItem<RndLevelset> rootItem = new TreeItem<>(rootSet);
		rootItem.setExpanded(true);
		rootItem.setGraphic(new CheckBox());
		localTree.setRoot(rootItem);

		RndLevelset remoteRootSet = new RndLevelset();
		remoteRootSet.setName("Remote");
		remoteRootSet.setPath("levels/");
		TreeItem<RndLevelset> remoteRootItem = new TreeItem<>(remoteRootSet);
		remoteRootItem.setExpanded(true);
		remoteRootItem.setGraphic(new CheckBox());
		remoteTree.setRoot(remoteRootItem);
	}

	@FXML
	private void chooseBaseDirectory(ActionEvent event) {
		prefs.remove(USER_DIR);
		getDirectory();
	}

	private void createLevelsInfoConf(File downloadDir, File rootDirectory) throws IOException {
		if (downloadDir.getParentFile().getAbsoluteFile().equals(rootDirectory.getAbsoluteFile())) {
			return;
		}
		downloadDir.mkdirs();
		File saveFile = new File(downloadDir, "levelinfo.conf");
		if (!saveFile.exists()) {
			logger.info("Creating conf at " + saveFile);
			Files.copy(OverviewController.class.getResourceAsStream("levelinfo.conf"), saveFile.toPath());
		}
		createLevelsInfoConf(downloadDir.getParentFile(), rootDirectory);
	}

	private void createRecursively(List<RndLevelset> list, Map<String, List<RndLevelset>> childs, Map<String, TreeItem<RndLevelset>> nodes) {
		Consumer<RndLevelset> create = lset -> {
			TreeItem<RndLevelset> item = new TreeItem<>(lset);
			item.setGraphic(lset.isLevelGroup() ? new Label("GRP") : new CheckBox());
			nodes.put(lset.getPath(), item);
			TreeItem<RndLevelset> parent = lset.hasParentPath() ? nodes.get(lset.getParentPath()) : remoteTree.getRoot();
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
			createRecursively(childs.get(set.getPath()), childs, nodes);
		});
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
					downloadNode(client, remoteTree.getRoot());
				}
				catch (InterruptedException | IOException e) {
					logger.error("Error downloading selected: " + remoteTree.getSelectionModel().getSelectedItems(), e);
				}
			}
		}
	}

	private void downloadNode(RndDbClient client, TreeItem<RndLevelset> root) throws InterruptedException, IOException {
		File downloadDir = new File(userDirectory, "levels/download");
		createLevelsInfoConf(new File(downloadDir, "levels"), userDirectory);

		if (root.isLeaf()) {
			CheckBox checkbox = (CheckBox) root.getGraphic();
			if (!checkbox.isSelected()) {
				return;
			}
			for (TreeItem<RndLevelset> parent = root.getParent(); parent != null; parent = parent.getParent()) {
				if (parent.getValue().hasChecksum())
					client.downloadLevelSet(downloadDir, parent.getValue());
			}
			if (root.getValue().hasChecksum()) {
				client.downloadLevelSet(downloadDir, root.getValue());
			}
		}

		for (TreeItem<RndLevelset> item : root.getChildren()) {
			downloadNode(client, item);
		}

	}

	private File getDirectory() {
		String storedPath = prefs.get(USER_DIR, "");
		if (verifyRnDDir(storedPath)) {
			return new File(storedPath);
		}

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

	public Pane getRoot() {
		return root;
	}

	@Override
	public File getRootPath() {
		return this.userDirectory;
	}

	private Map<String, TreeItem<RndLevelset>> nodesMapForTree(TreeView<RndLevelset> localTree) {
		HashMap<String, TreeItem<RndLevelset>> result = new HashMap<>();
		result.put(localTree.getRoot().getValue().getPath(), localTree.getRoot());
		return result;
	}

	private void populateRemoteTree(RndDbSource source) {
		List<RndLevelset> allSets = source.getAllLevelSets();
		allSets.sort(Comparator.comparingInt(ls -> ls.getPath().length()));
		allSets.forEach(logger::info);

		RndLevelset nullParent = remoteTree.getRoot().getValue();
		Map<String, List<RndLevelset>> childs = allSets.stream().collect(Collectors.groupingBy(set -> Optional.ofNullable(set.getParentPath()).orElse(nullParent.getPath())));
		logger.info(childs);
		Map<String, TreeItem<RndLevelset>> nodes = new HashMap<>();
		nodes.put(nullParent.getPath(), remoteTree.getRoot());
		createRecursively(childs.get(nullParent.getPath()), childs, nodes);
	}

	private void realSaveNode(TreeItem<RndLevelset> node) {
		RndLevelset levelset = node.getValue();
		levelset.readFiles();
		levelset.calcMD5();
		remote.saveLevelSet(levelset);
	}

	private void saveNodeToDB(TreeItem<RndLevelset> root) {
		root.getChildren().stream().filter(node -> !node.isLeaf()).forEach(this::saveNodeToDB);

		root.getChildren().stream().filter(node -> node.isLeaf()).filter(node -> ((CheckBox) node.getGraphic()).isSelected()).forEach(leaf -> {
			TreeItem<RndLevelset> parent = leaf.getParent();
			while (parent != null) {
				// save `levelinfo.conf` and potentially also other files of all parent levelsets!
				if (parent.getParent() != null) {
					realSaveNode(parent);
				}
				parent = parent.getParent();
			}
			realSaveNode(leaf);
		});
	}

	public void scanAll() {
		logger.info("Scanning " + userDirectory);
		Map<String, TreeItem<RndLevelset>> nodes = nodesMapForTree(localTree);
		logger.info("Nodes: " + nodes);
		RndScanner.scanLevels(userDirectory, lset -> {
			TreeItem<RndLevelset> item = new TreeItem<>(lset);
			item.setGraphic(lset.isLevelGroup() ? new Label("GRP") : new CheckBox());
			nodes.put(lset.getPath(), item);
			TreeItem<RndLevelset> parent = lset.hasParentPath() ? nodes.get(lset.getParentPath()) : localTree.getRoot();
			logger.trace("Nodes add: " + lset.getPath() + " == " + item + " search for " + lset.getParentPath() + " parent found " + parent);
			List<TreeItem<RndLevelset>> kids = parent.getChildren();
			kids.add(item);
		});
	}

	public void setUserDirectory(File userDirectory) {
		this.userDirectory = userDirectory;
	}

	private void showDBcontents(ActionEvent event) {
		remote.getAllLevelSets().forEach(level -> logger.debug("DB: " + level));
		populateRemoteTree(remote);
	}

	@FXML
	private void upload(ActionEvent event) {
		this.saveNodeToDB(localTree.getRoot());
	}

	private boolean verifyRnDDir(String fileName) {
		File file = new File(fileName);
		if (!file.exists()) {
			return false;
		}

		File setupConf = new File(file, "levelsetup.conf");
		return setupConf.exists();
	}


}
