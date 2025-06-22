/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.models.spi.MemberDetails;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.hibernate.boot.model.naming.Identifier.toIdentifier;

/**
 * @author Gavin King
 */
class NaturalIdBinder {

	static void addNaturalIds(
			boolean inSecondPass,
			MemberDetails property,
			AnnotatedColumns columns,
			AnnotatedJoinColumns joinColumns,
			MetadataBuildingContext context) {
		// Natural ID columns must reside in one single UniqueKey within the Table.
		// For now, simply ensure consistent naming.
		final NaturalId naturalId = property.getDirectAnnotationUsage( NaturalId.class );
		if ( naturalId != null ) {
			final AnnotatedColumns annotatedColumns = joinColumns != null ? joinColumns : columns;
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
		final InFlightMetadataCollector collector = columns.getBuildingContext().getMetadataCollector();
		final Table table = columns.getTable();
		final UniqueKey uniqueKey = table.getOrCreateUniqueKey( name.render( collector.getDatabase().getDialect() ) );
		final Property property = columns.resolveProperty();
		if ( property.isComposite() ) {
			for ( Selectable selectable : property.getValue().getSelectables() ) {
				if ( selectable instanceof org.hibernate.mapping.Column column) {
					uniqueKey.addColumn( tableColumn( column, table, collector ) );
				}
			}
		}
		else {
			for ( AnnotatedColumn column : columns.getColumns() ) {
				uniqueKey.addColumn( tableColumn( column.getMappingColumn(), table, collector ) );
			}
		}
	}

	private static org.hibernate.mapping.Column tableColumn(
			org.hibernate.mapping.Column column, Table table, InFlightMetadataCollector collector) {
		final String columnName = collector.getLogicalColumnName( table, column.getQuotedName() );
		final org.hibernate.mapping.Column tableColumn = table.getColumn( collector, columnName );
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
