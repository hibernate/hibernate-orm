/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cfg.annotations;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.MapKeyClass;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.annotations.MapKeyType;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.cfg.AccessType;
import org.hibernate.cfg.AnnotatedClassType;
import org.hibernate.cfg.AnnotationBinder;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.CollectionSecondPass;
import org.hibernate.cfg.Ejb3Column;
import org.hibernate.cfg.Ejb3JoinColumn;
import org.hibernate.cfg.Mappings;
import org.hibernate.cfg.PropertyData;
import org.hibernate.cfg.PropertyHolder;
import org.hibernate.cfg.PropertyHolderBuilder;
import org.hibernate.cfg.PropertyPreloadedData;
import org.hibernate.cfg.SecondPass;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.sql.Template;

/**
 * Implementation to bind a Map
 *
 * @author Emmanuel Bernard
 */
public class MapBinder extends CollectionBinder {
	public MapBinder(boolean sorted) {
		super( sorted );
	}

	public MapBinder() {
		super();
	}

	public boolean isMap() {
		return true;
	}

	protected Collection createCollection(PersistentClass persistentClass) {
		return new org.hibernate.mapping.Map( getMappings(), persistentClass );
	}

	@Override
	public SecondPass getSecondPass(
			final Ejb3JoinColumn[] fkJoinColumns,
			final Ejb3JoinColumn[] keyColumns,
			final Ejb3JoinColumn[] inverseColumns,
			final Ejb3Column[] elementColumns,
			final Ejb3Column[] mapKeyColumns,
			final Ejb3JoinColumn[] mapKeyManyToManyColumns,
			final boolean isEmbedded,
			final XProperty property,
			final XClass collType,
			final boolean ignoreNotFound,
			final boolean unique,
			final TableBinder assocTableBinder,
			final Mappings mappings) {
		return new CollectionSecondPass( mappings, MapBinder.this.collection ) {
			public void secondPass(Map persistentClasses, Map inheritedMetas)
					throws MappingException {
				bindStarToManySecondPass(
						persistentClasses, collType, fkJoinColumns, keyColumns, inverseColumns, elementColumns,
						isEmbedded, property, unique, assocTableBinder, ignoreNotFound, mappings
				);
				bindKeyFromAssociationTable(
						collType, persistentClasses, mapKeyPropertyName, property, isEmbedded, mappings,
						mapKeyColumns, mapKeyManyToManyColumns,
						inverseColumns != null ? inverseColumns[0].getPropertyName() : null
				);
			}
		};
	}

