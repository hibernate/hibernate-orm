/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.AnyMetaDefs;
import org.hibernate.annotations.MetaValue;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.annotations.common.reflection.ClassLoadingException;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XPackage;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.annotations.EntityBinder;
import org.hibernate.cfg.annotations.Nullability;
import org.hibernate.cfg.annotations.TableBinder;
import org.hibernate.id.MultipleHiLoPerTableGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SyntheticProperty;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author Emmanuel Bernard
 */
public class BinderHelper {

	public static final String ANNOTATION_STRING_DEFAULT = "";

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, BinderHelper.class.getName());

	private BinderHelper() {
	}

	static {
		Set<String> primitiveNames = new HashSet<String>();
		primitiveNames.add( byte.class.getName() );
		primitiveNames.add( short.class.getName() );
		primitiveNames.add( int.class.getName() );
		primitiveNames.add( long.class.getName() );
		primitiveNames.add( float.class.getName() );
		primitiveNames.add( double.class.getName() );
		primitiveNames.add( char.class.getName() );
		primitiveNames.add( boolean.class.getName() );
		PRIMITIVE_NAMES = Collections.unmodifiableSet( primitiveNames );
	}

	public static final Set<String> PRIMITIVE_NAMES;

	/**
	 * create a property copy reusing the same value
	 */
	public static Property shallowCopy(Property property) {
		Property clone = new Property();
		clone.setCascade( property.getCascade() );
		clone.setInsertable( property.isInsertable() );
		clone.setLazy( property.isLazy() );
		clone.setName( property.getName() );
		clone.setNodeName( property.getNodeName() );
		clone.setNaturalIdentifier( property.isNaturalIdentifier() );
		clone.setOptimisticLocked( property.isOptimisticLocked() );
		clone.setOptional( property.isOptional() );
		clone.setPersistentClass( property.getPersistentClass() );
		clone.setPropertyAccessorName( property.getPropertyAccessorName() );
		clone.setSelectable( property.isSelectable() );
		clone.setUpdateable( property.isUpdateable() );
		clone.setValue( property.getValue() );
		return clone;
	}

