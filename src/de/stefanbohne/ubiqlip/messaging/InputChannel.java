package de.stefanbohne.ubiqlip.messaging;

public interface InputChannel {
	Message accept() throws InterruptedException;
}
