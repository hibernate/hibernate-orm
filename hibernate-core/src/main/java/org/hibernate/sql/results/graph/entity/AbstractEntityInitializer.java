/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity;

import java.util.Collection;
import java.util.function.Consumer;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.WrongClassException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventManager;
import org.hibernate.event.spi.HibernateMonitoringEvent;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.ast.internal.CacheEntityLoaderHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.DiscriminatorValueDetails;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UniqueKeyEntry;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.proxy.map.MapProxy;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.basic.BasicResultAssembler;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableAssembler;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.Type;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.UNFETCHED_PROPERTY;
import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.internal.log.LoggingHelper.toLoggableString;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * @author Steve Ebersole
 * @author Nathan Xu
 */
public abstract class AbstractEntityInitializer extends AbstractFetchParentAccess implements EntityInitializer {

	// NOTE : even though we only keep the EntityDescriptor here, rather than EntityResultGraphNode
	//		the "scope" of this initializer is a specific EntityReference.
	//
	//		The full EntityResultGraphNode is simply not needed here, and so we just keep
	//		the EntityDescriptor here to avoid chicken/egg issues in the creation of
	// 		these

	private final EntityValuedModelPart referencedModelPart;
	private final EntityPersister entityDescriptor;
	private final EntityPersister rootEntityDescriptor;
	private final NavigablePath navigablePath;
	private final LockMode lockMode;
	private final FetchParentAccess parentAccess;
	private final FetchParentAccess owningParent;
	private final EntityMappingType ownedModelPartDeclaringType;
	private final boolean isPartOfKey;

	private final DomainResultAssembler<?> identifierAssembler;
	private final BasicResultAssembler<?> discriminatorAssembler;
	private final DomainResultAssembler<?> versionAssembler;
	private final DomainResultAssembler<Object> rowIdAssembler;

	private final DomainResultAssembler<?>[][] assemblers;

	private boolean shallowCached;

	// per-row state
	private EntityPersister concreteDescriptor;
	private EntityKey entityKey;
	private Object version;
	private Object entityInstance;
	protected Object entityInstanceForNotify;
	protected State state = State.UNINITIALIZED;
	private boolean isOwningInitializer;
	private Object[] resolvedEntityState;

	/**
	 * @deprecated Use {@link AbstractEntityInitializer#AbstractEntityInitializer(EntityResultGraphNode, NavigablePath, LockMode, Fetch, Fetch, DomainResult, FetchParentAccess, AssemblerCreationState)} instead.
	 */
	@Deprecated(forRemoval = true)
	protected AbstractEntityInitializer(
			EntityResultGraphNode resultDescriptor,
			NavigablePath navigablePath,
			LockMode lockMode,
			Fetch identifierFetch,
			Fetch discriminatorFetch,
			DomainResult<Object> rowIdResult,
			AssemblerCreationState creationState) {
		this( resultDescriptor, navigablePath, lockMode, identifierFetch, discriminatorFetch, rowIdResult, null, creationState );
	}

	// todo (6.0) : ^^ need a better way to track whether we are loading the entity state or if something else is/has
	protected AbstractEntityInitializer(
			EntityResultGraphNode resultDescriptor,
			NavigablePath navigablePath,
			LockMode lockMode,
			Fetch identifierFetch,
			Fetch discriminatorFetch,
			DomainResult<Object> rowIdResult,
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		referencedModelPart = resultDescriptor.getEntityValuedModelPart();
		entityDescriptor = (EntityPersister) referencedModelPart.getEntityMappingType();

		final String rootEntityName = entityDescriptor.getRootEntityName();
		rootEntityDescriptor = rootEntityName == null || rootEntityName.equals( entityDescriptor.getEntityName() )
				? entityDescriptor
				: entityDescriptor.getRootEntityDescriptor().getEntityPersister();

		this.navigablePath = navigablePath;
		this.lockMode = lockMode;
		this.parentAccess = parentAccess;
		assert lockMode != null;
		this.isPartOfKey = Initializer.isPartOfKey( navigablePath, parentAccess );
		this.owningParent = FetchParentAccess.determineOwningParent( parentAccess );
		this.ownedModelPartDeclaringType = FetchParentAccess.determineOwnedModelPartDeclaringType( referencedModelPart, parentAccess, owningParent );

		assert identifierFetch != null || isResultInitializer() : "Identifier must be fetched, unless this is a result initializer";
		identifierAssembler = identifierFetch != null
				? identifierFetch.createAssembler( this, creationState )
				: null;

		assert entityDescriptor.hasSubclasses() == (discriminatorFetch != null) : "Discriminator should only be fetched if the entity has subclasses";
		discriminatorAssembler = discriminatorFetch != null
				? (BasicResultAssembler<?>) discriminatorFetch.createAssembler( this, creationState )
				: null;

		final EntityVersionMapping versionMapping = entityDescriptor.getVersionMapping();
		if ( versionMapping != null ) {
			final Fetch versionFetch = resultDescriptor.findFetch( versionMapping );
			// If there is a version mapping, there must be a fetch for it
			assert versionFetch != null;
			versionAssembler = versionFetch.createAssembler( this, creationState );
		}
		else {
			versionAssembler = null;
		}

		rowIdAssembler = rowIdResult != null
				? rowIdResult.createResultAssembler( this, creationState )
				: null;

		final Collection<EntityMappingType> subMappingTypes = rootEntityDescriptor.getSubMappingTypes();
		assemblers = new DomainResultAssembler[subMappingTypes.size() + 1][];
		assemblers[rootEntityDescriptor.getSubclassId()] = new DomainResultAssembler[rootEntityDescriptor.getNumberOfFetchables()];

		for ( EntityMappingType subMappingType : subMappingTypes ) {
			assemblers[subMappingType.getSubclassId()] = new DomainResultAssembler[subMappingType.getNumberOfFetchables()];
		}

		final int size = entityDescriptor.getNumberOfFetchables();
		for ( int i = 0; i < size; i++ ) {
			final AttributeMapping attributeMapping = entityDescriptor.getFetchable( i ).asAttributeMapping();
			// todo (6.0) : somehow we need to track whether all state is loaded/resolved
			//		note that lazy proxies or uninitialized collections count against
			//		that in the affirmative
			final Fetch fetch = resultDescriptor.findFetch( attributeMapping );
			final DomainResultAssembler<?> stateAssembler = fetch == null
					? new NullValueAssembler<>( attributeMapping.getMappedType().getMappedJavaType() )
					: fetch.createAssembler( this, creationState );

			final int stateArrayPosition = attributeMapping.getStateArrayPosition();
			final EntityMappingType declaringType = attributeMapping.getDeclaringType().asEntityMappingType();

			assemblers[declaringType.getSubclassId()][stateArrayPosition] = stateAssembler;
			for ( EntityMappingType subMappingType : declaringType.getSubMappingTypes() ) {
				assemblers[subMappingType.getSubclassId()][stateArrayPosition] = stateAssembler;
			}
		}
	}

