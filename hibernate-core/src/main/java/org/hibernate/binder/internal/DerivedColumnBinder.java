/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.binder.internal;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.DerivedColumn;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.binder.TypeBinder;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;

import static org.hibernate.internal.util.StringHelper.isBlank;

/**
 * Handles {@link DerivedColumn} annotations.
 */
public class DerivedColumnBinder implements AttributeBinder<DerivedColumn>, TypeBinder<DerivedColumn> {

	@Override
	public void bind(DerivedColumn annotation, MetadataBuildingContext context, PersistentClass entity) {
		addDerivedColumn( annotation, resolveEntityTable( annotation.table(), entity ), context );
	}

	@Override
	public void bind(DerivedColumn annotation, MetadataBuildingContext context, Component embeddable) {
		throw new AnnotationException( "Embeddable class '" + embeddable.getComponentClassName()
				+ "' was annotated '@DerivedColumn'" );
	}

	@Override
	public void bind(DerivedColumn annotation, MetadataBuildingContext context, PersistentClass entity, Property property) {
		if ( property.getValue() instanceof Collection collection ) {
			if ( collection.isInverse() ) {
				throw new AnnotationException( "Association '" + entity.getClassName() + "." + property.getName()
						+ "' is an unowned collection and may not be annotated '@DerivedColumn'" );
			}
			final var table = resolveTableForCollection( annotation.table(), collection, entity, property );
			addDerivedColumn( annotation, table, context );
		}
		else {
			throw new AnnotationException( "Property '" + entity.getClassName() + "." + property.getName()
					+ "' is not a collection and may not be annotated '@DerivedColumn'" );
		}
	}

	private static void addDerivedColumn(DerivedColumn annotation, Table table, MetadataBuildingContext context) {
		final var database = context.getMetadataCollector().getDatabase();
		final Identifier physicalName =
				context.getBuildingOptions().getPhysicalNamingStrategy()
						.toPhysicalColumnName(
								database.toIdentifier( annotation.name() ),
								database.getJdbcEnvironment()
						);
		final var existing = table.getColumn( physicalName );
		if ( existing == null ) {
			final var column = new Column();
			column.setName( physicalName.render( database.getDialect() ) );
			applyDerivedDefinition( column, annotation, context );
			final var basicValue = new BasicValue( context, table );
			basicValue.setExplicitJdbcTypeCode( annotation.sqlType() );
			basicValue.setExplicitJdbcTypeAccess( typeConfiguration ->
					typeConfiguration.getJdbcTypeRegistry()
							.getDescriptor( annotation.sqlType() ) );
			basicValue.addColumn( column );
			table.addColumn( column );
		}
		else {
			applyDerivedDefinition( existing, annotation, context );
		}
	}

	private static void applyDerivedDefinition(
			Column column,
			DerivedColumn annotation,
			MetadataBuildingContext context) {
		if ( !isBlank( annotation.comment() ) ) {
			column.setComment( annotation.comment() );
		}
		column.setSqlTypeCode( annotation.sqlType() );
		if ( column.getSqlType() == null ) {
			final var database = context.getMetadataCollector().getDatabase();
			column.setSqlType( database.getTypeConfiguration().getDdlTypeRegistry()
					.getTypeName( annotation.sqlType(), database.getDialect() ) );
		}
		column.setGeneratedAs( annotation.value() );
		column.setStored( annotation.stored() );
		column.setHidden( annotation.hidden() );
	}

	private static Table resolveEntityTable(String tableName, PersistentClass entity) {
		if ( isBlank( tableName ) ) {
			return entity.getTable();
		}
		else {
			final var resolved = findTable( entity, tableName );
			if ( resolved != null ) {
				return resolved;
			}
			throw new AnnotationException( "Secondary table '" + tableName + "' for entity '"
					+ entity.getClassName() + "' is not declared (use '@SecondaryTable' to declare the secondary table)" );
		}
	}


	private static Table resolveTableForCollection(
			String tableName,
			Collection collection,
			PersistentClass entity,
			Property property) {
		final var collectionTable = collection.getCollectionTable();
		if ( isBlank( tableName ) || collectionTable.getName().equalsIgnoreCase( tableName ) ) {
			return collectionTable;
		}
		throw new AnnotationException( "Association '" + entity.getClassName() + "." + property.getName()
				+ "' is mapped to table '" + collectionTable.getName()
				+ "' and may not be annotated '@DerivedColumn(table=\"" + tableName + "\")'" );
	}

	private static Table findTable(PersistentClass entity, String tableName) {
		final var primary = entity.getTable();
		if ( primary.getName().equalsIgnoreCase( tableName ) ) {
			return primary;
		}
		for ( var join : entity.getJoins() ) {
			final var secondary = join.getTable();
			if ( secondary.getName().equalsIgnoreCase( tableName ) ) {
				return secondary;
			}
		}
		return null;
	}

}
