/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation;
import org.hibernate.tool.schema.extract.spi.IndexInformation;
import org.hibernate.tool.schema.extract.spi.NameSpaceForeignKeysInformation;
import org.hibernate.tool.schema.extract.spi.NameSpaceIndexesInformation;
import org.hibernate.tool.schema.extract.spi.NameSpacePrimaryKeysInformation;
import org.hibernate.tool.schema.extract.spi.NameSpaceTablesInformation;
import org.hibernate.tool.schema.extract.spi.PrimaryKeyInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 7.2
 */
public class CachingDatabaseInformationImpl extends DatabaseInformationImpl {

	private final Map<Namespace.Name, NamespaceCacheEntry> namespaceCacheEntries = new HashMap<>();

	public CachingDatabaseInformationImpl(
			ServiceRegistry serviceRegistry,
			JdbcEnvironment jdbcEnvironment,
			SqlStringGenerationContext context,
			DdlTransactionIsolator ddlTransactionIsolator,
			SchemaManagementTool tool) throws SQLException {
		super( serviceRegistry, jdbcEnvironment, context, ddlTransactionIsolator, tool );
	}

	@Override
	public @Nullable TableInformation locateTableInformation(QualifiedTableName tableName) {
		final var namespace = new Namespace.Name( tableName.getCatalogName(), tableName.getSchemaName() );
		final var entry = namespaceCacheEntries.computeIfAbsent( namespace, k -> new NamespaceCacheEntry() );
		NameSpaceTablesInformation nameSpaceTablesInformation = entry.tableInformation;
		if ( nameSpaceTablesInformation == null ) {
			nameSpaceTablesInformation = extractor.getTables( namespace.catalog(), namespace.schema() );
			entry.tableInformation = nameSpaceTablesInformation;
		}
		return nameSpaceTablesInformation.getTableInformation( tableName.getTableName().getText() );
	}

	@Override
	public @Nullable PrimaryKeyInformation locatePrimaryKeyInformation(QualifiedTableName tableName) {
		final var namespace = new Namespace.Name( tableName.getCatalogName(), tableName.getSchemaName() );
		final var entry = namespaceCacheEntries.computeIfAbsent( namespace, k -> new NamespaceCacheEntry() );
		NameSpacePrimaryKeysInformation nameSpaceTablesInformation = entry.primaryKeysInformation;
		if ( nameSpaceTablesInformation == null ) {
			nameSpaceTablesInformation = extractor.getPrimaryKeys( namespace.catalog(), namespace.schema() );
			entry.primaryKeysInformation = nameSpaceTablesInformation;
		}
		return nameSpaceTablesInformation.getPrimaryKeyInformation( tableName.getTableName().getText() );
	}

	@Override
	public Iterable<ForeignKeyInformation> locateForeignKeyInformation(QualifiedTableName tableName) {
		final var namespace = new Namespace.Name( tableName.getCatalogName(), tableName.getSchemaName() );
		final var entry = namespaceCacheEntries.computeIfAbsent( namespace, k -> new NamespaceCacheEntry() );
		NameSpaceForeignKeysInformation nameSpaceTablesInformation = entry.foreignKeysInformation;
		if ( nameSpaceTablesInformation == null ) {
			nameSpaceTablesInformation = extractor.getForeignKeys( namespace.catalog(), namespace.schema() );
			entry.foreignKeysInformation = nameSpaceTablesInformation;
		}
		final List<ForeignKeyInformation> foreignKeysInformation =
				nameSpaceTablesInformation.getForeignKeysInformation( tableName.getTableName().getText() );
		return foreignKeysInformation == null ? Collections.emptyList() : foreignKeysInformation;
	}

	@Override
	public Iterable<IndexInformation> locateIndexesInformation(QualifiedTableName tableName) {
		final var namespace = new Namespace.Name( tableName.getCatalogName(), tableName.getSchemaName() );
		final var entry = namespaceCacheEntries.computeIfAbsent( namespace, k -> new NamespaceCacheEntry() );
		NameSpaceIndexesInformation nameSpaceTablesInformation = entry.indexesInformation;
		if ( nameSpaceTablesInformation == null ) {
			nameSpaceTablesInformation = extractor.getIndexes( namespace.catalog(), namespace.schema() );
			entry.indexesInformation = nameSpaceTablesInformation;
		}
		final List<IndexInformation> indexesInformation =
				nameSpaceTablesInformation.getIndexesInformation( tableName.getTableName().getText() );
		return indexesInformation == null ? Collections.emptyList() : indexesInformation;
	}

	@Override
	public boolean isCaching() {
		return true;
	}

	private static class NamespaceCacheEntry {
		NameSpaceTablesInformation tableInformation;
		NameSpacePrimaryKeysInformation primaryKeysInformation;
		NameSpaceForeignKeysInformation foreignKeysInformation;
		NameSpaceIndexesInformation indexesInformation;
	}
}
