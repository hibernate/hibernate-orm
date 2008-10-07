/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.tuple.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.tuple.IdentifierProperty;
import org.hibernate.tuple.StandardProperty;
import org.hibernate.tuple.PropertyFactory;
import org.hibernate.tuple.VersionProperty;
import org.hibernate.intercept.FieldInterceptionHelper;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.Versioning;
import org.hibernate.engine.ValueInclusion;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.ReflectHelper;

/**
 * Centralizes metamodel information about an entity.
 *
 * @author Steve Ebersole
 */
public class EntityMetamodel implements Serializable {

	private static final Logger log = LoggerFactory.getLogger(EntityMetamodel.class);

	private static final int NO_VERSION_INDX = -66;

	private final SessionFactoryImplementor sessionFactory;

	private final String name;
	private final String rootName;
	private final EntityType entityType;

	private final IdentifierProperty identifierProperty;
	private final boolean versioned;

	private final int propertySpan;
	private final int versionPropertyIndex;
	private final StandardProperty[] properties;
	// temporary ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private final String[] propertyNames;
	private final Type[] propertyTypes;
	private final boolean[] propertyLaziness;
	private final boolean[] propertyUpdateability;
	private final boolean[] nonlazyPropertyUpdateability;
	private final boolean[] propertyCheckability;
	private final boolean[] propertyInsertability;
	private final ValueInclusion[] insertInclusions;
	private final ValueInclusion[] updateInclusions;
	private final boolean[] propertyNullability;
	private final boolean[] propertyVersionability;
	private final CascadeStyle[] cascadeStyles;
	private final boolean hasInsertGeneratedValues;
	private final boolean hasUpdateGeneratedValues;
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private final Map propertyIndexes = new HashMap();
	private final boolean hasCollections;
	private final boolean hasMutableProperties;
	private final boolean hasLazyProperties;
	private final boolean hasNonIdentifierPropertyNamedId;

	private final int[] naturalIdPropertyNumbers;
	private final boolean hasImmutableNaturalId;

	private boolean lazy; //not final because proxy factory creation can fail
	private final boolean hasCascades;
	private final boolean mutable;
	private final boolean isAbstract;
	private final boolean selectBeforeUpdate;
	private final boolean dynamicUpdate;
	private final boolean dynamicInsert;
	private final int optimisticLockMode;

	private final boolean polymorphic;
	private final String superclass;  // superclass entity-name
	private final boolean explicitPolymorphism;
	private final boolean inherited;
	private final boolean hasSubclasses;
	private final Set subclassEntityNames = new HashSet();
	private final Map entityNameByInheritenceClassNameMap = new HashMap();

	private final EntityEntityModeToTuplizerMapping tuplizerMapping;

