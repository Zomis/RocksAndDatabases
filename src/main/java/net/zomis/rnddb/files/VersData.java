package net.zomis.rnddb.files;

import net.zomis.chunks.ChunkEntry;
import net.zomis.chunks.MicroChunk;

@MicroChunk(value = "VERS", isMicro = false)
public class VersData {

	// TODO: @FileVersion <--- indicates that this should be stored in memory when reading / writing a file and that other ChunkEntries may depend on this value
	@ChunkEntry(order = 0)
	private int fileVersion;
	
	@ChunkEntry(order = 1)
	private int gameVersion;
	
	public int getFileVersion() {
		return fileVersion;
	}
	
	public int getGameVersion() {
		return gameVersion;
	}
	
	public void setFileVersion(int fileVersion) {
		this.fileVersion = fileVersion;
	}
	
	public void setGameVersion(int gameVersion) {
		this.gameVersion = gameVersion;
	}
	
}
