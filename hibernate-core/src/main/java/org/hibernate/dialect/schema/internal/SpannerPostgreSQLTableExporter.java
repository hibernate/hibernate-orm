/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.internal;

import org.hibernate.dialect.Dialect;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.tool.schema.spi.StandardTableExporter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class SpannerPostgreSQLTableExporter extends StandardTableExporter {

	public SpannerPostgreSQLTableExporter(Dialect dialect) {
		super( dialect );
	}

	@Override
	public String[] getSqlDropStrings(Table table, Metadata metadata, SqlStringGenerationContext context) {
		// Spanner requires the indexes to be dropped before dropping the table
		List<String> sqlDropIndexStrings = new ArrayList<>();
		for ( Index index : table.getIndexes().values() ) {
			sqlDropIndexStrings.add( sqlDropIndexString(index.getName()) );
		}
		// Spanner requires all the unique indexes to be dropped before dropping the tables
		for ( UniqueKey uniqueKey : table.getUniqueKeys().values() ) {
			sqlDropIndexStrings.add( sqlDropIndexString(uniqueKey.getName()) );
		}
		for ( Column column : table.getColumns() ) {
			if ( column.isUnique() ) {
				sqlDropIndexStrings.add( sqlDropIndexString(column.getUniqueKeyName()) );
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
