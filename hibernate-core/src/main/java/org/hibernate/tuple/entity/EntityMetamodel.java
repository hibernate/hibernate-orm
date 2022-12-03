/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementHelper;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.GeneratorCreator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Subclass;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.generator.Generator;
import org.hibernate.tuple.IdentifierProperty;
import org.hibernate.generator.InDatabaseGenerator;
import org.hibernate.generator.InMemoryGenerator;
import org.hibernate.tuple.NonIdentifierAttribute;
import org.hibernate.tuple.PropertyFactory;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.Type;

import static org.hibernate.internal.CoreLogging.messageLogger;

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
	private final boolean[] propertyLaziness;
	private final boolean[] propertyUpdateability;
	private final boolean[] nonlazyPropertyUpdateability;
	private final boolean[] propertyCheckability;
	private final boolean[] propertyInsertability;
	private final boolean[] propertyNullability;
	private final boolean[] propertyVersionability;
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
	private final boolean hasCascadeDelete;
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
	private final Set<String> subclassEntityNames;
	private final Map<Class<?>,String> entityNameByInheritanceClassMap;

	private final BytecodeEnhancementMetadata bytecodeEnhancementMetadata;

	@Deprecated(since = "6.0")
	public EntityMetamodel(
			PersistentClass persistentClass,
			EntityPersister persister,
			PersisterCreationContext creationContext) {
		this( persistentClass, persister, (RuntimeModelCreationContext) creationContext );
	}

	public EntityMetamodel(
			PersistentClass persistentClass,
			EntityPersister persister,
			RuntimeModelCreationContext creationContext) {
		this.sessionFactory = creationContext.getSessionFactory();

		// Improves performance of EntityKey#equals by avoiding content check in String#equals
		name = persistentClass.getEntityName().intern();
		rootName = persistentClass.getRootClass().getEntityName().intern();
		subclassId = persistentClass.getSubclassId();

		identifierAttribute = PropertyFactory.buildIdentifierAttribute(
				persistentClass,
				sessionFactory.getGenerator( rootName )
		);

		versioned = persistentClass.isVersioned();

		SessionFactoryOptions sessionFactoryOptions = sessionFactory.getSessionFactoryOptions();

		if ( persistentClass.hasPojoRepresentation() ) {
			final Component identifierMapperComponent = persistentClass.getIdentifierMapper();
			final CompositeType nonAggregatedCidMapper;
			final Set<String> idAttributeNames;

			if ( identifierMapperComponent != null ) {
				nonAggregatedCidMapper = (CompositeType) identifierMapperComponent.getType();
				idAttributeNames = new HashSet<>( );
				for ( Property property : identifierMapperComponent.getProperties() ) {
					idAttributeNames.add( property.getName() );
				}
			}
			else {
				nonAggregatedCidMapper = null;
				idAttributeNames = Collections.singleton( identifierAttribute.getName() );
			}

			bytecodeEnhancementMetadata = BytecodeEnhancementMetadataPojoImpl.from(
					persistentClass,
					idAttributeNames,
					nonAggregatedCidMapper,
					sessionFactoryOptions.isCollectionsInDefaultFetchGroupEnabled(),
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
		propertyUpdateability = new boolean[propertySpan];
		propertyInsertability = new boolean[propertySpan];
		nonlazyPropertyUpdateability = new boolean[propertySpan];
		propertyCheckability = new boolean[propertySpan];
		propertyNullability = new boolean[propertySpan];
		propertyVersionability = new boolean[propertySpan];
		propertyLaziness = new boolean[propertySpan];
		cascadeStyles = new CascadeStyle[propertySpan];
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		// generated value strategies ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		this.generators = new Generator[propertySpan];

		boolean foundPreInsertGeneratedValues = false;
		boolean foundPreUpdateGeneratedValues = false;
		boolean foundPostInsertGeneratedValues = false;
		boolean foundPostUpdateGeneratedValues = false;
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		int tempVersionProperty = NO_VERSION_INDX;
		boolean foundCascade = false;
		boolean foundCascadeDelete = false;
		boolean foundCollection = false;
		boolean foundOwnedCollection = false;
		BitSet mutableIndexes = new BitSet();
		boolean foundNonIdentifierPropertyNamedId = false;
		boolean foundUpdateableNaturalIdProperty = false;

		List<Property> props = persistentClass.getPropertyClosure();
		for ( int i=0; i<props.size(); i++ ) {
			Property property = props.get(i);
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
					sessionFactoryOptions.isCollectionsInDefaultFetchGroupEnabled()
			);

			if ( lazy ) {
				hasLazy = true;
			}

			propertyLaziness[i] = lazy;

			propertyNames[i] = attribute.getName();
			final Type propertyType = attribute.getType();
			propertyTypes[i] = propertyType;
			propertyNullability[i] = attribute.isNullable();
			propertyUpdateability[i] = attribute.isUpdateable();
			propertyInsertability[i] = attribute.isInsertable();
			propertyVersionability[i] = attribute.isVersionable();
			nonlazyPropertyUpdateability[i] = attribute.isUpdateable() && !lazy;
			propertyCheckability[i] = propertyUpdateability[i] ||
					( propertyType.isAssociationType() && ( (AssociationType) propertyType ).isAlwaysDirtyChecked() );

			cascadeStyles[i] = attribute.getCascadeStyle();
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			// generated value strategies ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			final Generator generator = buildGenerator( property, creationContext );
			generators[i] = generator;
			if ( generator != null ) {
				if ( generatedWithNoParameter( generator ) ) {
					propertyInsertability[i] = false;
					propertyUpdateability[i] = false;
				}
				if ( generator.generatedOnInsert() ) {
					if ( generator.generatedByDatabase() ) {
						foundPostInsertGeneratedValues = true;
					}
					else {
						foundPreInsertGeneratedValues = true;
					}
				}
				if ( generator.generatedOnUpdate() ) {
					if ( generator.generatedByDatabase() ) {
						foundPostUpdateGeneratedValues = true;
					}
					else {
						foundPreUpdateGeneratedValues = true;
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
			if ( cascadeStyles[i].doCascade(CascadingActions.DELETE) ) {
				foundCascadeDelete = true;
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

			mapPropertyToIndex(property, i);
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

		this.hasPreInsertGeneratedValues = foundPreInsertGeneratedValues;
		this.hasPreUpdateGeneratedValues = foundPreUpdateGeneratedValues;
		this.hasInsertGeneratedValues = foundPostInsertGeneratedValues;
		this.hasUpdateGeneratedValues = foundPostUpdateGeneratedValues;

		hasCascades = foundCascade;
		hasCascadeDelete = foundCascadeDelete;
		hasNonIdentifierPropertyNamedId = foundNonIdentifierPropertyNamedId;
		versionPropertyIndex = tempVersionProperty;
		hasLazyProperties = hasLazy;
		if (hasLazyProperties) {
			LOG.lazyPropertyFetchingAvailable(name);
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
			isAbstract = persistentClass.isAbstract();
			if ( !isAbstract && persistentClass.hasPojoRepresentation() &&
					ReflectHelper.isAbstractClass( persistentClass.getMappedClass() ) ) {
				LOG.entityMappedAsNonAbstract(name);
			}
		}

		selectBeforeUpdate = persistentClass.hasSelectBeforeUpdate();

		dynamicUpdate = persistentClass.useDynamicUpdate()
				|| ( getBytecodeEnhancementMetadata().isEnhancedForLazyLoading() && getBytecodeEnhancementMetadata().getLazyAttributesMetadata().getFetchGroupNames().size() > 1 );
		dynamicInsert = persistentClass.useDynamicInsert();

		polymorphic = persistentClass.isPolymorphic();
		explicitPolymorphism = persistentClass.isExplicitPolymorphism();
		inherited = persistentClass.isInherited();
		superclass = inherited ?
				persistentClass.getSuperclass().getEntityName() :
				null;
		hasSubclasses = persistentClass.hasSubclasses();

		optimisticLockStyle = persistentClass.getOptimisticLockStyle();
		final boolean isAllOrDirty = optimisticLockStyle.isAllOrDirty();
		if ( isAllOrDirty && !dynamicUpdate ) {
			throw new MappingException( "optimistic-lock=all|dirty requires dynamic-update=\"true\": " + name );
		}
		if ( versionPropertyIndex != NO_VERSION_INDX && isAllOrDirty ) {
			throw new MappingException( "version and optimistic-lock=all|dirty are not a valid combination : " + name );
		}

		hasCollections = foundCollection;
		hasOwnedCollections = foundOwnedCollection;
		mutablePropertiesIndexes = mutableIndexes;

		final Set<String> subclassEntityNamesLocal = new HashSet<>();
		for ( Subclass subclass : persistentClass.getSubclasses() ) {
			subclassEntityNamesLocal.add( subclass.getEntityName() );
		}
		subclassEntityNamesLocal.add( name );
		subclassEntityNames = CollectionHelper.toSmallSet( subclassEntityNamesLocal );

		HashMap<Class<?>, String> entityNameByInheritanceClassMapLocal = new HashMap<>();
		if ( persistentClass.hasPojoRepresentation() ) {
			entityNameByInheritanceClassMapLocal.put( persistentClass.getMappedClass(), persistentClass.getEntityName() );
			for ( Subclass subclass : persistentClass.getSubclasses() ) {
				entityNameByInheritanceClassMapLocal.put( subclass.getMappedClass(), subclass.getEntityName() );
			}
		}
		entityNameByInheritanceClassMap = CollectionHelper.toSmallMap( entityNameByInheritanceClassMapLocal );
	}

	private static boolean generatedWithNoParameter(Generator generator) {
		return generator.generatedByDatabase()
			&& !((InDatabaseGenerator) generator).writePropertyValue();
	}

	private static Generator buildGenerator(
			final Property mappingProperty,
			final RuntimeModelCreationContext context) {
		final GeneratorCreator generatorCreator = mappingProperty.getValueGeneratorCreator();
		if ( generatorCreator != null ) {
			final Generator generator = mappingProperty.createGenerator( context );
			if ( generator.isNotNever() ) {
				return generator;
			}
		}
		if ( mappingProperty.getValue() instanceof Component ) {
			final Dialect dialect = context.getSessionFactory().getJdbcServices().getDialect();
			final CompositeGeneratorBuilder builder = new CompositeGeneratorBuilder( mappingProperty, dialect );
			final Component component = (Component) mappingProperty.getValue();
			for ( Property property : component.getProperties() ) {
				builder.addPair( property.createGenerator( context ) );
			}
			return builder.build();
		}
		return null;
	}

	public Generator[] getGenerators() {
		return generators;
	}

	public static class ValueGenerationStrategyException extends HibernateException {
		public ValueGenerationStrategyException(String message) {
			super( message );
		}
	}

	private static class CompositeGeneratorBuilder {
		private final Property mappingProperty;
		private final Dialect dialect;

		private boolean hadInMemoryGeneration;
		private boolean hadInDatabaseGeneration;

		private List<InDatabaseGenerator> inDatabaseStrategies;

		public CompositeGeneratorBuilder(Property mappingProperty, Dialect dialect) {
			this.mappingProperty = mappingProperty;
			this.dialect = dialect;
		}

		public void addPair(Generator generator) {
			if ( generator != null ) {
				if ( generator.generatedByDatabase() ) {
					if ( generator instanceof InDatabaseGenerator ) {
						add( (InDatabaseGenerator) generator );
					}
				}
				else {
					if ( generator instanceof InMemoryGenerator ) {
						add( (InMemoryGenerator) generator );
					}
				}
			}
		}

		private void add(InMemoryGenerator inMemoryStrategy) {
			if ( inMemoryStrategy.isNotNever() ) {
				hadInMemoryGeneration = true;
			}
		}

		private void add(InDatabaseGenerator inDatabaseStrategy) {
			if ( inDatabaseStrategies == null ) {
				inDatabaseStrategies = new ArrayList<>();
			}
			inDatabaseStrategies.add( inDatabaseStrategy );

			if ( inDatabaseStrategy.isNotNever() ) {
				hadInDatabaseGeneration = true;
			}
		}

		public Generator build() {
			if ( hadInMemoryGeneration && hadInDatabaseGeneration ) {
				throw new ValueGenerationStrategyException(
						"Composite attribute [" + mappingProperty.getName() + "] contained both in-memory"
								+ " and in-database value generation"
				);
			}
			else if ( hadInMemoryGeneration ) {
				throw new NotYetImplementedException( "Still need to wire in composite in-memory value generation" );

			}
			else if ( hadInDatabaseGeneration ) {
				final Component composite = (Component) mappingProperty.getValue();

				// we need the numbers to match up so that we can properly handle 'referenced sql column values'
				if ( inDatabaseStrategies.size() != composite.getPropertySpan() ) {
					throw new ValueGenerationStrategyException(
							"Internal error : mismatch between number of collected in-db generation strategies" +
									" and number of attributes for composite attribute : " + mappingProperty.getName()
					);
				}

				// the base-line values for the aggregated InDatabaseValueGenerationStrategy we will build here.
				boolean generatedOnInsert = false;
				boolean generatedOnUpdate = false;
				boolean referenceColumns = false;
				String[] columnValues = new String[ composite.getColumnSpan() ];

				// start building the aggregate values
				int propertyIndex = -1;
				int columnIndex = 0;
				for ( Property property : composite.getProperties() ) {
					propertyIndex++;
					final InDatabaseGenerator subStrategy = inDatabaseStrategies.get( propertyIndex );
					generatedOnUpdate = generatedOnUpdate || subStrategy.generatedOnUpdate();
					generatedOnInsert = generatedOnInsert || subStrategy.generatedOnInsert();
					if ( subStrategy.referenceColumnsInSql(dialect) ) {
						// override base-line value
						referenceColumns = true;
					}
					if ( subStrategy.getReferencedColumnValues(dialect) != null ) {
						if ( subStrategy.getReferencedColumnValues(dialect).length != property.getColumnSpan() ) {
							throw new ValueGenerationStrategyException(
									"Internal error : mismatch between number of collected 'referenced column values'" +
											" and number of columns for composite attribute : " + mappingProperty.getName() +
											'.' + property.getName()
							);
						}
						System.arraycopy(
								subStrategy.getReferencedColumnValues(dialect),
								0,
								columnValues,
								columnIndex,
								property.getColumnSpan()
						);
					}
				}

				// then use the aggregated values to build the InDatabaseValueGenerationStrategy
				return new InDatabaseGeneratorImpl( generatedOnUpdate, generatedOnInsert, referenceColumns, columnValues );
			}
			else {
				return new Generator() {
					@Override
					public boolean generatedOnInsert() {
						return false;
					}
					@Override
					public boolean generatedOnUpdate() {
						return false;
					}
					@Override
					public boolean generatedByDatabase() {
						return false;
					}
				};
			}
		}
	}

	private static class InDatabaseGeneratorImpl implements InDatabaseGenerator {
		private final boolean generatedOnUpdate;
		private final boolean generatedOnInsert;
		private final boolean referenceColumnInSql;
		private final String[] referencedColumnValues;

		private InDatabaseGeneratorImpl(
				boolean generatedOnUpdate,
				boolean generatedOnInsert,
				boolean referenceColumnInSql,
				String[] referencedColumnValues) {
			this.generatedOnUpdate = generatedOnUpdate;
			this.generatedOnInsert = generatedOnInsert;
			this.referenceColumnInSql = referenceColumnInSql;
			this.referencedColumnValues = referencedColumnValues;
		}

		@Override
		public boolean generatedOnInsert() {
			return generatedOnInsert;
		}

		@Override
		public boolean generatedOnUpdate() {
			return generatedOnUpdate;
		}

		@Override
		public boolean referenceColumnsInSql(Dialect dialect) {
			return referenceColumnInSql;
		}

		@Override
		public String[] getReferencedColumnValues(Dialect dialect) {
			return referencedColumnValues;
		}

		@Override
		public boolean writePropertyValue() {
			return false;
		}
	}


	private void mapPropertyToIndex(Property prop, int i) {
		propertyIndexes.put( prop.getName(), i );
		if ( prop.getValue() instanceof Component ) {
			Component composite = (Component) prop.getValue();
			for ( Property subprop : composite.getProperties() ) {
				propertyIndexes.put(
						prop.getName() + '.' + subprop.getName(),
						i
					);
			}
		}
	}

	public boolean isNaturalIdentifierInsertGenerated() {
		// the intention is for this call to replace the usage of the old ValueInclusion stuff (as exposed from
		// persister) in SelectGenerator to determine if it is safe to use the natural identifier to find the
		// insert-generated identifier.  That wont work if the natural-id is also insert-generated.
		//
		// Assumptions:
		//		* That code checks that there is a natural identifier before making this call, so we assume the same here
		// 		* That code assumes a non-composite natural-id, so we assume the same here
		final Generator strategy = generators[ naturalIdPropertyNumbers[0] ];
		return strategy != null && strategy.isNotNever();
	}

	public boolean isVersionGeneratedByDatabase() {
		final Generator strategy = generators[ versionPropertyIndex ];
		return strategy != null && strategy.isNotNever() && strategy.generatedByDatabase();
	}

	public boolean isVersionGeneratedInMemory() {
		final Generator strategy = generators[ versionPropertyIndex ];
		return strategy != null && strategy.isNotNever() && !strategy.generatedByDatabase();
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

	private static boolean indicatesCollection(Type type) {
		if ( type.isCollectionType() ) {
			return true;
		}
		else if ( type.isComponentType() ) {
			Type[] subtypes = ( (CompositeType) type ).getSubtypes();
			for ( Type subtype : subtypes ) {
				if ( indicatesCollection( subtype ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean indicatesOwnedCollection(Type type, MetadataImplementor metadata) {
		if ( type.isCollectionType() ) {
			String role = ( (CollectionType) type ).getRole();
			return !metadata.getCollectionBinding( role ).isInverse();
		}
		else if ( type.isComponentType() ) {
			Type[] subtypes = ( (CompositeType) type ).getSubtypes();
			for ( Type subtype : subtypes ) {
				if ( indicatesOwnedCollection( subtype, metadata ) ) {
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
		Integer index = getPropertyIndexOrNull(propertyName);
		if ( index == null ) {
			throw new HibernateException("Unable to resolve property: " + propertyName);
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

	public boolean hasCascadeDelete() {
		return hasCascadeDelete;
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
	 * @param inheritanceClass The class for which to resolve the entity-name.
	 * @return The mapped entity-name, or null if no such mapping was found.
	 */
	public String findEntityNameByEntityClass(Class<?> inheritanceClass) {
		return entityNameByInheritanceClassMap.get( inheritanceClass );
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
}
