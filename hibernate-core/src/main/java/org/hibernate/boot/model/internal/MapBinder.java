/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.annotations.MapKeyCompositeType;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantBasicValue;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.BasicType;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserCollectionType;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.MapKeyJoinColumns;

import static org.hibernate.boot.model.internal.AnnotatedClassType.EMBEDDABLE;
import static org.hibernate.boot.model.internal.AnnotatedClassType.NONE;
import static org.hibernate.boot.model.internal.BinderHelper.findPropertyByName;
import static org.hibernate.boot.model.internal.BinderHelper.isPrimitive;
import static org.hibernate.boot.model.internal.EmbeddableBinder.fillEmbeddable;
import static org.hibernate.boot.model.internal.PropertyHolderBuilder.buildPropertyHolder;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.internal.util.StringHelper.qualify;

/**
 * A {@link CollectionBinder} for {@link org.hibernate.collection.spi.PersistentMap maps},
 * whose mapping model type is {@link Map}.
 *
 * @author Emmanuel Bernard
 */
public class MapBinder extends CollectionBinder {

	public MapBinder(
			Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver,
			boolean sorted,
			MetadataBuildingContext buildingContext) {
		super( customTypeBeanResolver, sorted, buildingContext );
	}

	@Override
	boolean isMap() {
		return true;
	}

	private Map getMap() {
		return (Map) collection;
	}

	@Override
	protected Collection createCollection(PersistentClass owner) {
		return new Map( getCustomTypeBeanResolver(), owner, getBuildingContext() );
	}

	@Override
	SecondPass getSecondPass() {
		return new CollectionSecondPass( collection ) {
			public void secondPass(java.util.Map<String, PersistentClass> persistentClasses)
					throws MappingException {
				getMap().setHasMapKeyProperty( hasMapKeyProperty );
				bindStarToManySecondPass( persistentClasses );
				bindKeyFromAssociationTable(
						getElementType(),
						persistentClasses,
						hasMapKeyProperty,
						mapKeyPropertyName,
						property,
						isEmbedded,
						mapKeyColumns,
						mapKeyManyToManyColumns
				);
				makeOneToManyMapKeyColumnNullableIfNotInProperty( property );
			}
		};
	}

