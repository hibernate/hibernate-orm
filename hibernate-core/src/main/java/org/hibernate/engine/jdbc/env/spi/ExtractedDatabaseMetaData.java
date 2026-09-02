/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.spi;

import java.util.List;
import java.util.Set;

import org.hibernate.SPI;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

import static java.util.Collections.emptyList;
import static org.hibernate.SPI.Role.USE;

/// Raw JDBC driver and bootstrap-connection observations.
///
/// This contract records the defensively extracted `DatabaseMetaData` answers
/// and connection values. These observations are useful for diagnostics,
/// reporting, and integrations which explicitly need driver-reported state.
/// Use [JdbcMetadata] for Hibernate runtime decisions because its support
/// answers include Dialect metadata overrides and compatibility interpretation.
///
/// @see JdbcMetadata#getExtractedDatabaseMetaData()
/// @author Steve Ebersole
@SPI(USE)
public interface ExtractedDatabaseMetaData {
	/// The JDBC environment from which these observations were extracted.
	JdbcEnvironment getJdbcEnvironment();

	/// Whether a live JDBC metadata source was available when this snapshot was
	/// extracted.
	boolean isJdbcMetadataAccessible();

	/// The database product name reported by the JDBC driver.
	String getDatabaseProductName();

	/// The database product version reported by the JDBC driver.
	String getDatabaseProductVersion();

	/// Whether the driver reported support for named schemas in DML.
	///
	/// A `true` answer may also represent a defensive fallback when the driver
	/// could not be asked.
	boolean supportsSchemas();

	/// Whether the driver reported support for named catalogs in DML.
	///
	/// A `true` answer may also represent a defensive fallback when the driver
	/// could not be asked.
	boolean supportsCatalogs();

	/// The catalog in effect for the bootstrap connection.
	String getConnectionCatalogName();

	/// The schema in effect for the bootstrap connection.
	///
	/// @see AvailableSettings#DEFAULT_SCHEMA
	String getConnectionSchemaName();

	/// The normalized case strategy derived from the driver's unquoted
	/// identifier storage reports.
	IdentifierCaseStrategy getUnquotedIdentifierCaseStrategy();

	/// The normalized case strategy derived from the driver's quoted identifier
	/// storage reports.
	IdentifierCaseStrategy getQuotedIdentifierCaseStrategy();

	/// The immutable lowercase SQL-keyword set reported by the JDBC driver.
	Set<String> getSqlKeywords();

	/// Whether the driver reported support for named callable parameters.
	///
	/// A `false` answer may also represent a defensive fallback when the driver
	/// could not be asked. Use [JdbcMetadata#supportsNamedParameters()] for an
	/// effective runtime decision.
	///
	/// @see AvailableSettings#CALLABLE_NAMED_PARAMS_ENABLED
	boolean supportsNamedParameters();

	/// Whether the driver reported support for the standard JDBC REF_CURSOR API.
	///
	/// A `false` answer may also represent a defensive fallback when the driver
	/// could not be asked. Use [JdbcMetadata#supportsRefCursors()] for an
	/// effective runtime decision.
	boolean supportsRefCursors();

	/// Whether the driver reported support for scroll-insensitive result sets.
	///
	/// @see java.sql.DatabaseMetaData#supportsResultSetType(int)
	/// @see AvailableSettings#USE_SCROLLABLE_RESULTSET
	boolean supportsScrollableResults();

	/// Whether the driver reported support for generated-key retrieval.
	///
	/// @see java.sql.DatabaseMetaData#supportsGetGeneratedKeys()
	/// @see AvailableSettings#USE_GET_GENERATED_KEYS
	boolean supportsGetGeneratedKeys();

	/// Whether the driver reported support for JDBC batch updates.
	///
	/// A `true` answer may also represent a defensive fallback when the driver
	/// could not be asked. Use [JdbcMetadata#supportsBatchUpdates()] for an
	/// effective runtime decision.
	boolean supportsBatchUpdates();

	/// Whether the driver reported that DDL is supported within transactions.
	boolean supportsDataDefinitionInTransaction();

	/// Whether the driver reported that DDL causes an implicit transaction commit.
	boolean doesDataDefinitionCauseTransactionCommit();

	/// The SQL-state coding scheme reported by the JDBC driver.
	SQLStateType getSqlStateType();

	/// The JDBC URL reported by the driver.
	String getUrl();

	/// The JDBC driver name.
	String getDriver();

	/// The bootstrap connection's transaction isolation level.
	int getTransactionIsolation();

	/// The default transaction isolation level reported by the driver.
	int getDefaultTransactionIsolation();

	/// The default JDBC fetch size observed from the bootstrap connection.
	int getDefaultFetchSize();

	/// Information describing the database sequences.
	default List<SequenceInformation> getSequenceInformationList() {
		return emptyList();
	}
}
