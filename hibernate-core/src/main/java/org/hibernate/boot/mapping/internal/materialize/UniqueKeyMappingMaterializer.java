/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import java.util.List;

import org.hibernate.MappingException;
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
public final class UniqueKeyMappingMaterializer {
	private UniqueKeyMappingMaterializer() {
	}

	public static UniqueKey materializeUniqueKey(ResolvedUniqueKey uniqueKey) {
		final List<Column> keyColumns = uniqueKey.columns();
		if ( keyColumns.size() == 1 ) {
			if ( uniqueKey.tableUniqueKey() ) {
				return materializeTableUniqueKey( uniqueKey );
			}
			return materializeSingleColumnUniqueKey(
					uniqueKey.table(),
					keyColumns.get( 0 ),
					uniqueKey.metadataBuildingContext()
			);
		}
		return materializeTableUniqueKey( uniqueKey );
	}

	private static UniqueKey materializeSingleColumnUniqueKey(
			Table table,
			Column column,
			MetadataBuildingContext context) {
		final String keyName = implicitUniqueKeyName( table, List.of( column ), null, context );
		table.markColumnUnique( keyName, column );
		return null;
	}

	private static UniqueKey materializeTableUniqueKey(ResolvedUniqueKey resolvedUniqueKey) {
		final UniqueKey uniqueKey = resolvedUniqueKey.table().getOrCreateUniqueKey( keyName( resolvedUniqueKey ) );
		applyMetadata( uniqueKey, resolvedUniqueKey );
		final List<Column> keyColumns = resolvedUniqueKey.columns();
		for ( int i = 0; i < keyColumns.size(); i++ ) {
			uniqueKey.addColumn( keyColumns.get( i ), columnOrdering( resolvedUniqueKey, i ) );
		}
		return uniqueKey;
	}

	private static void applyMetadata(UniqueKey uniqueKey, ResolvedUniqueKey resolvedUniqueKey) {
		uniqueKey.setExplicit( resolvedUniqueKey.explicit() );
		uniqueKey.setNameExplicit( resolvedUniqueKey.nameExplicit() );
		uniqueKey.setNullsNotDistinct( resolvedUniqueKey.nullsNotDistinct() );
		if ( StringHelper.isNotEmpty( resolvedUniqueKey.options() ) ) {
			uniqueKey.setOptions( resolvedUniqueKey.options() );
		}
	}

	private static String keyName(ResolvedUniqueKey uniqueKey) {
		return implicitUniqueKeyName(
				uniqueKey.table(),
				uniqueKey.columns(),
				uniqueKey.name(),
				uniqueKey.metadataBuildingContext()
		);
	}

	private static String columnOrdering(ResolvedUniqueKey uniqueKey, int position) {
		return uniqueKey.columnOrderings() == null ? null : uniqueKey.columnOrderings().get( position );
	}

	private static String implicitUniqueKeyName(
			Table table,
			List<Column> keyColumns,
			String userProvidedName,
			MetadataBuildingContext context) {
		return context.getBuildingPlan().getImplicitNamingStrategy()
				.determineUniqueKeyName( new ImplicitUniqueKeyNameSource() {
					@Override
					public Identifier getTableName() {
						return logicalTableName( table, context );
					}

					@Override
					public List<Identifier> getColumnNames() {
						return keyColumns.stream()
								.map( column -> column.getNameIdentifier( context ) )
								.toList();
					}

					@Override
					public Identifier getUserProvidedIdentifier() {
						return StringHelper.isEmpty( userProvidedName )
								? null
								: context.getMetadataCollector().getDatabase().toIdentifier( userProvidedName );
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						return context;
					}
				} )
				.render( context.getMetadataCollector().getDatabase().getDialect() );
	}

	private static Identifier logicalTableName(Table table, MetadataBuildingContext context) {
		try {
			return context.getMetadataCollector()
					.getDatabase()
					.toIdentifier( context.getMetadataCollector().getLogicalTableName( table ) );
		}
		catch (MappingException ignored) {
			return table.getNameIdentifier();
		}
	}
}
