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
package org.hibernate.tuple.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.bytecode.spi.EntityInstrumentationMetadata;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.ValueInclusion;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.BasicAttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.SingularAttribute;
import org.hibernate.tuple.IdentifierProperty;
import org.hibernate.tuple.PropertyFactory;
import org.hibernate.tuple.StandardProperty;
import org.hibernate.tuple.VersionProperty;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * Centralizes metamodel information about an entity.
 *
 * @author Steve Ebersole
 */
public class EntityMetamodel implements Serializable {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, EntityMetamodel.class.getName());

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
	private final Map<String, Integer> propertyIndexes = new HashMap<String, Integer>();
	private final boolean hasCollections;
	private final boolean hasMutableProperties;
	private final boolean hasLazyProperties;
	private final boolean hasNonIdentifierPropertyNamedId;

	private final int[] naturalIdPropertyNumbers;
	private final boolean hasImmutableNaturalId;
	private final boolean hasCacheableNaturalId;

	private boolean lazy; //not final because proxy factory creation can fail
	private final boolean hasCascades;
	private final boolean mutable;
	private final boolean isAbstract;
	private final boolean selectBeforeUpdate;
	private final boolean dynamicUpdate;
	private final boolean dynamicInsert;
	private final OptimisticLockStyle optimisticLockStyle;

	private final boolean polymorphic;
	private final String superclass;  // superclass entity-name
	private final boolean explicitPolymorphism;
	private final boolean inherited;
	private final boolean hasSubclasses;
	private final Set subclassEntityNames = new HashSet();
	private final Map entityNameByInheritenceClassMap = new HashMap();

	private final EntityMode entityMode;
	private final EntityTuplizer entityTuplizer;
	private final EntityInstrumentationMetadata instrumentationMetadata;

	public EntityMetamodel(PersistentClass persistentClass, SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;

		name = persistentClass.getEntityName();
		rootName = persistentClass.getRootClass().getEntityName();
		entityType = sessionFactory.getTypeResolver().getTypeFactory().manyToOne( name );

		identifierProperty = PropertyFactory.buildIdentifierProperty(
		        persistentClass,
		        sessionFactory.getIdentifierGenerator( rootName )
			);

		versioned = persistentClass.isVersioned();

		instrumentationMetadata = persistentClass.hasPojoRepresentation()
				? Environment.getBytecodeProvider().getEntityInstrumentationMetadata( persistentClass.getMappedClass() )
				: new NonPojoInstrumentationMetadata( persistentClass.getEntityName() );

		boolean hasLazy = false;

		propertySpan = persistentClass.getPropertyClosureSpan();
		properties = new StandardProperty[propertySpan];
		List<Integer> naturalIdNumbers = new ArrayList<Integer>();
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
				properties[i] = PropertyFactory.buildVersionProperty( prop, instrumentationMetadata.isInstrumented() );
			}
			else {
				properties[i] = PropertyFactory.buildStandardProperty( prop, instrumentationMetadata.isInstrumented() );
			}

			if ( prop.isNaturalIdentifier() ) {
				naturalIdNumbers.add( i );
				if ( prop.isUpdateable() ) {
					foundUpdateableNaturalIdProperty = true;
				}
			}

			if ( "id".equals( prop.getName() ) ) {
				foundNonIdentifierPropertyNamedId = true;
			}

			// temporary ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			boolean lazy = prop.isLazy() && instrumentationMetadata.isInstrumented();
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
			hasCacheableNaturalId = false;
		}
		else {
			naturalIdPropertyNumbers = ArrayHelper.toIntArray(naturalIdNumbers);
			hasImmutableNaturalId = !foundUpdateableNaturalIdProperty;
			hasCacheableNaturalId = persistentClass.getNaturalIdCacheRegionName() != null;
		}

		hasInsertGeneratedValues = foundInsertGeneratedValue;
		hasUpdateGeneratedValues = foundUpdateGeneratedValue;

		hasCascades = foundCascade;
		hasNonIdentifierPropertyNamedId = foundNonIdentifierPropertyNamedId;
		versionPropertyIndex = tempVersionProperty;
		hasLazyProperties = hasLazy;
        if (hasLazyProperties) LOG.lazyPropertyFetchingAvailable(name);

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
                LOG.entityMappedAsNonAbstract(name);
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

		optimisticLockStyle = interpretOptLockMode( persistentClass.getOptimisticLockMode() );
		final boolean isAllOrDirty =
				optimisticLockStyle == OptimisticLockStyle.ALL
						|| optimisticLockStyle == OptimisticLockStyle.DIRTY;
		if ( isAllOrDirty && !dynamicUpdate ) {
			throw new MappingException( "optimistic-lock=all|dirty requires dynamic-update=\"true\": " + name );
		}
		if ( versionPropertyIndex != NO_VERSION_INDX && isAllOrDirty ) {
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
			entityNameByInheritenceClassMap.put( persistentClass.getMappedClass(), persistentClass.getEntityName() );
			iter = persistentClass.getSubclassIterator();
			while ( iter.hasNext() ) {
				final PersistentClass pc = ( PersistentClass ) iter.next();
				entityNameByInheritenceClassMap.put( pc.getMappedClass(), pc.getEntityName() );
			}
		}

		entityMode = persistentClass.hasPojoRepresentation() ? EntityMode.POJO : EntityMode.MAP;
		final EntityTuplizerFactory entityTuplizerFactory = sessionFactory.getSettings().getEntityTuplizerFactory();
		final String tuplizerClassName = persistentClass.getTuplizerImplClassName( entityMode );
		if ( tuplizerClassName == null ) {
			entityTuplizer = entityTuplizerFactory.constructDefaultTuplizer( entityMode, this, persistentClass );
		}
		else {
			entityTuplizer = entityTuplizerFactory.constructTuplizer( tuplizerClassName, this, persistentClass );
		}
	}

	private OptimisticLockStyle interpretOptLockMode(int optimisticLockMode) {
		switch ( optimisticLockMode ) {
			case Versioning.OPTIMISTIC_LOCK_NONE: {
				return OptimisticLockStyle.NONE;
			}
			case Versioning.OPTIMISTIC_LOCK_DIRTY: {
				return OptimisticLockStyle.DIRTY;
			}
			case Versioning.OPTIMISTIC_LOCK_ALL: {
				return OptimisticLockStyle.ALL;
			}
			default: {
				return OptimisticLockStyle.VERSION;
			}
		}
	}

	public EntityMetamodel(EntityBinding entityBinding, SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;

		name = entityBinding.getEntity().getName();

		rootName = entityBinding.getHierarchyDetails().getRootEntityBinding().getEntity().getName();
		entityType = sessionFactory.getTypeResolver().getTypeFactory().manyToOne( name );

		identifierProperty = PropertyFactory.buildIdentifierProperty(
		        entityBinding,
		        sessionFactory.getIdentifierGenerator( rootName )
		);

		versioned = entityBinding.isVersioned();

		boolean hasPojoRepresentation = false;
		Class<?> mappedClass = null;
		Class<?> proxyInterfaceClass = null;
		if ( entityBinding.getEntity().getClassReferenceUnresolved() != null ) {
			hasPojoRepresentation = true;
			mappedClass = entityBinding.getEntity().getClassReference();
			proxyInterfaceClass = entityBinding.getProxyInterfaceType().getValue();
		}
		instrumentationMetadata = Environment.getBytecodeProvider().getEntityInstrumentationMetadata( mappedClass );

		boolean hasLazy = false;

		// TODO: Fix after HHH-6337 is fixed; for now assume entityBinding is the root binding
		BasicAttributeBinding rootEntityIdentifier = entityBinding.getHierarchyDetails().getEntityIdentifier().getValueBinding();
		// entityBinding.getAttributeClosureSpan() includes the identifier binding;
		// "properties" here excludes the ID, so subtract 1 if the identifier binding is non-null
		propertySpan = rootEntityIdentifier == null ?
				entityBinding.getAttributeBindingClosureSpan() :
				entityBinding.getAttributeBindingClosureSpan() - 1;

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


		int i = 0;
		int tempVersionProperty = NO_VERSION_INDX;
		boolean foundCascade = false;
		boolean foundCollection = false;
		boolean foundMutable = false;
		boolean foundNonIdentifierPropertyNamedId = false;
		boolean foundInsertGeneratedValue = false;
		boolean foundUpdateGeneratedValue = false;
		boolean foundUpdateableNaturalIdProperty = false;

		for ( AttributeBinding attributeBinding : entityBinding.getAttributeBindingClosure() ) {
			if ( attributeBinding == rootEntityIdentifier ) {
				// skip the identifier attribute binding
				continue;
			}

			if ( attributeBinding == entityBinding.getHierarchyDetails().getVersioningAttributeBinding() ) {
				tempVersionProperty = i;
				properties[i] = PropertyFactory.buildVersionProperty(
						entityBinding.getHierarchyDetails().getVersioningAttributeBinding(),
						instrumentationMetadata.isInstrumented()
				);
			}
			else {
				properties[i] = PropertyFactory.buildStandardProperty( attributeBinding, instrumentationMetadata.isInstrumented() );
			}

			// TODO: fix when natural IDs are added (HHH-6354)
			//if ( attributeBinding.isNaturalIdentifier() ) {
			//	naturalIdNumbers.add( i );
			//	if ( attributeBinding.isUpdateable() ) {
			//		foundUpdateableNaturalIdProperty = true;
			//	}
			//}

			if ( "id".equals( attributeBinding.getAttribute().getName() ) ) {
				foundNonIdentifierPropertyNamedId = true;
			}

			// temporary ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			boolean lazy = attributeBinding.isLazy() && instrumentationMetadata.isInstrumented();
			if ( lazy ) hasLazy = true;
			propertyLaziness[i] = lazy;

			propertyNames[i] = properties[i].getName();
			propertyTypes[i] = properties[i].getType();
			propertyNullability[i] = properties[i].isNullable();
			propertyUpdateability[i] = properties[i].isUpdateable();
			propertyInsertability[i] = properties[i].isInsertable();
			insertInclusions[i] = determineInsertValueGenerationType( attributeBinding, properties[i] );
			updateInclusions[i] = determineUpdateValueGenerationType( attributeBinding, properties[i] );
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

			mapPropertyToIndex(attributeBinding.getAttribute(), i);
			i++;
		}

		if (naturalIdNumbers.size()==0) {
			naturalIdPropertyNumbers = null;
			hasImmutableNaturalId = false;
			hasCacheableNaturalId = false;
		}
		else {
			naturalIdPropertyNumbers = ArrayHelper.toIntArray(naturalIdNumbers);
			hasImmutableNaturalId = !foundUpdateableNaturalIdProperty;
			hasCacheableNaturalId = false; //See previous TODO and HHH-6354
		}

		hasInsertGeneratedValues = foundInsertGeneratedValue;
		hasUpdateGeneratedValues = foundUpdateGeneratedValue;

		hasCascades = foundCascade;
		hasNonIdentifierPropertyNamedId = foundNonIdentifierPropertyNamedId;
		versionPropertyIndex = tempVersionProperty;
		hasLazyProperties = hasLazy;
		if (hasLazyProperties) {
			LOG.lazyPropertyFetchingAvailable( name );
		}

		lazy = entityBinding.isLazy() && (
				// TODO: this disables laziness even in non-pojo entity modes:
				! hasPojoRepresentation ||
				! ReflectHelper.isFinalClass( proxyInterfaceClass )
		);
		mutable = entityBinding.isMutable();
		if ( entityBinding.isAbstract() == null ) {
			// legacy behavior (with no abstract attribute specified)
			isAbstract = hasPojoRepresentation &&
			             ReflectHelper.isAbstractClass( mappedClass );
		}
		else {
			isAbstract = entityBinding.isAbstract().booleanValue();
			if ( !isAbstract && hasPojoRepresentation &&
					ReflectHelper.isAbstractClass( mappedClass ) ) {
				LOG.entityMappedAsNonAbstract(name);
			}
		}
		selectBeforeUpdate = entityBinding.isSelectBeforeUpdate();
		dynamicUpdate = entityBinding.isDynamicUpdate();
		dynamicInsert = entityBinding.isDynamicInsert();

		hasSubclasses = entityBinding.hasSubEntityBindings();
		polymorphic = entityBinding.isPolymorphic();

		explicitPolymorphism = entityBinding.getHierarchyDetails().isExplicitPolymorphism();
		inherited = ! entityBinding.isRoot();
		superclass = inherited ?
				entityBinding.getEntity().getSuperType().getName() :
				null;

		optimisticLockStyle = entityBinding.getHierarchyDetails().getOptimisticLockStyle();
		final boolean isAllOrDirty =
				optimisticLockStyle == OptimisticLockStyle.ALL
						|| optimisticLockStyle == OptimisticLockStyle.DIRTY;
		if ( isAllOrDirty && !dynamicUpdate ) {
			throw new MappingException( "optimistic-lock=all|dirty requires dynamic-update=\"true\": " + name );
		}
		if ( versionPropertyIndex != NO_VERSION_INDX && isAllOrDirty ) {
			throw new MappingException( "version and optimistic-lock=all|dirty are not a valid combination : " + name );
		}

		hasCollections = foundCollection;
		hasMutableProperties = foundMutable;

		for ( EntityBinding subEntityBinding : entityBinding.getPostOrderSubEntityBindingClosure() ) {
			subclassEntityNames.add( subEntityBinding.getEntity().getName() );
			if ( subEntityBinding.getEntity().getClassReference() != null ) {
				entityNameByInheritenceClassMap.put(
						subEntityBinding.getEntity().getClassReference(),
						subEntityBinding.getEntity().getName() );
			}
		}
		subclassEntityNames.add( name );
		if ( mappedClass != null ) {
			entityNameByInheritenceClassMap.put( mappedClass, name );
		}

		entityMode = hasPojoRepresentation ? EntityMode.POJO : EntityMode.MAP;
		final EntityTuplizerFactory entityTuplizerFactory = sessionFactory.getSettings().getEntityTuplizerFactory();
		Class<? extends EntityTuplizer> tuplizerClass = entityBinding.getCustomEntityTuplizerClass();

		if ( tuplizerClass == null ) {
			entityTuplizer = entityTuplizerFactory.constructDefaultTuplizer( entityMode, this, entityBinding );
		}
		else {
			entityTuplizer = entityTuplizerFactory.constructTuplizer( tuplizerClass, this, entityBinding );
		}
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

	private ValueInclusion determineInsertValueGenerationType(AttributeBinding mappingProperty, StandardProperty runtimeProperty) {
		if ( runtimeProperty.isInsertGenerated() ) {
			return ValueInclusion.FULL;
		}
		// TODO: fix the following when components are working (HHH-6173)
		//else if ( mappingProperty.getValue() instanceof ComponentAttributeBinding ) {
		//	if ( hasPartialInsertComponentGeneration( ( ComponentAttributeBinding ) mappingProperty.getValue() ) ) {
		//		return ValueInclusion.PARTIAL;
		//	}
		//}
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

	private ValueInclusion determineUpdateValueGenerationType(AttributeBinding mappingProperty, StandardProperty runtimeProperty) {
		if ( runtimeProperty.isUpdateGenerated() ) {
			return ValueInclusion.FULL;
		}
		// TODO: fix the following when components are working (HHH-6173)
		//else if ( mappingProperty.getValue() instanceof ComponentAttributeBinding ) {
		//	if ( hasPartialUpdateComponentGeneration( ( ComponentAttributeBinding ) mappingProperty.getValue() ) ) {
		//		return ValueInclusion.PARTIAL;
		//	}
		//}
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
		propertyIndexes.put( prop.getName(), i );
		if ( prop.getValue() instanceof Component ) {
			Iterator iter = ( (Component) prop.getValue() ).getPropertyIterator();
			while ( iter.hasNext() ) {
				Property subprop = (Property) iter.next();
				propertyIndexes.put(
						prop.getName() + '.' + subprop.getName(),
						i
					);
			}
		}
	}

	private void mapPropertyToIndex(Attribute attribute, int i) {
		propertyIndexes.put( attribute.getName(), i );
		if ( attribute.isSingular() &&
				( ( SingularAttribute ) attribute ).getSingularAttributeType().isComponent() ) {
			org.hibernate.metamodel.domain.Component component =
					( org.hibernate.metamodel.domain.Component ) ( ( SingularAttribute ) attribute ).getSingularAttributeType();
			for ( Attribute subAttribute : component.attributes() ) {
				propertyIndexes.put(
						attribute.getName() + '.' + subAttribute.getName(),
						i
					);
			}
		}
	}

	public EntityTuplizer getTuplizer() {
		return entityTuplizer;
	}

	public int[] getNaturalIdentifierProperties() {
		return naturalIdPropertyNumbers;
	}

	public boolean hasNaturalIdentifier() {
		return naturalIdPropertyNumbers!=null;
	}
	
	public boolean isNaturalIdentifierCached() {
		return hasNaturalIdentifier() && hasCacheableNaturalId;
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
			Type[] subtypes = ( (CompositeType) type ).getSubtypes();
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

	public OptimisticLockStyle getOptimisticLockStyle() {
		return optimisticLockStyle;
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
	 * Return the entity-name mapped to the given class within our inheritance hierarchy, if any.
	 *
	 * @param inheritenceClass The class for which to resolve the entity-name.
	 * @return The mapped entity-name, or null if no such mapping was found.
	 */
	public String findEntityNameByEntityClass(Class inheritenceClass) {
		return ( String ) entityNameByInheritenceClassMap.get( inheritenceClass );
	}

	@Override
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

	public EntityMode getEntityMode() {
		return entityMode;
	}

	/**
	 * Whether or not this class can be lazy (ie intercepted)
	 */
	public boolean isInstrumented() {
		return instrumentationMetadata.isInstrumented();
	}

	public EntityInstrumentationMetadata getInstrumentationMetadata() {
		return instrumentationMetadata;
	}
}
