/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

/**
 * The mode how versions shall be matched.
 *
 * @see SkipForDialect
 * @see RequiresDialect
 */
public enum VersionMatchMode {
	/**
	 * A database version must be older than the specified version.
	 */
	OLDER,
	/**
	 * A database version must be the same as the specified version.
	 */
	SAME,
	/**
	 * A database version must be the same or newer than the specified version.
	 */
	SAME_OR_NEWER,
	/**
	 * A database version must be the same or older than the specified version.
	 */
	SAME_OR_OLDER,
	/**
	 * A database version must be newer than the specified version.
	 */
	NEWER
}
