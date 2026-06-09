/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.models.bind.internal.sources.AnySource;
import org.hibernate.boot.models.bind.internal.sources.CollectionSource;
import org.hibernate.boot.models.bind.internal.sources.ForeignKeySource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

/// Binds association-valued plural attributes.
///
/// Owning many-to-many and unidirectional one-to-many mappings create their
/// collection table and element association immediately, then defer the owner key
/// through [CollectionTableBinding].  Inverse plural mappings are registered as
/// [InversePluralAssociationBinding] because their table, key, element, and map
/// key details are copied from the owning side after member and table-key phases
/// have run.
///
/// @since 9.0
/// @author Steve Ebersole
class PluralAssociationAttributeBinder {
	private final IdentifiableTypeMetadata ownerType;
	private final PersistentClass ownerBinding;
	private final AttributeMetadata attributeMetadata;
	private final ModelBinders modelBinders;
	private final BindingOptions bindingOptions;
	private final BindingState bindingState;
	private final BindingContext bindingContext;

	PluralAssociationAttributeBinder(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			AttributeMetadata attributeMetadata,
			ModelBinders modelBinders,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		this.ownerType = ownerType;
		this.ownerBinding = ownerBinding;
		this.attributeMetadata = attributeMetadata;
		this.modelBinders = modelBinders;
		this.bindingOptions = bindingOptions;
		this.bindingState = bindingState;
		this.bindingContext = bindingContext;
	}

	Collection bindManyToMany(Property property) {
		final CollectionSource source = CollectionSource.manyToMany( attributeMetadata.getMember() );
		final ManyToMany manyToMany = source.manyToMany();
		if ( manyToMany != null && StringHelper.isNotEmpty( manyToMany.mappedBy() ) ) {
			return bindInverseManyToMany( source, manyToMany.mappedBy(), property );
		}
		return bindAssociation( source, false, property );
	}

	Collection bindOneToMany(Property property) {
		final CollectionSource source = CollectionSource.oneToMany( attributeMetadata.getMember() );
		final OneToMany oneToMany = source.oneToMany();
		if ( oneToMany != null && StringHelper.isNotEmpty( oneToMany.mappedBy() ) ) {
			return bindInverseOneToMany( source, oneToMany.mappedBy(), property );
		}
		return bindAssociation( source, true, property );
	}

	Collection bindManyToAny(Property property) {
		final CollectionSource source = CollectionSource.manyToAny( attributeMetadata.getMember() );
		return bindManyToAny( source, property );
	}

	private Collection bindInverseManyToMany(CollectionSource source, String mappedBy, Property property) {
		final ClassDetails targetClassDetails = resolveTargetClassDetails( source );
		final Collection collection = createCollection( source );
		collection.setRole( ownerBinding.getEntityName() + "." + attributeMetadata.getName() );
		collection.setInverse( true );
		collection.setMappedByProperty( mappedBy );
		collection.setMutable( true );
		collection.setOptimisticLocked( true );
		collection.setTypeUsingReflection( ownerType.getClassDetails().getClassName(), attributeMetadata.getName() );
		CollectionShapeBinder.apply( source, collection, bindingState );
		applyCascade( source, property, collection );

		bindingState.addInversePluralAssociationBinding( new InversePluralAssociationBinding(
				InversePluralAssociationBinding.Nature.MANY_TO_MANY,
				ownerType,
				ownerBinding,
				attributeMetadata,
				collection,
				targetClassDetails,
				mappedBy
		) );
		bindingState.addCollectionBinding( collection );
		return collection;
	}

	private Collection bindInverseOneToMany(CollectionSource source, String mappedBy, Property property) {
		final ClassDetails targetClassDetails = resolveTargetClassDetails( source );
		final Collection collection = createCollection( source );
		collection.setRole( ownerBinding.getEntityName() + "." + attributeMetadata.getName() );
		collection.setInverse( true );
		collection.setMappedByProperty( mappedBy );
		collection.setMutable( true );
		collection.setOptimisticLocked( true );
		collection.setTypeUsingReflection( ownerType.getClassDetails().getClassName(), attributeMetadata.getName() );
		CollectionShapeBinder.apply( source, collection, bindingState );
		applyCascade( source, property, collection );

		bindingState.addInversePluralAssociationBinding( new InversePluralAssociationBinding(
				InversePluralAssociationBinding.Nature.ONE_TO_MANY,
				ownerType,
				ownerBinding,
				attributeMetadata,
				collection,
				targetClassDetails,
				mappedBy
		) );
		bindingState.addCollectionBinding( collection );
		return collection;
	}