// This is sooooooooo close in terms of not generating a synthetic property if we do not have to (where property ref
// refers to a single property).  The sticking point is cases where the `referencedPropertyName` come from subclasses
// or secondary tables.  Part of the problem is in PersistentClass itself during attempts to resolve the referenced
// property; currently it only considers non-subclass and non-joined properties.  Part of the problem is in terms
// of SQL generation.
//	public static void createSyntheticPropertyReference(
//			Ejb3JoinColumn[] columns,
//			PersistentClass ownerEntity,
//			PersistentClass associatedEntity,
//			Value value,
//			boolean inverse,
//			Mappings mappings) {
//		//associated entity only used for more precise exception, yuk!
//		if ( columns[0].isImplicit() || StringHelper.isNotEmpty( columns[0].getMappedBy() ) ) return;
//		int fkEnum = Ejb3JoinColumn.checkReferencedColumnsType( columns, ownerEntity, mappings );
//		PersistentClass associatedClass = columns[0].getPropertyHolder() != null ?
//				columns[0].getPropertyHolder().getPersistentClass() :
//				null;
//		if ( Ejb3JoinColumn.NON_PK_REFERENCE == fkEnum ) {
//			//find properties associated to a certain column
//			Object columnOwner = findColumnOwner( ownerEntity, columns[0].getReferencedColumn(), mappings );
//			List<Property> properties = findPropertiesByColumns( columnOwner, columns, mappings );
//
//			if ( properties == null ) {
//				//TODO use a ToOne type doing a second select
//				StringBuilder columnsList = new StringBuilder();
//				columnsList.append( "referencedColumnNames(" );
//				for (Ejb3JoinColumn column : columns) {
//					columnsList.append( column.getReferencedColumn() ).append( ", " );
//				}
//				columnsList.setLength( columnsList.length() - 2 );
//				columnsList.append( ") " );
//
//				if ( associatedEntity != null ) {
//					//overidden destination
//					columnsList.append( "of " )
//							.append( associatedEntity.getEntityName() )
//							.append( "." )
//							.append( columns[0].getPropertyName() )
//							.append( " " );
//				}
//				else {
//					if ( columns[0].getPropertyHolder() != null ) {
//						columnsList.append( "of " )
//								.append( columns[0].getPropertyHolder().getEntityName() )
//								.append( "." )
//								.append( columns[0].getPropertyName() )
//								.append( " " );
//					}
//				}
//				columnsList.append( "referencing " )
//						.append( ownerEntity.getEntityName() )
//						.append( " not mapped to a single property" );
//				throw new AnnotationException( columnsList.toString() );
//			}
//
//			final String referencedPropertyName;
//
//			if ( properties.size() == 1 ) {
//				referencedPropertyName = properties.get(0).getName();
//			}
//			else {
//				// Create a synthetic (embedded composite) property to use as the referenced property which
//				// contains all the properties mapped to the referenced columns.  We need to make a shallow copy
//				// of the properties to mark them as non-insertable/updatable.
//
//				// todo : what if the columns all match with an existing component?
//
//				StringBuilder propertyNameBuffer = new StringBuilder( "_" );
//				propertyNameBuffer.append( associatedClass.getEntityName().replace( '.', '_' ) );
//				propertyNameBuffer.append( "_" ).append( columns[0].getPropertyName() );
//				String syntheticPropertyName = propertyNameBuffer.toString();
//				//create an embeddable component
//
//				//todo how about properties.size() == 1, this should be much simpler
//				Component embeddedComp = columnOwner instanceof PersistentClass ?
//						new Component( mappings, (PersistentClass) columnOwner ) :
//						new Component( mappings, (Join) columnOwner );
//				embeddedComp.setEmbedded( true );
//				embeddedComp.setNodeName( syntheticPropertyName );
//				embeddedComp.setComponentClassName( embeddedComp.getOwner().getClassName() );
//				for (Property property : properties) {
//					Property clone = BinderHelper.shallowCopy( property );
//					clone.setInsertable( false );
//					clone.setUpdateable( false );
//					clone.setNaturalIdentifier( false );
//					clone.setGeneration( property.getGeneration() );
//					embeddedComp.addProperty( clone );
//				}
//				SyntheticProperty synthProp = new SyntheticProperty();
//				synthProp.setName( syntheticPropertyName );
//				synthProp.setNodeName( syntheticPropertyName );
//				synthProp.setPersistentClass( ownerEntity );
//				synthProp.setUpdateable( false );
//				synthProp.setInsertable( false );
//				synthProp.setValue( embeddedComp );
//				synthProp.setPropertyAccessorName( "embedded" );
//				ownerEntity.addProperty( synthProp );
//				//make it unique
//				TableBinder.createUniqueConstraint( embeddedComp );
//
//				referencedPropertyName = syntheticPropertyName;
//			}
//
//			/**
//			 * creating the property ref to the new synthetic property
//			 */
//			if ( value instanceof ToOne ) {
//				( (ToOne) value ).setReferencedPropertyName( referencedPropertyName );
//				mappings.addUniquePropertyReference( ownerEntity.getEntityName(), referencedPropertyName );
//			}
//			else if ( value instanceof Collection ) {
//				( (Collection) value ).setReferencedPropertyName( referencedPropertyName );
//				//not unique because we could create a mtm wo association table
//				mappings.addPropertyReference( ownerEntity.getEntityName(), referencedPropertyName );
//			}
//			else {
//				throw new AssertionFailure(
//						"Do a property ref on an unexpected Value type: "
//								+ value.getClass().getName()
//				);
//			}
//			mappings.addPropertyReferencedAssociation(
//					( inverse ? "inverse__" : "" ) + associatedClass.getEntityName(),
//					columns[0].getPropertyName(),
//					referencedPropertyName
//			);
//		}
//	}

	public static void createSyntheticPropertyReference(
			Ejb3JoinColumn[] columns,
			PersistentClass ownerEntity,
			PersistentClass associatedEntity,
			Value value,
			boolean inverse,
			MetadataBuildingContext context) {
		//associated entity only used for more precise exception, yuk!
		if ( columns[0].isImplicit() || StringHelper.isNotEmpty( columns[0].getMappedBy() ) ) return;
		int fkEnum = Ejb3JoinColumn.checkReferencedColumnsType( columns, ownerEntity, context );
		PersistentClass associatedClass = columns[0].getPropertyHolder() != null ?
				columns[0].getPropertyHolder().getPersistentClass() :
				null;
		if ( Ejb3JoinColumn.NON_PK_REFERENCE == fkEnum ) {
			/**
			 * Create a synthetic property to refer to including an
			 * embedded component value containing all the properties
			 * mapped to the referenced columns
			 * We need to shallow copy those properties to mark them
			 * as non insertable / non updatable
			 */
			StringBuilder propertyNameBuffer = new StringBuilder( "_" );
			propertyNameBuffer.append( associatedClass.getEntityName().replace( '.', '_' ) );
			propertyNameBuffer.append( "_" ).append( columns[0].getPropertyName().replace( '.', '_' ) );
			String syntheticPropertyName = propertyNameBuffer.toString();
			//find properties associated to a certain column
			Object columnOwner = findColumnOwner( ownerEntity, columns[0].getReferencedColumn(), context );
			List<Property> properties = findPropertiesByColumns( columnOwner, columns, context );
			//create an embeddable component
                        Property synthProp = null;
			if ( properties != null ) {
                        //todo how about properties.size() == 1, this should be much simpler
				Component embeddedComp = columnOwner instanceof PersistentClass ?
						new Component( context.getMetadataCollector(), (PersistentClass) columnOwner ) :
						new Component( context.getMetadataCollector(), (Join) columnOwner );
				embeddedComp.setEmbedded( true );
				embeddedComp.setNodeName( syntheticPropertyName );
				embeddedComp.setComponentClassName( embeddedComp.getOwner().getClassName() );
				for (Property property : properties) {
					Property clone = BinderHelper.shallowCopy( property );
					clone.setInsertable( false );
					clone.setUpdateable( false );
					clone.setNaturalIdentifier( false );
					clone.setValueGenerationStrategy( property.getValueGenerationStrategy() );
					embeddedComp.addProperty( clone );
                                }
                                    synthProp = new SyntheticProperty();
				synthProp.setName( syntheticPropertyName );
				synthProp.setNodeName( syntheticPropertyName );
				synthProp.setPersistentClass( ownerEntity );
				synthProp.setUpdateable( false );
				synthProp.setInsertable( false );
				synthProp.setValue( embeddedComp );
				synthProp.setPropertyAccessorName( "embedded" );
				ownerEntity.addProperty( synthProp );
                                //make it unique
				TableBinder.createUniqueConstraint( embeddedComp );
                            }
			else {
				//TODO use a ToOne type doing a second select
				StringBuilder columnsList = new StringBuilder();
				columnsList.append( "referencedColumnNames(" );
				for (Ejb3JoinColumn column : columns) {
					columnsList.append( column.getReferencedColumn() ).append( ", " );
				}
				columnsList.setLength( columnsList.length() - 2 );
				columnsList.append( ") " );

				if ( associatedEntity != null ) {
					//overidden destination
					columnsList.append( "of " )
							.append( associatedEntity.getEntityName() )
							.append( "." )
							.append( columns[0].getPropertyName() )
							.append( " " );
				}
				else {
					if ( columns[0].getPropertyHolder() != null ) {
						columnsList.append( "of " )
								.append( columns[0].getPropertyHolder().getEntityName() )
								.append( "." )
								.append( columns[0].getPropertyName() )
								.append( " " );
					}
				}
				columnsList.append( "referencing " )
						.append( ownerEntity.getEntityName() )
						.append( " not mapped to a single property" );
				throw new AnnotationException( columnsList.toString() );
			}

			/**
			 * creating the property ref to the new synthetic property
			 */
			if ( value instanceof ToOne ) {
				( (ToOne) value ).setReferencedPropertyName( syntheticPropertyName );
				( (ToOne) value ).setReferenceToPrimaryKey( syntheticPropertyName == null );
				context.getMetadataCollector().addUniquePropertyReference(
						ownerEntity.getEntityName(),
						syntheticPropertyName
				);
			}
			else if ( value instanceof Collection ) {
				( (Collection) value ).setReferencedPropertyName( syntheticPropertyName );
				//not unique because we could create a mtm wo association table
				context.getMetadataCollector().addPropertyReference(
						ownerEntity.getEntityName(),
						syntheticPropertyName
				);
			}
			else {
				throw new AssertionFailure(
						"Do a property ref on an unexpected Value type: "
								+ value.getClass().getName()
				);
			}
			context.getMetadataCollector().addPropertyReferencedAssociation(
					( inverse ? "inverse__" : "" ) + associatedClass.getEntityName(),
					columns[0].getPropertyName(),
					syntheticPropertyName
			);
		}
	}


	private static List<Property> findPropertiesByColumns(
			Object columnOwner,
			Ejb3JoinColumn[] columns,
			MetadataBuildingContext context) {
		Map<Column, Set<Property>> columnsToProperty = new HashMap<Column, Set<Property>>();
		List<Column> orderedColumns = new ArrayList<Column>( columns.length );
		Table referencedTable = null;
		if ( columnOwner instanceof PersistentClass ) {
			referencedTable = ( (PersistentClass) columnOwner ).getTable();
		}
		else if ( columnOwner instanceof Join ) {
			referencedTable = ( (Join) columnOwner ).getTable();
		}
		else {
			throw new AssertionFailure(
					columnOwner == null ?
							"columnOwner is null" :
							"columnOwner neither PersistentClass nor Join: " + columnOwner.getClass()
			);
		}
		//build the list of column names
		for (Ejb3JoinColumn column1 : columns) {
			Column column = new Column(
					context.getMetadataCollector().getPhysicalColumnName(
							referencedTable,
							column1.getReferencedColumn()
					)
			);
			orderedColumns.add( column );
			columnsToProperty.put( column, new HashSet<Property>() );
		}
		boolean isPersistentClass = columnOwner instanceof PersistentClass;
		Iterator it = isPersistentClass ?
				( (PersistentClass) columnOwner ).getPropertyIterator() :
				( (Join) columnOwner ).getPropertyIterator();
		while ( it.hasNext() ) {
			matchColumnsByProperty( (Property) it.next(), columnsToProperty );
		}
		if ( isPersistentClass ) {
			matchColumnsByProperty( ( (PersistentClass) columnOwner ).getIdentifierProperty(), columnsToProperty );
		}

		//first naive implementation
		//only check 1 columns properties
		//TODO make it smarter by checking correctly ordered multi column properties
		List<Property> orderedProperties = new ArrayList<Property>();
		for (Column column : orderedColumns) {
			boolean found = false;
			for (Property property : columnsToProperty.get( column ) ) {
				if ( property.getColumnSpan() == 1 ) {
					orderedProperties.add( property );
					found = true;
					break;
				}
			}
			if ( !found ) return null; //have to find it the hard way
		}
		return orderedProperties;
	}

	private static void matchColumnsByProperty(Property property, Map<Column, Set<Property>> columnsToProperty) {
		if ( property == null ) return;
		if ( "noop".equals( property.getPropertyAccessorName() )
				|| "embedded".equals( property.getPropertyAccessorName() ) ) {
			return;
		}
// FIXME cannot use subproperties becasue the caller needs top level properties
//		if ( property.isComposite() ) {
//			Iterator subProperties = ( (Component) property.getValue() ).getPropertyIterator();
//			while ( subProperties.hasNext() ) {
//				matchColumnsByProperty( (Property) subProperties.next(), columnsToProperty );
//			}
//		}
		else {
			Iterator columnIt = property.getColumnIterator();
			while ( columnIt.hasNext() ) {
				Object column = columnIt.next(); //can be a Formula so we don't cast
				//noinspection SuspiciousMethodCalls
				if ( columnsToProperty.containsKey( column ) ) {
					columnsToProperty.get( column ).add( property );
				}
			}
		}
	}

	/**
	 * Retrieve the property by path in a recursive way, including IndetifierProperty in the loop
	 * If propertyName is null or empty, the IdentifierProperty is returned
	 */
	public static Property findPropertyByName(PersistentClass associatedClass, String propertyName) {
		Property property = null;
		Property idProperty = associatedClass.getIdentifierProperty();
		String idName = idProperty != null ? idProperty.getName() : null;
		try {
			if ( propertyName == null
					|| propertyName.length() == 0
					|| propertyName.equals( idName ) ) {
				//default to id
				property = idProperty;
			}
			else {
				if ( propertyName.indexOf( idName + "." ) == 0 ) {
					property = idProperty;
					propertyName = propertyName.substring( idName.length() + 1 );
				}
				StringTokenizer st = new StringTokenizer( propertyName, ".", false );
				while ( st.hasMoreElements() ) {
					String element = (String) st.nextElement();
					if ( property == null ) {
						property = associatedClass.getProperty( element );
					}
					else {
						if ( !property.isComposite() ) return null;
						property = ( (Component) property.getValue() ).getProperty( element );
					}
				}
			}
		}
		catch (MappingException e) {
			try {
				//if we do not find it try to check the identifier mapper
				if ( associatedClass.getIdentifierMapper() == null ) return null;
				StringTokenizer st = new StringTokenizer( propertyName, ".", false );
				while ( st.hasMoreElements() ) {
					String element = (String) st.nextElement();
					if ( property == null ) {
						property = associatedClass.getIdentifierMapper().getProperty( element );
					}
					else {
						if ( !property.isComposite() ) return null;
						property = ( (Component) property.getValue() ).getProperty( element );
					}
				}
			}
			catch (MappingException ee) {
				return null;
			}
		}
		return property;
	}

	/**
	 * Retrieve the property by path in a recursive way
	 */
	public static Property findPropertyByName(Component component, String propertyName) {
		Property property = null;
		try {
			if ( propertyName == null
					|| propertyName.length() == 0) {
				// Do not expect to use a primary key for this case
				return null;
			}
			else {
				StringTokenizer st = new StringTokenizer( propertyName, ".", false );
				while ( st.hasMoreElements() ) {
					String element = (String) st.nextElement();
					if ( property == null ) {
						property = component.getProperty( element );
					}
					else {
						if ( !property.isComposite() ) return null;
						property = ( (Component) property.getValue() ).getProperty( element );
					}
				}
			}
		}
		catch (MappingException e) {
			try {
				//if we do not find it try to check the identifier mapper
				if ( component.getOwner().getIdentifierMapper() == null ) return null;
				StringTokenizer st = new StringTokenizer( propertyName, ".", false );
				while ( st.hasMoreElements() ) {
					String element = (String) st.nextElement();
					if ( property == null ) {
						property = component.getOwner().getIdentifierMapper().getProperty( element );
					}
					else {
						if ( !property.isComposite() ) return null;
						property = ( (Component) property.getValue() ).getProperty( element );
					}
				}
			}
			catch (MappingException ee) {
				return null;
			}
		}
		return property;
	}

	public static String getRelativePath(PropertyHolder propertyHolder, String propertyName) {
		if ( propertyHolder == null ) return propertyName;
		String path = propertyHolder.getPath();
		String entityName = propertyHolder.getPersistentClass().getEntityName();
		if ( path.length() == entityName.length() ) {
			return propertyName;
		}
		else {
			return StringHelper.qualify( path.substring( entityName.length() + 1 ), propertyName );
		}
	}

	/**
	 * Find the column owner (ie PersistentClass or Join) of columnName.
	 * If columnName is null or empty, persistentClass is returned
	 */
	public static Object findColumnOwner(
			PersistentClass persistentClass,
			String columnName,
			MetadataBuildingContext context) {
		if ( StringHelper.isEmpty( columnName ) ) {
			return persistentClass; //shortcut for implicit referenced column names
		}
		PersistentClass current = persistentClass;
		Object result;
		boolean found = false;
		do {
			result = current;
			Table currentTable = current.getTable();
			try {
				context.getMetadataCollector().getPhysicalColumnName( currentTable, columnName );
				found = true;
			}
			catch (MappingException me) {
				//swallow it
			}
			Iterator joins = current.getJoinIterator();
			while ( !found && joins.hasNext() ) {
				result = joins.next();
				currentTable = ( (Join) result ).getTable();
				try {
					context.getMetadataCollector().getPhysicalColumnName( currentTable, columnName );
					found = true;
				}
				catch (MappingException me) {
					//swallow it
				}
			}
			current = current.getSuperclass();
		}
		while ( !found && current != null );
		return found ? result : null;
	}

	/**
	 * apply an id generator to a SimpleValue
	 */
	public static void makeIdGenerator(
			SimpleValue id,
			String generatorType,
			String generatorName,
			MetadataBuildingContext buildingContext,
			Map<String, IdentifierGeneratorDefinition> localGenerators) {
		Table table = id.getTable();
		table.setIdentifierValue( id );
		//generator settings
		id.setIdentifierGeneratorStrategy( generatorType );

		Properties params = new Properties();

		//always settable
		params.setProperty(
				PersistentIdentifierGenerator.TABLE, table.getName()
		);

		final String implicitCatalogName = buildingContext.getBuildingOptions().getMappingDefaults().getImplicitCatalogName();
		if ( implicitCatalogName != null ) {
			params.put( PersistentIdentifierGenerator.CATALOG, implicitCatalogName );
		}
		final String implicitSchemaName = buildingContext.getBuildingOptions().getMappingDefaults().getImplicitSchemaName();
		if ( implicitSchemaName != null ) {
			params.put( PersistentIdentifierGenerator.SCHEMA, implicitSchemaName );
		}

		if ( id.getColumnSpan() == 1 ) {
			params.setProperty(
					PersistentIdentifierGenerator.PK,
					( (org.hibernate.mapping.Column) id.getColumnIterator().next() ).getName()
			);
		}
		// YUCK!  but cannot think of a clean way to do this given the string-config based scheme
		params.put( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER, buildingContext.getObjectNameNormalizer() );

		if ( !isEmptyAnnotationValue( generatorName ) ) {
			//we have a named generator
			IdentifierGeneratorDefinition gen = getIdentifierGenerator( generatorName, localGenerators, buildingContext );
			if ( gen == null ) {
				throw new AnnotationException( "Unknown Id.generator: " + generatorName );
			}
			//This is quite vague in the spec but a generator could override the generate choice
			String identifierGeneratorStrategy = gen.getStrategy();
			//yuk! this is a hack not to override 'AUTO' even if generator is set
			final boolean avoidOverriding =
					identifierGeneratorStrategy.equals( "identity" )
							|| identifierGeneratorStrategy.equals( "seqhilo" )
							|| identifierGeneratorStrategy.equals( MultipleHiLoPerTableGenerator.class.getName() );
			if ( generatorType == null || !avoidOverriding ) {
				id.setIdentifierGeneratorStrategy( identifierGeneratorStrategy );
			}
			//checkIfMatchingGenerator(gen, generatorType, generatorName);
			for ( Object o : gen.getParameters().entrySet() ) {
				Map.Entry elt = (Map.Entry) o;
				params.setProperty( (String) elt.getKey(), (String) elt.getValue() );
			}
		}
		if ( "assigned".equals( generatorType ) ) id.setNullValue( "undefined" );
		id.setIdentifierGeneratorProperties( params );
	}

	public static IdentifierGeneratorDefinition getIdentifierGenerator(
			String name,
			Map<String, IdentifierGeneratorDefinition> localGenerators,
			MetadataBuildingContext buildingContext) {
		if ( localGenerators != null ) {
			final IdentifierGeneratorDefinition result = localGenerators.get( name );
			if ( result != null ) {
				return result;
			}
		}

		return buildingContext.getMetadataCollector().getIdentifierGenerator( name );
	}

	public static boolean isEmptyAnnotationValue(String annotationString) {
		return annotationString != null && annotationString.length() == 0;
		//equivalent to (but faster) ANNOTATION_STRING_DEFAULT.equals( annotationString );
	}

	public static Any buildAnyValue(
			String anyMetaDefName,
			Ejb3JoinColumn[] columns,
			javax.persistence.Column metaColumn,
			PropertyData inferredData,
			boolean cascadeOnDelete,
			Nullability nullability,
			PropertyHolder propertyHolder,
			EntityBinder entityBinder,
			boolean optional,
			MetadataBuildingContext context) {
		//All FK columns should be in the same table
		Any value = new Any( context.getMetadataCollector(), columns[0].getTable() );
		AnyMetaDef metaAnnDef = inferredData.getProperty().getAnnotation( AnyMetaDef.class );

		if ( metaAnnDef != null ) {
			//local has precedence over general and can be mapped for future reference if named
			bindAnyMetaDefs( inferredData.getProperty(), context );
		}
		else {
			metaAnnDef = context.getMetadataCollector().getAnyMetaDef( anyMetaDefName );
		}
		if ( metaAnnDef != null ) {
			value.setIdentifierType( metaAnnDef.idType() );
			value.setMetaType( metaAnnDef.metaType() );

			HashMap values = new HashMap();
			org.hibernate.type.Type metaType = context.getMetadataCollector().getTypeResolver().heuristicType( value.getMetaType() );
			for (MetaValue metaValue : metaAnnDef.metaValues()) {
				try {
					Object discrim = ( (org.hibernate.type.DiscriminatorType) metaType ).stringToObject( metaValue
							.value() );
					String entityName = metaValue.targetEntity().getName();
					values.put( discrim, entityName );
				}
				catch (ClassCastException cce) {
					throw new MappingException( "metaType was not a DiscriminatorType: "
							+ metaType.getName() );
				}
				catch (Exception e) {
					throw new MappingException( "could not interpret metaValue", e );
				}
			}
			if ( !values.isEmpty() ) value.setMetaValues( values );
		}
		else {
			throw new AnnotationException( "Unable to find @AnyMetaDef for an @(ManyTo)Any mapping: "
					+ StringHelper.qualify( propertyHolder.getPath(), inferredData.getPropertyName() ) );
		}

		value.setCascadeDeleteEnabled( cascadeOnDelete );
		if ( !optional ) {
			for (Ejb3JoinColumn column : columns) {
				column.setNullable( false );
			}
		}

		Ejb3Column[] metaColumns = Ejb3Column.buildColumnFromAnnotation(
				new javax.persistence.Column[] { metaColumn },
				null,
				nullability,
				propertyHolder,
				inferredData,
				entityBinder.getSecondaryTables(),
				context
		);

		//set metaColumn to the right table
		for (Ejb3Column column : metaColumns) {
			column.setTable( value.getTable() );
		}
		//meta column
		for (Ejb3Column column : metaColumns) {
			column.linkWithValue( value );
		}

		//id columns
		final String propertyName = inferredData.getPropertyName();
		Ejb3Column.checkPropertyConsistency( columns, propertyHolder.getEntityName() + "." + propertyName );
		for (Ejb3JoinColumn column : columns) {
			column.linkWithValue( value );
		}
		return value;
	}

	public static void bindAnyMetaDefs(XAnnotatedElement annotatedElement, MetadataBuildingContext context) {
		AnyMetaDef defAnn = annotatedElement.getAnnotation( AnyMetaDef.class );
		AnyMetaDefs defsAnn = annotatedElement.getAnnotation( AnyMetaDefs.class );
		boolean mustHaveName = XClass.class.isAssignableFrom( annotatedElement.getClass() )
				|| XPackage.class.isAssignableFrom( annotatedElement.getClass() );
		if ( defAnn != null ) {
			checkAnyMetaDefValidity( mustHaveName, defAnn, annotatedElement );
			bindAnyMetaDef( defAnn, context );
		}
		if ( defsAnn != null ) {
			for (AnyMetaDef def : defsAnn.value()) {
				checkAnyMetaDefValidity( mustHaveName, def, annotatedElement );
				bindAnyMetaDef( def, context );
			}
		}
	}

	private static void checkAnyMetaDefValidity(boolean mustHaveName, AnyMetaDef defAnn, XAnnotatedElement annotatedElement) {
		if ( mustHaveName && isEmptyAnnotationValue( defAnn.name() ) ) {
			String name = XClass.class.isAssignableFrom( annotatedElement.getClass() ) ?
					( (XClass) annotatedElement ).getName() :
					( (XPackage) annotatedElement ).getName();
			throw new AnnotationException( "@AnyMetaDef.name cannot be null on an entity or a package: " + name );
		}
	}

	private static void bindAnyMetaDef(AnyMetaDef defAnn, MetadataBuildingContext context) {
		if ( isEmptyAnnotationValue( defAnn.name() ) ) {
			return; //don't map not named definitions
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding Any Meta definition: %s", defAnn.name() );
		}
		context.getMetadataCollector().addAnyMetaDef( defAnn );
	}

	public static MappedSuperclass getMappedSuperclassOrNull(
			XClass declaringClass,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			MetadataBuildingContext context) {
		boolean retrieve = false;
		if ( declaringClass != null ) {
			final InheritanceState inheritanceState = inheritanceStatePerClass.get( declaringClass );
			if ( inheritanceState == null ) {
				throw new org.hibernate.annotations.common.AssertionFailure(
						"Declaring class is not found in the inheritance state hierarchy: " + declaringClass
				);
			}
			if ( inheritanceState.isEmbeddableSuperclass() ) {
				retrieve = true;
			}
		}

		if ( retrieve ) {
			return context.getMetadataCollector().getMappedSuperclass(
					context.getBuildingOptions().getReflectionManager().toClass( declaringClass )
			);
		}
		else {
			return null;
		}
	}

	public static String getPath(PropertyHolder holder, PropertyData property) {
		return StringHelper.qualify( holder.getPath(), property.getPropertyName() );
	}

	static PropertyData getPropertyOverriddenByMapperOrMapsId(
			boolean isId,
			PropertyHolder propertyHolder,
			String propertyName,
			MetadataBuildingContext buildingContext) {
		final XClass persistentXClass;
		try {
			 persistentXClass = buildingContext.getBuildingOptions().getReflectionManager()
					.classForName( propertyHolder.getPersistentClass().getClassName() );
		}
		catch ( ClassLoadingException e ) {
			throw new AssertionFailure( "PersistentClass name cannot be converted into a Class", e);
		}
		if ( propertyHolder.isInIdClass() ) {
			PropertyData pd = buildingContext.getMetadataCollector().getPropertyAnnotatedWithIdAndToOne(
					persistentXClass,
					propertyName
			);
			if ( pd == null && buildingContext.getBuildingOptions().isSpecjProprietarySyntaxEnabled() ) {
				pd = buildingContext.getMetadataCollector().getPropertyAnnotatedWithMapsId(
						persistentXClass,
						propertyName
				);
			}
			return pd;
		}
        String propertyPath = isId ? "" : propertyName;
        return buildingContext.getMetadataCollector().getPropertyAnnotatedWithMapsId( persistentXClass, propertyPath );
	}
	
	public static Map<String,String> toAliasTableMap(SqlFragmentAlias[] aliases){
		Map<String,String> ret = new HashMap<String,String>();
		for (int i = 0; i < aliases.length; i++){
			if (StringHelper.isNotEmpty(aliases[i].table())){
				ret.put(aliases[i].alias(), aliases[i].table());
}
		}
		return ret;
	}
	
	public static Map<String,String> toAliasEntityMap(SqlFragmentAlias[] aliases){
		Map<String,String> ret = new HashMap<String,String>();
		for (int i = 0; i < aliases.length; i++){
			if (aliases[i].entity() != void.class){
				ret.put(aliases[i].alias(), aliases[i].entity().getName());
			}
		}
		return ret;
	}
}
