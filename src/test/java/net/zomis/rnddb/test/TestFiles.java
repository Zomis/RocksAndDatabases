package net.zomis.rnddb.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.InputStream;

import net.zomis.chunks.ChunkException;
import net.zomis.chunks.ChunkRead;
import net.zomis.rnddb.files.RocksLevel;

public class TestFiles {

	public static File fileFor(String fileName) {
		return new File(Thread.currentThread().getContextClassLoader().getResource(fileName).toExternalForm().substring("file:\\".length()));
	}
	
	public static RocksLevel loadLevelFromResource(String fileName) throws ChunkException {
		InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
		
		assertNotNull(stream);
		return new ChunkRead().readFile(stream, RocksLevel.class);
	}
	
}
