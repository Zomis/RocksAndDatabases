package net.zomis.rnddb.files;

import java.io.IOException;

import net.zomis.chunks.MicroChunk;
import net.zomis.chunks.OnRead;
import net.zomis.chunks.SerializationContext;

@MicroChunk(value = "ELEM")
public class ElemData {
	
	
	
	@OnRead(RocksLevel.class)
	public void read(SerializationContext context, RocksLevel rocks) throws IOException {
		context.skip(context.getExpectedBytes());
	}

}
