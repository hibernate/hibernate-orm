/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.mapping.internal.binders.SelectableCorrespondence;
import org.hibernate.boot.mapping.internal.binders.SelectableOrderResolution;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.ForeignKeyColumnMapping;
import org.hibernate.mapping.ForeignKeyColumnMappings;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.ToOne;

/// Explicit materializer for physical foreign-key constraints.
///
/// New binder code should route foreign-key creation here instead of calling
/// hidden `Value` or `KeyValue` creation helpers directly.
///
/// @since 9.0
/// @author Steve Ebersole
public final class ForeignKeyMappingMaterializer {
	private ForeignKeyMappingMaterializer() {
	}

	public static ForeignKey materializeForeignKey(
			ManyToOne value,
			PersistentClass referencedClass,
			String sourceRole) {
		if ( value.getReferencedPropertyName() == null ) {
			return null;
		}

		value.sortProperties( entityName -> referencedClass.getEntityName().equals( entityName ) ? referencedClass : null );

		final String referencedPropertyName = value.getReferencedPropertyName();
		final Property property = referencedClass.getReferencedProperty( referencedPropertyName );
		if ( property == null ) {
			throw new MappingException( "Referenced entity '" + referencedClass.getEntityName()
					+ "' has no property named '" + referencedPropertyName + "'" );
		}

		if ( property.getValue() instanceof Component component ) {
			component.sortProperties();
		}

		if ( value.isConstrained() && !hasAuxiliaryColumnInPrimaryKey( referencedClass ) ) {
			final ForeignKey foreignKey = materializeForeignKey(
					new ResolvedForeignKey(
							value.getTable(),
							value.getForeignKeyName(),
							value.getType().getAssociatedEntityName(),
							value.getForeignKeyDefinition(),
							value.getForeignKeyOptions(),
							value.getOnDeleteAction(),
							columnMappings( value, new ArrayList<>( property.getColumns() ), sourceRole ),
							property.getValue().getTable()
					),
					referencedClass
			);
			if ( foreignKey != null ) {
				foreignKey.setReferencedTable( property.getValue().getTable() );
			}
			return foreignKey;
		}

		return null;
	}

	public static ForeignKey materializeForeignKey(
			KeyValue key,
			PersistentClass referencedEntity,
			String sourceRole) {
		if ( key instanceof SimpleValue simpleValue ) {
			if ( !simpleValue.isConstrained() ) {
				return null;
			}
			return materializeForeignKey(
					ResolvedForeignKey.from(
							simpleValue,
							referencedEntity.getEntityName(),
							columnMappings( simpleValue, referencedEntity.getIdentifier().getColumns(), sourceRole )
					),
					referencedEntity
			);
		}
		return null;
	}

	public static ForeignKey materializeForeignKey(
			KeyValue key,
			PersistentClass referencedEntity,
			String sourceRole,
			List<Column> referencedColumns) {
		if ( key instanceof SimpleValue simpleValue ) {
			if ( !simpleValue.isConstrained() ) {
				return null;
			}
			return materializeForeignKey(
					ResolvedForeignKey.from(
							simpleValue,
							referencedEntity.getEntityName(),
							columnMappings( simpleValue, referencedColumns, sourceRole )
					),
					referencedEntity
			);
		}
		return null;
	}

	public static ForeignKey materializeForeignKey(ResolvedForeignKey foreignKey, PersistentClass referencedEntity) {
		if ( foreignKey.selectableOrder().isEmpty()
				|| referencedEntity.getRootClass().isAuxiliaryColumnInPrimaryKey() ) {
			return null;
		}

		final ForeignKey mappingForeignKey = foreignKey.table().createForeignKey(
				foreignKey.foreignKeyName(),
				foreignKeyColumnMappings( foreignKey, referencedEntity ),
				foreignKey.referencedEntityName(),
				foreignKey.foreignKeyDefinition(),
				foreignKey.foreignKeyOptions()
		);
		mappingForeignKey.setOnDeleteAction( foreignKey.onDeleteAction() );
		if ( foreignKey.referencedTable() != null ) {
			mappingForeignKey.setReferencedTable( foreignKey.referencedTable() );
		}
		return mappingForeignKey;
	}

	private static ForeignKeyColumnMappings foreignKeyColumnMappings(
			ResolvedForeignKey foreignKey,
			PersistentClass referencedEntity) {
		final SelectableOrderResolution selectableOrder = foreignKey.selectableOrder();
		return new ForeignKeyColumnMappings(
				selectableOrder.correspondences().stream()
						.map( (correspondence) -> new ForeignKeyColumnMapping(
								correspondence.localColumn(),
								referencesPrimaryKey( selectableOrder, referencedEntity )
										? null
										: correspondence.referencedColumn()
						) )
						.toList()
		);
	}

	private static boolean referencesPrimaryKey(
			SelectableOrderResolution selectableOrder,
			PersistentClass referencedEntity) {
		final var primaryKey = referencedEntity.getTable().getPrimaryKey();
		return primaryKey != null && primaryKey.getColumns().equals( selectableOrder.referencedColumns() );
	}

	public static ForeignKey materializeForeignKey(ToOne value, PersistentClass referencedEntity, String sourceRole) {
		value.sortProperties( entityName -> referencedEntity.getEntityName().equals( entityName ) ? referencedEntity : null );
		if ( !value.isForeignKeyEnabled()
				|| !value.isReferenceToPrimaryKey()
				|| value.getReferencedPropertyName() != null
				|| value.hasFormula() ) {
			return null;
		}
		return materializeForeignKey(
				ResolvedForeignKey.from(
						value,
						value.getReferencedEntityName(),
						columnMappings( value, referencedEntity.getIdentifier().getColumns(), sourceRole )
				),
				referencedEntity
		);
	}

	private static SelectableOrderResolution columnMappings(
			SimpleValue value,
			List<Column> referencedColumns,
			String sourceRole) {
		final var localColumns = value.getConstraintColumns();
		if ( localColumns.size() != referencedColumns.size() ) {
			throw new MappingException(
					"Foreign key column count did not match referenced column count for " + sourceRole
			);
		}
		final var correspondences = new ArrayList<SelectableCorrespondence>( localColumns.size() );
		for ( int i = 0; i < localColumns.size(); i++ ) {
			correspondences.add( new SelectableCorrespondence(
					localColumns.get( i ),
					referencedColumns.get( i ),
					i,
					i,
					sourceRole
			) );
		}
		return new SelectableOrderResolution( correspondences );
	}

	private static boolean hasAuxiliaryColumnInPrimaryKey(PersistentClass referencedEntity) {
		return referencedEntity.getRootClass().isAuxiliaryColumnInPrimaryKey();
	}
}
