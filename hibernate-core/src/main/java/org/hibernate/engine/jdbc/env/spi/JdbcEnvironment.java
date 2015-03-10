/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
	public Dialect getDialect();

	/**
	 * Access to the bits of information we pulled off the JDBC {@link java.sql.DatabaseMetaData} (that did not get
	 * "interpreted" into the helpers/delegates available here).
	 *
	 * @return The values extracted from JDBC DatabaseMetaData
	 */
	public ExtractedDatabaseMetaData getExtractedDatabaseMetaData();

	/**
	 * Get the current database catalog.  Typically will come from either {@link java.sql.Connection#getCatalog()}
	 * or {@link org.hibernate.cfg.AvailableSettings#DEFAULT_CATALOG}.
	 *
	 * @return The current catalog.
	 */
	public Identifier getCurrentCatalog();

	/**
	 * Get the current database catalog.  Typically will come from either
	 * {@link SchemaNameResolver#resolveSchemaName(java.sql.Connection, org.hibernate.dialect.Dialect)} or
	 * {@link org.hibernate.cfg.AvailableSettings#DEFAULT_CATALOG}.
	 *
	 * @return The current schema
	 */
	public Identifier getCurrentSchema();

	/**
	 * Obtain support for formatting qualified object names.
	 *
	 * @return Qualified name support.
	 */
	public QualifiedObjectNameFormatter getQualifiedObjectNameFormatter();

	/**
	 * Obtain the helper for dealing with identifiers in this environment.
	 * <p/>
	 * Note that the Identifiers returned from this IdentifierHelper already account for
	 * auto-quoting :) yaay!
	 *
	 * @return The identifier helper.
	 */
	public IdentifierHelper getIdentifierHelper();

	/**
	 * Check whether the given word represents a reserved word.
	 *
	 * @param word The word to check
	 *
	 * @return {@code true} if the given word represents a reserved word; {@code false} otherwise.
	 */
	public boolean isReservedWord(String word);

	/**
	 * Obtain the helper for dealing with JDBC {@link java.sql.SQLException} faults.
	 *
	 * @return This environment's helper.
	 */
	public SqlExceptionHelper getSqlExceptionHelper();

	/**
	 * Retrieve the delegate for building {@link org.hibernate.engine.jdbc.LobCreator} instances.
	 *
	 * @return The LobCreator builder.
	 */
	public LobCreatorBuilder getLobCreatorBuilder();

	/**
	 * Find type information for the type identified by the given "JDBC type code".
	 *
	 * @param jdbcTypeCode The JDBC type code.
	 *
	 * @return The corresponding type info.
	 */
	public TypeInfo getTypeInfoForJdbcCode(int jdbcTypeCode);
}
