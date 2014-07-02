package net.zomis.rnddb.host;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import net.zomis.rnddb.entities.RndLevel;
import net.zomis.rnddb.entities.RndLevelset;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
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
		int bytesRead;
		char[] readBuffer = new char[32000];
		try {
			while ((bytesRead = in.read(readBuffer)) != -1) {
				String message = new String(readBuffer, 0, bytesRead);
				logger.debug("Incoming " + message.length() + ": " + message);
				messages.offer(message);
			}
		}
		catch (IOException e) {
			logger.error("Error reading from server", e);
		}
	}
	
	private void send(String string) {
		logger.debug("Sending: " + string);
		out.write(string);
		out.flush();
	}

	@Override
	public void saveLevelSet(RndLevelset value) {
		try {
			send("SSET " + mapper.writeValueAsString(value));
			SendMessage result = receive();
			logger.info(result.getMessage());
		}
		catch (JsonProcessingException e) {
			logger.error("Error sending request", e);
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
			SendMessage result = mapper.reader(SendMessage.class).readValue(messages.take());
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
	
}
