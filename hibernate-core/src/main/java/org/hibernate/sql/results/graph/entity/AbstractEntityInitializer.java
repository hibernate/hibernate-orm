/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.WrongClassException;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
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
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.proxy.map.MapProxy;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.AssociationType;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeHelper;

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

	private final DomainResultAssembler identifierAssembler;
	private final DomainResultAssembler discriminatorAssembler;
	private final DomainResultAssembler versionAssembler;
	private final DomainResultAssembler<Object> rowIdAssembler;

	private final Map<AttributeMapping, DomainResultAssembler> assemblerMap;

	// per-row state
	private EntityPersister concreteDescriptor;
	private EntityKey entityKey;
	private Object entityInstance;
	private Object entityInstanceForNotify;
	private boolean missing;
	boolean isInitialized;
	private boolean isOwningInitializer;
	private Object[] resolvedEntityState;

	// todo (6.0) : ^^ need a better way to track whether we are loading the entity state or if something else is/has

	@SuppressWarnings("WeakerAccess")
	protected AbstractEntityInitializer(
			EntityResultGraphNode resultDescriptor,
			NavigablePath navigablePath,
			LockMode lockMode,
			Fetch identifierFetch,
			Fetch discriminatorFetch,
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

		if ( identifierFetch != null ) {
			this.identifierAssembler = identifierFetch.createAssembler(
					this,
					creationState
			);
		}
		else {
			this.identifierAssembler = null;
		}

		if ( discriminatorFetch != null ) {
			discriminatorAssembler = discriminatorFetch.createAssembler( this, creationState );
		}
		else {
			discriminatorAssembler = null;
		}

		final EntityVersionMapping versionMapping = entityDescriptor.getVersionMapping();
		if ( versionMapping != null ) {
			final Fetch versionFetch = resultDescriptor.findFetch( versionMapping );
			// If there is a version mapping, there must be a fetch for it
			assert versionFetch != null;
			this.versionAssembler = versionFetch.createAssembler( this, creationState );
		}
		else {
			this.versionAssembler = null;
		}

		if ( rowIdResult != null ) {
			this.rowIdAssembler = rowIdResult.createResultAssembler(
					this,
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
	public void registerResolutionListener(Consumer<Object> listener) {
		if ( entityInstanceForNotify != null ) {
			listener.accept( entityInstanceForNotify );
			return;
		}
		super.registerResolutionListener( listener );
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

		final String concreteEntityName = entityDescriptor.getDiscriminatorMapping().getConcreteEntityNameForDiscriminatorValue( discriminatorValue );

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

		return identifierAssembler.assemble(
				rowProcessingState,
				jdbcValuesSourceProcessingState.getProcessingOptions()
		);
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
		final LoadingEntityEntry existingLoadingEntry = persistenceContext
				.getLoadContexts()
				.findLoadingEntityEntry( entityKey );
		setIsOwningInitializer(entityKey.getIdentifier(), existingLoadingEntry  );

		if ( proxy != null && ( proxy instanceof MapProxy
				|| entityDescriptor.getJavaTypeDescriptor().getJavaTypeClass().isInstance( proxy ) ) ) {
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
				entityInstance = resolveInstance(
						entityIdentifier,
						existingLoadingEntry,
						rowProcessingState,
						session
				);
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
			final BasicType<?> versionType = concreteDescriptor.getVersionType();
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

	private void setIsOwningInitializer(Object entityIdentifier,LoadingEntityEntry existingLoadingEntry) {
		if ( existingLoadingEntry != null ) {
			if ( EntityLoadingLogger.DEBUG_ENABLED ) {
				EntityLoadingLogger.LOGGER.debugf(
						"(%s) Found existing loading entry [%s] - using loading instance",
						getSimpleConcreteImplName(),
						toLoggableString( getNavigablePath(), entityIdentifier )
				);
			}
			if ( existingLoadingEntry.getEntityInitializer() == this ) {
				isOwningInitializer = true;
			}
		}
		else {
			isOwningInitializer = true;
		}
	}

	private Object resolveInstance(
			Object entityIdentifier,
			LoadingEntityEntry existingLoadingEntry,
			RowProcessingState rowProcessingState,
			SharedSessionContractImplementor session) {
		if ( !isOwningInitializer ) {
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
			return existingLoadingEntry.getEntityInstance();
		}

		assert existingLoadingEntry == null || existingLoadingEntry.getEntityInstance() == null;

		Object instance = null;

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
		if ( missing || isInitialized ) {
			return;
		}

		preLoad( rowProcessingState );

		final SharedSessionContractImplementor session = rowProcessingState.getJdbcValuesSourceProcessingState().getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		if ( entityInstance instanceof HibernateProxy ) {
			LazyInitializer hibernateLazyInitializer = ( (HibernateProxy) entityInstance ).getHibernateLazyInitializer();

			Object instance = persistenceContext.getEntity( entityKey );
			if ( instance == null ) {
				instance = resolveInstance(
						entityKey.getIdentifier(),
						persistenceContext.getLoadContexts().findLoadingEntityEntry( entityKey ),
						rowProcessingState,
						session
				);
				initializeEntity( instance, rowProcessingState, session, persistenceContext );
			}

			hibernateLazyInitializer.setImplementation( instance );
			entityInstanceForNotify = instance;
		}
		else {
			initializeEntity( entityInstance, rowProcessingState, session, persistenceContext );
			entityInstanceForNotify = entityInstance;
		}

		notifyResolutionListeners( entityInstanceForNotify );
		isInitialized = true;
	}

	private void initializeEntity(
			Object toInitialize,
			RowProcessingState rowProcessingState,
			SharedSessionContractImplementor session,
			PersistenceContext persistenceContext) {
		final EntityEntry entry = persistenceContext.getEntry( toInitialize );
		if ( entry != null ) {
			if ( skipInitialization( toInitialize, rowProcessingState, entry ) ) {
				return;
			}
		}

		final Object entity = persistenceContext.getEntity( entityKey );
		assert entity == null || entity == toInitialize;

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

		// Also register possible unique key entries
		for ( Type propertyType : concreteDescriptor.getPropertyTypes() ) {
			if ( propertyType instanceof AssociationType ) {
				final AssociationType associationType = (AssociationType) propertyType;
				final String ukName = associationType.getLHSPropertyName();
				if ( ukName != null ) {
					final int index = ( (UniqueKeyLoadable) concreteDescriptor ).getPropertyIndex( ukName );
					final Type type = concreteDescriptor.getPropertyTypes()[index];

					// polymorphism not really handled completely correctly,
					// perhaps...well, actually its ok, assuming that the
					// entity name used in the lookup is the same as the
					// the one used here, which it will be

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
		}

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

	private boolean skipInitialization(
			Object toInitialize,
			RowProcessingState rowProcessingState,
			EntityEntry entry) {
		if ( !isOwningInitializer ) {
			return true;
		}

		if ( toInitialize instanceof PersistentAttributeInterceptable ) {
			final PersistentAttributeInterceptor interceptor = ( (PersistentAttributeInterceptable) toInitialize ).$$_hibernate_getInterceptor();
			if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
				if ( entry.getStatus() != Status.LOADING ) {
					// Avoid loading the same entity proxy twice for the same result set: it could lead to errors,
					// because some code writes to its input (ID in hydrated state replaced by the loaded entity, in particular).
					return false;
				}
				return true;
			}
		}

		// If the instance to initialize is the main entity, we can't skip this
		// This can happen if we initialize an enhanced proxy
		if ( entry.getStatus() != Status.LOADING ) {
			final Object optionalEntityInstance = rowProcessingState.getJdbcValuesSourceProcessingState()
					.getProcessingOptions()
					.getEffectiveOptionalObject();
			// If the instance to initialize is the main entity, we can't skip this
			// This can happen if we initialize an enhanced proxy
			if ( !isEntityReturn() || toInitialize != optionalEntityInstance ) {
				return true;
			}
		}
		return false;
	}

	private boolean isReadOnly(
			RowProcessingState rowProcessingState,
			SharedSessionContractImplementor persistenceContext) {
		final Boolean readOnly = rowProcessingState.getQueryOptions().isReadOnly();
		return readOnly == null ? persistenceContext.isDefaultReadOnly() : readOnly;
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
		isOwningInitializer = false;
		concreteDescriptor = null;
		entityKey = null;
		entityInstance = null;
		entityInstanceForNotify = null;
		missing = false;
		resolvedEntityState = null;
		isInitialized = false;
		clearResolutionListeners();
	}
}
