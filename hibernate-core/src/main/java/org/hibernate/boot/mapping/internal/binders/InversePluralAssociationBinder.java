/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.MappingException;
import org.hibernate.boot.mapping.internal.materialize.CollectionKeyMappingMaterializer;
import org.hibernate.boot.mapping.internal.sources.CollectionSource;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.MapKey;

/// Resolves inverse plural associations from their owning-side mapping objects.
///
/// @since 9.0
/// @author Steve Ebersole
class InversePluralAssociationBinder {
	private final EntityTypeBinder entityBinder;
	private final BindingState bindingState;
	private final CollectionKeyMappingMaterializer collectionKeyMappingMaterializer;

	InversePluralAssociationBinder(EntityTypeBinder entityBinder) {
		this.entityBinder = entityBinder;
		this.bindingState = entityBinder.getBindingState();
		this.collectionKeyMappingMaterializer = new CollectionKeyMappingMaterializer( bindingState::getEntityBinding );
	}

	void bindInverseAssociations() {
		bindingState.forEachInversePluralAssociationBinding( (inverseBinding) -> {
			if ( inverseBinding.ownerBinding() == entityBinder.getTypeBinding() ) {
				switch ( inverseBinding.nature() ) {
					case MANY_TO_MANY -> bindInverseManyToMany( inverseBinding );
					case ONE_TO_MANY -> bindInverseOneToMany( inverseBinding );
				}
			}
		} );
	}

	private void bindInverseManyToMany(InversePluralAssociationBinding inverseBinding) {
		final EntityTypeBinder targetTypeBinder = (EntityTypeBinder) bindingState.getTypeBinder(
				inverseBinding.targetClassDetails()
		);
		if ( targetTypeBinder == null ) {
			throw new MappingException(
					"Could not resolve local type binding for inverse plural association target entity - "
							+ describeType( inverseBinding.targetClassDetails() )
			);
		}

		final Property owningProperty = resolveMappedByProperty( targetTypeBinder, inverseBinding.mappedBy() );
		if ( !( owningProperty.getValue() instanceof Collection owningCollection ) ) {
			throw new MappingException(
					"Inverse plural association mappedBy did not name a collection-valued owning attribute - "
							+ inverseBinding.ownerType().getClassDetails().getClassName()
							+ "." + inverseBinding.attributeMetadata().getName()
			);
		}
		if ( owningCollection.isInverse() ) {
			throw new MappingException(
					"Inverse plural association mappedBy named another inverse collection - "
							+ inverseBinding.ownerType().getClassDetails().getClassName()
							+ "." + inverseBinding.attributeMetadata().getName()
			);
		}
		if ( !( owningCollection.getElement() instanceof ManyToOne owningElement ) ) {
			throw new MappingException(
					"Inverse @ManyToMany mappedBy did not name an owning many-to-many collection - "
							+ inverseBinding.ownerType().getClassDetails().getClassName()
							+ "." + inverseBinding.attributeMetadata().getName()
			);
		}
		if ( owningCollection.getKey() == null ) {
			throw new MappingException(
					"Owning many-to-many collection key was not available while resolving mappedBy - "
							+ owningCollection.getRole()
			);
		}

		final Collection inverseCollection = inverseBinding.collection();
		final Table collectionTable = owningCollection.getCollectionTable();
		inverseCollection.setCollectionTable( collectionTable );
		inverseCollection.setKey( createInverseKey( inverseBinding, collectionTable, owningElement ) );
		inverseCollection.setElement( createInverseElement( inverseBinding, collectionTable, targetTypeBinder, owningCollection ) );
		bindInverseIndex( inverseBinding, owningCollection, inverseCollection );
		materializePrimaryKeyIfNeeded( inverseCollection );
	}