	private Collection bindAssociation(CollectionSource source, boolean uniqueTargetColumns, Property property) {
		final TargetEntityBinding target = resolveTargetEntityBinding( source );
		final Table table = modelBinders.getTableBinder()
				.bindAssociationTable(
						resolveOwnerEntityType(),
						ownerBinding.getTable(),
						attributeMetadata.getName(),
						target.entityType(),
						target.primaryTable(),
						source.joinTable()
				)
				.binding();

		final Collection collection = createCollection( source );
		collection.setRole( ownerBinding.getEntityName() + "." + attributeMetadata.getName() );
		collection.setCollectionTable( table );
		collection.setInverse( false );
		collection.setMutable( true );
		collection.setOptimisticLocked( true );
		collection.setTypeUsingReflection( ownerType.getClassDetails().getClassName(), attributeMetadata.getName() );
		CollectionShapeBinder.apply( source, collection, bindingState );
		applyCascade( source, property, collection );

		final ManyToOne element = bindElementValue( source, target, table, uniqueTargetColumns );
		collection.setElement( element );
		if ( collection instanceof org.hibernate.mapping.Map map ) {
			CollectionIndexBinder.bindMapKey(
					source,
					map,
					table,
					bindingOptions,
					bindingState,
					bindingContext
			);
		}
		else if ( collection instanceof IndexedCollection indexedCollection ) {
			CollectionIndexBinder.bindListIndex(
					source,
					indexedCollection,
					table,
					bindingOptions,
					bindingState,
					bindingContext
			);
		}
		else if ( collection instanceof IdentifierCollection identifierCollection ) {
			CollectionIdBinder.bindCollectionId(
					source,
					identifierCollection,
					table,
					bindingOptions,
					bindingState,
					bindingContext
			);
		}
		bindingState.addCollectionTableBinding( new CollectionTableBinding(
				collection,
				source.associationJoinColumns(),
				ForeignKeySource.from( source.joinTable() ),
				source.joinTable() == null ? new jakarta.persistence.UniqueConstraint[0] : source.joinTable().uniqueConstraints(),
				source.joinTable() == null ? new jakarta.persistence.Index[0] : source.joinTable().indexes()
		) );
		bindingState.addCollectionBinding( collection );
		return collection;
	}

	@SuppressWarnings("removal")
	private Collection bindManyToAny(CollectionSource source, Property property) {
		final JoinTable joinTable = source.joinTable();
		final Table table = joinTable == null
				? modelBinders.getTableBinder()
						.bindCollectionTable(
								resolveOwnerEntityType(),
								ownerBinding.getTable(),
								attributeMetadata.getName(),
								null
						)
						.binding()
				: modelBinders.getTableBinder()
						.bindAssociationTable(
								resolveOwnerEntityType(),
								ownerBinding.getTable(),
								attributeMetadata.getName(),
								resolveOwnerEntityType(),
								ownerBinding.getTable(),
								joinTable
						)
						.binding();

		final Collection collection = createCollection( source );
		collection.setRole( ownerBinding.getEntityName() + "." + attributeMetadata.getName() );
		collection.setCollectionTable( table );
		collection.setInverse( false );
		collection.setMutable( true );
		collection.setOptimisticLocked( true );
		collection.setTypeUsingReflection( ownerType.getClassDetails().getClassName(), attributeMetadata.getName() );
		CollectionShapeBinder.apply( source, collection, bindingState );

		final AnySource anySource = AnySource.createManyToAny( source, bindingContext, bindingState );
		final org.hibernate.mapping.Any element = new AnyValueBinder(
				bindingOptions,
				bindingState,
				bindingContext
		).bind( anySource, attributeMetadata.getName(), table );
		collection.setElement( element );
		property.setCascade( anySource.cascades() );
		if ( collection instanceof org.hibernate.mapping.Map map ) {
			CollectionIndexBinder.bindMapKey(
					source,
					map,
					table,
					bindingOptions,
					bindingState,
					bindingContext
			);
		}
		else if ( collection instanceof IndexedCollection indexedCollection ) {
			CollectionIndexBinder.bindListIndex(
					source,
					indexedCollection,
					table,
					bindingOptions,
					bindingState,
					bindingContext
			);
		}
		else if ( collection instanceof IdentifierCollection identifierCollection ) {
			CollectionIdBinder.bindCollectionId(
					source,
					identifierCollection,
					table,
					bindingOptions,
					bindingState,
					bindingContext
			);
		}
		bindingState.addCollectionTableBinding( new CollectionTableBinding(
				collection,
				source.associationJoinColumns(),
				ForeignKeySource.from( joinTable ),
				joinTable == null ? new jakarta.persistence.UniqueConstraint[0] : joinTable.uniqueConstraints(),
				joinTable == null ? new jakarta.persistence.Index[0] : joinTable.indexes()
		) );
		bindingState.addCollectionBinding( collection );
		return collection;
	}

