/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.annotations.OnDelete;
import org.hibernate.boot.models.bind.internal.materialize.EmbeddableMappingMaterializer;
import org.hibernate.boot.models.bind.internal.sources.BasicValueSource;
import org.hibernate.boot.models.bind.internal.sources.ColumnSource;
import org.hibernate.boot.models.bind.internal.sources.CollectionSource;
import org.hibernate.boot.models.bind.internal.sources.ComponentSource;
import org.hibernate.boot.models.bind.internal.sources.ForeignKeySource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.UniqueConstraint;

/// Binds element-collection attributes.
///
/// This binder creates the collection mapping, collection table, element value,
/// and any synthetic index value for list/map variants.  The owner key is
/// intentionally deferred through [CollectionTableBinding] because it depends on
/// the owner hierarchy's identifier binding and participates in later foreign-key
/// creation.
///
/// Element values may be basic or embeddable.  For embeddable elements, the
/// collection member remains the source for path-based overrides and converter
/// declarations, even though the bound component members come from the embeddable
/// element type.
///
/// @since 9.0
/// @author Steve Ebersole
class ElementCollectionAttributeBinder {
	private final IdentifiableTypeMetadata ownerType;
	private final PersistentClass ownerBinding;
	private final AttributeMetadata attributeMetadata;
	private final ModelBinders modelBinders;
	private final BindingOptions bindingOptions;
	private final BindingState bindingState;
	private final BindingContext bindingContext;
	private final String collectionRolePath;
	private final boolean registerCollectionBindings;

