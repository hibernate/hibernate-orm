/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tuple.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementHelper;
import org.hibernate.bytecode.internal.BytecodeEnhancementMetadataNonPojoImpl;
import org.hibernate.bytecode.internal.BytecodeEnhancementMetadataPojoImpl;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.GeneratorCreator;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.IdentifierProperty;
import org.hibernate.tuple.NonIdentifierAttribute;
import org.hibernate.tuple.PropertyFactory;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.Type;

import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;
import static org.hibernate.internal.CoreLogging.messageLogger;
import static org.hibernate.internal.util.ReflectHelper.isAbstractClass;
import static org.hibernate.internal.util.ReflectHelper.isFinalClass;
import static org.hibernate.internal.util.collections.ArrayHelper.toIntArray;
import static org.hibernate.internal.util.collections.CollectionHelper.toSmallSet;
import static org.hibernate.tuple.PropertyFactory.buildIdentifierAttribute;

/**
 * Centralizes metamodel information about an entity.
 *
 * @author Steve Ebersole
 *
 * @deprecated Replaced by {@link EntityMappingType}.  EntityMetamodel
 * was a first attempt at what has become {@link EntityMappingType}
 */
@Deprecated( since = "6", forRemoval = true )
public class EntityMetamodel implements Serializable {
	private static final CoreMessageLogger LOG = messageLogger( EntityMetamodel.class );

	public static final int NO_VERSION_INDX = -66;

	private final SessionFactoryImplementor sessionFactory;

	private final String name;
	private final String rootName;
	private EntityType entityType;

	private final int subclassId;
	private final IdentifierProperty identifierAttribute;
	private final boolean versioned;

	private final int propertySpan;
	private final int versionPropertyIndex;
	private final NonIdentifierAttribute[] properties;
	// temporary ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private final String[] propertyNames;
	private final Type[] propertyTypes;
	private final @Nullable Type[] dirtyCheckablePropertyTypes;
	private final boolean[] propertyLaziness;
	private final boolean[] propertyUpdateability;
	private final boolean[] nonlazyPropertyUpdateability;
	private final boolean[] propertyCheckability;
	private final boolean[] propertyInsertability;
	private final boolean[] propertyNullability;
	private final boolean[] propertyVersionability;
	private final OnDeleteAction[] propertyOnDeleteActions;
	private final CascadeStyle[] cascadeStyles;
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// value generations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private final boolean hasPreInsertGeneratedValues;
	private final boolean hasPreUpdateGeneratedValues;
	private final boolean hasInsertGeneratedValues;
	private final boolean hasUpdateGeneratedValues;

	private final Generator[] generators;
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private final Map<String, Integer> propertyIndexes = new HashMap<>();
	private final boolean hasCollections;
	private final boolean hasOwnedCollections;
	private final BitSet mutablePropertiesIndexes;
	private final boolean hasLazyProperties;
	private final boolean hasNonIdentifierPropertyNamedId;

	private final int[] naturalIdPropertyNumbers;
	private final boolean hasImmutableNaturalId;
	private final boolean hasCacheableNaturalId;

	private boolean lazy; //not final because proxy factory creation can fail
	private final boolean hasCascades;
	private final boolean hasToOnes;
	private final boolean hasCascadePersist;
	private final boolean hasCascadeDelete;
	private final boolean mutable;
	private final boolean isAbstract;
	private final boolean selectBeforeUpdate;
	private final boolean dynamicUpdate;
	private final boolean dynamicInsert;
	private final OptimisticLockStyle optimisticLockStyle;

	private final boolean polymorphic;
	private final String superclass;  // superclass entity-name
	private final boolean inherited;
	private final boolean hasSubclasses;
	private final Set<String> subclassEntityNames;
//	private final Map<Class<?>,String> entityNameByInheritanceClassMap;

	private final BeforeExecutionGenerator versionGenerator;

	private final BytecodeEnhancementMetadata bytecodeEnhancementMetadata;

	public EntityMetamodel(
			PersistentClass persistentClass,
			EntityPersister persister,
			RuntimeModelCreationContext creationContext) {
		this( persistentClass, persister, creationContext,
				rootName -> buildIdGenerator( rootName, persistentClass, creationContext ) );
	}

