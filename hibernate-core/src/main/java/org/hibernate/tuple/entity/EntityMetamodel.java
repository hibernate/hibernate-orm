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
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.IdentifierProperty;
import org.hibernate.tuple.NonIdentifierAttribute;
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
import static org.hibernate.bytecode.enhance.spi.interceptor.EnhancementHelper.includeInBaseFetchGroup;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.ReflectHelper.isAbstractClass;
import static org.hibernate.internal.util.ReflectHelper.isFinalClass;
import static org.hibernate.internal.util.collections.ArrayHelper.toIntArray;
import static org.hibernate.internal.util.collections.CollectionHelper.toSmallSet;
import static org.hibernate.tuple.PropertyFactory.buildEntityBasedAttribute;
import static org.hibernate.tuple.PropertyFactory.buildIdentifierAttribute;
import static org.hibernate.tuple.PropertyFactory.buildVersionProperty;

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

	public static final int NO_VERSION_INDX = -66;

	private final SessionFactoryImplementor sessionFactory;

	private final String name;
	private final String rootName;
	private final EntityType entityType;

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
			RuntimeModelCreationContext creationContext) {
		this( persistentClass, creationContext,
				rootName -> creationContext.getOrCreateIdGenerator( rootName, persistentClass ) );
	}

	/*
	 * Used by Hibernate Reactive to adapt the id generators
	 */
	public EntityMetamodel(
			PersistentClass persistentClass,
			RuntimeModelCreationContext creationContext,
			Function<String, Generator> generatorSupplier) {
		sessionFactory = creationContext.getSessionFactory();

		// Improves performance of EntityKey#equals by avoiding content check in String#equals
		name = persistentClass.getEntityName().intern();
		rootName = persistentClass.getRootClass().getEntityName().intern();
		// Make sure the hashCodes are cached
		//noinspection ResultOfMethodCallIgnored
		name.hashCode();
		//noinspection ResultOfMethodCallIgnored
		rootName.hashCode();

		entityType = new ManyToOneType( name, creationContext.getTypeConfiguration() );

		subclassId = persistentClass.getSubclassId();

		identifierAttribute =
				buildIdentifierAttribute(
						persistentClass,
						generatorSupplier.apply( rootName )
				);

		versioned = persistentClass.isVersioned();

		final boolean collectionsInDefaultFetchGroup =
				creationContext.getSessionFactoryOptions()
						.isCollectionsInDefaultFetchGroupEnabled();

		bytecodeEnhancementMetadata =
				bytecodeEnhancementMetadata(
						persistentClass,
						identifierAttribute,
						creationContext,
						collectionsInDefaultFetchGroup
				);

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

		final boolean supportsCascadeDelete = creationContext.getDialect().supportsCascadeDelete();

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

		final var props = persistentClass.getPropertyClosure();
		for ( int i=0; i<props.size(); i++ ) {
			final var property = props.get(i);
			if ( property == persistentClass.getVersion() ) {
				tempVersionProperty = i;
			}
			final var attribute = buildAttribute( persistentClass, creationContext, property, i );
			properties[i] = attribute;

			if ( property.isNaturalIdentifier() ) {
				verifyNaturalIdProperty( property );
				naturalIdNumbers.add( i );
				if ( property.isUpdatable() ) {
					foundUpdateableNaturalIdProperty = true;
				}
			}

			if ( "id".equals( property.getName() ) ) {
				foundNonIdentifierPropertyNamedId = true;
			}

			// temporary ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			final boolean lazy =
					isLazy( creationContext, property, collectionsInDefaultFetchGroup, bytecodeEnhancementMetadata );
			if ( lazy ) {
				hasLazy = true;
			}
			propertyLaziness[i] = lazy;
			propertyNames[i] = attribute.getName();
			final var propertyType = attribute.getType();
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
					|| propertyType instanceof AssociationType associationType
							&& associationType.isAlwaysDirtyChecked();
			propertyOnDeleteActions[i] = supportsCascadeDelete ? attribute.getOnDeleteAction() : null;
			cascadeStyles[i] = attribute.getCascadeStyle();
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			// generated value strategies ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			final var generator = buildGenerator( name, property, creationContext );
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

			if ( indicatesToOne( propertyType ) ) {
				foundToOne = true;
			}
			if ( indicatesCollection( propertyType ) ) {
				foundCollection = true;
			}
			if ( indicatesOwnedCollection( propertyType, creationContext.getMetadata() ) ) {
				foundOwnedCollection = true;
			}

			// Component types are dirty-tracked as well, so they
			// are not exactly mutable for the "maybeDirty" check
			if ( propertyType.isMutable()
					&& propertyCheckability[i]
					&& !( propertyType instanceof ComponentType ) ) {
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
			CORE_LOGGER.lazyPropertyFetchingAvailable( name );
		}

		lazy = isLazy( persistentClass, bytecodeEnhancementMetadata );

		mutable = persistentClass.isMutable();
		isAbstract = isAbstract( persistentClass );

		selectBeforeUpdate = persistentClass.hasSelectBeforeUpdate();

		dynamicUpdate = persistentClass.useDynamicUpdate() || hasMultipleFetchGroups( bytecodeEnhancementMetadata );
		dynamicInsert = persistentClass.useDynamicInsert();

		polymorphic = persistentClass.isPolymorphic();
		inherited = persistentClass.isInherited();
		superclass = inherited ? persistentClass.getSuperclass().getEntityName() : null;
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

		subclassEntityNames = collectSubclassEntityNames( persistentClass );

//		HashMap<Class<?>, String> entityNameByInheritanceClassMapLocal = new HashMap<>();
//		if ( persistentClass.hasPojoRepresentation() ) {
//			entityNameByInheritanceClassMapLocal.put( persistentClass.getMappedClass(), persistentClass.getEntityName() );
//			for ( Subclass subclass : persistentClass.getSubclasses() ) {
//				entityNameByInheritanceClassMapLocal.put( subclass.getMappedClass(), subclass.getEntityName() );
//			}
//		}
//		entityNameByInheritanceClassMap = toSmallMap( entityNameByInheritanceClassMapLocal );
	}

	private Set<String> collectSubclassEntityNames(PersistentClass persistentClass) {
		final Set<String> entityNames = new LinkedHashSet<>(); // Need deterministic ordering
		entityNames.add( name );
		for ( var subclass : persistentClass.getSubclasses() ) {
			entityNames.add( subclass.getEntityName() );
		}
		return toSmallSet( entityNames );
	}

	private static boolean isAbstract(PersistentClass persistentClass) {
		final Boolean isAbstract = persistentClass.isAbstract();
		if ( isAbstract == null ) {
			// legacy behavior (with no abstract attribute specified)
			return persistentClass.hasPojoRepresentation()
				&& isAbstractClass( persistentClass.getMappedClass() );
		}
		else {
			if ( !isAbstract
					&& persistentClass.hasPojoRepresentation()
					&& isAbstractClass( persistentClass.getMappedClass() ) ) {
				CORE_LOGGER.entityMappedAsNonAbstract( persistentClass.getEntityName() );
			}
			return isAbstract;
		}
	}

	//TODO: move this down to AbstractEntityPersister
	private NonIdentifierAttribute buildAttribute(
			PersistentClass persistentClass,
			RuntimeModelCreationContext creationContext,
			Property property,
			int index) {
		if ( property == persistentClass.getVersion() ) {
			return buildVersionProperty(
					(EntityPersister) this,
					sessionFactory,
					index,
					property,
					bytecodeEnhancementMetadata.isEnhancedForLazyLoading()
			);
		}
		else {
			return buildEntityBasedAttribute(
					(EntityPersister) this,
					sessionFactory,
					index,
					property,
					bytecodeEnhancementMetadata.isEnhancedForLazyLoading(),
					creationContext
			);
		}
	}

	private boolean isLazy(PersistentClass persistentClass, BytecodeEnhancementMetadata enhancementMetadata) {
		return persistentClass.isLazy()
				// TODO: this disables laziness even in non-pojo entity modes:
				&& ( !persistentClass.hasPojoRepresentation() || !isFinalClass( persistentClass.getProxyInterface() ) )
			|| enhancementMetadata.isEnhancedForLazyLoading();
	}

	private boolean hasMultipleFetchGroups(BytecodeEnhancementMetadata enhancementMetadata) {
		return enhancementMetadata.isEnhancedForLazyLoading()
			&& enhancementMetadata.getLazyAttributesMetadata().getFetchGroupNames().size() > 1;
	}

	private static boolean isLazy(
			RuntimeModelCreationContext creationContext,
			Property property,
			boolean collectionsInDefaultFetchGroupEnabled,
			BytecodeEnhancementMetadata enhancementMetadata) {
		return !includeInBaseFetchGroup(
				property,
				enhancementMetadata.isEnhancedForLazyLoading(),
				entityName -> {
					final var entityBinding =
							creationContext.getMetadata().getEntityBinding( entityName );
					assert entityBinding != null;
					return entityBinding.hasSubclasses();
				},
				collectionsInDefaultFetchGroupEnabled
		);
	}

	private BytecodeEnhancementMetadata bytecodeEnhancementMetadata(
			PersistentClass persistentClass,
			IdentifierProperty identifierAttribute,
			RuntimeModelCreationContext creationContext,
			boolean collectionsInDefaultFetchGroupEnabled) {
		if ( persistentClass.hasPojoRepresentation() ) {
			final var identifierMapperComponent = persistentClass.getIdentifierMapper();
			final CompositeType nonAggregatedCidMapper;
			final Set<String> idAttributeNames;
			if ( identifierMapperComponent != null ) {
				nonAggregatedCidMapper = identifierMapperComponent.getType();
				final HashSet<String> propertyNames = new HashSet<>();
				for ( var property : identifierMapperComponent.getProperties() ) {
					propertyNames.add( property.getName() );
				}
				idAttributeNames = toSmallSet( unmodifiableSet( propertyNames ) );
			}
			else {
				nonAggregatedCidMapper = null;
				idAttributeNames = singleton( identifierAttribute.getName() );
			}

			return getBytecodeEnhancementMetadataPojo(
					persistentClass,
					creationContext,
					idAttributeNames,
					nonAggregatedCidMapper,
					collectionsInDefaultFetchGroupEnabled
			);
		}
		else {
			return new BytecodeEnhancementMetadataNonPojoImpl( persistentClass.getEntityName() );
		}
	}

	/*
	 * Used by Hibernate Reactive
	 */
	protected BytecodeEnhancementMetadata getBytecodeEnhancementMetadataPojo(PersistentClass persistentClass, RuntimeModelCreationContext creationContext, Set<String> idAttributeNames, CompositeType nonAggregatedCidMapper, boolean collectionsInDefaultFetchGroupEnabled) {
		return BytecodeEnhancementMetadataPojoImpl.from(
				persistentClass,
				idAttributeNames,
				nonAggregatedCidMapper,
				collectionsInDefaultFetchGroupEnabled,
				creationContext.getMetadata()
		);
	}

	private static boolean writePropertyValue(OnExecutionGenerator generator) {
		final boolean writePropertyValue = generator.writePropertyValue();
		// TODO: move this validation somewhere else!
//		if ( !writePropertyValue && generator instanceof BeforeExecutionGenerator ) {
//			throw new HibernateException( "BeforeExecutionGenerator returned false from OnExecutionGenerator.writePropertyValue()" );
//		}
		return writePropertyValue;
	}

	private void verifyNaturalIdProperty(Property property) {
		final var value = property.getValue();
		if ( value instanceof ManyToOne toOne ) {
			if ( toOne.getNotFoundAction() == NotFoundAction.IGNORE ) {
				throw new MappingException( "Association '" + propertyName( property )
											+ "' marked as '@NaturalId' is also annotated '@NotFound(IGNORE)'"
				);
			}
		}
		else if ( value instanceof Component component ) {
			for ( var componentProperty : component.getProperties() ) {
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
		final var generatorCreator = mappingProperty.getValueGeneratorCreator();
		if ( generatorCreator != null ) {
			final var generator = mappingProperty.createGenerator( context );
			if ( generator.generatesSometimes() ) {
				return generator;
			}
		}
		if ( mappingProperty.getValue() instanceof Component component ) {
			final var builder =
					new CompositeGeneratorBuilder( entityName, mappingProperty, context.getDialect() );
			for ( var property : component.getProperties() ) {
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
			for ( var subproperty : composite.getProperties() ) {
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
		for ( int naturalIdPropertyNumber : naturalIdPropertyNumbers ) {
			final var strategy = generators[naturalIdPropertyNumber];
			if ( strategy != null
					&& strategy.generatesOnInsert()
					&& strategy.generatedOnExecution() ) {
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
			for ( var subtype : compositeType.getSubtypes() ) {
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
			for ( var subtype : compositeType.getSubtypes() ) {
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
			for ( var subtype : compositeType.getSubtypes() ) {
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
		return NO_VERSION_INDX == versionPropertyIndex
				? null
				: (VersionProperty) properties[versionPropertyIndex];
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
