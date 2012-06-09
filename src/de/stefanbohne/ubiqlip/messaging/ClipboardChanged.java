package de.stefanbohne.ubiqlip.messaging;

import java.io.IOException;

@S11nInfo(id=5)
public class ClipboardChanged extends Message {
	public static void send(OutputChannel channel, int serial, int priority, ClipboardData data) {
		channel.send(new ClipboardChanged(serial, priority, data));
	}

	public final int serial;
	public final int priority;
	public final ClipboardData data;

	public ClipboardChanged(int serial, int priority, ClipboardData data) {
		this.serial = serial;
		this.priority = priority;
		this.data = data;
	}
	public ClipboardChanged(S11nInput in) throws IOException {
		this.serial = in.readInt();
		this.priority = in.readInt();
		this.data = new ClipboardData(in);
	}
	@Override
	public void s5ize(S11nOutput out) throws IOException {
		out.writeInt(serial);
		out.writeInt(priority);
		this.data.s5ize(out);
	}
	@Override
	public String toString() {
		return String.format("ClipboardChanged(%s, %s, %s)", serial, priority, data);
	}
}
