/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.internal;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.tool.schema.spi.StandardTableExporter;

import java.util.ArrayList;
import java.util.stream.Stream;

/// Built-in exporter for Cloud Spanner create and drop table statements.
///
/// @author Steve Ebersole
/// @author Chengyuan Zhao
/// @author Daniel Zou
public final class SpannerDialectTableExporter extends StandardTableExporter {

	public SpannerDialectTableExporter(SpannerDialect spannerDialect) {
		super( spannerDialect );
	}

	@Override
	public String[] getSqlDropStrings(Table table, Metadata metadata, SqlStringGenerationContext context) {
		final ArrayList<String> sqlDropIndexStrings = new ArrayList<>();
		for ( var index : table.getIndexes().values() ) {
			sqlDropIndexStrings.add( sqlDropIndexString( index.getName() ) );
		}
		for ( UniqueKey uniqueKey : table.getUniqueKeys().values() ) {
			sqlDropIndexStrings.add( sqlDropIndexString( uniqueKey.getName() ) );
		}
		for ( Column column : table.getColumns() ) {
			if ( column.isUnique() ) {
				sqlDropIndexStrings.add( sqlDropIndexString( column.getUniqueKeyName() ) );
			}
		}
		String[] sqlDropStrings = super.getSqlDropStrings( table, metadata, context );
		return Stream.concat( sqlDropIndexStrings.stream(), Stream.of( sqlDropStrings ) )
				.toArray( String[]::new );
	}

	private String sqlDropIndexString(String indexName) {
		return "drop index if exists " + indexName;
	}
}
