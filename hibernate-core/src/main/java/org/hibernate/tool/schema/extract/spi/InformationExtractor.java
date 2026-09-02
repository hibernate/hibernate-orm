/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.spi;

import jakarta.annotation.Nullable;
import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.Dialect;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Extracts information about objects in database schemas.
///
/// Implement this contract when standard JDBC metadata and the stock profiles
/// in [InformationExtractors] do not match the database. An extractor is bound
/// to the [ExtractionContext] used to create it; do not cache it across
/// contexts or retain JDBC resources after an operation completes.
///
/// @author Steve Ebersole
/// @see Dialect#getInformationExtractor(ExtractionContext)
@Incubating
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface InformationExtractor {

	/**
	 * Does the given catalog exist yet?
	 *
	 * @param catalog The name of the catalog to look for.
	 *
	 * @return {@code true} if the catalog does exist; {@code false} otherwise
	 */
	boolean catalogExists(Identifier catalog);

	/**
	 * Does the given schema exist yet?
	 *
	 * @param catalog The name of the catalog to look in.
	 * @param schema The name of the schema to look for.
	 *
	 * @return {@code true} if the schema does exist; {@code false} otherwise
	 */
	boolean schemaExists(Identifier catalog, Identifier schema);

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
	TableInformation getTable(Identifier catalog, Identifier schema, Identifier tableName);

	/**
	 * Extract all the tables information.
	 *
	 * @param catalog Can be {@code null}, indicating that any catalog may be considered a match.  A
	 * non-{@code null} value indicates that search should be limited to the passed catalog.
	 * @param schema Can  be {@code null}, indicating that any schema may be considered a match.  A
	 * non-{@code null} value indicates that search should be limited to the passed schema .
	 *
	 * @return a {@link NameSpaceTablesInformation}
	 */
	NameSpaceTablesInformation getTables(Identifier catalog, Identifier schema);

	/**
	 * Extract information about the given table's primary key.
	 *
	 * @param tableInformation The table for which to locate primary key information,
	 *
	 * @return The extracted primary key information
	 */
	@Nullable PrimaryKeyInformation getPrimaryKey(TableInformation tableInformation);

	/**
	 * Extract all the primary keys information.
	 *
	 * @param catalog Can be {@code null}, indicating that any catalog may be considered a match.  A
	 * non-{@code null} value indicates that search should be limited to the passed catalog.
	 * @param schema Can  be {@code null}, indicating that any schema may be considered a match.  A
	 * non-{@code null} value indicates that search should be limited to the passed schema .
	 *
	 * @return a {@link NameSpacePrimaryKeysInformation}
	 * @throws SchemaExtractionException when bulk extraction isn't supported
	 * @since 7.2
	 */
	NameSpacePrimaryKeysInformation getPrimaryKeys(Identifier catalog, Identifier schema);

	/**
	 * Extract information about indexes defined against the given table.  Typically called from the TableInformation
	 * itself as part of on-demand initialization of its state.
	 *
	 * @param tableInformation The table for which to locate indexes
	 *
	 * @return The extracted index information
	 */
	Iterable<IndexInformation> getIndexes(TableInformation tableInformation);

	/**
	 * Extract all the indexes information.
	 *
	 * @param catalog Can be {@code null}, indicating that any catalog may be considered a match.  A
	 * non-{@code null} value indicates that search should be limited to the passed catalog.
	 * @param schema Can  be {@code null}, indicating that any schema may be considered a match.  A
	 * non-{@code null} value indicates that search should be limited to the passed schema .
	 *
	 * @return a {@link NameSpaceIndexesInformation}
	 * @throws SchemaExtractionException when bulk extraction isn't supported
	 * @since 7.2
	 */
	NameSpaceIndexesInformation getIndexes(Identifier catalog, Identifier schema);

	/**
	 * Extract information about foreign keys defined on the given table (targeting or point-at other tables).
	 * Typically called from the TableInformation itself as part of on-demand initialization of its state.
	 *
	 * @param tableInformation The table for which to locate foreign-keys
	 *
	 * @return The extracted foreign-key information
	 */
	Iterable<ForeignKeyInformation> getForeignKeys(TableInformation tableInformation);

	/**
	 * Extract all the foreign keys information.
	 *
	 * @param catalog Can be {@code null}, indicating that any catalog may be considered a match.  A
	 * non-{@code null} value indicates that search should be limited to the passed catalog.
	 * @param schema Can  be {@code null}, indicating that any schema may be considered a match.  A
	 * non-{@code null} value indicates that search should be limited to the passed schema .
	 *
	 * @return a {@link NameSpaceForeignKeysInformation}
	 * @throws SchemaExtractionException when bulk extraction isn't supported
	 * @since 7.2
	 */
	NameSpaceForeignKeysInformation getForeignKeys(Identifier catalog, Identifier schema);

	/**
	 * Can {@link #getPrimaryKeys(Identifier, Identifier)} be used?
	 *
	 * @since 7.2
	 */
	boolean supportsBulkPrimaryKeyRetrieval();

	/**
	 * Can {@link #getForeignKeys(Identifier, Identifier)} be used?
	 *
	 * @since 7.2
	 */
	boolean supportsBulkForeignKeyRetrieval();

	/**
	 * Can {@link #getIndexes(Identifier, Identifier)} be used?
	 *
	 * @since 7.2
	 */
	boolean supportsBulkIndexRetrieval();
}
