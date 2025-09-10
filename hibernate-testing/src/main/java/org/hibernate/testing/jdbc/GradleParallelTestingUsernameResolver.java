/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.jdbc;

import org.hibernate.cfg.AvailableSettings;

import java.util.Map;
import java.util.Properties;

/**
 * JDBC username resolver for parallel tests.
 *
 * @author Loïc Lefèvre
 */
public class GradleParallelTestingUsernameResolver {

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
		if( connectionProps != null ) {
			// If Gradle parallel testing is enabled (maxParallelForks > 1)
			final String user = connectionProps.getProperty( JDBC_USER_CONNECTION_PROPERTY );
			if ( user.contains( GRADLE_WORKER_PATTERN ) ) {
				connectionProps.put( JDBC_USER_CONNECTION_PROPERTY,
						user.replace( GRADLE_WORKER_PATTERN, String.valueOf( getRunningID() ) ) );
			}
		}
	}

	public static void resolveFromSettings(final Properties settingsProps) {
		if( settingsProps != null ) {
			// If Gradle parallel testing is enabled (maxParallelForks > 1)
			final String user = settingsProps.getProperty( AvailableSettings.USER );
			if ( user.contains( GRADLE_WORKER_PATTERN ) ) {
				settingsProps.put( AvailableSettings.USER,
						user.replace( GRADLE_WORKER_PATTERN, String.valueOf( getRunningID() ) ) );
			}
		}
	}

	public static void resolveFromSettings(final Map<String, Object> settingsProps) {
		if( settingsProps != null ) {
			// If Gradle parallel testing is enabled (maxParallelForks > 1)
			final String user = (String) settingsProps.get( AvailableSettings.USER );
			if ( user.contains( GRADLE_WORKER_PATTERN ) ) {
				settingsProps.put( AvailableSettings.USER,
						user.replace( GRADLE_WORKER_PATTERN, String.valueOf( getRunningID() ) ) );
			}
		}
	}

	public static String resolveUsername(String username) {
		if(username != null) {
			if ( username.contains( GRADLE_WORKER_PATTERN ) ) {
				return username.replace( GRADLE_WORKER_PATTERN, String.valueOf( getRunningID() ) );
			}
			else {
				return username;
			}
		} else {
			throw new IllegalArgumentException("Username must not be null!");
		}
	}

	/**
	 * Create a JVM Running ID based on the Gradle properties.
	 * Whenever a task is running in parallel, Gradle will fork JVMs and assign
	 * a monotonic sequence number to it (it may not start with 1) which can be
	 * retrieved using the system property {@link #GRADLE_WORKER_ID}.
	 *
	 * @return an integer between 1 and {@link #GRADLE_MAXIMUM_PARALLEL_FORKS} system property (inclusive)
	 */
	private static int getRunningID() {
		// enable parallelization of up to GRADLE_MAXIMUM_PARALLEL_FORKS
		final Integer maxParallelForks = Integer.valueOf( System.getProperty( GRADLE_MAXIMUM_PARALLEL_FORKS, "1" ) );
		final Integer worker = Integer.valueOf( System.getProperty( GRADLE_WORKER_ID, "1" ) );
		return (worker % maxParallelForks) + 1; // returns 1, 2, 3...
	}
}
