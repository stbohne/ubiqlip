package de.stefanbohne.ubiqlip.messaging;

public interface InputChannel<T> {
	T accept() throws InterruptedException;
}
