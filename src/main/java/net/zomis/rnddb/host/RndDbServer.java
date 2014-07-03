package net.zomis.rnddb.host;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import net.zomis.rnddb.entities.RndFile;
import net.zomis.rnddb.entities.RndLevel;
import net.zomis.rnddb.entities.RndLevelset;
import net.zomis.utils.MD5Util;
import net.zomis.utils.ZSubstr;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RndDbServer implements AutoCloseable, RndDbSource {

	private static final Logger logger = LogManager.getLogger(RndDbServer.class);
	private final ServerSocket server;
	private final RndDbSource source;
	
	public RndDbServer(RndDbSource source) throws IOException {
		server = new ServerSocket(4242);
		this.source = source;
		new Thread(this::listen).start();
	}
	
	private void listen() {
		try {
			while (true) {
				logger.info("Waiting for client...");
				Socket client = server.accept();
				new Thread(new ClientListener(client)).start();
			}
		}
		catch (IOException e) {
			logger.error("Failed to accept client", e);
		}
	}
	
	private class ClientListener implements Runnable {
		
		private final Socket client;
		private final PrintWriter out;
		private final ObjectMapper mapper = new ObjectMapper();

		public ClientListener(Socket client) throws IOException {
			this.client = client;
			this.out = new PrintWriter(client.getOutputStream(), true);
		}

		@Override
		public void run() {
			int bytesRead;
			char[] readBuffer = new char[32000];
			logger.debug("Server is now listening on " + client);
			
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				while ((bytesRead = in.read(readBuffer)) != -1) {
					String message = new String(readBuffer, 0, bytesRead);
					logger.debug("Server Received: " + message);
					String messageType = ZSubstr.substr(message, 0, 4);
					SendMessage send = new SendMessage();
					String param = ZSubstr.substr(message, 5);
					switch (messageType) {
						case "LOAD":
							send.setSets(source.getAllLevelSets());
							send(send);
							break;
						case "LVEL":
							send.setLevels(Arrays.asList(source.getLevel(param)));
							send(send);
							break;
						case "LSET":
							send.setSets(Arrays.asList(source.getLevelSet(param)));
							send(send);
							break;
						case "SSET":
							source.saveLevelSet(mapper.reader(RndLevelset.class).readValue(param));
							send.setMessage("Saved(?)");
							send(send);
							break;
						case "DLOD":
							RndLevelset levelset = source.getLevelSet(param);
							sendFiles(levelset, source.getFilesInSet(levelset.getId()));
							break;
						default:
							logger.warn("Unknown message: " + message);
							send.setMessage("Unknown message: " + messageType);
							send(send);
					}
				}
			}
			catch (IOException e) {
				logger.error("Error reading from server", e);
			}
		}

		public void sendFiles(RndLevelset levelset, List<RndFile> list) {
			try {
				send(mapper.writeValueAsString(levelset));
				
				for (RndFile file : list) {
					File f = file.getFile();
					byte[] bytes = Files.readAllBytes(f.toPath());
					
					send(mapper.writeValueAsString(file));
					send(MD5Util.toHEX(bytes, false));
					send("FEND");
				}
				send("DEND");
			}
			catch (IOException e) {
				logger.error("Unable to send file", e);
			}
		}

		private void send(SendMessage send) throws JsonProcessingException {
			send(mapper.writeValueAsString(send));
		}

		private void send(String writeValueAsString) {
			logger.debug("Server sending " + writeValueAsString.length() + ": " + writeValueAsString);
			out.write(writeValueAsString + (char) 0);
			out.flush();
		}
	}

	@Override
	public void close() {
		try {
			server.close();
		}
		catch (IOException e) {
			logger.error("Error closing server", e);
		}
		source.close();
	}

	@Override
	public void saveLevelSet(RndLevelset value) {
		source.saveLevelSet(value);
	}

	@Override
	public RndLevelset getLevelSet(String md5) {
		return source.getLevelSet(md5);
	}

	@Override
	public RndLevel getLevel(String md5) {
		return source.getLevel(md5);
	}

	@Override
	public List<RndLevelset> getAllLevelSets() {
		return source.getAllLevelSets();
	}

	@Override
	public List<RndFile> getFilesInSet(Long id) {
		return source.getFilesInSet(id);
	}
	
}
