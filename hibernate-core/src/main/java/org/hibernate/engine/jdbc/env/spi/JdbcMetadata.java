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

/// Effective JDBC metadata used for Hibernate runtime decisions.
///
/// Hibernate combines the raw [ExtractedDatabaseMetaData] snapshot with the
/// Dialect's
/// [JDBC metadata override profile][org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides].
/// Use the effective support methods on this
/// contract for runtime decisions. Access [#getExtractedDatabaseMetaData()] only
/// for diagnostics, reporting, or an integration which explicitly requires
/// the raw driver observations.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public interface JdbcMetadata {
	/// The JDBC environment owning this effective metadata view.
	JdbcEnvironment getJdbcEnvironment();

	/// Whether a live JDBC metadata source was available when the underlying
	/// snapshot was extracted.
	boolean isJdbcMetadataAccessible();

	/// The raw driver and connection observations underlying this view.
	///
	/// Do not use a raw support report as an effective runtime decision.
	ExtractedDatabaseMetaData getExtractedDatabaseMetaData();

	/// The database product name reported by the JDBC driver.
	String getDatabaseProductName();

	/// The database product version reported by the JDBC driver.
	String getDatabaseProductVersion();

	/// Whether the JDBC driver reported that schemas are supported in DML.
	boolean supportsSchemas();

	/// Whether the JDBC driver reported that catalogs are supported in DML.
	boolean supportsCatalogs();

	/// The catalog in effect for the bootstrap connection.
	String getConnectionCatalogName();

	/// The schema in effect for the bootstrap connection.
	///
	/// @see AvailableSettings#DEFAULT_SCHEMA
	String getConnectionSchemaName();

	/// The effective unquoted-identifier case strategy captured from JDBC
	/// metadata, or the standard fallback when metadata is unavailable.
	IdentifierCaseStrategy getUnquotedIdentifierCaseStrategy();

	/// The effective quoted-identifier case strategy captured from JDBC
	/// metadata, or the standard fallback when metadata is unavailable.
	IdentifierCaseStrategy getQuotedIdentifierCaseStrategy();

	/// The immutable lowercase SQL-keyword set reported by the JDBC driver.
	Set<String> getSqlKeywords();

	/// Whether named callable parameters are effectively supported.
	///
	/// Hibernate resolves the raw driver report through
	/// [the named-parameter override][org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides#getNamedParameterSupport()].
	///
	/// @see AvailableSettings#CALLABLE_NAMED_PARAMS_ENABLED
	boolean supportsNamedParameters();

	/// Whether the standard JDBC REF_CURSOR API is effectively supported.
	///
	/// This answer controls `Types.REF_CURSOR` registration and typed
	/// `getObject(..., ResultSet.class)` access. It does not say whether the
	/// callable SQL protocol admits a REF_CURSOR parameter. Hibernate resolves
	/// the raw driver report and compatibility workarounds through
	/// [the standard REF_CURSOR override][org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides#getStandardRefCursorSupport()].
	boolean supportsRefCursors();

	/// Whether scroll-insensitive result sets are reported as supported.
	boolean supportsScrollableResults();

	/// Whether JDBC generated-key retrieval is reported as supported.
	boolean supportsGetGeneratedKeys();

	/// Whether JDBC batch updates are effectively supported.
	///
	/// Hibernate resolves the raw driver report through
	/// [the batch-update override][org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides#getBatchUpdateSupport()].
	boolean supportsBatchUpdates();

	/// Whether DDL is reported as supported within transactions.
	boolean supportsDataDefinitionInTransaction();

	/// Whether DDL is reported to cause an implicit transaction commit.
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
