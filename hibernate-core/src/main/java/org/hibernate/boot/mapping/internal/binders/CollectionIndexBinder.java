/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.annotations.MapKeyCompositeType;
import org.hibernate.boot.mapping.internal.materialize.EmbeddableMappingMaterializer;
import org.hibernate.boot.mapping.internal.sources.BasicValueSource;
import org.hibernate.boot.mapping.internal.sources.ColumnSource;
import org.hibernate.boot.mapping.internal.sources.CollectionSource;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.boot.mapping.internal.sources.ForeignKeySource;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantBasicValue;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.ModelsException;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.usertype.CompositeUserType;

import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyJoinColumn;

/// Binds synthetic collection index values such as list indexes and map keys.
///
/// Collection index binding is split between immediate work and deferred work.
/// Synthetic list indexes and basic map keys can be created while binding the
/// collection member.  Property-derived map keys, especially `@MapKey(name)` on
/// entity-valued elements, need the element type's members to exist first and are
/// completed in the collection-index phase from [PropertyMapKeyBinding].
///
/// @since 9.0
/// @author Steve Ebersole
class CollectionIndexBinder {
	static void bindListIndex(
			CollectionSource source,
			IndexedCollection collection,
			Table table,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final BasicValue index = new BasicValue( bindingState.getMetadataBuildingContext(), table );
		index.setTable( table );
		BasicValueBinder.bindBasicValue(
				BasicValueSource.listIndex( source.member() ),
				null,
				index,
				bindingOptions,
				bindingState,
				bindingContext
		);

		final org.hibernate.mapping.Column indexColumn = ColumnBinder.bindColumn(
				ColumnSource.from( source.orderColumn() ),
				() -> IndexedCollection.DEFAULT_INDEX_COLUMN_NAME
		);
		table.addColumn( indexColumn );
		index.addColumn(
				indexColumn,
				source.orderColumn() == null || source.orderColumn().insertable(),
				source.orderColumn() == null || source.orderColumn().updatable()
		);
		collection.setIndex( index );
	}

	static void bindMapKey(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			CollectionSource source,
			org.hibernate.mapping.Map collection,
			Table table,
			ModelBinders modelBinders,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final MapKey mapKey = source.mapKey();
		if ( mapKey != null ) {
			bindPropertyMapKey( source, collection, mapKey, bindingState );
			return;
		}
		if ( !source.mapKeyJoinColumns().isEmpty() ) {
			bindEntityMapKey( source, collection, table, bindingState );
			return;
		}
		if ( isEntityMapKey( source, bindingState ) ) {
			throw new UnsupportedOperationException(
					"Entity-valued map keys require @MapKeyJoinColumn support - " + collection.getRole()
			);
		}
		final ComponentMapKey componentMapKey = resolveComponentMapKey( source, bindingState, bindingContext );
		if ( componentMapKey != null ) {
			bindComponentMapKey(
					ownerType,
					ownerBinding,
					source,
					collection,
					table,
					componentMapKey,
					modelBinders,
					bindingOptions,
					bindingState,
					bindingContext
			);
			return;
		}
		bindBasicMapKey( source, collection, table, bindingOptions, bindingState, bindingContext );
	}

	private record ComponentMapKey(
			ClassDetails componentType,
			Class<? extends CompositeUserType<?>> compositeUserTypeClass) {
	}

	private static ComponentMapKey resolveComponentMapKey(
			CollectionSource source,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( source.mapKeyType() == null ) {
			return null;
		}

		final MapKeyCompositeType mapKeyCompositeType =
				source.member().getDirectAnnotationUsage( MapKeyCompositeType.class );
		if ( mapKeyCompositeType != null ) {
			final CompositeUserType<?> compositeUserType = instantiateCompositeUserType(
					mapKeyCompositeType.value(),
					bindingContext
			);
			return new ComponentMapKey(
					bindingContext.getClassDetailsRegistry()
							.resolveClassDetails( compositeUserType.embeddable().getName() ),
					mapKeyCompositeType.value()
			);
		}

		final Class<?> mapKeyJavaType = source.mapKeyType().determineRawClass().toJavaClass();
		final Class<? extends CompositeUserType<?>> registeredCompositeUserType =
				bindingState.getMetadataBuildingContext()
						.getMetadataCollector()
						.findRegisteredCompositeUserType( mapKeyJavaType );
		if ( registeredCompositeUserType != null ) {
			final CompositeUserType<?> compositeUserType = instantiateCompositeUserType(
					registeredCompositeUserType,
					bindingContext
			);
			return new ComponentMapKey(
					bindingContext.getClassDetailsRegistry()
							.resolveClassDetails( compositeUserType.embeddable().getName() ),
					registeredCompositeUserType
			);
		}

		final ClassDetails mapKeyType = source.mapKeyType().determineRawClass();
		if ( mapKeyType.hasDirectAnnotationUsage( jakarta.persistence.Embeddable.class ) ) {
			return new ComponentMapKey( mapKeyType, null );
		}

		return null;
	}

