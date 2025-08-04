/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;
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
	private final Class<? extends Dialect> dialectClass;
	protected final DatabaseVersion dialectVersion;
	private final boolean hasSchema;
	private final boolean hasCatalog;
	protected final String schema;
	protected final String catalog;
	protected final String autoCommitMode;
	protected final String isolationLevel;
	protected final Integer poolMinSize;
	protected final Integer poolMaxSize;
	private final Integer fetchSize;

	public DatabaseConnectionInfoImpl(
			Class<? extends ConnectionProvider> connectionProviderClass,
			String jdbcUrl,
			String jdbcDriver,
			Class<? extends Dialect> dialectClass,
			DatabaseVersion dialectVersion,
			boolean hasSchema,
			boolean hasCatalog,
			String schema,
			String catalog,
			String autoCommitMode,
			String isolationLevel,
			Integer poolMinSize,
			Integer poolMaxSize,
			Integer fetchSize) {
		this.connectionProviderClass = connectionProviderClass;
		this.jdbcUrl = nullIfEmpty( jdbcUrl );
		this.jdbcDriver = nullIfEmpty( jdbcDriver );
		this.dialectVersion = dialectVersion;
		this.hasSchema = hasSchema;
		this.hasCatalog = hasCatalog;
		this.schema = schema;
		this.catalog = catalog;
		this.autoCommitMode = nullIfEmpty( autoCommitMode );
		this.isolationLevel = nullIfEmpty( isolationLevel );
		this.poolMinSize = poolMinSize;
		this.poolMaxSize = poolMaxSize;
		this.fetchSize = fetchSize;
		this.dialectClass = dialectClass;
	}

	public DatabaseConnectionInfoImpl(Map<String, Object> settings, Dialect dialect) {
		this(
				null,
				determineUrl( settings ),
				determineDriver( settings ),
				dialect.getClass(),
				dialect.getVersion(),
				true,
				true,
				null,
				null,
				determineAutoCommitMode( settings ),
				determineIsolationLevel( settings ),
				// No setting for min. pool size
				null,
				determinePoolMaxSize( settings ),
				null
		);
	}

	public DatabaseConnectionInfoImpl(Dialect dialect) {
		this(
				null,
				null,
				null,
				dialect.getClass(),
				dialect.getVersion(),
				true,
				true,
				null,
				null,
				null,
				null,
				null,
				null,
				null
		);
	}

	public static boolean hasSchema(Connection connection) {
		try {
			return connection.getMetaData().supportsSchemasInTableDefinitions();
		}
		catch ( SQLException ignored ) {
			return true;
		}
	}

	public static boolean hasCatalog(Connection connection) {
		try {
			return connection.getMetaData().supportsCatalogsInDataManipulation();
		}
		catch ( SQLException ignored ) {
			return true;
		}
	}

	public static String getSchema(Connection connection) {
		try {
			// method introduced in 1.7
			return connection.getSchema();
		}
		catch ( SQLException|AbstractMethodError ignored ) {
			return null;
		}
	}

	public static String getCatalog(Connection connection) {
		try {
			return connection.getCatalog();
		}
		catch ( SQLException ignored ) {
			return null;
		}
	}

	public static Integer getFetchSize(Connection connection) {
		try ( var statement = connection.createStatement() ) {
			return statement.getFetchSize();
		}
		catch ( SQLException ignored ) {
			return null;
		}
	}

	public static Integer getIsolation(Connection connection) {
		try {
			return connection.getTransactionIsolation();
		}
		catch ( SQLException ignored ) {
			return null;
		}
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
	public @Nullable Integer getJdbcFetchSize() {
		return fetchSize;
	}

	@Override
	public @Nullable String getSchema() {
		return schema;
	}

	@Override
	public @Nullable String getCatalog() {
		return catalog;
	}

	@Override
	public boolean hasSchema() {
		return hasSchema;
	}

	@Override
	public boolean hasCatalog() {
		return hasCatalog;
	}

	@Override
	public String toInfoString() {
		return """
				\tDatabase JDBC URL [%s]
				\tDatabase driver: %s
				\tDatabase dialect: %s
				\tDatabase version: %s
				\tDefault catalog/schema: %s/%s
				\tAutocommit mode: %s
				\tIsolation level: %s
				\tJDBC fetch size: %s
				\tPool: %s
				\tMinimum pool size: %s
				\tMaximum pool size: %s"""
				.formatted(
						handleEmpty( jdbcUrl ),
						handleEmpty( jdbcDriver ),
						handleEmpty( dialectClass ),
						handleEmpty( dialectVersion ),
						handleEmpty( catalog, hasCatalog ),
						handleEmpty( schema, hasSchema ),
						handleEmpty( autoCommitMode ),
						handleEmpty( isolationLevel ),
						handleFetchSize( fetchSize ),
						handleEmpty( connectionProviderClass ),
						handleEmpty( poolMinSize ),
						handleEmpty( poolMaxSize )
				);
	}

	private static String handleEmpty(String value) {
		return isNotEmpty( value ) ? value : DEFAULT;
	}

	private static String handleEmpty(String value, boolean defined) {
		return isNotEmpty( value ) ? value : (defined ? "unknown" : "undefined");
	}

	private static String handleEmpty(DatabaseVersion dialectVersion) {
		return dialectVersion != null ? dialectVersion.toString() : ZERO_VERSION.toString();
	}

	private static String handleEmpty(Integer value) {
		return value != null ? value.toString() : DEFAULT;
	}

	private static String handleFetchSize(Integer value) {
		return value != null ? ( value == 0 ? "none" : value.toString() ) : DEFAULT;
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
