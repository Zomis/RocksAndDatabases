package net.zomis.rnddb.files;

import net.zomis.chunks.ChunkEntry;
import net.zomis.chunks.MicroChunk;

@MicroChunk(value = "DATE", isMicro = false)
public class DateData {
	@ChunkEntry(order = 0)
	private int date;
}
