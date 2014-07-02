package net.zomis.chunks;

import java.io.DataInputStream;
import java.lang.reflect.Field;
import java.util.List;

public class ChunkWrite {
	
	@Deprecated
	public int read(Object result, DataInputStream stream, Class<?> clazz) {
		MicroChunk chunk = clazz.getAnnotation(MicroChunk.class);
		if (chunk == null) {
			throw new IllegalArgumentException(clazz + " does not have " + MicroChunk.class + " annotation");
		}
		try {
			List<Field> fields = ChunkUtils.getFields(clazz);
			
			System.out.println("Reading " + chunk + " fields " + fields);
//			String head = readString(stream, 4);
//			if (!head.equals(chunk.value()))
//				throw new IllegalStateException("Expected " + chunk.value() + " but read " + head);
//
//			for (Field f : fields) {
//				readField(result, stream, f);
//			}
			
			return 0;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return 0;
	}
	

}
