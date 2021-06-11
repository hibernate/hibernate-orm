/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.WrongClassException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.entity.CacheEntityLoaderHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.proxy.map.MapProxy;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.TypeHelper;
import org.hibernate.type.VersionType;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;

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

	private final List<Initializer> identifierInitializers = new ArrayList<>();

	private final DomainResultAssembler identifierAssembler;
	private final DomainResultAssembler discriminatorAssembler;
	private final DomainResultAssembler versionAssembler;
	private final DomainResultAssembler<Object> rowIdAssembler;

	private final Map<AttributeMapping, DomainResultAssembler> assemblerMap;

	// per-row state
	private EntityPersister concreteDescriptor;
	private EntityKey entityKey;
	private Object entityInstance;
	private boolean missing;
	private Object[] resolvedEntityState;

	// todo (6.0) : ^^ need a better way to track whether we are loading the entity state or if something else is/has

	@SuppressWarnings("WeakerAccess")
	protected AbstractEntityInitializer(
			EntityResultGraphNode resultDescriptor,
			NavigablePath navigablePath,
			LockMode lockMode,
			DomainResult<?> identifierResult,
			DomainResult<?> discriminatorResult,
			DomainResult<?> versionResult,
			DomainResult<Object> rowIdResult,
			AssemblerCreationState creationState) {
		super();

		this.referencedModelPart = resultDescriptor.getEntityValuedModelPart();
		this.entityDescriptor = (EntityPersister) referencedModelPart.getEntityMappingType();

		final String rootEntityName = entityDescriptor.getRootEntityName();
		if ( rootEntityName == null || rootEntityName.equals( entityDescriptor.getEntityName() ) ) {
			this.rootEntityDescriptor = entityDescriptor;
		}
		else {
			this.rootEntityDescriptor = entityDescriptor.getRootEntityDescriptor().getEntityPersister();
		}

		this.navigablePath = navigablePath;
		this.lockMode = lockMode;
		assert lockMode != null;

		if ( identifierResult != null ) {
			this.identifierAssembler = identifierResult.createResultAssembler(
					new AssemblerCreationState() {
						@Override
						public LockMode determineEffectiveLockMode(String identificationVariable) {
							return creationState.determineEffectiveLockMode( identificationVariable );
						}

						@Override
						public Initializer resolveInitializer(
								NavigablePath navigablePath,
								ModelPart fetchedModelPart,
								Supplier<Initializer> producer) {
							for ( int i = 0; i < identifierInitializers.size(); i++ ) {
								final Initializer existing = identifierInitializers.get( i );
								if ( existing.getNavigablePath().equals( navigablePath )
										&& fetchedModelPart.getNavigableRole()
										.equals( existing.getInitializedPart().getNavigableRole() ) ) {
									assert fetchedModelPart == existing.getInitializedPart();
									return existing;
								}
							}

//							// also check the non-identifier initializers
//							final Initializer otherExisting = creationState.resolveInitializer(
//									navigablePath,
//									() -> null
//							);
//
//							if ( otherExisting != null ) {
//								identifierInitializers.add( otherExisting );
//								return otherExisting;
//							}

							final Initializer initializer = creationState.resolveInitializer( navigablePath, fetchedModelPart, producer );
							identifierInitializers.add( initializer );
							return initializer;
						}

						@Override
						public SqlAstCreationContext getSqlAstCreationContext() {
							return creationState.getSqlAstCreationContext();
						}
					}
			);
		}
		else {
			this.identifierAssembler = null;
		}

		if ( discriminatorResult != null ) {
			discriminatorAssembler = discriminatorResult.createResultAssembler( creationState );
		}
		else {
			discriminatorAssembler = null;
		}

		if ( versionResult != null ) {
			this.versionAssembler = versionResult.createResultAssembler( creationState );
		}
		else {
			this.versionAssembler = null;
		}

		if ( rowIdResult != null ) {
			this.rowIdAssembler = rowIdResult.createResultAssembler(
					creationState
			);
		}
		else {
			this.rowIdAssembler = null;
		}

		assemblerMap = new IdentityHashMap<>( entityDescriptor.getNumberOfAttributeMappings() );

		entityDescriptor.visitFetchables(
				fetchable -> {
					final AttributeMapping attributeMapping = (AttributeMapping) fetchable;

					// todo (6.0) : somehow we need to track whether all state is loaded/resolved
					//		note that lazy proxies or uninitialized collections count against
					//		that in the affirmative

					final Fetch fetch = resultDescriptor.findFetch( fetchable );

					final DomainResultAssembler<?> stateAssembler;
					if ( fetch == null ) {
						stateAssembler = new NullValueAssembler<>(
								attributeMapping.getMappedType().getMappedJavaTypeDescriptor()
						);
					}
					else {
						stateAssembler = fetch.createAssembler( this, creationState );
					}

					assemblerMap.put( attributeMapping, stateAssembler );
				},
				null
		);
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

	protected abstract boolean isEntityReturn();

	@Override
	public EntityPersister getEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public Object getEntityInstance() {
		return entityInstance;
	}

	@SuppressWarnings("unused")
	public Object getKeyValue() {
		if ( entityKey == null ) {
			return null;
		}
		return entityKey.getIdentifier();
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
	public Object getFetchParentInstance() {
		if ( entityInstance == null ) {
			throw new IllegalStateException( "Unexpected state condition - entity instance not yet resolved" );
		}

		return getEntityInstance();
	}

	// todo (6.0) : how to best handle possibility of null association?

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		// todo (6.0) : atm we do not handle sequential selects
		// 		- see AbstractEntityPersister#hasSequentialSelect and
		//			AbstractEntityPersister#getSequentialSelect in 5.2

		if ( entityKey != null ) {
			return;
		}

		if ( EntityLoadingLogger.TRACE_ENABLED ) {
			EntityLoadingLogger.LOGGER.tracef(
					"(%s) Beginning Initializer#resolveKey process for entity : %s",
					StringHelper.collapse( this.getClass().getName() ),
					getNavigablePath().getFullPath()
			);
		}

		final SharedSessionContractImplementor session = rowProcessingState.getJdbcValuesSourceProcessingState().getSession();
		concreteDescriptor = determineConcreteEntityDescriptor( rowProcessingState, session );
		if ( concreteDescriptor == null ) {
			missing = true;
			return;
		}

		resolveEntityKey( rowProcessingState );

		if ( entityKey == null ) {
			EntityLoadingLogger.LOGGER.debugf(
					"(%s) EntityKey (%s) is null",
					getSimpleConcreteImplName(),
					getNavigablePath()
			);

			assert missing;

			return;
		}

		if ( EntityLoadingLogger.DEBUG_ENABLED ) {
			EntityLoadingLogger.LOGGER.debugf(
					"(%s) Hydrated EntityKey (%s): %s",
					getSimpleConcreteImplName(),
					getNavigablePath(),
					entityKey.getIdentifier()
			);
		}
	}

	private EntityPersister determineConcreteEntityDescriptor(
			RowProcessingState rowProcessingState,
			SharedSessionContractImplementor session) throws WrongClassException {
		if ( discriminatorAssembler == null ) {
			return entityDescriptor;
		}

		final Object discriminatorValue = discriminatorAssembler.assemble(
				rowProcessingState,
				rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions()
		);

		final String concreteEntityName = ( (Loadable) entityDescriptor.getRootEntityDescriptor() ).getSubclassForDiscriminatorValue( discriminatorValue );
		if ( concreteEntityName == null ) {
			return entityDescriptor;
		}

		final EntityPersister concreteType = session.getFactory().getMetamodel().findEntityDescriptor( concreteEntityName );

		if ( concreteType == null || !concreteType.isTypeOrSuperType( entityDescriptor ) ) {
			throw new WrongClassException(
					concreteEntityName,
					null,
					entityDescriptor.getEntityName(),
					discriminatorValue
			);
		}

		// verify that the `entityDescriptor` is either == concreteType or its super-type
		assert concreteType.isTypeOrSuperType( entityDescriptor );

		return concreteType;
	}

	@SuppressWarnings("WeakerAccess")
	protected void resolveEntityKey(RowProcessingState rowProcessingState) {
		if ( entityKey != null ) {
			// its already been resolved
			return;
		}

		final JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState = rowProcessingState.getJdbcValuesSourceProcessingState();
		final SharedSessionContractImplementor session = jdbcValuesSourceProcessingState.getSession();

		//		1) resolve the hydrated identifier value(s) into its identifier representation
		final Object id = initializeIdentifier( rowProcessingState, jdbcValuesSourceProcessingState );

		if ( id == null ) {
			missing = true;
			// EARLY EXIT!!!
			return;
		}

		//		2) build the EntityKey
		this.entityKey = new EntityKey( id, concreteDescriptor );

		if ( jdbcValuesSourceProcessingState.findInitializer( entityKey ) == null ) {
			jdbcValuesSourceProcessingState.registerInitilaizer( entityKey, this );
		}

		//		3) schedule the EntityKey for batch loading, if possible
		if ( concreteDescriptor.isBatchLoadable() ) {
			if ( !session.getPersistenceContext().containsEntity( entityKey ) ) {
				session.getPersistenceContext().getBatchFetchQueue().addBatchLoadableEntityKey( entityKey );
			}
		}
	}

	private Object initializeIdentifier(
			RowProcessingState rowProcessingState,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState) {
		final Object id = jdbcValuesSourceProcessingState.getProcessingOptions().getEffectiveOptionalId();
		final boolean useEmbeddedIdentifierInstanceAsEntity = id != null && id.getClass()
				.equals( concreteDescriptor.getJavaTypeDescriptor().getJavaType() );
		if ( useEmbeddedIdentifierInstanceAsEntity ) {
			entityInstance = id;
			return id;
		}

		if ( identifierAssembler == null ) {
			return id;
		}

		initializeIdentifier( rowProcessingState );
		return identifierAssembler.assemble(
				rowProcessingState,
				jdbcValuesSourceProcessingState.getProcessingOptions()
		);
	}

	@SuppressWarnings("WeakerAccess")
	protected void initializeIdentifier(RowProcessingState rowProcessingState) {
		if ( EntityLoadingLogger.TRACE_ENABLED ) {
			EntityLoadingLogger.LOGGER.tracef(
					"(%s) Beginning Initializer#initializeIdentifier process for entity (%s) ",
					StringHelper.collapse( this.getClass().getName() ),
					getNavigablePath()
			);
		}

		identifierInitializers.forEach( initializer -> initializer.resolveKey( rowProcessingState ) );
		identifierInitializers.forEach( initializer -> initializer.resolveInstance( rowProcessingState ) );
		identifierInitializers.forEach( initializer -> initializer.initializeInstance( rowProcessingState ) );

		if ( EntityLoadingLogger.TRACE_ENABLED ) {
			EntityLoadingLogger.LOGGER.tracef(
					"(%s) Fiish Initializer#initializeIdentifier process for entity (%s) ",
					StringHelper.collapse( this.getClass().getName() ),
					getNavigablePath()
			);
		}
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		if ( missing ) {
			return;
		}

		if ( entityInstance != null ) {
			return;
		}

		final Object entityIdentifier = entityKey.getIdentifier();

		if ( EntityLoadingLogger.TRACE_ENABLED ) {
			EntityLoadingLogger.LOGGER.tracef(
					"(%s) Beginning Initializer#resolveInstance process for entity (%s) : %s",
					StringHelper.collapse( this.getClass().getName() ),
					getNavigablePath(),
					entityIdentifier
			);
		}

		final SharedSessionContractImplementor session = rowProcessingState
				.getJdbcValuesSourceProcessingState()
				.getSession();

		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final Object proxy = getProxy( persistenceContext );

		// Special case map proxy to avoid stack overflows
		// We know that a map proxy will always be of "the right type" so just use that object
		if ( proxy != null && ( proxy instanceof MapProxy || concreteDescriptor.isInstance( proxy ) ) ) {
			entityInstance = proxy;
		}
		else {
			final Object existingEntity = persistenceContext.getEntity( entityKey );

			if ( existingEntity != null ) {
				entityInstance = existingEntity;
			}
			else {
				// look to see if another initializer from a parent load context or an earlier
				// initializer is already loading the entity
				if ( entityInstance == null ) {
					entityInstance = resolveInstance(
							entityIdentifier,
							rowProcessingState,
							session,
							persistenceContext
					);

				}
			}

			if ( LockMode.NONE != lockMode ) {
				final EntityEntry entry = session.getPersistenceContextInternal().getEntry( entityInstance );
				if ( entry != null && entry.getLockMode().lessThan( lockMode ) ) {
					//we only check the version when _upgrading_ lock modes
					if ( versionAssembler != null ) {
						checkVersion( entry, rowProcessingState );
					}
					//we need to upgrade the lock mode to the mode requested
					entry.setLockMode( lockMode );
				}
			}
		}
		notifyParentResolutionListeners( entityInstance );

		preLoad( rowProcessingState );
	}

	/**
	 * Check the version of the object in the <tt>RowProcessingState</tt> against
	 * the object version in the session cache, throwing an exception
	 * if the version numbers are different
	 */
	private void checkVersion(EntityEntry entry, final RowProcessingState rowProcessingState) throws HibernateException {
		final Object version = entry.getVersion();

		if ( version != null ) {
			// null version means the object is in the process of being loaded somewhere else in the ResultSet
			final VersionType<?> versionType = concreteDescriptor.getVersionType();
			final Object currentVersion = versionAssembler.assemble( rowProcessingState );
			if ( !versionType.isEqual( version, currentVersion ) ) {
				final StatisticsImplementor statistics = rowProcessingState.getSession().getFactory().getStatistics();
				if ( statistics.isStatisticsEnabled() ) {
					statistics.optimisticFailure( concreteDescriptor.getEntityName() );
				}
				throw new StaleObjectStateException( concreteDescriptor.getEntityName(), entry.getId() );
			}
		}

	}

	protected Object getProxy(PersistenceContext persistenceContext) {
		return persistenceContext.getProxy( entityKey );
	}

	private Object resolveInstance(
			Object entityIdentifier,
			RowProcessingState rowProcessingState,
			SharedSessionContractImplementor session,
			PersistenceContext persistenceContext) {
		final LoadingEntityEntry existingLoadingEntry = persistenceContext
				.getLoadContexts()
				.findLoadingEntityEntry( entityKey );

		Object instance = null;
		if ( existingLoadingEntry != null ) {
			if ( EntityLoadingLogger.DEBUG_ENABLED ) {
				EntityLoadingLogger.LOGGER.debugf(
						"(%s) Found existing loading entry [%s] - using loading instance",
						getSimpleConcreteImplName(),
						toLoggableString( getNavigablePath(), entityIdentifier )
				);
			}

			instance = existingLoadingEntry.getEntityInstance();

			if ( existingLoadingEntry.getEntityInitializer() != this ) {
				// the entity is already being loaded elsewhere
				if ( EntityLoadingLogger.DEBUG_ENABLED ) {
					EntityLoadingLogger.LOGGER.debugf(
							"(%s) Entity [%s] being loaded by another initializer [%s] - skipping processing",
							getSimpleConcreteImplName(),
							toLoggableString( getNavigablePath(), entityIdentifier ),
							existingLoadingEntry.getEntityInitializer()
					);
				}

				// EARLY EXIT!!!
				return instance;
			}
		}

		if ( instance == null ) {
			// this isEntityReturn bit is just for entity loaders, not hql/criteria
			if ( isEntityReturn() ) {
				final Object requestedEntityId = rowProcessingState.getJdbcValuesSourceProcessingState()
						.getProcessingOptions()
						.getEffectiveOptionalId();
				final Object optionalEntityInstance = rowProcessingState.getJdbcValuesSourceProcessingState()
						.getProcessingOptions()
						.getEffectiveOptionalObject();
				if ( requestedEntityId != null && optionalEntityInstance != null && requestedEntityId.equals(
						entityKey.getIdentifier() ) ) {
					instance = optionalEntityInstance;
				}
			}
		}

		// We have to query the second level cache if reference cache entries are used
		if ( instance == null && entityDescriptor.canUseReferenceCacheEntries() ) {
			instance = CacheEntityLoaderHelper.INSTANCE.loadFromSecondLevelCache(
					(EventSource) rowProcessingState.getSession(),
					null,
					lockMode,
					entityDescriptor,
					entityKey
			);

			if ( instance != null ) {
				// EARLY EXIT!!!
				return instance;
			}
		}

		if ( instance == null ) {
			instance = session.instantiate(
					concreteDescriptor.getEntityName(),
					entityKey.getIdentifier()
			);

			if ( EntityLoadingLogger.DEBUG_ENABLED ) {
				EntityLoadingLogger.LOGGER.debugf(
						"(%s) Created new entity instance [%s] : %s",
						getSimpleConcreteImplName(),
						toLoggableString( getNavigablePath(), entityIdentifier ),
						instance
				);
			}
		}

		final LoadingEntityEntry loadingEntry = new LoadingEntityEntry(
				this,
				entityKey,
				concreteDescriptor,
				instance
		);
		rowProcessingState.getJdbcValuesSourceProcessingState().registerLoadingEntity(
				entityKey,
				loadingEntry
		);
		return instance;
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		if ( missing ) {
			return;
		}
		final SharedSessionContractImplementor session = rowProcessingState.getJdbcValuesSourceProcessingState().getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContext();

		if ( entityInstance instanceof HibernateProxy ) {
			LazyInitializer hibernateLazyInitializer = ( (HibernateProxy) entityInstance ).getHibernateLazyInitializer();
			if ( !hibernateLazyInitializer.isUninitialized() ) {
				return;
			}
			Object instance = resolveInstance(
					entityKey.getIdentifier(),
					rowProcessingState,
					session,
					persistenceContext
			);
			initializeEntity( instance, rowProcessingState, session, persistenceContext );
			hibernateLazyInitializer.setImplementation( instance );
		}
		else {
			initializeEntity( entityInstance, rowProcessingState, session, persistenceContext );
		}
	}

	private void initializeEntity(
			Object toInitialize,
			RowProcessingState rowProcessingState,
			SharedSessionContractImplementor session,
			PersistenceContext persistenceContext) {
		final EntityEntry entry = persistenceContext.getEntry( toInitialize );
		if ( entry != null ) {
			if ( entry.getStatus() != Status.LOADING ) {
				final Object optionalEntityInstance = rowProcessingState.getJdbcValuesSourceProcessingState()
						.getProcessingOptions()
						.getEffectiveOptionalObject();
				// If the instance to initialize is the main entity, we can't skip this
				// This can happen if we initialize an enhanced proxy
				if ( !isEntityReturn() || toInitialize != optionalEntityInstance ) {
					return;
				}
			}
		}

		final Object entity = persistenceContext.getEntity( entityKey );
		assert entity == null || entity == toInitialize;
//
//		if ( entity != null ) {
//			return;
//		}

		final Object entityIdentifier = entityKey.getIdentifier();

		if ( EntityLoadingLogger.TRACE_ENABLED ) {
			EntityLoadingLogger.LOGGER.tracef(
					"(%s) Beginning Initializer#initializeInstance process for entity %s",
					getSimpleConcreteImplName(),
					toLoggableString( getNavigablePath(), entityIdentifier )
			);
		}

		// todo (6.0): do we really need this check ?
		if ( persistenceContext.containsEntity( entityKey ) ) {
			Status status = persistenceContext.getEntry( entity )
					.getStatus();
			if ( status == Status.DELETED || status == Status.GONE ) {
				return;
			}
		}

		entityDescriptor.setIdentifier( toInitialize, entityIdentifier, session );

		resolvedEntityState = concreteDescriptor.extractConcreteTypeStateValues(
				assemblerMap,
				rowProcessingState
		);

		concreteDescriptor.setPropertyValues( toInitialize, resolvedEntityState );

		persistenceContext.addEntity( entityKey, toInitialize );

		final Object version;

		if ( versionAssembler != null ) {
			version = versionAssembler.assemble( rowProcessingState );
		}
		else {
			version = null;
		}

		final Object rowId;

		if ( rowIdAssembler != null ) {
			rowId = rowIdAssembler.assemble( rowProcessingState );
		}
		else {
			rowId = null;
		}
		// from the perspective of Hibernate, an entity is read locked as soon as it is read
		// so regardless of the requested lock mode, we upgrade to at least the read level
		final LockMode lockModeToAcquire = lockMode == LockMode.NONE
				? LockMode.READ
				: lockMode;

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

		final SessionFactoryImplementor factory = session.getFactory();
		final EntityDataAccess cacheAccess = concreteDescriptor.getCacheAccessStrategy();
		final StatisticsImplementor statistics = factory.getStatistics();
		// No need to put into the entity cache is this is coming from the query cache already
		if ( !rowProcessingState.isQueryCacheHit() && cacheAccess != null && session.getCacheMode().isPutEnabled() ) {

			if ( EntityLoadingLogger.DEBUG_ENABLED ) {
				EntityLoadingLogger.LOGGER.debugf(
						"(%S) Adding entityInstance to second-level cache: %s",
						getSimpleConcreteImplName(),
						toLoggableString( getNavigablePath(), entityIdentifier )
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
			if ( persistenceContext.wasInsertedDuringTransaction( concreteDescriptor, entityIdentifier ) ) {
				cacheAccess.update(
						session,
						cacheKey,
						rootEntityDescriptor.getCacheEntryStructure().structure( cacheEntry ),
						version,
						version
				);
			}
			else {
				final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
				try {
					eventListenerManager.cachePutStart();
					final boolean put = cacheAccess.putFromLoad(
							session,
							cacheKey,
							rootEntityDescriptor.getCacheEntryStructure().structure( cacheEntry ),
							version,
							//useMinimalPuts( session, entityEntry )
							false
					);

					if ( put && statistics.isStatisticsEnabled() ) {
						statistics.entityCachePut( rootEntityDescriptor.getNavigableRole(), cacheAccess.getRegion().getName() );
					}
				}
				finally {
					eventListenerManager.cachePutEnd();
				}
			}
		}

		if ( entityDescriptor.getNaturalIdMapping() != null ) {
			persistenceContext.getNaturalIdResolutions().cacheResolutionFromLoad(
					entityIdentifier,
					entityDescriptor.getNaturalIdMapping().extractNaturalIdFromEntityState( resolvedEntityState, session ),
					entityDescriptor
			);
		}

		boolean isReallyReadOnly = isReadOnly( rowProcessingState, session );
		if ( ! concreteDescriptor.isMutable() ) {
			isReallyReadOnly = true;
		}
		else {
			if ( entityInstance instanceof HibernateProxy) {
				// there is already a proxy for this impl
				// only set the status to read-only if the proxy is read-only
				isReallyReadOnly = 	( (HibernateProxy) entityInstance ).getHibernateLazyInitializer().isReadOnly();
			}
		}
		if ( isReallyReadOnly ) {
			//no need to take a snapshot - this is a
			//performance optimization, but not really
			//important, except for entities with huge
			//mutable property values
			persistenceContext.setEntryStatus( entityEntry, Status.READ_ONLY );
		}
		else {
			//take a snapshot
			TypeHelper.deepCopy(
					concreteDescriptor,
					resolvedEntityState,
					resolvedEntityState,
					attributeMapping -> attributeMapping.getAttributeMetadataAccess().resolveAttributeMetadata( concreteDescriptor ).isUpdatable()
			);
			persistenceContext.setEntryStatus( entityEntry, Status.MANAGED );
		}

		concreteDescriptor.afterInitialize( toInitialize, session );

		if ( EntityLoadingLogger.DEBUG_ENABLED ) {
			EntityLoadingLogger.LOGGER.debugf(
					"(%s) Done materializing entityInstance : %s",
					getSimpleConcreteImplName(),
					toLoggableString( getNavigablePath(), entityIdentifier )
			);
		}
		if ( statistics.isStatisticsEnabled() ) {
			statistics.loadEntity( concreteDescriptor.getEntityName() );
		}
	}

	private boolean isReadOnly(
			RowProcessingState rowProcessingState,
			SharedSessionContractImplementor persistenceContext) {
		if ( persistenceContext.isDefaultReadOnly() ) {
			return true;
		}


		final Boolean queryOption = rowProcessingState.getJdbcValuesSourceProcessingState().getQueryOptions().isReadOnly();

		return queryOption == null ? false : queryOption;
	}

	private void preLoad(RowProcessingState rowProcessingState) {
		final SharedSessionContractImplementor session = rowProcessingState.getJdbcValuesSourceProcessingState().getSession();

		if ( session instanceof EventSource ) {
			final PreLoadEvent preLoadEvent = rowProcessingState.getJdbcValuesSourceProcessingState().getPreLoadEvent();
			assert preLoadEvent != null;

			preLoadEvent.reset();

			preLoadEvent.setEntity( entityInstance )
					.setId( entityKey.getIdentifier() )
					.setPersister( concreteDescriptor );

			final EventListenerGroup<PreLoadEventListener> listenerGroup = session.getFactory()
					.getFastSessionServices()
					.eventListenerGroup_PRE_LOAD;
			for ( PreLoadEventListener listener : listenerGroup.listeners() ) {
				listener.onPreLoad( preLoadEvent );
			}
		}
	}

	@Override
	public EntityPersister getConcreteDescriptor() {
		return concreteDescriptor;
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		// reset row state
		concreteDescriptor = null;
		entityKey = null;
		entityInstance = null;
		missing = false;
		resolvedEntityState = null;
		identifierInitializers.forEach( initializer -> initializer.finishUpRow( rowProcessingState ) );
		clearParentResolutionListeners();
	}
}
