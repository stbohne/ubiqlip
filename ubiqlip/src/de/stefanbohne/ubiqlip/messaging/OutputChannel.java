package de.stefanbohne.ubiqlip.messaging;

public interface OutputChannel<T> {
	void send(T message);
}
