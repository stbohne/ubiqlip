package de.stefanbohne.ubiqlip;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.stefanbohne.ubiqlip.messaging.AuthenticationAccepted;
import de.stefanbohne.ubiqlip.messaging.AuthenticationRejected;
import de.stefanbohne.ubiqlip.messaging.AuthenticationRequest;
import de.stefanbohne.ubiqlip.messaging.ClipboardChanged;
import de.stefanbohne.ubiqlip.messaging.ClipboardData;
import de.stefanbohne.ubiqlip.messaging.Message;
import de.stefanbohne.ubiqlip.messaging.Protocol;
import de.stefanbohne.ubiqlip.messaging.SocketChannel;

public class Ubiqlipboard {
	private final static Logger logger = LoggerFactory.getLogger(Ubiqlipboard.class);
	public final static byte[] emptyPassword = passwordHash("");

	public final String name;
	public final byte[] password;
	public final ServerSocket server;
	public final List<SocketChannel> peers = new ArrayList<SocketChannel>();
	private final UbiqlipboardChangedListener listener;
	private int serial = 0;
	private int priority = 0;
	private ClipboardData currentClipboardData;

	private class Server implements Runnable {
		@Override
		public void run() {
			while (!server.isClosed()) {
				SocketChannel channel = null;
				try {
					channel = new SocketChannel(server.accept());
					logger.info("Connection from {}", channel.socket);
					final SocketChannel c = channel;
					Thread receiveThread = new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								Protocol.send(c);
								Protocol protocol = (Protocol)c.accept();
								logger.debug("Peer protocol version ({})", protocol.version);
								AuthenticationRequest auth = (AuthenticationRequest)c.accept();
								if (!auth.name.equals(name) ||
										(!Arrays.equals(password, emptyPassword) &&
												!Arrays.equals(auth.password, password))) {
									AuthenticationRejected.send(c);
									c.closeWhenFinished();
								} else {
									synchronized (Ubiqlipboard.this) {
										AuthenticationAccepted.send(c, priority + 1);
										ClipboardChanged.send(c, serial, priority, currentClipboardData);
									}
									peerReceiveLoop(c);
								}
							} catch (ClassCastException e) {
								logger.warn("Error accepting peer. Unexpected reply.", e);
								if (c != null)
									c.closeWhenFinished();
							} catch (InterruptedException e) {
								logger.warn("Error accepting peer. ", e);
								if (c != null)
									IOUtils.closeQuietly(c);
							}
						}
					});
					receiveThread.setName(c.socket.toString() + " receive");
					receiveThread.start();
				} catch (IOException e) {
					logger.warn("Error accepting peer.", e);
					IOUtils.closeQuietly(channel);
				}
			}
		}
	}

	public Ubiqlipboard(String name,
			byte[] password,
			ClipboardData clipboardData,
			UbiqlipboardChangedListener listener) throws IOException {
		assert(password.length == 32);
		this.name = name;
		this.password = password;
		this.server = new ServerSocket();
		this.server.bind(null);
		this.currentClipboardData = clipboardData;
		this.listener = listener;
		Thread serverThread = new Thread(new Server());
		serverThread.setName("server");
		serverThread.start();
		logger.info("Started Ubiqlipboard on port {}", this.server.getLocalPort());
	}
	public void close()
	{
		try {
			server.close();
		} catch (IOException e) {
			logger.warn("Error closing server socket", e);
		}
 		for (SocketChannel peer : peers)
			try {
				peer.close();
			} catch (IOException e) {
				logger.warn("Error closing connection to {}", peer, e);
			}
 		peers.clear();
	}

	private void peerReceiveLoop(SocketChannel peer) {
		synchronized (peers) {
			peers.add(peer);
		}
		while (true) {
			try {
				Message message = peer.accept();
				if (message instanceof ClipboardChanged) {
					ClipboardChanged changed = (ClipboardChanged)message;
					logger.debug("ClipboardChanged from {}, serial {}, priority {}",
							new Object[] { peer, changed.serial, changed.priority });
					boolean accept;
					synchronized (this) {
						accept = changed.serial - serial > 0 ||
								(changed.serial == serial && priority > changed.priority);
						if (accept) {
							serial = changed.serial;
							currentClipboardData = ((ClipboardChanged)message).data;
						}
					}
					if (accept)
						listener.uniqlipboardChanged(((ClipboardChanged)message).data);
					else
						logger.debug("old data");
				} else
					logger.warn("Error receiving from {}. Invalid reply.", peer.socket);
			} catch (InterruptedException e) {
				logger.warn("Error receiving from {}", peer.socket, e);
				break;
			}
		}
		IOUtils.closeQuietly(peer);
		synchronized (peers) {
			peers.remove(peer);
		}
	}

	public void connectToPeer(final String address, final int port) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				logger.info("Connecting to {}", address);
				SocketChannel channel = null;
				try {
					channel = new SocketChannel(new Socket(address, port));
					Thread.currentThread().setName(channel.socket.toString() + " receive");
					Protocol.send(channel);
					Protocol protocol = (Protocol)channel.accept();
					logger.debug("Peer protocol version {}.", protocol.version);
					AuthenticationRequest.send(channel, name, password);
					Message reply = channel.accept();
					if (reply instanceof AuthenticationRejected)
						logger.info("Error connecting to {}:{}. Authorization rejected.", address, port);
					else if (!(reply instanceof AuthenticationAccepted)) {
						logger.warn("Error connecting to {}:{}. Invalid reply.", address, port);
						channel.close();
					} else {
						synchronized (Ubiqlipboard.this) {
							priority = Math.max(priority, ((AuthenticationAccepted)reply).priority);
						}
						peerReceiveLoop(channel);
					}
				} catch (UnknownHostException e) {
					logger.warn("Error connecting to {}:{}.", new Object[] { address, port, e });
				} catch (IOException e) {
					logger.warn("Error connecting to {}:{}.", new Object[] { address, port, e });
					IOUtils.closeQuietly(channel);
				} catch (InterruptedException e) {
					logger.warn("Error connecting to {}:{}.", new Object[] { address, port, e });
					IOUtils.closeQuietly(channel);
				}
			}
		}).start();
	}

	public synchronized void setClipboardData(ClipboardData data) {
		if (!currentClipboardData.equals(data)) {
			logger.debug("Clipboard data changed");
			serial += 1;
			currentClipboardData = data;
			for (SocketChannel peer : peers)
				ClipboardChanged.send(peer, serial, priority, currentClipboardData);
		}
	}

	public static byte[] passwordHash(String password) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		md.update("Ubiqlip".getBytes());
		md.update(password.getBytes());
		return md.digest();
	}
}