	private static void deepCopy(ManagedMappingType containerDescriptor, Object[] source, Object[] target) {
		final int numberOfAttributeMappings = containerDescriptor.getNumberOfAttributeMappings();
		for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
			final AttributeMapping attributeMapping = containerDescriptor.getAttributeMapping( i );
			final AttributeMetadata attributeMetadata = attributeMapping.getAttributeMetadata();
			if ( attributeMetadata.isUpdatable() ) {
				final int position = attributeMapping.getStateArrayPosition();
				target[position] = copy( attributeMetadata, source[position] );
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static Object copy(AttributeMetadata attributeMetadata, Object sourceValue) {
		return sourceValue == LazyPropertyInitializer.UNFETCHED_PROPERTY
					|| sourceValue == PropertyAccessStrategyBackRefImpl.UNKNOWN
				? sourceValue
				: attributeMetadata.getMutabilityPlan().deepCopy( sourceValue );
	}

	@Override
	public ModelPart getInitializedPart() {
		return referencedModelPart;
	}

	/**
	 * Simple class name of this initializer for logging
	 */
	protected abstract String getSimpleConcreteImplName();

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
	public Object getEntityInstance() {
		return entityInstance;
	}

	protected void setEntityInstance(Object entityInstance) {
		this.entityInstance = entityInstance;
	}

	@SuppressWarnings("unused")
	public Object getKeyValue() {
		return entityKey == null ? null : entityKey.getIdentifier();
	}

	@Override
	public EntityKey getEntityKey() {
		return entityKey;
	}

	@Override
	public Object getParentKey() {
		return getKeyValue();
	}

	@Override
	public FetchParentAccess getFetchParentAccess() {
		return parentAccess;
	}

	@Override
	public @Nullable FetchParentAccess getOwningParent() {
		return owningParent;
	}

	@Override
	public @Nullable EntityMappingType getOwnedModelPartDeclaringType() {
		return ownedModelPartDeclaringType;
	}

	@Override
	public void registerResolutionListener(Consumer<Object> listener) {
		if ( state == State.RESOLVED || state == State.INITIALIZED ) {
			listener.accept( entityInstanceForNotify );
		}
		else {
			super.registerResolutionListener( listener );
		}
	}

	@Override
	public void startLoading(RowProcessingState rowProcessingState) {
		if ( rowProcessingState.isQueryCacheHit() && entityDescriptor.useShallowQueryCacheLayout() && !isParentShallowCached() ) {
			shallowCached = true;
			// Inform sub-initializers if this is a query cache hit for a shallow entry
			markSubInitializersAsShallowCached();
		}
	}

	@Override
	public void markShallowCached() {
		super.markShallowCached();
		markSubInitializersAsShallowCached();
	}

	private void markSubInitializersAsShallowCached() {
		for ( DomainResultAssembler<?>[] subtypeAssemblers : assemblers ) {
			for ( DomainResultAssembler<?> assembler : subtypeAssemblers ) {
				if ( assembler != null ) {
					final Initializer initializer = assembler.getInitializer();
					if ( initializer != null ) {
						initializer.markShallowCached();
					}
				}
			}
		}
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		// todo (6.0) : atm we do not handle sequential selects
		// 		- see AbstractEntityPersister#hasSequentialSelect and
		//			AbstractEntityPersister#getSequentialSelect in 5.2

		if ( state == State.UNINITIALIZED ) {
			if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isTraceEnabled() ) {
				EntityLoadingLogging.ENTITY_LOADING_LOGGER.tracef(
						"(%s) Beginning Initializer#resolveKey process for entity : %s",
						StringHelper.collapse( this.getClass().getName() ),
						getNavigablePath()
				);
			}

			final Object id = initializeIdentifier( rowProcessingState );
			if ( id == null ) {
				state = State.MISSING;
				EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
						"(%s) EntityKey (%s) is null",
						getSimpleConcreteImplName(),
						getNavigablePath()
				);
			}
			else if ( concreteDescriptor != null
					|| ( concreteDescriptor = determineConcreteEntityDescriptor( rowProcessingState ) ) != null ) {
				// 2) build the EntityKey
				entityKey = new EntityKey( id, concreteDescriptor );
				state = State.KEY_RESOLVED;
				if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isDebugEnabled() ) {
					EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
							"(%s) Hydrated EntityKey (%s): %s",
							getSimpleConcreteImplName(),
							getNavigablePath(),
							entityKey.getIdentifier()
					);
				}
			}
		}
	}

	private EntityPersister determineConcreteEntityDescriptor(RowProcessingState rowProcessingState)
			throws WrongClassException {
		if ( discriminatorAssembler == null
				|| rowProcessingState.isQueryCacheHit() && !entityDescriptor.storeDiscriminatorInShallowQueryCacheLayout() ) {
			return entityDescriptor;
		}
		else {
			assert entityDescriptor.hasSubclasses() : "Reading a discriminator from a result set should only happen if the entity has subclasses";
			final EntityDiscriminatorMapping discriminatorMapping = entityDescriptor.getDiscriminatorMapping();
			assert discriminatorMapping != null;
			final Object discriminator = discriminatorAssembler.extractRawValue( rowProcessingState );
			final DiscriminatorValueDetails discriminatorDetails =
					discriminatorMapping.resolveDiscriminatorValue( discriminator );
			if ( discriminatorDetails == null ) {
				return entityDescriptor;
			}
			else {
				final EntityMappingType indicatedEntity = discriminatorDetails.getIndicatedEntity();
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
	private Object initializeIdentifier(RowProcessingState rowProcessingState) {
		final JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState =
				rowProcessingState.getJdbcValuesSourceProcessingState();
		final Object id = jdbcValuesSourceProcessingState.getProcessingOptions().getEffectiveOptionalId();
		if ( useEmbeddedIdentifierInstanceAsEntity( id, rowProcessingState ) ) {
			entityInstance = entityInstanceForNotify = id;
			return id;
		}
		else if ( identifierAssembler == null ) {
			assert id != null : "Initializer requires a not null id for loading";
			return id;
		}
		else {
			return identifierAssembler.assemble(
					rowProcessingState,
					jdbcValuesSourceProcessingState.getProcessingOptions()
			);
		}
	}

	private boolean useEmbeddedIdentifierInstanceAsEntity(Object id, RowProcessingState rowProcessingState) {
		return id != null && isResultInitializer()
				// The id can only be the entity instance if this is a non-aggregated id that has no containing class
				&& entityDescriptor.getIdentifierMapping() instanceof CompositeIdentifierMapping
				&& !( (CompositeIdentifierMapping) entityDescriptor.getIdentifierMapping() ).hasContainingClass()
				&& ( concreteDescriptor = determineConcreteEntityDescriptor( rowProcessingState ) ) != null
				&& concreteDescriptor.isInstance( id );
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		if ( state == State.KEY_RESOLVED ) {
			// Note that entities are always resolved to be initialized,
			// even if a parent fetch parent access is "missing".
			// We loaded the data already, so let's put it into the persistence context,
			// even if the parent instance will not refer to this entity.

			final PersistenceContext persistenceContext = rowProcessingState.getSession().getPersistenceContextInternal();
			final EntityHolder holder = persistenceContext.claimEntityHolderIfPossible(
					entityKey,
					null,
					rowProcessingState.getJdbcValuesSourceProcessingState(),
					this
			);
			state = State.RESOLVED;
			isOwningInitializer = holder.getEntityInitializer() == this;

			if ( entityInstance == null ) {
				resolveEntityInstance( rowProcessingState, holder, entityKey.getIdentifier() );
				if ( isResultInitializer() ) {
					final String uniqueKeyAttributePath = rowProcessingState.getEntityUniqueKeyAttributePath();
					if ( uniqueKeyAttributePath != null ) {
						final SharedSessionContractImplementor session = rowProcessingState.getSession();
						final EntityUniqueKey euk = new EntityUniqueKey(
								getConcreteDescriptor().getEntityName(),
								uniqueKeyAttributePath,
								rowProcessingState.getEntityUniqueKey(),
								getConcreteDescriptor().getPropertyType( uniqueKeyAttributePath ),
								session.getFactory()
						);
						session.getPersistenceContextInternal().addEntity( euk, getEntityInstance() );
					}
				}
			}
			else if ( !isOwningInitializer ) {
				entityInstance = holder.getManagedObject();
				entityInstanceForNotify = holder.getEntity();
				state = State.INITIALIZED;
			}
			notifyResolutionListeners( entityInstanceForNotify );
			if ( shallowCached ) {
				initializeSubInstancesFromParent( rowProcessingState );
			}
		}
	}

	protected void resolveEntityInstance(
			RowProcessingState rowProcessingState,
			EntityHolder holder,
			Object entityIdentifier) {

		if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isTraceEnabled() ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.tracef(
					"(%s) Beginning Initializer#resolveInstance process for entity (%s) : %s",
					StringHelper.collapse( this.getClass().getName() ),
					getNavigablePath(),
					entityIdentifier
			);
		}

		final Object proxy = holder.getProxy();
		final boolean unwrapProxy = proxy != null && referencedModelPart instanceof ToOneAttributeMapping
				&& ( (ToOneAttributeMapping) referencedModelPart ).isUnwrapProxy()
				&& getConcreteDescriptor().getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();
		final Object entityFromExecutionContext;
		if ( !unwrapProxy && isProxyInstance( proxy ) ) {
			if ( ( entityFromExecutionContext = getEntityFromExecutionContext( rowProcessingState ) ) != null ) {
				entityInstance = entityInstanceForNotify = entityFromExecutionContext;
				// If the entity comes from the execution context, it is treated as not initialized
				// so that we can refresh the data as requested
				registerReloadedEntity( rowProcessingState, holder );
			}
			else {
				entityInstance = proxy;
				if ( Hibernate.isInitialized( entityInstance ) ) {
					state = State.INITIALIZED;
					entityInstanceForNotify = Hibernate.unproxy( entityInstance );
					registerReloadedEntity( rowProcessingState, holder );
					if ( !rowProcessingState.isQueryCacheHit() && rowProcessingState.getQueryOptions().isResultCachingEnabled() == Boolean.TRUE ) {
						// We need to read result set values to correctly populate the query cache
						resolveState( rowProcessingState );
					}
				}
				else {
					final LazyInitializer lazyInitializer = extractLazyInitializer( entityInstance );
					assert lazyInitializer != null;
					entityInstanceForNotify = resolveInstance( entityIdentifier, holder, rowProcessingState );
					lazyInitializer.setImplementation( entityInstanceForNotify );
				}
			}
		}
		else {
			final Object existingEntity = holder.getEntity();
			if ( existingEntity != null ) {
				entityInstance = entityInstanceForNotify = existingEntity;
				if ( holder.getEntityInitializer() == null ) {
					if ( isExistingEntityInitialized( existingEntity ) ) {
						state = State.INITIALIZED;
						registerReloadedEntity( rowProcessingState, holder );
						if ( !rowProcessingState.isQueryCacheHit() && rowProcessingState.getQueryOptions().isResultCachingEnabled() == Boolean.TRUE ) {
							// We need to read result set values to correctly populate the query cache
							resolveState( rowProcessingState );
						}
					}
					else if ( isResultInitializer() ) {
						registerLoadingEntity( rowProcessingState, entityInstance );
					}
				}
				else if ( !isOwningInitializer ) {
					state = State.INITIALIZED;
				}
			}
			else if ( ( entityFromExecutionContext = getEntityFromExecutionContext( rowProcessingState ) ) != null ) {
				// This is the entity to refresh, so don't set the state to initialized
				entityInstance = entityInstanceForNotify = entityFromExecutionContext;
				if ( isResultInitializer() ) {
					registerLoadingEntity( rowProcessingState, entityInstance );
				}
			}
			else {
				// look to see if another initializer from a parent load context or an earlier
				// initializer is already loading the entity
				entityInstance = entityInstanceForNotify = resolveInstance( entityIdentifier, holder, rowProcessingState );
				if ( isOwningInitializer && !isEntityInitialized() && identifierAssembler instanceof EmbeddableAssembler ) {
					// If this is the owning initializer and the returned object is not initialized,
					// this means that the entity instance was just instantiated.
					// In this case, we want to call "assemble" and hence "initializeInstance" on the initializer
					// for possibly non-aggregated identifier mappings, so inject the virtual id representation
					identifierAssembler.assemble( rowProcessingState );
				}
			}
		}
		upgradeLockMode( rowProcessingState );
	}

	protected Object getEntityFromExecutionContext(RowProcessingState rowProcessingState) {
		final ExecutionContext executionContext = rowProcessingState.getJdbcValuesSourceProcessingState()
				.getExecutionContext();
		if ( rootEntityDescriptor == executionContext.getRootEntityDescriptor()
				&& getEntityKey().getIdentifier().equals( executionContext.getEntityId() ) ) {
			return executionContext.getEntityInstance();
		}
		return null;
	}

	private void upgradeLockMode(RowProcessingState rowProcessingState) {
		if ( lockMode != LockMode.NONE && rowProcessingState.upgradeLocks() ) {
			final EntityEntry entry =
					rowProcessingState.getSession().getPersistenceContextInternal()
							.getEntry( entityInstance );
			if ( entry != null && entry.getLockMode().lessThan( lockMode ) ) {
				//we only check the version when _upgrading_ lock modes
				if ( versionAssembler != null && entry.getLockMode() != LockMode.NONE ) {
					checkVersion( entry, rowProcessingState );
				}
				//we need to upgrade the lock mode to the mode requested
				entry.setLockMode( lockMode );
			}
		}
	}

	private boolean isProxyInstance(Object proxy) {
		return proxy != null
			&& ( proxy instanceof MapProxy || entityDescriptor.getJavaType().getJavaTypeClass().isInstance( proxy ) );
	}

	private boolean isExistingEntityInitialized(Object existingEntity) {
		return Hibernate.isInitialized( existingEntity );
	}

	/**
	 * Check the version of the object in the {@code RowProcessingState} against
	 * the object version in the session cache, throwing an exception
	 * if the version numbers are different
	 */
	private void checkVersion(EntityEntry entry, final RowProcessingState rowProcessingState) throws HibernateException {
		final Object version = entry.getVersion();
		if ( version != null ) {
			// null version means the object is in the process of being loaded somewhere else in the ResultSet
			final Object currentVersion = versionAssembler.assemble( rowProcessingState );
			if ( !concreteDescriptor.getVersionType().isEqual( version, currentVersion ) ) {
				final StatisticsImplementor statistics = rowProcessingState.getSession().getFactory().getStatistics();
				if ( statistics.isStatisticsEnabled() ) {
					statistics.optimisticFailure( concreteDescriptor.getEntityName() );
				}
				throw new StaleObjectStateException( concreteDescriptor.getEntityName(), entry.getId() );
			}
		}

	}

	/**
	 * Used by Hibernate Reactive
	 */
	protected Object resolveInstance(
			Object entityIdentifier,
			EntityHolder holder,
			RowProcessingState rowProcessingState) {
		if ( isOwningInitializer ) {
			assert holder.getEntity() == null;
			return resolveEntityInstance( entityIdentifier, rowProcessingState );
		}
		else {
			// the entity is already being loaded elsewhere
			if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isDebugEnabled() ) {
				EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
						"(%s) Entity [%s] being loaded by another initializer [%s] - skipping processing",
						getSimpleConcreteImplName(),
						toLoggableString( getNavigablePath(), entityIdentifier ),
						holder.getEntityInitializer()
				);
			}
			return holder.getEntity();
		}
	}

	protected Object resolveEntityInstance(Object entityIdentifier, RowProcessingState rowProcessingState) {
		final Object resolved = resolveToOptionalInstance( rowProcessingState );
		if ( resolved != null ) {
			registerLoadingEntity( rowProcessingState, resolved );
			return resolved;
		}
		else {
			if ( rowProcessingState.isQueryCacheHit() && entityDescriptor.useShallowQueryCacheLayout() ) {
				// We must load the entity this way, because the query cache entry contains only the primary key
				state = State.INITIALIZED;
				final SharedSessionContractImplementor session = rowProcessingState.getSession();
				assert isOwningInitializer;
				// If this initializer owns the entity, we have to remove the entity holder,
				// because the subsequent loading process will claim the entity
				session.getPersistenceContextInternal().removeEntityHolder( entityKey );
				return session.internalLoad(
						concreteDescriptor.getEntityName(),
						entityIdentifier,
						true,
						false
				);
			}
			// We have to query the second level cache if reference cache entries are used
			else if ( entityDescriptor.canUseReferenceCacheEntries() ) {
				final Object cached = resolveInstanceFromCache( rowProcessingState );
				if ( cached != null ) {
					// EARLY EXIT!!!
					// because the second level cache has reference cache entries, the entity is initialized
					state = State.INITIALIZED;
					registerReloadedEntity( rowProcessingState );
					return cached;
				}
			}
			final Object instance = instantiateEntity( entityIdentifier, rowProcessingState.getSession() );
			registerLoadingEntity( rowProcessingState, instance );
			return instance;
		}
	}

	protected Object instantiateEntity(Object entityIdentifier, SharedSessionContractImplementor session) {
		final Object instance = session.instantiate( concreteDescriptor, entityKey.getIdentifier() );
		if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isDebugEnabled() ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
					"(%s) Created new entity instance [%s] : %s",
					getSimpleConcreteImplName(),
					toLoggableString( getNavigablePath(), entityIdentifier ),
					System.identityHashCode( instance )
			);
		}
		return instance;
	}

	private Object resolveToOptionalInstance(RowProcessingState rowProcessingState) {
		if ( isResultInitializer() ) {
			// this isEntityReturn bit is just for entity loaders, not hql/criteria
			final JdbcValuesSourceProcessingOptions processingOptions =
					rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions();
			return matchesOptionalInstance( processingOptions ) ? processingOptions.getEffectiveOptionalObject() : null;
		}
		else {
			return null;
		}
	}

	private boolean matchesOptionalInstance(JdbcValuesSourceProcessingOptions processingOptions) {
		final Object optionalEntityInstance = processingOptions.getEffectiveOptionalObject();
		final Object requestedEntityId = processingOptions.getEffectiveOptionalId();
		return requestedEntityId != null
			&& optionalEntityInstance != null
			&& requestedEntityId.equals( entityKey.getIdentifier() );
	}

	private Object resolveInstanceFromCache(RowProcessingState rowProcessingState) {
		return CacheEntityLoaderHelper.INSTANCE.loadFromSecondLevelCache(
				rowProcessingState.getSession().asEventSource(),
				null,
				lockMode,
				entityDescriptor,
				entityKey
		);
	}

	protected void registerLoadingEntity(RowProcessingState rowProcessingState, Object instance) {
		rowProcessingState.getSession().getPersistenceContextInternal().claimEntityHolderIfPossible(
				entityKey,
				instance,
				rowProcessingState.getJdbcValuesSourceProcessingState(),
				this
		);
	}

	protected void registerReloadedEntity(RowProcessingState rowProcessingState) {
		if ( rowProcessingState.hasCallbackActions() ) {
			rowProcessingState.getSession().getPersistenceContextInternal().getEntityHolder( entityKey )
					.markAsReloaded( rowProcessingState.getJdbcValuesSourceProcessingState() );
		}
	}

	protected void registerReloadedEntity(RowProcessingState rowProcessingState, EntityHolder holder) {
		if ( rowProcessingState.hasCallbackActions() ) {
			// This is only needed for follow-on locking, so skip registering the entity if there is no callback
			holder.markAsReloaded( rowProcessingState.getJdbcValuesSourceProcessingState() );
		}
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		if ( state == State.KEY_RESOLVED || state == State.RESOLVED ) {
			initializeEntity( entityInstanceForNotify, rowProcessingState );
			state = State.INITIALIZED;
		}
	}

	private void initializeEntity(Object toInitialize, RowProcessingState rowProcessingState) {
		if ( !skipInitialization( toInitialize, rowProcessingState ) ) {
			assert consistentInstance( toInitialize, rowProcessingState );
			initializeEntityInstance( toInitialize, rowProcessingState );
		}
	}

	protected boolean consistentInstance(Object toInitialize, RowProcessingState rowProcessingState) {
		final PersistenceContext persistenceContextInternal =
				rowProcessingState.getSession().getPersistenceContextInternal();
		// Only call PersistenceContext#getEntity within the assert expression, as it is costly
		final Object entity = persistenceContextInternal.getEntity( entityKey );
		return entity == null || entity == toInitialize;
	}

	private void initializeEntityInstance(Object toInitialize, RowProcessingState rowProcessingState) {
		final Object entityIdentifier = entityKey.getIdentifier();
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();

		if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isTraceEnabled() ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.tracef(
					"(%s) Beginning Initializer#initializeInstance process for entity %s",
					getSimpleConcreteImplName(),
					toLoggableString( getNavigablePath(), entityIdentifier )
			);
		}

		resolvedEntityState = extractConcreteTypeStateValues( rowProcessingState );

		preLoad( rowProcessingState );

		if ( isPersistentAttributeInterceptable(toInitialize) ) {
			PersistentAttributeInterceptor persistentAttributeInterceptor =
					asPersistentAttributeInterceptable( toInitialize ).$$_hibernate_getInterceptor();
			if ( persistentAttributeInterceptor == null
					|| persistentAttributeInterceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
				// if we do this after the entity has been initialized the
				// BytecodeLazyAttributeInterceptor#isAttributeLoaded(String fieldName) would return false;
				concreteDescriptor.getBytecodeEnhancementMetadata()
						.injectInterceptor( toInitialize, entityIdentifier, session );
			}
		}
		concreteDescriptor.setPropertyValues( toInitialize, resolvedEntityState );

		persistenceContext.addEntity( entityKey, toInitialize );

		// Also register possible unique key entries
		registerPossibleUniqueKeyEntries( toInitialize, session );

		version = versionAssembler != null ? versionAssembler.assemble( rowProcessingState ) : null;
		final Object rowId = rowIdAssembler != null ? rowIdAssembler.assemble( rowProcessingState ) : null;

		// from the perspective of Hibernate, an entity is read locked as soon as it is read
		// so regardless of the requested lock mode, we upgrade to at least the read level
		final LockMode lockModeToAcquire = lockMode == LockMode.NONE ? LockMode.READ : lockMode;

		final EntityEntry entityEntry = persistenceContext.addEntry(
				toInitialize,
				Status.LOADING,
				resolvedEntityState,
				rowId,
				entityKey.getIdentifier(),
				version,
				lockModeToAcquire,
				true,
				concreteDescriptor,
				false
		);

		registerNaturalIdResolution( persistenceContext, entityIdentifier );

		takeSnapshot( rowProcessingState, session, persistenceContext, entityEntry );

		concreteDescriptor.afterInitialize( toInitialize, session );

		if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isDebugEnabled() ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
					"(%s) Done materializing entityInstance : %s",
					getSimpleConcreteImplName(),
					toLoggableString( getNavigablePath(), entityIdentifier )
			);
		}

		assert concreteDescriptor.getIdentifier( toInitialize, session ) != null;

		final StatisticsImplementor statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			if ( !rowProcessingState.isQueryCacheHit() ) {
				statistics.loadEntity( concreteDescriptor.getEntityName() );
			}
		}
	}

	protected void updateCaches(
			Object toInitialize,
			RowProcessingState rowProcessingState,
			SharedSessionContractImplementor session,
			PersistenceContext persistenceContext,
			Object entityIdentifier,
			Object version) {
		// No need to put into the entity cache if this is coming from the query cache already
		final EntityDataAccess cacheAccess = concreteDescriptor.getCacheAccessStrategy();
		if ( !rowProcessingState.isQueryCacheHit() && cacheAccess != null && session.getCacheMode().isPutEnabled() ) {
			putInCache( toInitialize, session, persistenceContext, entityIdentifier, version, cacheAccess );
		}
	}

	protected void registerNaturalIdResolution(PersistenceContext persistenceContext, Object entityIdentifier) {
		if ( entityDescriptor.getNaturalIdMapping() != null ) {
			final Object naturalId =
					entityDescriptor.getNaturalIdMapping().extractNaturalIdFromEntityState( resolvedEntityState );
			persistenceContext.getNaturalIdResolutions()
					.cacheResolutionFromLoad( entityIdentifier, naturalId, entityDescriptor );
		}
	}

	protected void takeSnapshot(
			RowProcessingState rowProcessingState,
			SharedSessionContractImplementor session,
			PersistenceContext persistenceContext,
			EntityEntry entityEntry) {
		if ( isReallyReadOnly( rowProcessingState, session ) ) {
			//no need to take a snapshot - this is a
			//performance optimization, but not really
			//important, except for entities with huge
			//mutable property values
			persistenceContext.setEntryStatus( entityEntry, Status.READ_ONLY );
		}
		else {
			//take a snapshot
			deepCopy( concreteDescriptor, resolvedEntityState, resolvedEntityState );
			persistenceContext.setEntryStatus( entityEntry, Status.MANAGED );
		}
	}

	private boolean isReallyReadOnly(RowProcessingState rowProcessingState, SharedSessionContractImplementor session) {
		if ( !concreteDescriptor.isMutable() ) {
			return true;
		}
		else {
			final LazyInitializer lazyInitializer = extractLazyInitializer( entityInstance );
			if ( lazyInitializer != null ) {
				// there is already a proxy for this impl
				// only set the status to read-only if the proxy is read-only
				return lazyInitializer.isReadOnly();
			}
			else {
				return isReadOnly( rowProcessingState, session );
			}
		}
	}

	private void putInCache(
			Object toInitialize,
			SharedSessionContractImplementor session,
			PersistenceContext persistenceContext,
			Object entityIdentifier,
			Object version,
			EntityDataAccess cacheAccess) {
		final SessionFactoryImplementor factory = session.getFactory();

		if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isDebugEnabled() ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
					"(%S) Adding entityInstance to second-level cache: %s",
					getSimpleConcreteImplName(),
					toLoggableString( getNavigablePath(), entityIdentifier)
			);
		}

		final CacheEntry cacheEntry = concreteDescriptor.buildCacheEntry( toInitialize, resolvedEntityState, version, session );
		final Object cacheKey = cacheAccess.generateCacheKey(
				entityIdentifier,
				rootEntityDescriptor,
				factory,
				session.getTenantIdentifier()
		);

		// explicit handling of caching for rows just inserted and then somehow forced to be read
		// from the database *within the same transaction*.  usually this is done by
		// 		1) Session#refresh, or
		// 		2) Session#clear + some form of load
		//
		// we need to be careful not to clobber the lock here in the cache so that it can be rolled back if need be
		final EventManager eventManager = session.getEventManager();
		if ( persistenceContext.wasInsertedDuringTransaction( concreteDescriptor, entityIdentifier) ) {
			boolean update = false;
			final HibernateMonitoringEvent cachePutEvent = eventManager.beginCachePutEvent();
			try {
				update = cacheAccess.update(
						session,
						cacheKey,
						concreteDescriptor.getCacheEntryStructure().structure( cacheEntry ),
						version,
						version
				);
			}
			finally {
				eventManager.completeCachePutEvent(
						cachePutEvent,
						session,
						cacheAccess,
						concreteDescriptor,
						update,
						EventManager.CacheActionDescription.ENTITY_UPDATE
				);
			}
		}
		else {
			final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
			boolean put = false;
			final HibernateMonitoringEvent cachePutEvent = eventManager.beginCachePutEvent();
			try {
				eventListenerManager.cachePutStart();
				put = cacheAccess.putFromLoad(
						session,
						cacheKey,
						concreteDescriptor.getCacheEntryStructure().structure( cacheEntry ),
						version,
						//useMinimalPuts( session, entityEntry )
						false
				);
			}
			finally {
				eventManager.completeCachePutEvent(
						cachePutEvent,
						session,
						cacheAccess,
						concreteDescriptor,
						put,
						EventManager.CacheActionDescription.ENTITY_LOAD
				);
				final StatisticsImplementor statistics = factory.getStatistics();
				if ( put && statistics.isStatisticsEnabled() ) {
					statistics.entityCachePut( rootEntityDescriptor.getNavigableRole(), cacheAccess.getRegion().getName() );
				}
				eventListenerManager.cachePutEnd();
			}
		}
	}

	protected void registerPossibleUniqueKeyEntries(final Object toInitialize, final SharedSessionContractImplementor session) {
		for ( UniqueKeyEntry entry : concreteDescriptor.uniqueKeyEntries() ) {
			final String ukName = entry.getUniqueKeyName();
			final int index = entry.getStateArrayPosition();
			final Type type = entry.getPropertyType();

			// polymorphism not really handled completely correctly,
			// perhaps...well, actually its ok, assuming that the
			// entity name used in the lookup is the same as the
			// one used here, which it will be

			if ( resolvedEntityState[index] != null ) {
				final EntityUniqueKey euk = new EntityUniqueKey(
						concreteDescriptor.getRootEntityDescriptor().getEntityName(),
						//polymorphism comment above
						ukName,
						resolvedEntityState[index],
						type,
						session.getFactory()
				);
				session.getPersistenceContextInternal().addEntity( euk, toInitialize );
			}
		}
	}

	protected Object[] extractConcreteTypeStateValues(RowProcessingState rowProcessingState) {
		final Object[] values = new Object[concreteDescriptor.getNumberOfAttributeMappings()];
		final DomainResultAssembler<?>[] concreteAssemblers = assemblers[concreteDescriptor.getSubclassId()];
		for ( int i = 0; i < values.length; i++ ) {
			final DomainResultAssembler<?> assembler = concreteAssemblers[i];
			values[i] = assembler == null ? UNFETCHED_PROPERTY : assembler.assemble( rowProcessingState );
		}
		return values;
	}

	private void resolveState(RowProcessingState rowProcessingState) {
		for ( final DomainResultAssembler<?> assembler : assemblers[concreteDescriptor.getSubclassId()] ) {
			if ( assembler != null ) {
				assembler.resolveState( rowProcessingState );
			}
		}
	}

	protected boolean skipInitialization(Object toInitialize, RowProcessingState rowProcessingState) {
		if ( !isOwningInitializer ) {
			return true;
		}
		final EntityEntry entry =
				rowProcessingState.getSession().getPersistenceContextInternal().getEntry( toInitialize );
		if ( entry == null ) {
			return false;
		}
		// todo (6.0): do we really need this check ?
		else if ( entry.getStatus().isDeletedOrGone() ) {
			return true;
		}
		else {
			if ( isPersistentAttributeInterceptable( toInitialize ) ) {
				final PersistentAttributeInterceptor interceptor =
						asPersistentAttributeInterceptable( toInitialize ).$$_hibernate_getInterceptor();
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
							.getEffectiveOptionalObject() != toInitialize;
			}
			else {
				return false;
			}
		}
	}

	private boolean isReadOnly(RowProcessingState rowProcessingState, SharedSessionContractImplementor persistenceContext) {
		final Boolean readOnly = rowProcessingState.getQueryOptions().isReadOnly();
		return readOnly == null ? persistenceContext.isDefaultReadOnly() : readOnly;
	}

	protected void preLoad(RowProcessingState rowProcessingState) {
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		if ( session.isEventSource() ) {
			final PreLoadEvent preLoadEvent = rowProcessingState.getJdbcValuesSourceProcessingState().getPreLoadEvent();
			assert preLoadEvent != null;

			preLoadEvent.reset();

			preLoadEvent.setEntity( entityInstance )
					.setState( resolvedEntityState )
					.setId( entityKey.getIdentifier() )
					.setPersister( concreteDescriptor );

			session.getFactory()
					.getFastSessionServices()
					.eventListenerGroup_PRE_LOAD
					.fireEventOnEachListener( preLoadEvent, PreLoadEventListener::onPreLoad );
		}
	}

	@Override
	public boolean isPartOfKey() {
		return isPartOfKey;
	}

	@Override
	public boolean isEntityInitialized() {
		return state == State.INITIALIZED;
	}

	@Override
	public EntityPersister getConcreteDescriptor() {
		assert state != State.UNINITIALIZED;
		return concreteDescriptor == null ? entityDescriptor : concreteDescriptor;
	}

	protected void initializeSubInstancesFromParent(RowProcessingState rowProcessingState) {
		if ( entityInstanceForNotify != null ) {
			for ( DomainResultAssembler<?>[] subtypeAssemblers : getAssemblers() ) {
				for ( DomainResultAssembler<?> assembler : subtypeAssemblers ) {
					if ( assembler != null ) {
						final Initializer initializer = assembler.getInitializer();
						if ( initializer != null ) {
							initializer.initializeInstanceFromParent( entityInstanceForNotify, rowProcessingState );
						}
					}
				}
			}
		}
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		if ( resolvedEntityState != null ) {
			updateCaches(
					entityInstanceForNotify,
					rowProcessingState,
					session,
					session.getPersistenceContext(),
					entityKey.getIdentifier(),
					version
			);
		}

		// reset row state
		isOwningInitializer = false;
		concreteDescriptor = null;
		entityKey = null;
		version = null;
		entityInstance = null;
		entityInstanceForNotify = null;
		resolvedEntityState = null;
		state = State.UNINITIALIZED;
		clearResolutionListeners();
	}

	@Override
	public void endLoading(ExecutionContext executionContext) {
		super.endLoading( executionContext );
		shallowCached = false;
	}

	protected enum State {
		UNINITIALIZED,
		MISSING,
		KEY_RESOLVED,
		RESOLVED,
		INITIALIZED
	}

	//#########################
	// For Hibernate Reactive
	//#########################

	protected void setEntityInstanceForNotify(Object entityInstanceForNotify) {
		this.entityInstanceForNotify = entityInstanceForNotify;
	}

	protected Object getEntityInstanceForNotify() {
		return entityInstanceForNotify;
	}

	protected void setResolvedEntityState(Object[] resolvedEntityState) {
		this.resolvedEntityState = resolvedEntityState;
	}

	protected Object[] getResolvedEntityState() {
		return resolvedEntityState;
	}

	protected DomainResultAssembler<?> getVersionAssembler() {
		return versionAssembler;
	}

	protected DomainResultAssembler<Object> getRowIdAssembler() {
		return rowIdAssembler;
	}

	protected LockMode getLockMode() {
		return lockMode;
	}

	protected DomainResultAssembler<?>[][] getAssemblers() {
		return assemblers;
	}
}