	private void bindInverseOneToMany(InversePluralAssociationBinding inverseBinding) {
		final EntityTypeBinder targetTypeBinder = resolveTargetTypeBinder( inverseBinding );
		final Property owningProperty = resolveMappedByProperty( targetTypeBinder, inverseBinding.mappedBy() );
		final Value owningValue = owningProperty.getValue();
		if ( owningValue instanceof BasicValue owningBasicValue ) {
			bindInverseOneToMany( inverseBinding, targetTypeBinder, owningBasicValue );
			return;
		}
		if ( owningValue instanceof Any owningAny ) {
			bindInverseOneToMany( inverseBinding, targetTypeBinder, owningAny );
			return;
		}
		if ( !( owningValue instanceof ManyToOne owningToOne ) ) {
			throw new MappingException(
					"Inverse @OneToMany mappedBy did not name an owning to-one attribute - "
							+ inverseBinding.ownerType().getClassDetails().getClassName()
							+ "." + inverseBinding.attributeMetadata().getName()
			);
		}
		if ( !isSameOrSuperEntity( inverseBinding.ownerBinding(), owningToOne.getReferencedEntityName() ) ) {
			throw new MappingException(
					"Inverse @OneToMany mappedBy named a to-one attribute that targets `"
							+ owningToOne.getReferencedEntityName() + "` rather than `"
							+ inverseBinding.ownerBinding().getEntityName() + "` - "
							+ inverseBinding.ownerType().getClassDetails().getClassName()
							+ "." + inverseBinding.attributeMetadata().getName()
			);
		}

		final Collection inverseCollection = inverseBinding.collection();
		final Table collectionTable = owningToOne.getTable();
		inverseCollection.setCollectionTable( collectionTable );
		if ( !owningToOne.isReferenceToPrimaryKey() ) {
			inverseCollection.setReferencedPropertyName( owningToOne.getReferencedPropertyName() );
		}
		inverseCollection.setKey( createInverseKey( inverseBinding, collectionTable, owningToOne ) );
		final Join owningJoin = findAssociationJoinContainingProperty( targetTypeBinder, owningProperty );
		if ( owningJoin == null ) {
			inverseCollection.setElement( createOneToManyElement( inverseBinding, targetTypeBinder ) );
		}
		else {
			inverseCollection.setElement( createInverseElement( inverseBinding, collectionTable, targetTypeBinder, owningJoin ) );
		}
		bindInverseOneToManyIndex( inverseBinding, targetTypeBinder, inverseCollection );
		materializePrimaryKeyIfNeeded( inverseCollection );
	}

	private void bindInverseOneToMany(
			InversePluralAssociationBinding inverseBinding,
			EntityTypeBinder targetTypeBinder,
			BasicValue owningBasicValue) {
		final Collection inverseCollection = inverseBinding.collection();
		final Table collectionTable = owningBasicValue.getTable();
		inverseCollection.setCollectionTable( collectionTable );
		inverseCollection.setKey( createInverseKey( inverseBinding, collectionTable, owningBasicValue ) );
		inverseCollection.setElement( createOneToManyElement( inverseBinding, targetTypeBinder ) );
		bindInverseOneToManyIndex( inverseBinding, targetTypeBinder, inverseCollection );
		materializePrimaryKeyIfNeeded( inverseCollection );
	}

	private void bindInverseOneToMany(
			InversePluralAssociationBinding inverseBinding,
			EntityTypeBinder targetTypeBinder,
			Any owningAny) {
		final Collection inverseCollection = inverseBinding.collection();
		final Table collectionTable = owningAny.getTable();
		inverseCollection.setCollectionTable( collectionTable );
		inverseCollection.setKey( createInverseKey( inverseBinding, collectionTable, owningAny ) );
		inverseCollection.setElement( createOneToManyElement( inverseBinding, targetTypeBinder ) );
		bindInverseOneToManyIndex( inverseBinding, targetTypeBinder, inverseCollection );
		materializePrimaryKeyIfNeeded( inverseCollection );
	}

