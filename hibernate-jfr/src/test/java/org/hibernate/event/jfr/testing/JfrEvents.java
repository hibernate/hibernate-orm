/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.jfr.testing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

/**
 * Captures JFR events emitted during a test.
 *
 * <p>Uses the dump-and-read approach: each {@link #events()} call dumps the running
 * {@link Recording} to a temporary file and reads the events via
 * {@link RecordingFile#readAllEvents(Path)}.
 * {@link #reset()} discards accumulated events by stopping the current recording
 * and starting a fresh one with the same enabled events.</p>
 *
 * <p>Lifecycle is managed by {@link JfrEventTestExtension}:
 * {@link #configure(List)} is called before each test,
 * {@link #cleanup()} after each test.</p>
 */
public class JfrEvents {

	private Recording recording;
	private List<String> enabledEventNames;
	private final List<Path> tempFiles = new ArrayList<>();

	/**
	 * Configures and starts recording the given event types.
	 * Called by {@link JfrEventTestExtension} before each test method.
	 */
	void configure(final List<String> eventNames) {
		this.enabledEventNames = new ArrayList<>( eventNames );
		startNewRecording();
	}

	/**
	 * Discards all previously recorded events and begins a fresh recording
	 * with the same enabled event types.
	 */
	public void reset() {
		checkActiveRecording();
		stopAndCloseRecording();
		startNewRecording();
	}

	/**
	 * Returns a stream of all events recorded since the last {@link #reset()}
	 * (or since {@link #configure(List)} if reset was never called).
	 *
	 * <p>The recording remains running — this method dumps a snapshot to a
	 * temporary file without stopping the recording, so subsequent calls
	 * return the same events plus any new ones.</p>
	 */
	public Stream<RecordedEvent> events() {
		checkActiveRecording();
		try {
			final Path tempFile = Files.createTempFile( "hibernate-jfr-test-", ".jfr" );
			tempFiles.add( tempFile );
			recording.dump( tempFile );
			return RecordingFile.readAllEvents( tempFile ).stream();
		}
		catch (IOException e) {
			throw new UncheckedIOException( "Failed to dump or read JFR recording", e );
		}
	}

	/**
	 * Stops the recording and deletes temporary files.
	 * Called by {@link JfrEventTestExtension} after each test method.
	 * Idempotent — safe to call multiple times.
	 */
	void cleanup() {
		stopAndCloseRecording();
		for ( final Path tempFile : tempFiles ) {
			try {
				Files.deleteIfExists( tempFile );
			}
			catch (IOException ignored) {
				// best-effort cleanup — temp files will be removed by the OS eventually
			}
		}
		tempFiles.clear();
	}

	private void checkActiveRecording() {
		if ( recording == null ) {
			throw new IllegalStateException(
					"No active JFR recording — was @JfrEventTest applied to the test class?" );
		}
	}

	private void startNewRecording() {
		recording = new Recording();
		for ( final String eventName : enabledEventNames ) {
			recording.enable( eventName );
		}
		recording.start();
	}

	private void stopAndCloseRecording() {
		if ( recording != null ) {
			try {
				recording.stop();
			}
			catch (IllegalStateException ignored) {
				// recording was already stopped — not an error
			}
			recording.close();
			recording = null;
		}
	}
}
