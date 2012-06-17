package de.stefanbohne.ubiqlip.messaging;

import java.io.IOException;

@S11nInfo(id=3)
public class AuthenticationAccepted extends Message {
	public static void send(OutputChannel<? super AuthenticationAccepted> channel, int priority) {
		channel.send(new AuthenticationAccepted(priority));
	}

	public final int priority;

	public AuthenticationAccepted(int priority) {
		this.priority = priority;
	}
	public AuthenticationAccepted(S11nInput in) throws IOException {
		this.priority = in.readInt();
	}
	@Override
	public void s5ize(S11nOutput out) throws IOException {
		out.writeInt(this.priority);
	}
	@Override
	public String toString() {
		return String.format("AuthenticationAccepted(%s)", priority);
	}
}