	/*
	 * Used by Hibernate Reactive to adapt the id generators
	 */
	public EntityMetamodel(
			PersistentClass persistentClass,
			EntityPersister persister,
			RuntimeModelCreationContext creationContext,
			Function<String, Generator> generatorSupplier) {
		sessionFactory = creationContext.getSessionFactory();

		// Improves performance of EntityKey#equals by avoiding content check in String#equals
		name = persistentClass.getEntityName().intern();
		rootName = persistentClass.getRootClass().getEntityName().intern();
		// Make sure the hashCodes are cached
		name.hashCode();
		rootName.hashCode();

		subclassId = persistentClass.getSubclassId();

		final Generator idgenerator = generatorSupplier.apply( rootName );
		identifierAttribute = buildIdentifierAttribute( persistentClass, idgenerator );

		versioned = persistentClass.isVersioned();

		final boolean collectionsInDefaultFetchGroupEnabled =
				creationContext.getSessionFactoryOptions().isCollectionsInDefaultFetchGroupEnabled();
		final boolean supportsCascadeDelete = creationContext.getDialect().supportsCascadeDelete();

		if ( persistentClass.hasPojoRepresentation() ) {
			final Component identifierMapperComponent = persistentClass.getIdentifierMapper();
			final CompositeType nonAggregatedCidMapper;
			final Set<String> idAttributeNames;
			if ( identifierMapperComponent != null ) {
				nonAggregatedCidMapper = identifierMapperComponent.getType();
				HashSet<String> tmpSet = new HashSet<>();
				for ( Property property : identifierMapperComponent.getProperties() ) {
					tmpSet.add( property.getName() );
				}
				idAttributeNames = toSmallSet( unmodifiableSet( tmpSet ) );
			}
			else {
				nonAggregatedCidMapper = null;
				idAttributeNames = singleton( identifierAttribute.getName() );
			}

			bytecodeEnhancementMetadata = BytecodeEnhancementMetadataPojoImpl.from(
					persistentClass,
					idAttributeNames,
					nonAggregatedCidMapper,
					collectionsInDefaultFetchGroupEnabled,
					creationContext.getMetadata()
			);
		}
		else {
			bytecodeEnhancementMetadata = new BytecodeEnhancementMetadataNonPojoImpl( persistentClass.getEntityName() );
		}

		boolean hasLazy = false;

		propertySpan = persistentClass.getPropertyClosureSpan();
		properties = new NonIdentifierAttribute[propertySpan];
		List<Integer> naturalIdNumbers = new ArrayList<>();
		// temporary ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		propertyNames = new String[propertySpan];
		propertyTypes = new Type[propertySpan];
		dirtyCheckablePropertyTypes = new Type[propertySpan];
		propertyUpdateability = new boolean[propertySpan];
		propertyInsertability = new boolean[propertySpan];
		nonlazyPropertyUpdateability = new boolean[propertySpan];
		propertyCheckability = new boolean[propertySpan];
		propertyNullability = new boolean[propertySpan];
		propertyVersionability = new boolean[propertySpan];
		propertyLaziness = new boolean[propertySpan];
		propertyOnDeleteActions = new OnDeleteAction[propertySpan];
		cascadeStyles = new CascadeStyle[propertySpan];
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		// generated value strategies ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		generators = new Generator[propertySpan];

		boolean foundPreInsertGeneratedValues = false;
		boolean foundPreUpdateGeneratedValues = false;
		boolean foundPostInsertGeneratedValues = false;
		boolean foundPostUpdateGeneratedValues = false;
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		int tempVersionProperty = NO_VERSION_INDX;
		boolean foundCascade = false;
		boolean foundToOne = false;
		boolean foundCascadePersist = false;
		boolean foundCascadeDelete = false;
		boolean foundCollection = false;
		boolean foundOwnedCollection = false;
		BitSet mutableIndexes = new BitSet();
		boolean foundNonIdentifierPropertyNamedId = false;
		boolean foundUpdateableNaturalIdProperty = false;
		BeforeExecutionGenerator tempVersionGenerator = null;

		final List<Property> props = persistentClass.getPropertyClosure();
		for ( int i=0; i<props.size(); i++ ) {
			final Property property = props.get(i);
			final NonIdentifierAttribute attribute;
			if ( property == persistentClass.getVersion() ) {
				tempVersionProperty = i;
				attribute = PropertyFactory.buildVersionProperty(
						persister,
						sessionFactory,
						i,
						property,
						bytecodeEnhancementMetadata.isEnhancedForLazyLoading()
				);
			}
			else {
				attribute = PropertyFactory.buildEntityBasedAttribute(
						persister,
						sessionFactory,
						i,
						property,
						bytecodeEnhancementMetadata.isEnhancedForLazyLoading(),
						creationContext
				);
			}
			properties[i] = attribute;

			if ( property.isNaturalIdentifier() ) {
				verifyNaturalIdProperty( property );
				naturalIdNumbers.add( i );
				if ( property.isUpdateable() ) {
					foundUpdateableNaturalIdProperty = true;
				}
			}

			if ( "id".equals( property.getName() ) ) {
				foundNonIdentifierPropertyNamedId = true;
			}

			// temporary ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			boolean lazy = ! EnhancementHelper.includeInBaseFetchGroup(
					property,
					bytecodeEnhancementMetadata.isEnhancedForLazyLoading(),
					(entityName) -> {
						final MetadataImplementor metadata = creationContext.getMetadata();
						final PersistentClass entityBinding = metadata.getEntityBinding( entityName );
						assert entityBinding != null;
						return entityBinding.hasSubclasses();
					},
					collectionsInDefaultFetchGroupEnabled
			);

			if ( lazy ) {
				hasLazy = true;
			}

			propertyLaziness[i] = lazy;

			propertyNames[i] = attribute.getName();
			final Type propertyType = attribute.getType();
			propertyTypes[i] = propertyType;
			if ( attribute.isDirtyCheckable() && !( propertyType instanceof OneToOneType ) ) {
				dirtyCheckablePropertyTypes[i] = propertyType;
			}
			propertyNullability[i] = attribute.isNullable();
			propertyUpdateability[i] = attribute.isUpdateable();
			propertyInsertability[i] = attribute.isInsertable();
			propertyVersionability[i] = attribute.isVersionable();
			nonlazyPropertyUpdateability[i] = attribute.isUpdateable() && !lazy;
			propertyCheckability[i] = propertyUpdateability[i]
					|| propertyType.isAssociationType() && ( (AssociationType) propertyType ).isAlwaysDirtyChecked();
			propertyOnDeleteActions[i] = supportsCascadeDelete ? attribute.getOnDeleteAction() : null;
			cascadeStyles[i] = attribute.getCascadeStyle();
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			// generated value strategies ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			final Generator generator = buildGenerator( name, property, creationContext );
			if ( generator != null ) {
				final boolean generatedOnExecution = generator.generatedOnExecution();
				if ( i == tempVersionProperty && !generatedOnExecution ) {
					// when we have an in-memory generator for the version, we
					// want to plug it in to the older infrastructure specific
					// to version generation, instead of treating it like a
					// plain "value" generator for a regular attribute
					tempVersionGenerator = (BeforeExecutionGenerator) generator;
				}
				else {
					generators[i] = generator;
					final boolean allowMutation = generator.allowMutation();
					if ( !allowMutation ) {
						propertyCheckability[i] = false;
					}
					if ( generator.generatesOnInsert() ) {
						if ( generatedOnExecution ) {
							propertyInsertability[i] = writePropertyValue( (OnExecutionGenerator) generator );
						}
						foundPostInsertGeneratedValues = foundPostInsertGeneratedValues
								|| generator instanceof OnExecutionGenerator;
						foundPreInsertGeneratedValues = foundPreInsertGeneratedValues
								|| generator instanceof BeforeExecutionGenerator;
					}
					else if ( !allowMutation ) {
						propertyInsertability[i] = false;
					}
					if ( generator.generatesOnUpdate() ) {
						if ( generatedOnExecution ) {
							propertyUpdateability[i] = writePropertyValue( (OnExecutionGenerator) generator );
						}
						foundPostUpdateGeneratedValues = foundPostUpdateGeneratedValues
								|| generator instanceof OnExecutionGenerator;
						foundPreUpdateGeneratedValues = foundPreUpdateGeneratedValues
								|| generator instanceof BeforeExecutionGenerator;
					}
					else if ( !allowMutation ) {
						propertyUpdateability[i] = false;
					}
				}
			}

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			if ( attribute.isLazy() ) {
				hasLazy = true;
			}

			if ( cascadeStyles[i] != CascadeStyles.NONE ) {
				foundCascade = true;
			}
			if ( cascadeStyles[i].doCascade(CascadingActions.PERSIST)
					|| cascadeStyles[i].doCascade(CascadingActions.PERSIST_ON_FLUSH) ) {
				foundCascadePersist = true;
			}
			if ( cascadeStyles[i].doCascade(CascadingActions.REMOVE) ) {
				foundCascadeDelete = true;
			}

			if ( indicatesToOne( attribute.getType() ) ) {
				foundToOne = true;
			}
			if ( indicatesCollection( attribute.getType() ) ) {
				foundCollection = true;
			}
			if ( indicatesOwnedCollection( attribute.getType(), creationContext.getMetadata() ) ) {
				foundOwnedCollection = true;
			}

			// Component types are dirty tracked as well so they are not exactly mutable for the "maybeDirty" check
			if ( propertyType.isMutable() && propertyCheckability[i] && !( propertyType instanceof ComponentType ) ) {
				mutableIndexes.set( i );
			}

			mapPropertyToIndex( property, i );
		}

