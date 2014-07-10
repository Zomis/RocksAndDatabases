package net.zomis.rnddb.host;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import net.zomis.rnddb.entities.RndFile;
import net.zomis.rnddb.entities.RndLevelset;
import net.zomis.utils.MD5Util;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RndReadStream {
	private static final Logger logger = LogManager.getLogger(RndReadStream.class);
	private final List<String> datas;
	private final ObjectMapper mapper = new ObjectMapper();

	public RndReadStream(List<String> data) {
		this.datas = data;
	}

	public <T> T readJSON(Class<T> class1) throws JsonParseException, JsonMappingException, IOException {
		StringBuilder str = new StringBuilder();
		Iterator<String> it = datas.iterator();
		while (it.hasNext()) {
			String data = it.next();
			it.remove();
			if (data.startsWith("JEND")) {
				String json = str.toString();
				return mapper.readValue(json, class1);
			}
			else {
				str.append(data);
			}
		}
		throw new IllegalStateException("Finished reading datas but no JEND found.");
	}
	
	public static RndReadStream readUntilEnd(BlockingQueue<String> incoming) throws InterruptedException {
		// TODO: Make it possible to process things while reading them. To use less resources.
		List<String> takes = new ArrayList<>();
		String take;
		while (true) {
			take = incoming.take();
			logger.trace("Take: " + take);
			if (take.endsWith("DEND")) {
				break;
			}
			takes.add(take);
		}
		return new RndReadStream(takes);
	}

	public List<RndFileWithData> readFilesWithData(RndLevelset levelset) throws IOException {
		RndFile file = null;
		List<RndFileWithData> list = new ArrayList<>();
		StringBuilder fileData = new StringBuilder();
		for (String data : datas) {
			if (file == null) {
				file = mapper.reader(RndFile.class).readValue(data);
				file.setLevelset(levelset);
				continue;
			}

			if (data.startsWith("FEND")) {
				String hex = fileData.toString();
				fileData = new StringBuilder();
				
				list.add(new RndFileWithData(file, MD5Util.hex2byte(hex)));
				file = null;
			}
			else fileData.append(data);
		}
		return list;
	}
	
	
	
}
