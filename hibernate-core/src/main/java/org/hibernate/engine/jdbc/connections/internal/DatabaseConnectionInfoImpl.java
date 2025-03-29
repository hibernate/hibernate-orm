/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.util.Map;

import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.DatabaseConnectionInfo;
import org.hibernate.internal.util.NullnessHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;

import static org.hibernate.dialect.SimpleDatabaseVersion.ZERO_VERSION;
import static org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator.interpretIsolation;
import static org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator.toIsolationNiceName;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.internal.util.config.ConfigurationHelper.getString;

/**
 * Standard implementation of {@link DatabaseConnectionInfo}
 *
 * @author Jan Schatteman
 */
public class DatabaseConnectionInfoImpl implements DatabaseConnectionInfo {
	public static final String DEFAULT = "undefined/unknown";

	private final Class<?> connectionProviderClass;
	protected final String jdbcUrl;
	protected final String jdbcDriver;
	protected final DatabaseVersion dialectVersion;
	protected final String autoCommitMode;
	protected final String isolationLevel;
	protected final Integer poolMinSize;
	protected final Integer poolMaxSize;

	public DatabaseConnectionInfoImpl(
			Class<? extends ConnectionProvider> connectionProviderClass,
			String jdbcUrl,
			String jdbcDriver,
			DatabaseVersion dialectVersion,
			String autoCommitMode,
			String isolationLevel,
			Integer poolMinSize,
			Integer poolMaxSize) {
		this.connectionProviderClass = connectionProviderClass;
		this.jdbcUrl = nullIfEmpty( jdbcUrl );
		this.jdbcDriver = nullIfEmpty( jdbcDriver );
		this.dialectVersion = dialectVersion;
		this.autoCommitMode = nullIfEmpty( autoCommitMode );
		this.isolationLevel = nullIfEmpty( isolationLevel );
		this.poolMinSize = poolMinSize;
		this.poolMaxSize = poolMaxSize;
	}

	public DatabaseConnectionInfoImpl(Map<String, Object> settings, Dialect dialect) {
		this(
				null,
				determineUrl( settings ),
				determineDriver( settings ),
				dialect.getVersion(),
				determineAutoCommitMode( settings ),
				determineIsolationLevel( settings ),
				// No setting for min. pool size
				null,
				determinePoolMaxSize( settings )
		);
	}

	public DatabaseConnectionInfoImpl(Dialect dialect) {
		this( null, null, null, dialect.getVersion(), null, null, null, null );
	}

	@Override
	public String getJdbcUrl() {
		return jdbcUrl;
	}

	@Override
	public String getJdbcDriver() {
		return jdbcDriver;
	}

	@Override
	public DatabaseVersion getDialectVersion() {
		return dialectVersion;
	}

	@Override
	public String getAutoCommitMode() {
		return autoCommitMode;
	}

	@Override
	public String getIsolationLevel() {
		return isolationLevel;
	}

	@Override
	public Integer getPoolMinSize() {
		return poolMinSize;
	}

	@Override
	public Integer getPoolMaxSize() {
		return poolMaxSize;
	}

	@Override
	public String toInfoString() {
		return "\tDatabase JDBC URL [" + handleEmpty( jdbcUrl ) + ']' +
				"\n\tDatabase driver: " + handleEmpty( jdbcDriver ) +
				"\n\tDatabase version: " + handleEmpty( dialectVersion ) +
				"\n\tAutocommit mode: " + handleEmpty( autoCommitMode ) +
				"\n\tIsolation level: " + handleEmpty( isolationLevel ) +
				"\n\tPool: " + handleEmpty( connectionProviderClass ) +
				"\n\tMinimum pool size: " + handleEmpty( poolMinSize ) +
				"\n\tMaximum pool size: " + handleEmpty( poolMaxSize );
	}

	private static String handleEmpty(String value) {
		return isNotEmpty( value ) ? value : DEFAULT;
	}

	private static String handleEmpty(DatabaseVersion dialectVersion) {
		return dialectVersion != null ? dialectVersion.toString() : ZERO_VERSION.toString();
	}

	private static String handleEmpty(Integer value) {
		return value != null ? value.toString() : DEFAULT;
	}

	private static String handleEmpty(Class<?> value) {
		return value != null ? value.getSimpleName() : DEFAULT;
	}


	@SuppressWarnings("deprecation")
	private static String determineUrl(Map<String, Object> settings) {
		return NullnessHelper.coalesceSuppliedValues(
				() -> getString( JdbcSettings.JAKARTA_JDBC_URL, settings ),
				() -> getString( JdbcSettings.URL, settings ),
				() -> getString( JdbcSettings.JPA_JDBC_URL, settings )
		);
	}

	@SuppressWarnings("deprecation")
	private static String determineDriver(Map<String, Object> settings) {
		return NullnessHelper.coalesceSuppliedValues(
				() -> getString( JdbcSettings.JAKARTA_JDBC_DRIVER, settings ),
				() -> getString( JdbcSettings.DRIVER, settings ),
				() -> getString( JdbcSettings.JPA_JDBC_DRIVER, settings )
		);
	}

	private static String determineAutoCommitMode(Map<String, Object> settings) {
		return getString( JdbcSettings.AUTOCOMMIT, settings );
	}

	private static String determineIsolationLevel(Map<String, Object> settings) {
		return toIsolationNiceName( interpretIsolation( settings.get(JdbcSettings.ISOLATION ) ) );
	}

	private static Integer determinePoolMaxSize(Map<String, Object> settings) {
		return ConfigurationHelper.getInteger( JdbcSettings.POOL_SIZE, settings );
	}
}