		if ( naturalIdNumbers.isEmpty() ) {
			naturalIdPropertyNumbers = null;
			hasImmutableNaturalId = false;
			hasCacheableNaturalId = false;
		}
		else {
			naturalIdPropertyNumbers = toIntArray( naturalIdNumbers );
			hasImmutableNaturalId = !foundUpdateableNaturalIdProperty;
			hasCacheableNaturalId = persistentClass.getNaturalIdCacheRegionName() != null;
		}

		hasPreInsertGeneratedValues = foundPreInsertGeneratedValues;
		hasPreUpdateGeneratedValues = foundPreUpdateGeneratedValues;
		hasInsertGeneratedValues = foundPostInsertGeneratedValues;
		hasUpdateGeneratedValues = foundPostUpdateGeneratedValues;

		versionGenerator = tempVersionGenerator;

		hasCascades = foundCascade;
		hasToOnes = foundToOne;
		hasCascadePersist = foundCascadePersist;
		hasCascadeDelete = foundCascadeDelete;
		hasNonIdentifierPropertyNamedId = foundNonIdentifierPropertyNamedId;
		versionPropertyIndex = tempVersionProperty;
		hasLazyProperties = hasLazy;
		if ( hasLazyProperties ) {
			LOG.lazyPropertyFetchingAvailable( name );
		}

