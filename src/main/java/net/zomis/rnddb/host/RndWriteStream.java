package net.zomis.rnddb.host;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;

import net.zomis.rnddb.entities.RndFile;
import net.zomis.rnddb.entities.RndLevelset;
import net.zomis.utils.MD5Util;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RndWriteStream {
	private static final Logger logger = LogManager.getLogger(RndWriteStream.class);
	private final ObjectMapper mapper = new ObjectMapper();
	private final PrintWriter out;
	
	public RndWriteStream(PrintWriter out) {
		this.out = out;
	}
	
	public void sendFiles(RndLevelset levelset, List<RndFile> list) throws IOException {
		send(mapper.writeValueAsString(levelset));
		send("JEND");
		for (RndFile file : list) {
			file.setLevelset(levelset);
			File f = file.getFile();
			byte[] bytes = Files.readAllBytes(f.toPath());

			send(mapper.writeValueAsString(file));
			send(MD5Util.toHEX(bytes, false));
			send("FEND");
		}
		send("DEND");
	}

	private void send(String string) {
		logger.debug("Sending " + string.length() + ": " + string);
		out.write(string + (char) 0);
		out.flush();
	}
	
}
