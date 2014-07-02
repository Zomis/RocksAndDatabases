package net.zomis.chunks;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ChunkUtils {
	public static Comparator<ChunkEntry> chComp = new Comparator<ChunkEntry>() {
		@Override
		public int compare(ChunkEntry o1, ChunkEntry o2) {
			return Integer.compare(o1.order(), o2.order());
		}
	};

	public static List<Field> getFields(Class<?> clazz) {
		List<Field> fields = new ArrayList<Field>();
		Class<?> clz = clazz;
		while (clz != null) {
			fields.addAll(Arrays.asList(clz.getDeclaredFields()));
			clz = clz.getSuperclass();
		}
		fields.removeIf(field -> field.getAnnotation(ChunkEntry.class) == null);
		Collections.sort(fields, (o1, o2) -> chComp.compare(o1.getAnnotation(ChunkEntry.class), o2.getAnnotation(ChunkEntry.class)));
		
		Set<Integer> orders = new HashSet<>();
		for (Field f : fields) {
			int i = f.getAnnotation(ChunkEntry.class).order();
			if (orders.contains(i)) {
				throw new IllegalStateException("Duplicate orders: " + i + " when reading " + f + " in " + clazz);
			}
			orders.add(i);
		}
		return fields;
	}

	public static Field findField(List<Field> fields, int microId) throws ChunkException {
		List<Field> scanFields = fields.stream().filter(new FieldSizeFilter(microId)).collect(Collectors.toList());
		
//		System.out.println("MicroId " + microId + ": Remaining: " + fields);
		int index = microId & 0x0f;
		if (index == 0) {
			throw new ChunkException("Invalid microId " + microId + " valid fields is " + scanFields);
		}
		
		if (scanFields.size() >= index) {
			return scanFields.get(index - 1);
		}
		
		return null;
	}
	
	public static class FieldSizeFilter implements Predicate<Field> {

		private final int microId;

		public FieldSizeFilter(int size) {
			this.microId = size;
		}
		
		@Override
		public boolean test(Field obj) {
			int length = 0;
			ChunkEntry ch = obj.getAnnotation(ChunkEntry.class);
			if (ch == null)
				return false;
			int size = ch.size();
			if (size == -1)
				size = defaultSizeForFieldType(obj.getType());
			
			int sizeMask = microId & 0xf0;
			
			if (sizeMask == 0xC0) length = 8;
			else if (sizeMask == 0x80) length = 4;
			else if (sizeMask == 0x40) length = 2;
			else if (sizeMask == 0x00) length = 1;
			return size == length;
		}
		
	}

	public static int readInt(DataInputStream stream, int size) throws ChunkException {
		try {
			if (size == 1)
				return stream.read();
			if (size == 2) {
				int a = stream.read();
				int b = stream.read();
				return (a << 8) + b;
			}
			if (size == 4)
				return stream.readInt();
			throw new ChunkException("Illegal size for int: " + size);
		}
		catch (IOException e) {
			throw new ChunkException(e);
		}
	}

	public static int defaultSizeForFieldType(Class<?> type) {
		if (type == boolean.class)
			return 1;
		if (type == int.class)
			return 4;
		if (type == String.class)
			return 8;
		throw new RuntimeException("type does not have specified size: " + type);
	}

	public static Field findChunkField(Class<?> type, String string, int i) {
		List<Field> fields = ChunkUtils.getFields(type);
		try {
			return ChunkUtils.findField(fields, i);
		}
		catch (ChunkException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
