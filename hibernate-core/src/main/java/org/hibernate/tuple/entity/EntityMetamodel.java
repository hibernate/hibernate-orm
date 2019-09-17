/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementHelper;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.IdentifierProperty;
import org.hibernate.tuple.InDatabaseValueGenerationStrategy;
import org.hibernate.tuple.InMemoryValueGenerationStrategy;
import org.hibernate.tuple.NonIdentifierAttribute;
import org.hibernate.tuple.PropertyFactory;
import org.hibernate.tuple.ValueGeneration;
import org.hibernate.tuple.ValueGenerator;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * Centralizes metamodel information about an entity.
 *
 * @author Steve Ebersole
 */
public class EntityMetamodel implements Serializable {
	private static final CoreMessageLogger LOG = messageLogger( EntityMetamodel.class );

	private static final int NO_VERSION_INDX = -66;

	private final SessionFactoryImplementor sessionFactory;

	private final String name;
	private final String rootName;
	private EntityType entityType;

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

	private final InMemoryValueGenerationStrategy[] inMemoryValueGenerationStrategies;
	private final InDatabaseValueGenerationStrategy[] inDatabaseValueGenerationStrategies;
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private final Map<String, Integer> propertyIndexes = new HashMap<>();
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
	private final BytecodeEnhancementMetadata bytecodeEnhancementMetadata;