	private static CompositeUserType<?> instantiateCompositeUserType(
			Class<? extends CompositeUserType<?>> compositeUserTypeClass,
			BindingContext bindingContext) {
		return bindingContext.getBootstrapContext().getMetadataBuildingOptions().isAllowExtensionsInCdi()
				? bindingContext.getBootstrapContext()
						.getManagedBeanRegistry()
						.getBean( compositeUserTypeClass )
						.getBeanInstance()
				: FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( compositeUserTypeClass );
	}

	private static void bindComponentMapKey(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			CollectionSource collectionSource,
			org.hibernate.mapping.Map collection,
			Table table,
			ComponentMapKey componentMapKey,
			ModelBinders modelBinders,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final ComponentSource source = ComponentSource.mapKey(
				collectionSource.member(),
				componentMapKey.componentType(),
				ownerType.getAccessType(),
				bindingContext
		);
		final Component component =
				new EmbeddableMappingMaterializer( bindingState ).createMapKeyComponent(
						source,
						collection,
						table
				);
		if ( componentMapKey.compositeUserTypeClass() != null ) {
			component.setTypeName( componentMapKey.compositeUserTypeClass().getName() );
		}

		new ComponentBinder( modelBinders, bindingState, bindingOptions, bindingContext ).bindBasicProperties(
				ownerType,
				ownerBinding,
				source,
				component,
				table,
				(ignored, column) -> table.addColumn( column ),
				false,
				false,
				false
		);
		collection.setIndex( component );
	}

	private static boolean isEntityMapKey(CollectionSource source, BindingState bindingState) {
		return source.mapKeyType() != null
				&& bindingState.getTypeBinder( source.mapKeyType().determineRawClass() ) instanceof EntityTypeBinder;
	}

	private static void bindPropertyMapKey(
			CollectionSource source,
			org.hibernate.mapping.Map collection,
			MapKey mapKey,
			BindingState bindingState) {
		if ( mapKey.name().isEmpty() ) {
			bindImplicitPropertyMapKey( source, collection, bindingState );
			return;
		}
		final ManagedTypeBinder elementTypeBinder = bindingState.getTypeBinder(
				source.elementType().determineRawClass()
		);
		if ( elementTypeBinder instanceof EntityTypeBinder entityTypeBinder ) {
			bindingState.addPropertyMapKeyBinding( new PropertyMapKeyBinding(
					collection,
					entityTypeBinder,
					mapKey.name()
			) );
			return;
		}

		final Property targetProperty = resolveComponentMapKeyProperty( source, collection, mapKey );
		collection.setIndex( createPropertyMapKeyValue( collection, targetProperty.getValue(), bindingState ) );
		collection.setMapKeyPropertyName( mapKey.name() );
		collection.setHasMapKeyProperty( true );
	}

	static void bindPropertyMapKey(
			PropertyMapKeyBinding propertyMapKeyBinding,
			BindingState bindingState) {
		final Property targetProperty = resolveEntityMapKeyProperty( propertyMapKeyBinding );
		final org.hibernate.mapping.Map collection = propertyMapKeyBinding.collection();
		collection.setIndex( createPropertyMapKeyValue( collection, targetProperty.getValue(), bindingState ) );
		collection.setMapKeyPropertyName( propertyMapKeyBinding.propertyName() );
		collection.setHasMapKeyProperty( true );
	}

	private static void bindImplicitPropertyMapKey(
			CollectionSource source,
			org.hibernate.mapping.Map collection,
			BindingState bindingState) {
		final ManagedTypeBinder elementTypeBinder = bindingState.getTypeBinder(
				source.elementType().determineRawClass()
		);
		if ( !( elementTypeBinder instanceof EntityTypeBinder entityTypeBinder ) ) {
			throw new UnsupportedOperationException(
					"Implicit @MapKey is only implemented for entity-valued map elements - " + collection.getRole()
			);
		}
		collection.setIndex( createPropertyMapKeyValue(
				collection,
				entityTypeBinder.getTypeBinding().getIdentifier(),
				bindingState
		) );
		collection.setMapKeyPropertyName( null );
		collection.setHasMapKeyProperty( true );
	}

