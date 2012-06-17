package de.stefanbohne.ubiqlip.messaging;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class S11nInput {
	private final static Map<Integer, Class<? extends S11nable>> idMap =
			new HashMap<Integer, Class<? extends S11nable>>();
	private static void register(Class<? extends S11nable> cls) {
		int id = cls.getAnnotation(S11nInfo.class).id();
		assert(!idMap.containsKey(id));
		idMap.put(id, cls);
	}
	static {
		register(Protocol.class);
		register(AuthenticationRequest.class);
		register(AuthenticationAccepted.class);
		register(AuthenticationRejected.class);
		register(ClipboardChanged.class);
	}

	private final DataInputStream in;
	public S11nInput(InputStream stream) {
		this.in = new DataInputStream(stream);
	}

	public byte readByte() throws IOException {
		return in.readByte();
	}
	public int readInt() throws IOException {
		return in.readInt();
	}
	public String readString() throws IOException {
		return in.readUTF();
	}
	public byte[] readFixedByteArray(int size) throws IOException {
		byte[] result = new byte[size];
		in.readFully(result);
		return result;
	}
	public byte[] readByteArray() throws IOException {
		return readFixedByteArray(readInt());
	}
	public InetAddress readInetAddress() throws IOException {
		return InetAddress.getByAddress(readFixedByteArray(4));
	}
	public InetSocketAddress readInetSocketAddress() throws IOException {
		return new InetSocketAddress(readInetAddress(), readInt());
	}
	public S11nable readObject() throws IOException {
		int id = readInt();
		if (!idMap.containsKey(id))
			throw new InvalidObjectException(String.format("Invalid id %s", id));
		try {
			return idMap.get(id).getConstructor(S11nInput.class).newInstance(this);
		} catch (Exception e) {
			if (e instanceof InvocationTargetException &&
					((InvocationTargetException)e).getTargetException() instanceof IOException)
				throw (IOException)((InvocationTargetException)e).getTargetException();
			else
				throw new RuntimeException(e);
		}
	}
}
