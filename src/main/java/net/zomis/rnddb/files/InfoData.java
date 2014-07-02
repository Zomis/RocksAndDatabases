package net.zomis.rnddb.files;

import net.zomis.chunks.ChunkEntry;
import net.zomis.chunks.MicroChunk;
import net.zomis.chunks.SaveType;

@MicroChunk(value = "INFO")
public class InfoData {
	
	@ChunkEntry(order = 1, size = 1, save = SaveType.ALWAYS)
	private GameEngine gameEngineType;
	
	@ChunkEntry(order = 2, size = 2, standard = 64, save = SaveType.ALWAYS)
	private int width;
	
	@ChunkEntry(order = 3, size = 2, standard = 64, save = SaveType.ALWAYS)
	private int height;
	
	@ChunkEntry(order = 4, size = 2, standard = 100, save = SaveType.ALWAYS)
	private int time;
	
	@ChunkEntry(order = 5, size = 2, standard = 0, save = SaveType.ALWAYS)
	private int gemsNeeded;
	
	@ChunkEntry(order = 6)
	private int randomSeed;
	
	@ChunkEntry(order = 7)
	private boolean useSteps;
	
	@ChunkEntry(order = 8, size = 1)
	@Deprecated
	private Void deprecated;
	
	@ChunkEntry(order = 9, size = 1)
	private int windDirectionInitial;
	
	@ChunkEntry(order = 10, size = 1)
	private int emSlipperyGems;
	
	@ChunkEntry(order = 11, size = 1)
	private int useCustomTemplate;
	
	@ChunkEntry(order = 12, standard = Integer.MAX_VALUE)
	private int canMoveIntoAcidBits;
	
	@ChunkEntry(order = 13, size = 1, standard = 0xff)
	private int dontCollideWithBits;
	
	@ChunkEntry(order = 14)
	private boolean emExplodesByFire;
	
	@ChunkEntry(order = 15, size = 2, standard = 1)
	private int scoreTimeBonus;
	
	@ChunkEntry(order = 16)
	private boolean autoExitSokoban;
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public int getGemsNeeded() {
		return gemsNeeded;
	}
	
	public int getTime() {
		return time;
	}
	
	public void setWidth(int width) {
		this.width = width;
	}
	
	public void setHeight(int height) {
		this.height = height;
	}
	
	public void setGemsNeeded(int gemsNeeded) {
		this.gemsNeeded = gemsNeeded;
	}
	
	public void setTime(int time) {
		this.time = time;
	}
	
}