	private static Property resolveEntityMapKeyProperty(PropertyMapKeyBinding propertyMapKeyBinding) {
		final Property identifierProperty = propertyMapKeyBinding.elementTypeBinder().getTypeBinding()
				.getIdentifierProperty();
		if ( identifierProperty != null && identifierProperty.getName().equals( propertyMapKeyBinding.propertyName() ) ) {
			return identifierProperty;
		}
		return propertyMapKeyBinding.elementTypeBinder().getTypeBinding()
				.getProperty( propertyMapKeyBinding.propertyName() );
	}

	private static Property resolveComponentMapKeyProperty(
			CollectionSource source,
			org.hibernate.mapping.Map collection,
			MapKey mapKey) {
		if ( collection.getElement() instanceof Component component ) {
			return component.getProperty( mapKey.name() );
		}
		throw new MappingException(
				"Could not resolve property-based map key element - "
						+ source.elementType().determineRawClass().getClassName()
		);
	}

	static Value createPropertyMapKeyValue(
			org.hibernate.mapping.Map collection,
			Value targetPropertyValue,
			BindingState bindingState) {
		if ( targetPropertyValue instanceof BasicValue basicValue ) {
			final DependantBasicValue index = new DependantBasicValue(
					bindingState.getMetadataBuildingContext(),
					basicValue.getTable(),
					basicValue,
					false,
					false
			);
			index.setImplicitJavaTypeAccess( (typeConfiguration) -> basicValue.resolve()
					.getDomainJavaType()
					.getJavaType() );
			for ( Column column : basicValue.getColumns() ) {
				index.addColumn( column.clone(), false, false );
			}
			return index;
		}
		throw new UnsupportedOperationException(
				"@MapKey(name) is only implemented for basic target properties - " + collection.getRole()
		);
	}

	private static void bindBasicMapKey(
			CollectionSource source,
			org.hibernate.mapping.Map collection,
			Table table,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final BasicValue index = new BasicValue( bindingState.getMetadataBuildingContext(), table );
		index.setTable( table );
		BasicValueBinder.bindBasicValue(
				BasicValueSource.mapKey( source.member(), bindingContext ),
				null,
				index,
				bindingOptions,
				bindingState,
				bindingContext
		);

		final org.hibernate.mapping.Column indexColumn = ColumnBinder.bindColumn(
				ColumnSource.from( source.mapKeyColumn() ),
				() -> Collection.DEFAULT_KEY_COLUMN_NAME,
				false,
				false
		);
		table.addColumn( indexColumn );
		index.addColumn(
				indexColumn,
				source.mapKeyColumn() == null || source.mapKeyColumn().insertable(),
				source.mapKeyColumn() == null || source.mapKeyColumn().updatable()
		);
		collection.setIndex( index );
	}

	private static void bindEntityMapKey(
			CollectionSource source,
			org.hibernate.mapping.Map collection,
			Table table,
			BindingState bindingState) {
		final EntityTypeBinder targetTypeBinder = (EntityTypeBinder) bindingState.getTypeBinder(
				source.mapKeyType().determineRawClass()
		);
		if ( targetTypeBinder == null ) {
			throw new MappingException(
					"Could not resolve local type binding for entity-valued map key - "
							+ source.mapKeyType().determineRawClass().getClassName()
			);
		}

		final IdentifierBinding identifierBinding = bindingState.getIdentifierBinding(
				targetTypeBinder.getManagedType().getHierarchy().getRoot()
		);
		if ( identifierBinding == null ) {
			throw new MappingException(
					"Could not resolve identifier binding for entity-valued map key - "
							+ targetTypeBinder.getTypeBinding().getEntityName()
			);
		}

		final ManyToOne index = new ManyToOne( bindingState.getMetadataBuildingContext(), table );
		index.setReferencedEntityName( targetTypeBinder.getTypeBinding().getEntityName() );
		final boolean referenceToPrimaryKey = referencesPrimaryKey(
				source.mapKeyJoinColumns(),
				identifierBinding.columns()
		);
		index.setReferenceToPrimaryKey( referenceToPrimaryKey );
		index.setTypeName( targetTypeBinder.getTypeBinding().getEntityName() );
		index.setTypeUsingReflection( collection.getOwner().getClassName(), source.member().resolveAttributeName() );

		final List<MapKeyJoinColumn> orderedJoinColumns = referenceToPrimaryKey
				? orderMapKeyJoinColumns(
						source.mapKeyJoinColumns(),
						identifierBinding.columns(),
						collection.getRole()
				)
				: source.mapKeyJoinColumns();
		final int columnCount = referenceToPrimaryKey ? identifierBinding.columns().size() : source.mapKeyJoinColumns().size();
		for ( int i = 0; i < columnCount; i++ ) {
			final MapKeyJoinColumn mapKeyJoinColumn = orderedJoinColumns.isEmpty() ? null : orderedJoinColumns.get( i );
			final String targetColumnName = referenceToPrimaryKey
					? identifierBinding.columns().get( i ).getName()
					: mapKeyJoinColumn.referencedColumnName();
			final Column column = ColumnBinder.bindColumn(
					ColumnSource.from( mapKeyJoinColumn ),
					() -> Collection.DEFAULT_KEY_COLUMN_NAME + "_" + targetColumnName,
					false,
					false
			);
			table.addColumn( column );
			index.addColumn(
					column,
					mapKeyJoinColumn == null || mapKeyJoinColumn.insertable(),
				mapKeyJoinColumn == null || mapKeyJoinColumn.updatable()
			);
		}
		collection.setIndex( index );
		if ( !referenceToPrimaryKey ) {
			bindingState.addAssociationTargetBinding( new AssociationTargetBinding(
					collection.getOwner(),
					index,
					targetTypeBinder,
					referencedColumnNames( source.mapKeyJoinColumns() ),
					collection.getRole()
			) );
		}
		bindingState.addForeignKeyBinding( new ForeignKeyBinding(
				collection.getOwner(),
				index,
				orderedJoinColumns.isEmpty() ? null : ForeignKeySource.from( orderedJoinColumns.get( 0 ).foreignKey() )
		) );
	}

