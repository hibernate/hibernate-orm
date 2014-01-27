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
package org.hibernate.tool.schema.extract.spi;

import org.hibernate.tool.schema.extract.internal.TableInformationImpl;

/**
 * Contract for extracting information about objects in the database schema(s).  To an extent, the contract largely
 * mirrors parts of the JDBC {@link java.sql.DatabaseMetaData} contract.  THe intention is to insulate callers
 * from {@link java.sql.DatabaseMetaData} since on many databases there are better ways to get information from
 * the meta schema.
 *
 * NOTE : Concepts here taken largely from the {@code MetaDataDialect} class Hibernate Tools.
 *
 * @author Steve Ebersole
 */
public interface SchemaMetaDataExtractor {
	public static final String ALL_CATALOGS_FILTER = null;
	public static final String SANS_CATALOG_FILTER = "";

	public static final String ALL_SCHEMAS_FILTER = null;
	public static final String SANS_SCHEMA_FILTER = "";

	/**
	 * Return information about all matching tables
	 *
	 * @param catalogFilter Filter to be applied for the catalog to which tables belong.  Can be either the
	 * name of the catalog to match or one of 2 special values:<ol>
	 *     <li>
	 *         {@code null} ({@link #ALL_CATALOGS_FILTER}) - Indicates that tables from all catalogs should be returned
	 *     </li>
	 *     <li>
	 *         {@code ""} (empty String) ({@link #SANS_CATALOG_FILTER}) - Indicates that only tables without a catalog
	 *         should be returned
	 *     </li>
	 * </ol>
	 * @param schemaFilter Filter to be applied for the schema to which tables belong.  Can be either the
	 * name of the schema to match or one of 2 special values:<ol>
	 *     <li>
	 *         {@code null} ({@link #ALL_SCHEMAS_FILTER}) - Indicates that tables from all schemas should be returned
	 *     </li>
	 *     <li>
	 *         {@code ""} (empty String) ({@link #SANS_SCHEMA_FILTER}) - Indicates that only tables without a schema
	 *         should be returned
	 *     </li>
	 * </ol>
	 *
	 * @return iterator with map elements that has "TABLE_NAME", "TABLE_SCHEMA", "TABLE_CAT", "TABLE_TYPE" keys.
	 */
	public Iterable<TableInformation> getTables(String catalogFilter, String schemaFilter);

	/**
	 * Return information about columns for the given table.  Typically called from the TableInformation itself
	 * as part of on-demand initialization of its state.
	 *
	 * @param tableInformation The table for which to locate columns
	 *
	 * @return The extracted column information
	 */
	public Iterable<ColumnInformation> getColumns(TableInformation tableInformation);

	/**
	 * Extract information about the given table's primary key.
	 *
	 * @param tableInformation The table for which to locate primary key information,
	 *
	 * @return The extracted primary key information
	 */
	public PrimaryKeyInformation getPrimaryKey(TableInformationImpl tableInformation);

	/**
	 * Extract information about indexes defined against the given table.  Typically called from the TableInformation
	 * itself as part of on-demand initialization of its state.
	 *
	 * @param tableInformation The table for which to locate indexes
	 *
	 * @return The extracted index information
	 */
	public Iterable<IndexInformation> getIndexes(TableInformation tableInformation);

	/**
	 * Extract information about foreign keys defined on the given table (targeting or point-at other tables).
	 * Typically called from the TableInformation itself as part of on-demand initialization of its state.
	 *
	 * @param tableInformation The table for which to locate foreign-keys
	 *
	 * @return The extracted foreign-key information
	 */
	public Iterable<ForeignKeyInformation> getForeignKeys(TableInformation tableInformation);
}