	private void applyCascade(CollectionSource source, Property property, Collection collection) {
		final var cascades = source.cascades( bindingState );
		final boolean orphanRemoval = source.orphanRemoval();
		property.setCascade( cascades, orphanRemoval );
		if ( orphanRemoval ) {
			collection.setOrphanDelete( true );
		}
	}

	private Collection createCollection(CollectionSource source) {
		return switch ( source.classification() ) {
			case SET, ORDERED_SET, SORTED_SET -> new org.hibernate.mapping.Set( bindingState.getMetadataBuildingContext(), ownerBinding );
			case LIST -> new org.hibernate.mapping.List( bindingState.getMetadataBuildingContext(), ownerBinding );
			case BAG -> new org.hibernate.mapping.Bag( bindingState.getMetadataBuildingContext(), ownerBinding );
			case ID_BAG -> new IdentifierBag( bindingState.getMetadataBuildingContext(), ownerBinding );
			case MAP, ORDERED_MAP, SORTED_MAP -> new org.hibernate.mapping.Map( bindingState.getMetadataBuildingContext(), ownerBinding );
			case ARRAY -> {
				final org.hibernate.mapping.Array array = new org.hibernate.mapping.Array( bindingState.getMetadataBuildingContext(), ownerBinding );
				array.setElementClassName( source.elementType().determineRawClass().getClassName() );
				yield array;
			}
		};
	}

	private ManyToOne bindElementValue(
			CollectionSource source,
			TargetEntityBinding target,
			Table table,
			boolean uniqueByDefault) {
		final ManyToOne element = new ManyToOne( bindingState.getMetadataBuildingContext(), table );
		final List<JoinColumn> inverseJoinColumns = source.associationInverseJoinColumns();
		final boolean referenceToPrimaryKey = ToOneAttributeBinder.referencesPrimaryKey(
				inverseJoinColumns,
				target.identifierColumns()
		);
		element.setReferencedEntityName( target.entityName() );
		element.setReferenceToPrimaryKey( referenceToPrimaryKey );
		element.setTypeName( target.entityName() );
		element.setTypeUsingReflection( ownerType.getClassDetails().getClassName(), attributeMetadata.getName() );

		bindJoinColumns(
				inverseJoinColumns,
				element,
				target,
				referenceToPrimaryKey,
				table,
				uniqueByDefault,
				attributeMetadata.getName()
		);
		if ( !referenceToPrimaryKey ) {
			bindingState.addAssociationTargetBinding( new AssociationTargetBinding(
					ownerBinding,
					element,
					target.typeBinder(),
					ToOneAttributeBinder.referencedColumnNames( inverseJoinColumns ),
					ownerType.getClassDetails().getClassName() + "." + attributeMetadata.getName()
			) );
		}
		bindingState.addForeignKeyBinding( new ForeignKeyBinding(
				ownerBinding,
				element,
				ForeignKeySource.inverseFrom( source.joinTable() )
		) );
		return element;
	}

