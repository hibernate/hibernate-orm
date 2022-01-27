/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AccessType;
import org.hibernate.cfg.AnnotatedClassType;
import org.hibernate.cfg.AnnotationBinder;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.CollectionPropertyHolder;
import org.hibernate.cfg.CollectionSecondPass;
import org.hibernate.cfg.AnnotatedColumn;
import org.hibernate.cfg.AnnotatedJoinColumn;
import org.hibernate.cfg.InheritanceState;
import org.hibernate.cfg.PropertyData;
import org.hibernate.cfg.PropertyHolderBuilder;
import org.hibernate.cfg.PropertyPreloadedData;
import org.hibernate.cfg.SecondPass;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantBasicValue;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.usertype.UserCollectionType;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.MapKeyJoinColumns;

/**
 * Implementation to bind a Map
 *
 * @author Emmanuel Bernard
 */
public class MapBinder extends CollectionBinder {
	public MapBinder(Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver, boolean sorted, MetadataBuildingContext buildingContext) {
		super( customTypeBeanResolver, sorted, buildingContext );
	}

	public boolean isMap() {
		return true;
	}

	protected Collection createCollection(PersistentClass owner) {
		return new org.hibernate.mapping.Map( getCustomTypeBeanResolver(), owner, getBuildingContext() );
	}

	@Override
	public SecondPass getSecondPass(
			final AnnotatedJoinColumn[] fkJoinColumns,
			final AnnotatedJoinColumn[] keyColumns,
			final AnnotatedJoinColumn[] inverseColumns,
			final AnnotatedColumn[] elementColumns,
			final AnnotatedColumn[] mapKeyColumns,
			final AnnotatedJoinColumn[] mapKeyManyToManyColumns,
			final boolean isEmbedded,
			final XProperty property,
			final XClass collType,
			final boolean ignoreNotFound,
			final boolean unique,
			final TableBinder assocTableBinder,
			final MetadataBuildingContext buildingContext) {
		return new CollectionSecondPass( buildingContext, MapBinder.this.collection ) {
			public void secondPass(Map persistentClasses, Map inheritedMetas)
					throws MappingException {
				bindStarToManySecondPass(
						persistentClasses, collType, fkJoinColumns, keyColumns, inverseColumns, elementColumns,
						isEmbedded, property, unique, assocTableBinder, ignoreNotFound, buildingContext
				);
				bindKeyFromAssociationTable(
						collType, persistentClasses, mapKeyPropertyName, property, isEmbedded, buildingContext,
						mapKeyColumns, mapKeyManyToManyColumns,
						inverseColumns != null ? inverseColumns[0].getPropertyName() : null
				);
				makeOneToManyMapKeyColumnNullableIfNotInProperty( property );
			}
		};
	}