	private KeyValue createInverseKey(
			InversePluralAssociationBinding inverseBinding,
			Table collectionTable,
			ManyToOne owningElement) {
		final IdentifierBinding entityIdentifierBinding = bindingState.getIdentifierBinding(
				inverseBinding.ownerType().getHierarchy().getRoot()
		);
		if ( entityIdentifierBinding == null ) {
			throw new MappingException(
					"Could not resolve identifier binding for inverse plural association owner - "
							+ inverseBinding.ownerBinding().getEntityName()
			);
		}

		final DependantValue key = new DependantValue(
				bindingState.getMetadataBuildingContext(),
				collectionTable,
				owningElement.isReferenceToPrimaryKey()
						? entityIdentifierBinding.value()
						: (KeyValue) inverseBinding.ownerBinding()
								.getReferencedProperty( owningElement.getReferencedPropertyName() )
								.getValue()
		);
		key.setNullable( false );
		key.setUpdateable( false );
		for ( Selectable selectable : owningElement.getSelectables() ) {
			if ( selectable instanceof Column column ) {
				key.addColumn( copyColumn( collectionTable, column, false ), true, false );
			}
			else if ( selectable instanceof Formula formula ) {
				key.addFormula( copyFormula( formula ) );
			}
		}
		return key;
	}

	private KeyValue createInverseKey(
			InversePluralAssociationBinding inverseBinding,
			Table collectionTable,
			BasicValue owningBasicValue) {
		final IdentifierBinding entityIdentifierBinding = bindingState.getIdentifierBinding(
				inverseBinding.ownerType().getHierarchy().getRoot()
		);
		if ( entityIdentifierBinding == null ) {
			throw new MappingException(
					"Could not resolve identifier binding for inverse plural association owner - "
							+ inverseBinding.ownerBinding().getEntityName()
			);
		}

		final DependantValue key = new DependantValue(
				bindingState.getMetadataBuildingContext(),
				collectionTable,
				entityIdentifierBinding.value()
		);
		key.setNullable( false );
		key.setUpdateable( false );
		for ( Column column : owningBasicValue.getColumns() ) {
			key.addColumn( copyColumn( collectionTable, column, false ), true, false );
		}
		return key;
	}

	private KeyValue createInverseKey(
			InversePluralAssociationBinding inverseBinding,
			Table collectionTable,
			Any owningAny) {
		final IdentifierBinding entityIdentifierBinding = bindingState.getIdentifierBinding(
				inverseBinding.ownerType().getHierarchy().getRoot()
		);
		if ( entityIdentifierBinding == null ) {
			throw new MappingException(
					"Could not resolve identifier binding for inverse plural association owner - "
							+ inverseBinding.ownerBinding().getEntityName()
			);
		}

		final BasicValue owningKey = owningAny.getKeyDescriptor();
		if ( owningKey == null ) {
			throw new MappingException(
					"Could not resolve @Any key binding for inverse plural association mappedBy - "
							+ inverseBinding.ownerBinding().getEntityName()
							+ "." + inverseBinding.attributeMetadata().getName()
			);
		}

		final DependantValue key = new DependantValue(
				bindingState.getMetadataBuildingContext(),
				collectionTable,
				entityIdentifierBinding.value()
		);
		key.setNullable( false );
		key.setUpdateable( false );
		for ( Column column : owningKey.getColumns() ) {
			key.addColumn( copyColumn( collectionTable, column, false ), true, false );
		}
		return key;
	}

	private ManyToOne createInverseElement(
			InversePluralAssociationBinding inverseBinding,
			Table collectionTable,
			EntityTypeBinder targetTypeBinder,
			Collection owningCollection) {
		final ManyToOne element = new ManyToOne( bindingState.getMetadataBuildingContext(), collectionTable );
		element.setReferencedEntityName( targetTypeBinder.getTypeBinding().getEntityName() );
		element.setReferenceToPrimaryKey( true );
		element.setTypeName( targetTypeBinder.getTypeBinding().getEntityName() );
		element.setTypeUsingReflection(
				inverseBinding.ownerType().getClassDetails().getClassName(),
				inverseBinding.attributeMetadata().getName()
		);
		for ( Column owningKeyColumn : owningCollection.getKey().getColumns() ) {
			element.addColumn( copyColumn( collectionTable, owningKeyColumn, owningKeyColumn.isUnique() ) );
		}
		return element;
	}

