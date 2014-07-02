package net.zomis.rnddb.test;

import static org.junit.Assert.*;

import java.util.List;

import net.zomis.rnddb.entities.RndLevelset;
import net.zomis.rnddb.host.RndDbClient;
import net.zomis.rnddb.host.RndDbServer;
import net.zomis.rnddb.host.RndDbSource;

import org.junit.Before;
import org.junit.Test;

public class ServerConnTest {

	private RndDbSource source;

	@Before
	public void source() {
		source = new MockSource();
		assertNotNull(source);
	}
	
	@Test
	public void readFiles() {
		List<RndLevelset> lsets = source.getAllLevelSets();
		assertNotNull(lsets);
		assertEquals(1, lsets.size());
	}
	
	@Test(timeout = 20000)
	public void serverClient() throws Exception {
		try (RndDbServer server = new RndDbServer(source)) {
			Thread.sleep(2000);
			RndDbClient client = new RndDbClient("127.0.0.1", 4242);
			Thread.sleep(2000);
			List<RndLevelset> sets = client.getAllLevelSets();
			
			assertEquals(1, sets.size());
//			assertEquals(11, sets.get(0).getLevels().size());
			
			server.close();
		}
	}
	
}
