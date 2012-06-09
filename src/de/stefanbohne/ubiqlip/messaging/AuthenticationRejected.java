package de.stefanbohne.ubiqlip.messaging;

import java.io.IOException;

@S11nInfo(id=4)
public class AuthenticationRejected extends Message {
	public static void send(OutputChannel channel) {
		channel.send(new AuthenticationRejected());
	}

	public AuthenticationRejected() {}
	public AuthenticationRejected(S11nInput in) {}
	@Override
	public void s5ize(S11nOutput out) throws IOException {}
	@Override
	public String toString() {
		return "AuthenticationRejected";
	}
}
