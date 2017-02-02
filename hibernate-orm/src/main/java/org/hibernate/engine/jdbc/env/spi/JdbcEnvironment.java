/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.env.spi;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.TypeInfo;
import org.hibernate.service.Service;

/**
 * Initial look at this concept we keep talking about with merging information from {@link java.sql.DatabaseMetaData}
 * and {@link org.hibernate.dialect.Dialect}
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

	/**
	 * Access to the bits of information we pulled off the JDBC {@link java.sql.DatabaseMetaData} (that did not get
	 * "interpreted" into the helpers/delegates available here).
	 *
	 * @return The values extracted from JDBC DatabaseMetaData
	 */
	ExtractedDatabaseMetaData getExtractedDatabaseMetaData();

	/**
	 * Get the current database catalog.  Typically will come from either {@link java.sql.Connection#getCatalog()}
	 * or {@link org.hibernate.cfg.AvailableSettings#DEFAULT_CATALOG}.
	 *
	 * @return The current catalog.
	 */
	Identifier getCurrentCatalog();

	/**
	 * Get the current database catalog.  Typically will come from either
	 * {@link SchemaNameResolver#resolveSchemaName(java.sql.Connection, org.hibernate.dialect.Dialect)} or
	 * {@link org.hibernate.cfg.AvailableSettings#DEFAULT_CATALOG}.
	 *
	 * @return The current schema
	 */
	Identifier getCurrentSchema();

	/**
	 * Obtain support for formatting qualified object names.
	 *
	 * @return Qualified name support.
	 */
	QualifiedObjectNameFormatter getQualifiedObjectNameFormatter();

	/**
	 * Obtain the helper for dealing with identifiers in this environment.
	 * <p/>
	 * Note that the Identifiers returned from this IdentifierHelper already account for
	 * auto-quoting :) yaay!
	 *
	 * @return The identifier helper.
	 */
	IdentifierHelper getIdentifierHelper();

	/**
	 * Obtain the level of support for qualified names.
	 *
	 * @return
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

	/**
	 * Find type information for the type identified by the given "JDBC type code".
	 *
	 * @param jdbcTypeCode The JDBC type code.
	 *
	 * @return The corresponding type info.
	 */
	TypeInfo getTypeInfoForJdbcCode(int jdbcTypeCode);
}
