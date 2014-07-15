package net.zomis.rnddb.host;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import net.zomis.rnddb.entities.RndFile;
import net.zomis.rnddb.entities.RndLevel;
import net.zomis.rnddb.entities.RndLevelset;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RndDbClient implements RndDbSource {

	private static final Logger logger = LogManager.getLogger(RndDbClient.class);
	private final Socket socket;
	private final PrintWriter out;
	private final BufferedReader in;
	private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
	private final ObjectMapper mapper = new ObjectMapper();
	
	public RndDbClient(String host, int port) throws UnknownHostException, IOException {
		socket = new Socket(host, port);
		out = new PrintWriter(socket.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		new Thread(this::listen).start();
	}
	
	private void listen() {
		String splitString = String.valueOf((char) 0);
		int bytesRead;
		char[] readBuffer = new char[32000];
		try {
			while ((bytesRead = in.read(readBuffer)) != -1) {
				String message = new String(readBuffer, 0, bytesRead);
				for (String string : message.split(splitString)) {
					logger.debug("Incoming " + string.length() + ": " + string.substring(0, Math.min(100, string.length())));
					messages.offer(string);
				}
				
			}
		}
		catch (IOException e) {
			logger.error("Error reading from server", e);
		}
	}
	
	private void send(String string) {
		logger.debug("Client Sending: " + string);
		out.write(string);
		out.flush();
	}

	public void downloadLevelSet(File root, RndLevelset value) throws InterruptedException, IOException {
		send("DLOD " + value.getChecksum());
		
		RndReadStream read = RndReadStream.readUntilEnd(messages);
		
		RndLevelset levelset = read.readJSON(RndLevelset.class);
		levelset.setRootPath(root);
		
		List<RndFileWithData> files = read.readFilesWithData(levelset);
		for (RndFileWithData file : files) { 
			file.saveToDisc();// throws exception and cannot use lambdas
		}
	}
	
	@Deprecated
	private List<String> takeUntilEnd() throws InterruptedException {
		List<String> takes = new ArrayList<>();
		String take;
		while (true) {
			take = messages.take();
			logger.trace("Take: " + take);
			if (take.endsWith("DEND")) {
				break;
			}
			takes.add(take);
		}
		return takes;
	}

	@Override
	public RndLevelset saveLevelSet(RndLevelset value) {
		try {
		 	RndWriteStream write = new RndWriteStream(out);
			send("SSET");
			write.sendFiles(value, value.getLevels());
			
			SendMessage result = receive();
			logger.info(result.getMessage());
			return null; // result.getSets().stream().findFirst().orElse(null);
		}
		catch (IOException e) {
			logger.error("Error sending request", e);
			return null;
		}
	}

	@Override
	public RndLevelset getLevelSet(String md5) {
		send("LSET " + md5);
		SendMessage result = receive();
		return result.getSets().stream().findFirst().orElse(null);
	}

	@Override
	public List<RndLevelset> getAllLevelSets() {
		send("LOAD");
		SendMessage result = receive();
		return result.getSets();
	}

	private SendMessage receive() {
		try {
			String value = String.join("", takeUntilEnd());
			SendMessage result = mapper.reader(SendMessage.class).readValue(value);
			return result;
		}
		catch (InterruptedException | IOException e) {
			logger.error("Exception occoured when waiting for response from server", e);
		}
		return null;
	}

	@Override
	public RndLevel getLevel(String md5) {
		send("LVEL " + md5);
		SendMessage result = receive();
		return result.getLevels().stream().findFirst().orElse(null);
	}

	@Override
	public void close() {
		try {
			this.socket.close();
		}
		catch (IOException e) {
			logger.error("", e);
		}
	}

	@Override
	public List<RndFile> getFilesInSet(Long id) {
		throw new UnsupportedOperationException();
	}
	
}