	public EntityMetamodel(PersistentClass persistentClass, SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;

		name = persistentClass.getEntityName();
		rootName = persistentClass.getRootClass().getEntityName();
		entityType = TypeFactory.manyToOne( name );

		identifierProperty = PropertyFactory.buildIdentifierProperty(
		        persistentClass,
		        sessionFactory.getIdentifierGenerator( rootName )
			);

		versioned = persistentClass.isVersioned();

		boolean lazyAvailable = persistentClass.hasPojoRepresentation() &&
		                        FieldInterceptionHelper.isInstrumented( persistentClass.getMappedClass() );
		boolean hasLazy = false;

		propertySpan = persistentClass.getPropertyClosureSpan();
		properties = new StandardProperty[propertySpan];
		List naturalIdNumbers = new ArrayList();
		// temporary ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		propertyNames = new String[propertySpan];
		propertyTypes = new Type[propertySpan];
		propertyUpdateability = new boolean[propertySpan];
		propertyInsertability = new boolean[propertySpan];
		insertInclusions = new ValueInclusion[propertySpan];
		updateInclusions = new ValueInclusion[propertySpan];
		nonlazyPropertyUpdateability = new boolean[propertySpan];
		propertyCheckability = new boolean[propertySpan];
		propertyNullability = new boolean[propertySpan];
		propertyVersionability = new boolean[propertySpan];
		propertyLaziness = new boolean[propertySpan];
		cascadeStyles = new CascadeStyle[propertySpan];
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


		Iterator iter = persistentClass.getPropertyClosureIterator();
		int i = 0;
		int tempVersionProperty = NO_VERSION_INDX;
		boolean foundCascade = false;
		boolean foundCollection = false;
		boolean foundMutable = false;
		boolean foundNonIdentifierPropertyNamedId = false;
		boolean foundInsertGeneratedValue = false;
		boolean foundUpdateGeneratedValue = false;
		boolean foundUpdateableNaturalIdProperty = false;

		while ( iter.hasNext() ) {
			Property prop = ( Property ) iter.next();

			if ( prop == persistentClass.getVersion() ) {
				tempVersionProperty = i;
				properties[i] = PropertyFactory.buildVersionProperty( prop, lazyAvailable );
			}
			else {
				properties[i] = PropertyFactory.buildStandardProperty( prop, lazyAvailable );
			}

			if ( prop.isNaturalIdentifier() ) {
				naturalIdNumbers.add( new Integer(i) );
				if ( prop.isUpdateable() ) {
					foundUpdateableNaturalIdProperty = true;
				}
			}

			if ( "id".equals( prop.getName() ) ) {
				foundNonIdentifierPropertyNamedId = true;
			}

			// temporary ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			boolean lazy = prop.isLazy() && lazyAvailable;
			if ( lazy ) hasLazy = true;
			propertyLaziness[i] = lazy;

			propertyNames[i] = properties[i].getName();
			propertyTypes[i] = properties[i].getType();
			propertyNullability[i] = properties[i].isNullable();
			propertyUpdateability[i] = properties[i].isUpdateable();
			propertyInsertability[i] = properties[i].isInsertable();
			insertInclusions[i] = determineInsertValueGenerationType( prop, properties[i] );
			updateInclusions[i] = determineUpdateValueGenerationType( prop, properties[i] );
			propertyVersionability[i] = properties[i].isVersionable();
			nonlazyPropertyUpdateability[i] = properties[i].isUpdateable() && !lazy;
			propertyCheckability[i] = propertyUpdateability[i] ||
					( propertyTypes[i].isAssociationType() && ( (AssociationType) propertyTypes[i] ).isAlwaysDirtyChecked() );

			cascadeStyles[i] = properties[i].getCascadeStyle();
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			if ( properties[i].isLazy() ) {
				hasLazy = true;
			}

			if ( properties[i].getCascadeStyle() != CascadeStyle.NONE ) {
				foundCascade = true;
			}

			if ( indicatesCollection( properties[i].getType() ) ) {
				foundCollection = true;
			}

			if ( propertyTypes[i].isMutable() && propertyCheckability[i] ) {
				foundMutable = true;
			}

			if ( insertInclusions[i] != ValueInclusion.NONE ) {
				foundInsertGeneratedValue = true;
			}

			if ( updateInclusions[i] != ValueInclusion.NONE ) {
				foundUpdateGeneratedValue = true;
			}

			mapPropertyToIndex(prop, i);
			i++;
		}

		if (naturalIdNumbers.size()==0) {
			naturalIdPropertyNumbers = null;
			hasImmutableNaturalId = false;
		}
		else {
			naturalIdPropertyNumbers = ArrayHelper.toIntArray(naturalIdNumbers);
			hasImmutableNaturalId = !foundUpdateableNaturalIdProperty;
		}

		hasInsertGeneratedValues = foundInsertGeneratedValue;
		hasUpdateGeneratedValues = foundUpdateGeneratedValue;

		hasCascades = foundCascade;
		hasNonIdentifierPropertyNamedId = foundNonIdentifierPropertyNamedId;
		versionPropertyIndex = tempVersionProperty;
		hasLazyProperties = hasLazy;
		if ( hasLazyProperties ) {
			log.info( "lazy property fetching available for: " + name );
		}

		lazy = persistentClass.isLazy() && (
				// TODO: this disables laziness even in non-pojo entity modes:
				!persistentClass.hasPojoRepresentation() ||
				!ReflectHelper.isFinalClass( persistentClass.getProxyInterface() )
		);
		mutable = persistentClass.isMutable();
		if ( persistentClass.isAbstract() == null ) {
			// legacy behavior (with no abstract attribute specified)
			isAbstract = persistentClass.hasPojoRepresentation() &&
			             ReflectHelper.isAbstractClass( persistentClass.getMappedClass() );
		}
		else {
			isAbstract = persistentClass.isAbstract().booleanValue();
			if ( !isAbstract && persistentClass.hasPojoRepresentation() &&
			     ReflectHelper.isAbstractClass( persistentClass.getMappedClass() ) ) {
				log.warn( "entity [" + name + "] is abstract-class/interface explicitly mapped as non-abstract; be sure to supply entity-names" );
			}
		}
		selectBeforeUpdate = persistentClass.hasSelectBeforeUpdate();
		dynamicUpdate = persistentClass.useDynamicUpdate();
		dynamicInsert = persistentClass.useDynamicInsert();

		polymorphic = persistentClass.isPolymorphic();
		explicitPolymorphism = persistentClass.isExplicitPolymorphism();
		inherited = persistentClass.isInherited();
		superclass = inherited ?
				persistentClass.getSuperclass().getEntityName() :
				null;
		hasSubclasses = persistentClass.hasSubclasses();

		optimisticLockMode = persistentClass.getOptimisticLockMode();
		if ( optimisticLockMode > Versioning.OPTIMISTIC_LOCK_VERSION && !dynamicUpdate ) {
			throw new MappingException( "optimistic-lock=all|dirty requires dynamic-update=\"true\": " + name );
		}
		if ( versionPropertyIndex != NO_VERSION_INDX && optimisticLockMode > Versioning.OPTIMISTIC_LOCK_VERSION ) {
			throw new MappingException( "version and optimistic-lock=all|dirty are not a valid combination : " + name );
		}

		hasCollections = foundCollection;
		hasMutableProperties = foundMutable;

		iter = persistentClass.getSubclassIterator();
		while ( iter.hasNext() ) {
			subclassEntityNames.add( ( (PersistentClass) iter.next() ).getEntityName() );
		}
		subclassEntityNames.add( name );

		if ( persistentClass.hasPojoRepresentation() ) {
			entityNameByInheritenceClassNameMap.put( persistentClass.getMappedClass(), persistentClass.getEntityName() );
			iter = persistentClass.getSubclassIterator();
			while ( iter.hasNext() ) {
				final PersistentClass pc = ( PersistentClass ) iter.next();
				entityNameByInheritenceClassNameMap.put( pc.getMappedClass(), pc.getEntityName() );
			}
		}

		tuplizerMapping = new EntityEntityModeToTuplizerMapping( persistentClass, this );
	}

