/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.annotations.MapKeyCompositeType;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.boot.spi.BootstrapContext;
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
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.BasicType;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserCollectionType;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.ConstraintMode;
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

	public boolean isMap() {
		return true;
	}

	private Map getMap() {
		return (Map) collection;
	}

	protected Collection createCollection(PersistentClass owner) {
		return new Map( getCustomTypeBeanResolver(), owner, getBuildingContext() );
	}

	@Override
	SecondPass getSecondPass() {
		return new CollectionSecondPass( MapBinder.this.collection ) {
			public void secondPass(java.util.Map<String, PersistentClass> persistentClasses)
					throws MappingException {
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

	private void makeOneToManyMapKeyColumnNullableIfNotInProperty(
			final XProperty property) {
		final Map map = (Map) this.collection;
		if ( map.isOneToMany() &&
				property.isAnnotationPresent( MapKeyColumn.class ) ) {
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
			XClass elementType,
			java.util.Map<String, PersistentClass> persistentClasses,
			boolean hasMapKeyProperty,
			String mapKeyPropertyName,
			XProperty property,
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
			XProperty property,
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
			final XClass keyClass = mapKeyClass( mapKeyType );
			handleMapKey(
					property,
					mapKeyColumns,
					mapKeyType,
					keyClass,
					annotatedMapKeyType( property, isEmbedded, mapKeyType, keyClass ),
					buildCollectionPropertyHolder( property, keyClass ),
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
			XProperty property,
			boolean isEmbedded,
			String mapKeyType,
			XClass keyClass) {
		if ( isPrimitive( mapKeyType ) ) {
			return NONE;
		}
		else {
			// force in case of attribute override naming the key
			return isEmbedded || mappingDefinedAttributeOverrideOnMapKey( property )
					? EMBEDDABLE
					: buildingContext.getMetadataCollector().getClassType( keyClass );
		}
	}

	private XClass mapKeyClass(String mapKeyType) {
		if ( isPrimitive( mapKeyType ) ) {
			return null;
		}
		else {
			final BootstrapContext bootstrapContext = buildingContext.getBootstrapContext();
			final Class<Object> mapKeyClass = bootstrapContext.getClassLoaderAccess().classForName( mapKeyType );
			return bootstrapContext.getReflectionManager().toXClass( mapKeyClass );
		}
	}

	private static String getKeyType(XProperty property) {
		//target has priority over reflection for the map key type
		//JPA 2 has priority
		final Class<?> target = property.isAnnotationPresent( MapKeyClass.class )
				? property.getAnnotation( MapKeyClass.class ).value()
				: void.class;
		return void.class.equals( target ) ? property.getMapKey().getName() : target.getName();
	}

	private void handleMapKeyProperty(
			XClass elementType,
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
		final InheritanceState inheritanceState = inheritanceStatePerClass.get( elementType );
		final PersistentClass targetEntity = InheritanceType.JOINED == inheritanceState.getType()
				? mapProperty.getPersistentClass()
				: associatedClass;
		final Value indexValue = createFormulatedValue( mapProperty.getValue(), collection, associatedClass, targetEntity );
		getMap().setIndex( indexValue );
		getMap().setMapKeyPropertyName( mapKeyPropertyName );
	}

	private CollectionPropertyHolder buildCollectionPropertyHolder(
			XProperty property,
			XClass keyClass) {
		final CollectionPropertyHolder holder = buildPropertyHolder(
				collection,
				qualify( collection.getRole(), "mapkey" ),
				keyClass,
				property,
				propertyHolder,
				buildingContext
		);
		// 'propertyHolder' is the PropertyHolder for the owner of the collection
		// 'holder' is the CollectionPropertyHolder.
		// 'property' is the collection XProperty
		propertyHolder.startingProperty( property );
		holder.prepare( property, !( collection.getKey().getType() instanceof BasicType ) );
		return holder;
	}

	private void handleForeignKey(XProperty property, ManyToOne element) {
		final jakarta.persistence.ForeignKey foreignKey = getMapKeyForeignKey( property );
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
			XProperty property,
			AnnotatedColumns mapKeyColumns,
			String mapKeyType,
			XClass keyClass,
			AnnotatedClassType classType,
			CollectionPropertyHolder holder,
			AccessType accessType) {
		final Class<? extends CompositeUserType<?>> compositeUserType =
				resolveCompositeUserType( property, keyClass, buildingContext );
		if ( classType == EMBEDDABLE || compositeUserType != null ) {
			handleCompositeMapKey( keyClass, holder, accessType, compositeUserType );
		}
		else {
			handleMapKey( property, mapKeyColumns, mapKeyType, keyClass, holder, accessType );
		}
	}

	private void handleMapKey(
			XProperty property,
			AnnotatedColumns mapKeyColumns,
			String mapKeyType,
			XClass keyClass,
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
				keyClass,
				collection.getOwnerEntityName(),
				holder.mapKeyAttributeConverterDescriptor( property, keyClass )
		);
		elementBinder.setPersistentClassName( propertyHolder.getEntityName() );
		elementBinder.setAccessType( accessType );
		getMap().setIndex( elementBinder.make() );
	}

	private void handleCompositeMapKey(
			XClass keyClass,
			CollectionPropertyHolder holder,
			AccessType accessType,
			Class<? extends CompositeUserType<?>> compositeUserType) {
		getMap().setIndex( fillEmbeddable(
				holder,
				propertyPreloadedData( keyClass ),
				accessType,
				//TODO be smart with isNullable
				true,
				new EntityBinder(),
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

	private PropertyPreloadedData propertyPreloadedData(XClass keyClass) {
		return isHibernateExtensionMapping()
				? new PropertyPreloadedData( AccessType.PROPERTY, "index", keyClass )
				// "key" is the JPA 2 prefix for map keys
				: new PropertyPreloadedData( AccessType.PROPERTY, "key", keyClass );
	}

	private static Class<? extends CompositeUserType<?>> resolveCompositeUserType(
			XProperty property,
			XClass returnedClass,
			MetadataBuildingContext context) {
		final MapKeyCompositeType compositeType = property.getAnnotation( MapKeyCompositeType.class );
		if ( compositeType != null ) {
			return compositeType.value();
		}

		if ( returnedClass != null ) {
			final Class<?> embeddableClass = context.getBootstrapContext()
					.getReflectionManager()
					.toClass( returnedClass );
			if ( embeddableClass != null ) {
				return context.getMetadataCollector().findRegisteredCompositeUserType( embeddableClass );
			}
		}

		return null;
	}

	private jakarta.persistence.ForeignKey getMapKeyForeignKey(XProperty property) {
		final MapKeyJoinColumns mapKeyJoinColumns = property.getAnnotation( MapKeyJoinColumns.class );
		final MapKeyJoinColumn mapKeyJoinColumn = property.getAnnotation( MapKeyJoinColumn.class );
		if ( mapKeyJoinColumns != null ) {
			return mapKeyJoinColumns.foreignKey();
		}
		else if ( mapKeyJoinColumn != null ) {
			return mapKeyJoinColumn.foreignKey();
		}
		else {
			return null;
		}
	}

	private boolean mappingDefinedAttributeOverrideOnMapKey(XProperty property) {
		if ( property.isAnnotationPresent( AttributeOverride.class ) ) {
			return namedMapKey( property.getAnnotation( AttributeOverride.class ) );
		}
		if ( property.isAnnotationPresent( AttributeOverrides.class ) ) {
			final AttributeOverrides annotations = property.getAnnotation( AttributeOverrides.class );
			for ( AttributeOverride attributeOverride : annotations.value() ) {
				if ( namedMapKey( attributeOverride ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean namedMapKey(AttributeOverride annotation) {
		return annotation.name().startsWith( "key." );
	}

	private Value createFormulatedValue(
			Value value,
			Collection collection,
			PersistentClass associatedClass,
			PersistentClass targetPropertyPersistentClass) {
		if ( value instanceof Component ) {
			return createIndexComponent( collection, associatedClass, (Component) value );
		}
		else {
			// HHH-11005 - only if we are @OneToMany and location of map key property is
			// at a different level, need to add a select
			final Table mapKeyTable = !associatedClass.equals( targetPropertyPersistentClass )
					? targetPropertyPersistentClass.getTable()
					: associatedClass.getTable();
			if ( value instanceof BasicValue ) {
				return createDependantBasicValue( mapKeyTable, (BasicValue) value );
			}
			else if ( value instanceof SimpleValue ) {
				return createTargetValue( mapKeyTable, (SimpleValue) value );
			}
			else {
				throw new AssertionFailure( "Unknown type encountered for map key: " + value.getClass() );
			}
		}
	}

	private SimpleValue createTargetValue(Table mapKeyTable, SimpleValue sourceValue) {
		final SimpleValue targetValue;
		if ( sourceValue instanceof ManyToOne ) {
			final ManyToOne sourceManyToOne = (ManyToOne) sourceValue;
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
		if ( selectable instanceof Column ) {
			targetValue.addColumn( ( (Column) selectable).clone(), false, false  );
		}
		else if ( selectable instanceof Formula ) {
			targetValue.addFormula( new Formula( ( (Formula) selectable).getFormula() ) );
		}
		else {
			throw new AssertionFailure( "Unknown element in column iterator: " + selectable.getClass() );
		}
	}

	private Component createIndexComponent(Collection collection, PersistentClass associatedClass, Component component) {
		final Component indexComponent = new Component( getBuildingContext(), collection);
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
					createFormulatedValue( property.getValue(), collection, associatedClass, associatedClass)
			);
			indexComponent.addProperty( newProperty );
		}
		return indexComponent;
	}
}
