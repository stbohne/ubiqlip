package de.stefanbohne.ubiqlip.messaging;

import java.io.IOException;

import org.apache.commons.lang.ArrayUtils;

@S11nInfo(id=2)
public class AuthenticationRequest extends Message {
	public static void send(OutputChannel<? super AuthenticationRequest> channel, String name, byte[] password) {
		channel.send(new AuthenticationRequest(name, password));
	}

	public final String name;
	public final byte[] password;

	public AuthenticationRequest(String name, byte[] password) {
		assert(password.length == 32);
		this.name = name;
		this.password = password;
	}
	public AuthenticationRequest(S11nInput in) throws IOException {
		this.name = in.readString();
		this.password = in.readFixedByteArray(32);
	}
	@Override
	public void s5ize(S11nOutput out) throws IOException {
		out.writeString(name);
		out.writeFixedByteArray(password, 32);
	}
	@Override
	public String toString() {
		return String.format("AuthenticationRequest(%s, %s)", name, ArrayUtils.toString(password));
	}
}
