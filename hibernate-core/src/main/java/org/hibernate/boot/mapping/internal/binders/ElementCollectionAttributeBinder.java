/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.OnDelete;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.mapping.internal.materialize.EmbeddableMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.ResolvedUniqueKey;
import org.hibernate.boot.mapping.internal.materialize.UniqueKeyMappingMaterializer;
import org.hibernate.boot.mapping.internal.model.BasicValueIntent;
import org.hibernate.boot.mapping.internal.model.CollectionValueIntent;
import org.hibernate.boot.mapping.internal.sources.BasicValueSource;
import org.hibernate.boot.mapping.internal.sources.ColumnSource;
import org.hibernate.boot.mapping.internal.sources.CollectionSource;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.boot.mapping.internal.sources.ForeignKeySource;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.categorize.AttributeMetadata;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.usertype.CompositeUserType;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.UniqueConstraint;

import static org.hibernate.boot.mapping.internal.binders.AttributeBinder.applyChecks;
import static org.hibernate.boot.mapping.internal.binders.AttributeBinder.applyColumnTransformer;

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
	private final CollectionValueIntent collectionValueIntent;
	private final boolean registerCollectionBindings;
	private final UniqueKeyMappingMaterializer uniqueKeyMappingMaterializer = new UniqueKeyMappingMaterializer();

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
				null,
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
				null,
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
				registerCollectionBindings
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
		this.collectionValueIntent = collectionValueIntent;
		this.registerCollectionBindings = registerCollectionBindings;
	}

	Collection bind(Property property) {
		final CollectionSource source = collectionValueIntent == null
				? CollectionSource.elementCollection(
						attributeMetadata.getMember(),
						bindingContext.getClassDetailsRegistry().resolveClassDetails( ownerBinding.getClassName() ),
						ownerType.getHierarchy().getRoot().getClassDetails(),
						bindingContext.getModelsContext()
				)
				: collectionValueIntent.source();
		final CollectionTable collectionTable = source.collectionTable();
		final Table table = registerCollectionBindings ? bindCollectionTable( source ) : createDeclarationOnlyTable();
		final Collection collection = createCollection( source );
		collection.setRole( ownerBinding.getEntityName() + "." + collectionRolePath );
		collection.setCollectionTable( table );
		collection.setInverse( false );
		collection.setMutable( source.isMutable() );
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
		if ( !joinColumns.isEmpty()
				&& joinColumns.size() != ownerIdentifierBinding.columns().size()
				&& !ToOneAttributeBinder.hasReferencedColumnName( joinColumns ) ) {
			throw new MappingException(
					"Collection table join column count did not match owner identifier column count - "
							+ ownerType.getClassDetails().getClassName()
			);
		}
		if ( registerCollectionBindings ) {
			bindingState.addCollectionTableBinding( new CollectionTableBinding(
					collection,
					joinColumns,
					ForeignKeySource.firstSpecified(
							ForeignKeySource.fromFirstSpecifiedJoinColumn( joinColumns ),
							source.joinTable() == null
									? ForeignKeySource.from( collectionTable )
									: ForeignKeySource.from( source.joinTable() )
					),
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
		return CollectionMappingHelper.createCollection( source, ownerBinding, bindingState );
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
								collectionRolePath,
								source.joinTable()
						)
						.binding();
			}
			return modelBinders.getTableBinder()
					.bindCollectionTable(
							resolveOwnerEntityType(),
							ownerBinding.getTable(),
							collectionRolePath,
							source.collectionTable()
					)
					.binding();
		}

	private Value bindElementValue(CollectionSource source, Collection collection, Table table) {
		final ComponentElement componentElement = resolveComponentElement( source );
		if ( componentElement != null ) {
			return bindEmbeddableElementValue( source, collection, table, componentElement );
		}
			return bindBasicElementValue( source, table );
		}

	private Component bindEmbeddableElementValue(
			CollectionSource collectionSource,
			Collection collection,
			Table table,
			ComponentElement componentElement) {
		final ComponentSource source = componentElement.compositeUserTypeClass() == null
				? ComponentSource.collectionElement(
						collectionSource.member(),
						ownerType.getAccessType(),
						collectionRolePath,
						bindingContext
				)
				: ComponentSource.syntheticCollectionElement(
						collectionSource.member(),
						componentElement.componentType(),
						ownerType.getAccessType(),
						bindingContext
				);
		final Component component =
				new EmbeddableMappingMaterializer( bindingState ).createCollectionElementComponent(
						source,
						collection,
						table
				);
		if ( componentElement.compositeUserTypeClass() != null ) {
			component.setTypeName( componentElement.compositeUserTypeClass().getName() );
		}
		EmbeddableAttributeBinder.bindDiscriminator(
				component,
				table,
				source,
				"element_DTYPE",
				bindingState,
				bindingOptions,
				bindingContext
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
		if ( componentElement.compositeUserTypeClass() != null ) {
			EmbeddableAttributeBinder.processCompositeUserType(
					component,
					instantiateCompositeUserType( componentElement.compositeUserTypeClass() )
			);
		}
		return component;
	}

	private record ComponentElement(
			ClassDetails componentType,
			Class<? extends CompositeUserType<?>> compositeUserTypeClass) {
	}

	private ComponentElement resolveComponentElement(CollectionSource source) {
		final CompositeType compositeType = source.member().getDirectAnnotationUsage( CompositeType.class );
		if ( compositeType != null ) {
			final CompositeUserType<?> compositeUserType = instantiateCompositeUserType( compositeType.value() );
			return new ComponentElement(
					bindingContext.getClassDetailsRegistry()
							.resolveClassDetails( compositeUserType.embeddable().getName() ),
					compositeType.value()
			);
		}

		final ClassDetails elementType = source.elementType().determineRawClass();
		if ( elementType.isRealClass() ) {
			final Class<?> elementJavaType = elementType.toJavaClass();
			final Class<? extends CompositeUserType<?>> registeredCompositeUserType =
					elementJavaType == null ? null : bindingState.findRegisteredCompositeUserType( elementJavaType );
			if ( registeredCompositeUserType != null ) {
				final CompositeUserType<?> compositeUserType = instantiateCompositeUserType( registeredCompositeUserType );
				return new ComponentElement(
						bindingContext.getClassDetailsRegistry()
								.resolveClassDetails( compositeUserType.embeddable().getName() ),
						registeredCompositeUserType
				);
			}
		}

		return source.hasEmbeddableElement() ? new ComponentElement( elementType, null ) : null;
	}

	private CompositeUserType<?> instantiateCompositeUserType(
			Class<? extends CompositeUserType<?>> compositeUserTypeClass) {
		return bindingContext.getBuildingPlan().isAllowExtensionsInCdi()
				? bindingContext.getManagedBeanRegistry().getBean( compositeUserTypeClass ).getBeanInstance()
				: FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( compositeUserTypeClass );
	}

	private BasicValue bindBasicElementValue(CollectionSource source, Table table) {
		final BasicValue element = BasicValue.unregistered( bindingState.getMetadataBuildingContext(), table );
		element.setTable( table );
		final BasicValueIntent valueIntent = BasicValueIntent.fromCollectionElement( source );
		final var resolutionInput = BasicValueSourceBinder.bindBasicValue(
				BasicValueSource.collectionElement( source.member(), source.elementType(), bindingContext ),
				null,
				element,
				bindingOptions,
				bindingState,
				bindingContext
		);
		bindingState.addAttributeValueResolution( AttributeBindingPhase.valueResolution( resolutionInput ) );

		final jakarta.persistence.Column column = source.elementColumn();
		final org.hibernate.mapping.Column elementColumn = ColumnBinder.bindColumn(
				ColumnSource.from( column ),
				() -> implicitElementColumnName( source )
		);
		final Property property = new Property();
		property.setName( source.member().resolveAttributeName() );
		applyColumnTransformer( valueIntent, property, elementColumn );
		applyChecks( valueIntent, elementColumn );
		table.addColumn( elementColumn );
		element.addColumn( elementColumn );
		if ( elementColumn.isUnique() ) {
			uniqueKeyMappingMaterializer.materializeUniqueKey(
					ResolvedUniqueKey.from( elementColumn, table, bindingState.getMetadataBuildingContext() )
			);
		}
		return element;
	}

		private String implicitElementColumnName(CollectionSource source) {
			return bindingContext.getImplicitNamingStrategy()
					.determineBasicColumnName( new ImplicitBasicColumnNameSource() {
						@Override
						public AttributePath getAttributePath() {
							return AttributePath.parse( source.member().resolveAttributeName() );
						}

						@Override
						public boolean isCollectionElement() {
							return false;
						}

						@Override
						public MetadataBuildingContext getBuildingContext() {
							return bindingState.getMetadataBuildingContext();
						}
					} )
					.getText();
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