	@Override
	protected boolean mappingDefinedAttributeOverrideOnElement(MemberDetails property) {
		if ( property.hasDirectAnnotationUsage( AttributeOverride.class ) ) {
			return namedMapValue( property.getDirectAnnotationUsage( AttributeOverride.class ) );
		}
		if ( property.hasDirectAnnotationUsage( AttributeOverrides.class ) ) {
			final AttributeOverrides annotations = property.getDirectAnnotationUsage( AttributeOverrides.class );
			for ( AttributeOverride attributeOverride : annotations.value() ) {
				if ( namedMapValue( attributeOverride ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean namedMapValue(AttributeOverride annotation) {
		return annotation.name().startsWith( "value." );
	}

	private void makeOneToManyMapKeyColumnNullableIfNotInProperty(MemberDetails property) {
		final Map map = (Map) this.collection;
		if ( map.isOneToMany() && property.hasDirectAnnotationUsage( MapKeyColumn.class ) ) {
			final Value indexValue = map.getIndex();
			if ( indexValue.getColumnSpan() != 1 ) {
				throw new AssertionFailure( "Map key mapped by @MapKeyColumn does not have 1 column" );
			}
			final Selectable selectable = indexValue.getSelectables().get(0);
			if ( selectable.isFormula() ) {
				throw new AssertionFailure( "Map key mapped by @MapKeyColumn is a Formula" );
			}
			final Column column = (Column) selectable;
			if ( !column.isNullable() ) {
				final PersistentClass persistentClass = ( (OneToMany) map.getElement() ).getAssociatedClass();
				// check if the index column has been mapped by the associated entity to a property;
				// @MapKeyColumn only maps a column to the primary table for the one-to-many, so we only
				// need to check "un-joined" properties.
				if ( !propertiesContainColumn( persistentClass.getUnjoinedProperties(), column ) ) {
					// The index column is not mapped to an associated entity property so we can
					// safely make the index column nullable.
					column.setNullable( true );
				}
			}
		}
	}

	private boolean propertiesContainColumn(List<Property> properties, Column column) {
		for ( Property property : properties ) {
			for ( Selectable selectable: property.getSelectables() ) {
				if ( column.equals( selectable ) ) {
					final Column iteratedColumn = (Column) selectable;
					if ( column.getValue().getTable().equals( iteratedColumn.getValue().getTable() ) ) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private void bindKeyFromAssociationTable(
			TypeDetails elementType,
			java.util.Map<String, PersistentClass> persistentClasses,
			boolean hasMapKeyProperty,
			String mapKeyPropertyName,
			MemberDetails property,
			boolean isEmbedded,
			AnnotatedColumns mapKeyColumns,
			AnnotatedJoinColumns mapKeyManyToManyColumns) {
		if ( hasMapKeyProperty ) {
			//this is an EJB3 @MapKey
			handleMapKeyProperty( elementType, persistentClasses, mapKeyPropertyName );
		}
		else {
			//this is a true Map mapping
			handleMapKey( persistentClasses, property, isEmbedded, mapKeyColumns, mapKeyManyToManyColumns );
		}
	}

	private void handleMapKey(
			java.util.Map<String, PersistentClass> persistentClasses,
			MemberDetails property,
			boolean isEmbedded,
			AnnotatedColumns mapKeyColumns,
			AnnotatedJoinColumns mapKeyManyToManyColumns) {
		final String mapKeyType = getKeyType( property );
		final PersistentClass collectionEntity = persistentClasses.get( mapKeyType );
		final boolean isKeyedByEntities = collectionEntity != null;
		if ( isKeyedByEntities ) {
			final ManyToOne element = handleCollectionKeyedByEntities( mapKeyType );
			handleForeignKey( property, element );
			// a map key column has no unique constraint, so pass 'unique=false' here
			bindManyToManyInverseForeignKey( collectionEntity, mapKeyManyToManyColumns, element, false );
		}
		else {
			final ClassDetails keyClass = mapKeyClass( mapKeyType );
			final TypeDetails keyTypeDetails = new ClassTypeDetailsImpl( keyClass, TypeDetails.Kind.CLASS );
			handleMapKey(
					property,
					mapKeyColumns,
					mapKeyType,
					keyTypeDetails,
					annotatedMapKeyType( property, isEmbedded, mapKeyType, keyTypeDetails ),
					buildCollectionPropertyHolder( property, keyTypeDetails ),
					accessType( property, collection.getOwner() )
			);
		}
		//FIXME pass the Index Entity JoinColumns
		if ( !collection.isOneToMany() ) {
			//index column should not be null
			for ( AnnotatedJoinColumn column : mapKeyManyToManyColumns.getJoinColumns() ) {
				column.forceNotNull();
			}
		}
	}

	private AnnotatedClassType annotatedMapKeyType(
			MemberDetails property,
			boolean isEmbedded,
			String mapKeyType,
			TypeDetails keyTypeDetails) {
		if ( isPrimitive( mapKeyType ) ) {
			return NONE;
		}
		else {
			// force in case of attribute override naming the key
			return isEmbedded || mappingDefinedAttributeOverrideOnMapKey( property )
					? EMBEDDABLE
					: buildingContext.getMetadataCollector().getClassType( keyTypeDetails.determineRawClass() );
		}
	}

	private ClassDetails mapKeyClass(String mapKeyType) {
		return isPrimitive( mapKeyType )
				? null
				: buildingContext.getBootstrapContext()
						.getModelsContext()
						.getClassDetailsRegistry()
						.resolveClassDetails( mapKeyType );
	}

	private static String getKeyType(MemberDetails property) {
		//target has priority over reflection for the map key type
		//JPA 2 has priority
		final MapKeyClass mapKeyClassAnn = property.getDirectAnnotationUsage( MapKeyClass.class );
		final Class<?> target = mapKeyClassAnn != null ? mapKeyClassAnn.value() : void.class;
		return void.class.equals( target ) ? property.getMapKeyType().getName() : target.getName();
	}

	private void handleMapKeyProperty(
			TypeDetails elementType,
			java.util.Map<String, PersistentClass> persistentClasses,
			String mapKeyPropertyName) {
		final PersistentClass associatedClass = persistentClasses.get( elementType.getName() );
		if ( associatedClass == null ) {
			throw new AnnotationException( "Association '" + safeCollectionRole() + "'"
					+ targetEntityMessage( elementType ) );
		}
		final Property mapProperty = findPropertyByName( associatedClass, mapKeyPropertyName );
		if ( mapProperty == null ) {
			throw new AnnotationException( "Map key property '" + mapKeyPropertyName
					+ "' not found in target entity '" + associatedClass.getEntityName() + "'" );
		}
		// HHH-11005 - if InheritanceType.JOINED then need to find class defining the column
		final PersistentClass targetEntity =
				inheritanceStatePerClass.get( elementType.determineRawClass() ).getType() == InheritanceType.JOINED
						? mapProperty.getPersistentClass()
						: associatedClass;
		final Value indexValue =
				createFormulatedValue( mapProperty.getValue(), collection, associatedClass, targetEntity );
		getMap().setIndex( indexValue );
		getMap().setMapKeyPropertyName( mapKeyPropertyName );
	}

	private CollectionPropertyHolder buildCollectionPropertyHolder(
			MemberDetails property,
			TypeDetails keyClass) {
		return buildCollectionPropertyHolder( property, keyClass.determineRawClass() );
	}
	private CollectionPropertyHolder buildCollectionPropertyHolder(
			MemberDetails property,
			ClassDetails keyClass) {
		final CollectionPropertyHolder holder =
				buildPropertyHolder( collection, getPath(), keyClass, property, propertyHolder, buildingContext );
		// 'propertyHolder' is the PropertyHolder for the owner of the collection
		// 'holder' is the CollectionPropertyHolder.
		// 'property' is the collection XProperty
		propertyHolder.startingProperty( property );
		holder.prepare( property, !( collection.getKey().getType() instanceof BasicType ) );
		return holder;
	}

	private String getPath() {
		return qualify( collection.getRole(), "mapkey" );
	}

	private void handleForeignKey(MemberDetails property, ManyToOne element) {
		final ForeignKey foreignKey = getMapKeyForeignKey( property );
		if ( foreignKey != null ) {
			final ConstraintMode constraintMode = foreignKey.value();
			if ( constraintMode == ConstraintMode.NO_CONSTRAINT
					|| constraintMode == ConstraintMode.PROVIDER_DEFAULT
							&& getBuildingContext().getBuildingOptions().isNoConstraintByDefault() ) {
				element.disableForeignKey();
			}
			else {
				element.setForeignKeyName( nullIfEmpty( foreignKey.name() ) );
				element.setForeignKeyDefinition( nullIfEmpty( foreignKey.foreignKeyDefinition() ) );
				element.setForeignKeyOptions( foreignKey.options() );
			}
		}
	}

	//similar to CollectionBinder.handleCollectionOfEntities()
	private ManyToOne handleCollectionKeyedByEntities(
			String mapKeyType) {
		final ManyToOne element = new ManyToOne( buildingContext, collection.getCollectionTable() );
		getMap().setIndex( element );
		element.setReferencedEntityName( mapKeyType );
		//element.setFetchMode( fetchMode );
		//element.setLazy( fetchMode != FetchMode.JOIN );
		//make the second join non-lazy
		element.setFetchMode( FetchMode.JOIN );
		element.setLazy( false );
		//does not make sense for a map key element.setIgnoreNotFound( ignoreNotFound );
		return element;
	}

	private void handleMapKey(
			MemberDetails property,
			AnnotatedColumns mapKeyColumns,
			String mapKeyType,
			TypeDetails keyTypeDetails,
			AnnotatedClassType classType,
			CollectionPropertyHolder holder,
			AccessType accessType) {
		final Class<? extends CompositeUserType<?>> compositeUserType
				= resolveCompositeUserType( property, keyTypeDetails, buildingContext );
		if ( classType == EMBEDDABLE || compositeUserType != null ) {
			handleCompositeMapKey( keyTypeDetails, holder, accessType, compositeUserType );
		}
		else {
			handleMapKey( property, mapKeyColumns, mapKeyType, keyTypeDetails, holder, accessType );
		}
	}

	private void handleMapKey(
			MemberDetails property,
			AnnotatedColumns mapKeyColumns,
			String mapKeyType,
			TypeDetails keyTypeDetails,
			CollectionPropertyHolder holder,
			AccessType accessType) {
		final BasicValueBinder elementBinder = new BasicValueBinder( BasicValueBinder.Kind.MAP_KEY, buildingContext );
		elementBinder.setReturnedClassName(mapKeyType);
		final AnnotatedColumns keyColumns = createElementColumnsIfNecessary(
				collection,
				mapKeyColumns,
				Collection.DEFAULT_KEY_COLUMN_NAME,
				Size.DEFAULT_LENGTH, //TODO: is this really necessary??!!
				buildingContext
		);
		elementBinder.setColumns( keyColumns );
		//do not call setType as it extracts the type from @Type
		//the algorithm generally does not apply for map key anyway
		elementBinder.setType(
				property,
				keyTypeDetails,
				collection.getOwnerEntityName(),
				holder.mapKeyAttributeConverterDescriptor( property, keyTypeDetails )
		);
		elementBinder.setPersistentClassName( propertyHolder.getEntityName() );
		elementBinder.setAccessType( accessType );
		getMap().setIndex( elementBinder.make() );
	}

	private void handleCompositeMapKey(
			TypeDetails keyTypeDetails,
			CollectionPropertyHolder holder,
			AccessType accessType,
			Class<? extends CompositeUserType<?>> compositeUserType) {
		getMap().setIndex( fillEmbeddable(
				holder,
				propertyPreloadedData( keyTypeDetails ),
				accessType,
				//TODO be smart with isNullable
				true,
				new EntityBinder( buildingContext ),
				false,
				false,
				true,
				null,
				compositeUserType,
				null,
				buildingContext,
				inheritanceStatePerClass
		) );
	}

	private PropertyPreloadedData propertyPreloadedData(TypeDetails keyTypeDetails) {
		return isHibernateExtensionMapping()
				? new PropertyPreloadedData( AccessType.PROPERTY, "index", keyTypeDetails )
				// "key" is the JPA 2 prefix for map keys
				: new PropertyPreloadedData( AccessType.PROPERTY, "key", keyTypeDetails );
	}

	private static Class<? extends CompositeUserType<?>> resolveCompositeUserType(
			MemberDetails property,
			TypeDetails returnedClass,
			MetadataBuildingContext context) {
		final MapKeyCompositeType compositeType = property.getDirectAnnotationUsage( MapKeyCompositeType.class );
		if ( compositeType != null ) {
			return compositeType.value();
		}
		else if ( returnedClass != null ) {
			return context.getMetadataCollector()
					.findRegisteredCompositeUserType( returnedClass.determineRawClass().toJavaClass() );
		}
		else {
			return null;
		}
	}

	private jakarta.persistence.ForeignKey getMapKeyForeignKey(MemberDetails property) {
		final MapKeyJoinColumns mapKeyJoinColumns = property.getDirectAnnotationUsage( MapKeyJoinColumns.class );
		if ( mapKeyJoinColumns != null ) {
			return mapKeyJoinColumns.foreignKey();
		}

		final MapKeyJoinColumn mapKeyJoinColumn = property.getDirectAnnotationUsage( MapKeyJoinColumn.class );
		if ( mapKeyJoinColumn != null ) {
			return mapKeyJoinColumn.foreignKey();
		}

		return null;
	}

	private boolean mappingDefinedAttributeOverrideOnMapKey(MemberDetails property) {
		final AttributeOverride overrideAnn = property.getDirectAnnotationUsage( AttributeOverride.class );
		if ( overrideAnn != null ) {
			return namedMapKey( overrideAnn );
		}

		final AttributeOverrides overridesAnn = property.getDirectAnnotationUsage( AttributeOverrides.class );
		if ( overridesAnn != null ) {
			for ( AttributeOverride nestedAnn : overridesAnn.value() ) {
				if ( namedMapKey( nestedAnn ) ) {
					return true;
				}
			}
		}

		return false;
	}

	private static boolean namedMapKey(AttributeOverride annotation) {
		return annotation.name().startsWith( "key." );
	}

	private Value createFormulatedValue(
			Value value,
			Collection collection,
			PersistentClass associatedClass,
			PersistentClass targetPropertyPersistentClass) {
		if ( value instanceof Component component ) {
			return createIndexComponent( collection, associatedClass, component );
		}
		else {
			// HHH-11005 - only if we are @OneToMany and location of map key property is
			// at a different level, need to add a select
			final Table mapKeyTable =
					!associatedClass.equals( targetPropertyPersistentClass )
							? targetPropertyPersistentClass.getTable()
							: associatedClass.getTable();
			if ( value instanceof BasicValue basicValue ) {
				return createDependantBasicValue( mapKeyTable, basicValue );
			}
			else if ( value instanceof SimpleValue simpleValue ) {
				return createTargetValue( mapKeyTable, simpleValue );
			}
			else {
				throw new AssertionFailure( "Unknown type encountered for map key: " + value.getClass() );
			}
		}
	}

	private SimpleValue createTargetValue(Table mapKeyTable, SimpleValue sourceValue) {
		final SimpleValue targetValue;
		if ( sourceValue instanceof ManyToOne sourceManyToOne ) {
			final ManyToOne targetManyToOne = new ManyToOne( getBuildingContext(), mapKeyTable);
			targetManyToOne.setFetchMode( FetchMode.DEFAULT );
			targetManyToOne.setLazy( true );
			//targetValue.setIgnoreNotFound( ); does not make sense for a map key
			targetManyToOne.setReferencedEntityName( sourceManyToOne.getReferencedEntityName() );
			targetValue = targetManyToOne;
		}
		else {
			targetValue = new BasicValue( getBuildingContext(), mapKeyTable);
			targetValue.copyTypeFrom( sourceValue );
		}
		for ( Selectable selectable : sourceValue.getSelectables() ) {
			addSelectable( targetValue, selectable );
		}
		return targetValue;
	}

	private DependantBasicValue createDependantBasicValue(Table mapKeyTable, BasicValue sourceValue) {
		final DependantBasicValue dependantBasicValue = new DependantBasicValue(
				getBuildingContext(),
				mapKeyTable,
				sourceValue,
				false,
				false
		);
		addSelectable( dependantBasicValue, sourceValue.getColumn() );
		return dependantBasicValue;
	}

	private static void addSelectable(SimpleValue targetValue, Selectable selectable) {
		if ( selectable instanceof Column column ) {
			targetValue.addColumn( column.clone(), false, false  );
		}
		else if ( selectable instanceof Formula formula ) {
			targetValue.addFormula( new Formula( formula.getFormula() ) );
		}
	}

	private Component createIndexComponent(Collection collection, PersistentClass associatedClass, Component component) {
		final Component indexComponent = new Component( getBuildingContext(), collection );
		indexComponent.setComponentClassName( component.getComponentClassName() );
		for ( Property property : component.getProperties() ) {
			final Property newProperty = new Property();
			newProperty.setCascade( property.getCascade() );
			newProperty.setValueGeneratorCreator( property.getValueGeneratorCreator() );
			newProperty.setInsertable( false );
			newProperty.setUpdateable( false );
			newProperty.setMetaAttributes( property.getMetaAttributes() );
			newProperty.setName( property.getName() );
			newProperty.setNaturalIdentifier( false );
			//newProperty.setOptimisticLocked( false );
			newProperty.setOptional( false );
			newProperty.setPersistentClass( property.getPersistentClass() );
			newProperty.setPropertyAccessorName( property.getPropertyAccessorName() );
			newProperty.setSelectable( property.isSelectable() );
			newProperty.setValue(
					createFormulatedValue( property.getValue(), collection, associatedClass, associatedClass )
			);
			indexComponent.addProperty( newProperty );
		}
		return indexComponent;
	}
}