	private void bindKeyFromAssociationTable(
			XClass collType,
			Map persistentClasses,
			String mapKeyPropertyName,
			XProperty property,
			boolean isEmbedded,
			Mappings mappings,
			Ejb3Column[] mapKeyColumns,
			Ejb3JoinColumn[] mapKeyManyToManyColumns,
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
			Value indexValue = createFormulatedValue(
					mapProperty.getValue(), map, targetPropertyName, associatedClass, mappings
			);
			map.setIndex( indexValue );
		}
		else {
			//this is a true Map mapping
			//TODO ugly copy/pastle from CollectionBinder.bindManyToManySecondPass
			String mapKeyType;
			Class target = void.class;
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
				element = new ManyToOne( mappings, mapValue.getCollectionTable() );
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
				XClass elementClass;
				AnnotatedClassType classType;
				PropertyHolder holder = null;
				if ( BinderHelper.PRIMITIVE_NAMES.contains( mapKeyType ) ) {
					classType = AnnotatedClassType.NONE;
					elementClass = null;
				}
				else {
					try {
						elementClass = mappings.getReflectionManager().classForName( mapKeyType, MapBinder.class );
					}
					catch (ClassNotFoundException e) {
						throw new AnnotationException( "Unable to find class: " + mapKeyType, e );
					}
					classType = mappings.getClassType( elementClass );

					holder = PropertyHolderBuilder.buildPropertyHolder(
							mapValue,
							StringHelper.qualify( mapValue.getRole(), "mapkey" ),
							elementClass,
							property, propertyHolder, mappings
					);
					//force in case of attribute override
					boolean attributeOverride = property.isAnnotationPresent( AttributeOverride.class )
							|| property.isAnnotationPresent( AttributeOverrides.class );
					if ( isEmbedded || attributeOverride ) {
						classType = AnnotatedClassType.EMBEDDABLE;
					}
				}

				PersistentClass owner = mapValue.getOwner();
				AccessType accessType;
				// FIXME support @Access for collection of elements
				// String accessType = access != null ? access.value() : null;
				if ( owner.getIdentifierProperty() != null ) {
					accessType = owner.getIdentifierProperty().getPropertyAccessorName().equals( "property" ) ? AccessType.PROPERTY
							: AccessType.FIELD;
				}
				else if ( owner.getIdentifierMapper() != null && owner.getIdentifierMapper().getPropertySpan() > 0 ) {
					Property prop = (Property) owner.getIdentifierMapper().getPropertyIterator().next();
					accessType = prop.getPropertyAccessorName().equals( "property" ) ? AccessType.PROPERTY
							: AccessType.FIELD;
				}
				else {
					throw new AssertionFailure( "Unable to guess collection property accessor name" );
				}

				if ( AnnotatedClassType.EMBEDDABLE.equals( classType ) ) {
					EntityBinder entityBinder = new EntityBinder();

					PropertyData inferredData;
					if ( isHibernateExtensionMapping() ) {
						inferredData = new PropertyPreloadedData( AccessType.PROPERTY, "index", elementClass );
					}
					else {
						//"key" is the JPA 2 prefix for map keys
						inferredData = new PropertyPreloadedData( AccessType.PROPERTY, "key", elementClass );
					}

					//TODO be smart with isNullable
					Component component = AnnotationBinder.fillComponent(
							holder, inferredData, accessType, true,
							entityBinder, false, false,
							true, mappings, inheritanceStatePerClass
					);
					mapValue.setIndex( component );
				}
				else {
					SimpleValueBinder elementBinder = new SimpleValueBinder();
					elementBinder.setMappings( mappings );
					elementBinder.setReturnedClassName( mapKeyType );

					Ejb3Column[] elementColumns = mapKeyColumns;
					if ( elementColumns == null || elementColumns.length == 0 ) {
						elementColumns = new Ejb3Column[1];
						Ejb3Column column = new Ejb3Column();
						column.setImplicit( false );
						column.setNullable( true );
						column.setLength( Ejb3Column.DEFAULT_COLUMN_LENGTH );
						column.setLogicalColumnName( Collection.DEFAULT_KEY_COLUMN_NAME );
						//TODO create an EMPTY_JOINS collection
						column.setJoins( new HashMap<String, Join>() );
						column.setMappings( mappings );
						column.bind();
						elementColumns[0] = column;
					}
					//override the table
					for (Ejb3Column column : elementColumns) {
						column.setTable( mapValue.getCollectionTable() );
					}
					elementBinder.setColumns( elementColumns );
					//do not call setType as it extract the type from @Type
					//the algorithm generally does not apply for map key anyway
					elementBinder.setKey(true);
					MapKeyType mapKeyTypeAnnotation = property.getAnnotation( MapKeyType.class );
					if ( mapKeyTypeAnnotation != null && !BinderHelper.isEmptyAnnotationValue(
							mapKeyTypeAnnotation.value()
									.type()
					) ) {
						elementBinder.setExplicitType( mapKeyTypeAnnotation.value() );
					}
					else {
						elementBinder.setType( property, elementClass, this.collection.getOwnerEntityName() );
					}
					elementBinder.setPersistentClassName( propertyHolder.getEntityName() );
					elementBinder.setAccessType( accessType );
					mapValue.setIndex( elementBinder.make() );
				}
			}
			//FIXME pass the Index Entity JoinColumns
			if ( !collection.isOneToMany() ) {
				//index column shoud not be null
				for (Ejb3JoinColumn col : mapKeyManyToManyColumns) {
					col.forceNotNull();
				}
			}
			if ( isIndexOfEntities ) {
				bindManytoManyInverseFk(
						collectionEntity,
						mapKeyManyToManyColumns,
						element,
						false, //a map key column has no unique constraint
						mappings
				);
			}
		}
	}

	protected Value createFormulatedValue(
			Value value,
			Collection collection,
			String targetPropertyName,
			PersistentClass associatedClass,
			Mappings mappings) {
		Value element = collection.getElement();
		String fromAndWhere = null;
		if ( !( element instanceof OneToMany ) ) {
			String referencedPropertyName = null;
			if ( element instanceof ToOne ) {
				referencedPropertyName = ( (ToOne) element ).getReferencedPropertyName();
			}
			else if ( element instanceof DependantValue ) {
				//TODO this never happen I think
				if ( propertyName != null ) {
					referencedPropertyName = collection.getReferencedPropertyName();
				}
				else {
					throw new AnnotationException( "SecondaryTable JoinColumn cannot reference a non primary key" );
				}
			}
			Iterator referencedEntityColumns;
			if ( referencedPropertyName == null ) {
				referencedEntityColumns = associatedClass.getIdentifier().getColumnIterator();
			}
			else {
				Property referencedProperty = associatedClass.getRecursiveProperty( referencedPropertyName );
				referencedEntityColumns = referencedProperty.getColumnIterator();
			}
			String alias = "$alias$";
			StringBuilder fromAndWhereSb = new StringBuilder( " from " )
					.append( associatedClass.getTable().getName() )
							//.append(" as ") //Oracle doesn't support it in subqueries
					.append( " " )
					.append( alias ).append( " where " );
			Iterator collectionTableColumns = element.getColumnIterator();
			while ( collectionTableColumns.hasNext() ) {
				Column colColumn = (Column) collectionTableColumns.next();
				Column refColumn = (Column) referencedEntityColumns.next();
				fromAndWhereSb.append( alias ).append( '.' ).append( refColumn.getQuotedName() )
						.append( '=' ).append( colColumn.getQuotedName() ).append( " and " );
			}
			fromAndWhere = fromAndWhereSb.substring( 0, fromAndWhereSb.length() - 5 );
		}

		if ( value instanceof Component ) {
			Component component = (Component) value;
			Iterator properties = component.getPropertyIterator();
			Component indexComponent = new Component( mappings, collection );
			indexComponent.setComponentClassName( component.getComponentClassName() );
			//TODO I don't know if this is appropriate
			indexComponent.setNodeName( "index" );
			while ( properties.hasNext() ) {
				Property current = (Property) properties.next();
				Property newProperty = new Property();
				newProperty.setCascade( current.getCascade() );
				newProperty.setGeneration( current.getGeneration() );
				newProperty.setInsertable( false );
				newProperty.setUpdateable( false );
				newProperty.setMetaAttributes( current.getMetaAttributes() );
				newProperty.setName( current.getName() );
				newProperty.setNodeName( current.getNodeName() );
				newProperty.setNaturalIdentifier( false );
				//newProperty.setOptimisticLocked( false );
				newProperty.setOptional( false );
				newProperty.setPersistentClass( current.getPersistentClass() );
				newProperty.setPropertyAccessorName( current.getPropertyAccessorName() );
				newProperty.setSelectable( current.isSelectable() );
				newProperty.setValue(
						createFormulatedValue(
								current.getValue(), collection, targetPropertyName, associatedClass, mappings
						)
				);
				indexComponent.addProperty( newProperty );
			}
			return indexComponent;
		}
		else if ( value instanceof SimpleValue ) {
			SimpleValue sourceValue = (SimpleValue) value;
			SimpleValue targetValue;
			if ( value instanceof ManyToOne ) {
				ManyToOne sourceManyToOne = (ManyToOne) sourceValue;
				ManyToOne targetManyToOne = new ManyToOne( mappings, collection.getCollectionTable() );
				targetManyToOne.setFetchMode( FetchMode.DEFAULT );
				targetManyToOne.setLazy( true );
				//targetValue.setIgnoreNotFound( ); does not make sense for a map key
				targetManyToOne.setReferencedEntityName( sourceManyToOne.getReferencedEntityName() );
				targetValue = targetManyToOne;
			}
			else {
				targetValue = new SimpleValue( mappings, collection.getCollectionTable() );
				targetValue.setTypeName( sourceValue.getTypeName() );
				targetValue.setTypeParameters( sourceValue.getTypeParameters() );
			}
			Iterator columns = sourceValue.getColumnIterator();
			Random random = new Random();
			while ( columns.hasNext() ) {
				Object current = columns.next();
				Formula formula = new Formula();
				String formulaString;
				if ( current instanceof Column ) {
					formulaString = ( (Column) current ).getQuotedName();
				}
				else if ( current instanceof Formula ) {
					formulaString = ( (Formula) current ).getFormula();
				}
				else {
					throw new AssertionFailure( "Unknown element in column iterator: " + current.getClass() );
				}
				if ( fromAndWhere != null ) {
					formulaString = Template.renderWhereStringTemplate( formulaString, "$alias$", new HSQLDialect() );
					formulaString = "(select " + formulaString + fromAndWhere + ")";
					formulaString = StringHelper.replace(
							formulaString,
							"$alias$",
							"a" + random.nextInt( 16 )
					);
				}
				formula.setFormula( formulaString );
				targetValue.addFormula( formula );

			}
			return targetValue;
		}
		else {
			throw new AssertionFailure( "Unknown type encounters for map key: " + value.getClass() );
		}
	}
}
