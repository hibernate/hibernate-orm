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
import org.hibernate.mapping.CheckConstraint;
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
				() -> source.member().resolveAttributeName() + "_ORDER"
		);
		indexColumn.addCheckConstraint( new CheckConstraint(
				null,
				indexColumn.getQuotedName( bindingState.getDatabase().getDialect() )
						+ ">=" + effectiveListIndexBase( source )
		) );
		table.addColumn( indexColumn );
		index.addColumn(
				indexColumn,
				source.orderColumn() == null || source.orderColumn().insertable(),
				source.orderColumn() == null || source.orderColumn().updatable()
		);
		collection.setIndex( index );
	}

	private static int effectiveListIndexBase(CollectionSource source) {
		final var listIndexBase = source.listIndexBase();
		return listIndexBase == null ? 0 : listIndexBase.value();
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
			bindPropertyMapKey( source, collection, source.mapKeyName(), bindingState );
			return;
		}
		if ( !source.mapKeyJoinColumns().isEmpty() ) {
			bindEntityMapKey( source, collection, table, bindingState );
			return;
		}
		if ( isEntityMapKey( source, bindingState ) ) {
			bindEntityMapKey( source, collection, table, bindingState );
			return;
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
			String mapKeyName,
			BindingState bindingState) {
		if ( mapKeyName.isEmpty() ) {
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
					mapKeyName
			) );
			return;
		}

		final Property targetProperty = resolveComponentMapKeyProperty( source, collection, mapKeyName );
		bindPropertyMapKeyValue( collection, targetProperty.getValue(), mapKeyName, bindingState );
	}

	static void bindPropertyMapKey(
			PropertyMapKeyBinding propertyMapKeyBinding,
			BindingState bindingState) {
		final Property targetProperty = resolveEntityMapKeyProperty( propertyMapKeyBinding );
		final org.hibernate.mapping.Map collection = propertyMapKeyBinding.collection();
		bindPropertyMapKeyValue( collection, targetProperty.getValue(), propertyMapKeyBinding.propertyName(), bindingState );
	}

	static void bindPropertyMapKeyValue(
			org.hibernate.mapping.Map collection,
		Value targetPropertyValue,
		String propertyName,
		BindingState bindingState) {
		collection.setIndex( createPropertyMapKeyValue( collection, targetPropertyValue, bindingState ) );
		collection.setMapKeyPropertyName( propertyName );
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
			throw new MappingException(
					"Implicit @MapKey requires an entity-valued map element whose identifier can be used as the map key - "
							+ collection.getRole()
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
		if ( propertyMapKeyBinding.propertyName().contains( "." ) ) {
			return resolvePropertyPath(
					propertyMapKeyBinding.elementTypeBinder().getTypeBinding(),
					propertyMapKeyBinding.propertyName()
			);
		}
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
			String mapKeyName) {
		if ( collection.getElement() instanceof Component component ) {
			return mapKeyName.contains( "." )
					? resolvePropertyPath( component, mapKeyName )
					: component.getProperty( mapKeyName );
		}
		throw new MappingException(
				"Could not resolve property-based map key element - "
						+ source.elementType().determineRawClass().getClassName()
		);
	}

	static Property resolvePropertyPath(PersistentClass typeBinding, String propertyPath) {
		final int separator = propertyPath.indexOf( '.' );
		if ( separator < 0 ) {
			final Property identifierProperty = typeBinding.getIdentifierProperty();
			if ( identifierProperty != null && identifierProperty.getName().equals( propertyPath ) ) {
				return identifierProperty;
			}
			return typeBinding.getProperty( propertyPath );
		}

		final String firstSegment = propertyPath.substring( 0, separator );
		final Property firstProperty = resolvePropertyPath( typeBinding, firstSegment );
		return resolvePropertyPath( firstProperty.getValue(), propertyPath.substring( separator + 1 ) );
	}

	private static Property resolvePropertyPath(Component component, String propertyPath) {
		return resolvePropertyPath( (Value) component, propertyPath );
	}

	private static Property resolvePropertyPath(Value value, String propertyPath) {
		if ( !( value instanceof Component component ) ) {
			throw new MappingException(
					"Could not resolve nested @MapKey property path `" + propertyPath + "` from non-component value"
			);
		}

		final int separator = propertyPath.indexOf( '.' );
		final String firstSegment = separator < 0 ? propertyPath : propertyPath.substring( 0, separator );
		final Property property = component.getProperty( firstSegment );
		return separator < 0
				? property
				: resolvePropertyPath( property.getValue(), propertyPath.substring( separator + 1 ) );
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
		if ( targetPropertyValue instanceof ManyToOne manyToOne ) {
			final ManyToOne index = new ManyToOne(
					bindingState.getMetadataBuildingContext(),
					manyToOne.getTable()
			);
			index.setReferencedEntityName( manyToOne.getReferencedEntityName() );
			index.setReferenceToPrimaryKey( manyToOne.isReferenceToPrimaryKey() );
			index.setReferencedPropertyName( manyToOne.getReferencedPropertyName() );
			index.setTypeName( manyToOne.getTypeName() );
			index.setTypeUsingReflection(
					collection.getOwner().getClassName(),
					collection.getRole().substring( collection.getRole().lastIndexOf( '.' ) + 1 )
			);
			if ( manyToOne.isLogicalOneToOne() ) {
				index.markAsLogicalOneToOne();
			}
			for ( Column column : manyToOne.getColumns() ) {
				index.addColumn( column.clone(), false, false );
			}
			return index;
		}
		if ( targetPropertyValue instanceof Component component ) {
			final Component index = new Component( bindingState.getMetadataBuildingContext(), collection );
			index.setComponentClassName( component.getComponentClassName() );
			index.setEmbedded( component.isEmbedded() );
			index.setDynamic( component.isDynamic() );
			index.setRoleName( component.getRoleName() );
			for ( Property property : component.getProperties() ) {
				final Property copy = property.copy();
				copy.setValue( createPropertyMapKeyValue( collection, property.getValue(), bindingState ) );
				copy.setInsertable( false );
				copy.setUpdatable( false );
				copy.setPersistentClass( collection.getOwner() );
				index.addProperty( copy );
			}
			index.sortProperties();
			return index;
		}
		throw new UnsupportedOperationException(
				"@MapKey(name) is only implemented for basic, to-one, and component target properties - "
						+ collection.getRole()
		);
	}

	private static Column copyColumn(Table table, Column source, boolean unique) {
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
				() -> source.member().resolveAttributeName() + "_KEY",
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

		final IdentifierBinding entityIdentifierBinding = bindingState.getIdentifierBinding(
				targetTypeBinder.getManagedType().getHierarchy().getRoot()
		);
		if ( entityIdentifierBinding == null ) {
			throw new MappingException(
					"Could not resolve identifier binding for entity-valued map key - "
							+ targetTypeBinder.getTypeBinding().getEntityName()
			);
		}

		final ManyToOne index = new ManyToOne( bindingState.getMetadataBuildingContext(), table );
		index.setReferencedEntityName( targetTypeBinder.getTypeBinding().getEntityName() );
		final boolean referenceToPrimaryKey = referencesPrimaryKey(
				source.mapKeyJoinColumns(),
				entityIdentifierBinding.columns()
		);
		index.setReferenceToPrimaryKey( referenceToPrimaryKey );
		index.setTypeName( targetTypeBinder.getTypeBinding().getEntityName() );
		index.setTypeUsingReflection( collection.getOwner().getClassName(), source.member().resolveAttributeName() );

		final List<MapKeyJoinColumn> orderedJoinColumns = referenceToPrimaryKey
				? orderMapKeyJoinColumns(
						source.mapKeyJoinColumns(),
						entityIdentifierBinding.columns(),
						collection.getRole()
				)
				: source.mapKeyJoinColumns();
		final int columnCount = referenceToPrimaryKey ? entityIdentifierBinding.columns().size() : source.mapKeyJoinColumns().size();
		for ( int i = 0; i < columnCount; i++ ) {
			final MapKeyJoinColumn mapKeyJoinColumn = orderedJoinColumns.isEmpty() ? null : orderedJoinColumns.get( i );
			final String targetColumnName = referenceToPrimaryKey
					? entityIdentifierBinding.columns().get( i ).getName()
					: mapKeyJoinColumn.referencedColumnName();
			final Column column = ColumnBinder.bindColumn(
					ColumnSource.from( mapKeyJoinColumn ),
					() -> implicitMapKeyJoinColumnName( source, referenceToPrimaryKey, columnCount, targetColumnName ),
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

	private static String implicitMapKeyJoinColumnName(
			CollectionSource source,
			boolean referenceToPrimaryKey,
			int columnCount,
			String targetColumnName) {
		if ( referenceToPrimaryKey && columnCount == 1 ) {
			return source.member().resolveAttributeName() + "_KEY";
		}
		return source.member().resolveAttributeName() + "_KEY_" + targetColumnName;
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
