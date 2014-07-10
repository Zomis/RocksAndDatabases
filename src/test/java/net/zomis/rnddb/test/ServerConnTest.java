package net.zomis.rnddb.test;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import net.zomis.rnddb.entities.RndLevelset;
import net.zomis.rnddb.host.RndDbClient;
import net.zomis.rnddb.host.RndDbServer;
import net.zomis.rnddb.host.RndDbSource;
import net.zomis.utils.MD5Util;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

public class ServerConnTest {
	private static final Logger logger = LogManager
			.getLogger(ServerConnTest.class);
	private static final int port = 4242;
	private RndDbSource source;

	@Before
	public void source() {
		source = new MockSource();
		assertNotNull(source);
	}
	
	@Test
	public void hexEncrypt() {
		assertEquals("6170", MD5Util.toHEX(new byte[]{ 0x61, 0x70 }, false));
		assertArrayEquals(new byte[]{ 0x61, 0x70 }, MD5Util.hex2byte("6170"));
	}
	
	@Test
	public void readFiles() {
		List<RndLevelset> lsets = source.getAllLevelSets();
		assertNotNull(lsets);
		assertEquals(1, lsets.size());
	}
	
	@Test(timeout = 20000)
	public void fetchLevelsetsFromServer() throws Exception {
		try (RndDbServer server = new RndDbServer(port, null, source)) {
			Thread.sleep(2000);
			RndDbClient client = new RndDbClient("127.0.0.1", port);
			Thread.sleep(2000);
			
			List<RndLevelset> sets = client.getAllLevelSets();
			assertEquals(1, sets.size());
//			assertEquals(11, sets.get(0).getLevels().size());
		}
	}
	
	@Test(timeout = 40000)
	public void serverClientTransferFiles() throws Exception {
		try (RndDbServer server = new RndDbServer(port, null, source)) {
			Thread.sleep(2000);
			RndDbClient client = new RndDbClient("127.0.0.1", port);
			Thread.sleep(2000);
			
			List<RndLevelset> sets = client.getAllLevelSets();
			assertEquals(1, sets.size());
			
			File dir = Files.createTempDirectory("zomisrnddb").toFile();
			client.downloadLevelSet(dir, sets.get(0));
			
			logger.info("Directory: " + dir.getAbsolutePath());
			logger.info("Files: " + dir.list().length);
			for (File file : dir.listFiles()) {
				logger.info("File: " + file.getAbsolutePath());
			}
			assertNotEquals(0, dir.list().length);
			deleteRecursive(dir);
		}
	}

	private void deleteRecursive(File file) {
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				if (f.isDirectory()) {
					deleteRecursive(f);
				}
				else f.delete();
			}
		}
		file.delete();
	}
	
}
