package net.zomis.rnddb.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import javafx.scene.Parent;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import net.zomis.rnddb.entities.RndLevelset;
import net.zomis.rnddb.host.RndDatabaseManager;
import net.zomis.rnddb.host.RndDbClient;
import net.zomis.rnddb.host.RndDbServer;
import net.zomis.rnddb.host.RndDbSource;
import net.zomis.rnddb.host.RootPathFinder;
import net.zomis.rnddb.main.DatabaseConfig;
import net.zomis.rnddb.main.OverviewController;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.loadui.testfx.GuiTest;

import com.google.common.io.Files;

public class OverviewTest extends GuiTest {
	private static final Logger logger = LogManager.getLogger(OverviewTest.class);
	
	private OverviewController	overview;
	private static final int port = 4242;
	private final File rootServer = new File("test-server-dir");
	private final File rootClient = new File("test-client-dir");

	@Override
	protected Parent getRootNode() {
		RootPathFinder root = () -> rootServer;
		try {
			RndDbServer server = new RndDbServer(port, root, new RndDatabaseManager(TestEMF.localhostTest()));
			RndDatabaseManager local = new RndDatabaseManager(DatabaseConfig.localhostEmbedded());
			RndDbSource remote = new RndDbClient("127.0.0.1", port);
			this.overview = OverviewController.create(local, remote);
			
//			OverviewController.create(server, server);
			overview.setUserDirectory(rootClient);
			return overview.getRoot();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void removeIfExistsIn(File remove, File original) {
		
	}
	
	@Test
	public void emptyRemoteTree() throws InterruptedException, IOException {
		Thread.sleep(6000);
		
		removeIfExistsIn(rootServer, TestFiles.fileFor("userdir"));
		rootClient.mkdirs();
		copyDirectory(new File(TestFiles.fileFor("userdir"), "levels"), new File(rootClient, "levels"));
		rootServer.mkdirs();
		
		logger.info("Log");
		overview.scanAll();
		
		TreeView<RndLevelset> local = find("#localTree");
		TreeView<RndLevelset> remote = find("#remoteTree");
		
		// Make sure that the current local contains something and that server is empty
		click("#downloadButton");
		assertTrue(remote.getRoot().isLeaf());
		assertFalse(local.getRoot().isLeaf());
		Thread.sleep(3000);
		
		// Upload a local levelset to the server
		select(local.getRoot().getChildren().get(0));
		click("#uploadButton");
		Thread.sleep(1000);
		
		// TODO: Make sure that the levelsets exists on the server
		Thread.sleep(2000);
		
		// Retreive list of server levelsets
		click("#downloadButton");
		Thread.sleep(2000);
		assertFalse(remote.getRoot().isLeaf());
		assertFalse(local.getRoot().isLeaf());
		
		select(remote.getRoot().getChildren().get(0));
		click("#downloadButton");
		Thread.sleep(2000);
		// TODO: Make sure that the levelset exists on client
		
		// TODO: Make sure that it's not possible to upload the same levelset again, both same path and another path. Use MD5.
		
		
		
	}

	private void copyDirectory(File from, File to) throws IOException {
		assertTrue("Not a directory: " + from, from.isDirectory());
		assertFalse("Already exists: " + to, to.exists());
		
		to.mkdirs();
		for (File file : from.listFiles()) {
			if (file.isDirectory()) {
				copyDirectory(file, new File(to, file.getName()));
			}
			else {
				Files.copy(file, new File(to, file.getName()));
			}
		}
		
		
	}

	private void select(TreeItem<RndLevelset> treeItem) {
		click(treeItem.getGraphic());
	}

}
