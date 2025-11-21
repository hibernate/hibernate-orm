/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.hibernate.EntityFilterException;
import org.hibernate.FetchNotFoundException;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.WrongClassException;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.internal.util.ImmutableBitSet;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.proxy.map.MapProxy;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.UnfetchedResultAssembler;
import org.hibernate.sql.results.graph.basic.BasicResultAssembler;
import org.hibernate.sql.results.graph.collection.internal.AbstractImmediateCollectionInitializer;
import org.hibernate.sql.results.graph.collection.internal.UnfetchedCollectionAssembler;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;
import org.hibernate.sql.results.graph.internal.AbstractInitializer;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.MutabilityPlan;

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.UNFETCHED_PROPERTY;
import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.internal.log.LoggingHelper.toLoggableString;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.loader.internal.CacheLoadHelper.loadFromSecondLevelCache;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * @author Andrea Boriero
 */
public class EntityInitializerImpl
		extends AbstractInitializer<EntityInitializerImpl.EntityInitializerData>
		implements EntityInitializer<EntityInitializerImpl.EntityInitializerData> {

	// NOTE: even though we only keep the EntityDescriptor here, rather than EntityResultGraphNode,
	//		the "scope" of this initializer is a specific EntityReference.
	//
	//		The full EntityResultGraphNode is simply not needed here, and so we just keep
	//		the EntityDescriptor here to avoid chicken/egg issues in the creation of
	// 		these

	private final EntityValuedModelPart referencedModelPart;
	private final EntityPersister entityDescriptor;
	private final EntityPersister rootEntityDescriptor;
	private final @Nullable Type keyTypeForEqualsHashCode;
	private final NavigablePath navigablePath;
	private final String sourceAlias;
	private final @Nullable InitializerParent<?> parent;
	private final NotFoundAction notFoundAction;
	private final boolean affectedByFilter;
	private final boolean isPartOfKey;
	private final boolean isResultInitializer;
	private final boolean hasKeyManyToOne;
	/**
	 * Indicates whether there is a high chance of the previous row to have the same entity key as the current row
	 * and hence enable a check in the {@link #resolveKey(RowProcessingState)} phase which compare the previously read
	 * identifier with the current row identifier. If it matches, the state from the previous row processing can be reused.
	 * In addition to that, all direct sub-initializers can be informed about the reuse by calling {@link Initializer#resolveFromPreviousRow(RowProcessingState)},
	 * so that these initializers can avoid unnecessary processing as well.
	 */
	private final boolean previousRowReuse;
	private final boolean couldUseEmbeddedIdentifierInstanceAsEntity;

	private final @Nullable DomainResultAssembler<?> keyAssembler;
	private final @Nullable DomainResultAssembler<?> identifierAssembler;
	private final @Nullable BasicResultAssembler<?> discriminatorAssembler;
	private final @Nullable DomainResultAssembler<?> versionAssembler;
	private final @Nullable DomainResultAssembler<Object> rowIdAssembler;

	private final DomainResultAssembler<?>[][] assemblers;
	private final @Nullable Initializer<?>[] allInitializers;
	private final @Nullable Initializer<?>[][] subInitializers;
	private final @Nullable Initializer<?>[][] eagerSubInitializers;
	private final @Nullable Initializer<?>[][] collectionContainingSubInitializers;
	private final MutabilityPlan<Object>[][] updatableAttributeMutabilityPlans;
	private final ImmutableBitSet[] lazySets;
	private final ImmutableBitSet[] maybeLazySets;
	private final boolean hasLazySubInitializers;
	private final boolean hasLazyInitializingSubAssemblers;

	public static class EntityInitializerData extends InitializerData {

		protected final boolean shallowCached;
		protected final LockMode lockMode;
		protected final String uniqueKeyAttributePath;
		protected final Type[] uniqueKeyPropertyTypes;
		protected final boolean canUseEmbeddedIdentifierInstanceAsEntity;
		protected final boolean hasCallbackActions;
		protected final @Nullable EntityPersister defaultConcreteDescriptor;

		// per-row state
		protected @Nullable EntityPersister concreteDescriptor;
		protected @Nullable EntityKey entityKey;
		protected @Nullable Object entityInstanceForNotify;
		protected @Nullable EntityHolder entityHolder;

		public EntityInitializerData(EntityInitializerImpl initializer, RowProcessingState rowProcessingState) {
			super( rowProcessingState );
			final var entityDescriptor = initializer.entityDescriptor;
			shallowCached = rowProcessingState.isQueryCacheHit() && entityDescriptor.useShallowQueryCacheLayout();
			lockMode = rowProcessingState.determineEffectiveLockMode( initializer.sourceAlias );
			if ( initializer.isResultInitializer() ) {
				uniqueKeyAttributePath = rowProcessingState.getEntityUniqueKeyAttributePath();
				uniqueKeyPropertyTypes =
						uniqueKeyAttributePath != null
								? initializer.getParentEntityAttributeTypes( uniqueKeyAttributePath )
								: null;
				canUseEmbeddedIdentifierInstanceAsEntity =
						rowProcessingState.getEntityId() != null
								&& initializer.couldUseEmbeddedIdentifierInstanceAsEntity;
			}
			else {
				uniqueKeyAttributePath = null;
				uniqueKeyPropertyTypes = null;
				canUseEmbeddedIdentifierInstanceAsEntity = false;
			}
			hasCallbackActions = rowProcessingState.hasCallbackActions();
			defaultConcreteDescriptor =
					hasConcreteDescriptor( rowProcessingState, initializer.discriminatorAssembler, entityDescriptor )
							? entityDescriptor
							: null;
		}

		/*
		 * Used by Hibernate Reactive
		 */
		public EntityInitializerData(EntityInitializerData original) {
			super( original );
			this.shallowCached = original.shallowCached;
			this.lockMode = original.lockMode;
			this.uniqueKeyAttributePath = original.uniqueKeyAttributePath;
			this.uniqueKeyPropertyTypes = original.uniqueKeyPropertyTypes;
			this.canUseEmbeddedIdentifierInstanceAsEntity = original.canUseEmbeddedIdentifierInstanceAsEntity;
			this.hasCallbackActions = original.hasCallbackActions;
			this.defaultConcreteDescriptor = original.defaultConcreteDescriptor;
			this.concreteDescriptor = original.concreteDescriptor;
			this.entityKey = original.entityKey;
			this.entityInstanceForNotify = original.entityInstanceForNotify;
			this.entityHolder = original.entityHolder;
		}
	}

	private static boolean hasConcreteDescriptor(
			RowProcessingState rowProcessingState,
			BasicResultAssembler<?> discriminatorAssembler,
			EntityPersister entityDescriptor) {
		return discriminatorAssembler == null
			|| rowProcessingState.isQueryCacheHit()
				&& entityDescriptor.useShallowQueryCacheLayout()
				&& !entityDescriptor.storeDiscriminatorInShallowQueryCacheLayout();
	}

	public EntityInitializerImpl(
			EntityResultGraphNode resultDescriptor,
			String sourceAlias,
			@Nullable Fetch identifierFetch,
			@Nullable Fetch discriminatorFetch,
			@Nullable DomainResult<?> keyResult,
			@Nullable DomainResult<Object> rowIdResult,
			NotFoundAction notFoundAction,
			boolean affectedByFilter,
			@Nullable InitializerParent<?> parent,
			boolean isResultInitializer,
			AssemblerCreationState creationState) {
		super( creationState );
		this.sourceAlias = sourceAlias;
		this.parent = parent;
		this.isResultInitializer = isResultInitializer;

		referencedModelPart = resultDescriptor.getEntityValuedModelPart();
		entityDescriptor = (EntityPersister) referencedModelPart.getEntityMappingType();

		final String rootEntityName = entityDescriptor.getRootEntityName();
		rootEntityDescriptor =
				rootEntityName == null || rootEntityName.equals( entityDescriptor.getEntityName() )
						? entityDescriptor
						: entityDescriptor.getRootEntityDescriptor().getEntityPersister();
		keyTypeForEqualsHashCode = entityDescriptor.getIdentifierType().getTypeForEqualsHashCode();
		// The id can only be the entity instance if this is a non-aggregated id that has no containing class
		couldUseEmbeddedIdentifierInstanceAsEntity =
				entityDescriptor.getIdentifierMapping() instanceof CompositeIdentifierMapping composite
						&& !composite.hasContainingClass();

		navigablePath = resultDescriptor.getNavigablePath();
		isPartOfKey = Initializer.isPartOfKey( navigablePath, parent );
		// If the parent already has previous row reuse enabled, we can skip that here
		previousRowReuse = !isPreviousRowReuse( parent ) && (
				// If this entity domain result contains a collection join fetch, this usually means that the entity data is
				// duplicate in the result data for every collection element. Since collections usually have more than one element,
				// optimizing the resolving of the entity data is very beneficial.
				resultDescriptor.containsCollectionFetches()
						// Result duplication generally also happens if more than one collection is join fetched,
						|| creationState.containsMultipleCollectionFetches()
		);

		assert identifierFetch != null || isResultInitializer : "Identifier must be fetched, unless this is a result initializer";
		if ( identifierFetch == null ) {
			identifierAssembler = null;
			hasKeyManyToOne = false;
		}
		else {
			identifierAssembler = identifierFetch.createAssembler( this, creationState );
			final var initializer = identifierAssembler.getInitializer();
			hasKeyManyToOne = initializer != null && initializer.isLazyCapable();
		}

		assert entityDescriptor.hasSubclasses() == (discriminatorFetch != null)
				: "Discriminator should only be fetched if the entity has subclasses";
		discriminatorAssembler =
				discriminatorFetch == null
						? null
						: (BasicResultAssembler<?>)
								discriminatorFetch.createAssembler( this, creationState );

		final var versionMapping = entityDescriptor.getVersionMapping();
		if ( versionMapping != null ) {
			final var versionFetch = resultDescriptor.findFetch( versionMapping );
			// If there is a version mapping, there must be a fetch for it
			assert versionFetch != null;
			versionAssembler = versionFetch.createAssembler( this, creationState );
		}
		else {
			versionAssembler = null;
		}

		rowIdAssembler =
				rowIdResult == null
						? null :
						rowIdResult.createResultAssembler( this, creationState );

		final int fetchableCount = entityDescriptor.getNumberOfFetchables();
		final var subMappingTypes = rootEntityDescriptor.getSubMappingTypes();
		final int size = subMappingTypes.size() + 1;
		final var assemblers = new DomainResultAssembler[size][];
		final var allInitializers = new Initializer<?>[fetchableCount];
		final var subInitializers = new Initializer<?>[size][];
		final var eagerSubInitializers = new Initializer<?>[size][];
		final var collectionContainingSubInitializers = new Initializer<?>[size][];
		final var lazySets = new BitSet[size];
		final var maybeLazySets = new BitSet[size];
		final var updatableAttributeMutabilityPlans = new MutabilityPlan[size][];
		final int rootId = rootEntityDescriptor.getSubclassId();
		final int rootNumberOfFetchables = rootEntityDescriptor.getNumberOfFetchables();
		final int numberOfRootAttributeMappings = rootEntityDescriptor.getNumberOfAttributeMappings();
		assemblers[rootId] = new DomainResultAssembler[rootNumberOfFetchables];
		updatableAttributeMutabilityPlans[rootId] = new MutabilityPlan[numberOfRootAttributeMappings];

		for ( var subMappingType : subMappingTypes ) {
			final int subclassId = subMappingType.getSubclassId();
			final int numberOfFetchables = subMappingType.getNumberOfFetchables();
			final int numberOfAttributeMappings = subMappingType.getNumberOfAttributeMappings();
			assemblers[subclassId] = new DomainResultAssembler[numberOfFetchables];
			updatableAttributeMutabilityPlans[subclassId] = new MutabilityPlan[numberOfAttributeMappings];
		}

		boolean hasLazySubInitializers = false;
		boolean hasLazyInitializingSubAssemblers = false;
		for ( int i = 0; i < fetchableCount; i++ ) {
			final var attributeMapping = entityDescriptor.getFetchable( i ).asAttributeMapping();
			final var fetch = resultDescriptor.findFetch( attributeMapping );
			final var stateAssembler =
					fetch == null
							? new NullValueAssembler<>( attributeMapping.getMappedType().getMappedJavaType() )
							: fetch.createAssembler( this, creationState );

			final int stateArrayPosition = attributeMapping.getStateArrayPosition();
			final var declaringType = attributeMapping.getDeclaringType().asEntityMappingType();
			final int subclassId = declaringType.getSubclassId();

			final var subInitializer = stateAssembler.getInitializer();
			if ( subInitializer != null ) {
				allInitializers[i] = subInitializer;
				if ( subInitializers[subclassId] == null ) {
					subInitializers[subclassId] = new Initializer<?>[fetchableCount];
					eagerSubInitializers[subclassId] = new Initializer<?>[fetchableCount];
					collectionContainingSubInitializers[subclassId] = new Initializer<?>[fetchableCount];
					if ( lazySets[subclassId] == null ) {
						lazySets[subclassId] = new BitSet( fetchableCount );
						maybeLazySets[subclassId] = new BitSet( fetchableCount );
					}
				}
				subInitializers[subclassId][stateArrayPosition] = subInitializer;
				if ( subInitializer.isEager() ) {
					eagerSubInitializers[subclassId][stateArrayPosition] = subInitializer;
					if ( subInitializer.hasLazySubInitializers() ) {
						maybeLazySets[subclassId].set( stateArrayPosition );
						hasLazySubInitializers = true;
					}
					assert fetch != null;
					final var fetchParent = fetch.asFetchParent();
					if ( fetchParent != null && fetchParent.containsCollectionFetches()
							|| subInitializer.isCollectionInitializer() ) {
						collectionContainingSubInitializers[subclassId][stateArrayPosition] = subInitializer;
					}
				}
				else {
					// Lazy initializer
					lazySets[subclassId].set( stateArrayPosition );
					maybeLazySets[subclassId].set( stateArrayPosition );
					hasLazySubInitializers = true;
				}
			}
			else if ( stateAssembler instanceof UnfetchedResultAssembler<?>
					|| stateAssembler instanceof UnfetchedCollectionAssembler ) {
				if ( lazySets[subclassId] == null ) {
					lazySets[subclassId] = new BitSet( fetchableCount );
					maybeLazySets[subclassId] = new BitSet( fetchableCount );
				}
				// Lazy basic attribute
				lazySets[subclassId].set( stateArrayPosition );
				maybeLazySets[subclassId].set( stateArrayPosition );
			}
			else if ( attributeMapping.getMappedFetchOptions().getTiming() == FetchTiming.DELAYED ) {
				// The state assembler for this lazy basic attribute will initialize the attribute
				hasLazyInitializingSubAssemblers = true;
			}

			assemblers[subclassId][stateArrayPosition] = stateAssembler;
			final var attributeMetadata = attributeMapping.getAttributeMetadata();
			if ( attributeMetadata.isUpdatable() ) {
				updatableAttributeMutabilityPlans[subclassId][stateArrayPosition] = attributeMetadata.getMutabilityPlan();
			}
			for ( var subMappingType : declaringType.getSubMappingTypes() ) {
				assemblers[subMappingType.getSubclassId()][stateArrayPosition] = stateAssembler;
				updatableAttributeMutabilityPlans[subMappingType.getSubclassId()][stateArrayPosition] =
						updatableAttributeMutabilityPlans[subclassId][stateArrayPosition];
				if ( subInitializer != null ) {
					if ( subInitializers[subMappingType.getSubclassId()] == null ) {
						subInitializers[subMappingType.getSubclassId()] = new Initializer<?>[fetchableCount];
						eagerSubInitializers[subMappingType.getSubclassId()] = new Initializer<?>[fetchableCount];
						collectionContainingSubInitializers[subMappingType.getSubclassId()] = new Initializer<?>[fetchableCount];
					}
					subInitializers[subMappingType.getSubclassId()][stateArrayPosition] = subInitializer;
					eagerSubInitializers[subMappingType.getSubclassId()][stateArrayPosition] =
							eagerSubInitializers[subclassId][stateArrayPosition];
					collectionContainingSubInitializers[subMappingType.getSubclassId()][stateArrayPosition] =
							collectionContainingSubInitializers[subclassId][stateArrayPosition];
				}
				if ( lazySets[subclassId] != null ) {
					assert maybeLazySets[subclassId] != null;
					if ( lazySets[subMappingType.getSubclassId()] == null ) {
						lazySets[subMappingType.getSubclassId()] = new BitSet( fetchableCount );
						maybeLazySets[subMappingType.getSubclassId()] = new BitSet( fetchableCount );
					}
					if ( lazySets[subclassId].get( stateArrayPosition ) ) {
						lazySets[subMappingType.getSubclassId()].set( stateArrayPosition );
					}
					if ( maybeLazySets[subclassId].get( stateArrayPosition ) ) {
						maybeLazySets[subMappingType.getSubclassId()].set( stateArrayPosition );
					}
				}
			}
		}
		for ( int i = 0; i < subInitializers.length; i++ ) {
			boolean emptySubInitializers = true;
			if ( subInitializers[i] != null ) {
				for ( var initializer : subInitializers[i] ) {
					if ( initializer != null ) {
						emptySubInitializers = false;
						break;
					}
				}
			}
			if ( emptySubInitializers ) {
				subInitializers[i] = Initializer.EMPTY_ARRAY;
			}

			boolean emptyContainingSubInitializers = true;
			if ( collectionContainingSubInitializers[i] != null ) {
				for ( Initializer<?> initializer : collectionContainingSubInitializers[i] ) {
					if ( initializer != null ) {
						emptyContainingSubInitializers = false;
						break;
					}
				}
			}
			if ( emptyContainingSubInitializers ) {
				collectionContainingSubInitializers[i] = Initializer.EMPTY_ARRAY;
			}
			boolean emptyEagerSubInitializers = true;
			if ( eagerSubInitializers[i] != null ) {
				for ( Initializer<?> initializer : eagerSubInitializers[i] ) {
					if ( initializer != null ) {
						emptyEagerSubInitializers = false;
						break;
					}
				}
			}
			if ( emptyEagerSubInitializers ) {
				eagerSubInitializers[i] = Initializer.EMPTY_ARRAY;
			}
		}

		this.assemblers = assemblers;
		this.allInitializers = allInitializers;
		this.subInitializers = subInitializers;
		this.eagerSubInitializers = eagerSubInitializers;
		this.collectionContainingSubInitializers = collectionContainingSubInitializers;
		this.lazySets = toBitSetArray( lazySets );
		this.maybeLazySets = toBitSetArray( maybeLazySets );
		this.hasLazySubInitializers = hasLazySubInitializers;
		this.hasLazyInitializingSubAssemblers = hasLazyInitializingSubAssemblers;
		this.updatableAttributeMutabilityPlans = updatableAttributeMutabilityPlans;
		this.notFoundAction = notFoundAction;

		this.keyAssembler = keyResult == null ? null : keyResult.createResultAssembler( this, creationState );
		this.affectedByFilter = affectedByFilter;
	}

	private static ImmutableBitSet[] toBitSetArray(BitSet[] lazySets) {
		return Arrays.stream( lazySets )
				.map( ImmutableBitSet::valueOfOrEmpty )
				.toArray( ImmutableBitSet[]::new );
	}

	private static boolean isPreviousRowReuse(@Nullable InitializerParent<?> parent) {
		// Traverse up the parents to find out if one of our parents has row reuse enabled
		while ( parent != null ) {
			if ( parent instanceof EntityInitializerImpl entityInitializer ) {
				return entityInitializer.isPreviousRowReuse();
			}
			// Immediate collections don't reuse previous rows for elements, so we can safely assume false
			if ( parent instanceof AbstractImmediateCollectionInitializer<?> ) {
				return false;
			}
			parent = parent.getParent();
		}
		return false;
	}

	@Override
	protected EntityInitializerData createInitializerData(RowProcessingState rowProcessingState) {
		return new EntityInitializerData( this, rowProcessingState );
	}

	@Override
	public void resolveKey(EntityInitializerData data) {
		resolveKey( data, false );
	}

	@Override
	public @Nullable Object getEntityIdentifier(EntityInitializerData data) {
		return data.entityKey == null ? null : data.entityKey.getIdentifier();
	}

	@Override
	public @Nullable EntityKey resolveEntityKeyOnly(RowProcessingState rowProcessingState) {
		assert identifierAssembler != null;
		final var data = getData( rowProcessingState );
		resolveKey( data, true );
		try {
			if ( data.getState() == State.MISSING ) {
				return null;
			}
			if ( data.entityKey == null ) {
				final Object id = identifierAssembler.assemble( rowProcessingState );
				if ( id == null ) {
					setMissing( data );
					return null;
				}
				resolveEntityKey( data, id );
			}
			return data.entityKey;
		}
		finally {
			final var initializer = identifierAssembler.getInitializer();
			if ( hasKeyManyToOne && initializer != null ) {
				final var embeddableInitializer = initializer.asEmbeddableInitializer();
				assert embeddableInitializer != null;
				embeddableInitializer.resetResolvedEntityRegistrations( rowProcessingState );
			}
		}
	}

	@Override
	public void resetResolvedEntityRegistrations(RowProcessingState rowProcessingState) {
		final var data = getData( rowProcessingState );
		if ( data.getState() == State.RESOLVED ) {
			rowProcessingState.getSession()
					.getPersistenceContextInternal()
					.removeEntityHolder( data.entityKey );
			data.entityKey = null;
			data.entityHolder = null;
			data.entityInstanceForNotify = null;
			data.setInstance( null );
		}
	}

	protected void resolveKey(EntityInitializerData data, boolean entityKeyOnly) {
		// todo (6.0) : atm we do not handle sequential selects
		// 		- see AbstractEntityPersister#hasSequentialSelect and
		//			AbstractEntityPersister#getSequentialSelect in 5.2
		if ( data.getState() == State.UNINITIALIZED ) {
			data.setState( State.KEY_RESOLVED );

			final var oldEntityKey = data.entityKey;
			final Object oldEntityInstance = data.getInstance();
			final Object oldEntityInstanceForNotify = data.entityInstanceForNotify;
			final var oldEntityHolder = data.entityHolder;
			// reset row state
			final var concreteDescriptor = data.concreteDescriptor = data.defaultConcreteDescriptor;
			data.entityKey = null;
			data.setInstance( null );
			data.entityInstanceForNotify = null;
			data.entityHolder = null;

			final var rowProcessingState = data.getRowProcessingState();
			final Object id;
			if ( identifierAssembler == null ) {
				id = rowProcessingState.getEntityId();
				assert id != null : "Initializer requires a not null id for loading";
			}
			else {
				//noinspection unchecked
				final var initializer =
						(Initializer<InitializerData>)
								identifierAssembler.getInitializer();
				if ( initializer != null ) {
					final var subData = initializer.getData( rowProcessingState );
					initializer.resolveKey( subData );
					if ( subData.getState() == State.MISSING ) {
						setMissing( data );
						return;
					}
					else {
						if ( concreteDescriptor == null ) {
							data.concreteDescriptor = determineConcreteEntityDescriptor(
									rowProcessingState,
									discriminatorAssembler,
									entityDescriptor
							);
							assert data.concreteDescriptor != null;
						}
						if ( hasKeyManyToOne ) {
							if ( !data.shallowCached && !entityKeyOnly ) {
								resolveKeySubInitializers( data );
							}
							return;
						}
					}
				}
				id = identifierAssembler.assemble( rowProcessingState );
				if ( id == null ) {
					setMissing( data );
					return;
				}
			}

			if ( oldEntityKey != null
					&& previousRowReuse
					&& oldEntityInstance != null
					&& areKeysEqual( oldEntityKey.getIdentifier(), id )
					&& !oldEntityHolder.isDetached() ) {
				data.setState( State.INITIALIZED );
				data.entityKey = oldEntityKey;
				data.setInstance( oldEntityInstance );
				data.entityInstanceForNotify = oldEntityInstanceForNotify;
				data.concreteDescriptor = oldEntityKey.getPersister();
				data.entityHolder = oldEntityHolder;
				if ( !entityKeyOnly ) {
					notifySubInitializersToReusePreviousRowInstance( data );
				}
				return;
			}
			resolveEntityKey( data, id );
			if ( !entityKeyOnly ) {
				// Resolve the entity instance early as we have no key many-to-one
				resolveInstance( data );
				if ( !data.shallowCached ) {
					if ( data.getState() == State.INITIALIZED ) {
						if ( data.entityHolder.getEntityInitializer() == null ) {
							// The entity is already part of the persistence context,
							// so let's figure out the loaded state and only run sub-initializers if necessary
							resolveInstanceSubInitializers( data );
						}
						// If the entity is initialized and getEntityInitializer() == this,
						// we already processed a row for this entity before,
						// but we still have to call resolveKeySubInitializers to activate sub-initializers,
						// because a row might contain data that sub-initializers want to consume
						else {
							// todo: try to diff the eagerness of the sub-initializers to avoid further processing
							resolveKeySubInitializers( data );
						}
					}
					else {
						resolveKeySubInitializers( data );
					}
				}
			}
		}
	}

	private boolean areKeysEqual(Object key1, Object key2) {
		return keyTypeForEqualsHashCode == null
				? key1.equals( key2 )
				: keyTypeForEqualsHashCode.isEqual( key1, key2 );
	}

	protected void resolveInstanceSubInitializers(EntityInitializerData data) {
		final int subclassId = data.concreteDescriptor.getSubclassId();
		final var entityEntry = data.entityHolder.getEntityEntry();
		assert entityEntry != null : "This method should only be called if the entity is already initialized";

		final Initializer<?>[] initializers;
		final ImmutableBitSet maybeLazySet;
		if ( data.entityHolder.getEntityInitializer() == this ) {
			// When this entity is already initialized, but this initializer runs anyway,
			// we only need to process collection containing initializers
			initializers = collectionContainingSubInitializers[subclassId];
			maybeLazySet = null;
		}
		else {
			// If an entity has unfetched attributes, we should probably also invoke non-eager initializers.
			// Non-eager initializers only set proxies, but since that contains the FK information,
			// it would be wasteful not to set that information on the bytecode enhanced entity
			final var subInitializersToUse =
					rootEntityDescriptor.getBytecodeEnhancementMetadata()
							.hasUnFetchedAttributes( data.entityInstanceForNotify )
					? subInitializers
					: eagerSubInitializers;
			initializers = subInitializersToUse[subclassId];
			maybeLazySet = entityEntry.getMaybeLazySet();
			// Skip resolving if this initializer has no sub-initializers
			// or the lazy set of this initializer is a superset/contains the entity entry maybeLazySet
			if ( initializers.length == 0 && !hasLazyInitializingSubAssemblers
				|| maybeLazySet != null && lazySets[subclassId].contains( maybeLazySet ) ) {
				return;
			}
		}
		final var rowProcessingState = data.getRowProcessingState();
		final var persistenceContext = rowProcessingState.getSession().getPersistenceContextInternal();
		assert entityEntry == persistenceContext.getEntry( data.entityInstanceForNotify );
		final var loadedState = entityEntry.getLoadedState();
		final Object[] state;
		if ( loadedState == null ) {
			if ( entityEntry.getStatus() == Status.READ_ONLY ) {
				state = data.concreteDescriptor.getValues( data.entityInstanceForNotify );
			}
			else {
				// This branch is entered when a load happens while a cache entry is assembling.
				// The EntityEntry has the LOADING state, but the loaded state is still empty.
				assert entityEntry.getStatus() == Status.LOADING;
				// Just skip any initialization in this case as the cache entry assembling will take care of it
				return;
			}
		}
		else {
			state = loadedState;
		}
		boolean needsLoadedValuesUpdate = false;
		if ( initializers.length == 0 ) {
			if ( hasLazyInitializingSubAssemblers ) {
				var subAssemblers = assemblers[subclassId];
				for ( int i = 0; i < state.length; i++ ) {
					final Object subInstance = state[i];
					final var assembler = subAssemblers[i];
					if ( subInstance == UNFETCHED_PROPERTY
						&& !(assembler instanceof UnfetchedResultAssembler<?>)
						&& !(assembler instanceof UnfetchedCollectionAssembler) ) {
						// This assembler will produce a value when the underlying entity property is still lazy
						needsLoadedValuesUpdate = true;
						break;
					}
				}
			}
		}
		else {
			var eagerInitializers = eagerSubInitializers[subclassId];
			for ( int i = 0; i < state.length; i++ ) {
				final var initializer = initializers[i];
				if ( maybeLazySet == null || maybeLazySet.get( i ) ) {
					final Object subInstance = state[i];
					if ( initializer != null ) {
						if ( subInstance == UNFETCHED_PROPERTY ) {
							// Go through the normal initializer process
							initializer.resolveKey( rowProcessingState );
							// Assume that the initializer will produce a proxy or the real value
							needsLoadedValuesUpdate = true;
						}
						// Avoid resolving initializers that are not lazy when the property isn't unfetched
						else if ( eagerInitializers.length != 0 && eagerInitializers[i] != null ) {
							initializer.resolveInstance( subInstance, rowProcessingState );
						}
					}
					else if ( !needsLoadedValuesUpdate && hasLazyInitializingSubAssemblers && subInstance == UNFETCHED_PROPERTY ) {
						final var assembler = assemblers[subclassId][i];
						if ( !( assembler instanceof UnfetchedResultAssembler<?> )
							&& !( assembler instanceof UnfetchedCollectionAssembler ) ) {
							// This assembler will produce a value when the underlying entity property is still lazy
							needsLoadedValuesUpdate = true;
						}
					}
				}
			}
		}
		if ( needsLoadedValuesUpdate ) {
			// Mark as resolved to update the state of the entity during initialization phase
			data.setState( State.RESOLVED );
		}
	}

	private void notifySubInitializersToReusePreviousRowInstance(EntityInitializerData data) {
		final var entityEntry = data.entityHolder.getEntityEntry();
		final Initializer<?>[] subInitializer;
		final ImmutableBitSet maybeLazySet;
		if ( data.entityHolder.getEntityInitializer() == this ) {
			// When a previous row initialized this entity already, we only need to process collections
			subInitializer = collectionContainingSubInitializers[data.concreteDescriptor.getSubclassId()];
			maybeLazySet = null;
		}
		else {
			// The entity is already initialized, and we saw it in the previous row,
			// so the only sensible thing to do is to notify all eager sub-initializers about this.
			// The eager sub-initializers can then potentially initialize already set proxies or
			// continue resolving data for collections that ought to be loaded through this initializer
			subInitializer = eagerSubInitializers[data.concreteDescriptor.getSubclassId()];
			maybeLazySet = entityEntry == null ? null : entityEntry.getMaybeLazySet();
		}
		final var rowProcessingState = data.getRowProcessingState();
		for ( int i = 0; i < subInitializer.length; i++ ) {
			final var initializer = subInitializer[i];
			// It is vital to only resolveFromPreviousRow only for the initializers where the state is maybe lazy,
			// as the initialization process for the previous row also only called those initializers
			if ( initializer != null
					&& ( maybeLazySet == null || maybeLazySet.get( i ) ) ) {
				initializer.resolveFromPreviousRow( rowProcessingState );
			}
		}
	}

	protected void resolveKeySubInitializers(EntityInitializerData data) {
		final var rowProcessingState = data.getRowProcessingState();
		for ( var initializer : subInitializers[data.concreteDescriptor.getSubclassId()] ) {
			if ( initializer != null ) {
				initializer.resolveKey( rowProcessingState );
			}
		}
	}

	@EnsuresNonNull( "data.entityKey" )
	protected void resolveEntityKey(EntityInitializerData data, Object id) {
		var concreteDescriptor = data.concreteDescriptor;
		if ( concreteDescriptor == null ) {
			concreteDescriptor = data.concreteDescriptor =
					determineConcreteEntityDescriptor( data.getRowProcessingState(),
							discriminatorAssembler, entityDescriptor );
			assert concreteDescriptor != null;
		}
		data.entityKey = new EntityKey( id, concreteDescriptor );
	}

	protected void setMissing(EntityInitializerData data) {
		data.entityKey = null;
		data.concreteDescriptor = null;
		data.setInstance( null );
		data.entityInstanceForNotify = null;
		data.entityHolder = null;
		data.setState( State.MISSING );

		// super processes the foreign-key target column.  here we
		// need to also look at the foreign-key value column to check
		// for a dangling foreign-key

		if ( keyAssembler != null ) {
			final Object foreignKeyValue = keyAssembler.assemble( data.getRowProcessingState() );
			if ( foreignKeyValue != null ) {
				if ( notFoundAction != NotFoundAction.IGNORE ) {
					final String entityName = getEntityDescriptor().getEntityName();
					if ( affectedByFilter ) {
						throw new EntityFilterException( entityName, foreignKeyValue,
								referencedModelPart.getNavigableRole().getFullPath() );
					}
					throw new FetchNotFoundException( entityName, foreignKeyValue );
				}
			}
		}
	}

	@Override
	public void resolveFromPreviousRow(EntityInitializerData data) {
		if ( data.getState() == State.UNINITIALIZED ) {
			final var entityKey = data.entityKey;
			if ( entityKey == null ) {
				setMissing( data );
			}
			else {
				data.setState( State.INITIALIZED );
				notifySubInitializersToReusePreviousRowInstance( data );
			}
		}
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance, EntityInitializerData data) {
		final var attributeMapping = getInitializedPart().asAttributeMapping();
		final Object instance =
				attributeMapping != null
						? attributeMapping.getValue( parentInstance )
						: parentInstance;
		final var session = data.getRowProcessingState().getSession();
		if ( instance == null ) {
			setMissing( data );
		}
		else {
			data.setInstance( instance );
			final Object entityInstanceForNotify = data.entityInstanceForNotify = Hibernate.unproxy( instance );
			data.concreteDescriptor = session.getEntityPersister( null, entityInstanceForNotify );
			resolveEntityKey( data,
					data.concreteDescriptor.getIdentifier( entityInstanceForNotify, session ) );
			data.entityHolder = session.getPersistenceContextInternal().getEntityHolder( data.entityKey );
			data.setState( State.INITIALIZED );
			initializeSubInstancesFromParent( data );
		}
	}

	@Override
	public boolean isResultInitializer() {
		return isResultInitializer;
	}

	private void deepCopy(EntityPersister containerDescriptor, Object[] source, Object[] target) {
		final var updatableAttributeMutabilityPlan =
				updatableAttributeMutabilityPlans[containerDescriptor.getSubclassId()];
		for ( int i = 0; i < updatableAttributeMutabilityPlan.length; i++ ) {
			final Object sourceValue = source[i];
			if ( updatableAttributeMutabilityPlan[i] != null
					&& sourceValue != LazyPropertyInitializer.UNFETCHED_PROPERTY
					&& sourceValue != PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
				target[i] = updatableAttributeMutabilityPlan[i].deepCopy( source[i] );
			}
		}
	}

	@Override
	public ModelPart getInitializedPart() {
		return referencedModelPart;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	protected DomainResultAssembler<?> getIdentifierAssembler() {
		return identifierAssembler;
	}

	@Override
	public EntityPersister getEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public Object getTargetInstance(EntityInitializerData data) {
		return data.entityInstanceForNotify;
	}

	@Override
	public @Nullable InitializerParent<?> getParent() {
		return parent;
	}

	private final ConcurrentHashMap<String, Type[]> parentEntityAttributeTypes = new ConcurrentHashMap<>();

	protected Type[] getParentEntityAttributeTypes(String attributeName) {
		var types = parentEntityAttributeTypes.get( attributeName );
		if ( types == null ) {
			types = new Type[entityDescriptor.getRootEntityDescriptor().getSubclassEntityNames().size()];
			initializeAttributeType( types, entityDescriptor, attributeName );
			for ( var subMappingType : entityDescriptor.getSubMappingTypes() ) {
				initializeAttributeType( types, subMappingType.getEntityPersister(), attributeName );
			}
			parentEntityAttributeTypes.putIfAbsent( attributeName, types );
		}
		return types;
	}

	protected void initializeAttributeType(Type[] attributeTypes, EntityPersister entityDescriptor, String attributeName) {
		if ( entityDescriptor.findByPath( attributeName ) != null ) {
			attributeTypes[entityDescriptor.getSubclassId()] = entityDescriptor.getPropertyType( attributeName );
		}
	}

	public static @Nullable EntityPersister determineConcreteEntityDescriptor(
			RowProcessingState rowProcessingState,
			@Nullable BasicResultAssembler<?> discriminatorAssembler,
			EntityPersister entityDescriptor)
			throws WrongClassException {
		if ( hasConcreteDescriptor( rowProcessingState, discriminatorAssembler, entityDescriptor ) ) {
			return entityDescriptor;
		}
		else {
			assert entityDescriptor.hasSubclasses()
					: "Reading a discriminator from a result set should only happen if the entity has subclasses";
			final var discriminatorMapping = entityDescriptor.getDiscriminatorMapping();
			assert discriminatorMapping != null;
			final Object discriminator = discriminatorAssembler.extractRawValue( rowProcessingState );
			final var discriminatorDetails = discriminatorMapping.resolveDiscriminatorValue( discriminator );
			if ( discriminatorDetails == null ) {
				assert discriminator == null : "Discriminator details should only be null for null values";
				return null;
			}
			else {
				final var indicatedEntity = discriminatorDetails.getIndicatedEntity();
				if ( indicatedEntity.isTypeOrSuperType( entityDescriptor ) ) {
					return indicatedEntity.getEntityPersister();
				}
				else {
					throw new WrongClassException(
							indicatedEntity.getEntityName(),
							null,
							entityDescriptor.getEntityName(),
							discriminator
					);
				}
			}
		}
	}

	protected boolean useEmbeddedIdentifierInstanceAsEntity(EntityInitializerData data) {
		if ( data.canUseEmbeddedIdentifierInstanceAsEntity ) {
			data.concreteDescriptor =
					determineConcreteEntityDescriptor( data.getRowProcessingState(),
							discriminatorAssembler, entityDescriptor );
			return data.concreteDescriptor != null
				&& data.concreteDescriptor.isInstance( data.getRowProcessingState().getEntityId() );
		}
		else {
			return false;
		}
	}

	@Override
	public void resolveInstance(Object instance, EntityInitializerData data) {
		if ( instance == null ) {
			setMissing( data );
		}
		else {
			final var lazyInitializer = extractLazyInitializer( instance );
			final var rowProcessingState = data.getRowProcessingState();
			final var session = rowProcessingState.getSession();
			final var persistenceContext = session.getPersistenceContextInternal();
			if ( lazyInitializer == null ) {
				// Entity is most probably initialized
				data.concreteDescriptor = session.getEntityPersister( null, instance );
				resolveEntityKey( data, data.concreteDescriptor.getIdentifier( instance, session ) );
				data.entityHolder = persistenceContext.claimEntityHolderIfPossible(
						data.entityKey,
						null,
						rowProcessingState.getJdbcValuesSourceProcessingState(),
						this
				);
				if ( data.entityHolder.getManagedObject() == null ) {
					final var entry = persistenceContext.getEntry( instance ); // make sure an EntityEntry exists
					if ( entry == null ) {
						// We cannot reuse an entity instance that has no entry in the PC,
						// this can happen if the parent entity contained a detached instance.
						// We need to create a new instance in this case (see resolveEntityInstance1)
						instance = resolveEntityInstance( data );
					}
					else {
						// Entity was most probably removed in the same session without setting this association to null.
						// Since this load request can happen through `find()` which doesn't auto-flush on association joins,
						// the entity must be fully initialized, even if it is removed already
						data.entityHolder = persistenceContext.claimEntityHolderIfPossible(
								data.entityKey,
								instance,
								rowProcessingState.getJdbcValuesSourceProcessingState(),
								this
						);
					}
				}
				else {
					final Object entity = data.entityHolder.getEntity();
					final var entityKey = data.entityHolder.getEntityKey();
					if ( entity == null ) {
						final Object proxy = data.entityHolder.getProxy();
						assert proxy != instance;
						instance = resolveEntityInstance( data );
						data.entityKey = entityKey;
						if ( proxy != null ) {
							castNonNull( extractLazyInitializer( proxy ) )
									.setImplementation( instance );
						}
					}
					else if ( entity != instance ) {
						// The instance contained in the parent entity is different from the managed persistent instance
						// currently in the persistence context. We should always initialize the managed one in this case.
						instance = entity;
						data.entityKey = entityKey;
					}
				}

				data.entityInstanceForNotify = instance;

				if ( data.concreteDescriptor.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading()
						&& isPersistentAttributeInterceptable( data.entityInstanceForNotify )
						&& getAttributeInterceptor( data.entityInstanceForNotify )
								instanceof EnhancementAsProxyLazinessInterceptor enhancementInterceptor
						&& !enhancementInterceptor.isInitialized() ) {
					data.setState( State.RESOLVED );
				}
				else {
					// If the entity initializer is null, we know the entity is fully initialized;
					// otherwise it will be initialized by some other initializer
					data.setState( data.entityHolder.getEntityInitializer() == null ? State.INITIALIZED : State.RESOLVED );
				}

				if ( data.getState() == State.RESOLVED ) {
					data.entityHolder = persistenceContext.claimEntityHolderIfPossible(
							data.entityKey,
							data.entityInstanceForNotify,
							rowProcessingState.getJdbcValuesSourceProcessingState(),
							this
					);
				}
			}
			else if ( lazyInitializer.isUninitialized() ) {
				data.setState( State.RESOLVED );
				// Read the discriminator from the result set if necessary
				data.concreteDescriptor =
						discriminatorAssembler == null
								? entityDescriptor
								: determineConcreteEntityDescriptor( rowProcessingState,
										discriminatorAssembler, entityDescriptor );
				assert data.concreteDescriptor != null;
				resolveEntityKey( data, lazyInitializer.getInternalIdentifier() );
				data.entityHolder = persistenceContext.claimEntityHolderIfPossible(
						data.entityKey,
						null,
						rowProcessingState.getJdbcValuesSourceProcessingState(),
						this
				);
				// Resolve and potentially create the entity instance
				final Object proxy = data.entityHolder.getProxy();
				if ( proxy == instance ) {
					data.entityInstanceForNotify = resolveEntityInstance( data );
					lazyInitializer.setImplementation( data.entityInstanceForNotify );
				}
				else {
					resolveEntity( data, proxy );
				}
			}
			else {
				final var implementation = lazyInitializer.getImplementation();
				data.concreteDescriptor = session.getEntityPersister( null, implementation );
				resolveEntityKey( data, lazyInitializer.getInternalIdentifier() );
				data.entityHolder = persistenceContext.getEntityHolder( data.entityKey );
				final Object proxy = data.entityHolder.getProxy();
				if ( proxy == instance ) {
					data.entityInstanceForNotify = implementation;
					// Even though the lazyInitializer reports it is initialized,
					// check if the entity holder reports initialized, because in
					// a nested initialization scenario, this nested initializer
					// must initialize the entity
					data.setState( data.entityHolder.isInitialized() ? State.INITIALIZED : State.RESOLVED );
				}
				else {
					resolveEntity( data, proxy );
					data.entityKey = data.entityHolder.getEntityKey();
				}
			}

			data.setInstance( data.entityHolder.getManagedObject() );

			if ( identifierAssembler != null ) {
				final var initializer = identifierAssembler.getInitializer();
				if ( initializer != null ) {
					initializer.resolveInstance( data.entityKey.getIdentifier(), rowProcessingState );
				}
			}
			upgradeLockMode( data );
			if ( data.getState() == State.INITIALIZED ) {
				registerReloadedEntity( data );
				resolveInstanceSubInitializers( data );
				if ( data.getState() == State.INITIALIZED
						&& rowProcessingState.needsResolveState() ) {
					// We need to read result set values to correctly populate the query cache
					resolveEntityState( data );
				}
			}
			else {
				resolveKeySubInitializers( data );
			}
		}
	}

	private void resolveEntity(EntityInitializerData data, Object proxy) {
		final Object entity = data.entityHolder.getEntity();
		if ( entity == null ) {
			data.entityInstanceForNotify = resolveEntityInstance( data );
			if ( proxy != null ) {
				castNonNull( extractLazyInitializer( proxy ) )
						.setImplementation( data.entityInstanceForNotify );
			}
			data.setState( State.RESOLVED );
		}
		else {
			data.entityInstanceForNotify = entity;
			data.setState( data.entityHolder.isInitialized() ? State.INITIALIZED : State.RESOLVED );
		}
	}

	@Override
	public void resolveInstance(EntityInitializerData data) {
		if ( data.getState() == State.KEY_RESOLVED ) {
			final var rowProcessingState = data.getRowProcessingState();
			data.setState( State.RESOLVED );
			if ( data.entityKey == null ) {
				assert identifierAssembler != null;
				final Object id = identifierAssembler.assemble( rowProcessingState );
				if ( id == null ) {
					setMissing( data );
					return;
				}
				resolveEntityKey( data, id );
			}
			data.entityHolder =
					rowProcessingState.getSession().getPersistenceContextInternal()
							.claimEntityHolderIfPossible(
									data.entityKey,
									null,
									rowProcessingState.getJdbcValuesSourceProcessingState(),
									this
							);

			if ( useEmbeddedIdentifierInstanceAsEntity( data ) ) {
				data.setInstance( data.entityInstanceForNotify = rowProcessingState.getEntityId() );
			}
			else {
				resolveEntityInstance1( data );
				if ( data.uniqueKeyAttributePath != null ) {
					final var session = rowProcessingState.getSession();
					final var concreteDescriptor = getConcreteDescriptor( data );
					final var entityUniqueKey = new EntityUniqueKey(
							concreteDescriptor.getEntityName(),
							data.uniqueKeyAttributePath,
							rowProcessingState.getEntityUniqueKey(),
							data.uniqueKeyPropertyTypes[concreteDescriptor.getSubclassId()],
							session.getFactory()
					);
					session.getPersistenceContextInternal()
							.addEntity( entityUniqueKey, data.getInstance() );
				}
			}

			if ( data.getInstance() != null ) {
				upgradeLockMode( data );
				if ( data.getState() == State.INITIALIZED ) {
					registerReloadedEntity( data );
					if ( rowProcessingState.needsResolveState() ) {
						// We need to read result set values to correctly populate the query cache
						resolveEntityState( data );
					}
				}
				if ( data.shallowCached ) {
					initializeSubInstancesFromParent( data );
				}
			}
		}
	}

	protected void resolveEntityInstance1(EntityInitializerData data) {
		final var entityHolder = data.entityHolder;
		final Object proxy = entityHolder.getProxy();
		if ( !unwrapProxy( data, proxy ) && isProxyInstance( proxy ) ) {
			final Object entityFromExecutionContext = getEntityFromExecutionContext( data );
			if ( entityFromExecutionContext != null ) {
				data.entityInstanceForNotify = entityFromExecutionContext;
				data.setInstance( entityFromExecutionContext );
				// If the entity comes from the execution context,
				// it is treated as not initialized so that we can
				// refresh the data as requested
				registerReloadedEntity( data );
			}
			else {
				data.setInstance( proxy );
				if ( Hibernate.isInitialized( proxy ) ) {
					if ( data.entityHolder.isInitialized() ) {
						data.setState( State.INITIALIZED );
					}
					data.entityInstanceForNotify = Hibernate.unproxy( proxy );
				}
				else {
					final var lazyInitializer = extractLazyInitializer( proxy );
					assert lazyInitializer != null;
					data.entityInstanceForNotify = resolveEntityInstance2( data );
					lazyInitializer.setImplementation( data.entityInstanceForNotify );
				}
			}
		}
		else {
			final Object existingEntity = entityHolder.getEntity();
			final var initializer = entityHolder.getEntityInitializer();
			if ( existingEntity != null ) {
				data.setInstance( data.entityInstanceForNotify = existingEntity );
				if ( initializer == null ) {
					assert entityHolder.isInitialized() == isExistingEntityInitialized( existingEntity );
					if ( entityHolder.isInitialized() ) {
						data.setState( State.INITIALIZED );
					}
					else if ( isResultInitializer() ) {
						registerLoadingEntity( data, existingEntity );
					}
				}
				else if ( initializer != this ) {
					// The other initializer will take care of initialization
					if ( !hasLazyInitializingSubAssemblers ) {
						// but we can only skip the initialization phase of this initializer
						// if this initializer does not initialize lazy basic attributes
						data.setState( State.INITIALIZED );
					}
				}
				else if ( data.shallowCached ) {
					// For shallow cached entities, only the id is available, so ensure we load the data immediately
					final Object resolvedEntityInstance = resolveEntityInstance( data );
					data.entityInstanceForNotify = resolvedEntityInstance;
					data.setInstance( resolvedEntityInstance );
				}
			}
			else {
				final Object entityFromExecutionContext = getEntityFromExecutionContext( data );
				if ( entityFromExecutionContext != null ) {
					// This is the entity to refresh, so don't set the state to initialized
					data.entityInstanceForNotify = entityFromExecutionContext;
					data.setInstance( entityFromExecutionContext );
					if ( isResultInitializer() ) {
						registerLoadingEntity( data, entityFromExecutionContext );
					}
				}
				else {
					assert initializer == this;
					// look to see if another initializer from a parent load context or an earlier
					// initializer is already loading the entity
					final Object resolvedEntityInstance = resolveEntityInstance2( data );
					data.entityInstanceForNotify = resolvedEntityInstance;
					data.setInstance( resolvedEntityInstance );
					if ( data.getState() != State.INITIALIZED && identifierAssembler != null ) {
						final var idInitializer = identifierAssembler.getInitializer();
						if ( idInitializer != null ) {
							// If this is the owning initializer and the returned object is not initialized,
							// this means that the entity instance was just instantiated.
							// In this case, we want to call "assemble" and hence "initializeInstance" on the initializer
							// for possibly non-aggregated identifier mappings, so inject the virtual id representation
							idInitializer.initializeInstance( data.getRowProcessingState() );
						}
					}
				}
			}
		}
		// todo: ensure we initialize the entity
		assert !data.shallowCached || data.getState() == State.INITIALIZED : "Forgot to initialize the entity";
	}

	private boolean unwrapProxy(EntityInitializerData data, Object proxy) {
		return proxy != null
			&& referencedModelPart instanceof ToOneAttributeMapping toOneAttributeMapping
			&& toOneAttributeMapping.isUnwrapProxy()
			&& getConcreteDescriptor( data ).getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();
	}

	protected Object getEntityFromExecutionContext(EntityInitializerData data) {
		final var executionContext =
				data.getRowProcessingState()
						.getJdbcValuesSourceProcessingState()
						.getExecutionContext();
		return rootEntityDescriptor == executionContext.getRootEntityDescriptor()
			&& areKeysEqual( data.entityKey.getIdentifier(), executionContext.getEntityId() )
				? executionContext.getEntityInstance()
				: null;
	}

	protected void upgradeLockMode(EntityInitializerData data) {
		if ( data.lockMode != LockMode.NONE ) {
			final var rowProcessingState = data.getRowProcessingState();
			if ( rowProcessingState.upgradeLocks() ) {
				final var entityEntry = data.entityHolder.getEntityEntry();
				assert entityEntry ==
					rowProcessingState.getSession().getPersistenceContextInternal()
							.getEntry( data.entityInstanceForNotify );
				if ( entityEntry != null ) {
					final var entryLockMode = entityEntry.getLockMode();
					if ( entryLockMode.lessThan( data.lockMode ) ) {
						//we only check the version when _upgrading_ lock modes
						if ( versionAssembler != null && entryLockMode != LockMode.NONE ) {
							checkVersion( data, entityEntry, rowProcessingState );
						}
						//we need to upgrade the lock mode to the mode requested
						entityEntry.setLockMode( data.lockMode );
					}
				}
			}
		}
	}

	// Used by Hibernate Reactive
	protected boolean isProxyInstance(Object proxy) {
		return proxy != null
			&& ( proxy instanceof MapProxy
				|| entityDescriptor.getJavaType().getJavaTypeClass().isInstance( proxy ) );
	}

	private boolean isExistingEntityInitialized(Object existingEntity) {
		return Hibernate.isInitialized( existingEntity );
	}

	/**
	 * Check the version of the object in the {@code RowProcessingState} against
	 * the object version in the session cache, throwing an exception
	 * if the version numbers are different
	 */
	private void checkVersion(
			EntityInitializerData data,
			EntityEntry entry,
			RowProcessingState rowProcessingState) {
		final Object version = entry.getVersion();
		if ( version != null ) {
			// null version means the object is in the process of being loaded somewhere else in the ResultSet
			final Object currentVersion = versionAssembler.assemble( rowProcessingState );
			if ( !data.concreteDescriptor.getVersionType().isEqual( version, currentVersion ) ) {
				final String entityName = data.concreteDescriptor.getEntityName();
				final var statistics = rowProcessingState.getSession().getFactory().getStatistics();
				if ( statistics.isStatisticsEnabled() ) {
					statistics.optimisticFailure( entityName );
				}
				throw new StaleObjectStateException( entityName, entry.getId(),
						"Query result contains conflicting version of entity already held in persistence context" );
			}
		}

	}

	/**
	 * Used by Hibernate Reactive
	 */
	protected Object resolveEntityInstance2(EntityInitializerData data) {
		if ( data.entityHolder.getEntityInitializer() == this ) {
			assert data.entityHolder.getEntity() == null;
			return resolveEntityInstance( data );
		}
		else {
			// the entity is already being loaded elsewhere
			return data.entityHolder.getEntity();
		}
	}

	protected Object resolveEntityInstance(EntityInitializerData data) {
		final Object resolved = resolveToOptionalInstance( data );
		if ( resolved != null ) {
			registerLoadingEntity( data, resolved );
			return resolved;
		}
		else {
			if ( data.shallowCached ) {
				// We must load the entity this way, because the query cache entry contains only the primary key
				data.setState( State.INITIALIZED );
				final var rowProcessingState = data.getRowProcessingState();
				final var session = rowProcessingState.getSession();
				assert data.entityHolder.getEntityInitializer() == this;
				// If this initializer owns the entity, we have to remove the entity holder,
				// because the subsequent loading process will claim the entity
				session.getPersistenceContextInternal().removeEntityHolder( data.entityKey );
				return session.internalLoad(
						data.concreteDescriptor.getEntityName(),
						data.entityKey.getIdentifier(),
						true,
						false
				);
			}
			// We have to query the second level cache if reference cache entries are used
			else if ( entityDescriptor.canUseReferenceCacheEntries() ) {
				final Object cached = resolveInstanceFromCache( data );
				if ( cached != null ) {
					// EARLY EXIT!!!
					// because the second level cache has reference cache entries, the entity is initialized
					data.setState( State.INITIALIZED );
					return cached;
				}
			}
			final Object instance = instantiateEntity( data );
			registerLoadingEntity( data, instance );
			return instance;
		}
	}

	protected Object instantiateEntity(EntityInitializerData data) {
		return data.getRowProcessingState().getSession()
				.instantiate( data.concreteDescriptor, data.entityKey.getIdentifier() );
	}

	// Used by Hibernate Reactive
	protected Object resolveToOptionalInstance(EntityInitializerData data) {
		if ( isResultInitializer() ) {
			// this isEntityReturn bit is just for entity loaders, not HQL/criteria
			final var processingOptions =
					data.getRowProcessingState()
							.getJdbcValuesSourceProcessingState()
							.getProcessingOptions();
			return matchesOptionalInstance( data, processingOptions )
					? processingOptions.getEffectiveOptionalObject()
					: null;
		}
		else {
			return null;
		}
	}

	// Used by Hibernate Reactive
	protected boolean matchesOptionalInstance(
			EntityInitializerData data,
			JdbcValuesSourceProcessingOptions processingOptions) {
		final Object optionalEntityInstance = processingOptions.getEffectiveOptionalObject();
		final Object requestedEntityId = processingOptions.getEffectiveOptionalId();
		return requestedEntityId != null
			&& optionalEntityInstance != null
			&& areKeysEqual( requestedEntityId, data.entityKey.getIdentifier() );
	}

	// Used by Hibernate Reactive
	protected Object resolveInstanceFromCache(EntityInitializerData data) {
		return loadFromSecondLevelCache(
				data.getRowProcessingState().getSession().asEventSource(),
				null,
				data.lockMode,
				entityDescriptor,
				data.entityKey
		);
	}

	protected void registerLoadingEntity(EntityInitializerData data, Object instance) {
		final var rowProcessingState = data.getRowProcessingState();
		final var valuesSourceProcessingState = rowProcessingState.getJdbcValuesSourceProcessingState();
		rowProcessingState.getSession().getPersistenceContextInternal()
				.claimEntityHolderIfPossible( data.entityKey, instance, valuesSourceProcessingState, this );
	}

	protected void registerReloadedEntity(EntityInitializerData data) {
		if ( data.hasCallbackActions ) {
			// This is only needed for follow-on locking, so skip registering the entity if there is no callback
			data.entityHolder.markAsReloaded( data.getRowProcessingState().getJdbcValuesSourceProcessingState() );
		}
	}

	@Override
	public void initializeInstance(EntityInitializerData data) {
		if ( data.getState() == State.RESOLVED ) {
			if ( !skipInitialization( data ) ) {
				assert consistentInstance( data );
				initializeEntityInstance( data );
			}
			else {
				if ( data.getRowProcessingState().needsResolveState() ) {
					// A sub-initializer might have taken responsibility for this entity,
					// but we still need to resolve the state to correctly populate a query cache
					resolveState( data );
				}
				if ( rootEntityDescriptor.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading()
					&& data.entityHolder.getEntityInitializer() != this
					&& data.entityHolder.isInitialized() ) {
					updateInitializedEntityInstance( data );
				}
			}
			data.setState( State.INITIALIZED );
		}
	}

	protected void updateInitializedEntityInstance(EntityInitializerData data) {
		assert rootEntityDescriptor.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();

		final var rowProcessingState = data.getRowProcessingState();
		final var entityEntry = data.entityHolder.getEntityEntry();
		final var loadedState = entityEntry.getLoadedState();
		final var concreteAssemblers = assemblers[data.concreteDescriptor.getSubclassId()];

		for ( int i = 0; i < loadedState.length; i++ ) {
			final var subInstance = loadedState[i];
			final var assembler = concreteAssemblers[i];
			if ( subInstance == UNFETCHED_PROPERTY
				&& assembler != null
				&& !(assembler instanceof UnfetchedResultAssembler<?>)
				&& !(assembler instanceof UnfetchedCollectionAssembler) ) {
				final var value = assembler.assemble( rowProcessingState );
				if ( value != UNFETCHED_PROPERTY ) {
					loadedState[i] = value;
					data.concreteDescriptor.setValue( data.entityInstanceForNotify, i, value );
				}
			}
		}

		final var session = rowProcessingState.getSession();
		updateCaches(
				data,
				session,
				session.getPersistenceContextInternal(),
				loadedState,
				entityEntry.getVersion()
		);
	}

	protected boolean consistentInstance(EntityInitializerData data) {
		// Only call PersistenceContext#getEntity within the assert expression, as it is costly
		final Object entity =
				data.getRowProcessingState().getSession()
						.getPersistenceContextInternal()
						.getEntity( data.entityKey );
		return entity == null
			|| entity == data.entityInstanceForNotify;
	}

	protected void initializeEntityInstance(EntityInitializerData data) {
		final var rowProcessingState = data.getRowProcessingState();
		final var session = rowProcessingState.getSession();
		final var persistenceContext = session.getPersistenceContextInternal();
		final var entityKey = data.entityKey;
		assert entityKey != null;

		final var entityIdentifier = entityKey.getIdentifier();
		final var resolvedEntityState = extractConcreteTypeStateValues( data );

		rowProcessingState.getJdbcValuesSourceProcessingState()
				.registerLoadingEntityHolder( data.entityHolder );
		preLoad( data, resolvedEntityState );

		final Object entityInstanceForNotify = data.entityInstanceForNotify;
		if ( isPersistentAttributeInterceptable( entityInstanceForNotify ) ) {
			final var persistentAttributeInterceptor =
					asPersistentAttributeInterceptable( entityInstanceForNotify )
							.$$_hibernate_getInterceptor();
			if ( persistentAttributeInterceptor == null
					|| persistentAttributeInterceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
				// if we do this after the entity has been initialized the
				// BytecodeLazyAttributeInterceptor#isAttributeLoaded(String fieldName) would return false;
				data.concreteDescriptor.getBytecodeEnhancementMetadata()
						.injectInterceptor( entityInstanceForNotify, entityIdentifier, session );
			}
		}
		data.concreteDescriptor.setValues( entityInstanceForNotify, resolvedEntityState );

		persistenceContext.addEntity( entityKey, entityInstanceForNotify );

		// Also register possible unique key entries
		registerPossibleUniqueKeyEntries( data, resolvedEntityState, session );

		final Object version = versionAssembler == null ? null : versionAssembler.assemble( rowProcessingState );
		final Object rowId = rowIdAssembler == null ? null : rowIdAssembler.assemble( rowProcessingState );

		final var entityEntry =
				persistenceContext.addEntry(
						entityInstanceForNotify,
						Status.LOADING,
						resolvedEntityState,
						rowId,
						entityIdentifier,
						version,
						lockModeToAcquire( data ),
						true,
						data.concreteDescriptor,
						false
				);

		entityEntry.setMaybeLazySet( maybeLazySets[data.concreteDescriptor.getSubclassId()] );
		data.entityHolder.setEntityEntry( entityEntry );

		registerNaturalIdResolution( data, persistenceContext, resolvedEntityState );

		takeSnapshot( data, session, persistenceContext, entityEntry, resolvedEntityState );

		data.concreteDescriptor.afterInitialize( entityInstanceForNotify, session );
		entityEntry.postLoad( entityInstanceForNotify );

		assert data.concreteDescriptor.getIdentifier( entityInstanceForNotify, session ) != null;

		if ( !rowProcessingState.isQueryCacheHit() ) {
			final var statistics = session.getFactory().getStatistics();
			if ( statistics.isStatisticsEnabled() ) {
				statistics.loadEntity( data.concreteDescriptor.getEntityName() );
			}
		}
		updateCaches(
				data,
				session,
				session.getPersistenceContextInternal(),
				resolvedEntityState,
				version
		);
	}

	private static LockMode lockModeToAcquire(EntityInitializerData data) {
		if ( data.getRowProcessingState().isTransactionActive() ) {
			// from the perspective of Hibernate, an entity is read locked as soon as it is read
			// so regardless of the requested lock mode, we upgrade to at least the read level
			return data.lockMode == LockMode.NONE ? LockMode.READ : data.lockMode;
		}
		else {
			// data read outside transaction is marked as unlocked
			return LockMode.NONE;
		}
	}

	protected void updateCaches(
			EntityInitializerData data,
			SharedSessionContractImplementor session,
			PersistenceContext persistenceContext,
			Object[] resolvedEntityState,
			Object version) {
		if ( data.concreteDescriptor.canWriteToCache()
				// No need to put into the entity cache if this is coming from the query cache already
				&& !data.getRowProcessingState().isQueryCacheHit()
				&& session.getCacheMode().isPutEnabled() ) {
			final var cacheAccess = data.concreteDescriptor.getCacheAccessStrategy();
			if ( cacheAccess != null  ) {
				putInCache( data, session, persistenceContext, resolvedEntityState, version, cacheAccess );
			}
		}
	}

	protected void registerNaturalIdResolution(
			EntityInitializerData data,
			PersistenceContext persistenceContext,
			Object[] resolvedEntityState) {
		if ( entityDescriptor.getNaturalIdMapping() != null ) {
			final Object naturalId =
					entityDescriptor.getNaturalIdMapping()
							.extractNaturalIdFromEntityState( resolvedEntityState );
			persistenceContext.getNaturalIdResolutions()
					.cacheResolutionFromLoad( data.entityKey.getIdentifier(), naturalId, entityDescriptor );
		}
	}

	protected void takeSnapshot(
			EntityInitializerData data,
			SharedSessionContractImplementor session,
			PersistenceContext persistenceContext,
			EntityEntry entityEntry,
			Object[] resolvedEntityState) {
		if ( isReallyReadOnly( data, session ) ) {
			//no need to take a snapshot - this is a
			//performance optimization, but not really
			//important, except for entities with huge
			//mutable property values
			persistenceContext.setEntryStatus( entityEntry, Status.READ_ONLY );
		}
		else {
			//take a snapshot
			deepCopy( data.concreteDescriptor, resolvedEntityState, resolvedEntityState );
			persistenceContext.setEntryStatus( entityEntry, Status.MANAGED );
		}
	}

	private boolean isReallyReadOnly(EntityInitializerData data, SharedSessionContractImplementor session) {
		if ( !data.concreteDescriptor.isMutable() ) {
			return true;
		}
		else {
			final var lazyInitializer = extractLazyInitializer( data.getInstance() );
			return lazyInitializer != null
					// there is already a proxy for this impl
					// only set the status to read-only if the proxy is read-only
					? lazyInitializer.isReadOnly()
					: isReadOnly( data.getRowProcessingState(), session );
		}
	}

	private void putInCache(
			EntityInitializerData data,
			SharedSessionContractImplementor session,
			PersistenceContext persistenceContext,
			Object[] resolvedEntityState,
			Object version,
			EntityDataAccess cacheAccess) {
		final var cacheEntry =
				data.concreteDescriptor.buildCacheEntry(
						data.entityInstanceForNotify,
						resolvedEntityState,
						version,
						session
				);
		final Object cacheKey =
				cacheAccess.generateCacheKey(
						data.entityKey.getIdentifier(),
						rootEntityDescriptor,
						session.getFactory(),
						session.getTenantIdentifier()
				);
		// Explicit handling of caching for rows just inserted and then somehow forced to be
		// read from the database *within the same transaction*. Usually this is done by:
		// 		1) Session#refresh, or
		// 		2) Session#clear + some form of load
		//
		// We need to be careful not to clobber the lock here in the cache so that it can be
		// rolled back if necessary
		if ( persistenceContext.wasInsertedDuringTransaction( data.concreteDescriptor, data.entityKey.getIdentifier() ) ) {
			putRecentlyInsertedInCache( data, session, version, cacheAccess, cacheKey, cacheEntry );
		}
		else {
			putInCache( data, session, version, cacheAccess, cacheKey, cacheEntry );
		}
	}

	private void putInCache(
			EntityInitializerData data,
			SharedSessionContractImplementor session,
			Object version,
			EntityDataAccess cacheAccess,
			Object cacheKey, CacheEntry cacheEntry) {
		final var eventListenerManager = session.getEventListenerManager();
		boolean cacheContentChanged = false;
		final var eventMonitor = session.getEventMonitor();
		final var cachePutEvent = eventMonitor.beginCachePutEvent();
		try {
			eventListenerManager.cachePutStart();
			cacheContentChanged = cacheAccess.putFromLoad(
					session,
					cacheKey,
					data.concreteDescriptor.getCacheEntryStructure().structure( cacheEntry ),
					version,
					//useMinimalPuts( session, entityEntry )
					false
			);
		}
		finally {
			eventMonitor.completeCachePutEvent(
					cachePutEvent,
					session,
					cacheAccess,
					data.concreteDescriptor,
					cacheContentChanged,
					EventMonitor.CacheActionDescription.ENTITY_LOAD
			);
			if ( cacheContentChanged ) {
				final var statistics = session.getFactory().getStatistics();
				if ( statistics.isStatisticsEnabled() ) {
					statistics.entityCachePut( rootEntityDescriptor.getNavigableRole(),
							cacheAccess.getRegion().getName() );
				}
			}
			eventListenerManager.cachePutEnd();
		}
	}

	private static void putRecentlyInsertedInCache(
			EntityInitializerData data,
			SharedSessionContractImplementor session,
			Object version,
			EntityDataAccess cacheAccess,
			Object cacheKey, CacheEntry cacheEntry) {
		boolean cacheContentChanged = false;
		final var eventMonitor = session.getEventMonitor();
		final var cachePutEvent = eventMonitor.beginCachePutEvent();
		try {
			// Updating the cache entry for entities that were inserted in this transaction only
			// makes sense for transactional caches. Other cache implementations no-op for #update.
			// Since #afterInsert will run at the end of the transaction, the state of an entity
			// will be stored in the cache eventually. Refreshing an inserted entity is a potential
			// concern because one might think that we are missing to store the refreshed data in
			// the cache. Actually, for non-transactional caches, an entity is evicted from the
			// cache on refresh via CachedDomainDataAccess#unlockItem after transaction completion,
			// so all is fine.
			if ( cacheAccess.getAccessType() == AccessType.TRANSACTIONAL ) {
				cacheContentChanged = cacheAccess.update(
						session,
						cacheKey,
						data.concreteDescriptor.getCacheEntryStructure().structure( cacheEntry ),
						version,
						version
				);
			}
		}
		finally {
			eventMonitor.completeCachePutEvent(
					cachePutEvent,
					session,
					cacheAccess,
					data.concreteDescriptor,
					cacheContentChanged,
					EventMonitor.CacheActionDescription.ENTITY_UPDATE
			);
			//TODO: Statistics. Treat it like a regular put as above?
		}
	}

	protected void registerPossibleUniqueKeyEntries(
			EntityInitializerData data,
			Object[] resolvedEntityState,
			final SharedSessionContractImplementor session) {
		for ( var uniqueKeyEntry : data.concreteDescriptor.uniqueKeyEntries() ) {
			final String uniqueKeyName = uniqueKeyEntry.getUniqueKeyName();
			final int index = uniqueKeyEntry.getStateArrayPosition();
			final Type type = uniqueKeyEntry.getPropertyType();

			// polymorphism not really handled completely correctly,
			// perhaps...well, actually it's ok, assuming that the
			// entity name used in the lookup is the same as the
			// one used here, which it will be

			final Object resolvedState = resolvedEntityState[index];
			if ( resolvedState != null ) {
				final Object key;
				if ( type instanceof ManyToOneType manyToOneType ) {
					key = ForeignKeys.getEntityIdentifier(
							manyToOneType.getAssociatedEntityName(),
							resolvedState,
							session
					);
				}
				else {
					key = resolvedState;
				}
				final var entityUniqueKey =
						new EntityUniqueKey(
								data.concreteDescriptor.getRootEntityDescriptor().getEntityName(),
								//polymorphism comment above
								uniqueKeyName,
								key,
								type,
								session.getFactory()
						);
				session.getPersistenceContextInternal()
						.addEntity( entityUniqueKey, data.entityInstanceForNotify );
			}
		}
	}

	protected Object[] extractConcreteTypeStateValues(EntityInitializerData data) {
		final var rowProcessingState = data.getRowProcessingState();
		final var values = new Object[data.concreteDescriptor.getNumberOfAttributeMappings()];
		final var concreteAssemblers = assemblers[data.concreteDescriptor.getSubclassId()];
		for ( int i = 0; i < values.length; i++ ) {
			final var assembler = concreteAssemblers[i];
			values[i] = assembler == null ? UNFETCHED_PROPERTY : assembler.assemble( rowProcessingState );
		}
		return values;
	}

	@Override
	public void resolveState(EntityInitializerData data) {
		if ( identifierAssembler != null ) {
			identifierAssembler.resolveState( data.getRowProcessingState() );
		}
		if ( discriminatorAssembler != null
			&& ( !data.shallowCached || rootEntityDescriptor.storeDiscriminatorInShallowQueryCacheLayout() ) ) {
			discriminatorAssembler.resolveState( data.getRowProcessingState() );
		}
		if ( !data.shallowCached ) {
			if ( keyAssembler != null ) {
				keyAssembler.resolveState( data.getRowProcessingState() );
			}
			if ( versionAssembler != null ) {
				versionAssembler.resolveState( data.getRowProcessingState() );
			}
			if ( rowIdAssembler != null ) {
				rowIdAssembler.resolveState( data.getRowProcessingState() );
			}
			if ( data.concreteDescriptor == null ) {
				data.concreteDescriptor = data.defaultConcreteDescriptor;
				if ( data.concreteDescriptor == null ) {
					data.concreteDescriptor = determineConcreteEntityDescriptor(
							data.getRowProcessingState(),
							castNonNull( discriminatorAssembler ),
							entityDescriptor
					);
					if ( data.concreteDescriptor == null ) {
						// this should imply the entity is missing
						return;
					}
				}
			}
			resolveEntityState( data );
		}
	}

	protected void resolveEntityState(EntityInitializerData data) {
		assert data.concreteDescriptor != null;
		final var rowProcessingState = data.getRowProcessingState();
		for ( var assembler : assemblers[data.concreteDescriptor.getSubclassId()] ) {
			if ( assembler != null ) {
				assembler.resolveState( rowProcessingState );
			}
		}
	}

	protected boolean skipInitialization(EntityInitializerData data) {
		if ( data.entityHolder.getEntityInitializer() != this ) {
			return true;
		}
		else {
			final var rowProcessingState = data.getRowProcessingState();
			final var entry = data.entityHolder.getEntityEntry();
			assert entry ==
					rowProcessingState.getSession().getPersistenceContextInternal()
						.getEntry( data.entityInstanceForNotify );
			if ( entry == null ) {
				return false;
			}
			// todo (6.0): do we really need this check ?
			else if ( entry.getStatus().isDeletedOrGone() ) {
				return true;
			}
			else {
				if ( isPersistentAttributeInterceptable( data.entityInstanceForNotify ) ) {
					final var interceptor =
							asPersistentAttributeInterceptable( data.entityInstanceForNotify )
									.$$_hibernate_getInterceptor();
					if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
						// Avoid loading the same entity proxy twice for the same result set: it could lead to errors,
						// because some code writes to its input (ID in hydrated state replaced by the loaded entity, in particular).
						return entry.getStatus() == Status.LOADING;
					}
				}

				// If the instance to initialize is the main entity, we can't skip this.
				// This can happen if we initialize an enhanced proxy.
				if ( entry.getStatus() != Status.LOADING ) {
					// If the instance to initialize is the main entity, we can't skip this.
					// This can happen if we initialize an enhanced proxy.
					return rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions()
								.getEffectiveOptionalObject() != data.entityInstanceForNotify;
				}
				else {
					return false;
				}
			}
		}
	}

	private boolean isReadOnly(RowProcessingState rowProcessingState, SharedSessionContractImplementor persistenceContext) {
		final Boolean readOnly = rowProcessingState.getQueryOptions().isReadOnly();
		return readOnly == null ? persistenceContext.isDefaultReadOnly() : readOnly;
	}

	protected void preLoad(EntityInitializerData data, Object[] resolvedEntityState) {
		final var rowProcessingState = data.getRowProcessingState();
		final var session = rowProcessingState.getSession();
		if ( session.isEventSource() ) {
			final var preLoadEvent =
					rowProcessingState.getJdbcValuesSourceProcessingState()
							.getPreLoadEvent();
			assert preLoadEvent != null;

			preLoadEvent.reset();

			preLoadEvent.setEntity( data.getInstance() )
					.setState( resolvedEntityState )
					.setId( data.entityKey.getIdentifier() )
					.setPersister( data.concreteDescriptor );

			session.getFactory()
					.getEventListenerGroups()
					.eventListenerGroup_PRE_LOAD
					.fireEventOnEachListener( preLoadEvent, PreLoadEventListener::onPreLoad );
		}
	}

	@Override
	public boolean isPartOfKey() {
		return isPartOfKey;
	}

	@Override
	public boolean isEager() {
		return true;
	}

	@Override
	public boolean hasLazySubInitializers() {
		return hasLazySubInitializers;
	}

	public boolean isPreviousRowReuse() {
		return previousRowReuse;
	}

	@Override
	public EntityPersister getConcreteDescriptor(EntityInitializerData data) {
		assert data.getState() != State.UNINITIALIZED;
		return data.concreteDescriptor == null ? entityDescriptor : data.concreteDescriptor;
	}

	protected void initializeSubInstancesFromParent(EntityInitializerData data) {
		if ( data.entityInstanceForNotify != null ) {
			for ( var initializer : subInitializers[data.concreteDescriptor.getSubclassId()] ) {
				if ( initializer != null ) {
					initializer.initializeInstanceFromParent( data.entityInstanceForNotify, data.getRowProcessingState() );
				}
			}
		}
	}

	@Override
	protected void forEachSubInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, InitializerData data) {
		final var rowProcessingState = data.getRowProcessingState();
		if ( keyAssembler != null ) {
			final var initializer = keyAssembler.getInitializer();
			if ( initializer != null ) {
				consumer.accept( initializer, rowProcessingState );
			}
		}
		if ( identifierAssembler != null ) {
			final var initializer = identifierAssembler.getInitializer();
			if ( initializer != null ) {
				consumer.accept( initializer, rowProcessingState );
			}
		}
		final var entityInitializerData = (EntityInitializerData) data;
		if ( entityInitializerData.concreteDescriptor == null ) {
			for ( var initializer : allInitializers ) {
				if ( initializer != null ) {
					consumer.accept( initializer, rowProcessingState );
				}
			}
		}
		else {
			for ( var initializer : subInitializers[entityInitializerData.concreteDescriptor.getSubclassId()] ) {
				if ( initializer != null ) {
					consumer.accept( initializer, rowProcessingState );
				}
			}
		}
	}

	public static PersistentAttributeInterceptor getAttributeInterceptor(Object entity) {
		return asPersistentAttributeInterceptable( entity ).$$_hibernate_getInterceptor();
	}

	@Override
	public String toString() {
		return "EntityJoinedFetchInitializer("
				+ toLoggableString( getNavigablePath() ) + ")";
	}

	//#########################
	// For Hibernate Reactive
	//#########################
	protected EntityPersister getRootEntityDescriptor() {
		return rootEntityDescriptor;
	}

	protected @Nullable Initializer<?>[][] getEagerSubInitializers() {
		return eagerSubInitializers;
	}

	protected @Nullable Initializer<?>[][] getCollectionContainingSubInitializers() {
		return collectionContainingSubInitializers;
	}

	protected ImmutableBitSet[] getLazySets() {
		return lazySets;
	}

	public boolean isHasLazyInitializingSubAssemblers() {
		return hasLazyInitializingSubAssemblers;
	}

	protected @Nullable DomainResultAssembler<?> getVersionAssembler() {
		return versionAssembler;
	}

	protected @Nullable DomainResultAssembler<Object> getRowIdAssembler() {
		return rowIdAssembler;
	}

	protected @Nullable DomainResultAssembler<?>[][] getAssemblers() {
		return assemblers;
	}

	protected @Nullable BasicResultAssembler<?> discriminatorAssembler() {
		return discriminatorAssembler;
	}

	protected boolean isKeyManyToOne() {
		return hasKeyManyToOne;
	}

	protected Initializer<?>[][] getSubInitializers() {
		return subInitializers;
	}

	public @Nullable DomainResultAssembler<?> getKeyAssembler() {
		return keyAssembler;
	}
}
