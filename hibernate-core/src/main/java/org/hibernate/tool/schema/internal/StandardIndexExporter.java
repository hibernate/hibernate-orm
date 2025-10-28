/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;


import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Index;
import org.hibernate.tool.schema.spi.Exporter;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.qualify;

/**
 * An {@link Exporter} for {@linkplain Index indexes}.
 *
 * @author Steve Ebersole
 */
public class StandardIndexExporter implements Exporter<Index> {

	private final Dialect dialect;

	public StandardIndexExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	protected Dialect getDialect() {
		return dialect;
	}

	@Override
	public String[] getSqlCreateStrings(Index index, Metadata metadata, SqlStringGenerationContext context) {
		final var createIndex = new StringBuilder()
				.append( dialect.getCreateIndexString( index.isUnique() ) )
				.append( " " )
				.append( indexName( index, context, metadata ) )
				.append( " on " )
				.append( context.format( index.getTable().getQualifiedTableName() ) )
				.append( " (" );
		appendColumnList( index, createIndex );
		createIndex.append( ")" );
		if ( isNotEmpty( index.getOptions() ) ) {
			createIndex.append( " " ).append( index.getOptions() );
		}
		return new String[] { createIndex.toString() };
	}

	private String indexName(Index index, SqlStringGenerationContext context, Metadata metadata) {
		if ( dialect.qualifyIndexName() ) {
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
		if ( !dialect.dropConstraints() ) {
			return NO_COMMANDS;
		}
		else {
			final String tableName = context.format( index.getTable().getQualifiedTableName() );
			final String indexNameForCreation = dialect.qualifyIndexName()
					? qualify( tableName, index.getName() )
					: index.getName();
			return new String[] {"drop index " + indexNameForCreation};
		}
	}
}
