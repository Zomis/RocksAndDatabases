package net.zomis.rnddb.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import net.zomis.chunks.ChunkException;
import net.zomis.chunks.ChunkRead;
import net.zomis.rnddb.entities.RndLevel;
import net.zomis.rnddb.files.RocksLevel;

import org.apache.log4j.PropertyConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

public class LoadLevelTest {
	@BeforeClass
	public static void before() {
		PropertyConfigurator.configure(Thread.currentThread().getContextClassLoader().getResourceAsStream("log4j.properties"));
	}
	
	@Test
	public void loadFail() {
		try {
			loadFromResource("danilo-readme-incorrect.level");
			fail();
		}
		catch (ChunkException ex) { }
	}
	
	
	@Test
	public void loadWithGRPX() throws ChunkException {
		RocksLevel result = loadFromResource("newformat-withGRPX.level");
		assertEquals("Johan", result.getAuthor());
		assertEquals("nameless level", result.getName());
	}

	@Test
	public void loadBigfile() throws IOException {
		String fileName = "bigfile-CUS4-GRP1.level";
		RocksLevel result = loadFromResource(fileName);
		assertEquals("alan", result.getAuthor());
		assertEquals("necropolis", result.getName());
		File file = new File(Thread.currentThread().getContextClassLoader().getResource(fileName).toExternalForm().substring("file:\\".length()));
		RndLevel level = new RndLevel();
		level.setFromLevel(result, null, file);
		level.calcMD5();
		
		assertEquals(164720, level.serialize().length());
		
	}

	@Test
	public void loadOldStyle8bitBODY() throws ChunkException {
		RocksLevel result = loadFromResource("oldformat-1.2-body8.level");
		assertEquals(0x0D, result.getBody().values[0][0]);
//		assertEquals(0x0D, result.getBody().values[1][0]);
		
		assertEquals("anonymous", result.getAuthor());
		assertEquals("Level 19", result.getName());
	}

	@Test
	public void loadOldFormat10() throws ChunkException {
		RocksLevel result = loadFromResource("oldformat-1.0-body8.level");
		assertEquals(0x08, result.getBody().values[0][0]);
		assertEquals(0x01, result.getBody().values[1][0]);
		
		assertEquals("anonymous", result.getAuthor());
		assertEquals("Emeralds'n'Diamonds", result.getName());
	}

	@Test
	public void loadWithELEMandNOTE() throws ChunkException {
		RocksLevel result = loadFromResource("newformat-ELEM-NOTE.level");
		assertEquals("rndscripter", result.getAuthor());
		assertEquals("square maze 63*31", result.getName());
	}

	@Test
	public void loadPrettyEmpty() throws ChunkException {
		RocksLevel result = loadFromResource("newformat-prettyempty.level");
		assertEquals("zomis", result.getAuthor());
		assertEquals("nameless level", result.getName());
	}

	@Test
	public void loadWithCUS3() throws ChunkException {
		RocksLevel result = loadFromResource("newformat-withCUS3.level");
		assertEquals("alfa", result.getAuthor());
		assertEquals("nameless level", result.getName());
	}

	@Test
	public void loadWithCUSX() throws ChunkException {
		RocksLevel result = loadFromResource("newformat-withCUSX.level");
		assertEquals("zomis", result.getAuthor());
		assertEquals("rotation 4", result.getName());
	}

	@Test
	public void loadCNT3_CUS4_GRP1() throws ChunkException {
		RocksLevel result = loadFromResource("newformat-CNT3-CUS4-GRP1.level");
		assertEquals("zomis", result.getAuthor());
		assertEquals("minesweeper", result.getName());
	}

	@Test
	public void loadCNT3_CUS4_GRP1_x2() throws ChunkException {
		RocksLevel result = loadFromResource("newformat-CNT3-CUS4-GRP1 x2.level");
		assertEquals("zomis", result.getAuthor());
		assertEquals("minesweeper advanced", result.getName());
	}

	@Test
	public void loadOldFromat14() throws ChunkException {
		RocksLevel result = loadFromResource("oldformat-1.4.level");
		assertEquals("zomis", result.getAuthor());
		assertEquals("delays, lamps, and stuff...", result.getName());
	}

	private RocksLevel loadFromResource(String fileName) throws ChunkException {
		InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
		
		assertNotNull(stream);
		return new ChunkRead().readFile(stream, RocksLevel.class);
	}
	
	
}
