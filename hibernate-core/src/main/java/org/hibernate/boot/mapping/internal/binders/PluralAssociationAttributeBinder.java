/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.mapping.internal.materialize.ResolvedForeignKey;
import org.hibernate.boot.mapping.internal.sources.AnySource;
import org.hibernate.boot.mapping.internal.sources.CollectionSource;
import org.hibernate.boot.mapping.internal.sources.ColumnSource;
import org.hibernate.boot.mapping.internal.sources.ForeignKeySource;
import org.hibernate.boot.mapping.internal.sources.ToOneSource.JoinColumnOrFormulaSource;
import org.hibernate.boot.mapping.internal.model.CollectionValueIntent;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.categorize.AttributeMetadata;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SortableValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.ExcludedFromVersioning;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;
import jakarta.persistence.UniqueConstraint;

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
	private final String collectionRolePath;
	private final AssociationOverride associationOverride;
	private final CollectionValueIntent collectionValueIntent;
	private final boolean registerCollectionBindings;

	PluralAssociationAttributeBinder(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			AttributeMetadata attributeMetadata,
			ModelBinders modelBinders,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		this(
				ownerType,
				ownerBinding,
				attributeMetadata,
				modelBinders,
				bindingOptions,
				bindingState,
				bindingContext,
				attributeMetadata.getName(),
				null,
				null,
				true
		);
	}

	PluralAssociationAttributeBinder(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			AttributeMetadata attributeMetadata,
			ModelBinders modelBinders,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext,
			String collectionRolePath) {
		this(
				ownerType,
				ownerBinding,
				attributeMetadata,
				modelBinders,
				bindingOptions,
				bindingState,
				bindingContext,
				collectionRolePath,
				null,
				null,
				true
		);
	}

	PluralAssociationAttributeBinder(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			AttributeMetadata attributeMetadata,
			ModelBinders modelBinders,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext,
			String collectionRolePath,
			AssociationOverride associationOverride) {
		this(
				ownerType,
				ownerBinding,
				attributeMetadata,
				modelBinders,
				bindingOptions,
				bindingState,
				bindingContext,
				collectionRolePath,
				associationOverride,
				null,
				true
		);
	}

	PluralAssociationAttributeBinder(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			AttributeMetadata attributeMetadata,
			ModelBinders modelBinders,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext,
			String collectionRolePath,
			AssociationOverride associationOverride,
			boolean registerCollectionBindings) {
		this(
				ownerType,
				ownerBinding,
				attributeMetadata,
				modelBinders,
				bindingOptions,
				bindingState,
				bindingContext,
				collectionRolePath,
				associationOverride,
				null,
				registerCollectionBindings
		);
	}

	PluralAssociationAttributeBinder(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			AttributeMetadata attributeMetadata,
			ModelBinders modelBinders,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext,
			String collectionRolePath,
			AssociationOverride associationOverride,
			CollectionValueIntent collectionValueIntent,
			boolean registerCollectionBindings) {
		this.ownerType = ownerType;
		this.ownerBinding = ownerBinding;
		this.attributeMetadata = attributeMetadata;
		this.modelBinders = modelBinders;
		this.bindingOptions = bindingOptions;
		this.bindingState = bindingState;
		this.bindingContext = bindingContext;
		this.collectionRolePath = collectionRolePath;
		this.associationOverride = associationOverride;
		this.collectionValueIntent = collectionValueIntent;
		this.registerCollectionBindings = registerCollectionBindings;
	}

	Collection bindManyToMany(Property property) {
		final CollectionSource source = collectionValueIntent == null
				? CollectionSource.manyToMany(
						attributeMetadata.getMember(),
						ownerType.getClassDetails(),
						ownerType.getHierarchy().getRoot().getClassDetails(),
						associationOverride,
						bindingContext.getBootstrapContext().getModelsContext()
				)
				: collectionValueIntent.source();
		final ManyToMany manyToMany = source.manyToMany();
		if ( manyToMany != null && StringHelper.isNotEmpty( manyToMany.mappedBy() ) ) {
			return bindInverseManyToMany( source, manyToMany.mappedBy(), property );
		}
		return bindAssociation( source, false, property );
	}

	Collection bindOneToMany(Property property) {
		final CollectionSource source = collectionValueIntent == null
				? CollectionSource.oneToMany(
						attributeMetadata.getMember(),
						ownerType.getClassDetails(),
						ownerType.getHierarchy().getRoot().getClassDetails(),
						associationOverride,
						bindingContext.getBootstrapContext().getModelsContext()
				)
				: collectionValueIntent.source();
		final OneToMany oneToMany = source.oneToMany();
		if ( oneToMany != null && StringHelper.isNotEmpty( oneToMany.mappedBy() ) ) {
			return bindInverseOneToMany( source, oneToMany.mappedBy(), property );
		}
		validateOnDeleteJoinColumn();
		if ( source.joinTable() == null && !source.oneToManyJoinColumnsOrFormulas().isEmpty() ) {
			return bindOneToManyWithForeignKey( source, property );
		}
		return bindAssociation( source, true, property );
	}

	Collection bindManyToAny(Property property) {
		final CollectionSource source = collectionValueIntent == null
				? CollectionSource.manyToAny(
						attributeMetadata.getMember(),
						bindingContext.getBootstrapContext().getModelsContext()
				)
				: collectionValueIntent.source();
		return bindManyToAny( source, property );
	}

	private Collection bindInverseManyToMany(CollectionSource source, String mappedBy, Property property) {
		final ClassDetails targetClassDetails = resolveTargetClassDetails( source );
		final Collection collection = createCollection( source );
		collection.setRole( ownerBinding.getEntityName() + "." + collectionRolePath );
		collection.setInverse( true );
		collection.setMappedByProperty( mappedBy );
		collection.setMutable( source.isMutable() );
		bindOptimisticLock( collection, property, true );
		collection.setTypeUsingReflection(
				attributeMetadata.getMember().getDeclaringType().getName(),
				attributeMetadata.getName()
		);
		CollectionShapeBinder.apply( source, collection, bindingState );
		applyCascade( source, property, collection );

		if ( !registerCollectionBindings ) {
			collection.setElement( createDeclarationOnlyManyToManyElement( targetClassDetails ) );
		}
		if ( registerCollectionBindings ) {
			bindingState.addInversePluralAssociationBinding( new InversePluralAssociationBinding(
					InversePluralAssociationBinding.Nature.MANY_TO_MANY,
					ownerType,
					ownerBinding,
					attributeMetadata,
					source,
					collection,
					targetClassDetails,
					mappedBy
			) );
			bindingState.addCollectionBinding( collection );
		}
		return collection;
	}

	private Collection bindInverseOneToMany(CollectionSource source, String mappedBy, Property property) {
		final ClassDetails targetClassDetails = resolveTargetClassDetails( source );
		final Collection collection = createCollection( source );
		collection.setRole( ownerBinding.getEntityName() + "." + collectionRolePath );
		collection.setInverse( true );
		collection.setMappedByProperty( mappedBy );
		collection.setMutable( source.isMutable() );
		bindOptimisticLock( collection, property, true );
		collection.setTypeUsingReflection(
				attributeMetadata.getMember().getDeclaringType().getName(),
				attributeMetadata.getName()
		);
		CollectionShapeBinder.apply( source, collection, bindingState );
		applyCascade( source, property, collection );

		if ( !registerCollectionBindings ) {
			collection.setElement( createDeclarationOnlyOneToManyElement( targetClassDetails ) );
		}
		if ( registerCollectionBindings ) {
			bindingState.addInversePluralAssociationBinding( new InversePluralAssociationBinding(
					InversePluralAssociationBinding.Nature.ONE_TO_MANY,
					ownerType,
					ownerBinding,
					attributeMetadata,
					source,
					collection,
					targetClassDetails,
					mappedBy
			) );
			bindingState.addCollectionBinding( collection );
		}
		return collection;
	}

	private Collection bindAssociation(CollectionSource source, boolean uniqueTargetColumns, Property property) {
		final TargetEntityBinding target = resolveTargetEntityBinding( source );
		final Table table = registerCollectionBindings
				? bindAssociationTable( source, target )
				: createDeclarationOnlyTable();

		final Collection collection = createCollection( source );
		collection.setRole( ownerBinding.getEntityName() + "." + collectionRolePath );
		collection.setCollectionTable( table );
		collection.setInverse( false );
		collection.setMutable( source.isMutable() );
		bindOptimisticLock( collection, property, false );
		collection.setTypeUsingReflection(
				attributeMetadata.getMember().getDeclaringType().getName(),
				attributeMetadata.getName()
		);
		CollectionShapeBinder.apply( source, collection, bindingState );
		applyCascade( source, property, collection );

		final ManyToOne element = bindElementValue( source, target, table, uniqueTargetColumns );
		collection.setElement( element );
		if ( collection instanceof org.hibernate.mapping.Map map ) {
			CollectionIndexBinder.bindMapKey(
					ownerType,
					ownerBinding,
					source,
					map,
					table,
					modelBinders,
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
		if ( registerCollectionBindings ) {
			bindingState.addCollectionTableBinding( new CollectionTableBinding(
					collection,
					source.associationJoinColumns(),
					source.associationJoinColumns().stream().map( JoinColumnOrFormulaSource::column ).toList(),
					source.associationInverseJoinColumns(),
					ForeignKeySource.firstSpecified(
							associationTableForeignKey( source ),
							ForeignKeySource.fromFirstSpecifiedJoinColumn( source.associationJoinColumns() )
					),
					resolveOnDeleteAction(),
					associationTableUniqueConstraints( source ),
					associationTableIndexes( source ),
					uniqueTargetColumns
			) );
			bindingState.addCollectionBinding( collection );
		}
		return collection;
	}

	private Collection bindOneToManyWithForeignKey(CollectionSource source, Property property) {
		final TargetEntityBinding target = resolveTargetEntityBinding( source );
		final Table table = registerCollectionBindings
				? target.primaryTable()
				: createDeclarationOnlyTable();

		final Collection collection = createCollection( source );
		collection.setRole( ownerBinding.getEntityName() + "." + collectionRolePath );
		collection.setCollectionTable( table );
		collection.setInverse( false );
		collection.setMutable( source.isMutable() );
		bindOptimisticLock( collection, property, false );
		collection.setTypeUsingReflection(
				attributeMetadata.getMember().getDeclaringType().getName(),
				attributeMetadata.getName()
		);
		CollectionShapeBinder.apply( source, collection, bindingState );
		applyCascade( source, property, collection );

		final org.hibernate.mapping.OneToMany element = new org.hibernate.mapping.OneToMany(
				bindingState.getMetadataBuildingContext(),
				ownerBinding
		);
		element.setAssociatedClass( target.typeBinder().getTypeBinding() );
		element.setReferencedEntityName( target.entityName() );
		element.setTypeUsingReflection(
				attributeMetadata.getMember().getDeclaringType().getName(),
				attributeMetadata.getName()
		);
		collection.setElement( element );
		StateManagementBindingPhase.registerOneToManyCollection(
				source,
				collection,
				element.getReferencedEntityName(),
				bindingState
		);

		if ( collection instanceof org.hibernate.mapping.Map map ) {
			CollectionIndexBinder.bindMapKey(
					ownerType,
					ownerBinding,
					source,
					map,
					table,
					modelBinders,
					bindingOptions,
					bindingState,
					bindingContext,
					true
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
		if ( registerCollectionBindings ) {
			bindingState.addCollectionTableBinding( new CollectionTableBinding(
					collection,
					source.oneToManyJoinColumns(),
					source.oneToManyJoinColumnsOrFormulas(),
					List.of(),
					ForeignKeySource.firstSpecified(
							source.oneToManyForeignKeySource(),
							ForeignKeySource.from( source.joinTable() )
						),
						resolveOnDeleteAction(),
						new jakarta.persistence.UniqueConstraint[0],
						new jakarta.persistence.Index[0],
						false
				) );
				bindingState.addCollectionBinding( collection );
			}
		return collection;
	}

	@SuppressWarnings("removal")
	private Collection bindManyToAny(CollectionSource source, Property property) {
		final JoinTable joinTable = source.joinTable();
		final Table table = registerCollectionBindings
				? bindManyToAnyTable( source, joinTable )
				: createDeclarationOnlyTable();

		final Collection collection = createCollection( source );
		collection.setRole( ownerBinding.getEntityName() + "." + collectionRolePath );
		collection.setCollectionTable( table );
		collection.setInverse( false );
		collection.setMutable( source.isMutable() );
		bindOptimisticLock( collection, property, false );
		collection.setTypeUsingReflection(
				attributeMetadata.getMember().getDeclaringType().getName(),
				attributeMetadata.getName()
		);
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
					ownerType,
					ownerBinding,
					source,
					map,
					table,
					modelBinders,
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
		if ( registerCollectionBindings ) {
			bindingState.addCollectionTableBinding( new CollectionTableBinding(
					collection,
					source.associationJoinColumns(),
					ForeignKeySource.firstSpecified(
							ForeignKeySource.fromFirstSpecifiedJoinColumn( source.associationJoinColumns() ),
							associationTableForeignKey( source )
					),
					resolveOnDeleteAction(),
					associationTableUniqueConstraints( source ),
					associationTableIndexes( source )
			) );
			bindingState.addCollectionBinding( collection );
		}
		return collection;
	}

	private Table bindAssociationTable(CollectionSource source, TargetEntityBinding target) {
		final JoinTable joinTable = source.joinTable();
		if ( joinTable != null ) {
			return modelBinders.getTableBinder()
					.bindAssociationTable(
							resolveOwnerEntityType(),
							ownerBinding.getTable(),
							attributeMetadata.getName(),
							target.entityType(),
							target.primaryTable(),
							joinTable
					)
					.binding();
		}
		final CollectionTable collectionTable = source.collectionTable();
		return modelBinders.getTableBinder()
				.bindAssociationTable(
						resolveOwnerEntityType(),
						ownerBinding.getTable(),
						attributeMetadata.getName(),
						target.entityType(),
						target.primaryTable(),
						collectionTable
				)
				.binding();
	}

	private Table bindManyToAnyTable(CollectionSource source, JoinTable joinTable) {
		if ( joinTable != null ) {
			return modelBinders.getTableBinder()
					.bindAssociationTable(
							resolveOwnerEntityType(),
							ownerBinding.getTable(),
							attributeMetadata.getName(),
							resolveOwnerEntityType(),
							ownerBinding.getTable(),
							joinTable
					)
					.binding();
		}
		final CollectionTable collectionTable = source.collectionTable();
		if ( collectionTable != null ) {
			return modelBinders.getTableBinder()
					.bindAssociationTable(
							resolveOwnerEntityType(),
							ownerBinding.getTable(),
							attributeMetadata.getName(),
							resolveOwnerEntityType(),
							ownerBinding.getTable(),
							collectionTable
					)
					.binding();
		}
		return modelBinders.getTableBinder()
				.bindCollectionTable(
						resolveOwnerEntityType(),
						ownerBinding.getTable(),
						attributeMetadata.getName(),
						null
				)
				.binding();
	}

	private ForeignKeySource associationTableForeignKey(CollectionSource source) {
		final JoinTable joinTable = source.joinTable();
		if ( joinTable != null ) {
			return ForeignKeySource.from( joinTable );
		}
		return ForeignKeySource.from( source.collectionTable() );
	}

	private UniqueConstraint[] associationTableUniqueConstraints(CollectionSource source) {
		final JoinTable joinTable = source.joinTable();
		if ( joinTable != null ) {
			return joinTable.uniqueConstraints();
		}
		final CollectionTable collectionTable = source.collectionTable();
		return collectionTable == null ? new UniqueConstraint[0] : collectionTable.uniqueConstraints();
	}

	private Index[] associationTableIndexes(CollectionSource source) {
		final JoinTable joinTable = source.joinTable();
		if ( joinTable != null ) {
			return joinTable.indexes();
		}
		final CollectionTable collectionTable = source.collectionTable();
		return collectionTable == null ? new Index[0] : collectionTable.indexes();
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
		return CollectionMappingHelper.createCollection( source, ownerBinding, bindingState );
	}

	private ManyToOne bindElementValue(
			CollectionSource source,
			TargetEntityBinding target,
			Table table,
			boolean uniqueByDefault) {
		final ManyToOne element = new ManyToOne( bindingState.getMetadataBuildingContext(), table );
		final List<JoinColumn> inverseJoinColumns = source.associationInverseJoinColumns();
		final boolean referenceToPrimaryKey = referencesPrimaryKey( inverseJoinColumns, target );
		element.setReferencedEntityName( target.entityName() );
		element.setReferenceToPrimaryKey( referenceToPrimaryKey );
		if ( !referenceToPrimaryKey ) {
			element.disableForeignKey();
			final String referencedPropertyName = resolveReferencedPropertyName( target, inverseJoinColumns );
			if ( referencedPropertyName != null ) {
				element.setReferencedPropertyName( referencedPropertyName );
			}
		}
		element.setTypeName( target.entityName() );
		element.setTypeUsingReflection(
				attributeMetadata.getMember().getDeclaringType().getName(),
				attributeMetadata.getName()
		);
		applyOnDelete( element );

		bindJoinColumns(
				inverseJoinColumns,
				element,
				target,
				referenceToPrimaryKey,
				table,
				uniqueByDefault,
				attributeMetadata.getName()
		);
		if ( registerCollectionBindings && !referenceToPrimaryKey ) {
			bindingState.addAssociationTargetBinding( new AssociationTargetBinding(
					ownerBinding,
					element,
					target.typeBinder(),
					ToOneAttributeBinder.referencedColumnNames( inverseJoinColumns ),
					ownerType.getClassDetails().getClassName() + "." + attributeMetadata.getName()
			) );
		}
		if ( registerCollectionBindings ) {
			final List<org.hibernate.mapping.Column> targetPrimaryKeyColumns = referencedPrimaryKeyColumns(
					inverseJoinColumns,
					target,
					referenceToPrimaryKey
			);
			bindingState.addForeignKeyBinding( new ForeignKeyBinding(
					ownerBinding,
					element,
					ForeignKeySource.firstSpecified(
							ForeignKeySource.inverseFrom( source.joinTable() ),
							ForeignKeySource.fromFirstSpecifiedJoinColumn( inverseJoinColumns )
					),
					referenceToPrimaryKey && element.isConstrained()
							? ResolvedForeignKey.from(
									element,
									element.getReferencedEntityName(),
									SelectableOrderResolver.resolveByTargetOrder(
											element.getColumns(),
											targetPrimaryKeyColumns,
											ownerType.getClassDetails().getClassName()
													+ "." + attributeMetadata.getName()
									)
							)
							: null,
					referenceToPrimaryKey
							? List.of()
							: ToOneAttributeBinder.referencedColumnNames( inverseJoinColumns )
			) );
		}
		return element;
	}

	private String resolveReferencedPropertyName(TargetEntityBinding target, List<JoinColumn> joinColumns) {
		final List<Identifier> referencedColumnNames = new ArrayList<>( joinColumns.size() );
		for ( JoinColumn joinColumn : joinColumns ) {
			referencedColumnNames.add( bindingState.getDatabase().toIdentifier( joinColumn.referencedColumnName() ) );
		}
		for ( Property property : referenceableProperties( target.typeBinder().getTypeBinding() ) ) {
			if ( property.getValue() instanceof SimpleValue simpleValue
					&& !( simpleValue instanceof ToOne )
					&& columnNamesMatch( simpleValue.getColumns(), referencedColumnNames ) ) {
				return property.getName();
			}
		}
		return null;
	}

	private List<Property> referenceableProperties(PersistentClass targetBinding) {
		final ArrayList<Property> properties = new ArrayList<>();
		if ( targetBinding.getIdentifierProperty() != null ) {
			properties.add( targetBinding.getIdentifierProperty() );
		}
		properties.addAll( targetBinding.getReferenceableProperties() );
		return properties;
	}

	private boolean columnNamesMatch(
			List<org.hibernate.mapping.Column> columns,
			List<Identifier> referencedColumnNames) {
		if ( columns.size() != referencedColumnNames.size() ) {
			return false;
		}
		for ( int i = 0; i < columns.size(); i++ ) {
			if ( !columns.get( i ).getNameIdentifier( bindingState.getDatabase() ).matches( referencedColumnNames.get( i ) ) ) {
				return false;
			}
		}
		return true;
	}

	private Table createDeclarationOnlyTable() {
		return new Table( "orm", ownerBinding.getEntityName() + "." + collectionRolePath + "#mapped-superclass" );
	}

	private ManyToOne createDeclarationOnlyManyToManyElement(ClassDetails targetClassDetails) {
		final EntityTypeBinder targetTypeBinder = resolveTargetTypeBinder( targetClassDetails );
		final ManyToOne element = new ManyToOne( bindingState.getMetadataBuildingContext(), createDeclarationOnlyTable() );
		element.setReferencedEntityName( targetTypeBinder.getTypeBinding().getEntityName() );
		element.setReferenceToPrimaryKey( true );
		element.setTypeName( targetTypeBinder.getTypeBinding().getEntityName() );
		element.setTypeUsingReflection(
				attributeMetadata.getMember().getDeclaringType().getName(),
				attributeMetadata.getName()
		);
		return element;
	}

	private org.hibernate.mapping.OneToMany createDeclarationOnlyOneToManyElement(ClassDetails targetClassDetails) {
		final EntityTypeBinder targetTypeBinder = resolveTargetTypeBinder( targetClassDetails );
		final org.hibernate.mapping.OneToMany element = new org.hibernate.mapping.OneToMany(
				bindingState.getMetadataBuildingContext(),
				ownerBinding
		);
		element.setAssociatedClass( targetTypeBinder.getTypeBinding() );
		element.setReferencedEntityName( targetTypeBinder.getTypeBinding().getEntityName() );
		element.setTypeUsingReflection(
				attributeMetadata.getMember().getDeclaringType().getName(),
				attributeMetadata.getName()
		);
		return element;
	}

	private org.hibernate.annotations.OnDeleteAction resolveOnDeleteAction() {
		final OnDelete onDelete = attributeMetadata.getMember().getDirectAnnotationUsage( OnDelete.class );
		return onDelete == null ? null : onDelete.action();
	}

	private void validateOnDeleteJoinColumn() {
		if ( attributeMetadata.getMember().hasDirectAnnotationUsage( OnDelete.class ) && !hasExplicitJoinColumn() ) {
			throw new AnnotationException(
					"Unidirectional '@OneToMany' association '" + ownerBinding.getEntityName() + "."
							+ attributeMetadata.getName()
							+ "' is annotated '@OnDelete' and must explicitly specify a '@JoinColumn'"
			);
		}
	}

	private boolean hasExplicitJoinColumn() {
		if ( attributeMetadata.getMember().hasDirectAnnotationUsage( JoinColumn.class )
				|| attributeMetadata.getMember().hasDirectAnnotationUsage( JoinColumns.class ) ) {
			return true;
		}
		final JoinTable joinTable = attributeMetadata.getMember().getDirectAnnotationUsage( JoinTable.class );
		return joinTable != null && joinTable.joinColumns().length > 0;
	}

	private void applyOnDelete(ManyToOne value) {
		final OnDelete onDelete = attributeMetadata.getMember().getDirectAnnotationUsage( OnDelete.class );
		if ( onDelete != null ) {
			value.setOnDeleteAction( onDelete.action() );
		}
	}

	private void bindJoinColumns(
			List<JoinColumn> joinColumnAnns,
			ManyToOne value,
			TargetEntityBinding target,
			boolean referenceToPrimaryKey,
			Table table,
			boolean uniqueByDefault,
			String propertyName) {
		final List<org.hibernate.mapping.Column> targetColumns = referencedPrimaryKeyColumns(
				joinColumnAnns,
				target,
				referenceToPrimaryKey
		);

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
						bindingState.getDatabase(),
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
			final Supplier<String> implicitName =
					() -> implicitPluralAssociationElementJoinColumnName( target, propertyName, targetColumnName );
			final ColumnSource columnSource = ColumnSource.from( joinColumnAnn );
			final org.hibernate.mapping.Column column = ColumnBinder.bindColumn(
					columnSource,
					implicitName,
					uniqueByDefault,
					false,
					bindingOptions,
					bindingState
			);
			if ( uniqueByDefault ) {
				column.setUnique( true );
			}
			table.addColumn( column );
			ColumnBinder.registerColumnNameBinding(
					table,
					ColumnBinder.columnName( columnSource, implicitName ),
					column,
					bindingOptions,
					bindingState
			);
			value.addColumn( column );
		}
	}

	private String implicitPluralAssociationElementJoinColumnName(
			TargetEntityBinding target,
			String propertyName,
			String targetColumnName) {
		return bindingState.getMetadataBuildingContext()
				.getBuildingOptions()
				.getImplicitNamingStrategy()
				.determineJoinColumnName( new ImplicitJoinColumnNameSource() {
					@Override
					public Nature getNature() {
						return Nature.ENTITY_COLLECTION;
					}

					@Override
					public org.hibernate.boot.model.naming.EntityNaming getEntityNaming() {
						return target.entityType();
					}

					@Override
					public AttributePath getAttributePath() {
						return AttributePath.parse( propertyName );
					}

					@Override
					public Identifier getReferencedTableName() {
						return target.primaryTable().getNameIdentifier();
					}

					@Override
					public Identifier getReferencedColumnName() {
						return bindingState.getDatabase().toIdentifier( targetColumnName );
					}

					@Override
					public org.hibernate.boot.spi.MetadataBuildingContext getBuildingContext() {
						return bindingState.getMetadataBuildingContext();
					}
				} )
				.getText();
	}

	private boolean referencesPrimaryKey(List<JoinColumn> joinColumns, TargetEntityBinding target) {
		return ToOneAttributeBinder.referencesPrimaryKey( joinColumns, target.identifierColumns(), bindingState.getDatabase() )
			|| referencesPrimaryKeyJoinColumns( joinColumns, targetPrimaryKeyJoinColumns( target ) );
	}

	private List<org.hibernate.mapping.Column> referencedPrimaryKeyColumns(
			List<JoinColumn> joinColumns,
			TargetEntityBinding target,
			boolean referenceToPrimaryKey) {
		if ( !referenceToPrimaryKey
				|| ToOneAttributeBinder.referencesPrimaryKey( joinColumns, target.identifierColumns(), bindingState.getDatabase() ) ) {
			return target.identifierColumns();
		}
		final PrimaryKeyJoinColumn[] primaryKeyJoinColumns = targetPrimaryKeyJoinColumns( target );
		if ( referencesPrimaryKeyJoinColumns( joinColumns, primaryKeyJoinColumns ) ) {
			return primaryKeyJoinColumns( primaryKeyJoinColumns );
		}
		return target.identifierColumns();
	}

	private PrimaryKeyJoinColumn[] targetPrimaryKeyJoinColumns(TargetEntityBinding target) {
		final ClassDetails classDetails = target.typeBinder().getManagedType().getClassDetails();
		final PrimaryKeyJoinColumns primaryKeyJoinColumns = classDetails.getDirectAnnotationUsage(
				PrimaryKeyJoinColumns.class
		);
		if ( primaryKeyJoinColumns != null ) {
			return primaryKeyJoinColumns.value();
		}
		return classDetails.getRepeatedAnnotationUsages(
				PrimaryKeyJoinColumn.class,
				target.typeBinder().getBindingContext().getBootstrapContext().getModelsContext()
		);
	}

	private boolean referencesPrimaryKeyJoinColumns(
			List<JoinColumn> joinColumns,
			PrimaryKeyJoinColumn[] primaryKeyJoinColumns) {
		if ( joinColumns.size() != primaryKeyJoinColumns.length ) {
			return false;
		}
		final ArrayList<PrimaryKeyJoinColumn> unmatchedPrimaryKeyJoinColumns = new ArrayList<>(
				List.of( primaryKeyJoinColumns )
		);
		for ( JoinColumn joinColumn : joinColumns ) {
			final PrimaryKeyJoinColumn primaryKeyJoinColumn = findPrimaryKeyJoinColumn(
					unmatchedPrimaryKeyJoinColumns,
					joinColumn.referencedColumnName()
			);
			if ( primaryKeyJoinColumn == null ) {
				return false;
			}
			unmatchedPrimaryKeyJoinColumns.remove( primaryKeyJoinColumn );
		}
		return unmatchedPrimaryKeyJoinColumns.isEmpty();
	}

	private PrimaryKeyJoinColumn findPrimaryKeyJoinColumn(
			List<PrimaryKeyJoinColumn> primaryKeyJoinColumns,
			String columnName) {
		final Identifier columnIdentifier = bindingState.getDatabase().toIdentifier( columnName );
		for ( PrimaryKeyJoinColumn primaryKeyJoinColumn : primaryKeyJoinColumns ) {
			if ( bindingState.getDatabase().toIdentifier( primaryKeyJoinColumn.name() ).matches( columnIdentifier ) ) {
				return primaryKeyJoinColumn;
			}
		}
		return null;
	}

	private List<org.hibernate.mapping.Column> primaryKeyJoinColumns(PrimaryKeyJoinColumn[] primaryKeyJoinColumns) {
		final ArrayList<org.hibernate.mapping.Column> result = new ArrayList<>( primaryKeyJoinColumns.length );
		for ( PrimaryKeyJoinColumn primaryKeyJoinColumn : primaryKeyJoinColumns ) {
			result.add( new org.hibernate.mapping.Column( primaryKeyJoinColumn.name() ) );
		}
		return result;
	}

	private void bindOptimisticLock(Collection collection, Property property, boolean mappedBy) {
		final OptimisticLock optimisticLock = attributeMetadata.getMember().getDirectAnnotationUsage( OptimisticLock.class );
		final ExcludedFromVersioning excludedFromVersioning =
				attributeMetadata.getMember().getDirectAnnotationUsage( ExcludedFromVersioning.class );
		final boolean optimisticLocked = optimisticLock != null
				? !optimisticLock.excluded()
				: excludedFromVersioning == null && !mappedBy;
		collection.setOptimisticLocked( optimisticLocked );
		property.setOptimisticLocked( optimisticLocked );
	}

	private TargetEntityBinding resolveTargetEntityBinding(CollectionSource source) {
		final ClassDetails targetClassDetails = resolveTargetClassDetails( source );
		final EntityTypeBinder targetTypeBinder = resolveTargetTypeBinder( targetClassDetails, source );

		final IdentifierBinding entityIdentifierBinding = bindingState.getIdentifierBinding(
				targetTypeBinder.getManagedType().getHierarchy().getRoot()
		);
		if ( entityIdentifierBinding == null ) {
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
				entityIdentifierBinding,
				entityIdentifierBinding.columns()
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
		if ( source.elementType() == null ) {
			throw new AnnotationException(
					source.member().getDeclaringType().getName() + "." + source.member().resolveAttributeName()
							+ " is not a collection and may not be a '@OneToMany', '@ManyToMany', or '@ElementCollection'"
			);
		}
		return resolveTargetClassDetails( source.elementType() );
	}

	private ClassDetails resolveTargetClassDetails(TypeDetails elementType) {
		return switch ( elementType.getTypeKind() ) {
			case PARAMETERIZED_TYPE -> elementType.asParameterizedType().getRawClassDetails();
			case WILDCARD_TYPE -> resolveTargetClassDetails( elementType.asWildcardType().getExtendsBound() );
			default -> elementType.determineRawClass();
		};
	}

	private EntityTypeBinder resolveTargetTypeBinder(ClassDetails targetClassDetails, CollectionSource source) {
		final EntityTypeBinder targetTypeBinder = (EntityTypeBinder) bindingState.getTypeBinder( targetClassDetails );
		if ( targetTypeBinder == null ) {
			throw new MappingException(
					"Could not resolve local type binding for plural association target entity - "
							+ targetClassDetails.getClassName()
							+ " (role: " + ownerType.getClassDetails().getClassName() + "." + collectionRolePath
							+ ", source element type: " + source.elementType().getTypeKind() + " " + source.elementType().getName()
							+ ")"
			);
		}
		return targetTypeBinder;
	}

	private EntityTypeBinder resolveTargetTypeBinder(ClassDetails targetClassDetails) {
		final EntityTypeBinder targetTypeBinder = (EntityTypeBinder) bindingState.getTypeBinder( targetClassDetails );
		if ( targetTypeBinder == null ) {
			throw new MappingException(
					"Could not resolve local type binding for plural association target entity - "
							+ targetClassDetails.getClassName()
			);
		}
		return targetTypeBinder;
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
			IdentifierBinding entityIdentifierBinding,
			List<org.hibernate.mapping.Column> identifierColumns) {
		@Override
		public List<org.hibernate.mapping.Column> identifierColumns() {
			if ( entityIdentifierBinding.value() instanceof SortableValue sortableValue ) {
				sortableValue.sortProperties();
				return entityIdentifierBinding.value().getColumns();
			}
			if ( primaryTable.getPrimaryKey() != null
					&& !primaryTable.getPrimaryKey().getColumns().isEmpty()
					&& primaryTable.getPrimaryKey().getColumns().size() >= identifierColumns.size() ) {
				return primaryTable.getPrimaryKey().getColumns();
			}
			return identifierColumns;
		}
	}
}
