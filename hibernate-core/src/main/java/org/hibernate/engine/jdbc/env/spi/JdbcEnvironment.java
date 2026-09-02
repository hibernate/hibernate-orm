/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.spi;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.service.Service;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;

/**
 * Initial look at this concept we keep talking about with merging information from {@link java.sql.DatabaseMetaData}
 * and {@link Dialect}
 *
 * @author Steve Ebersole
 */
public interface JdbcEnvironment extends Service {
	/**
	 * Get the dialect for this environment.
	 *
	 * @return The dialect.
	 */
	Dialect getDialect();

	SqlAstTranslatorFactory getSqlAstTranslatorFactory();

	/// Obtain the stable effective JDBC metadata view for this environment.
	///
	/// Use this view for runtime support decisions. Access its raw snapshot only
	/// when diagnostics, reporting, or an integration contract specifically
	/// requires driver observations.
	///
	/// @since 8.0
	JdbcMetadata getJdbcMetadata();

	/// Obtain the raw JDBC driver and bootstrap-connection observations.
	///
	/// @return the values extracted from JDBC `DatabaseMetaData`
	/// @deprecated Use [#getJdbcMetadata()] for effective runtime decisions. For
	/// intentional raw access, use
	/// `getJdbcMetadata().getExtractedDatabaseMetaData()`.
	@Deprecated(since = "8.0")
	ExtractedDatabaseMetaData getExtractedDatabaseMetaData();

	/**
	 * Get the current database catalog.  Typically, will come from either
	 * {@link java.sql.Connection#getCatalog()} or
	 * {@value org.hibernate.cfg.AvailableSettings#DEFAULT_CATALOG}.
	 *
	 * @return The current catalog.
	 */
	Identifier getCurrentCatalog();

	/**
	 * Get the current database catalog.  Typically, will come from either
	 * {@link java.sql.Connection#getSchema()} or
	 * {@value org.hibernate.cfg.AvailableSettings#DEFAULT_SCHEMA}.
	 *
	 * @see org.hibernate.engine.jdbc.env.spi.SchemaNameResolver
	 *
	 * @return The current schema
	 */
	Identifier getCurrentSchema();

	/**
	 * Obtain support for formatting qualified object names.
	 *
	 * @return Qualified name support.
	 * @deprecated Use a provided {@link org.hibernate.boot.model.relational.SqlStringGenerationContext} instead.
	 */
	@Deprecated
	QualifiedObjectNameFormatter getQualifiedObjectNameFormatter();

	/**
	 * Obtain the helper for dealing with identifiers in this environment.
	 * <p>
	 * Note that the Identifiers returned from this IdentifierHelper already account for
	 * auto-quoting :) yaay!
	 *
	 * @return The identifier helper.
	 */
	IdentifierHelper getIdentifierHelper();

	/**
	 * Obtain the level of support for qualified names.
	 *
	 * @return The NameQualifierSupport.
	 */
	NameQualifierSupport getNameQualifierSupport();

	/**
	 * Obtain the helper for dealing with JDBC {@link java.sql.SQLException} faults.
	 *
	 * @return This environment's helper.
	 */
	SqlExceptionHelper getSqlExceptionHelper();

	/**
	 * Retrieve the delegate for building {@link org.hibernate.engine.jdbc.LobCreator} instances.
	 *
	 * @return The LobCreator builder.
	 */
	LobCreatorBuilder getLobCreatorBuilder();
}
