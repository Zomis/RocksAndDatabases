package net.zomis.rnddb.host;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import net.zomis.rnddb.entities.RndFile;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class RndFileWithData {
	private static final Logger logger = LogManager.getLogger(RndFileWithData.class);
	private final RndFile	file;
	private final byte[]	bytes;

	public RndFileWithData(RndFile file, byte[] bs) {
		this.file = file;
		this.bytes = bs;
	}
	
	public byte[] getBytes() {
		return bytes;
	}
	
	public RndFile getFile() {
		return file;
	}
	
	public void saveToDisc() throws IOException {
		if (file.getFile().exists()) {
			throw new IOException("File already exists");
		}
		
		logger.info("Writing file " + file.getFile());
		file.getFile().getParentFile().mkdirs();
		Files.write(file.getFile().toPath(), bytes, StandardOpenOption.CREATE);
	}
	
}
