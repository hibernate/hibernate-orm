/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc.spi;

import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.engine.jdbc.internal.TypeInfo;

/**
 * Information extracted from {@link java.sql.DatabaseMetaData} regarding what the JDBC driver reports as
 * being supported or not.  Obviously {@link java.sql.DatabaseMetaData} reports many things, these are a few in
 * which we have particular interest.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings( {"UnusedDeclaration"})
public interface ExtractedDatabaseMetaData {

	public enum SQLStateType {
		XOpen,
		SQL99,
		UNKOWN
	}

	/**
	 * Did the driver report to supporting scrollable result sets?
	 *
	 * @return True if the driver reported to support {@link java.sql.ResultSet#TYPE_SCROLL_INSENSITIVE}.
	 *
	 * @see java.sql.DatabaseMetaData#supportsResultSetType
	 */
	public boolean supportsScrollableResults();

	/**
	 * Did the driver report to supporting retrieval of generated keys?
	 *
	 * @return True if the if the driver reported to support calls to {@link java.sql.Statement#getGeneratedKeys}
	 *
	 * @see java.sql.DatabaseMetaData#supportsGetGeneratedKeys
	 */
	public boolean supportsGetGeneratedKeys();

	/**
	 * Did the driver report to supporting batched updates?
	 *
	 * @return True if the driver supports batched updates
	 *
	 * @see java.sql.DatabaseMetaData#supportsBatchUpdates
	 */
	public boolean supportsBatchUpdates();

	/**
	 * Did the driver report to support performing DDL within transactions?
	 *
	 * @return True if the drivers supports DDL statements within transactions.
	 *
	 * @see java.sql.DatabaseMetaData#dataDefinitionIgnoredInTransactions
	 */
	public boolean supportsDataDefinitionInTransaction();

	/**
	 * Did the driver report to DDL statements performed within a transaction performing an implicit commit of the
	 * transaction.
	 *
	 * @return True if the driver/database performs an implicit commit of transaction when DDL statement is
	 * performed
	 *
	 * @see java.sql.DatabaseMetaData#dataDefinitionCausesTransactionCommit()
	 */
	public boolean doesDataDefinitionCauseTransactionCommit();

	/**
	 * Get the list of extra keywords (beyond standard SQL92 keywords) reported by the driver.
	 *
	 * @return The extra keywords used by this database.
	 *
	 * @see java.sql.DatabaseMetaData#getSQLKeywords()
	 */
	public Set<String> getExtraKeywords();

	/**
	 * Retrieve the type of codes the driver says it uses for {@code SQLState}.  They might follow either
	 * the X/Open standard or the SQL92 standard.
	 *
	 * @return The SQLState strategy reportedly used by this driver/database.
	 *
	 * @see java.sql.DatabaseMetaData#getSQLStateType()
	 */
	public SQLStateType getSqlStateType();

	/**
	 * Did the driver report that updates to a LOB locator affect a copy of the LOB?
	 *
	 * @return True if updates to the state of a LOB locator update only a copy.
	 *
	 * @see java.sql.DatabaseMetaData#locatorsUpdateCopy()
	 */
	public boolean doesLobLocatorUpdateCopy();

	/**
	 * Retrieve the name of the schema in effect when we connected to the database.
	 *
	 * @return The schema name
	 */
	public String getConnectionSchemaName();

	/**
	 * Retrieve the name of the catalog in effect when we connected to the database.
	 *
	 * @return The catalog name
	 */
	public String getConnectionCatalogName();

	/**
	 * Set of type info reported by the driver.
	 *
	 * @return The type information obtained from the driver.
	 *
	 * @see java.sql.DatabaseMetaData#getTypeInfo()
	 */
	public LinkedHashSet<TypeInfo> getTypeInfoSet();
}
