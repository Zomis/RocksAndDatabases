package net.zomis.chunks;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializationContext {
	
	private static final Logger logger = LoggerFactory.getLogger(SerializationContext.class);

	private Object serialization;
	
	private DataInputStream stream;

	private int	read;

	/**
	 * Expected bytes in current chunk
	 */
	int	expectedBytes = -1;
	
	public int getExpectedBytes() {
		if (expectedBytes < 0) {
			throw new IllegalStateException("Currently not reading any chunk");
		}
		return expectedBytes;
	}

	public SerializationContext(DataInputStream stream2, Object result) {
		this.serialization = result;
		this.stream = stream2;
	}

	public int readInt(int size) throws ChunkException {
		read += size;
		return ChunkUtils.readInt(stream, size);
	}
	
	public Object getObject() {
		return serialization;
	}
	
	public DataInputStream getStream() {
		return stream;
	}

	public int readByte() throws ChunkException {
		try {
			int result = stream.read();
			read++;
			return result;
		}
		catch (IOException e) {
			throw new ChunkException(e);
		}
	}

	public int readInt() throws ChunkException {
		try {
			int result = stream.readInt();
			read += 4;
			return result;
		}
		catch (IOException e) {
			throw new ChunkException(e);
		}
	}

	public int getRead() {
		return read;
	}

	public int readNextMicroField(Object fieldObject, List<Field> fields, Class<?> type) throws ChunkException {
		int read = 0;
		int microId = this.readByte();
		read++;
		
		Field field = ChunkUtils.findField(fields, microId);
		if (field == null)
			throw new ChunkException("No field matching " + Integer.toString(microId, 16) + " in " + type);
		read += ChunkRead.readField(fieldObject, this, field);
		return read;
	}

	public void skip(int bytes) throws IOException {
		logger.info("Skipping " + bytes + " bytes");
		stream.read(new byte[bytes]);
		read += bytes;
	}

	public int readWord() throws ChunkException {
		return readByte() << 8 + readByte();
	}

	public String readString(int count) throws IOException {
		byte[] bytes = new byte[count];
		int read = stream.read(bytes);
		if (read != count) {
			throw new IllegalStateException("Expected " + count + " but read " + read);
		}
		String result = new String(bytes);
		int nullTermIndex = result.indexOf(0);
		if (nullTermIndex >= 0) {
			result = result.substring(0, nullTermIndex);
		}
		return result;
	}

	public boolean readBoolean() throws ChunkException {
		return readByte() == 1;
	}

	public int readBytes(byte[] chunkNameBytes) throws IOException {
		int i = stream.read(chunkNameBytes);
		read += i;
		return i;
	}
	
	public void setExpectedBytes(int expectedBytes) {
		this.expectedBytes = expectedBytes;
	}
	
}
