package net.zomis.rnddb.host;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import net.zomis.rnddb.entities.RndFile;
import net.zomis.rnddb.entities.RndLevel;
import net.zomis.rnddb.entities.RndLevelset;
import net.zomis.utils.ZSubstr;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RndDbServer implements AutoCloseable, RndDbSource {

	private static final Logger logger = LogManager.getLogger(RndDbServer.class);
	private final ServerSocket server;
	private final RndDbSource source;
	private final RootPathFinder rootPath;
	
	public RndDbServer(int port, RootPathFinder rootPath, RndDbSource source) throws IOException {
		server = new ServerSocket(port);
		this.rootPath = rootPath;
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
		private final BlockingQueue<String> incoming = new LinkedBlockingQueue<>();

		public ClientListener(Socket client) throws IOException {
			this.client = client;
			this.out = new PrintWriter(client.getOutputStream(), true);
			new Thread(this::listen).start();
		}
		
		@Override
		public void run() {
			try {
				while (true) {
					String message = incoming.take();
					logger.debug("Server Received: " + message);
					String messageType = ZSubstr.substr(message, 0, 4);
					SendMessage send = new SendMessage();
					String param = ZSubstr.substr(message, 5);
					switch (messageType) {
						case "LOAD":
							send.setSets(source.getAllLevelSets());
							send.getSets().forEach(lset -> lset.clearLevelsForSending());
							send(send);
							break;
//						case "LVEL":
//							send.setLevels(Arrays.asList(source.getLevel(param)));
//							send(send);
//							break;
						case "LSET":
							send.setSets(Arrays.asList(source.getLevelSet(param)));
							send(send);
							break;
						case "SSET":
//							 * Client: SSET
//							 * Client: `RndLevelset` JSON
//							 * Client: JEND
//							 * Client: `RndFile` JSON data
//							 * Client: hex
//							 * Client: hex...
//							 * Client: FEND
//							 * Client: `RndFile` JSON data
//							 * Client: hex
//							 * Client: hex...
//							 * Client: DEND
//							 * Server: Response message
							
							RndReadStream stream = RndReadStream.readUntilEnd(incoming);
							RndLevelset levelset = stream.readJSON(RndLevelset.class);
							levelset.clearLevelsForSending();
							levelset.setRootPath(new File(rootPath.getRootPath(), "levels/uploaded"));
							List<RndFileWithData> files = stream.readFilesWithData(levelset);
							for (RndFileWithData file : files) {
								file.getFile().setLevelset(levelset);
								levelset.addFile(file.getFile());
								file.saveToDisc();
							}
							source.saveLevelSet(levelset);
							
							send.setMessage("Saved");
							send(send);
							break;
						case "DLOD":
//							RndLevelset levelset = source.getLevelSet();
							sendFiles(param); //levelset, source.getFilesInSet(levelset.getId()));
							break;
						default:
							logger.warn("Unknown message: " + message);
							send.setMessage("Unknown message: " + messageType);
							send(send);
					}
				}
			}
			catch (InterruptedException e) {
				logger.warn("Interrupted, stopped listening for incomings.");
			}
			catch (IOException e) {
				logger.error("IOException", e);
			}
		}

		private void listen() {
			logger.debug("Server is now listening on " + client);
			String SPLIT_STRING = String.valueOf((char) 0);
			int bytesRead;
			char[] readBuffer = new char[32000];
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				while ((bytesRead = in.read(readBuffer)) != -1) {
					String message = new String(readBuffer, 0, bytesRead);
					for (String mess : message.split(SPLIT_STRING)) {
						logger.debug("Incoming " + mess.length() + ": " + mess);
						incoming.offer(mess);
					}
					
				}
			}
			catch (IOException e) {
				logger.error("Error reading from server", e);
			}
		}
		
		public void sendFiles(String md5) throws IOException {
			RndLevelset levelset = source.getLevelSet(md5);
			if (rootPath != null) {
				levelset.setRootPath(rootPath.getRootPath());
			}
			List<RndFile> list = source.getFilesInSet(levelset.getId());
				
			RndWriteStream write = new RndWriteStream(out);
			write.sendFiles(levelset, list);
		}

		private void send(SendMessage send) {
			try {
				send(mapper.writeValueAsString(send));
			}
			catch (JsonProcessingException e) {
				logger.error(e.getMessage(), e);
				throw new RuntimeException(e);
			}
			send("DEND");
		}

		private void send(String writeValueAsString) {
			logger.debug("Sending " + writeValueAsString.length() + ": " + writeValueAsString);
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
	public RndLevelset saveLevelSet(RndLevelset value) {
		return source.saveLevelSet(value);
	}

	@Override
	public RndLevelset getLevelSet(String md5) {
		return source.getLevelSet(md5);
	}

	@Override
	@Deprecated
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
