/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import java.util.Set;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.TypeInfo;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;

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
	 * Obtain support for reading and writing qualified object names.
	 *
	 * @return Qualified name support.
	 */
	public QualifiedObjectNameSupport getQualifiedObjectNameSupport();

	/**
	 * Obtain the helper for dealing with identifiers in this environment.
	 *
	 * @return The identifier helper.
	 */
	public IdentifierHelper getIdentifierHelper();

	/**
	 * Get the complete set of reserved words for this environment.  These are significant because they represent
	 * the complete set of terms that MUST BE quoted if used as identifiers.  This allows us to apply auto-quoting
	 * in the metamodel based on these terms.
	 *
	 * Note that the standard IdentifierHelper returned by {@link #getIdentifierHelper()} already accounts for
	 * auto-quoting :) yaay!
	 *
	 * @return Reserved words
	 */
	public Set<String> getReservedWords();

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
	
	public ServiceRegistry getServiceRegistry();
}
