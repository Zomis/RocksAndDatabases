package net.zomis.chunks;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.zomis.rnddb.RndScanner;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class ChunkRead {
	
	private static final int CHUNK_BYTES = 4;
	private static final Logger logger = LogManager.getLogger(ChunkRead.class);
	
	public <T> T readFile(InputStream stream, Class<T> clazz) throws ChunkException {
		try {
			Map<String, Field> chunks = detectChunks(clazz);
			
			T result = clazz.newInstance();
			SerializationContext serialContext = new SerializationContext(new DataInputStream(stream), result);
			initializeDefaults(result, clazz);
			
			byte[] chunkNameBytes = new byte[CHUNK_BYTES];
			int read;
			
			while ((read = serialContext.readBytes(chunkNameBytes)) > 0) {
				String chunkName = new String(chunkNameBytes);
				logger.info("Read " + read + " - " + chunkName);
				if (read != CHUNK_BYTES) {
					throw new IOException("Expected " + CHUNK_BYTES + " bytes but only read " + read);
				}
				Field field = chunks.get(chunkName);
				if (field == null) {
					if (!onUnknownChunkMethod(clazz, result, chunkName, serialContext)) {
						throw new IOException("Unknown chunk name: " + chunkName + " bytes " + Arrays.toString(chunkNameBytes) + " at " + serialContext.getRead());
					}
				}
				else readField(result, serialContext, field);
			}
			logger.info("");
			
			stream.close();
			return result;
		}
		catch (IOException e) {
			throw new ChunkException(e);
		}
		catch (InstantiationException e) {
			throw new ChunkException(e);
		}
		catch (IllegalAccessException e) {
			throw new ChunkException(e);
		}
	}
	
	private <T> boolean onUnknownChunkMethod(Class<?> clazz, Object obj, String chunkName, SerializationContext context) throws ChunkException {
		logger.info("Scanning " + clazz + " for method with OnUnknownChunk. Methods are " + Arrays.toString(clazz.getMethods()));
		Optional<Method> call = Arrays.stream(clazz.getMethods()).filter(method -> method.getAnnotation(OnUnknownChunk.class) != null).findAny();
		if (call.isPresent()) {
			try {
				call.get().invoke(obj, chunkName, context);
			}
			catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new ChunkException(e);
			}
			return true;
		}
		return false;
	}
	
	public static Map<String, Field> detectChunks(Class<?> clazz) throws ChunkException {
		Map<String, Field> map = new HashMap<>();
		for (Field f : clazz.getDeclaredFields()) {
			ChunkEntry chunkEntry = f.getAnnotation(ChunkEntry.class);
			if (chunkEntry == null) {
				continue;
			}
			
			MicroChunk mChunk = f.getAnnotation(MicroChunk.class);
			if (mChunk == null)
				mChunk = f.getType().getAnnotation(MicroChunk.class);
			if (mChunk != null) {
				if (map.containsKey(mChunk.value())) {
					throw new ChunkException("Duplicate chunk definition: " + mChunk + " at field " + f + " previously defined by " + map.get(mChunk.value()));
				}
				
				map.put(mChunk.value(), f);
			}
		}
		return map;
	}

	private <T> void initializeDefaults(T t, Class<T> clazz) {
		// TODO: Initialize defaults
	}
	
	static String readString(DataInputStream stream, int stringLength) throws IOException {
		byte[] bytes = new byte[stringLength];
		int read = stream.read(bytes);
		if (read != stringLength)
			throw new IllegalStateException("Expected " + stringLength + " but read " + read);
		String result = new String(bytes);
		int nullTermIndex = result.indexOf(0);
		if (nullTermIndex >= 0) {
			result = result.substring(0, nullTermIndex);
		}
		return result;
	}

	public static int readField(Object obj, SerializationContext context, Field field) throws ChunkException {
		DataInputStream stream = context.getStream();
		try {
			ChunkEntry entry = field.getAnnotation(ChunkEntry.class);
			MicroChunk micro = field.getAnnotation(MicroChunk.class);
			if (micro == null)
				micro = field.getType().getAnnotation(MicroChunk.class);
			logger.info("readField: " + field);
			logger.info("- entry: " + entry);
			logger.info("- micro: " + micro);
			
			int read = 0;
			field.setAccessible(true);
			Class<?> fieldType = field.getType();
			
			// TODO: Refactor fieldType readers.
			
			if (fieldType == int.class) {
				read += readInt(field, entry, stream, obj);
			}
			else if (fieldType == boolean.class) {
				boolean value = stream.readBoolean();
				field.setBoolean(obj, value);
				read++;
			}
			else if (fieldType.isEnum()) {
				read += readEnum(field, entry, stream, obj);
			}
			else if (fieldType == Void.class) {
				context.skip(entry.size());
			}
			else if (fieldType == String.class) {
				int length;
				// TODO: If reading as part of MicroChunk, then size is 2.
				if (micro != null && !micro.isMicro()) {
					length = 4;
				}
				else
					throw new UnsupportedOperationException("Undetermined string length when reading " + field + " micro " + micro + " entry " + entry);
//					length = 1;
				read += length;
				
				length = ChunkUtils.readInt(stream, length);
				
				String str = readString(stream, length);
				logger.info("Reading string " + field + " = " + str);
				field.set(obj, str);
				read += length;
			}
			else if (micro != null) {
				int expectedByteSize = micro.size();
				if (expectedByteSize == -1) {
					expectedByteSize = stream.readInt();
				}
				logger.info("Encountered field " + field + " expecting " + expectedByteSize);
				
				Object fieldObject = field.getType().newInstance();
				int bytesRead;
				
				Method customReadMethod = findReadMethod(field.getType(), context);
				context.expectedBytes = expectedByteSize;
				if (customReadMethod != null) {
					int old = context.getRead();
					customReadMethod.invoke(fieldObject, context, context.getObject());
					expectedByteSize = context.getExpectedBytes();
					bytesRead = context.getRead() - old;
				}
				else if (micro.isMicro())
					bytesRead = readMicro(fieldObject, context, field.getType(), expectedByteSize);
				else {
					bytesRead = readChunk(fieldObject, context, field.getType());
				}
				if (bytesRead != expectedByteSize) {
					throw new ChunkException("Expected " + expectedByteSize + " but read " + bytesRead + " in chunk " + field);
				}
				context.expectedBytes = -1;
				field.set(obj, fieldObject);
				read += bytesRead;
			}
			else throw new UnsupportedOperationException("Unsupported " + field + " with annotations " + Arrays.asList(field.getAnnotations()) + " in " + obj.getClass());
			
			return read;
		}
		catch (ChunkException e) {
			throw e;
		}
		catch (SecurityException | IllegalArgumentException
				| IllegalAccessException | InstantiationException | IOException | InvocationTargetException e) {
			throw new ChunkException(e);
		}
	}

	private static int readEnum(Field field, ChunkEntry entry, DataInputStream stream, Object obj) throws IOException, IllegalArgumentException, IllegalAccessException {
		Class<?> fieldType = field.getType();
		logger.info("Enum type " + fieldType);
		logger.info(Arrays.toString(fieldType.getDeclaredFields()));
		Object[] values = fieldType.getEnumConstants();
		logger.info(Arrays.toString(values));
		int index = stream.read();
		field.set(obj, values[index]);
		return 1;
	}

	private static int readInt(Field field, ChunkEntry entry, DataInputStream stream, Object obj) throws ChunkException, IllegalArgumentException, IllegalAccessException {
		int size = entry.size() > 0 ? entry.size() : 4;
		
		int value = ChunkUtils.readInt(stream, size);
		field.setInt(obj, value);
		logger.info("Reading int " + field + " = " + value + " of size " + size);
		return size;
	}

	private static int readChunk(Object fieldObject, SerializationContext context, Class<?> clazz) throws ChunkException {
		logger.info("readChunk: Read " + clazz + " to " + fieldObject);
		List<Field> fields = ChunkUtils.getFields(clazz);
		logger.info("readChunk - fields: " + fields);
		int i = 0;
		for (Field f : fields) {
			i += readField(fieldObject, context, f);
		}
		return i;
	}

	private static int readMicro(Object fieldObject, SerializationContext context, Class<?> type, int expected) throws ChunkException {
		logger.info("Read micro " + type + " to " + fieldObject);
		List<Field> fields = ChunkUtils.getFields(type);
		
		int read = 0;
		do {
			read += context.readNextMicroField(fieldObject, fields, type);
		}
		while (read < expected);
		
		return read;
	}

	public static void main(String[] args) {
		
		File gameDir = new RndScanner().inputDirectory(RndScanner.GAME_DIR);
		File userDir = new RndScanner().inputDirectory(RndScanner.USER_DIR);
		
//		File dir = new File(".");
//		RndScanner.scan(dir);
//		RnddbFrame.showFrame();
		
		RndScanner.scan(RndScanner.getLevelsDir(gameDir), (lset) -> {});
		RndScanner.scan(RndScanner.getLevelsDir(userDir), (lset) -> {});
	}
	
	private static Method findReadMethod(Class<?> type, SerializationContext context) {
		return Arrays.stream(type.getMethods()).filter(method -> method.getAnnotation(OnRead.class) != null).findFirst().orElse(null);
	}

	
}
