/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.model.IdGeneratorStrategyInterpreter;
import org.hibernate.boot.model.IdGeneratorStrategyInterpreter.GeneratorNameDeterminationContext;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.annotations.BasicValueBinder;
import org.hibernate.cfg.annotations.EntityBinder;
import org.hibernate.cfg.annotations.Nullability;
import org.hibernate.dialect.Dialect;
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
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

import static org.hibernate.cfg.AnnotatedJoinColumn.NON_PK_REFERENCE;
import static org.hibernate.cfg.AnnotatedJoinColumn.checkReferencedColumnsType;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies.EMBEDDED;
import static org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies.NOOP;
import static org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies.interpret;

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
		Property clone = new SyntheticProperty();
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

	/**
	 * Here we address a fundamental problem: the {@code @JoinColumn}
	 * annotation specifies the referenced column in the target table
	 * via {@code referencedColumnName}, but Hibernate needs to know
	 * which property or field of the target entity class holds the
	 * value of the referenced column at the Java level. (It's going
	 * to need the value when it writes the association.)
	 * <p>
	 * Complicating this hugely is the fact that an association might
	 * be based on a composite key with multiple {@code @JoinColumns},
	 * and so the referenced columns might even be spread out over
	 * multiple fields or properties of the target entity. There's
	 * even some extra minor complications resulting from multi-table
	 * inheritance and secondary tables.
	 * <p>
	 * The solution here is:
	 * <ul>
	 * <li>if the referenced columns correspond to exactly one property
	 *     of the target entity, we're good, just use it, or
	 * <li>otherwise, if a composite key is spread out over multiple
	 *     properties, then create a "synthetic" {@link Component} in
	 *     the model that aggregates these properties and is considered
	 *     the target of the association.
	 * </ul>
	 * Certain limitations arise from the way this solution is currently
	 * implemented: for example, if a referenced column belongs to a
	 * property of an {@code @Embeddable}, then every column of that
	 * embeddable must occur in the list of referenced columns, and the
	 * order of the columns must line up! Some of these limitations
	 * could be relaxed using by writing a better algorithm for building
	 * the synthetic {@link Component}.
	 */
	public static void createSyntheticPropertyReference(
			AnnotatedJoinColumn[] columns,
			// the target entity of the association, to which the columns belong
			PersistentClass targetEntity,
			// the entity which declares the association (used for exception message)
			PersistentClass associatedEntity,
			Value value,
			String propertyName,
			// true when we do the reverse side of a @ManyToMany
			boolean inverse,
			MetadataBuildingContext context) {

		// TODO: instead of pulling info like the property name and whether
		//       it's on the owning side off the zeroth column coming in, we
		//       should receive it directly in the argument list, or from a
		//       Property instance
		final AnnotatedJoinColumn firstColumn = columns[0];
		if ( !firstColumn.isImplicit()
				// not necessary for a primary key reference
				&& checkReferencedColumnsType( columns, targetEntity, context ) == NON_PK_REFERENCE ) {

			// all the columns have to belong to the same table;
			// figure out which table has the columns by looking
			// for a PersistentClass or Join in the hierarchy of
			// the target entity which has the first column
			final Object columnOwner = findColumnOwner( targetEntity, firstColumn.getReferencedColumn(), context );
			checkColumnInSameTable( columns, targetEntity, associatedEntity, context, columnOwner );
			// find all properties mapped to each column
			final List<Property> properties = findPropertiesByColumns( columnOwner, columns, associatedEntity, context );
			// create a Property along with the new synthetic
			// Component if necessary (or reuse the existing
			// Property that matches exactly)
			final Property property = referencedProperty(
					targetEntity,
					associatedEntity,
					propertyName,
					inverse,
					columnOwner,
					properties,
					context
			);
			// register the mapping with the InFlightMetadataCollector
			registerSyntheticProperty(
					targetEntity,
					value,
					inverse,
					firstColumn.getPropertyHolder().getPersistentClass(),
					firstColumn.getPropertyName(),
					property.getName(),
					context
			);
		}
	}

	/**
	 * All the referenced columns must belong to the same table, that is
	 * {@link #findColumnOwner(PersistentClass, String, MetadataBuildingContext)}
	 * must return the same value for all of them.
	 */
	private static void checkColumnInSameTable(
			AnnotatedJoinColumn[] columns,
			PersistentClass targetEntity,
			PersistentClass associatedEntity,
			MetadataBuildingContext context,
			Object columnOwner) {
		final AnnotatedJoinColumn firstColumn = columns[0];
		for ( AnnotatedJoinColumn column: columns) {
			if ( column.hasMappedBy() ) {
				// we should only get called for owning side of association
				throw new AssertionFailure("no need to create synthetic properties for unowned collections");
			}
			final Object owner = findColumnOwner( targetEntity, column.getReferencedColumn(), context );
			if ( owner == null ) {
				throw new AnnotationException( "A '@JoinColumn' for association "
						+ associationMessage(associatedEntity, firstColumn)
						+ " references a column named '" + column.getReferencedColumn()
						+ "' which is not mapped by the target entity '"
						+ targetEntity.getEntityName() + "'" );
			}
			if ( owner != columnOwner) {
				throw new AnnotationException( "The '@JoinColumn's for association "
						+ associationMessage(associatedEntity, firstColumn)
						+ " reference columns of different tables mapped by the target entity '"
						+ targetEntity.getEntityName() + "' ('" + column.getReferencedColumn() +
						"' belongs to a different table to '" + firstColumn.getReferencedColumn() + "'" );
			}
		}
	}

	/**
	 * If the referenced columns correspond to exactly one property
	 * of the primary table of the exact target entity subclass,
	 * just use that property. Otherwise, if a composite key is
	 * spread out over multiple properties, then create a "synthetic"
	 * {@link Component} that aggregates these properties and is
	 * considered the target of the association. This method adds
	 * the property holding the synthetic component to the target
	 * entity {@link PersistentClass} by side effect.
	 */
	private static Property referencedProperty(
			PersistentClass ownerEntity,
			PersistentClass associatedEntity,
			String propertyName,
			boolean inverse,
			Object columnOwner,
			List<Property> properties,
			MetadataBuildingContext context) {
		if ( properties.size() == 1
				// necessary to handle the case where the columnOwner is a supertype
				&& ownerEntity == columnOwner
				//TODO: this is only necessary because of a NotYetImplementedFor6Exception
				//      in MappingMetamodelCreationHelper.interpretToOneKeyDescriptor
				//      and ideally we should remove this last condition once that is fixed
				&& !( properties.get(0).getValue() instanceof ToOne ) ) {
			// no need to make a synthetic property
			return properties.get(0);
		}
		else {
			// Create a synthetic Property whose Value is a synthetic
			// embeddable component containing the target properties
			// mapped to the referenced columns. We need to shallow
			// clone those properties to mark them as non-insertable
			// and non-updatable
			final String syntheticPropertyName = syntheticPropertyName( propertyName, inverse, associatedEntity );
			return makeSyntheticComponentProperty( ownerEntity, columnOwner, context, syntheticPropertyName, properties );
		}
	}

	private static void registerSyntheticProperty(
			PersistentClass ownerEntity,
			Value value,
			boolean inverse,
			PersistentClass associatedClass,
			String propertyName,
			String syntheticPropertyName,
			MetadataBuildingContext context) {
		if ( value instanceof ToOne ) {
			( (ToOne) value).setReferencedPropertyName( syntheticPropertyName );
			( (ToOne) value).setReferenceToPrimaryKey( false );
			context.getMetadataCollector().addUniquePropertyReference(
					ownerEntity.getEntityName(),
					syntheticPropertyName
			);
		}
		else if ( value instanceof Collection ) {
			( (Collection) value).setReferencedPropertyName( syntheticPropertyName );
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
				propertyName,
				syntheticPropertyName
		);
	}

	private static String syntheticPropertyName(
			String propertyName,
			boolean inverse,
			PersistentClass associatedClass) {
		String syntheticPropertyName =
				"_" + associatedClass.getEntityName().replace('.', '_') +
				"_" + propertyName.replace('.', '_');
		if ( inverse ) {
			// Use a different name for inverse synthetic properties to avoid duplicate properties for self-referencing models
			syntheticPropertyName += "_inverse";
		}
		return syntheticPropertyName;
	}

	private static String associationMessage(PersistentClass associatedEntity, AnnotatedJoinColumn firstColumn) {
		StringBuilder message = new StringBuilder();
		if ( associatedEntity != null ) {
			message.append( "'" )
					.append( associatedEntity.getEntityName() )
					.append( "." )
					.append( firstColumn.getPropertyName() )
					.append( "'" );
		}
		else {
			if ( firstColumn.getPropertyHolder() != null ) {
				message.append( "'" )
						.append( firstColumn.getPropertyHolder().getEntityName() )
						.append( "." )
						.append( firstColumn.getPropertyName() )
						.append( "'" );
			}
		}
		return message.toString();
	}

	private static Property makeSyntheticComponentProperty(
			PersistentClass ownerEntity,
			Object persistentClassOrJoin,
			MetadataBuildingContext context,
			String syntheticPropertyName,
			List<Property> properties) {
		Component embeddedComp = persistentClassOrJoin instanceof PersistentClass
				? new Component( context, (PersistentClass) persistentClassOrJoin )
				: new Component( context, (Join) persistentClassOrJoin );
		embeddedComp.setComponentClassName( embeddedComp.getOwner().getClassName() );
		embeddedComp.setEmbedded( true );
		Property property = makeComponent( ownerEntity, context, syntheticPropertyName, embeddedComp, properties );
		property.setPropertyAccessorName( "embedded" );
		ownerEntity.addProperty( property );
		embeddedComp.createUniqueKey(); //make it unique
		return property;
	}

	private static Property makeComponent(
			PersistentClass ownerEntity,
			MetadataBuildingContext context,
			String name,
			Component embeddedComp,
			List<Property> properties) {
		for ( Property property : properties ) {
			Property clone = cloneProperty( ownerEntity, context, property );
			embeddedComp.addProperty( clone );
		}
		embeddedComp.sortProperties();
		Property synthProp = new SyntheticProperty();
		synthProp.setName( name );
		synthProp.setPersistentClass( ownerEntity );
		synthProp.setUpdateable( false );
		synthProp.setInsertable( false );
		synthProp.setValue( embeddedComp );
		return synthProp;
	}

	private static Property cloneProperty(PersistentClass ownerEntity, MetadataBuildingContext context, Property property) {
		if ( property.isComposite() ) {
			Component component = (Component) property.getValue();
			Component copy = new Component( context, component );
			copy.setComponentClassName( component.getComponentClassName() );
			copy.setEmbedded( component.isEmbedded() );
			Property clone = makeComponent( ownerEntity, context, property.getName(), copy, component.getProperties() );
			clone.setPropertyAccessorName( property.getPropertyAccessorName() );
			return clone;
		}
		else {
			Property clone = shallowCopy( property );
			clone.setInsertable( false );
			clone.setUpdateable( false );
			clone.setNaturalIdentifier( false );
			clone.setValueGenerationStrategy( property.getValueGenerationStrategy() );
			return clone;
		}
	}

	private static List<Property> findPropertiesByColumns(
			Object columnOwner,
			AnnotatedJoinColumn[] columns,
			PersistentClass associatedEntity,
			MetadataBuildingContext context) {

		final Table referencedTable;
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

		// Build the list of column names in the exact order they were
		// specified by the @JoinColumn annotations.
		final List<Column> orderedColumns = new ArrayList<>( columns.length );
		final Map<Column, Set<Property>> columnsToProperty = new HashMap<>();
		for ( AnnotatedJoinColumn joinColumn : columns ) {
			Column column = new Column(
					context.getMetadataCollector().getPhysicalColumnName(
							referencedTable,
							joinColumn.getReferencedColumn()
					)
			);
			orderedColumns.add( column );
			columnsToProperty.put( column, new HashSet<>() );
		}

		// Now, for each column find the properties of the target entity
		// which are mapped to that column. (There might be multiple such
		// properties for each column.)
		if ( columnOwner instanceof PersistentClass ) {
			PersistentClass persistentClass = (PersistentClass) columnOwner;
			for ( Property property : persistentClass.getProperties() ) {
				matchColumnsByProperty( property, columnsToProperty );
			}
			if ( persistentClass.hasIdentifierProperty() ) {
				matchColumnsByProperty( persistentClass.getIdentifierProperty(), columnsToProperty );
			}
			else {
				// special case for entities with multiple @Id properties
				Component key = persistentClass.getIdentifierMapper();
				for ( Property p : key.getProperties() ) {
					matchColumnsByProperty( p, columnsToProperty );
				}
			}
		}
		else {
			for ( Property property : ((Join) columnOwner).getProperties() ) {
				matchColumnsByProperty( property, columnsToProperty );
			}
		}

		// Now we need to line up the properties with the columns in the
		// same order they were specified by the @JoinColumn annotations
		// this is very tricky because a single property might span
		// multiple columns.
		// TODO: For now we only consider the first property that matched
		//       each column, but this means we will reject some mappings
		//       that could be made to work for a different choice of
		//       properties (it's also not very deterministic)
		List<Property> orderedProperties = new ArrayList<>();
		int lastPropertyColumnIndex = 0;
		Property currentProperty = null;
		for ( Column column : orderedColumns ) {
			Set<Property> properties = columnsToProperty.get( column );
			if ( properties.isEmpty() ) {
				// no property found which maps to this column
				throw new AnnotationException( "Referenced column '" + column.getName()
						+ "' in '@JoinColumn' for " + associationMessage( associatedEntity, columns[0] )
						+ " is not mapped by any property of the target entity" );
			}
			for ( Property property : properties ) {
				if ( property == currentProperty ) {
					// we have the next column of the previous property
					if ( !property.getColumns().get( lastPropertyColumnIndex ).equals( column ) ) {
						// the columns have to occur in the right order in the property
						throw new AnnotationException( "Referenced column '" + column.getName()
								+ "' mapped by target property '" + property.getName()
								+ "' occurs out of order in the list of '@JoinColumn's for association "
								+ associationMessage( associatedEntity, columns[0] ) );
					}
					lastPropertyColumnIndex++;
					if ( lastPropertyColumnIndex == currentProperty.getColumnSpan() ) {
						//we have exhausted the columns in this property
						currentProperty = null;
						lastPropertyColumnIndex = 0;
					}
				}
				else if ( currentProperty != null ) {
					// we didn't use up all the columns of the previous property
					throw new AnnotationException( "Target property '" + property.getName() + "' has "
							+ property.getColumnSpan() + " columns which must be referenced by a '@JoinColumn' for "
							+ associationMessage( associatedEntity, columns[0] )
							+ " (every column mapped by '" + property.getName()
							+ "' must occur exactly once as a 'referencedColumnName', and in the correct order)" );
				}
				else if ( orderedProperties.contains( property ) ) {
					// we already used up all the columns of this property
					throw new AnnotationException( "Target property '" + property.getName() + "' has only "
							+ property.getColumnSpan() + " columns which may be referenced by a '@JoinColumn' for "
							+ associationMessage( associatedEntity, columns[0] )
							+ " (each column mapped by '" + property.getName()
							+ "' may only occur once as a 'referencedColumnName')" );

				}
				else {
					// we have the first column of a new property
					orderedProperties.add( property );
					if ( property.getColumnSpan() > 1 ) {
						if ( !property.getColumns().get(0).equals( column ) ) {
							// the columns have to occur in the right order in the property
							throw new AnnotationException("Referenced column '" + column.getName()
									+ "' mapped by target property '" + property.getName()
									+ "' occurs out of order in the list of '@JoinColumn's");
						}
						currentProperty = property;
						lastPropertyColumnIndex = 1;
					}
				}
				break; // we're only considering the first matching property for now
			}
		}
		return orderedProperties;
	}

	private static void matchColumnsByProperty(Property property, Map<Column, Set<Property>> columnsToProperty) {
		if ( property != null
				&& NOOP != interpret( property.getPropertyAccessorName() )
				&& EMBEDDED != interpret( property.getPropertyAccessorName() ) ) {
			//TODO: we can't return subproperties because the caller
			//      needs top level properties, but this results in
			//      a limitation where I need to be referencing all
			//      columns of an embeddable instead of just some
//			if ( property.isComposite() ) {
//				for ( Property sp : ( (Component) property.getValue() ).getProperties() ) {
//					matchColumnsByProperty( sp, columnsToProperty );
//				}
//			}
//			else {
			for ( Selectable selectable : property.getSelectables() ) {
				//can be a Formula, so we don't cast
				//noinspection SuspiciousMethodCalls
				if ( columnsToProperty.containsKey( selectable ) ) {
					//noinspection SuspiciousMethodCalls
					columnsToProperty.get( selectable ).add( property );
				}
			}
//			}
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
		if ( isEmpty( columnName ) ) {
			//shortcut for implicit referenced column names
			return persistentClass;
		}
		PersistentClass current = persistentClass;
		while ( current != null ) {
			try {
				context.getMetadataCollector().getPhysicalColumnName( current.getTable(), columnName );
				return current;
			}
			catch (MappingException me) {
				//swallow it
			}
			for ( Join join : current.getJoins() ) {
				try {
					context.getMetadataCollector().getPhysicalColumnName( join.getTable(), columnName );
					return join;
				}
				catch (MappingException me) {
					//swallow it
				}
			}
			current = current.getSuperclass();
		}
		return null;
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

		final Map<String,Object> params = new HashMap<>();

		//always settable
		params.put( PersistentIdentifierGenerator.TABLE, table.getName() );

		if ( id.getColumnSpan() == 1 ) {
			params.put( PersistentIdentifierGenerator.PK, id.getColumns().get(0).getName() );
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
				throw new AnnotationException( "No id generator was declared with the name '" + generatorName
						+ "' specified by '@GeneratedValue'"
						+ " (define a named generator using '@SequenceGenerator', '@TableGenerator', or '@GenericGenerator')" );
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
			for ( Map.Entry<String,String> elt : gen.getParameters().entrySet() ) {
				if ( elt.getKey() != null ) {
					params.put( elt.getKey(), elt.getValue() );
				}
			}
		}
		if ( "assigned".equals( generatorType ) ) {
			id.setNullValue( "undefined" );
		}
		id.setIdentifierGeneratorParameters( params );
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
			// this should really never happen, but it's easy to protect against it...
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
		return isEmptyOrNullAnnotationValue( value ) ? null : value;
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

		final BasicValueBinder discriminatorValueBinder =
				new BasicValueBinder( BasicValueBinder.Kind.ANY_DISCRIMINATOR, context );

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

		BasicValueBinder keyValueBinder = new BasicValueBinder( BasicValueBinder.Kind.ANY_KEY, context );
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
		final InFlightMetadataCollector metadataCollector = buildingContext.getMetadataCollector();
		if ( propertyHolder.isInIdClass() ) {
			PropertyData pd = metadataCollector.getPropertyAnnotatedWithIdAndToOne( persistentXClass, propertyName );
			if ( pd == null && buildingContext.getBuildingOptions().isSpecjProprietarySyntaxEnabled() ) {
				pd = metadataCollector.getPropertyAnnotatedWithMapsId( persistentXClass, propertyName );
			}
			return pd;
		}
		else {
			return metadataCollector.getPropertyAnnotatedWithMapsId( persistentXClass, isId ? "" : propertyName);
		}
	}
	
	public static Map<String,String> toAliasTableMap(SqlFragmentAlias[] aliases){
		Map<String,String> ret = new HashMap<>();
		for ( SqlFragmentAlias aliase : aliases ) {
			if ( isNotEmpty( aliase.table() ) ) {
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

	public static boolean hasToOneAnnotation(XAnnotatedElement property) {
		return property.isAnnotationPresent(ManyToOne.class)
			|| property.isAnnotationPresent(OneToOne.class);
	}

	public static <T extends Annotation> T getOverridableAnnotation(
			XAnnotatedElement element,
			Class<T> annotationType,
			MetadataBuildingContext context) {
		Dialect dialect = context.getMetadataCollector().getDatabase().getDialect();
		Iterator<Annotation> annotations =
				Arrays.stream( element.getAnnotations() )
						.flatMap(annotation -> {
							try {
								Method value = annotation.annotationType().getDeclaredMethod("value");
								Class<?> returnType = value.getReturnType();
								if ( returnType.isArray()
										&& returnType.getComponentType().isAnnotationPresent(Repeatable.class)
										&& returnType.getComponentType().isAnnotationPresent(DialectOverride.OverridesAnnotation.class) ) {
									return Stream.of( (Annotation[]) value.invoke(annotation) );
								}
							}
							catch (NoSuchMethodException ignored) {}
							catch (Exception e) {
								throw new AssertionFailure("could not read @DialectOverride annotation", e);
							}
							return Stream.of(annotation);
						}).iterator();
		while ( annotations.hasNext() ) {
			Annotation annotation = annotations.next();
			Class<? extends Annotation> type = annotation.annotationType();
			DialectOverride.OverridesAnnotation overridesAnnotation = type.getAnnotation(DialectOverride.OverridesAnnotation.class);
			if ( overridesAnnotation != null
					&& overridesAnnotation.value().equals(annotationType) ) {
				try {
					//noinspection unchecked
					Class<? extends Dialect> overrideDialect = (Class<? extends Dialect>)
							type.getDeclaredMethod("dialect").invoke(annotation);
					if ( overrideDialect.isAssignableFrom( dialect.getClass() ) ) {
						DialectOverride.Version before = (DialectOverride.Version)
								type.getDeclaredMethod("before").invoke(annotation);
						DialectOverride.Version sameOrAfter = (DialectOverride.Version)
								type.getDeclaredMethod("sameOrAfter").invoke(annotation);
						if ( dialect.getVersion().isBefore( before.major(), before.minor() )
							&& dialect.getVersion().isSameOrAfter( sameOrAfter.major(), sameOrAfter.minor() ) ) {
							//noinspection unchecked
							return (T) type.getDeclaredMethod("override").invoke(annotation);
						}
					}
				}
				catch (Exception e) {
					throw new AssertionFailure("could not read @DialectOverride annotation", e);
				}
			}
		}
		return element.getAnnotation( annotationType );
	}
}
