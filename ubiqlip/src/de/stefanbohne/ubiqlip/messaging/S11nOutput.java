package de.stefanbohne.ubiqlip.messaging;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class S11nOutput {
	private final DataOutputStream out;
	public S11nOutput(OutputStream stream) {
		this.out = new DataOutputStream(stream);
	}
	public void writeByte(byte v) throws IOException {
		out.writeByte(v);
	}
	public void writeInt(int v) throws IOException {
		out.writeInt(v);
	}
	public void writeString(String v) throws IOException {
		out.writeUTF(v);
	}
	public void writeInetAddress(InetAddress v) throws IOException {
		assert(v.getAddress().length == 4);
		out.write(v.getAddress());
	}
	public void writeInetSocketAddress(InetSocketAddress v) throws IOException {
		writeInetAddress(v.getAddress());
		writeInt(v.getPort());
	}
	public void writeObject(S11nable v) throws IOException {
		writeInt(v.getClass().getAnnotation(S11nInfo.class).id());
		v.s5ize(this);
	}
	public void writeFixedByteArray(byte[] value, int length) throws IOException {
		assert(value.length == length);
		out.write(value);
	}
	public void writeByteArray(byte[] value) throws IOException {
		out.writeInt(value.length);
		writeFixedByteArray(value, value.length);
	}
}
