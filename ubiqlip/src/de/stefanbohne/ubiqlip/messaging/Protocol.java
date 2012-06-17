package de.stefanbohne.ubiqlip.messaging;

import java.io.IOException;

@S11nInfo(id=1)
public class Protocol extends Message {
	public final static int VERSION = 1;

	public static void send(OutputChannel<? super Protocol> channel) {
		channel.send(new Protocol());
	}

	public final int version;

	public Protocol(S11nInput in) throws IOException {
		this.version = in.readInt();
	}
	public Protocol() {
		this.version = VERSION;
	}
	@Override
	public void s5ize(S11nOutput out) throws IOException {
		out.writeInt(version);
	}
	@Override
	public String toString() {
		return String.format("Protocol(%s)", version);
	}
}