		lazy = persistentClass.isLazy()
				// TODO: this disables laziness even in non-pojo entity modes:
				&& (!persistentClass.hasPojoRepresentation() || !isFinalClass( persistentClass.getProxyInterface() ) )
						|| bytecodeEnhancementMetadata.isEnhancedForLazyLoading();

		mutable = persistentClass.isMutable();
		if ( persistentClass.isAbstract() == null ) {
			// legacy behavior (with no abstract attribute specified)
			isAbstract = persistentClass.hasPojoRepresentation()
					&& isAbstractClass( persistentClass.getMappedClass() );
		}
		else {
			isAbstract = persistentClass.isAbstract();
			if ( !isAbstract
					&& persistentClass.hasPojoRepresentation()
					&& isAbstractClass( persistentClass.getMappedClass() ) ) {
				LOG.entityMappedAsNonAbstract( name );
			}
		}

		selectBeforeUpdate = persistentClass.hasSelectBeforeUpdate();

		dynamicUpdate = persistentClass.useDynamicUpdate()
				|| ( getBytecodeEnhancementMetadata().isEnhancedForLazyLoading()
					&& getBytecodeEnhancementMetadata().getLazyAttributesMetadata().getFetchGroupNames().size() > 1 );
		dynamicInsert = persistentClass.useDynamicInsert();

		polymorphic = persistentClass.isPolymorphic();
		inherited = persistentClass.isInherited();
		superclass = inherited
				? persistentClass.getSuperclass().getEntityName()
				: null;
		hasSubclasses = persistentClass.hasSubclasses();

		optimisticLockStyle = persistentClass.getOptimisticLockStyle();
		//TODO: move these checks into the Binders
		if ( optimisticLockStyle.isAllOrDirty() ) {
			if ( !dynamicUpdate ) {
				throw new MappingException( "Entity '" + name
											+ "' has 'OptimisticLockType." + optimisticLockStyle
											+ "' but is not annotated '@DynamicUpdate'" );
			}
			if ( versionPropertyIndex != NO_VERSION_INDX ) {
				throw new MappingException( "Entity '" + name
											+ "' has 'OptimisticLockType." + optimisticLockStyle
											+ "' but declares a '@Version' field" );
			}
		}

		hasCollections = foundCollection;
		hasOwnedCollections = foundOwnedCollection;
		mutablePropertiesIndexes = mutableIndexes;