	private void bindJoinColumns(
			List<JoinColumn> joinColumnAnns,
			ManyToOne value,
			TargetEntityBinding target,
			boolean referenceToPrimaryKey,
			Table table,
			boolean uniqueByDefault,
			String propertyName) {
		final List<org.hibernate.mapping.Column> targetColumns = target.identifierColumns();

		if ( referenceToPrimaryKey && !joinColumnAnns.isEmpty() && joinColumnAnns.size() != targetColumns.size() ) {
			throw new MappingException(
					"Plural association inverse join column count did not match target identifier column count - "
							+ ownerType.getClassDetails().getClassName() + "." + propertyName
			);
		}

		final List<JoinColumn> orderedJoinColumns = referenceToPrimaryKey
				? ToOneAttributeBinder.orderJoinColumns(
						joinColumnAnns,
						targetColumns,
						ownerType.getClassDetails().getClassName(),
						propertyName
				)
				: joinColumnAnns;
		final int columnCount = referenceToPrimaryKey ? targetColumns.size() : joinColumnAnns.size();
		for ( int i = 0; i < columnCount; i++ ) {
			final JoinColumn joinColumnAnn = orderedJoinColumns.isEmpty() ? null : orderedJoinColumns.get( i );
			final String targetColumnName = referenceToPrimaryKey
					? targetColumns.get( i ).getName()
					: joinColumnAnn.referencedColumnName();
			final org.hibernate.mapping.Column column = ColumnBinder.bindColumn(
					org.hibernate.boot.models.bind.internal.sources.ColumnSource.from( joinColumnAnn ),
					() -> propertyName + "_" + targetColumnName,
					uniqueByDefault,
					false
			);
			if ( uniqueByDefault ) {
				column.setUnique( true );
			}
			table.addColumn( column );
			value.addColumn( column );
		}
	}

	private TargetEntityBinding resolveTargetEntityBinding(CollectionSource source) {
		final ClassDetails targetClassDetails = resolveTargetClassDetails( source );
		final EntityTypeBinder targetTypeBinder = (EntityTypeBinder) bindingState.getTypeBinder(
				targetClassDetails
		);
		if ( targetTypeBinder == null ) {
			throw new MappingException(
					"Could not resolve local type binding for plural association target entity - "
							+ targetClassDetails.getClassName()
			);
		}

		final IdentifierBinding identifierBinding = bindingState.getIdentifierBinding(
				targetTypeBinder.getManagedType().getHierarchy().getRoot()
		);
		if ( identifierBinding == null ) {
			throw new MappingException(
					"Could not resolve identifier binding for plural association target entity - "
							+ targetTypeBinder.getTypeBinding().getEntityName()
			);
		}

		return new TargetEntityBinding(
				targetTypeBinder.getTypeBinding().getEntityName(),
				targetTypeBinder,
				targetTypeBinder.getManagedType(),
				targetTypeBinder.getTable(),
				identifierBinding.columns()
		);
	}

	private ClassDetails resolveTargetClassDetails(CollectionSource source) {
		final ManyToMany manyToMany = source.manyToMany();
		if ( manyToMany != null && manyToMany.targetEntity() != void.class ) {
			return bindingContext.getClassDetailsRegistry().resolveClassDetails( manyToMany.targetEntity().getName() );
		}
		final OneToMany oneToMany = source.oneToMany();
		if ( oneToMany != null && oneToMany.targetEntity() != void.class ) {
			return bindingContext.getClassDetailsRegistry().resolveClassDetails( oneToMany.targetEntity().getName() );
		}
		return source.elementType().determineRawClass();
	}

	private EntityTypeMetadata resolveOwnerEntityType() {
		if ( ownerType instanceof EntityTypeMetadata entityType ) {
			return entityType;
		}
		return ownerType.getHierarchy().getRoot();
	}

	private record TargetEntityBinding(
			String entityName,
			EntityTypeBinder typeBinder,
			EntityTypeMetadata entityType,
			Table primaryTable,
			List<org.hibernate.mapping.Column> identifierColumns) {
	}
}
