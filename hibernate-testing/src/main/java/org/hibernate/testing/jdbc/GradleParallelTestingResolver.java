/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.jdbc;

import org.hibernate.cfg.AvailableSettings;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.Map;
import java.util.Properties;

/**
 * JDBC config resolver for parallel tests (uses a Reentrant File System Based Sequence).
 *
 * @author Loïc Lefèvre
 */
public class GradleParallelTestingResolver {

	private static final String JDBC_USER_CONNECTION_PROPERTY = "user";

	/**
	 * Pattern that will be replaced by the forked JVM ID used for tests by Gradle.
	 */
	private static final String GRADLE_WORKER_PATTERN = "$worker";

	/**
	 * @see <a href="https://docs.gradle.org/current/userguide/java_testing.html#sec:test_execution">Gradle Test Execution</a>
	 */
	private static final String GRADLE_WORKER_ID = "org.gradle.test.worker";

	private static final String GRADLE_MAXIMUM_PARALLEL_FORKS = "maxParallelForks";

	public static void resolve(final Properties connectionProps) {
		if ( connectionProps != null ) {
			// If Gradle parallel testing is enabled (maxParallelForks > 1)
			final String user = connectionProps.getProperty( JDBC_USER_CONNECTION_PROPERTY );
			if ( user != null && user.contains( GRADLE_WORKER_PATTERN ) ) {
				connectionProps.put( JDBC_USER_CONNECTION_PROPERTY,
						user.replace( GRADLE_WORKER_PATTERN, String.valueOf( getWorkerID() ) ) );
			}
			final String url = connectionProps.getProperty( AvailableSettings.URL );
			if ( url != null && url.contains( GRADLE_WORKER_PATTERN ) ) {
				connectionProps.put( AvailableSettings.URL,
						url.replace( GRADLE_WORKER_PATTERN, String.valueOf( getWorkerID() ) ) );
			}
		}
	}

	public static void resolveFromSettings(final Properties settingsProps) {
		if ( settingsProps != null ) {
			// If Gradle parallel testing is enabled (maxParallelForks > 1)
			final String user = settingsProps.getProperty( AvailableSettings.USER );
			if ( user != null && user.contains( GRADLE_WORKER_PATTERN ) ) {
				settingsProps.put( AvailableSettings.USER,
						user.replace( GRADLE_WORKER_PATTERN, String.valueOf( getWorkerID() ) ) );
			}
			final String url = settingsProps.getProperty( AvailableSettings.URL );
			if ( url != null && url.contains( GRADLE_WORKER_PATTERN ) ) {
				settingsProps.put( AvailableSettings.URL,
						url.replace( GRADLE_WORKER_PATTERN, String.valueOf( getWorkerID() ) ) );
			}
		}
	}

	public static void resolveFromSettings(final Map<String, Object> settingsProps) {
		if ( settingsProps != null ) {
			// If Gradle parallel testing is enabled (maxParallelForks > 1)
			final String user = (String) settingsProps.get( AvailableSettings.USER );
			if ( user != null && user.contains( GRADLE_WORKER_PATTERN ) ) {
				settingsProps.put( AvailableSettings.USER,
						user.replace( GRADLE_WORKER_PATTERN, String.valueOf( getWorkerID() ) ) );
			}
			final String url = (String) settingsProps.get( AvailableSettings.URL );
			if ( url != null && url.contains( GRADLE_WORKER_PATTERN ) ) {
				settingsProps.put( AvailableSettings.URL,
						url.replace( GRADLE_WORKER_PATTERN, String.valueOf( getWorkerID() ) ) );
			}
		}
	}

	public static String resolveUrl(final String url) {
		return url.replace( GRADLE_WORKER_PATTERN, String.valueOf( getWorkerID() ) );
	}

	public static String resolveUsername(final String username) {
		return username.replace( GRADLE_WORKER_PATTERN, String.valueOf( getWorkerID() ) );
	}

	/**
	 * Retrieves the worker ID based on the Gradle properties.
	 * Whenever a Gradle task is running in parallel, Gradle will fork JVMs and assign
	 * a monotonic sequence number to it (it may not start with 1, and it can have "holes") which can be
	 * retrieved using the system property {@link #GRADLE_WORKER_ID}.
	 * <p>
	 * <b>To cope with the Gradle sequence number limitations ("holes"), we use a <i>reentrant file system based sequence</i>.</b>
	 * </p>
	 *
	 * @return an integer between 1 and {@link #GRADLE_MAXIMUM_PARALLEL_FORKS} system property (inclusive)
	 */
	private static int getWorkerID() {
		// maximum degree of parallelization
		final int maxParallelForks = Integer.parseInt( System.getProperty( GRADLE_MAXIMUM_PARALLEL_FORKS, "1" ) );

		// target JDBC user 1 if no parallel tests enabled
		if(maxParallelForks == 1) {
			return 1;
		}

		// current Gradle worker ID (can be for the same task: 157, 158, <hole>, 160, 161
		final long id = Long.parseLong( System.getProperty( GRADLE_WORKER_ID, "1" ) );

		// sequence file will be stored within the target sub-folder of gradle modules with parallel tests enabled
		// we use the parent process handle because Gradle forks JVMs
		final File sequenceFile = new File( new File( System.getProperty( "user.dir" ), "target" ),
				String.format( "%d.sequence", ProcessHandle.current().parent().get().pid() ) );

		// we'll rely on file system locks
		try (RandomAccessFile file = new RandomAccessFile( sequenceFile, "rws" )) {
			FileChannel fc = file.getChannel();

			if ( file.length() > 0 ) {
				// read full content and try searching for my own id
				final ByteBuffer bb = ByteBuffer.allocate( Long.BYTES * maxParallelForks );
				do {
					try (FileLock lock = fc.lock( 0L, Long.MAX_VALUE, true )) {
						final int bytesRead = fc.read( bb, 0 );
						final LongBuffer lb = bb.rewind().asLongBuffer();

						for ( int i = 0; i < lb.limit(); i++ ) {
							if ( lb.get( i ) == id ) {
								return i + 1;
							}
						}
						// could not find our own id inside the file, exit read loop!
						break;
					}
					catch (OverlappingFileLockException e) {
						try {
							Thread.sleep( 50L );
						}
						catch (InterruptedException ignored) {
						}
					}
				}
				while ( true );
			}

			// write lock
			do {
				try {
					try (FileLock lock = fc.lock()) {
						long length = file.length();
						if ( length >= (long) Long.BYTES * maxParallelForks ) {
							fc.truncate( 0 );
							length = 0;
						}
						file.seek( length );
						final ByteBuffer bb = ByteBuffer.allocate( Long.BYTES );
						bb.asLongBuffer().put( new long[] {id} );
						final int bytesWritten = fc.write( bb );
						fc.force( true );

						return (int) ((length / Long.BYTES) + 1);
					}
				}
				catch (OverlappingFileLockException e) {
					try {
						Thread.sleep( 50L );
					}
					catch (InterruptedException ignored) {
					}
				}
			}
			while ( true );
		}
		catch (IOException ioe) {
			throw new RuntimeException( "An error occurred when computing worker ID", ioe );
		}
	}
}
