/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

/**
 * @author Steve Ebersole
 */
public interface C3p0Settings {

	/**
	 * A setting prefix used to indicate settings that target the hibernate-c3p0 integration
	 */
	String C3P0_CONFIG_PREFIX = "hibernate.c3p0";

	/**
	 * Maximum size of C3P0 connection pool
	 */
	String C3P0_MAX_SIZE = "hibernate.c3p0.max_size";

	/**
	 * Minimum size of C3P0 connection pool
	 */
	String C3P0_MIN_SIZE = "hibernate.c3p0.min_size";

	/**
	 * Maximum idle time for C3P0 connection pool
	 */
	String C3P0_TIMEOUT = "hibernate.c3p0.timeout";

	/**
	 * Maximum size of C3P0 statement cache
	 */
	String C3P0_MAX_STATEMENTS = "hibernate.c3p0.max_statements";

	/**
	 * Number of connections acquired when pool is exhausted
	 */
	String C3P0_ACQUIRE_INCREMENT = "hibernate.c3p0.acquire_increment";

	/**
	 * Idle time before a C3P0 pooled connection is validated
	 */
	String C3P0_IDLE_TEST_PERIOD = "hibernate.c3p0.idle_test_period";
}
