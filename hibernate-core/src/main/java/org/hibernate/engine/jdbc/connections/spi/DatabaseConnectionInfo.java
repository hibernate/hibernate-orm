/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.spi;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Contract used for logging "database information" on bootstrap
 *
 * @apiNote Most of the getters here may return {@code null} which indicates the value is not known
 *
 * @author Jan Schatteman
 */
public interface DatabaseConnectionInfo {
	/**
	 * The JDBC URL to be used for connections
	 */
	@Nullable
	String getJdbcUrl();

	/**
	 * The JDBC Driver to be used for connections
	 */
	@Nullable
	String getJdbcDriver();

	/**
	 * The database version.
	 *
	 * @see Dialect#getVersion()
	 */
	@Nullable
	DatabaseVersion getDialectVersion();

	/**
	 * The transaction auto-commit mode in effect.
	 */
	@Nullable
	String getAutoCommitMode();

	/**
	 * The transaction isolation-level in effect.
	 */
	@Nullable
	String getIsolationLevel();

	/**
	 * The minimum connection pool size.
	 */
	@Nullable
	Integer getPoolMinSize();

	/**
	 * The maximum connection pool size.
	 */
	@Nullable
	Integer getPoolMaxSize();

	/**
	 * The default JDBC fetch size.
	 */
	@Nullable
	Integer getJdbcFetchSize();

	/**
	 * Collects the information available here as a single String with the intent of using it in logging.
	 */
	String toInfoString();
}