	private ValueInclusion determineInsertValueGenerationType(Property mappingProperty, StandardProperty runtimeProperty) {
		if ( runtimeProperty.isInsertGenerated() ) {
			return ValueInclusion.FULL;
		}
		else if ( mappingProperty.getValue() instanceof Component ) {
			if ( hasPartialInsertComponentGeneration( ( Component ) mappingProperty.getValue() ) ) {
				return ValueInclusion.PARTIAL;
			}
		}
		return ValueInclusion.NONE;
	}

	private boolean hasPartialInsertComponentGeneration(Component component) {
		Iterator subProperties = component.getPropertyIterator();
		while ( subProperties.hasNext() ) {
			Property prop = ( Property ) subProperties.next();
			if ( prop.getGeneration() == PropertyGeneration.ALWAYS || prop.getGeneration() == PropertyGeneration.INSERT ) {
				return true;
			}
			else if ( prop.getValue() instanceof Component ) {
				if ( hasPartialInsertComponentGeneration( ( Component ) prop.getValue() ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private ValueInclusion determineUpdateValueGenerationType(Property mappingProperty, StandardProperty runtimeProperty) {
		if ( runtimeProperty.isUpdateGenerated() ) {
			return ValueInclusion.FULL;
		}
		else if ( mappingProperty.getValue() instanceof Component ) {
			if ( hasPartialUpdateComponentGeneration( ( Component ) mappingProperty.getValue() ) ) {
				return ValueInclusion.PARTIAL;
			}
		}
		return ValueInclusion.NONE;
	}

	private boolean hasPartialUpdateComponentGeneration(Component component) {
		Iterator subProperties = component.getPropertyIterator();
		while ( subProperties.hasNext() ) {
			Property prop = ( Property ) subProperties.next();
			if ( prop.getGeneration() == PropertyGeneration.ALWAYS ) {
				return true;
			}
			else if ( prop.getValue() instanceof Component ) {
				if ( hasPartialUpdateComponentGeneration( ( Component ) prop.getValue() ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private void mapPropertyToIndex(Property prop, int i) {
		propertyIndexes.put( prop.getName(), new Integer(i) );
		if ( prop.getValue() instanceof Component ) {
			Iterator iter = ( (Component) prop.getValue() ).getPropertyIterator();
			while ( iter.hasNext() ) {
				Property subprop = (Property) iter.next();
				propertyIndexes.put(
						prop.getName() + '.' + subprop.getName(),
						new Integer(i)
					);
			}
		}
	}

	public EntityEntityModeToTuplizerMapping getTuplizerMapping() {
		return tuplizerMapping;
	}

	public EntityTuplizer getTuplizer(EntityMode entityMode) {
		return (EntityTuplizer) tuplizerMapping.getTuplizer( entityMode );
	}

	public EntityTuplizer getTuplizerOrNull(EntityMode entityMode) {
		return ( EntityTuplizer ) tuplizerMapping.getTuplizerOrNull( entityMode );
	}

	public EntityMode guessEntityMode(Object object) {
		return tuplizerMapping.guessEntityMode( object );
	}

	public int[] getNaturalIdentifierProperties() {
		return naturalIdPropertyNumbers;
	}

	public boolean hasNaturalIdentifier() {
		return naturalIdPropertyNumbers!=null;
	}

	public boolean hasImmutableNaturalId() {
		return hasImmutableNaturalId;
	}

	public Set getSubclassEntityNames() {
		return subclassEntityNames;
	}

	private boolean indicatesCollection(Type type) {
		if ( type.isCollectionType() ) {
			return true;
		}
		else if ( type.isComponentType() ) {
			Type[] subtypes = ( ( AbstractComponentType ) type ).getSubtypes();
			for ( int i = 0; i < subtypes.length; i++ ) {
				if ( indicatesCollection( subtypes[i] ) ) {
					return true;
				}
			}
		}
		return false;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	public String getName() {
		return name;
	}

	public String getRootName() {
		return rootName;
	}

	public EntityType getEntityType() {
		return entityType;
	}

	public IdentifierProperty getIdentifierProperty() {
		return identifierProperty;
	}

	public int getPropertySpan() {
		return propertySpan;
	}

	public int getVersionPropertyIndex() {
		return versionPropertyIndex;
	}

	public VersionProperty getVersionProperty() {
		if ( NO_VERSION_INDX == versionPropertyIndex ) {
			return null;
		}
		else {
			return ( VersionProperty ) properties[ versionPropertyIndex ];
		}
	}

	public StandardProperty[] getProperties() {
		return properties;
	}

	public int getPropertyIndex(String propertyName) {
		Integer index = getPropertyIndexOrNull(propertyName);
		if ( index == null ) {
			throw new HibernateException("Unable to resolve property: " + propertyName);
		}
		return index.intValue();
	}

	public Integer getPropertyIndexOrNull(String propertyName) {
		return (Integer) propertyIndexes.get( propertyName );
	}

	public boolean hasCollections() {
		return hasCollections;
	}

	public boolean hasMutableProperties() {
		return hasMutableProperties;
	}

	public boolean hasNonIdentifierPropertyNamedId() {
		return hasNonIdentifierPropertyNamedId;
	}

	public boolean hasLazyProperties() {
		return hasLazyProperties;
	}

	public boolean hasCascades() {
		return hasCascades;
	}

	public boolean isMutable() {
		return mutable;
	}

	public boolean isSelectBeforeUpdate() {
		return selectBeforeUpdate;
	}

	public boolean isDynamicUpdate() {
		return dynamicUpdate;
	}

	public boolean isDynamicInsert() {
		return dynamicInsert;
	}

	public int getOptimisticLockMode() {
		return optimisticLockMode;
	}

	public boolean isPolymorphic() {
		return polymorphic;
	}

	public String getSuperclass() {
		return superclass;
	}

	public boolean isExplicitPolymorphism() {
		return explicitPolymorphism;
	}

	public boolean isInherited() {
		return inherited;
	}

	public boolean hasSubclasses() {
		return hasSubclasses;
	}

	public boolean isLazy() {
		return lazy;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public boolean isVersioned() {
		return versioned;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	/**
	 * Return the entity-name mapped to the given class within our inheritence hierarchy, if any.
	 *
	 * @param inheritenceClass The class for which to resolve the entity-name.
	 * @return The mapped entity-name, or null if no such mapping was found.
	 */
	public String findEntityNameByEntityClass(Class inheritenceClass) {
		return ( String ) entityNameByInheritenceClassNameMap.get( inheritenceClass.getName() );
	}

	public String toString() {
		return "EntityMetamodel(" + name + ':' + ArrayHelper.toString(properties) + ')';
	}

	// temporary ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	public String[] getPropertyNames() {
		return propertyNames;
	}

	public Type[] getPropertyTypes() {
		return propertyTypes;
	}

	public boolean[] getPropertyLaziness() {
		return propertyLaziness;
	}

	public boolean[] getPropertyUpdateability() {
		return propertyUpdateability;
	}

	public boolean[] getPropertyCheckability() {
		return propertyCheckability;
	}

	public boolean[] getNonlazyPropertyUpdateability() {
		return nonlazyPropertyUpdateability;
	}

	public boolean[] getPropertyInsertability() {
		return propertyInsertability;
	}

	public ValueInclusion[] getPropertyInsertGenerationInclusions() {
		return insertInclusions;
	}

	public ValueInclusion[] getPropertyUpdateGenerationInclusions() {
		return updateInclusions;
	}

	public boolean[] getPropertyNullability() {
		return propertyNullability;
	}

	public boolean[] getPropertyVersionability() {
		return propertyVersionability;
	}

	public CascadeStyle[] getCascadeStyles() {
		return cascadeStyles;
	}

	public boolean hasInsertGeneratedValues() {
		return hasInsertGeneratedValues;
	}

	public boolean hasUpdateGeneratedValues() {
		return hasUpdateGeneratedValues;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
}