	public EntityMetamodel(
			PersistentClass persistentClass,
			EntityPersister persister,
			SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;

		name = persistentClass.getEntityName();
		rootName = persistentClass.getRootClass().getEntityName();

		identifierAttribute = PropertyFactory.buildIdentifierAttribute(
				persistentClass,
				sessionFactory.getIdentifierGenerator( rootName )
		);

		versioned = persistentClass.isVersioned();

		if ( persistentClass.hasPojoRepresentation() ) {
			final Component identifierMapperComponent = persistentClass.getIdentifierMapper();
			final CompositeType nonAggregatedCidMapper;
			final Set<String> idAttributeNames;

			if ( identifierMapperComponent != null ) {
				nonAggregatedCidMapper = (CompositeType) identifierMapperComponent.getType();
				idAttributeNames = new HashSet<>( );
				//noinspection unchecked
				final Iterator<Property> propertyItr = identifierMapperComponent.getPropertyIterator();
				while ( propertyItr.hasNext() ) {
					idAttributeNames.add( propertyItr.next().getName() );
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
					sessionFactory.getSessionFactoryOptions().isEnhancementAsProxyEnabled()
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
		this.inMemoryValueGenerationStrategies = new InMemoryValueGenerationStrategy[propertySpan];
		this.inDatabaseValueGenerationStrategies = new InDatabaseValueGenerationStrategy[propertySpan];

		boolean foundPreInsertGeneratedValues = false;
		boolean foundPreUpdateGeneratedValues = false;
		boolean foundPostInsertGeneratedValues = false;
		boolean foundPostUpdateGeneratedValues = false;
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		Iterator iter = persistentClass.getPropertyClosureIterator();
		int i = 0;
		int tempVersionProperty = NO_VERSION_INDX;
		boolean foundCascade = false;
		boolean foundCollection = false;
		boolean foundMutable = false;
		boolean foundNonIdentifierPropertyNamedId = false;
		boolean foundUpdateableNaturalIdProperty = false;

		while ( iter.hasNext() ) {
			Property prop = ( Property ) iter.next();

			if ( prop == persistentClass.getVersion() ) {
				tempVersionProperty = i;
				properties[i] = PropertyFactory.buildVersionProperty(
						persister,
						sessionFactory,
						i,
						prop,
						bytecodeEnhancementMetadata.isEnhancedForLazyLoading()
				);
			}
			else {
				properties[i] = PropertyFactory.buildEntityBasedAttribute(
						persister,
						sessionFactory,
						i,
						prop,
						bytecodeEnhancementMetadata.isEnhancedForLazyLoading()
				);
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
			boolean lazy = ! EnhancementHelper.includeInBaseFetchGroup(
					prop,
					bytecodeEnhancementMetadata.isEnhancedForLazyLoading(),
					sessionFactory.getSessionFactoryOptions().isEnhancementAsProxyEnabled()
			);

			if ( lazy ) {
				hasLazy = true;
			}

			propertyLaziness[i] = lazy;

			propertyNames[i] = properties[i].getName();
			propertyTypes[i] = properties[i].getType();
			propertyNullability[i] = properties[i].isNullable();
			propertyUpdateability[i] = properties[i].isUpdateable();
			propertyInsertability[i] = properties[i].isInsertable();
			propertyVersionability[i] = properties[i].isVersionable();
			nonlazyPropertyUpdateability[i] = properties[i].isUpdateable() && !lazy;
			propertyCheckability[i] = propertyUpdateability[i] ||
					( propertyTypes[i].isAssociationType() && ( (AssociationType) propertyTypes[i] ).isAlwaysDirtyChecked() );

			cascadeStyles[i] = properties[i].getCascadeStyle();
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			// generated value strategies ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			GenerationStrategyPair pair = buildGenerationStrategyPair( sessionFactory, prop );
			inMemoryValueGenerationStrategies[i] = pair.getInMemoryStrategy();
			inDatabaseValueGenerationStrategies[i] = pair.getInDatabaseStrategy();

			if ( pair.getInMemoryStrategy() != null ) {
				final GenerationTiming timing = pair.getInMemoryStrategy().getGenerationTiming();
				if ( timing != GenerationTiming.NEVER ) {
					final ValueGenerator generator = pair.getInMemoryStrategy().getValueGenerator();
					if ( generator != null ) {
						// we have some level of generation indicated
						if ( timing == GenerationTiming.INSERT ) {
							foundPreInsertGeneratedValues = true;
						}
						else if ( timing == GenerationTiming.ALWAYS ) {
							foundPreInsertGeneratedValues = true;
							foundPreUpdateGeneratedValues = true;
						}
					}
				}
			}
			if (  pair.getInDatabaseStrategy() != null ) {
				final GenerationTiming timing =  pair.getInDatabaseStrategy().getGenerationTiming();
				if ( timing == GenerationTiming.INSERT ) {
					foundPostInsertGeneratedValues = true;
				}
				else if ( timing == GenerationTiming.ALWAYS ) {
					foundPostInsertGeneratedValues = true;
					foundPostUpdateGeneratedValues = true;
				}
			}
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			if ( properties[i].isLazy() ) {
				hasLazy = true;
			}

			if ( properties[i].getCascadeStyle() != CascadeStyles.NONE ) {
				foundCascade = true;
			}

			if ( indicatesCollection( properties[i].getType() ) ) {
				foundCollection = true;
			}

			if ( propertyTypes[i].isMutable() && propertyCheckability[i] ) {
				foundMutable = true;
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

		this.hasPreInsertGeneratedValues = foundPreInsertGeneratedValues;
		this.hasPreUpdateGeneratedValues = foundPreUpdateGeneratedValues;
		this.hasInsertGeneratedValues = foundPostInsertGeneratedValues;
		this.hasUpdateGeneratedValues = foundPostUpdateGeneratedValues;

		hasCascades = foundCascade;
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
			isAbstract = persistentClass.isAbstract().booleanValue();
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
		final EntityTuplizerFactory entityTuplizerFactory = sessionFactory.getSessionFactoryOptions().getEntityTuplizerFactory();
		final String tuplizerClassName = persistentClass.getTuplizerImplClassName( entityMode );
		if ( tuplizerClassName == null ) {
			entityTuplizer = entityTuplizerFactory.constructDefaultTuplizer( entityMode, this, persistentClass );
		}
		else {
			entityTuplizer = entityTuplizerFactory.constructTuplizer( tuplizerClassName, this, persistentClass );
		}
	}

	private static GenerationStrategyPair buildGenerationStrategyPair(
			final SessionFactoryImplementor sessionFactory,
			final Property mappingProperty) {
		final ValueGeneration valueGeneration = mappingProperty.getValueGenerationStrategy();
		if ( valueGeneration != null && valueGeneration.getGenerationTiming() != GenerationTiming.NEVER ) {
			// the property is generated in full. build the generation strategy pair.
			if ( valueGeneration.getValueGenerator() != null ) {
				// in-memory generation
				return new GenerationStrategyPair(
						FullInMemoryValueGenerationStrategy.create( valueGeneration )
				);
			}
			else {
				// in-db generation
				return new GenerationStrategyPair(
						create(
								sessionFactory,
								mappingProperty,
								valueGeneration
						)
				);
			}
		}
		else if ( mappingProperty.getValue() instanceof Component ) {
			final CompositeGenerationStrategyPairBuilder builder = new CompositeGenerationStrategyPairBuilder( mappingProperty );
			interpretPartialCompositeValueGeneration( sessionFactory, (Component) mappingProperty.getValue(), builder );
			return builder.buildPair();
		}

		return NO_GEN_PAIR;
	}

	private static final GenerationStrategyPair NO_GEN_PAIR = new GenerationStrategyPair();

	private static void interpretPartialCompositeValueGeneration(
			SessionFactoryImplementor sessionFactory,
			Component composite,
			CompositeGenerationStrategyPairBuilder builder) {
		Iterator subProperties = composite.getPropertyIterator();
		while ( subProperties.hasNext() ) {
			final Property subProperty = (Property) subProperties.next();
			builder.addPair( buildGenerationStrategyPair( sessionFactory, subProperty ) );
		}
	}

	public static InDatabaseValueGenerationStrategyImpl create(
			SessionFactoryImplementor sessionFactoryImplementor,
			Property mappingProperty,
			ValueGeneration valueGeneration) {
		final int numberOfMappedColumns = mappingProperty.getType().getColumnSpan( sessionFactoryImplementor );
		if ( numberOfMappedColumns == 1 ) {
			return new InDatabaseValueGenerationStrategyImpl(
					valueGeneration.getGenerationTiming(),
					valueGeneration.referenceColumnInSql(),
					new String[] { valueGeneration.getDatabaseGeneratedReferencedColumnValue() }

			);
		}
		else {
			if ( valueGeneration.getDatabaseGeneratedReferencedColumnValue() != null ) {
				LOG.debugf(
						"Value generator specified column value in reference to multi-column attribute [%s -> %s]; ignoring",
						mappingProperty.getPersistentClass(),
						mappingProperty.getName()
				);
			}
			return new InDatabaseValueGenerationStrategyImpl(
					valueGeneration.getGenerationTiming(),
					valueGeneration.referenceColumnInSql(),
					new String[numberOfMappedColumns]
			);
		}
	}

	public static class GenerationStrategyPair {
		private final InMemoryValueGenerationStrategy inMemoryStrategy;
		private final InDatabaseValueGenerationStrategy inDatabaseStrategy;

		public GenerationStrategyPair() {
			this( NoInMemoryValueGenerationStrategy.INSTANCE, NoInDatabaseValueGenerationStrategy.INSTANCE );
		}

		public GenerationStrategyPair(FullInMemoryValueGenerationStrategy inMemoryStrategy) {
			this( inMemoryStrategy, NoInDatabaseValueGenerationStrategy.INSTANCE );
		}

		public GenerationStrategyPair(InDatabaseValueGenerationStrategyImpl inDatabaseStrategy) {
			this( NoInMemoryValueGenerationStrategy.INSTANCE, inDatabaseStrategy );
		}

		public GenerationStrategyPair(
				InMemoryValueGenerationStrategy inMemoryStrategy,
				InDatabaseValueGenerationStrategy inDatabaseStrategy) {
			// perform some normalization.  Also check that only one (if any) strategy is specified
			if ( inMemoryStrategy == null ) {
				inMemoryStrategy = NoInMemoryValueGenerationStrategy.INSTANCE;
			}
			if ( inDatabaseStrategy == null ) {
				inDatabaseStrategy = NoInDatabaseValueGenerationStrategy.INSTANCE;
			}

			if ( inMemoryStrategy.getGenerationTiming() != GenerationTiming.NEVER
					&& inDatabaseStrategy.getGenerationTiming() != GenerationTiming.NEVER ) {
				throw new ValueGenerationStrategyException(
						"in-memory and in-database value generation are mutually exclusive"
				);
			}

			this.inMemoryStrategy = inMemoryStrategy;
			this.inDatabaseStrategy = inDatabaseStrategy;
		}

		public InMemoryValueGenerationStrategy getInMemoryStrategy() {
			return inMemoryStrategy;
		}

		public InDatabaseValueGenerationStrategy getInDatabaseStrategy() {
			return inDatabaseStrategy;
		}
	}

	public static class ValueGenerationStrategyException extends HibernateException {
		public ValueGenerationStrategyException(String message) {
			super( message );
		}
	}

	private static class CompositeGenerationStrategyPairBuilder {
		private final Property mappingProperty;

		private boolean hadInMemoryGeneration;
		private boolean hadInDatabaseGeneration;

		private List<InDatabaseValueGenerationStrategy> inDatabaseStrategies;

		public CompositeGenerationStrategyPairBuilder(Property mappingProperty) {
			this.mappingProperty = mappingProperty;
		}

		public void addPair(GenerationStrategyPair generationStrategyPair) {
			add( generationStrategyPair.getInMemoryStrategy() );
			add( generationStrategyPair.getInDatabaseStrategy() );
		}

		private void add(InMemoryValueGenerationStrategy inMemoryStrategy) {
			if ( inMemoryStrategy.getGenerationTiming() != GenerationTiming.NEVER ) {
				hadInMemoryGeneration = true;
			}
		}

		private void add(InDatabaseValueGenerationStrategy inDatabaseStrategy) {
			if ( inDatabaseStrategies == null ) {
				inDatabaseStrategies = new ArrayList<>();
			}
			inDatabaseStrategies.add( inDatabaseStrategy );

			if ( inDatabaseStrategy.getGenerationTiming() != GenerationTiming.NEVER ) {
				hadInDatabaseGeneration = true;
			}
		}

		public GenerationStrategyPair buildPair() {
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

				// we need the numbers to match up so we can properly handle 'referenced sql column values'
				if ( inDatabaseStrategies.size() != composite.getPropertySpan() ) {
					throw new ValueGenerationStrategyException(
							"Internal error : mismatch between number of collected in-db generation strategies" +
									" and number of attributes for composite attribute : " + mappingProperty.getName()
					);
				}

				// the base-line values for the aggregated InDatabaseValueGenerationStrategy we will build here.
				GenerationTiming timing = GenerationTiming.INSERT;
				boolean referenceColumns = false;
				String[] columnValues = new String[ composite.getColumnSpan() ];

				// start building the aggregate values
				int propertyIndex = -1;
				int columnIndex = 0;
				Iterator subProperties = composite.getPropertyIterator();
				while ( subProperties.hasNext() ) {
					propertyIndex++;
					final Property subProperty = (Property) subProperties.next();
					final InDatabaseValueGenerationStrategy subStrategy = inDatabaseStrategies.get( propertyIndex );

					if ( subStrategy.getGenerationTiming() == GenerationTiming.ALWAYS ) {
						// override the base-line to the more often "ALWAYS"...
						timing = GenerationTiming.ALWAYS;

					}
					if ( subStrategy.referenceColumnsInSql() ) {
						// override base-line value
						referenceColumns = true;
					}
					if ( subStrategy.getReferencedColumnValues() != null ) {
						if ( subStrategy.getReferencedColumnValues().length != subProperty.getColumnSpan() ) {
							throw new ValueGenerationStrategyException(
									"Internal error : mismatch between number of collected 'referenced column values'" +
											" and number of columns for composite attribute : " + mappingProperty.getName() +
											'.' + subProperty.getName()
							);
						}
						System.arraycopy(
								subStrategy.getReferencedColumnValues(),
								0,
								columnValues,
								columnIndex,
								subProperty.getColumnSpan()
						);
					}
				}

				// then use the aggregated values to build the InDatabaseValueGenerationStrategy
				return new GenerationStrategyPair(
						new InDatabaseValueGenerationStrategyImpl( timing, referenceColumns, columnValues )
				);
			}
			else {
				return NO_GEN_PAIR;
			}
		}
	}

	private static class NoInMemoryValueGenerationStrategy implements InMemoryValueGenerationStrategy {
		/**
		 * Singleton access
		 */
		public static final NoInMemoryValueGenerationStrategy INSTANCE = new NoInMemoryValueGenerationStrategy();

		@Override
		public GenerationTiming getGenerationTiming() {
			return GenerationTiming.NEVER;
		}

		@Override
		public ValueGenerator getValueGenerator() {
			return null;
		}
	}

	private static class FullInMemoryValueGenerationStrategy implements InMemoryValueGenerationStrategy {
		private final GenerationTiming timing;
		private final ValueGenerator generator;

		private FullInMemoryValueGenerationStrategy(GenerationTiming timing, ValueGenerator generator) {
			this.timing = timing;
			this.generator = generator;
		}

		public static FullInMemoryValueGenerationStrategy create(ValueGeneration valueGeneration) {
			return new FullInMemoryValueGenerationStrategy(
					valueGeneration.getGenerationTiming(),
					valueGeneration.getValueGenerator()
			);
		}

		@Override
		public GenerationTiming getGenerationTiming() {
			return timing;
		}

		@Override
		public ValueGenerator getValueGenerator() {
			return generator;
		}
	}

	private static class NoInDatabaseValueGenerationStrategy implements InDatabaseValueGenerationStrategy {
		/**
		 * Singleton access
		 */
		public static final NoInDatabaseValueGenerationStrategy INSTANCE = new NoInDatabaseValueGenerationStrategy();

		@Override
		public GenerationTiming getGenerationTiming() {
			return GenerationTiming.NEVER;
		}

		@Override
		public boolean referenceColumnsInSql() {
			return true;
		}

		@Override
		public String[] getReferencedColumnValues() {
			return null;
		}
	}

	private static class InDatabaseValueGenerationStrategyImpl implements InDatabaseValueGenerationStrategy {
		private final GenerationTiming timing;
		private final boolean referenceColumnInSql;
		private final String[] referencedColumnValues;

		private InDatabaseValueGenerationStrategyImpl(
				GenerationTiming timing,
				boolean referenceColumnInSql,
				String[] referencedColumnValues) {
			this.timing = timing;
			this.referenceColumnInSql = referenceColumnInSql;
			this.referencedColumnValues = referencedColumnValues;
		}

		@Override
		public GenerationTiming getGenerationTiming() {
			return timing;
		}

		@Override
		public boolean referenceColumnsInSql() {
			return referenceColumnInSql;
		}

		@Override
		public String[] getReferencedColumnValues() {
			return referencedColumnValues;
		}
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

	public EntityTuplizer getTuplizer() {
		return entityTuplizer;
	}

	public boolean isNaturalIdentifierInsertGenerated() {
		// the intention is for this call to replace the usage of the old ValueInclusion stuff (as exposed from
		// persister) in SelectGenerator to determine if it is safe to use the natural identifier to find the
		// insert-generated identifier.  That wont work if the natural-id is also insert-generated.
		//
		// Assumptions:
		//		* That code checks that there is a natural identifier before making this call, so we assume the same here
		// 		* That code assumes a non-composite natural-id, so we assume the same here
		final InDatabaseValueGenerationStrategy strategy = inDatabaseValueGenerationStrategies[ naturalIdPropertyNumbers[0] ];
		return strategy != null && strategy.getGenerationTiming() != GenerationTiming.NEVER;
	}

	public boolean isVersionGenerated() {
		final InDatabaseValueGenerationStrategy strategy = inDatabaseValueGenerationStrategies[ versionPropertyIndex ];
		return strategy != null && strategy.getGenerationTiming() != GenerationTiming.NEVER;
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
			for ( Type subtype : subtypes ) {
					if ( indicatesCollection( subtype ) ) {
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
		if ( entityType == null ) {
			entityType = sessionFactory.getTypeResolver().getTypeFactory().manyToOne( name );
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

	public InMemoryValueGenerationStrategy[] getInMemoryValueGenerationStrategies() {
		return inMemoryValueGenerationStrategies;
	}

	public InDatabaseValueGenerationStrategy[] getInDatabaseValueGenerationStrategies() {
		return inDatabaseValueGenerationStrategies;
	}

	public EntityMode getEntityMode() {
		return entityMode;
	}

	/**
	 * Whether or not this class can be lazy (ie intercepted)
	 */
	public boolean isInstrumented() {
		return bytecodeEnhancementMetadata.isEnhancedForLazyLoading();
	}

	public BytecodeEnhancementMetadata getBytecodeEnhancementMetadata() {
		return bytecodeEnhancementMetadata;
	}
}
