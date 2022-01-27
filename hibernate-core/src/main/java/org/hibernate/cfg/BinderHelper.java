/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.lang.annotation.Annotation;
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
import java.util.function.Consumer;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.model.IdGeneratorStrategyInterpreter;
import org.hibernate.boot.model.IdGeneratorStrategyInterpreter.GeneratorNameDeterminationContext;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.annotations.BasicValueBinder;
import org.hibernate.cfg.annotations.EntityBinder;
import org.hibernate.cfg.annotations.Nullability;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SyntheticProperty;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.type.descriptor.java.JavaType;

import org.jboss.logging.Logger;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.UniqueConstraint;

import static org.hibernate.cfg.AnnotatedColumn.buildColumnOrFormulaFromAnnotation;

/**
 * @author Emmanuel Bernard
 */
public class BinderHelper {

	private static final Logger log = CoreLogging.logger( BinderHelper.class );

	private BinderHelper() {
	}

	public static final Set<String> PRIMITIVE_NAMES = Set.of(
			byte.class.getName(),
			short.class.getName(),
			int.class.getName(),
			long.class.getName(),
			float.class.getName(),
			double.class.getName(),
			char.class.getName(),
			boolean.class.getName()
	);

	/**
	 * create a property copy reusing the same value
	 */
	public static Property shallowCopy(Property property) {
		Property clone = new Property();
		clone.setCascade( property.getCascade() );
		clone.setInsertable( property.isInsertable() );
		clone.setLazy( property.isLazy() );
		clone.setName( property.getName() );
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


	public static void createSyntheticPropertyReference(
			AnnotatedJoinColumn[] columns,
			PersistentClass ownerEntity,
			PersistentClass associatedEntity,
			Value value,
			boolean inverse,
			MetadataBuildingContext context) {
		//associated entity only used for more precise exception, yuk!
		if ( columns[0].isImplicit() || StringHelper.isNotEmpty( columns[0].getMappedBy() ) ) {
			return;
		}
		int fkEnum = AnnotatedJoinColumn.checkReferencedColumnsType( columns, ownerEntity, context );
		PersistentClass associatedClass = columns[0].getPropertyHolder() != null ?
				columns[0].getPropertyHolder().getPersistentClass() :
				null;
		if ( AnnotatedJoinColumn.NON_PK_REFERENCE == fkEnum ) {
			/*
			 * Create a synthetic property to refer to including an
			 * embedded component value containing all the properties
			 * mapped to the referenced columns
			 * We need to shallow copy those properties to mark them
			 * as non insertable / non updatable
			 */
			String syntheticPropertyName =
					"_" + associatedClass.getEntityName().replace('.', '_') +
					"_" + columns[0].getPropertyName().replace('.', '_');
			//find properties associated to a certain column
			Object columnOwner = findColumnOwner( ownerEntity, columns[0].getReferencedColumn(), context );
			List<Property> properties = findPropertiesByColumns( columnOwner, columns, context );
			//create an embeddable component
			Property synthProp;
			if ( properties != null ) {
                        //todo how about properties.size() == 1, this should be much simpler
				Component embeddedComp = columnOwner instanceof PersistentClass ?
						new Component( context, (PersistentClass) columnOwner ) :
						new Component( context, (Join) columnOwner );
				embeddedComp.setEmbedded( true );
				embeddedComp.setComponentClassName( embeddedComp.getOwner().getClassName() );
				for (Property property : properties) {
					Property clone = BinderHelper.shallowCopy( property );
					clone.setInsertable( false );
					clone.setUpdateable( false );
					clone.setNaturalIdentifier( false );
					clone.setValueGenerationStrategy( property.getValueGenerationStrategy() );
					embeddedComp.addProperty( clone );
				}
				embeddedComp.sortProperties();
				synthProp = new SyntheticProperty();
				synthProp.setName( syntheticPropertyName );
				synthProp.setPersistentClass( ownerEntity );
				synthProp.setUpdateable( false );
				synthProp.setInsertable( false );
				synthProp.setValue( embeddedComp );
				synthProp.setPropertyAccessorName( "embedded" );
				ownerEntity.addProperty( synthProp );
				//make it unique
				embeddedComp.createUniqueKey();
			}
			else {
				//TODO use a ToOne type doing a second select
				StringBuilder columnsList = new StringBuilder();
				columnsList.append( "referencedColumnNames(" );
				for (AnnotatedJoinColumn column : columns) {
					columnsList.append( column.getReferencedColumn() ).append( ", " );
				}
				columnsList.setLength( columnsList.length() - 2 );
				columnsList.append( ") " );

				if ( associatedEntity != null ) {
					//overridden destination
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

			/*
			 * creating the property ref to the new synthetic property
			 */
			if ( value instanceof ToOne ) {
				( (ToOne) value ).setReferencedPropertyName( syntheticPropertyName );
				( (ToOne) value ).setReferenceToPrimaryKey( false );
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
			AnnotatedJoinColumn[] columns,
			MetadataBuildingContext context) {
		Map<Column, Set<Property>> columnsToProperty = new HashMap<>();
		List<Column> orderedColumns = new ArrayList<>( columns.length );
		Table referencedTable;
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
		for (AnnotatedJoinColumn column1 : columns) {
			Column column = new Column(
					context.getMetadataCollector().getPhysicalColumnName(
							referencedTable,
							column1.getReferencedColumn()
					)
			);
			orderedColumns.add( column );
			columnsToProperty.put( column, new HashSet<>() );
		}
		boolean isPersistentClass = columnOwner instanceof PersistentClass;
		List<Property> properties = isPersistentClass ?
				( (PersistentClass) columnOwner ).getProperties() :
				( (Join) columnOwner ).getProperties();
		for (Property property : properties) {
			matchColumnsByProperty( property, columnsToProperty );
		}
		if ( isPersistentClass ) {
			matchColumnsByProperty( ( (PersistentClass) columnOwner ).getIdentifierProperty(), columnsToProperty );
		}

		//first naive implementation
		//only check 1 columns properties
		//TODO make it smarter by checking correctly ordered multi column properties
		List<Property> orderedProperties = new ArrayList<>();
		for (Column column : orderedColumns) {
			boolean found = false;
			for (Property property : columnsToProperty.get( column ) ) {
				if ( property.getColumnSpan() == 1 ) {
					orderedProperties.add( property );
					found = true;
					break;
				}
			}
			if ( !found ) {
				//have to find it the hard way
				return null;
			}
		}
		return orderedProperties;
	}

	private static void matchColumnsByProperty(Property property, Map<Column, Set<Property>> columnsToProperty) {
		if ( property == null ) {
			return;
		}
		if ( "noop".equals( property.getPropertyAccessorName() )
				|| "embedded".equals( property.getPropertyAccessorName() ) ) {
			return;
		}
// FIXME cannot use subproperties because the caller needs top level properties
//		if ( property.isComposite() ) {
//			Iterator subProperties = ( (Component) property.getValue() ).getPropertyIterator();
//			while ( subProperties.hasNext() ) {
//				matchColumnsByProperty( (Property) subProperties.next(), columnsToProperty );
//			}
//		}
		else {
			for (Selectable selectable : property.getSelectables()) {
				//can be a Formula, so we don't cast
				//noinspection SuspiciousMethodCalls
				if ( columnsToProperty.containsKey( selectable ) ) {
					//noinspection SuspiciousMethodCalls
					columnsToProperty.get( selectable ).add( property );
				}
			}
		}
	}

	/**
	 * Retrieve the property by path in a recursive way, including IdentifierProperty in the loop
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
						if ( !property.isComposite() ) {
							return null;
						}
						property = ( (Component) property.getValue() ).getProperty( element );
					}
				}
			}
		}
		catch (MappingException e) {
			try {
				//if we do not find it try to check the identifier mapper
				if ( associatedClass.getIdentifierMapper() == null ) {
					return null;
				}
				StringTokenizer st = new StringTokenizer( propertyName, ".", false );
				while ( st.hasMoreElements() ) {
					String element = (String) st.nextElement();
					if ( property == null ) {
						property = associatedClass.getIdentifierMapper().getProperty( element );
					}
					else {
						if ( !property.isComposite() ) {
							return null;
						}
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
						if ( !property.isComposite() ) {
							return null;
						}
						property = ( (Component) property.getValue() ).getProperty( element );
					}
				}
			}
		}
		catch (MappingException e) {
			try {
				//if we do not find it try to check the identifier mapper
				if ( component.getOwner().getIdentifierMapper() == null ) {
					return null;
				}
				StringTokenizer st = new StringTokenizer( propertyName, ".", false );
				while ( st.hasMoreElements() ) {
					String element = (String) st.nextElement();
					if ( property == null ) {
						property = component.getOwner().getIdentifierMapper().getProperty( element );
					}
					else {
						if ( !property.isComposite() ) {
							return null;
						}
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
		if ( propertyHolder == null ) {
			return propertyName;
		}
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
			//shortcut for implicit referenced column names
			return persistentClass;
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
			Iterator<Join> joins = current.getJoinIterator();
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
			XProperty idXProperty,
			String generatorType,
			String generatorName,
			MetadataBuildingContext buildingContext,
			Map<String, IdentifierGeneratorDefinition> localGenerators) {
		log.debugf( "#makeIdGenerator(%s, %s, %s, %s, ...)", id, idXProperty, generatorType, generatorName );

		Table table = id.getTable();
		table.setIdentifierValue( id );
		//generator settings
		id.setIdentifierGeneratorStrategy( generatorType );

		Properties params = new Properties();

		//always settable
		params.setProperty(
				PersistentIdentifierGenerator.TABLE, table.getName()
		);

		if ( id.getColumnSpan() == 1 ) {
			params.setProperty(
					PersistentIdentifierGenerator.PK,
					id.getColumns().get(0).getName()
			);
		}
		// YUCK!  but cannot think of a clean way to do this given the string-config based scheme
		params.put( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER, buildingContext.getObjectNameNormalizer() );
		params.put( IdentifierGenerator.GENERATOR_NAME, generatorName );

		if ( !isEmptyAnnotationValue( generatorName ) ) {
			//we have a named generator
			IdentifierGeneratorDefinition gen = getIdentifierGenerator(
					generatorName,
					idXProperty,
					localGenerators,
					buildingContext
			);
			if ( gen == null ) {
				throw new AnnotationException( "Unknown named generator (@GeneratedValue#generatorName): " + generatorName );
			}
			//This is quite vague in the spec but a generator could override the generator choice
			String identifierGeneratorStrategy = gen.getStrategy();
			//yuk! this is a hack not to override 'AUTO' even if generator is set
			final boolean avoidOverriding = identifierGeneratorStrategy.equals( "identity" )
					|| identifierGeneratorStrategy.equals( "seqhilo" );
			if ( generatorType == null || !avoidOverriding ) {
				id.setIdentifierGeneratorStrategy( identifierGeneratorStrategy );
			}
			//checkIfMatchingGenerator(gen, generatorType, generatorName);
			for ( Map.Entry<?,?> elt : gen.getParameters().entrySet() ) {
				if ( elt.getKey() == null ) {
					continue;
				}
				params.setProperty( (String) elt.getKey(), (String) elt.getValue() );
			}
		}
		if ( "assigned".equals( generatorType ) ) {
			id.setNullValue( "undefined" );
		}
		id.setIdentifierGeneratorProperties( params );
	}

	/**
	 * apply an id generator to a SimpleValue
	 */
	public static void makeIdGenerator(
			SimpleValue id,
			XProperty idXProperty,
			String generatorType,
			String generatorName,
			MetadataBuildingContext buildingContext,
			IdentifierGeneratorDefinition foreignKGeneratorDefinition) {
		Map<String, IdentifierGeneratorDefinition> localIdentifiers = null;
		if ( foreignKGeneratorDefinition != null ) {
			localIdentifiers = new HashMap<>();
			localIdentifiers.put( foreignKGeneratorDefinition.getName(), foreignKGeneratorDefinition );
		}
		makeIdGenerator( id, idXProperty, generatorType, generatorName, buildingContext, localIdentifiers );
	}

	private static IdentifierGeneratorDefinition getIdentifierGenerator(
			String name,
			XProperty idXProperty,
			Map<String, IdentifierGeneratorDefinition> localGenerators,
			MetadataBuildingContext buildingContext) {
		if ( localGenerators != null ) {
			final IdentifierGeneratorDefinition result = localGenerators.get( name );
			if ( result != null ) {
				return result;
			}
		}

		final IdentifierGeneratorDefinition globalDefinition =
				buildingContext.getMetadataCollector().getIdentifierGenerator( name );
		if ( globalDefinition != null ) {
			return globalDefinition;
		}

		log.debugf( "Could not resolve explicit IdentifierGeneratorDefinition - using implicit interpretation (%s)", name );

		// If we were unable to locate an actual matching named generator assume a sequence/table of the given name.
		//		this really needs access to the `jakarta.persistence.GenerationType` to work completely properly
		//
		// 		(the crux of HHH-12122)

		// temporarily, in lieu of having access to GenerationType, assume the EnhancedSequenceGenerator
		//		for the purpose of testing the feasibility of the approach

		final GeneratedValue generatedValueAnn = idXProperty.getAnnotation( GeneratedValue.class );
		if ( generatedValueAnn == null ) {
			// this should really never happen, but its easy to protect against it...
			return new IdentifierGeneratorDefinition( "assigned", "assigned" );
		}

		final IdGeneratorStrategyInterpreter generationInterpreter =
				buildingContext.getBuildingOptions().getIdGenerationTypeInterpreter();

		final GenerationType generationType = interpretGenerationType( generatedValueAnn );

		if ( generationType == null || generationType == GenerationType.SEQUENCE ) {
			// NOTE : `null` will ultimately be interpreted as "hibernate_sequence"
			log.debugf( "Building implicit sequence-based IdentifierGeneratorDefinition (%s)", name );
			final IdentifierGeneratorDefinition.Builder builder = new IdentifierGeneratorDefinition.Builder();
			generationInterpreter.interpretSequenceGenerator(
					new SequenceGenerator() {
						@Override
						public String name() {
							return name;
						}

						@Override
						public String sequenceName() {
							return "";
						}

						@Override
						public String catalog() {
							return "";
						}

						@Override
						public String schema() {
							return "";
						}

						@Override
						public int initialValue() {
							return 1;
						}

						@Override
						public int allocationSize() {
							return 50;
						}

						@Override
						public Class<? extends Annotation> annotationType() {
							return SequenceGenerator.class;
						}
					},
					builder
			);

			return builder.build();
		}
		else if ( generationType == GenerationType.TABLE ) {
			// NOTE : `null` will ultimately be interpreted as "hibernate_sequence"
			log.debugf( "Building implicit table-based IdentifierGeneratorDefinition (%s)", name );
			final IdentifierGeneratorDefinition.Builder builder = new IdentifierGeneratorDefinition.Builder();
			generationInterpreter.interpretTableGenerator(
					new TableGenerator() {
						@Override
						public String name() {
							return name;
						}

						@Override
						public String table() {
							return "";
						}

						@Override
						public int initialValue() {
							return 0;
						}

						@Override
						public int allocationSize() {
							return 50;
						}

						@Override
						public String catalog() {
							return "";
						}

						@Override
						public String schema() {
							return "";
						}

						@Override
						public String pkColumnName() {
							return "";
						}

						@Override
						public String valueColumnName() {
							return "";
						}

						@Override
						public String pkColumnValue() {
							return "";
						}

						@Override
						public UniqueConstraint[] uniqueConstraints() {
							return new UniqueConstraint[0];
						}

						@Override
						public Index[] indexes() {
							return new Index[0];
						}

						@Override
						public Class<? extends Annotation> annotationType() {
							return TableGenerator.class;
						}
					},
					builder
			);

			return builder.build();
		}


		// really AUTO and IDENTITY work the same in this respect, aside from the actual strategy name
		final String strategyName;
		if ( generationType == GenerationType.IDENTITY ) {
			strategyName = "identity";
		}
		else {
			strategyName = generationInterpreter.determineGeneratorName(
					generationType,
					new GeneratorNameDeterminationContext() {
						@Override
						public Class<?> getIdType() {
							return buildingContext
									.getBootstrapContext()
									.getReflectionManager()
									.toClass( idXProperty.getType() );
						}

						@Override
						public String getGeneratedValueGeneratorName() {
							return generatedValueAnn.generator();
						}
					}
			);
		}

		log.debugf( "Building implicit generic IdentifierGeneratorDefinition (%s) : %s", name, strategyName );
		return new IdentifierGeneratorDefinition(
				name,
				strategyName,
				Collections.singletonMap( IdentifierGenerator.GENERATOR_NAME, name )
		);
	}

	private static GenerationType interpretGenerationType(GeneratedValue generatedValueAnn) {
		if ( generatedValueAnn.strategy() == null ) {
			return GenerationType.AUTO;
		}

		return generatedValueAnn.strategy();
	}

	public static boolean isEmptyAnnotationValue(String annotationString) {
		return annotationString != null && annotationString.length() == 0;
		//equivalent to (but faster) ANNOTATION_STRING_DEFAULT.equals( annotationString );
	}

	public static boolean isEmptyOrNullAnnotationValue(String annotationString) {
		return annotationString == null || annotationString.length() == 0;
	}

	public static String getAnnotationValueStringOrNull(String value) {
		return isEmptyOrNullAnnotationValue( value )
				? null
				: value;
	}

	public static Any buildAnyValue(
			jakarta.persistence.Column discriminatorColumn,
			Formula discriminatorFormula,
			AnnotatedJoinColumn[] keyColumns,
			PropertyData inferredData,
			boolean cascadeOnDelete,
			boolean lazy,
			Nullability nullability,
			PropertyHolder propertyHolder,
			EntityBinder entityBinder,
			boolean optional,
			MetadataBuildingContext context) {
		final XProperty xProperty = inferredData.getProperty();

		final Any value = new Any( context, keyColumns[0].getTable(), true );
		value.setLazy( lazy );
		value.setCascadeDeleteEnabled( cascadeOnDelete );

		final BasicValueBinder<?> discriminatorValueBinder =
				new BasicValueBinder<>( BasicValueBinder.Kind.ANY_DISCRIMINATOR, context );

		final AnnotatedColumn[] discriminatorColumns = buildColumnOrFormulaFromAnnotation(
				discriminatorColumn,
				discriminatorFormula,
				null,
				nullability,
				propertyHolder,
				inferredData,
				entityBinder.getSecondaryTables(),
				context
		);
		assert discriminatorColumns.length == 1;
		discriminatorColumns[0].setTable( value.getTable() );
		discriminatorValueBinder.setColumns( discriminatorColumns );

		discriminatorValueBinder.setReturnedClassName( inferredData.getTypeName() );
		discriminatorValueBinder.setType( xProperty, xProperty.getType(), null, null );

		final BasicValue discriminatorDescriptor = discriminatorValueBinder.make();
		value.setDiscriminator( discriminatorDescriptor );
		discriminatorValueBinder.fillSimpleValue();
		discriminatorColumns[0].linkWithValue( discriminatorDescriptor );

		final JavaType<?> discriminatorJavaType = discriminatorDescriptor
				.resolve()
				.getRelationalJavaType();

		final Map<Object,Class<?>> discriminatorValueMappings = new HashMap<>();
		processAnyDiscriminatorValues(
				inferredData.getProperty(),
				(valueMapping) -> discriminatorValueMappings.put(
						discriminatorJavaType.wrap( valueMapping.discriminator(), null ),
						valueMapping.entity()
				)
		);
		value.setDiscriminatorValueMappings( discriminatorValueMappings );

		BasicValueBinder<?> keyValueBinder = new BasicValueBinder<>( BasicValueBinder.Kind.ANY_KEY, context );
		assert keyColumns.length == 1;
		keyColumns[0].setTable( value.getTable() );
		keyValueBinder.setColumns( keyColumns );

		if ( !optional ) {
			for (AnnotatedJoinColumn column : keyColumns) {
				column.setNullable( false );
			}
		}
		keyValueBinder.setType( xProperty, xProperty.getType(), null, null );
		final BasicValue keyDescriptor = keyValueBinder.make();
		value.setKey( keyDescriptor );
		keyValueBinder.fillSimpleValue();
		AnnotatedColumn.checkPropertyConsistency(
				keyColumns,
				propertyHolder.getEntityName() + "." + inferredData.getPropertyName()
		);
		keyColumns[0].linkWithValue( keyDescriptor );

		return value;
	}

	private static void processAnyDiscriminatorValues(
			XProperty property,
			Consumer<AnyDiscriminatorValue> consumer) {
		final AnyDiscriminatorValue valueAnn = property.getAnnotation( AnyDiscriminatorValue.class );
		if ( valueAnn != null ) {
			consumer.accept( valueAnn );
			return;
		}

		final AnyDiscriminatorValues valuesAnn = property.getAnnotation( AnyDiscriminatorValues.class );
		if ( valuesAnn != null ) {
			final AnyDiscriminatorValue[] valueAnns = valuesAnn.value();
			if ( valueAnns != null && valueAnns.length > 0 ) {
				for ( AnyDiscriminatorValue ann : valueAnns ) {
					consumer.accept(ann);
				}
			}
		}
	}

	public static MappedSuperclass getMappedSuperclassOrNull(
			XClass declaringClass,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			MetadataBuildingContext context) {
		boolean retrieve = false;
		if ( declaringClass != null ) {
			final InheritanceState inheritanceState = inheritanceStatePerClass.get( declaringClass );
			if ( inheritanceState == null ) {
				throw new AssertionFailure(
						"Declaring class is not found in the inheritance state hierarchy: " + declaringClass
				);
			}
			if ( inheritanceState.isEmbeddableSuperclass() ) {
				retrieve = true;
			}
		}

		if ( retrieve ) {
			return context.getMetadataCollector().getMappedSuperclass(
					context.getBootstrapContext().getReflectionManager().toClass( declaringClass )
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
		final XClass persistentXClass = buildingContext.getBootstrapContext().getReflectionManager()
					.toXClass( propertyHolder.getPersistentClass().getMappedClass() );
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
		return buildingContext.getMetadataCollector()
				.getPropertyAnnotatedWithMapsId( persistentXClass, isId ? "" : propertyName);
	}
	
	public static Map<String,String> toAliasTableMap(SqlFragmentAlias[] aliases){
		Map<String,String> ret = new HashMap<>();
		for ( SqlFragmentAlias aliase : aliases ) {
			if ( StringHelper.isNotEmpty( aliase.table() ) ) {
				ret.put( aliase.alias(), aliase.table() );
			}
		}
		return ret;
	}
	
	public static Map<String,String> toAliasEntityMap(SqlFragmentAlias[] aliases){
		Map<String,String> ret = new HashMap<>();
		for ( SqlFragmentAlias aliase : aliases ) {
			if ( aliase.entity() != void.class ) {
				ret.put( aliase.alias(), aliase.entity().getName() );
			}
		}
		return ret;
	}
}
