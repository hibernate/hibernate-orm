/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
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

		final Property owningProperty;
		try {
			owningProperty = targetTypeBinder.getTypeBinding().getRecursiveProperty( inverseBinding.mappedBy() );
		}
		catch (MappingException e) {
			throw new AnnotationException(
					"Association '" + inverseBinding.ownerBinding().getEntityName()
							+ "." + inverseBinding.attributeMetadata().getName()
							+ "' is 'mappedBy' a property named '" + inverseBinding.mappedBy()
							+ "' which does not exist in the target entity '"
							+ targetTypeBinder.getTypeBinding().getEntityName() + "'",
					e
			);
		}
		final Value owningValue = owningProperty.getValue();
		if ( !( owningValue instanceof ToOne owningToOne ) ) {
			throw new MappingException(
					"Inverse @OneToOne mappedBy did not name an owning to-one attribute - "
							+ inverseBinding.ownerType().getClassDetails().getClassName()
							+ "." + inverseBinding.attributeMetadata().getName()
			);
		}
		if ( !isCompatibleMappedByType( inverseBinding.ownerBinding(), owningToOne.getReferencedEntityName() ) ) {
			throw new AnnotationException(
					"Association '" + inverseBinding.ownerBinding().getEntityName()
							+ "." + inverseBinding.attributeMetadata().getName()
							+ "' is 'mappedBy' a property named '" + inverseBinding.mappedBy()
							+ "' which references the wrong entity type '"
							+ owningToOne.getReferencedEntityName()
							+ "', expected '" + inverseBinding.ownerBinding().getEntityName() + "'"
			);
		}
		final Join owningJoin = findAssociationJoinContainingProperty( targetTypeBinder, owningProperty );
		if ( owningJoin != null ) {
			if ( owningToOne instanceof ManyToOne manyToOne ) {
				bindInverseJoinTableOneToOne( inverseBinding, targetTypeBinder, manyToOne, owningJoin );
			}
			return;
		}
		if ( !isTableInTypeHierarchy( targetTypeBinder.getTypeBinding(), owningToOne.getTable() ) ) {
			final Join joinedTable = findJoinContainingProperty( targetTypeBinder, owningProperty );
			if ( joinedTable == null ) {
				throw new MappingException(
						"Could not resolve joined table for inverse @OneToOne mappedBy - "
								+ inverseBinding.ownerType().getClassDetails().getClassName()
								+ "." + inverseBinding.attributeMetadata().getName()
				);
			}
			if ( owningToOne instanceof ManyToOne manyToOne ) {
				bindInverseJoinTableOneToOne( inverseBinding, targetTypeBinder, manyToOne, joinedTable );
			}
			return;
		}

		UniquePropertyReferenceBinder.bindUniquePropertyReference(
				bindingState,
				inverseBinding.value(),
				inverseBinding.mappedBy()
		);
		inverseBinding.value().sortProperties();
	}

	private boolean isCompatibleMappedByType(PersistentClass ownerBinding, String referencedEntityName) {
		final PersistentClass referencedBinding = bindingState.getEntityBinding( referencedEntityName );
		PersistentClass ownerType = ownerBinding;
		while ( ownerType != null ) {
			PersistentClass referencedType = referencedBinding;
			while ( referencedType != null ) {
				if ( ownerType.getTable() == referencedType.getTable() ) {
					return true;
				}
				referencedType = referencedType.getSuperPersistentClass();
			}
			ownerType = ownerType.getSuperPersistentClass();
		}
		return false;
	}

	private boolean isTableInTypeHierarchy(PersistentClass typeBinding, Table table) {
		PersistentClass current = typeBinding;
		while ( current != null ) {
			if ( current.getTable() == table ) {
				return true;
			}
			current = current.getSuperPersistentClass();
		}
		return false;
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
		inverseValue.setReferencedPropertyName( inverseBinding.mappedBy() );
		bindingState.addUniquePropertyReference( inverseValue.getReferencedEntityName(), inverseBinding.mappedBy() );
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
		value.setReferenceToPrimaryKey( true );
		value.setTypeName( targetTypeBinder.getTypeBinding().getEntityName() );
		value.setTypeUsingReflection(
				inverseBinding.ownerType().getClassDetails().getClassName(),
				inverseBinding.attributeMetadata().getName()
		);
		value.setFetchStyle( inverseBinding.value().getFetchStyle() );
		value.setLazy( inverseBinding.value().isLazy() );
		value.setOnDeleteAction( inverseBinding.value().getOnDeleteAction() );
		value.setUnwrapProxy( inverseBinding.value().isUnwrapProxy() );
		value.markAsLogicalOneToOne();
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

	private Join findJoinContainingProperty(EntityTypeBinder targetTypeBinder, Property property) {
		for ( Join join : targetTypeBinder.getTypeBinding().getJoins() ) {
			if ( join.containsProperty( property ) ) {
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
