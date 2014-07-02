package net.zomis.chunks;

import java.io.IOException;

public class ChunkException extends IOException {

	private static final long	serialVersionUID	= 1318453279974692131L;
	
	public ChunkException(String message, Throwable cause) {
		super(message, cause);
	}

	public ChunkException(String message) {
		super(message);
	}

	public ChunkException(Throwable cause) {
		super(cause);
	}

	public ChunkException() {
		super();
	}

}