	private void makeOneToManyMapKeyColumnNullableIfNotInProperty(
			final XProperty property) {
		final org.hibernate.mapping.Map map = (org.hibernate.mapping.Map) this.collection;
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
			Column column = (Column) selectable;
			if ( !column.isNullable() ) {
				final PersistentClass persistentClass = ( ( OneToMany ) map.getElement() ).getAssociatedClass();
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
			XClass collType,
			Map persistentClasses,
			String mapKeyPropertyName,
			XProperty property,
			boolean isEmbedded,
			MetadataBuildingContext buildingContext,
			AnnotatedColumn[] mapKeyColumns,
			AnnotatedJoinColumn[] mapKeyManyToManyColumns,
			String targetPropertyName) {
		if ( mapKeyPropertyName != null ) {
			//this is an EJB3 @MapKey
			PersistentClass associatedClass = (PersistentClass) persistentClasses.get( collType.getName() );
			if ( associatedClass == null ) throw new AnnotationException( "Associated class not found: " + collType );
			Property mapProperty = BinderHelper.findPropertyByName( associatedClass, mapKeyPropertyName );
			if ( mapProperty == null ) {
				throw new AnnotationException(
						"Map key property not found: " + collType + "." + mapKeyPropertyName
				);
			}
			org.hibernate.mapping.Map map = (org.hibernate.mapping.Map) this.collection;
			// HHH-11005 - if InheritanceType.JOINED then need to find class defining the column
			InheritanceState inheritanceState = inheritanceStatePerClass.get( collType );
			PersistentClass targetPropertyPersistentClass = InheritanceType.JOINED.equals( inheritanceState.getType() ) ?
					mapProperty.getPersistentClass() :
					associatedClass;
			Value indexValue = createFormulatedValue(
					mapProperty.getValue(), map, targetPropertyName, associatedClass, targetPropertyPersistentClass, buildingContext
			);
			map.setIndex( indexValue );
			map.setMapKeyPropertyName( mapKeyPropertyName );
		}
		else {
			//this is a true Map mapping
			//TODO ugly copy/paste from CollectionBinder.bindManyToManySecondPass
			String mapKeyType;
			Class<?> target = void.class;
			/*
			 * target has priority over reflection for the map key type
			 * JPA 2 has priority
			 */
			if ( property.isAnnotationPresent( MapKeyClass.class ) ) {
				target = property.getAnnotation( MapKeyClass.class ).value();
			}
			if ( !void.class.equals( target ) ) {
				mapKeyType = target.getName();
			}
			else {
				mapKeyType = property.getMapKey().getName();
			}
			PersistentClass collectionEntity = (PersistentClass) persistentClasses.get( mapKeyType );
			boolean isIndexOfEntities = collectionEntity != null;
			ManyToOne element = null;
			org.hibernate.mapping.Map mapValue = (org.hibernate.mapping.Map) this.collection;
			if ( isIndexOfEntities ) {
				element = new ManyToOne( buildingContext, mapValue.getCollectionTable() );
				mapValue.setIndex( element );
				element.setReferencedEntityName( mapKeyType );
				//element.setFetchMode( fetchMode );
				//element.setLazy( fetchMode != FetchMode.JOIN );
				//make the second join non lazy
				element.setFetchMode( FetchMode.JOIN );
				element.setLazy( false );
				//does not make sense for a map key element.setIgnoreNotFound( ignoreNotFound );
			}
			else {
				final XClass keyXClass;
				AnnotatedClassType classType;
				if ( BinderHelper.PRIMITIVE_NAMES.contains( mapKeyType ) ) {
					classType = AnnotatedClassType.NONE;
					keyXClass = null;
				}
				else {
					final BootstrapContext bootstrapContext = buildingContext.getBootstrapContext();
					final Class<Object> mapKeyClass = bootstrapContext.getClassLoaderAccess().classForName( mapKeyType );
					keyXClass = bootstrapContext.getReflectionManager().toXClass( mapKeyClass );

					classType = buildingContext.getMetadataCollector().getClassType( keyXClass );
					// force in case of attribute override naming the key
					if ( isEmbedded || mappingDefinedAttributeOverrideOnMapKey( property ) ) {
						classType = AnnotatedClassType.EMBEDDABLE;
					}
				}

				CollectionPropertyHolder holder = PropertyHolderBuilder.buildPropertyHolder(
						mapValue,
						StringHelper.qualify( mapValue.getRole(), "mapkey" ),
						keyXClass,
						property,
						propertyHolder,
						buildingContext
				);


				// 'propertyHolder' is the PropertyHolder for the owner of the collection
				// 'holder' is the CollectionPropertyHolder.
				// 'property' is the collection XProperty
				propertyHolder.startingProperty( property );
				holder.prepare( property );

				PersistentClass owner = mapValue.getOwner();
				AccessType accessType;
				// FIXME support @Access for collection of elements
				// String accessType = access != null ? access.value() : null;
				if ( owner.getIdentifierProperty() != null ) {
					accessType = owner.getIdentifierProperty().getPropertyAccessorName().equals( "property" )
							? AccessType.PROPERTY
							: AccessType.FIELD;
				}
				else if ( owner.getIdentifierMapper() != null && owner.getIdentifierMapper().getPropertySpan() > 0 ) {
					Property prop = owner.getIdentifierMapper().getProperties().get(0);
					accessType = prop.getPropertyAccessorName().equals( "property" )
							? AccessType.PROPERTY
							: AccessType.FIELD;
				}
				else {
					throw new AssertionFailure( "Unable to guess collection property accessor name" );
				}

				if ( AnnotatedClassType.EMBEDDABLE.equals( classType ) ) {
					EntityBinder entityBinder = new EntityBinder();

					PropertyData inferredData;
					if ( isHibernateExtensionMapping() ) {
						inferredData = new PropertyPreloadedData( AccessType.PROPERTY, "index", keyXClass );
					}
					else {
						//"key" is the JPA 2 prefix for map keys
						inferredData = new PropertyPreloadedData( AccessType.PROPERTY, "key", keyXClass );
					}

					//TODO be smart with isNullable
					Component component = AnnotationBinder.fillComponent(
							holder,
							inferredData,
							accessType,
							true,
							entityBinder,
							false,
							false,
							true,
							null,
							buildingContext,
							inheritanceStatePerClass
					);
					mapValue.setIndex( component );
				}
				else {
					final BasicValueBinder<?> elementBinder = new BasicValueBinder<>( BasicValueBinder.Kind.MAP_KEY, buildingContext );
					elementBinder.setReturnedClassName( mapKeyType );

					AnnotatedColumn[] elementColumns = mapKeyColumns;
					if ( elementColumns == null || elementColumns.length == 0 ) {
						elementColumns = new AnnotatedColumn[1];
						AnnotatedColumn column = new AnnotatedColumn();
						column.setImplicit( false );
						column.setNullable( true );
						column.setLength( Size.DEFAULT_LENGTH );
						column.setLogicalColumnName( Collection.DEFAULT_KEY_COLUMN_NAME );
						//TODO create an EMPTY_JOINS collection
						column.setJoins( new HashMap<>() );
						column.setBuildingContext( buildingContext );
						column.bind();
						elementColumns[0] = column;
					}
					//override the table
					for (AnnotatedColumn column : elementColumns) {
						column.setTable( mapValue.getCollectionTable() );
					}
					elementBinder.setColumns( elementColumns );
					//do not call setType as it extracts the type from @Type
					//the algorithm generally does not apply for map key anyway
					elementBinder.setType(
							property,
							keyXClass,
							this.collection.getOwnerEntityName(),
							holder.mapKeyAttributeConverterDescriptor( property, keyXClass )
					);
					elementBinder.setPersistentClassName( propertyHolder.getEntityName() );
					elementBinder.setAccessType( accessType );
					mapValue.setIndex( elementBinder.make() );
				}
			}
			//FIXME pass the Index Entity JoinColumns
			if ( !collection.isOneToMany() ) {
				//index column should not be null
				for (AnnotatedJoinColumn col : mapKeyManyToManyColumns) {
					col.forceNotNull();
				}
			}

			if ( element != null ) {
				final jakarta.persistence.ForeignKey foreignKey = getMapKeyForeignKey( property );
				if ( foreignKey != null ) {
					if ( foreignKey.value() == ConstraintMode.NO_CONSTRAINT
							|| foreignKey.value() == ConstraintMode.PROVIDER_DEFAULT && getBuildingContext().getBuildingOptions().isNoConstraintByDefault() ) {
						element.disableForeignKey();
					}
					else {
						element.setForeignKeyName( StringHelper.nullIfEmpty( foreignKey.name() ) );
						element.setForeignKeyDefinition( StringHelper.nullIfEmpty( foreignKey.foreignKeyDefinition() ) );
					}
				}
			}

			if ( isIndexOfEntities ) {
				bindManytoManyInverseFk(
						collectionEntity,
						mapKeyManyToManyColumns,
						element,
						false, //a map key column has no unique constraint
						buildingContext
				);
			}
		}
	}

	private jakarta.persistence.ForeignKey getMapKeyForeignKey(XProperty property) {
		final MapKeyJoinColumns mapKeyJoinColumns = property.getAnnotation( MapKeyJoinColumns.class );
		if ( mapKeyJoinColumns != null ) {
			return mapKeyJoinColumns.foreignKey();
		}
		else {
			final MapKeyJoinColumn mapKeyJoinColumn = property.getAnnotation( MapKeyJoinColumn.class );
			if ( mapKeyJoinColumn != null ) {
				return mapKeyJoinColumn.foreignKey();
			}
		}
		return null;
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

	protected Value createFormulatedValue(
			Value value,
			Collection collection,
			String targetPropertyName,
			PersistentClass associatedClass,
			PersistentClass targetPropertyPersistentClass,
			MetadataBuildingContext buildingContext) {
		final Table mapKeyTable;
		// HHH-11005 - only if we are OneToMany and location of map key property is at a different level, need to add a select
		if ( !associatedClass.equals( targetPropertyPersistentClass ) ) {
			mapKeyTable = targetPropertyPersistentClass.getTable();
		}
		else {
			mapKeyTable = associatedClass.getTable();
		}

		if ( value instanceof Component ) {
			Component component = (Component) value;
			Component indexComponent = new Component( getBuildingContext(), collection );
			indexComponent.setComponentClassName( component.getComponentClassName() );
			for ( Property current : component.getProperties() ) {
				Property newProperty = new Property();
				newProperty.setCascade( current.getCascade() );
				newProperty.setValueGenerationStrategy( current.getValueGenerationStrategy() );
				newProperty.setInsertable( false );
				newProperty.setUpdateable( false );
				newProperty.setMetaAttributes( current.getMetaAttributes() );
				newProperty.setName( current.getName() );
				newProperty.setNaturalIdentifier( false );
				//newProperty.setOptimisticLocked( false );
				newProperty.setOptional( false );
				newProperty.setPersistentClass( current.getPersistentClass() );
				newProperty.setPropertyAccessorName( current.getPropertyAccessorName() );
				newProperty.setSelectable( current.isSelectable() );
				newProperty.setValue(
						createFormulatedValue(
								current.getValue(), collection, targetPropertyName, associatedClass, associatedClass, buildingContext
						)
				);
				indexComponent.addProperty( newProperty );
			}
			return indexComponent;
		}
		else if ( value instanceof BasicValue ) {
			final BasicValue sourceValue = (BasicValue) value;
			final DependantBasicValue dependantBasicValue = new DependantBasicValue(
					getBuildingContext(),
					mapKeyTable,
					sourceValue,
					false,
					false
			);

			final Selectable sourceValueColumn = sourceValue.getColumn();
			if ( sourceValueColumn instanceof Column ) {
				dependantBasicValue.addColumn( ( (Column) sourceValueColumn ).clone() );
			}
			else if ( sourceValueColumn instanceof Formula ) {
				dependantBasicValue.addFormula( new Formula( ( (Formula) sourceValueColumn ).getFormula() ) );
			}
			else {
				throw new AssertionFailure( "Unknown element column type : " + sourceValueColumn.getClass() );
			}

			return dependantBasicValue;
		}
		else if ( value instanceof SimpleValue ) {
			SimpleValue sourceValue = (SimpleValue) value;
			SimpleValue targetValue;
			if ( value instanceof ManyToOne ) {
				ManyToOne sourceManyToOne = (ManyToOne) sourceValue;
				ManyToOne targetManyToOne = new ManyToOne( getBuildingContext(), mapKeyTable );
				targetManyToOne.setFetchMode( FetchMode.DEFAULT );
				targetManyToOne.setLazy( true );
				//targetValue.setIgnoreNotFound( ); does not make sense for a map key
				targetManyToOne.setReferencedEntityName( sourceManyToOne.getReferencedEntityName() );
				targetValue = targetManyToOne;
			}
			else {
				targetValue = new BasicValue( getBuildingContext(), mapKeyTable );
				targetValue.copyTypeFrom( sourceValue );
			}
			for ( Selectable current : sourceValue.getSelectables() ) {
				if ( current instanceof Column ) {
					targetValue.addColumn( ( (Column) current ).clone() );
				}
				else if ( current instanceof Formula ) {
					targetValue.addFormula( new Formula( ( (Formula) current ).getFormula() ) );
				}
				else {
					throw new AssertionFailure( "Unknown element in column iterator: " + current.getClass() );
				}
			}
			return targetValue;
		}
		else {
			throw new AssertionFailure( "Unknown type encountered for map key: " + value.getClass() );
		}
	}
}