	private ManyToOne createInverseElement(
			InversePluralAssociationBinding inverseBinding,
			Table collectionTable,
			EntityTypeBinder targetTypeBinder,
			Join owningJoin) {
		final ManyToOne element = new ManyToOne( bindingState.getMetadataBuildingContext(), collectionTable );
		element.setReferencedEntityName( targetTypeBinder.getTypeBinding().getEntityName() );
		element.setReferenceToPrimaryKey( true );
		element.setTypeName( targetTypeBinder.getTypeBinding().getEntityName() );
		element.setTypeUsingReflection(
				inverseBinding.ownerType().getClassDetails().getClassName(),
				inverseBinding.attributeMetadata().getName()
		);
		for ( Column owningKeyColumn : owningJoin.getKey().getColumns() ) {
			element.addColumn( copyColumn( collectionTable, owningKeyColumn, owningKeyColumn.isUnique() ) );
		}
		return element;
	}

	private void bindInverseIndex(
			InversePluralAssociationBinding inverseBinding,
			Collection owningCollection,
			Collection inverseCollection) {
		if ( !( inverseCollection instanceof org.hibernate.mapping.Map inverseMap ) ) {
			return;
		}
		if ( !( owningCollection instanceof org.hibernate.mapping.Map owningMap ) ) {
			throw new MappingException(
					"Inverse map-valued @ManyToMany mappedBy did not name a map-valued owning collection - "
							+ inverseBinding.ownerType().getClassDetails().getClassName()
							+ "." + inverseBinding.attributeMetadata().getName()
			);
		}
		inverseMap.setIndex( copyIndexValue( inverseBinding, inverseMap, owningMap.getIndex() ) );

		inverseMap.setMapKeyPropertyName( owningMap.getMapKeyPropertyName() );
		inverseMap.setHasMapKeyProperty( owningMap.hasMapKeyProperty() );
	}

	private void bindInverseOneToManyIndex(
			InversePluralAssociationBinding inverseBinding,
			EntityTypeBinder targetTypeBinder,
			Collection inverseCollection) {
		final CollectionSource source = CollectionSource.oneToMany(
				inverseBinding.attributeMetadata().getMember(),
				entityBinder.getBindingContext().getBootstrapContext().getModelsContext()
		);
		if ( !( inverseCollection instanceof org.hibernate.mapping.Map inverseMap ) ) {
			if ( inverseCollection instanceof IndexedCollection indexedCollection ) {
				CollectionIndexBinder.bindListIndex(
						source,
						indexedCollection,
						inverseCollection.getCollectionTable(),
						entityBinder.getOptions(),
						bindingState,
						entityBinder.getBindingContext()
				);
			}
			return;
		}

		final MapKey mapKey = source.mapKey();
		final String mapKeyName = source.mapKeyName();
		final Value mapKeyValue = mapKey == null || mapKeyName.isEmpty()
				? targetTypeBinder.getTypeBinding().getIdentifier()
				: resolveTargetMapKeyProperty( targetTypeBinder, mapKeyName ).getValue();
		CollectionIndexBinder.bindPropertyMapKeyValue(
				inverseMap,
				mapKeyValue,
				mapKey == null || mapKeyName.isEmpty() ? null : mapKeyName,
				bindingState
		);
	}

	private Property resolveTargetMapKeyProperty(EntityTypeBinder targetTypeBinder, String propertyName) {
		if ( propertyName.contains( "." ) ) {
			return CollectionIndexBinder.resolvePropertyPath( targetTypeBinder.getTypeBinding(), propertyName );
		}
		final Property identifierProperty = targetTypeBinder.getTypeBinding().getIdentifierProperty();
		if ( identifierProperty != null && identifierProperty.getName().equals( propertyName ) ) {
			return identifierProperty;
		}
		return targetTypeBinder.getTypeBinding().getProperty( propertyName );
	}

