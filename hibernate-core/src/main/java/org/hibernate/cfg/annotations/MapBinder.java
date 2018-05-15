/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.ConstraintMode;
import javax.persistence.InheritanceType;
import javax.persistence.MapKeyClass;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.MapKeyJoinColumns;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.ClassLoadingException;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AccessType;
import org.hibernate.cfg.AnnotatedClassType;
import org.hibernate.cfg.AnnotationBinder;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.CollectionPropertyHolder;
import org.hibernate.cfg.CollectionSecondPass;
import org.hibernate.cfg.Ejb3Column;
import org.hibernate.cfg.Ejb3JoinColumn;
import org.hibernate.cfg.InheritanceState;
import org.hibernate.cfg.PropertyData;
import org.hibernate.cfg.PropertyHolderBuilder;
import org.hibernate.cfg.PropertyPreloadedData;
import org.hibernate.cfg.SecondPass;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.model.relational.spi.Size;
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

	public boolean isMap() {
		return true;
	}

	protected Collection createCollection(PersistentClass persistentClass) {
		return new org.hibernate.mapping.Map( getBuildingContext(), persistentClass );
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
			final Value<?> indexValue = map.getIndex();
			if ( indexValue.getColumnSpan() != 1 ) {
				throw new AssertionFailure( "Map key mapped by @MapKeyColumn does not have 1 column" );
			}

			final MappedColumn mappedColumn = indexValue.getMappedColumns().get( 0 );
			if ( mappedColumn.isFormula() ) {
				throw new AssertionFailure( "Map key mapped by @MapKeyColumn is a Formula" );
			}

			final Column column = (Column) map.getIndex().getMappedColumns().get( 0 );
			if ( !column.isNullable() ) {
				final PersistentClass persistentClass = ( ( OneToMany ) map.getElement() ).getAssociatedClass();
				// check if the index column has been mapped by the associated entity to a property;
				// @MapKeyColumn only maps a column to the primary table for the one-to-many, so we only
				// need to check "un-joined" properties.
				if ( !propertyIteratorContainsColumn( persistentClass.getUnjoinedPropertyIterator(), column ) ) {
					// The index column is not mapped to an associated entity property so we can
					// safely make the index column nullable.
					column.setNullable( true );
				}
			}
		}
	}

	private boolean propertyIteratorContainsColumn(Iterator propertyIterator, Column column) {
		while ( propertyIterator.hasNext() ) {
			final Property property = (Property) propertyIterator.next();

			for ( MappedColumn mappedColumn : property.getMappedColumns() ) {
				if ( column.equals( mappedColumn ) ) {
					final Column iteratedColumn = (Column) mappedColumn;
					if ( column.getTableName().equals( iteratedColumn.getTableName() ) ) {
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
			// HHH-11005 - if InheritanceType.JOINED then need to find class defining the column
			InheritanceState inheritanceState = inheritanceStatePerClass.get( collType );
			PersistentClass targetPropertyPersistentClass = InheritanceType.JOINED.equals( inheritanceState.getType() ) ?
					mapProperty.getPersistentClass() :
					associatedClass;
			Value indexValue = createFormulatedValue(
					mapProperty.getValue(),
					map,
					associatedClass,
					targetPropertyPersistentClass
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
				element = new ManyToOne( buildingContext, mapValue.getMappedTable() );
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
				XClass keyXClass;
				AnnotatedClassType classType;
				if ( BinderHelper.PRIMITIVE_NAMES.contains( mapKeyType ) ) {
					classType = AnnotatedClassType.NONE;
					keyXClass = null;
				}
				else {
					try {
						keyXClass = buildingContext.getBootstrapContext().getReflectionManager().classForName( mapKeyType );
					}
					catch (ClassLoadingException e) {
						throw new AnnotationException( "Unable to find class: " + mapKeyType, e );
					}
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

				final PersistentClass owner = mapValue.getOwner();
				AccessType accessType;
				// FIXME support @Access for collection of elements
				// String accessType = access != null ? access.value() : null;
				if ( owner.getIdentifierAttributeMapping() != null ) {
					accessType = owner.getIdentifierAttributeMapping().getPropertyAccessorName().equals( "property" )
							? AccessType.PROPERTY
							: AccessType.FIELD;
				}
				else if ( owner.getEntityMappingHierarchy().getIdentifierEmbeddedValueMapping() != null
						&& owner.getEntityMappingHierarchy().hasEmbeddedIdentifier() ) {
					final PersistentAttributeMapping prop = owner.getEntityMappingHierarchy()
							.getIdentifierEmbeddedValueMapping()
							.getDeclaredPersistentAttributes()
							.get( 0 );
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
							buildingContext,
							inheritanceStatePerClass
					);
					mapValue.setIndex( component );
				}
				else {
					BasicValueBinder elementBinder = new BasicValueBinder(
							BasicValueBinder.Kind.COLLECTION_INDEX,
							buildingContext
					);
					elementBinder.setReturnedClassName( mapKeyType );

					Ejb3Column[] elementColumns = mapKeyColumns;
					if ( elementColumns == null || elementColumns.length == 0 ) {
						elementColumns = new Ejb3Column[1];
						Ejb3Column column = new Ejb3Column( buildingContext );
						column.setImplicit( false );
						column.setNullable( true );
						column.setLength( Size.Builder.DEFAULT_LENGTH );
						column.setLogicalColumnName( Collection.DEFAULT_KEY_COLUMN_NAME );
						//TODO create an EMPTY_JOINS collection
						column.setJoins( new HashMap<>() );
						column.bind();
						elementColumns[0] = column;
					}
					//override the table
					for (Ejb3Column column : elementColumns) {
						column.setTable( mapValue.getMappedTable() );
					}
					elementBinder.setColumns( elementColumns );
					//do not call setType as it extract the type from @Type
					//the algorithm generally does not apply for map key anyway
					mapValue.setIndex( elementBinder.make() );
					elementBinder.setType(
							property,
							keyXClass,
							this.collection.getOwnerEntityName(),
							holder.mapKeyAttributeConverterDescriptor( property, keyXClass )
					);
					elementBinder.setPersistentClassName( propertyHolder.getEntityName() );
				}
			}
			//FIXME pass the Index Entity JoinColumns
			if ( !collection.isOneToMany() ) {
				//index column shoud not be null
				for (Ejb3JoinColumn col : mapKeyManyToManyColumns) {
					col.forceNotNull();
				}
			}

			if ( element != null ) {
				final javax.persistence.ForeignKey foreignKey = getMapKeyForeignKey( property );
				if ( foreignKey != null ) {
					if ( foreignKey.value() == ConstraintMode.NO_CONSTRAINT ) {
						element.setForeignKeyName( "none" );
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

	private javax.persistence.ForeignKey getMapKeyForeignKey(XProperty property) {
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
			PersistentClass associatedClass,
			PersistentClass targetPropertyPersistentClass) {
		final Value element = collection.getElement();
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
			List<MappedColumn> referencedEntityColumns;
			if ( referencedPropertyName == null ) {
				referencedEntityColumns = associatedClass.getIdentifier().getMappedColumns();
			}
			else {
				final Property referencedProperty = associatedClass.getRecursiveProperty( referencedPropertyName );
				referencedEntityColumns = referencedProperty.getMappedColumns();
			}
			fromAndWhere = getFromAndWhereFormula(
					associatedClass.getMappedTable().getName(),
					element.getMappedColumns(),
					referencedEntityColumns
			);
		}
		else {
			// HHH-11005 - only if we are OneToMany and location of map key property is at a different level, need to add a select
			if ( !associatedClass.equals( targetPropertyPersistentClass ) ) {
				fromAndWhere = getFromAndWhereFormula(
						targetPropertyPersistentClass.getMappedTable().getQualifiedTableName().toString(),
						element.getMappedColumns(),
						associatedClass.getIdentifier().getMappedColumns()
				);
			}
		}

		if ( value instanceof Component ) {
			final Component component = (Component) value;
			List<PersistentAttributeMapping> attributes = component.getDeclaredPersistentAttributes();

			Component indexComponent = new Component( getBuildingContext(), collection );
			indexComponent.setComponentClassName( component.getEmbeddableClassName() );
			attributes.stream()
					.map( Property.class::cast )
					.forEach( current -> {
						Property newProperty = new Property( getBuildingContext() );
						newProperty.setCascade( current.getCascade() );
						newProperty.setValueGenerationStrategy( current.getValueGenerationStrategy() );
						newProperty.setInsertable( false );
						newProperty.setUpdateable( false );
						newProperty.setMetaAttributes( current.getMetaAttributes() );
						newProperty.setName( current.getName() );
						newProperty.setNaturalIdentifier( false );
						//newProperty.setOptimisticLocked( false );
						newProperty.setOptional( false );
						newProperty.setPersistentClass( (PersistentClass) current.getEntity() );
						newProperty.setPropertyAccessorName( current.getPropertyAccessorName() );
						newProperty.setSelectable( current.isSelectable() );
						newProperty.setValue(
								createFormulatedValue(
										current.getValue(), collection, associatedClass, associatedClass )
						);
						indexComponent.addDeclaredPersistentAttribute( newProperty );
					} );
			return indexComponent;
		}
		else if ( value instanceof SimpleValue ) {
			final SimpleValue sourceValue = (SimpleValue) value;
			SimpleValue targetValue;
			if ( value instanceof ManyToOne ) {
				final ManyToOne sourceManyToOne = (ManyToOne) sourceValue;
				final ManyToOne targetManyToOne = new ManyToOne( getBuildingContext(), collection.getMappedTable() );
				targetManyToOne.setFetchMode( FetchMode.DEFAULT );
				targetManyToOne.setLazy( true );
				//targetValue.setIgnoreNotFound( ); does not make sense for a map key
				targetManyToOne.setReferencedEntityName( sourceManyToOne.getReferencedEntityName() );
				targetValue = targetManyToOne;
			}
			else if ( value instanceof BasicValue ) {
				targetValue = new BasicValue( getBuildingContext(), collection.getMappedTable()
				);
				targetValue.copyTypeFrom( sourceValue );
			}
			else {
				throw new AssertionFailure( "Unknown type encounters for map key: " + value.getClass() );
			}
			final Random random = new Random();
			for ( MappedColumn current : sourceValue.getMappedColumns() ) {
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
				final Formula formula = new Formula( formulaString );
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

	private String getFromAndWhereFormula(
			String tableName,
			List<MappedColumn> collectionTableColumns,
			List<MappedColumn> referencedEntityColumns) {
		String alias = "$alias$";
		StringBuilder fromAndWhereSb = new StringBuilder( " from " )
				.append( tableName )
				//.append(" as ") //Oracle doesn't support it in subqueries
				.append( " " )
				.append( alias ).append( " where " );
		for ( int i = 0; i < collectionTableColumns.size(); i++ ) {
			Column colColumn = (Column) collectionTableColumns.get( i );
			Column refColumn = (Column) referencedEntityColumns.get( i );
			fromAndWhereSb.append( alias )
					.append( '.' )
					.append( refColumn.getQuotedName() )
					.append( '=' )
					.append( colColumn.getQuotedName() )
					.append( " and " );
		}
		return fromAndWhereSb.substring( 0, fromAndWhereSb.length() - 5 );
	}
}
