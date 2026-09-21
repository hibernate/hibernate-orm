/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import org.hibernate.boot.model.naming.internal.ImplicitNamingSourceHelper;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.internal.ConstraintNamingHelper;
import org.hibernate.boot.model.naming.ImplicitIndexNameSource;
import org.hibernate.boot.model.naming.internal.ImplicitNamingContextImpl;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;

/// Explicit materializer for physical indexes.
///
/// @since 9.0
/// @author Steve Ebersole
public final class IndexMappingMaterializer {
	private IndexMappingMaterializer() {
	}

	public static Index materializeIndex(ResolvedIndex resolvedIndex) {
		final Index index = resolvedIndex.table().getOrCreateIndex( indexName( resolvedIndex ) );
		index.setUnique( resolvedIndex.unique() );
		if ( StringHelper.isNotEmpty( resolvedIndex.type() ) ) {
			index.setType( resolvedIndex.type() );
		}
		if ( StringHelper.isNotEmpty( resolvedIndex.using() ) ) {
			index.setUsing( resolvedIndex.using() );
		}
		if ( StringHelper.isNotEmpty( resolvedIndex.options() ) ) {
			index.setOptions( resolvedIndex.options() );
		}
		final List<Selectable> selectables = resolvedIndex.selectables();
		for ( int i = 0; i < selectables.size(); i++ ) {
			index.addColumn( selectables.get( i ), columnOrdering( resolvedIndex, i ) );
		}
		return index;
	}

	private static String indexName(ResolvedIndex resolvedIndex) {
		return implicitIndexName(
				resolvedIndex.table(),
				resolvedIndex.columnNames(),
				resolvedIndex.name(),
				resolvedIndex.metadataBuildingContext()
		);
	}

	private static String columnOrdering(ResolvedIndex resolvedIndex, int position) {
		return resolvedIndex.columnOrderings() == null ? null : resolvedIndex.columnOrderings().get( position );
	}

	private static String implicitIndexName(
			Table table,
			List<String> columnNames,
			String userProvidedName,
			MetadataBuildingContext context) {
		return ConstraintNamingHelper.resolve( userProvidedName, () -> context.getBuildingPlan().getImplicitNamingStrategy()
				.determineIndexName( new ImplicitIndexNameSource() {
					@Override
					public Identifier getTableName() {
						return logicalTableName( table, context );
					}

					@Override
					public List<Identifier> getColumnNames() {
						return columnNames.stream()
								.map( context.getMetadataCollector().getDatabase()::toIdentifier )
								.toList();
					}

					@Override
					public Identifier getUserProvidedIdentifier() {
						return StringHelper.isEmpty( userProvidedName )
								? null
								: context.getMetadataCollector().getDatabase().toIdentifier( userProvidedName );
					}

					@Override
					public ImplicitNamingContext getNamingContext() {
						return ImplicitNamingContextImpl.from( context );
					}
				} ), ConstraintNamingHelper.Kind.INDEX, context );
	}

	private static Identifier logicalTableName(Table table, MetadataBuildingContext context) {
		try {
			return context.getMetadataCollector()
					.getDatabase()
					.toIdentifier( context.getMetadataCollector().getLogicalTableName( table ) );
		}
		catch (MappingException ignored) {
			return ImplicitNamingSourceHelper.tableName( table );
		}
	}
}
