/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.annotations.AnyDiscriminatorImplicitValues;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.AttributeContainer;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SyntheticProperty;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.type.descriptor.java.JavaType;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Consumer;

import static jakarta.persistence.ConstraintMode.NO_CONSTRAINT;
import static jakarta.persistence.ConstraintMode.PROVIDER_DEFAULT;
import static java.util.Collections.addAll;
import static org.hibernate.boot.model.internal.AnnotatedColumn.buildColumnOrFormulaFromAnnotation;
import static org.hibernate.boot.model.internal.AnyBinder.resolveImplicitDiscriminatorStrategy;
import static org.hibernate.boot.model.internal.ForeignKeyType.NON_PRIMARY_KEY_REFERENCE;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotBlank;
import static org.hibernate.internal.util.StringHelper.qualifier;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.internal.util.collections.ArrayHelper.isEmpty;
import static org.hibernate.models.spi.TypeDetailsHelper.resolveRawClass;
import static org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies.EMBEDDED;
import static org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies.NOOP;
import static org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies.interpret;

/**
 * @author Emmanuel Bernard
 */
public class BinderHelper {

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

	public static boolean isPrimitive(String elementTypeName) {
		return PRIMITIVE_NAMES.contains( elementTypeName );
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
	 * be based on a composite key with multiple {@code @JoinColumn}s,
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
	 * <p>
	 * Certain limitations arise from the way this solution is currently
	 * implemented: for example, if a referenced column belongs to a
	 * property of an {@code @Embeddable}, then every column of that
	 * embeddable must occur in the list of referenced columns, and the
	 * order of the columns must line up! Some of these limitations
	 * could be relaxed by writing a better algorithm for building the
	 * synthetic {@link Component}.
	 */
	public static void createSyntheticPropertyReference(
			AnnotatedJoinColumns joinColumns,
			// the target entity of the association, to which the columns belong
			PersistentClass targetEntity,
			// the entity which declares the association (used for exception message)
			PersistentClass associatedEntity,
			Value value,
			String propertyName,
			// true when we do the reverse side of a @ManyToMany
			boolean inverse,
			MetadataBuildingContext context) {
		// this work is not necessary for a primary key reference
		if ( joinColumns.getReferencedColumnsType( targetEntity ) == NON_PRIMARY_KEY_REFERENCE ) { // && !firstColumn.isImplicit()
			// all the columns have to belong to the same table;
			// figure out which table has the columns by looking
			// for a PersistentClass or Join in the hierarchy of
			// the target entity which has the first column
			final AttributeContainer columnOwner =
					findReferencedColumnOwner( targetEntity, joinColumns.getJoinColumns().get(0), context );
			checkColumnInSameTable( joinColumns, targetEntity, associatedEntity, context, columnOwner );
			// find all properties mapped to each column
			final List<Property> properties =
					findPropertiesByColumns( columnOwner, joinColumns, associatedEntity, context );
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
					associatedEntity,
					propertyName,
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
			AnnotatedJoinColumns joinColumns,
			PersistentClass targetEntity,
			PersistentClass associatedEntity,
			MetadataBuildingContext context,
			AttributeContainer columnOwner) {
		if ( joinColumns.hasMappedBy() ) {
			// we should only get called for owning side of association
			throw new AssertionFailure("no need to create synthetic properties for unowned collections");
		}
		for ( AnnotatedJoinColumn column: joinColumns.getJoinColumns() ) {
			final AttributeContainer owner = findReferencedColumnOwner( targetEntity, column, context );
			if ( owner == null ) {
				throw new AnnotationException( "A '@JoinColumn' for association "
						+ associationMessage( associatedEntity, joinColumns )
						+ " references a column named '" + column.getReferencedColumn()
						+ "' which is not mapped by the target entity '"
						+ targetEntity.getEntityName() + "'" );
			}
			if ( owner != columnOwner) {
				final AnnotatedJoinColumn firstColumn = joinColumns.getJoinColumns().get(0);
				throw new AnnotationException( "The '@JoinColumn's for association "
						+ associationMessage( associatedEntity, joinColumns )
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
			AttributeContainer columnOwner,
			List<Property> properties,
			MetadataBuildingContext context) {
		if ( properties.size() == 1
				// necessary to handle the case where the columnOwner is a supertype
				&& ownerEntity == columnOwner
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
		if ( value instanceof ToOne toOne ) {
			toOne.setReferencedPropertyName( syntheticPropertyName );
			toOne.setReferenceToPrimaryKey( false );
			context.getMetadataCollector().addUniquePropertyReference(
					ownerEntity.getEntityName(),
					syntheticPropertyName
			);
		}
		else if ( value instanceof Collection collection ) {
			collection.setReferencedPropertyName( syntheticPropertyName );
			//not unique because we could create a mtm wo association table
			context.getMetadataCollector().addPropertyReference(
					ownerEntity.getEntityName(),
					syntheticPropertyName
			);
		}
		else {
			throw new AssertionFailure( "Property ref on an unexpected Value type: " + value.getClass().getName() );
		}
		final String associatedEntityName = associatedClass.getEntityName();
		final String generatedName = inverse ? "inverse__" + associatedEntityName : associatedEntityName;
		context.getMetadataCollector()
				.addPropertyReferencedAssociation( generatedName, propertyName, syntheticPropertyName );
	}

	private static String syntheticPropertyName(
			String propertyName,
			boolean inverse,
			PersistentClass associatedClass) {
		final String syntheticPropertyName =
				( "_" + associatedClass.getEntityName() + "_" + propertyName )
						.replace('.', '_');
		// Use a different name for inverse synthetic properties to
		// avoid duplicate properties for self-referencing models
		return inverse ? syntheticPropertyName + "_inverse" : syntheticPropertyName;
	}

	private static String associationMessage(PersistentClass associatedEntity, AnnotatedJoinColumns joinColumns) {
		if ( associatedEntity != null ) {
			return "'" + associatedEntity.getEntityName() + "." + joinColumns.getPropertyName() + "'";
		}
		else {
			final PropertyHolder propertyHolder = joinColumns.getPropertyHolder();
			if ( propertyHolder != null ) {
				return "'" + propertyHolder.getEntityName() + "." + joinColumns.getPropertyName() + "'";
			}
			else {
				return "";
			}
		}
	}

	/**
	 * Build the "synthetic" {@link Component} that holds all the
	 * properties referenced by a foreign key mapping specified as
	 * a list of {@link jakarta.persistence.JoinColumn} annotations
	 * with explicit {@code referencedColumnName()}s.
	 */
	private static Property makeSyntheticComponentProperty(
			PersistentClass ownerEntity,
			AttributeContainer persistentClassOrJoin,
			MetadataBuildingContext context,
			String syntheticPropertyName,
			List<Property> properties) {
		final Component embeddedComponent = persistentClassOrJoin instanceof PersistentClass
				? new Component( context, (PersistentClass) persistentClassOrJoin )
				: new Component( context, (Join) persistentClassOrJoin );
		embeddedComponent.setComponentClassName( embeddedComponent.getOwner().getClassName() );
		embeddedComponent.setEmbedded( true );
		for ( Property property : properties ) {
			embeddedComponent.addProperty( cloneProperty( ownerEntity, context, property ) );
		}
		embeddedComponent.sortProperties();
		final Property result = new SyntheticProperty();
		result.setName( syntheticPropertyName );
		result.setPersistentClass( ownerEntity );
		result.setUpdateable( false );
		result.setInsertable( false );
		result.setValue( embeddedComponent );
		result.setPropertyAccessorName( "embedded" );
		if ( persistentClassOrJoin instanceof Join ) {
			// the referenced column is in the joined table, add the synthetic property there
			persistentClassOrJoin.addProperty( result );
		}
		else {
			ownerEntity.addProperty( result );
		}
		embeddedComponent.createUniqueKey( context ); //make it unique
		return result;
	}

	/**
	 * Create a (deep) copy of the {@link Property}, by also recursively
	 * cloning any child {@link Component} instances, but reusing other
	 * kinds of {@link Value} and other attributes.
	 */
	private static Property cloneProperty(PersistentClass ownerEntity, MetadataBuildingContext context, Property property) {
		if ( property.isComposite() ) {
			final Component component = (Component) property.getValue();
			final Component copy = new Component( context, component );
			copy.setComponentClassName( component.getComponentClassName() );
			copy.setEmbedded( component.isEmbedded() );
			for ( Property subproperty : component.getProperties() ) {
				copy.addProperty( cloneProperty( ownerEntity, context, subproperty ) );
			}
			copy.sortProperties();
			final Property result = new SyntheticProperty();
			result.setName( property.getName() );
			result.setPersistentClass( ownerEntity );
			result.setUpdateable( false );
			result.setInsertable( false );
			result.setValue(copy);
			result.setPropertyAccessorName( property.getPropertyAccessorName() );
			return result;
		}
		else {
			final Property clone = shallowCopy( property );
			clone.setInsertable( false );
			clone.setUpdateable( false );
			clone.setNaturalIdentifier( false );
			clone.setValueGeneratorCreator( property.getValueGeneratorCreator() );
			return clone;
		}
	}

	/**
	 * Create a copy of the {@link Property}, reusing the same {@link Value}
	 * and other attributes.
	 */
	public static Property shallowCopy(Property property) {
		final Property clone = new SyntheticProperty();
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

	private static List<Property> findPropertiesByColumns(
			AttributeContainer columnOwner,
			AnnotatedJoinColumns columns,
			PersistentClass associatedEntity,
			MetadataBuildingContext context) {

		final Table referencedTable = columnOwner.getTable();

		// Build the list of column names in the exact order they were
		// specified by the @JoinColumn annotations.
		final List<Column> orderedColumns = new ArrayList<>( columns.getJoinColumns().size() );
		final Map<Column, Set<Property>> columnsToProperty = new HashMap<>();
		final InFlightMetadataCollector collector = context.getMetadataCollector();
		for ( AnnotatedJoinColumn joinColumn : columns.getJoinColumns() ) {
			if ( joinColumn.isReferenceImplicit() ) {
				throw new AnnotationException("Association " + associationMessage( associatedEntity, columns )
						+ " has a '@JoinColumn' which does not specify the 'referencedColumnName'"
						+ " (when an association has multiple '@JoinColumn's, they must each specify their 'referencedColumnName')");
			}
			final String name = collector.getPhysicalColumnName( referencedTable, joinColumn.getReferencedColumn() );
			final Column column = new Column( name );
			orderedColumns.add( column );
			columnsToProperty.put( column, new LinkedHashSet<>() ); //need to use a LinkedHashSet here to make it deterministic
		}

		// Now, for each column find the properties of the target entity
		// which are mapped to that column. (There might be multiple such
		// properties for each column.)
		if ( columnOwner instanceof PersistentClass persistentClass ) {
			// Process ToOne associations after Components, Basic and Id properties
			final List<Property> toOneProperties = new ArrayList<>();
			for ( Property property : persistentClass.getReferenceableProperties() ) {
				if ( property.getValue() instanceof ToOne ) {
					toOneProperties.add( property );
				}
				else {
					matchColumnsByProperty( property, columnsToProperty );
				}
			}
			if ( persistentClass.hasIdentifierProperty() ) {
				matchColumnsByProperty( persistentClass.getIdentifierProperty(), columnsToProperty );
			}
			else {
				// special case for entities with multiple @Id properties
				final Component key = persistentClass.getIdentifierMapper();
				for ( Property p : key.getProperties() ) {
					matchColumnsByProperty( p, columnsToProperty );
				}
			}
			for ( Property property : toOneProperties ) {
				matchColumnsByProperty( property, columnsToProperty );
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
		final List<Property> orderedProperties = new ArrayList<>();
		int lastPropertyColumnIndex = 0;
		Property currentProperty = null;
		for ( Column column : orderedColumns ) {
			final Set<Property> properties = columnsToProperty.get( column );
			if ( properties.isEmpty() ) {
				// no property found which maps to this column
				throw new AnnotationException( "Referenced column '" + column.getName()
						+ "' in '@JoinColumn' for " + associationMessage( associatedEntity, columns )
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
								+ associationMessage( associatedEntity, columns ) );
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
							+ associationMessage( associatedEntity, columns )
							+ " (every column mapped by '" + property.getName()
							+ "' must occur exactly once as a 'referencedColumnName', and in the correct order)" );
				}
				else if ( orderedProperties.contains( property ) ) {
					// we already used up all the columns of this property
					throw new AnnotationException( "Target property '" + property.getName() + "' has only "
							+ property.getColumnSpan() + " columns which may be referenced by a '@JoinColumn' for "
							+ associationMessage( associatedEntity, columns )
							+ " (each column mapped by '" + property.getName()
							+ "' may only occur once as a 'referencedColumnName')" );

				}
				else {
					// we have the first column of a new property
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
					orderedProperties.add( property );
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
		final Property idProperty = associatedClass.getIdentifierProperty();
		final String idName = idProperty == null ? null : idProperty.getName();
		try {
			if ( isEmpty( propertyName ) || propertyName.equals( idName ) ) {
				//default to id
				property = idProperty;
			}
			else {
				if ( propertyName.indexOf( idName + "." ) == 0 ) {
					property = idProperty;
					propertyName = propertyName.substring( idName.length() + 1 );
				}
				final StringTokenizer tokens = new StringTokenizer( propertyName, ".", false );
				while ( tokens.hasMoreElements() ) {
					String element = (String) tokens.nextElement();
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
		catch ( MappingException e ) {
			try {
				//if we do not find it try to check the identifier mapper
				if ( associatedClass.getIdentifierMapper() == null ) {
					return null;
				}
				final StringTokenizer tokens = new StringTokenizer( propertyName, ".", false );
				while ( tokens.hasMoreElements() ) {
					final String element = (String) tokens.nextElement();
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
			catch ( MappingException ee ) {
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
			if ( isEmpty( propertyName ) ) {
				// Do not expect to use a primary key for this case
				return null;
			}
			else {
				final StringTokenizer tokens = new StringTokenizer( propertyName, ".", false );
				while ( tokens.hasMoreElements() ) {
					final String element = (String) tokens.nextElement();
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
				final StringTokenizer tokens = new StringTokenizer( propertyName, ".", false );
				while ( tokens.hasMoreElements() ) {
					final String element = (String) tokens.nextElement();
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
		else {
			final String path = propertyHolder.getPath();
			final String entityName = propertyHolder.getPersistentClass().getEntityName();
			return path.length() == entityName.length()
					? propertyName
					: qualify( path.substring(entityName.length() + 1), propertyName );
		}
	}

	public static AttributeContainer findReferencedColumnOwner(
			PersistentClass persistentClass,
			AnnotatedJoinColumn joinColumn,
			MetadataBuildingContext context) {
		return joinColumn.isImplicit() || joinColumn.isReferenceImplicit()
				? persistentClass
				: findColumnOwner( persistentClass, joinColumn.getReferencedColumn(), context );
	}

	/**
	 * Find the column owner (ie PersistentClass or Join) of columnName.
	 * If columnName is null or empty, persistentClass is returned
	 */
	public static AttributeContainer findColumnOwner(
			PersistentClass persistentClass,
			String columnName,
			MetadataBuildingContext context) {
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

	public static Any buildAnyValue(
			jakarta.persistence.Column discriminatorColumn,
			Formula discriminatorFormula,
			AnnotatedJoinColumns keyColumns,
			PropertyData inferredData,
			OnDeleteAction onDeleteAction,
			boolean lazy,
			Nullability nullability,
			PropertyHolder propertyHolder,
			EntityBinder entityBinder,
			boolean optional,
			MetadataBuildingContext context) {
		final MemberDetails property = inferredData.getAttributeMember();

		final Any value = new Any( context, keyColumns.getTable(), true );
		value.setLazy( lazy );
		value.setOnDeleteAction( onDeleteAction );

		final BasicValueBinder discriminatorValueBinder =
				new BasicValueBinder( BasicValueBinder.Kind.ANY_DISCRIMINATOR, context );

		// TODO: if there can be only one discriminator column,
		//       why are we making a whole array of them??
		final AnnotatedColumns discriminatorColumns = buildColumnOrFormulaFromAnnotation(
				discriminatorColumn,
				discriminatorFormula,
				null,
//				null,
				nullability,
				propertyHolder,
				inferredData,
				entityBinder.getSecondaryTables(),
				context
		);
		assert discriminatorColumns.getColumns().size() == 1;

		discriminatorColumns.setTable( value.getTable() );
		discriminatorValueBinder.setColumns( discriminatorColumns );

		discriminatorValueBinder.setReturnedClassName( inferredData.getTypeName() );
		discriminatorValueBinder.setType( property, property.getType(), null, null );

		final BasicValue discriminatorDescriptor = discriminatorValueBinder.make();
		value.setDiscriminator( discriminatorDescriptor );
		discriminatorValueBinder.fillSimpleValue();
		// TODO: this is nasty
		final AnnotatedColumn firstDiscriminatorColumn = discriminatorColumns.getColumns().get(0);
		firstDiscriminatorColumn.linkWithValue( discriminatorDescriptor );

		final JavaType<?> discriminatorJavaType = discriminatorDescriptor
				.resolve()
				.getRelationalJavaType();

		final Map<Object,Class<?>> discriminatorValueMappings = new HashMap<>();
		processAnyDiscriminatorValues(
				inferredData.getAttributeMember(),
				valueMapping -> discriminatorValueMappings.put(
						discriminatorJavaType.wrap( valueMapping.discriminator(), null ),
						valueMapping.entity()
				),
				context.getBootstrapContext().getModelsContext()
		);
		value.setDiscriminatorValueMappings( discriminatorValueMappings );


		final AnyDiscriminatorImplicitValues anyDiscriminatorImplicitValues = property.getDirectAnnotationUsage( AnyDiscriminatorImplicitValues.class );
		if ( anyDiscriminatorImplicitValues != null ) {
			value.setImplicitDiscriminatorValueStrategy( resolveImplicitDiscriminatorStrategy( anyDiscriminatorImplicitValues, context ) );
		}

		final BasicValueBinder keyValueBinder = new BasicValueBinder( BasicValueBinder.Kind.ANY_KEY, context );
		final List<AnnotatedJoinColumn> columns = keyColumns.getJoinColumns();
		assert columns.size() == 1;
		keyColumns.setTable( value.getTable() );
		keyValueBinder.setColumns( keyColumns );
		if ( !optional ) {
			for ( AnnotatedJoinColumn column : columns ) {
				column.setNullable( false );
			}
		}
		keyValueBinder.setType( property, property.getType(), null, null );
		final BasicValue keyDescriptor = keyValueBinder.make();
		value.setKey( keyDescriptor );
		keyValueBinder.fillSimpleValue();
		keyColumns.checkPropertyConsistency();
		columns.get(0).linkWithValue( keyDescriptor ); //TODO: nasty
		return value;
	}

	private static void processAnyDiscriminatorValues(
			MemberDetails property,
			Consumer<AnyDiscriminatorValue> consumer,
			SourceModelBuildingContext sourceModelContext) {
		final AnyDiscriminatorValues valuesAnn =
				property.locateAnnotationUsage( AnyDiscriminatorValues.class, sourceModelContext );
		if ( valuesAnn != null ) {
			final AnyDiscriminatorValue[] nestedList = valuesAnn.value();
			ArrayHelper.forEach( nestedList, consumer );
			return;
		}

		final AnyDiscriminatorValue valueAnn =
				property.locateAnnotationUsage( AnyDiscriminatorValue.class, sourceModelContext );
		if ( valueAnn != null ) {
			consumer.accept( valueAnn );
		}
	}

	public static MappedSuperclass getMappedSuperclassOrNull(
			ClassDetails declaringClass,
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass,
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
			return context.getMetadataCollector().getMappedSuperclass( declaringClass.toJavaClass() );
		}
		else {
			return null;
		}
	}

	public static String getPath(PropertyHolder holder, PropertyData property) {
		return qualify( holder.getPath(), property.getPropertyName() );
	}

	public static Map<String,String> toAliasTableMap(SqlFragmentAlias[] aliases){
		final Map<String,String> ret = new HashMap<>();
		for ( SqlFragmentAlias aliasAnnotation : aliases ) {
			final String table = aliasAnnotation.table();
			if ( isNotBlank( table ) ) {
				ret.put( aliasAnnotation.alias(), table );
			}
		}
		return ret;
	}

	public static Map<String,String> toAliasEntityMap(SqlFragmentAlias[] aliases){
		final Map<String,String> result = new HashMap<>();
		for ( SqlFragmentAlias aliasAnnotation : aliases ) {
			final Class<?> entityClass = aliasAnnotation.entity();
			if ( entityClass != void.class ) {
				result.put( aliasAnnotation.alias(), entityClass.getName() );
			}
		}
		return result;
	}

	public static boolean hasToOneAnnotation(AnnotationTarget property) {
		return property.hasDirectAnnotationUsage(ManyToOne.class)
			|| property.hasDirectAnnotationUsage(OneToOne.class);
	}


	public static FetchMode getFetchMode(FetchType fetch) {
		return switch ( fetch ) {
			case EAGER -> FetchMode.JOIN;
			case LAZY -> FetchMode.SELECT;
		};
	}

	public static String getCascadeStrategy(
			jakarta.persistence.CascadeType[] ejbCascades,
			Cascade hibernateCascadeAnnotation,
			boolean orphanRemoval,
			MetadataBuildingContext context) {
		final EnumSet<CascadeType> cascadeTypes = convertToHibernateCascadeType( ejbCascades );
		final CascadeType[] hibernateCascades =
				hibernateCascadeAnnotation == null ? null : hibernateCascadeAnnotation.value();
		if ( !isEmpty( hibernateCascades ) ) {
			addAll( cascadeTypes, hibernateCascades );
		}
		if ( orphanRemoval ) {
			cascadeTypes.add( CascadeType.DELETE_ORPHAN );
			cascadeTypes.add( CascadeType.REMOVE );
		}
		if ( cascadeTypes.contains( CascadeType.REPLICATE ) ) {
			warnAboutDeprecatedCascadeType( CascadeType.REPLICATE );
		}
		cascadeTypes.addAll( context.getEffectiveDefaults().getDefaultCascadeTypes() );
		return renderCascadeTypeList( cascadeTypes );
	}

	private static EnumSet<CascadeType> convertToHibernateCascadeType(jakarta.persistence.CascadeType[] cascades) {
		final EnumSet<CascadeType> cascadeTypes = EnumSet.noneOf( CascadeType.class );
		if ( cascades != null ) {
			for ( jakarta.persistence.CascadeType cascade: cascades ) {
				cascadeTypes.add( convertCascadeType( cascade ) );
			}
		}
		return cascadeTypes;
	}

	private static CascadeType convertCascadeType(jakarta.persistence.CascadeType cascade) {
		return switch (cascade) {
			case ALL -> CascadeType.ALL;
			case PERSIST -> CascadeType.PERSIST;
			case MERGE -> CascadeType.MERGE;
			case REMOVE -> CascadeType.REMOVE;
			case REFRESH -> CascadeType.REFRESH;
			case DETACH -> CascadeType.DETACH;
		};
	}

	private static String renderCascadeTypeList(EnumSet<CascadeType> cascadeTypes) {
		final StringBuilder cascade = new StringBuilder();
		for ( CascadeType cascadeType : cascadeTypes ) {
			cascade.append( "," );
			cascade.append( switch ( cascadeType ) {
				case ALL -> "all";
				case PERSIST -> "persist";
				case MERGE -> "merge";
				case LOCK -> "lock";
				case REFRESH -> "refresh";
				case DETACH -> "evict";
				case REMOVE -> "delete";
				case DELETE_ORPHAN ->  "delete-orphan";
				case REPLICATE ->  "replicate";
			} );
		}
		return cascade.isEmpty() ? "none" : cascade.substring(1);
	}

	private static void warnAboutDeprecatedCascadeType(CascadeType cascadeType) {
		DEPRECATION_LOGGER.warnf( "CascadeType.%s is deprecated", cascadeType.name() );
	}

	static boolean isGlobalGeneratorNameGlobal(MetadataBuildingContext context) {
		return context.getBootstrapContext().getJpaCompliance().isGlobalGeneratorScopeEnabled();
	}

	public static boolean isDefault(ClassDetails clazz) {
		return clazz == ClassDetails.VOID_CLASS_DETAILS;
	}

	public static boolean isDefault(TypeDetails clazz) {
		return resolveRawClass( clazz ) == ClassDetails.VOID_CLASS_DETAILS;
	}

	public static void checkMappedByType(
			String mappedBy,
			Value targetValue,
			String propertyName,
			PropertyHolder propertyHolder,
			Map<String, PersistentClass> persistentClasses) {
		if ( targetValue instanceof Collection collection ) {
			final ToOne element = (ToOne) collection.getElement();
			checkMappedByType( mappedBy, propertyName, propertyHolder, persistentClasses, element );
		}
		else if ( targetValue instanceof ToOne toOne ) {
			checkMappedByType( mappedBy, propertyName, propertyHolder, persistentClasses, toOne );
		}
	}

	private static void checkMappedByType(
			String mappedBy,
			String propertyName,
			PropertyHolder propertyHolder,
			Map<String, PersistentClass> persistentClasses,
			ToOne toOne) {
		final String referencedEntityName = toOne.getReferencedEntityName();
		final PersistentClass referencedClass = persistentClasses.get( referencedEntityName );
		PersistentClass ownerClass = propertyHolder.getPersistentClass();
		while ( ownerClass != null ) {
			if ( checkReferencedClass( ownerClass, referencedClass ) ) {
				// the two entities map to the same table
				// so we are good
				return;
			}
			ownerClass = ownerClass.getSuperPersistentClass();
		}
		// we could not find any entity mapping to the same table
		throw new AnnotationException(
				"Association '" + qualify( propertyHolder.getPath(), propertyName )
				+ "' is 'mappedBy' a property named '" + mappedBy
				+ "' which references the wrong entity type '" + referencedEntityName
				+ "', expected '" + propertyHolder.getEntityName() + "'"
		);
	}

	private static boolean checkReferencedClass(PersistentClass ownerClass, PersistentClass referencedClass) {
		while ( referencedClass != null ) {
			// Allow different entity types as long as they map to the same table
			if ( ownerClass.getTable() == referencedClass.getTable() ) {
				return true;
			}
			referencedClass = referencedClass.getSuperPersistentClass();
		}
		return false;
	}

	public static boolean noConstraint(ForeignKey foreignKey, boolean noConstraintByDefault) {
		if ( foreignKey == null ) {
			return false;
		}
		else {
			final ConstraintMode mode = foreignKey.value();
			return mode == NO_CONSTRAINT
				|| mode == PROVIDER_DEFAULT && noConstraintByDefault;
		}
	}

	/**
	 * Extract an annotation from the package-info for the package the given class is defined in
	 *
	 * @param annotationType The type of annotation to return
	 * @param classDetails The class in the package
	 * @param context The processing context
	 *
	 * @return The annotation or {@code null}
	 */
	public static <A extends Annotation> A extractFromPackage(
			Class<A> annotationType,
			ClassDetails classDetails,
			MetadataBuildingContext context) {

// todo (soft-delete) : or if we want caching of this per package
//  +
//				final SoftDelete fromPackage = context.getMetadataCollector().resolvePackageAnnotation( packageName, SoftDelete.class );
//  +
//		where context.getMetadataCollector() can cache some of this - either the annotations themselves
//		or even just the XPackage resolutions

		final String declaringClassName = classDetails.getName();
		final String packageName = qualifier( declaringClassName );
		if ( isEmpty( packageName ) ) {
			return null;
		}
		else {
			final SourceModelBuildingContext modelsContext =
					context.getBootstrapContext().getModelsContext();
			try {
				return modelsContext.getClassDetailsRegistry()
						.resolveClassDetails( packageName + ".package-info" )
						.getAnnotationUsage( annotationType, modelsContext );
			}
			catch (ClassLoadingException ignore) {
				return null;
			}
		}
	}
}
