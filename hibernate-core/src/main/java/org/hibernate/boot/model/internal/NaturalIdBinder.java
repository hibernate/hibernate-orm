/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.mapping.internal.materialize.ResolvedUniqueKey;
import org.hibernate.boot.mapping.internal.materialize.UniqueKeyMappingMaterializer;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.MemberDetails;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.hibernate.boot.model.naming.Identifier.toIdentifier;

/**
 * @author Gavin King
 */
class NaturalIdBinder {
	private static final UniqueKeyMappingMaterializer UNIQUE_KEY_MAPPING_MATERIALIZER =
			new UniqueKeyMappingMaterializer();

	static void addNaturalIds(
			boolean inSecondPass,
			MemberDetails property,
			AnnotatedColumns columns,
			AnnotatedJoinColumns joinColumns,
			MetadataBuildingContext context) {
		// Natural ID columns must reside in one single UniqueKey within the Table.
		// For now, simply ensure consistent naming.
		final var naturalId = property.getDirectAnnotationUsage( NaturalId.class );
		if ( naturalId != null ) {
			final var annotatedColumns = joinColumns != null ? joinColumns : columns;
			final Identifier name = uniqueKeyName( context, annotatedColumns );
			if ( inSecondPass ) {
				addColumnsToUniqueKey( annotatedColumns, name );
			}
			else {
				context.getMetadataCollector()
						.addSecondPass( persistentClasses -> addColumnsToUniqueKey( annotatedColumns, name ) );
			}
		}
	}

	private static Identifier uniqueKeyName(MetadataBuildingContext context, AnnotatedColumns annotatedColumns) {
		return context.getBuildingOptions().getImplicitNamingStrategy()
				.determineUniqueKeyName( new NaturalIdNameSource( annotatedColumns.getTable(), context) );
	}

	private static void addColumnsToUniqueKey(AnnotatedColumns columns, Identifier name) {
		final var collector = columns.getBuildingContext().getMetadataCollector();
		final var table = columns.getTable();
		final List<org.hibernate.mapping.Column> uniqueKeyColumns;
		final var property = columns.resolveProperty();
		if ( property.isComposite() ) {
			uniqueKeyColumns = new java.util.ArrayList<>( property.getValue().getSelectables().size() );
			for ( Selectable selectable : property.getValue().getSelectables() ) {
				if ( selectable instanceof org.hibernate.mapping.Column column) {
					uniqueKeyColumns.add( tableColumn( column, table, collector ) );
				}
			}
		}
		else {
			uniqueKeyColumns = new java.util.ArrayList<>( columns.getColumns().size() );
			for ( var column : columns.getColumns() ) {
				uniqueKeyColumns.add( tableColumn( column.getMappingColumn(), table, collector ) );
			}
		}
		UNIQUE_KEY_MAPPING_MATERIALIZER.materializeUniqueKey(
				ResolvedUniqueKey.named(
						table,
						uniqueKeyColumns,
						columns.getBuildingContext(),
						name.render( collector.getDatabase().getDialect() ),
						"natural-id"
				)
		);
	}

	private static org.hibernate.mapping.Column tableColumn(
			org.hibernate.mapping.Column column, Table table, InFlightMetadataCollector collector) {
		final String columnName = collector.getLogicalColumnName( table, column.getQuotedName() );
		final var tableColumn = table.getColumn( collector, columnName );
		if ( tableColumn == null ) {
			throw new AnnotationException(
					"Table '" + table.getName() + "' has no column named '" + columnName
							+ "' matching the column specified in '@Index'"
			);
		}
		return tableColumn;
	}

	private static class NaturalIdNameSource implements ImplicitUniqueKeyNameSource {
		private final Table table;
		private final MetadataBuildingContext context;

		NaturalIdNameSource(Table table, MetadataBuildingContext context) {
			this.table = table;
			this.context = context;
		}

		@Override
		public Identifier getTableName() {
			return table.getNameIdentifier();
		}

		@Override
		public List<Identifier> getColumnNames() {
			return singletonList( toIdentifier("_NaturalID") );
		}

		@Override
		public Identifier getUserProvidedIdentifier() {
			return null;
		}

		@Override
		public MetadataBuildingContext getBuildingContext() {
			return context;
		}
	}
}
