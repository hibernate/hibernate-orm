/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import java.util.List;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

/// Explicit materializer bridge for physical unique-key constraints.
///
/// @since 9.0
/// @author Steve Ebersole
public class UniqueKeyMappingMaterializer {
	public UniqueKey materializeUniqueKey(ResolvedUniqueKey uniqueKey) {
		final List<Column> keyColumns = uniqueKey.columns();
		if ( keyColumns.size() == 1 ) {
			if ( uniqueKey.tableUniqueKey() ) {
				return materializeExplicitUniqueKey( uniqueKey );
			}
			return materializeSingleColumnUniqueKey(
					uniqueKey.table(),
					keyColumns.get( 0 ),
					uniqueKey.metadataBuildingContext()
			);
		}
		return materializeMultiColumnUniqueKey(
				uniqueKey
		);
	}

	private UniqueKey materializeSingleColumnUniqueKey(
			Table table,
			Column column,
			MetadataBuildingContext context) {
		final String keyName = implicitUniqueKeyName( table, List.of( column ), context );
		column.setUniqueKeyName( keyName );
		column.setUnique( true );
		return null;
	}

	private UniqueKey materializeMultiColumnUniqueKey(
			ResolvedUniqueKey resolvedUniqueKey) {
		final Table table = resolvedUniqueKey.table();
		final String keyName = keyName( resolvedUniqueKey );
		final UniqueKey uniqueKey = table.getOrCreateUniqueKey( keyName );
		applyMetadata( uniqueKey, resolvedUniqueKey );
		final List<Column> keyColumns = resolvedUniqueKey.columns();
		for ( int i = 0; i < keyColumns.size(); i++ ) {
			uniqueKey.addColumn( keyColumns.get( i ), columnOrdering( resolvedUniqueKey, i ) );
		}
		return uniqueKey;
	}

	private UniqueKey materializeExplicitUniqueKey(ResolvedUniqueKey resolvedUniqueKey) {
		final Table table = resolvedUniqueKey.table();
		final UniqueKey uniqueKey = table.getOrCreateUniqueKey( keyName( resolvedUniqueKey ) );
		applyMetadata( uniqueKey, resolvedUniqueKey );
		final List<Column> keyColumns = resolvedUniqueKey.columns();
		for ( int i = 0; i < keyColumns.size(); i++ ) {
			uniqueKey.addColumn( keyColumns.get( i ), columnOrdering( resolvedUniqueKey, i ) );
		}
		return uniqueKey;
	}

	private void applyMetadata(UniqueKey uniqueKey, ResolvedUniqueKey resolvedUniqueKey) {
		uniqueKey.setExplicit( resolvedUniqueKey.explicit() );
		uniqueKey.setNameExplicit( resolvedUniqueKey.nameExplicit() );
		uniqueKey.setNullsNotDistinct( resolvedUniqueKey.nullsNotDistinct() );
		if ( StringHelper.isNotEmpty( resolvedUniqueKey.options() ) ) {
			uniqueKey.setOptions( resolvedUniqueKey.options() );
		}
	}

	private String keyName(ResolvedUniqueKey uniqueKey) {
		if ( StringHelper.isNotEmpty( uniqueKey.name() ) ) {
			return uniqueKey.name();
		}
		return implicitUniqueKeyName(
				uniqueKey.table(),
				uniqueKey.columns(),
				uniqueKey.metadataBuildingContext()
		);
	}

	private String columnOrdering(ResolvedUniqueKey uniqueKey, int position) {
		return uniqueKey.columnOrderings() == null ? null : uniqueKey.columnOrderings().get( position );
	}

	private String implicitUniqueKeyName(
			Table table,
			List<Column> keyColumns,
			MetadataBuildingContext context) {
		return context.getBuildingOptions().getImplicitNamingStrategy()
				.determineUniqueKeyName( new ImplicitUniqueKeyNameSource() {
					@Override
					public Identifier getTableName() {
						return table.getNameIdentifier();
					}

					@Override
					public List<Identifier> getColumnNames() {
						return keyColumns.stream()
								.map( column -> column.getNameIdentifier( context ) )
								.toList();
					}

					@Override
					public Identifier getUserProvidedIdentifier() {
						return null;
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						return context;
					}
				} )
				.render( context.getMetadataCollector().getDatabase().getDialect() );
	}
}
