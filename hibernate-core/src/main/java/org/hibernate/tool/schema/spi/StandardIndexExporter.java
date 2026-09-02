/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;


import org.hibernate.SPI;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.IndexColumn;
import org.hibernate.dialect.schema.spi.IndexDdlRequest;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.internal.util.StringHelper.isNotBlank;
import static org.hibernate.internal.util.StringHelper.qualify;

/// Stock exporter for relational [Index] definitions.
///
/// Instantiate this class when standard index DDL is sufficient. Implement
/// [Exporter] directly when the database requires a different complete index
/// operation, and supply the result from [Dialect#getIndexExporter()].
///
/// @see Dialect#getIndexExporter()
///
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public final class StandardIndexExporter implements Exporter<Index> {

	private final Dialect dialect;

	/// Create a standard index exporter owned by `dialect`.
	public StandardIndexExporter(Dialect dialect) {
		if ( dialect == null ) {
			throw new IllegalArgumentException( "dialect must not be null" );
		}
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlCreateStrings(Index index, Metadata metadata, SqlStringGenerationContext context) {
		final var request = indexDdlRequest( index );
		final var createIndex = new StringBuilder()
				.append( createIndexString( index, request ) )
				.append( " " )
				.append( indexName( index, context, metadata ) )
				.append( " on " )
				.append( context.format( index.getTable().getQualifiedTableName() ) );
		final String using = index.getUsing();
		if ( isNotBlank( using ) ) {
			createIndex.append( " using " ).append( using );
		}
		createIndex.append( " (" );
		appendColumnList( index, createIndex );
		createIndex.append( ")" );
		String options = index.getOptions();
		if ( isNotBlank( options ) ) {
			createIndex.append( " " ).append( options );
		}
		createIndex.append( dialect.getIndexDdlSupport().createTail( request ) );
		return new String[] { createIndex.toString() };
	}

	private String createIndexString(Index index, IndexDdlRequest request) {
		final String createIndexString = dialect.getIndexDdlSupport().createCommand( request );
		final String type = index.getType();
		return isBlank( type )
				? createIndexString
				: createIndexString.replaceFirst( " (?i:index)",
						' ' + type + " index" );
	}

	private String indexName(Index index, SqlStringGenerationContext context, Metadata metadata) {
		if ( dialect.getIndexDdlSupport().nameQualification() == IndexNameQualification.QUALIFIED ) {
			final var qualifiedTableName = index.getTable().getQualifiedTableName();
			return context.format(
					new QualifiedNameImpl(
							qualifiedTableName.getCatalogName(),
							qualifiedTableName.getSchemaName(),
							metadata.getDatabase().getJdbcEnvironment().getIdentifierHelper()
									.toIdentifier( index.getQuotedName( dialect ) )
					)
			);
		}
		else {
			return index.getName();
		}
	}

	private void appendColumnList(Index index, StringBuilder createIndex) {
		boolean first = true;
		final var columnOrderMap = index.getSelectableOrderMap();
		for ( var column : index.getSelectables() ) {
			if ( first ) {
				first = false;
			}
			else {
				createIndex.append( ", " );
			}
			createIndex.append( column.getText( dialect ) );
			if ( columnOrderMap.containsKey( column ) ) {
				createIndex.append( " " ).append( columnOrderMap.get( column ) );
			}
		}
	}

	@Override
	public String[] getSqlDropStrings(Index index, Metadata metadata, SqlStringGenerationContext context) {
		if ( dialect.getSchemaDropSupport().constraintDropMode() == ConstraintDropMode.IMPLICIT ) {
			return NO_COMMANDS;
		}
		else {
			final String tableName = context.format( index.getTable().getQualifiedTableName() );
			final String indexNameForCreation = dialect.getIndexDdlSupport().nameQualification()
					== IndexNameQualification.QUALIFIED
					? qualify( tableName, index.getName() )
					: index.getName();
			return new String[] {"drop index " + indexNameForCreation};
		}
	}

	private IndexDdlRequest indexDdlRequest(Index index) {
		return new IndexDdlRequest(
				index.isUnique(),
				index.getSelectables().stream()
						.map( selectable -> new IndexColumn(
								selectable.getText( dialect ),
								selectable instanceof Column column && column.isNullable()
						) )
						.toList()
		);
	}
}