	private BasicValue copyBasicIndex(org.hibernate.mapping.Map inverseMap, BasicValue owningIndex) {
		final BasicValue inverseIndex = new BasicValue(
				bindingState.getMetadataBuildingContext(),
				inverseMap.getCollectionTable()
		);
		inverseIndex.copyTypeFrom( owningIndex );
		if ( inverseIndex.getImplicitJavaTypeAccess() == null ) {
			inverseIndex.setImplicitJavaTypeAccess( (typeConfiguration) -> owningIndex.resolve()
					.getDomainJavaType()
					.getJavaType() );
		}
		for ( Column column : owningIndex.getColumns() ) {
			inverseIndex.addColumn( copyColumn( inverseMap.getCollectionTable(), column, column.isUnique() ) );
		}
		return inverseIndex;
	}

	private Value copyIndexValue(
			InversePluralAssociationBinding inverseBinding,
			org.hibernate.mapping.Map inverseMap,
			Value owningIndex) {
		if ( owningIndex instanceof BasicValue owningBasicIndex ) {
			return copyBasicIndex( inverseMap, owningBasicIndex );
		}
		if ( owningIndex instanceof ManyToOne owningToOneIndex ) {
			return copyManyToOneIndex( inverseBinding, inverseMap, owningToOneIndex );
		}
		if ( owningIndex instanceof Component owningComponentIndex ) {
			return copyComponentIndex( inverseBinding, inverseMap, owningComponentIndex );
		}
		throw new UnsupportedOperationException(
				"Inverse map-valued @ManyToMany is only implemented for basic, to-one, and component owning map keys - "
						+ inverseMap.getRole()
		);
	}

	private Component copyComponentIndex(
			InversePluralAssociationBinding inverseBinding,
			org.hibernate.mapping.Map inverseMap,
			Component owningIndex) {
		final Component inverseIndex = new Component( bindingState.getMetadataBuildingContext(), inverseMap );
		inverseIndex.setComponentClassName( owningIndex.getComponentClassName() );
		inverseIndex.setEmbedded( owningIndex.isEmbedded() );
		inverseIndex.setDynamic( owningIndex.isDynamic() );
		inverseIndex.setRoleName( owningIndex.getRoleName() );
		for ( Property property : owningIndex.getProperties() ) {
			final Property copy = property.copy();
			copy.setValue( copyIndexValue( inverseBinding, inverseMap, property.getValue() ) );
			copy.setInsertable( false );
			copy.setUpdatable( false );
			copy.setPersistentClass( inverseMap.getOwner() );
			inverseIndex.addProperty( copy );
		}
		inverseIndex.sortProperties();
		return inverseIndex;
	}

	private ManyToOne copyManyToOneIndex(
			InversePluralAssociationBinding inverseBinding,
			org.hibernate.mapping.Map inverseMap,
			ManyToOne owningIndex) {
		final ManyToOne inverseIndex = new ManyToOne(
				bindingState.getMetadataBuildingContext(),
				inverseMap.getCollectionTable()
		);
		inverseIndex.setReferencedEntityName( owningIndex.getReferencedEntityName() );
		inverseIndex.setReferenceToPrimaryKey( owningIndex.isReferenceToPrimaryKey() );
		inverseIndex.setReferencedPropertyName( owningIndex.getReferencedPropertyName() );
		inverseIndex.setTypeName( owningIndex.getTypeName() );
		inverseIndex.setTypeUsingReflection(
				inverseBinding.ownerType().getClassDetails().getClassName(),
				inverseBinding.attributeMetadata().getName()
		);
		if ( owningIndex.isLogicalOneToOne() ) {
			inverseIndex.markAsLogicalOneToOne();
		}
		for ( Column column : owningIndex.getColumns() ) {
			inverseIndex.addColumn( copyColumn( inverseMap.getCollectionTable(), column, column.isUnique() ) );
		}
		return inverseIndex;
	}