		// Need deterministic ordering
		final Set<String> subclassEntityNamesLocal = new LinkedHashSet<>();
		subclassEntityNamesLocal.add( name );
		for ( Subclass subclass : persistentClass.getSubclasses() ) {
			subclassEntityNamesLocal.add( subclass.getEntityName() );
		}
		subclassEntityNames = toSmallSet( subclassEntityNamesLocal );

//		HashMap<Class<?>, String> entityNameByInheritanceClassMapLocal = new HashMap<>();
//		if ( persistentClass.hasPojoRepresentation() ) {
//			entityNameByInheritanceClassMapLocal.put( persistentClass.getMappedClass(), persistentClass.getEntityName() );
//			for ( Subclass subclass : persistentClass.getSubclasses() ) {
//				entityNameByInheritanceClassMapLocal.put( subclass.getMappedClass(), subclass.getEntityName() );
//			}
//		}
//		entityNameByInheritanceClassMap = toSmallMap( entityNameByInheritanceClassMapLocal );
	}

	private static boolean writePropertyValue(OnExecutionGenerator generator) {
		final boolean writePropertyValue = generator.writePropertyValue();
		// TODO: move this validation somewhere else!
//		if ( !writePropertyValue && generator instanceof BeforeExecutionGenerator ) {
//			throw new HibernateException( "BeforeExecutionGenerator returned false from OnExecutionGenerator.writePropertyValue()" );
//		}
		return writePropertyValue;
	}

	private static Generator buildIdGenerator(String rootName, PersistentClass persistentClass, RuntimeModelCreationContext creationContext) {
		final Generator existing = creationContext.getGenerators().get( rootName );
		if ( existing != null ) {
			return existing;
		}
		else {
			final Generator idgenerator =
					persistentClass.getIdentifier()
							// returns the cached Generator if it was already created
							.createGenerator(
									creationContext.getDialect(),
									persistentClass.getRootClass(),
									persistentClass.getIdentifierProperty(),
									creationContext.getGeneratorSettings()
							);
			creationContext.getGenerators().put( rootName, idgenerator );
			return idgenerator;
		}
	}

	private void verifyNaturalIdProperty(Property property) {
		final Value value = property.getValue();
		if ( value instanceof ManyToOne toOne ) {
			if ( toOne.getNotFoundAction() == NotFoundAction.IGNORE ) {
				throw new MappingException( "Association '" + propertyName( property )
											+ "' marked as '@NaturalId' is also annotated '@NotFound(IGNORE)'"
				);
			}
		}
		else if ( value instanceof Component component ) {
			for ( Property componentProperty : component.getProperties() ) {
				verifyNaturalIdProperty( componentProperty );
			}
		}
	}

	private String propertyName(Property property) {
		return getName() + "." + property.getName();
	}

	private static Generator buildGenerator(
			final String entityName,
			final Property mappingProperty,
			final RuntimeModelCreationContext context) {
		final GeneratorCreator generatorCreator = mappingProperty.getValueGeneratorCreator();
		if ( generatorCreator != null ) {
			final Generator generator = mappingProperty.createGenerator( context );
			if ( generator.generatesSometimes() ) {
				return generator;
			}
		}
		if ( mappingProperty.getValue() instanceof Component component ) {
			final CompositeGeneratorBuilder builder =
					new CompositeGeneratorBuilder( entityName, mappingProperty, context.getDialect() );
			for ( Property property : component.getProperties() ) {
				builder.add( property.createGenerator( context ) );
			}
			return builder.build();
		}
		return null;
	}

	public Generator[] getGenerators() {
		return generators;
	}

	public BeforeExecutionGenerator getVersionGenerator() {
		return versionGenerator;
	}

	private void mapPropertyToIndex(Property property, int i) {
		propertyIndexes.put( property.getName(), i );
		if ( property.getValue() instanceof Component composite ) {
			for ( Property subproperty : composite.getProperties() ) {
				propertyIndexes.put(
						property.getName() + '.' + subproperty.getName(),
						i
					);
			}
		}
	}

	/**
	 * @return {@code true} if one of the properties belonging to the natural id
	 *         is generated during the execution of an {@code insert} statement
	 */
	public boolean isNaturalIdentifierInsertGenerated() {
		if ( naturalIdPropertyNumbers.length == 0 ) {
			throw new IllegalStateException( "Entity '" + name + "' does not have a natural id" );
		}
		for ( int i = 0; i < naturalIdPropertyNumbers.length; i++ ) {
			final Generator strategy = generators[ naturalIdPropertyNumbers[i] ];
			if ( strategy != null && strategy.generatesOnInsert() && strategy.generatedOnExecution() ) {
				return true;
			}
		}
		return false;
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

	public Set<String> getSubclassEntityNames() {
		return subclassEntityNames;
	}

	private static boolean indicatesToOne(Type type) {
		if ( type.isEntityType() ) {
			return true;
		}
		else if ( type instanceof CompositeType compositeType ) {
			for ( Type subtype : compositeType.getSubtypes() ) {
				if ( indicatesToOne( subtype ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean indicatesCollection(Type type) {
		if ( type instanceof CollectionType ) {
			return true;
		}
		else if ( type instanceof CompositeType compositeType ) {
			for ( Type subtype : compositeType.getSubtypes() ) {
				if ( indicatesCollection( subtype ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean indicatesOwnedCollection(Type type, MetadataImplementor metadata) {
		if ( type instanceof CollectionType collectionType ) {
			return !metadata.getCollectionBinding( collectionType.getRole() ).isInverse();
		}
		else if ( type instanceof CompositeType compositeType ) {
			for ( Type subtype : compositeType.getSubtypes() ) {
				if ( indicatesOwnedCollection( subtype, metadata ) ) {
					return true;
				}
			}
			return false;
		}
		else {
			return false;
		}
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

	public int getSubclassId() {
		return subclassId;
	}

	public EntityType getEntityType() {
		if ( entityType == null ) {
			entityType = new ManyToOneType( name, getSessionFactory().getTypeConfiguration() );
		}
		return entityType;
	}

	public IdentifierProperty getIdentifierProperty() {
		return identifierAttribute;
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

	public NonIdentifierAttribute[] getProperties() {
		return properties;
	}

	public int getPropertyIndex(String propertyName) {
		final Integer index = getPropertyIndexOrNull( propertyName );
		if ( index == null ) {
			throw new HibernateException( "Unable to resolve property: " + propertyName );
		}
		return index;
	}

	public Integer getPropertyIndexOrNull(String propertyName) {
		return propertyIndexes.get( propertyName );
	}

	public boolean hasCollections() {
		return hasCollections;
	}

	public boolean hasOwnedCollections() {
		return hasOwnedCollections;
	}

	public boolean hasMutableProperties() {
		return !mutablePropertiesIndexes.isEmpty();
	}

	public BitSet getMutablePropertiesIndexes() {
		return mutablePropertiesIndexes;
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

	public boolean hasToOnes() {
		return hasToOnes;
	}

	public boolean hasCascadeDelete() {
		return hasCascadeDelete;
	}

	public boolean hasCascadePersist() {
		return hasCascadePersist;
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

	/**
	 * @deprecated No longer supported
	 */
	@Deprecated
	public boolean isExplicitPolymorphism() {
		return false;
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

//	/**
//	 * Return the entity-name mapped to the given class within our inheritance hierarchy, if any.
//	 *
//	 * @param inheritanceClass The class for which to resolve the entity-name.
//	 * @return The mapped entity-name, or null if no such mapping was found.
//	 */
//	public String findEntityNameByEntityClass(Class<?> inheritanceClass) {
//		return entityNameByInheritanceClassMap.get( inheritanceClass );
//	}

	@Override
	public String toString() {
		return "EntityMetamodel(" + name + ':' + ArrayHelper.toString( properties ) + ')';
	}

	// temporary ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	public String[] getPropertyNames() {
		return propertyNames;
	}

	public Type[] getPropertyTypes() {
		return propertyTypes;
	}

	public @Nullable Type[] getDirtyCheckablePropertyTypes() {
		return dirtyCheckablePropertyTypes;
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

	public boolean[] getPropertyNullability() {
		return propertyNullability;
	}

	public boolean[] getPropertyVersionability() {
		return propertyVersionability;
	}

	public CascadeStyle[] getCascadeStyles() {
		return cascadeStyles;
	}

	public boolean hasPreInsertGeneratedValues() {
		return hasPreInsertGeneratedValues;
	}

	public boolean hasPreUpdateGeneratedValues() {
		return hasPreUpdateGeneratedValues;
	}

	public boolean hasInsertGeneratedValues() {
		return hasInsertGeneratedValues;
	}

	public boolean hasUpdateGeneratedValues() {
		return hasUpdateGeneratedValues;
	}

	/**
	 * Whether this class can be lazy (ie intercepted)
	 */
	public boolean isInstrumented() {
		return bytecodeEnhancementMetadata.isEnhancedForLazyLoading();
	}

	public BytecodeEnhancementMetadata getBytecodeEnhancementMetadata() {
		return bytecodeEnhancementMetadata;
	}

	public OnDeleteAction[] getPropertyOnDeleteActions() {
		return propertyOnDeleteActions;
	}
}
