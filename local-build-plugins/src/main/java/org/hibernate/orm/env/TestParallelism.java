/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.env;

import java.util.OptionalInt;
import java.util.Set;

/**
 * Resolves the number of parallel test forks to use based on the target database.
 *
 * @see <a href="https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html#org.gradle.api.tasks.testing.Test:maxParallelForks">Test.maxParallelForks</a>
 */
public class TestParallelism {

	// Databases that support standard parallel testing
	private static final Set<String> STANDARD_PARALLEL_DBS = Set.of(
			"h2", "hsqldb", "postgresql", "pgsql", "postgis", "edb",
			"oracle_xe", "mysql", "mariadb", "db2", "mssql", "cockroachdb",
			"hana", "tidb", "spanner", "hana_cloud", "oracle"
	);

	/**
	 * Resolves the number of parallel test forks for the given database.
	 *
	 * <p>The {@code testThreadsOverride} parameter (from the {@code test.threads} Gradle property)
	 * takes precedence over any computed value when present.
	 *
	 * @return the thread count to use, or empty if parallel testing is not configured for this database
	 */
	public static OptionalInt resolveThreadCount(String db, String testThreadsOverride) {
		if ( STANDARD_PARALLEL_DBS.contains( db ) ) {
			// As soon as we hit 16+ threads, the returns are diminishing, so divide by 2
			int cpus = Runtime.getRuntime().availableProcessors();
			int threads = cpus >= 16 ? cpus / 2 : cpus;
			if ( testThreadsOverride != null ) {
				threads = Integer.parseInt( testThreadsOverride );
			}
			return OptionalInt.of( threads );
		}
		else if ( "oracle_test_pilot_database".equals( db ) ) {
			// Oracle TestPilot databases run on separate machines with higher latency;
			// doubling the thread count helps hide the latency effect
			int threads = Runtime.getRuntime().availableProcessors() * 2;
			if ( testThreadsOverride != null ) {
				threads = Integer.parseInt( testThreadsOverride );
			}
			return OptionalInt.of( threads );
		}
		else if ( "spannerpgsql".equals( db ) ) {
			// Spanner PostgreSQL dialect can't handle too many concurrent connections, so divide by 2
			int cpus = Runtime.getRuntime().availableProcessors();
			int threads = Math.max( cpus / 2, 1 );
			if ( testThreadsOverride != null ) {
				threads = Integer.parseInt( testThreadsOverride );
			}
			return OptionalInt.of( threads );
		}
		return OptionalInt.empty();
	}
}
