package de.stefanbohne.ubiqlip.messaging;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketChannel<T extends Message> implements InputChannel<T>, OutputChannel<T>, Closeable {
	final static Logger logger = LoggerFactory.getLogger(SocketChannel.class);
	public final Socket socket;
	private final BlockingQueue<T> sendQueue = new LinkedBlockingQueue<T>();
	private final S11nInput in;
	public SocketChannel(Socket socket) throws IOException {
		this.socket = socket;
		Thread sendThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					final S11nOutput out = new S11nOutput(SocketChannel.this.socket.getOutputStream());
					while (SocketChannel.this.socket.isConnected()) {
						T message = sendQueue.take();
						if (message == null) {
							close();
							break;
						}
						logger.debug("Sending {} to {}", message, SocketChannel.this.socket);
						out.writeObject(message);
					}
				} catch (Exception e) {
					logger.warn("", e);
				}
			}
		});
		sendThread.setName(socket.toString() + " send");
		sendThread.start();
		in = new S11nInput(socket.getInputStream());
	}
	public void closeWhenFinished() {
		sendQueue.add(null);
	}
	@Override
	public void close() throws IOException {
		socket.close();
	}
	@Override
	public void send(T message) {
		try {
			sendQueue.put(message);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public T accept() throws InterruptedException {
		try {
			@SuppressWarnings("unchecked")
			T message = (T)in.readObject();
			logger.debug("Received {} from {}", message, SocketChannel.this.socket);
			return message;
		} catch (IOException e) {
			logger.warn("", e);
			throw new InterruptedException();
		}
	}
}