	private static boolean referencesPrimaryKey(List<MapKeyJoinColumn> joinColumns, List<Column> targetColumns) {
		if ( joinColumns.isEmpty()
				|| joinColumns.stream().noneMatch( (joinColumn) -> !joinColumn.referencedColumnName().isEmpty() ) ) {
			return true;
		}
		if ( joinColumns.size() != targetColumns.size() ) {
			return false;
		}
		final ArrayList<Column> unmatchedTargetColumns = new ArrayList<>( targetColumns );
		for ( MapKeyJoinColumn joinColumn : joinColumns ) {
			final Column targetColumn = findTargetColumn( unmatchedTargetColumns, joinColumn.referencedColumnName() );
			if ( targetColumn == null ) {
				return false;
			}
			unmatchedTargetColumns.remove( targetColumn );
		}
		return unmatchedTargetColumns.isEmpty();
	}

	private static Column findTargetColumn(List<Column> targetColumns, String columnName) {
		for ( Column targetColumn : targetColumns ) {
			if ( targetColumn.getName().equals( columnName ) ) {
				return targetColumn;
			}
		}
		return null;
	}

	private static List<String> referencedColumnNames(List<MapKeyJoinColumn> joinColumns) {
		final ArrayList<String> result = new ArrayList<>( joinColumns.size() );
		for ( MapKeyJoinColumn joinColumn : joinColumns ) {
			result.add( joinColumn.referencedColumnName() );
		}
		return result;
	}

	private static List<MapKeyJoinColumn> orderMapKeyJoinColumns(
			List<MapKeyJoinColumn> joinColumns,
			List<Column> targetColumns,
			String role) {
		if ( joinColumns.isEmpty() || joinColumns.stream().noneMatch( (joinColumn) -> !joinColumn.referencedColumnName().isEmpty() ) ) {
			return joinColumns;
		}

		final ArrayList<MapKeyJoinColumn> orderedJoinColumns = new ArrayList<>( targetColumns.size() );
		final ArrayList<MapKeyJoinColumn> unmatchedJoinColumns = new ArrayList<>( joinColumns );
		for ( Column targetColumn : targetColumns ) {
			final MapKeyJoinColumn joinColumn = findMapKeyJoinColumn( targetColumn, unmatchedJoinColumns, role );
			orderedJoinColumns.add( joinColumn );
			unmatchedJoinColumns.remove( joinColumn );
		}
		return orderedJoinColumns;
	}

	private static MapKeyJoinColumn findMapKeyJoinColumn(
			Column targetColumn,
			List<MapKeyJoinColumn> joinColumns,
			String role) {
		for ( MapKeyJoinColumn joinColumn : joinColumns ) {
			if ( targetColumn.getName().equals( joinColumn.referencedColumnName() ) ) {
				return joinColumn;
			}
		}

		throw new ModelsException(
				"Unable to match map-key join column referencedColumnName to target identifier column `"
						+ targetColumn.getName() + "` - " + role
		);
	}
}
