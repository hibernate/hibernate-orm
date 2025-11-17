/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.spi;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

import static java.util.Collections.emptyList;

/**
 * Information extracted from {@link java.sql.DatabaseMetaData} regarding what the JDBC driver reports as
 * being supported or not.  Obviously {@link java.sql.DatabaseMetaData} reports many things, these are a few in
 * which we have particular interest.
 *
 * @author Steve Ebersole
 */
public interface ExtractedDatabaseMetaData {
	/**
	 * Obtain the JDBC Environment from which this metadata came.
	 *
	 * @return The JDBC environment
	 */
	JdbcEnvironment getJdbcEnvironment();

	/**
	 * The name of the database, according to the JDBC driver.
	 */
	String getDatabaseProductName();

	/**
	 * The version of the database, according to the JDBC driver.
	 */
	String getDatabaseProductVersion();

	/**
	 * Does this driver support named schemas in DML?
	 *
	 * @return {@code false} indicates the driver reported false;
	 * {@code true} indicates the driver reported true or that
	 * the driver could not be asked.
	 */
	boolean supportsSchemas();

	/**
	 * Does this driver support named catalogs in DML?
	 *
	 * @return {@code false} indicates the driver reported false;
	 * {@code true} indicates the driver reported true or that
	 * the driver could not be asked.
	 */
	boolean supportsCatalogs();

	/**
	 * Retrieve the name of the catalog in effect when we connected to the database.
	 *
	 * @return The catalog name
	 *
	 * @see AvailableSettings#DEFAULT_SCHEMA
	 */
	String getConnectionCatalogName();

	/**
	 * Retrieve the name of the schema in effect when we connected to the database.
	 *
	 * @return The schema name
	 */
	String getConnectionSchemaName();

	/**
	 * Does the driver report supporting named parameters?
	 *
	 * @return {@code true} indicates the driver reported true; {@code false} indicates the driver reported false
	 * or that the driver could not be asked.
	 *
	 * @see AvailableSettings#CALLABLE_NAMED_PARAMS_ENABLED
	 */
	boolean supportsNamedParameters();

	/**
	 * Does the driver report supporting {@link java.sql.Types#REF_CURSOR}?
	 *
	 * @return {@code true} indicates the driver reported true;
	 * {@code false} indicates the driver reported false or that
	 * the driver could not be asked.
	 *
	 * @see java.sql.DatabaseMetaData#supportsRefCursors()
	 * @see org.hibernate.dialect.Dialect#supportsRefCursors
	 */
	boolean supportsRefCursors();

	/**
	 * Did the driver report to supporting scrollable result sets?
	 *
	 * @return True if the driver reported to support {@link java.sql.ResultSet#TYPE_SCROLL_INSENSITIVE}.
	 *
	 * @see java.sql.DatabaseMetaData#supportsResultSetType
	 * @see AvailableSettings#USE_SCROLLABLE_RESULTSET
	 */
	boolean supportsScrollableResults();

	/**
	 * Did the driver report to supporting retrieval of generated keys?
	 *
	 * @return True if the driver reported to support calls to {@link java.sql.Statement#getGeneratedKeys}
	 *
	 * @see java.sql.DatabaseMetaData#supportsGetGeneratedKeys
	 * @see AvailableSettings#USE_GET_GENERATED_KEYS
	 */
	boolean supportsGetGeneratedKeys();

	/**
	 * Did the driver report to supporting batched updates?
	 *
	 * @return True if the driver supports batched updates
	 *
	 * @see java.sql.DatabaseMetaData#supportsBatchUpdates
	 * @see org.hibernate.dialect.Dialect#supportsBatchUpdates
	 */
	boolean supportsBatchUpdates();

	/**
	 * Did the driver report to support performing DDL within transactions?
	 *
	 * @return True if the drivers supports DDL statements within transactions.
	 *
	 * @see java.sql.DatabaseMetaData#dataDefinitionIgnoredInTransactions
	 */
	boolean supportsDataDefinitionInTransaction();

	/**
	 * Did the driver report to DDL statements performed within a transaction performing an implicit commit of the
	 * transaction.
	 *
	 * @return True if the driver/database performs an implicit commit of transaction when DDL statement is
	 * performed
	 *
	 * @see java.sql.DatabaseMetaData#dataDefinitionCausesTransactionCommit()
	 */
	boolean doesDataDefinitionCauseTransactionCommit();

	/**
	 * Retrieve the type of codes the driver says it uses for {@code SQLState}.  They might follow either
	 * the X/Open standard or the SQL92 standard.
	 *
	 * @return The SQLState strategy reportedly used by this driver/database.
	 *
	 * @see java.sql.DatabaseMetaData#getSQLStateType()
	 */
	SQLStateType getSqlStateType();

	/**
	 * Retrieve the JDBC URL.
	 *
	 * @see java.sql.DatabaseMetaData#getURL()
	 */
	String getUrl();

	/**
	 * Retrieve the JDBC driver name.
	 *
	 * @see java.sql.DatabaseMetaData#getDriverName()
	 */
	String getDriver();

	/**
	 * Retrieve the transaction isolation level.
	 *
	 * @see java.sql.Connection#getTransactionIsolation()
	 */
	int getTransactionIsolation();

	/**
	 * Retrieve the default transaction isolation level.
	 *
	 * @see java.sql.DatabaseMetaData#getDefaultTransactionIsolation()
	 */
	int getDefaultTransactionIsolation();

	/**
	 * Retrieve the default JDBC {@linkplain java.sql.Statement#getFetchSize fetch size}.
	 */
	int getDefaultFetchSize();

	/**
	 * Retrieve the list of {@code SequenceInformation} objects which describe the underlying database sequences.
	 *
	 * @return {@code SequenceInformation} objects.
	 */
	default List<SequenceInformation> getSequenceInformationList() {
		return emptyList();
	}
}
