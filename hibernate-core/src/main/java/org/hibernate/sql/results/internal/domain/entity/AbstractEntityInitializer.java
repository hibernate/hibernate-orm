/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
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
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.mapping.StateArrayContributorMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.internal.domain.AbstractFetchParentAccess;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.EntityInitializer;
import org.hibernate.sql.results.spi.EntityResultNode;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.LoadingEntityEntry;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.type.TypeHelper;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEntityInitializer extends AbstractFetchParentAccess implements EntityInitializer {

	// NOTE : even though we only keep the EntityDescriptor here, rather than EntityReference
	//		the "scope" of this initializer is a specific EntityReference.
	//
	//		The full EntityReference is simply not needed here, and so we just keep
	//		the EntityDescriptor here to avoid chicken/egg issues in the creation of
	// 		these

	private final EntityPersister entityDescriptor;
	private final NavigablePath navigablePath;
	private final LockMode lockMode;

	private final List<Initializer> identifierInitializers = new ArrayList<>();

	private final DomainResultAssembler identifierAssembler;
	private final DomainResultAssembler discriminatorAssembler;
	private final DomainResultAssembler versionAssembler;

	private final Map<StateArrayContributorMapping, DomainResultAssembler> assemblerMap = new HashMap<>();

	// per-row state
	private EntityPersister concreteDescriptor;
	private EntityKey entityKey;
	private Object entityInstance;
	private boolean missing;
	private Object[] resolvedEntityState;

	// todo (6.0) : ^^ need a better way to track whether we are loading the entity state or if something else is/has

	@SuppressWarnings("WeakerAccess")
	protected AbstractEntityInitializer(
			EntityResultNode resultDescriptor,
			NavigablePath navigablePath,
			LockMode lockMode,
			DomainResult<?> identifierResult,
			DomainResult<?> discriminatorResult,
			DomainResult<?> versionResult,
			Consumer<Initializer> initializerConsumer,
			AssemblerCreationState creationState) {
		super( );
		this.entityDescriptor = (EntityPersister) resultDescriptor.getEntityValuedModelPart().getEntityMappingType();
		this.navigablePath = navigablePath;
		this.lockMode = lockMode;

		this.identifierAssembler = identifierResult.createResultAssembler(
				identifierInitializers::add,
				creationState
		);

		if ( discriminatorResult != null ) {
			discriminatorAssembler = discriminatorResult.createResultAssembler(
					initializer -> {
						throw new UnsupportedOperationException(
								"Registering an Initializer as part of Entity discriminator is illegal" );
					},
					creationState
			);
		}
		else {
			discriminatorAssembler = null;
		}

		if ( versionResult != null ) {
			this.versionAssembler = versionResult.createResultAssembler(
					initializer -> {
						throw new UnsupportedOperationException(
								"Registering an Initializer as part of Entity version is illegal" );
					},
					creationState
			);
		}
		else {
			this.versionAssembler = null;
		}

		entityDescriptor.visitStateArrayContributors(
				attributeMapping -> {
					// todo (6.0) : somehow we need to track whether all state is loaded/resolved
					//		note that lazy proxies or uninitialized collections count against
					//		that in the affirmative

					final Fetch fetch = resultDescriptor.findFetch( attributeMapping.getAttributeName() );

					final DomainResultAssembler stateAssembler;
					if ( fetch == null ) {
						stateAssembler = new NullValueAssembler( attributeMapping.getMappedTypeDescriptor().getMappedJavaTypeDescriptor() );
					}
					else {
						stateAssembler = fetch.createAssembler(
								this,
								initializerConsumer,
								creationState
						);
					}

					assemblerMap.put( attributeMapping, stateAssembler );
				}
		);

		initializerConsumer.accept( this );
	}

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
		return entityKey.getIdentifier();
	}

	@Override
	public Object getFetchParentInstance() {
		if ( entityInstance == null ) {
			throw new IllegalStateException( "Unexpected state condition - entity instance not yet resolved" );
		}

		return entityInstance;
	}

	// todo (6.0) : how to best handle possibility of null association?

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		// todo (6.0) : atm we do not handle sequential selects
		// 		- see AbstractEntityPersister#hasSequentialSelect and
		//			AbstractEntityPersister#getSequentialSelect in 5.2

		if ( entityInstance != null ) {
			return;
		}

		if ( EntityLoadingLogger.TRACE_ENABLED ) {
			EntityLoadingLogger.INSTANCE.tracef(
					"(%s) Beginning Initializer#resolveKey process for entity : %s",
					StringHelper.collapse( this.getClass().getName() ),
					getNavigablePath().getFullPath()
			);
		}

		final SharedSessionContractImplementor session = rowProcessingState.getJdbcValuesSourceProcessingState().getSession();
		concreteDescriptor = determineConcreteEntityDescriptor( rowProcessingState, session );

		initializeIdentifier( rowProcessingState );
		resolveEntityKey( rowProcessingState );

		if ( entityKey == null ) {
			EntityLoadingLogger.INSTANCE.debugf(
					"(%s) EntityKey (%s) is null",
					StringHelper.collapse( this.getClass().getName() ),
					getNavigablePath()
			);

			assert missing;

			return;
		}

		if ( EntityLoadingLogger.DEBUG_ENABLED ) {
			EntityLoadingLogger.INSTANCE.debugf(
					"(%s) Hydrated EntityKey (%s): %s",
					StringHelper.collapse( this.getClass().getName() ),
					getNavigablePath(),
					entityKey.getIdentifier()
			);
		}
	}

	private EntityPersister determineConcreteEntityDescriptor(
			RowProcessingState rowProcessingState,
			SharedSessionContractImplementor persistenceContext) throws WrongClassException {
		if ( discriminatorAssembler == null ) {
			return entityDescriptor;
		}

		throw new NotYetImplementedFor6Exception( getClass() );
//		final Object discriminatorValue = discriminatorAssembler.assemble(
//				rowProcessingState,
//				rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions()
//		);
//
//		final String result = entityDescriptor.getDiscriminatorDescriptor()
//				.getDiscriminatorMappings()
//				.discriminatorValueToEntityName( discriminatorValue );
//
//		if ( result == null ) {
//			// oops - we got an instance of another class hierarchy branch
//			throw new WrongClassException(
//					"Discriminator: " + discriminatorValue,
//					entityKey.getIdentifier(),
//					entityDescriptor.getEntityName()
//			);
//		}
//
//		return persistenceContext.getFactory().getMetamodel().findEntityDescriptor( result );
	}

	@SuppressWarnings("WeakerAccess")
	protected void initializeIdentifier(RowProcessingState rowProcessingState) {
		identifierInitializers.forEach( initializer -> initializer.resolveKey( rowProcessingState ) );
		identifierInitializers.forEach( initializer -> initializer.resolveInstance( rowProcessingState ) );
		identifierInitializers.forEach( initializer -> initializer.initializeInstance( rowProcessingState ) );
	}

	@SuppressWarnings("WeakerAccess")
	protected void resolveEntityKey(RowProcessingState rowProcessingState) {
		if ( entityKey != null ) {
			// its already been resolved
			return;
		}

		final SharedSessionContractImplementor session = rowProcessingState.getJdbcValuesSourceProcessingState().getSession();

		//		1) resolve the hydrated identifier value(s) into its identifier representation
		final Object id  = identifierAssembler.assemble( rowProcessingState, rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions() );

		if ( id == null ) {
			missing = true;
			// EARLY EXIT!!!
			return;
		}

		//		2) build the EntityKey
		this.entityKey = new EntityKey( id, concreteDescriptor );

		//		3) schedule the EntityKey for batch loading, if possible
		if ( concreteDescriptor.isBatchLoadable() ) {
			if ( !session.getPersistenceContext().containsEntity( entityKey ) ) {
				session.getPersistenceContext().getBatchFetchQueue().addBatchLoadableEntityKey( entityKey );
			}
		}

		// todo (6.0) : subselect fetches similar to batch fetch handling above
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		if ( missing ) {
			return;
		}

		final Object entityIdentifier = entityKey.getIdentifier();

		if ( EntityLoadingLogger.TRACE_ENABLED ) {
			EntityLoadingLogger.INSTANCE.tracef(
					"(%s) Beginning Initializer#resolveInstance process for entity (%s) : %s",
					StringHelper.collapse( this.getClass().getName() ),
					getNavigablePath(),
					entityIdentifier
			);
		}

		final SharedSessionContractImplementor session = rowProcessingState.getJdbcValuesSourceProcessingState().getSession();

		// look to see if another initializer from a parent load context or an earlier
		// initializer is already loading the entity

		final LoadingEntityEntry existingLoadingEntry = session.getPersistenceContext()
				.getLoadContexts()
				.findLoadingEntityEntry( entityKey );

		if ( existingLoadingEntry != null ) {
			if ( EntityLoadingLogger.DEBUG_ENABLED ) {
				EntityLoadingLogger.INSTANCE.debugf(
						"(%s) Found existing loading entry [%s] - using loading instance",
						StringHelper.collapse( this.getClass().getName() ),
						toLoggableString( getNavigablePath(), entityIdentifier )
				);
			}

			this.entityInstance = existingLoadingEntry.getEntityInstance();

			if ( existingLoadingEntry.getEntityInitializer() != this ) {
				// the entity is already being loaded elsewhere
				if ( EntityLoadingLogger.DEBUG_ENABLED ) {
					EntityLoadingLogger.INSTANCE.debugf(
							"(%s) Entity [%s] being loaded by another initializer [%s] - skipping processing",
							StringHelper.collapse( this.getClass().getName() ),
							toLoggableString( getNavigablePath(), entityIdentifier ),
							existingLoadingEntry.getEntityInitializer()
					);
				}

				// EARLY EXIT!!!
				return;
			}
		}

		if ( entityInstance == null ) {
			// this isEntityReturn bit is just for entity loaders, not hql/criteria
			if ( isEntityReturn() ) {
				final Object requestedEntityId = rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions().getEffectiveOptionalId();
				if ( requestedEntityId != null && requestedEntityId.equals( entityKey.getIdentifier() ) ) {
					entityInstance = rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions().getEffectiveOptionalObject();
				}
			}
		}

		if ( entityInstance == null ) {
			// see if it is managed in the Session already
			final Object entity = session.getPersistenceContext().getEntity( entityKey );
			if ( entity != null ) {
				this.entityInstance = entity;
			}
		}

		if ( entityInstance == null ) {
			entityInstance = session.instantiate( concreteDescriptor.getEntityName(), entityKey.getIdentifier() );

			if ( EntityLoadingLogger.DEBUG_ENABLED ) {
				EntityLoadingLogger.INSTANCE.debugf(
						"Created new entity instance [%s] : %s",
						toLoggableString( getNavigablePath(), entityIdentifier ),
						entityInstance
				);
			}

			final LoadingEntityEntry loadingEntry = new LoadingEntityEntry(
					this,
					entityKey,
					concreteDescriptor,
					entityInstance
			);

			rowProcessingState.getJdbcValuesSourceProcessingState().registerLoadingEntity(
					entityKey,
					loadingEntry
			);

		}

		notifyParentResolutionListeners( entityInstance );

		preLoad( rowProcessingState );
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		if ( missing ) {
			return;
		}

		final Serializable entityIdentifier = entityKey.getIdentifier();

		if ( EntityLoadingLogger.TRACE_ENABLED ) {
			EntityLoadingLogger.INSTANCE.tracef(
					"Beginning Initializer#initializeInstance process for entity %s",
					toLoggableString( getNavigablePath(), entityIdentifier )
			);
		}

		final SharedSessionContractImplementor session = rowProcessingState.getJdbcValuesSourceProcessingState().getSession();
		PersistenceContext persistenceContext = session.getPersistenceContext();

		// todo (6.0): do we really need this check ?
		if ( persistenceContext.containsEntity( entityKey ) ) {
			Status status = persistenceContext.getEntry( persistenceContext.getEntity( entityKey ) )
					.getStatus();
			if ( status == Status.DELETED || status == Status.GONE ) {
				return;
			}
		}
		final Object rowId = null;
// todo (6.0) : rowId
//		final Object rowId;
//		if ( concreteDescriptor.getHierarchy().getRowIdDescriptor() != null ) {
//			rowId = ro sqlSelectionMappings.getRowIdSqlSelection().hydrateStateArray( rowProcessingState );
//
//			if ( rowId == null ) {
//				throw new HibernateException(
//						"Could not read entity row-id from JDBC : " + entityKey
//				);
//			}
//		}
//		else {
//			rowId = null;
//		}



		entityDescriptor.setIdentifier( entityInstance, entityIdentifier, session );

		resolvedEntityState = new Object[ assemblerMap.size() ];
		assemblerMap.forEach(
				(key, value) -> resolvedEntityState[ key.getStateArrayPosition() ] = value.assemble( rowProcessingState )
		);

		entityDescriptor.setPropertyValues( entityInstance, resolvedEntityState );

		persistenceContext.addEntity(
				entityKey,
				entityInstance
		);

		final Object version;
		if ( versionAssembler != null ) {
			version = versionAssembler.assemble( rowProcessingState );
		}
		else {
			version = null;
		}

		final EntityEntry entityEntry = persistenceContext.addEntry(
				entityInstance,
				Status.LOADING,
				resolvedEntityState,
				rowId,
				entityKey.getIdentifier(),
				version,
				lockMode,
				true,
				entityDescriptor,
				false
		);

		final SessionFactoryImplementor factory = session.getFactory();
		final EntityDataAccess cacheAccess = entityDescriptor.getCacheAccessStrategy();
		if ( cacheAccess != null && session.getCacheMode().isPutEnabled() ) {

			if ( EntityLoadingLogger.DEBUG_ENABLED ) {
				EntityLoadingLogger.INSTANCE.debugf(
						"Adding entityInstance to second-level cache: %s",
						toLoggableString( getNavigablePath(), entityIdentifier )
				);
			}

			final CacheEntry entry = entityDescriptor.buildCacheEntry( entityInstance, resolvedEntityState, version, session );
			final Object cacheKey = cacheAccess.generateCacheKey(
					entityIdentifier,
					entityDescriptor,
					factory,
					session.getTenantIdentifier()
			);

			// explicit handling of caching for rows just inserted and then somehow forced to be read
			// from the database *within the same transaction*.  usually this is done by
			// 		1) Session#refresh, or
			// 		2) Session#clear + some form of load
			//
			// we need to be careful not to clobber the lock here in the cache so that it can be rolled back if need be
			if ( persistenceContext.wasInsertedDuringTransaction( entityDescriptor, entityIdentifier ) ) {
				cacheAccess.update(
						session,
						cacheKey,
						entityDescriptor.getCacheEntryStructure().structure( entry ),
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
							entityDescriptor.getCacheEntryStructure().structure( entry ),
							version,
							//useMinimalPuts( session, entityEntry )
							false
					);

					if ( put && factory.getStatistics().isStatisticsEnabled() ) {
						factory.getStatistics().entityCachePut( entityDescriptor.getNavigableRole(), cacheAccess.getRegion().getName() );
					}
				}
				finally {
					eventListenerManager.cachePutEnd();
				}
			}
		}

		if ( entityDescriptor.getNaturalIdMapping() != null ) {
			persistenceContext.getNaturalIdHelper().cacheNaturalIdCrossReferenceFromLoad(
					entityDescriptor,
					entityIdentifier,
					persistenceContext
							.getNaturalIdHelper().extractNaturalIdValues( resolvedEntityState, entityDescriptor )
			);
		}

		boolean isReallyReadOnly = isReadOnly( rowProcessingState, session );
		if ( ! entityDescriptor.isMutable() ) {
			isReallyReadOnly = true;
		}
		else {
			final Object proxy = persistenceContext.getProxy( entityKey );
			if ( proxy != null ) {
				// there is already a proxy for this impl
				// only set the status to read-only if the proxy is read-only
				isReallyReadOnly = ( (HibernateProxy) proxy ).getHibernateLazyInitializer().isReadOnly();
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
					entityDescriptor,
					resolvedEntityState,
					resolvedEntityState,
					attributeMapping -> attributeMapping.getAttributeMetadataAccess().resolveAttributeMetadata( concreteDescriptor ).isUpdatable()
			);
			persistenceContext.setEntryStatus( entityEntry, Status.MANAGED );
		}

		entityDescriptor.afterInitialize( entityInstance, session );

		if ( EntityLoadingLogger.DEBUG_ENABLED ) {
			EntityLoadingLogger.INSTANCE.debugf(
					"Done materializing entityInstance : %s",
					toLoggableString( getNavigablePath(), entityIdentifier )
			);
		}

		if ( factory.getStatistics().isStatisticsEnabled() ) {
			factory.getStatistics().loadEntity( entityDescriptor.getEntityName() );
		}

		postLoad( rowProcessingState );
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
					.getServiceRegistry()
					.getService( EventListenerRegistry.class )
					.getEventListenerGroup( EventType.PRE_LOAD );
			for ( PreLoadEventListener listener : listenerGroup.listeners() ) {
				listener.onPreLoad( preLoadEvent );
			}
		}
	}

	private void postLoad(RowProcessingState rowProcessingState) {
		final SharedSessionContractImplementor session = rowProcessingState.getJdbcValuesSourceProcessingState().getSession();

		if ( session instanceof EventSource ) {
			final PostLoadEvent postLoadEvent = rowProcessingState.getJdbcValuesSourceProcessingState().getPostLoadEvent();
			assert postLoadEvent != null;

			postLoadEvent.reset();

			postLoadEvent.setEntity( entityInstance )
					.setId( entityKey.getIdentifier() )
					.setPersister( concreteDescriptor );

			final EventListenerGroup<PostLoadEventListener> listenerGroup = entityDescriptor.getFactory()
					.getServiceRegistry()
					.getService( EventListenerRegistry.class )
					.getEventListenerGroup( EventType.POST_LOAD );
			for ( PostLoadEventListener listener : listenerGroup.listeners() ) {
				listener.onPostLoad( postLoadEvent );
			}
		}
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		// reset row state
		concreteDescriptor = null;
		entityKey = null;
		entityInstance = null;
		missing = false;
		resolvedEntityState = null;

		clearParentResolutionListeners();
	}
}
