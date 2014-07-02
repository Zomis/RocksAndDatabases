package net.zomis.rnddb.files;

import net.zomis.chunks.ChunkEntry;
import net.zomis.chunks.ChunkException;
import net.zomis.chunks.MicroChunk;
import net.zomis.chunks.OnRead;
import net.zomis.chunks.SerializationContext;

@MicroChunk(value = "BODY", isMicro = false)
public class BodyData {
	@ChunkEntry(size = 2, order = 0)
	public int[][] values;
	
	@OnRead(RocksLevel.class)
	public void read(SerializationContext context, RocksLevel rocks) throws ChunkException {
		if (rocks.isEncoding_16bit_field() && rocks.getVers().getFileVersion() < RocksLevel.VERSION_IDENT(2, 0, 0, 0)) {
//			System.out.println("Modifying BODY chunk size expected because of old RnD Bug. Previous expected was " + context.getExpectedBytes());
			context.setExpectedBytes(context.getExpectedBytes() * 2);
		}
		
		int bytes = rocks.isEncoding_16bit_field() ? 2 : 1;
		int width = rocks.getInfo().getWidth();
		int height = rocks.getInfo().getHeight();
		values = new int[width][height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int element = context.readInt(bytes);
				values[x][y] = element;
			}
		}
	}
	
}
