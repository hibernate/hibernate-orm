/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringJoiner;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.Exporter;

import static java.util.Collections.addAll;
import static java.util.Collections.emptyList;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_STRING_ARRAY;

/**
 * The exporter for Cloud Spanner CREATE and DROP table statements.
 *
 * @author Chengyuan Zhao
 * @author Daniel Zou
 */
class SpannerDialectTableExporter implements Exporter<Table> {

	private final SpannerDialect spannerDialect;

	private final String createTableTemplate;

	/**
	 * Constructor.
	 *
	 * @param spannerDialect a Cloud Spanner dialect.
	 */
	public SpannerDialectTableExporter(SpannerDialect spannerDialect) {
		this.spannerDialect = spannerDialect;
		createTableTemplate = spannerDialect.getCreateTableString() + " {0} ({1}) PRIMARY KEY ({2})";
	}

	@Override
	public String[] getSqlCreateStrings(Table table, Metadata metadata, SqlStringGenerationContext context) {
		return getTableString( table, metadata, getKeyColumns( table ), context );
	}

	private static Collection<Column> getKeyColumns(Table table) {
		if ( table.hasPrimaryKey() ) {
			// a typical table that corresponds to an entity type
			return table.getPrimaryKey().getColumns();
		}
		else if ( !table.getForeignKeyCollection().isEmpty() ) {
			// a table with no PK's but has FK's; often corresponds to element collection properties
			return table.getColumns();
		}
		else {
			// the case corresponding to a sequence-table that will only have 1 row.
			return emptyList();
		}
	}

	private String[] getTableString(
			Table table,
			Metadata metadata,
			Iterable<Column> keyColumns,
			SqlStringGenerationContext context) {
		final ArrayList<String> statements = new ArrayList<>();
		statements.add(
				MessageFormat.format(
						createTableTemplate,
						context.format( table.getQualifiedTableName() ),
						columnsWithTypes( table, metadata ),
						primaryKeyColumnNames( keyColumns )
				)
		);
		for ( var initCommand : table.getInitCommands( context ) ) {
			addAll( statements, initCommand.initCommands() );
		}
		return statements.toArray( EMPTY_STRING_ARRAY );
	}

	private String primaryKeyColumnNames(Iterable<Column> keyColumns) {
		final var primaryKeyColNames = new StringJoiner( "," );
		for ( var col : keyColumns ) {
			primaryKeyColNames.add( col.getQuotedName( spannerDialect ) );
		}
		return primaryKeyColNames.toString();
	}

	private String columnsWithTypes(Table table, Metadata metadata) {
		final var colsAndTypes = new StringJoiner( "," );
		for ( var column : table.getColumns() ) {
			final String sqlType = column.getSqlType( metadata );
			final String columnDeclaration =
					column.getQuotedName( spannerDialect )
							+ " " + sqlType
							+ ( column.isNullable() ? spannerDialect.getNullColumnString( sqlType ) : " not null" );
			colsAndTypes.add( columnDeclaration );
		}
		return colsAndTypes.toString();
	}

	@Override
	public String[] getSqlDropStrings(Table table, Metadata metadata, SqlStringGenerationContext context) {
		// Cloud Spanner requires examining the metadata to find all indexes and interleaved tables.
		// These must be dropped before the given table can be dropped.
		// The current implementation does not support interleaved tables.

		final ArrayList<String> dropStrings = new ArrayList<>();
		for ( var index : table.getIndexes().values() ) {
			dropStrings.add( "drop index if exists " + index.getName() );
		}
		dropStrings.add( spannerDialect.getDropTableString( context.format( table.getQualifiedTableName() ) ) );
		return dropStrings.toArray( EMPTY_STRING_ARRAY );
	}
}
