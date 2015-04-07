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
package org.hibernate.tool.schema.extract.spi;

import java.util.Collection;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.tool.schema.extract.internal.TableInformationImpl;

/**
 * Contract for extracting information about objects in the database schema(s).  To an extent, the contract largely
 * mirrors parts of the JDBC {@link java.sql.DatabaseMetaData} contract.  THe intention is to insulate callers
 * from {@link java.sql.DatabaseMetaData} since on many databases there are better ways to get information from
 * the meta schema.
 *
 * NOTE : Concepts here taken largely from the {@code MetaDataDialect} class in Hibernate Tools.
 *
 * @author Steve Ebersole
 */
public interface InformationExtractor {

	boolean schemaExists(Identifier catalog, Identifier schema);

	/**
	 * Return information about all matching tables, depending on whether to apply filter for
	 * catalog/schema.
	 *
	 * @param catalog Can be {@code null}, indicating that any catalog may be considered a match.  A
	 * non-{@code null} value indicates that search should be limited to the passed catalog.
	 * @param schema Can  be {@code null}, indicating that any schema may be considered a match.  A
	 * non-{@code null} value indicates that search should be limited to the passed schema .
	 *
	 * @return The matching table information
	 */
	public Collection<TableInformation> getTables(Identifier catalog, Identifier schema);

	/**
	 * Look for a matching table.
	 *
	 * @param catalog Can be {@code null}, indicating that any catalog may be considered a match.  A
	 * non-{@code null} value indicates that search should be limited to the passed catalog.
	 * @param schema Can  be {@code null}, indicating that any schema may be considered a match.  A
	 * non-{@code null} value indicates that search should be limited to the passed schema .
	 * @param tableName The name of the table to look for.
	 *
	 * @return table info for the matching table
	 */
	public TableInformation getTable(Identifier catalog, Identifier schema, Identifier tableName);

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