	private OneToMany createOneToManyElement(
			InversePluralAssociationBinding inverseBinding,
			EntityTypeBinder targetTypeBinder) {
		final OneToMany element = new OneToMany(
				bindingState.getMetadataBuildingContext(),
				inverseBinding.ownerBinding()
		);
		element.setAssociatedClass( targetTypeBinder.getTypeBinding() );
		element.setReferencedEntityName( targetTypeBinder.getTypeBinding().getEntityName() );
		element.setTypeUsingReflection(
				inverseBinding.ownerType().getClassDetails().getClassName(),
				inverseBinding.attributeMetadata().getName()
		);
		return element;
	}

	private Join findAssociationJoinContainingProperty(EntityTypeBinder targetTypeBinder, Property property) {
		for ( Join join : targetTypeBinder.getTypeBinding().getJoins() ) {
			if ( join.containsProperty( property ) && bindingState.getAssociationTableBinding( join ) != null ) {
				return join;
			}
		}
		return null;
	}

	private EntityTypeBinder resolveTargetTypeBinder(InversePluralAssociationBinding inverseBinding) {
		final EntityTypeBinder targetTypeBinder = (EntityTypeBinder) bindingState.getTypeBinder(
				inverseBinding.targetClassDetails()
		);
		if ( targetTypeBinder == null ) {
			throw new MappingException(
					"Could not resolve local type binding for inverse plural association target entity - "
							+ describeType( inverseBinding.targetClassDetails() )
			);
		}
		return targetTypeBinder;
	}

	private void materializePrimaryKeyIfNeeded(Collection inverseCollection) {
		if ( !inverseCollection.getKey().hasFormula() ) {
			collectionKeyMappingMaterializer.materializePrimaryKeyIfNeeded(
					collectionKeyMappingMaterializer.resolveTableKey( inverseCollection )
			);
		}
	}

	private static String describeType(ClassDetails classDetails) {
		return classDetails.getName() + " (" + classDetails.getClassName() + ")";
	}

	private boolean isSameOrSuperEntity(PersistentClass ownerBinding, String referencedEntityName) {
		PersistentClass current = ownerBinding;
		while ( current != null ) {
			if ( current.getEntityName().equals( referencedEntityName ) ) {
				return true;
			}
			current = current.getSuperclass();
		}
		return false;
	}

	private Property resolveMappedByProperty(EntityTypeBinder targetTypeBinder, String mappedBy) {
		if ( mappedBy.indexOf( '.' ) < 0 ) {
			return targetTypeBinder.getTypeBinding().getProperty( mappedBy );
		}

		final String[] path = mappedBy.split( "\\." );
		Property property = targetTypeBinder.getTypeBinding().getProperty( path[0] );
		for ( int i = 1; i < path.length; i++ ) {
			if ( !( property.getValue() instanceof Component component ) ) {
				throw new MappingException(
						"Inverse plural association mappedBy path did not name an embedded attribute before `"
								+ path[i] + "` - " + mappedBy
				);
			}
			property = component.getProperty( path[i] );
		}
		return property;
	}

	private Column copyColumn(Table table, Column source, boolean unique) {
		final Column result = new Column( source.getName() );
		result.setLength( source.getLength() );
		result.setPrecision( source.getPrecision() );
		result.setScale( source.getScale() );
		result.setSqlType( source.getSqlType() );
		result.setNullable( source.isNullable() );
		result.setUnique( unique );
		table.addColumn( result );
		final Column canonicalColumn = table.getColumn( result );
		final Column column = canonicalColumn == null ? result : canonicalColumn;
		column.setNullable( source.isNullable() );
		column.setUnique( unique );
		return column;
	}

	private Formula copyFormula(Formula source) {
		return new Formula( source.getFormula() );
	}
}
