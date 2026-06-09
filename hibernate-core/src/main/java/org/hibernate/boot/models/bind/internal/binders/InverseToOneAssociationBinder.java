/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.MappingException;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;

/// Resolves inverse to-one associations from their owning-side mapping values.
///
/// @since 9.0
/// @author Steve Ebersole
class InverseToOneAssociationBinder {
	private final EntityTypeBinder entityBinder;
	private final BindingState bindingState;

	InverseToOneAssociationBinder(EntityTypeBinder entityBinder) {
		this.entityBinder = entityBinder;
		this.bindingState = entityBinder.getBindingState();
	}

	void bindInverseAssociations() {
		bindingState.forEachInverseToOneAssociationBinding( (inverseBinding) -> {
			if ( inverseBinding.ownerBinding() == entityBinder.getTypeBinding() ) {
				bindInverseOneToOne( inverseBinding );
			}
		} );
	}

	private void bindInverseOneToOne(InverseToOneAssociationBinding inverseBinding) {
		final EntityTypeBinder targetTypeBinder = (EntityTypeBinder) bindingState.getTypeBinder(
				inverseBinding.targetClassDetails()
		);
		if ( targetTypeBinder == null ) {
			throw new MappingException(
					"Could not resolve local type binding for inverse to-one association target entity - "
							+ inverseBinding.targetClassDetails().getClassName()
			);
		}

		final Property owningProperty = targetTypeBinder.getTypeBinding().getProperty( inverseBinding.mappedBy() );
		final Value owningValue = owningProperty.getValue();
		if ( !( owningValue instanceof ManyToOne owningToOne ) ) {
			throw new MappingException(
					"Inverse @OneToOne mappedBy did not name an owning to-one attribute - "
							+ inverseBinding.ownerType().getClassDetails().getClassName()
							+ "." + inverseBinding.attributeMetadata().getName()
			);
		}
		if ( !owningToOne.isLogicalOneToOne() ) {
			throw new MappingException(
					"Inverse @OneToOne mappedBy did not name an owning one-to-one attribute - "
							+ inverseBinding.ownerType().getClassDetails().getClassName()
							+ "." + inverseBinding.attributeMetadata().getName()
			);
		}
		if ( !inverseBinding.ownerBinding().getEntityName().equals( owningToOne.getReferencedEntityName() ) ) {
			throw new MappingException(
					"Inverse @OneToOne mappedBy named a to-one attribute that targets `"
							+ owningToOne.getReferencedEntityName() + "` rather than `"
							+ inverseBinding.ownerBinding().getEntityName() + "` - "
							+ inverseBinding.ownerType().getClassDetails().getClassName()
							+ "." + inverseBinding.attributeMetadata().getName()
			);
		}
		final Join owningJoin = findAssociationJoinContainingProperty( targetTypeBinder, owningProperty );
		if ( owningJoin != null ) {
			bindInverseJoinTableOneToOne( inverseBinding, targetTypeBinder, owningToOne, owningJoin );
			return;
		}
		if ( owningToOne.getTable() != targetTypeBinder.getTable() ) {
			throw new UnsupportedOperationException(
					"Inverse @OneToOne mappedBy through a joined table is not yet implemented"
			);
		}

		UniquePropertyReferenceBinder.bindUniquePropertyReference(
				bindingState,
				inverseBinding.value(),
				inverseBinding.mappedBy()
		);
		inverseBinding.value().sortProperties();
	}

	private void bindInverseJoinTableOneToOne(
			InverseToOneAssociationBinding inverseBinding,
			EntityTypeBinder targetTypeBinder,
			ManyToOne owningToOne,
			Join owningJoin) {
		final Join inverseJoin = createInverseJoin( inverseBinding, owningToOne, owningJoin.getTable() );
		final ManyToOne inverseValue = createInverseJoinTableValue(
				inverseBinding,
				targetTypeBinder,
				owningJoin
		);
		inverseBinding.property().setValue( inverseValue );
		inverseBinding.ownerBinding().addJoin( inverseJoin );
		inverseBinding.ownerBinding().movePropertyToJoin( inverseBinding.property(), inverseJoin );
	}

	private Join createInverseJoin(
			InverseToOneAssociationBinding inverseBinding,
			ManyToOne owningToOne,
			Table table) {
		final Join inverseJoin = new Join();
		inverseJoin.setPersistentClass( inverseBinding.ownerBinding() );
		inverseJoin.setTable( table );
		inverseJoin.setInverse( true );
		inverseJoin.setOptional( true );

		final DependantValue key = new DependantValue(
				bindingState.getMetadataBuildingContext(),
				table,
				inverseBinding.ownerBinding().getIdentifier()
		);
		key.setNullable( false );
		key.setUpdateable( false );
		for ( Column column : owningToOne.getColumns() ) {
			key.addColumn( copyColumn( table, column ), true, false );
		}
		inverseJoin.setKey( key );
		return inverseJoin;
	}

	private ManyToOne createInverseJoinTableValue(
			InverseToOneAssociationBinding inverseBinding,
			EntityTypeBinder targetTypeBinder,
			Join owningJoin) {
		final ManyToOne value = new ManyToOne( bindingState.getMetadataBuildingContext(), owningJoin.getTable() );
		value.setReferencedEntityName( targetTypeBinder.getTypeBinding().getEntityName() );
		value.setReferencedPropertyName( inverseBinding.mappedBy() );
		value.setReferenceToPrimaryKey( false );
		value.setTypeName( targetTypeBinder.getTypeBinding().getEntityName() );
		value.setTypeUsingReflection(
				inverseBinding.ownerType().getClassDetails().getClassName(),
				inverseBinding.attributeMetadata().getName()
		);
		value.markAsLogicalOneToOne();
		UniquePropertyReferenceBinder.bindUniquePropertyReference(
				bindingState,
				value,
				inverseBinding.mappedBy()
		);
		for ( Column column : owningJoin.getKey().getColumns() ) {
			value.addColumn( copyColumn( owningJoin.getTable(), column ) );
		}
		return value;
	}

	private Join findAssociationJoinContainingProperty(EntityTypeBinder targetTypeBinder, Property property) {
		for ( Join join : targetTypeBinder.getTypeBinding().getJoins() ) {
			if ( join.containsProperty( property ) && bindingState.getAssociationTableBinding( join ) != null ) {
				return join;
			}
		}
		return null;
	}

	private Column copyColumn(Table table, Column source) {
		final Column result = new Column( source.getName() );
		result.setLength( source.getLength() );
		result.setPrecision( source.getPrecision() );
		result.setScale( source.getScale() );
		result.setSqlType( source.getSqlType() );
		result.setNullable( false );
		result.setUnique( source.isUnique() );
		table.addColumn( result );
		final Column canonicalColumn = table.getColumn( result );
		return canonicalColumn == null ? result : canonicalColumn;
	}
}
