package net.zomis.rnddb.files;

import java.io.IOException;

import net.zomis.chunks.ChunkEntry;
import net.zomis.chunks.MicroChunk;
import net.zomis.chunks.OnRead;
import net.zomis.chunks.SerializationContext;

@MicroChunk(value = "NOTE")
public class EnvelopeData {

	private static final int MAX_ENVELOPE_XSIZE = 30;
	private static final int MAX_ENVELOPE_YSIZE = 20;
	
	@ChunkEntry(order = 1, standard = MAX_ENVELOPE_XSIZE)
	private int width;
	
	@ChunkEntry(order = 2, standard = MAX_ENVELOPE_YSIZE)
	private int height;
	
	@ChunkEntry(order = 3, standard = 0)
	private boolean autowrap;
	
	@ChunkEntry(order = 4, standard = 0)
	private boolean centered;
	
	@ChunkEntry(order = 5)
	private String text;
	
	@OnRead(RocksLevel.class)
	public void read(SerializationContext context, RocksLevel rocks) throws IOException {
		context.skip(context.getExpectedBytes());
	}
	
	
}