	ElementCollectionAttributeBinder(
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
				true
		);
	}

	ElementCollectionAttributeBinder(
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
				true
		);
	}

	ElementCollectionAttributeBinder(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			AttributeMetadata attributeMetadata,
			ModelBinders modelBinders,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext,
			String collectionRolePath,
			boolean registerCollectionBindings) {
		this.ownerType = ownerType;
		this.ownerBinding = ownerBinding;
		this.attributeMetadata = attributeMetadata;
		this.modelBinders = modelBinders;
		this.bindingOptions = bindingOptions;
		this.bindingState = bindingState;
		this.bindingContext = bindingContext;
		this.collectionRolePath = collectionRolePath;
		this.registerCollectionBindings = registerCollectionBindings;
	}

		Collection bind(Property property) {
			final CollectionSource source = CollectionSource.elementCollection(
					attributeMetadata.getMember(),
					bindingContext.getClassDetailsRegistry().resolveClassDetails( ownerBinding.getClassName() ),
					ownerType.getHierarchy().getRoot().getClassDetails(),
					bindingOptions.getDefaultListSemantics(),
					bindingContext.getBootstrapContext().getModelsContext()
			);
			final CollectionTable collectionTable = source.collectionTable();
			final Table table = registerCollectionBindings ? bindCollectionTable( source ) : createDeclarationOnlyTable();
		final Collection collection = createCollection( source );
		collection.setRole( ownerBinding.getEntityName() + "." + collectionRolePath );
		collection.setCollectionTable( table );
		collection.setInverse( false );
		collection.setMutable( true );
		collection.setOptimisticLocked( true );
		collection.setTypeUsingReflection(
				attributeMetadata.getMember().getDeclaringType().getName(),
				attributeMetadata.getName()
		);
		CollectionShapeBinder.apply( source, collection, bindingState );

		final Value element = bindElementValue( source, collection, table );
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

		final List<JoinColumn> joinColumns = source.joinColumns();
		final IdentifierBinding ownerIdentifierBinding = bindingState.getIdentifierBinding( ownerType.getHierarchy().getRoot() );
		if ( ownerIdentifierBinding == null ) {
			throw new MappingException(
					"Could not resolve identifier binding for element collection owner - "
							+ ownerType.getClassDetails().getClassName()
			);
		}
		if ( !joinColumns.isEmpty() && joinColumns.size() != ownerIdentifierBinding.columns().size() ) {
			throw new MappingException(
					"Collection table join column count did not match owner identifier column count - "
							+ ownerType.getClassDetails().getClassName()
			);
		}
		if ( registerCollectionBindings ) {
			bindingState.addCollectionTableBinding( new CollectionTableBinding(
					collection,
					joinColumns,
					source.joinTable() == null ? ForeignKeySource.from( collectionTable ) : ForeignKeySource.from( source.joinTable() ),
					resolveOnDeleteAction(),
					uniqueConstraints( source ),
					indexes( source )
			) );
			bindingState.addCollectionBinding( collection );
		}
		return collection;
	}

	private Table createDeclarationOnlyTable() {
		return new Table( "orm", ownerBinding.getEntityName() + "." + collectionRolePath + "#mapped-superclass" );
	}

	private Collection createCollection(CollectionSource source) {
		return switch ( source.classification() ) {
			case SET, ORDERED_SET, SORTED_SET -> new org.hibernate.mapping.Set( bindingState.getMetadataBuildingContext(), ownerBinding );
			case LIST -> new org.hibernate.mapping.List( bindingState.getMetadataBuildingContext(), ownerBinding );
			case MAP, ORDERED_MAP, SORTED_MAP -> new org.hibernate.mapping.Map( bindingState.getMetadataBuildingContext(), ownerBinding );
			case BAG -> new org.hibernate.mapping.Bag( bindingState.getMetadataBuildingContext(), ownerBinding );
			case ID_BAG -> new IdentifierBag( bindingState.getMetadataBuildingContext(), ownerBinding );
			case ARRAY -> {
				final org.hibernate.mapping.Array array = new org.hibernate.mapping.Array( bindingState.getMetadataBuildingContext(), ownerBinding );
				array.setElementClassName( source.elementType().determineRawClass().getClassName() );
				yield array;
			}
		};
	}

		private UniqueConstraint[] uniqueConstraints(CollectionSource source) {
			if ( source.collectionTable() != null ) {
				return source.collectionTable().uniqueConstraints();
			}
			return source.joinTable() == null ? new UniqueConstraint[0] : source.joinTable().uniqueConstraints();
		}

		private Index[] indexes(CollectionSource source) {
			if ( source.collectionTable() != null ) {
				return source.collectionTable().indexes();
			}
			return source.joinTable() == null ? new Index[0] : source.joinTable().indexes();
		}

		private Table bindCollectionTable(CollectionSource source) {
			if ( source.joinTable() != null ) {
				return modelBinders.getTableBinder()
						.bindOwnedTable(
								resolveOwnerEntityType(),
								ownerBinding.getTable(),
								attributeMetadata.getName(),
								source.joinTable()
						)
						.binding();
			}
			return modelBinders.getTableBinder()
					.bindCollectionTable(
							resolveOwnerEntityType(),
							ownerBinding.getTable(),
							attributeMetadata.getName(),
							source.collectionTable()
					)
					.binding();
		}

	private Value bindElementValue(CollectionSource source, Collection collection, Table table) {
		if ( source.hasEmbeddableElement() ) {
			return bindEmbeddableElementValue( source, collection, table );
		}
			return bindBasicElementValue( source, table );
		}

	private Component bindEmbeddableElementValue(CollectionSource collectionSource, Collection collection, Table table) {
		final ComponentSource source = ComponentSource.collectionElement(
				collectionSource.member(),
				ownerType.getAccessType(),
				bindingContext
		);
		final Component component =
				new EmbeddableMappingMaterializer( bindingState ).createCollectionElementComponent(
						source,
						collection,
						table
				);

		new ComponentBinder( modelBinders, bindingState, bindingOptions, bindingContext ).bindBasicProperties(
				ownerType,
				ownerBinding,
				source,
				component,
				table,
				(ignored, column) -> table.addColumn( column ),
				false,
				true,
				true
		);
		return component;
	}

		private BasicValue bindBasicElementValue(CollectionSource source, Table table) {
			final BasicValue element = new BasicValue( bindingState.getMetadataBuildingContext(), table );
			element.setTable( table );
			BasicValueBinder.bindBasicValue(
					BasicValueSource.collectionElement( source.member(), bindingContext ),
					null,
					element,
					bindingOptions,
				bindingState,
				bindingContext
			);

			final jakarta.persistence.Column column = source.elementColumn();
		final org.hibernate.mapping.Column elementColumn = ColumnBinder.bindColumn(
				ColumnSource.from( column ),
				() -> Collection.DEFAULT_ELEMENT_COLUMN_NAME
		);
		table.addColumn( elementColumn );
		element.addColumn( elementColumn );
		return element;
	}

	private EntityTypeMetadata resolveOwnerEntityType() {
		if ( ownerType instanceof EntityTypeMetadata entityType ) {
			return entityType;
		}
		return ownerType.getHierarchy().getRoot();
	}

	private org.hibernate.annotations.OnDeleteAction resolveOnDeleteAction() {
		final OnDelete onDelete = attributeMetadata.getMember().getDirectAnnotationUsage( OnDelete.class );
		return onDelete == null ? null : onDelete.action();
	}

}
