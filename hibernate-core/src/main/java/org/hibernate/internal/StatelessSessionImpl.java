/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.SessionException;
import org.hibernate.StatelessSession;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.StatefulPersistenceContext;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.event.spi.EventSource;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.tuple.entity.EntityMetamodel;

import jakarta.transaction.SystemException;

import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isHibernateProxy;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;

/**
 * Concrete implementation of the {@link StatelessSession} API.
 * <p/>
 * Exposes two interfaces:<ul>
 * <li>{@link StatelessSession} to the application</li>
 * <li>{@link org.hibernate.engine.spi.SharedSessionContractImplementor} to other Hibernate components (SPI)</li>
 * </ul>
 * <p/>
 * This class is not thread-safe.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class StatelessSessionImpl extends AbstractSharedSessionContract implements StatelessSession {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( StatelessSessionImpl.class );

	private static final LoadQueryInfluencers NO_INFLUENCERS = new LoadQueryInfluencers( null ) {
		@Override
		public String getInternalFetchProfile() {
			return null;
		}

		@Override
		public void setInternalFetchProfile(String internalFetchProfile) {
		}
	};

	private final PersistenceContext temporaryPersistenceContext = new StatefulPersistenceContext( this );

	private final boolean connectionProvided;

	public StatelessSessionImpl(SessionFactoryImpl factory, SessionCreationOptions options) {
		super( factory, options );
		connectionProvided = options.getConnection() != null;
	}

	@Override
	public boolean shouldAutoJoinTransaction() {
		return true;
	}

	// inserts ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Object insert(Object entity) {
		checkOpen();
		return insert( null, entity );
	}

	@Override
	public Object insert(String entityName, Object entity) {
		checkOpen();
		EntityPersister persister = getEntityPersister( entityName, entity );
		Object id = persister.getIdentifierGenerator().generate( this, entity );
		Object[] state = persister.getValues( entity );
		if ( persister.isVersioned() ) {
			boolean substitute = Versioning.seedVersion(
					state,
					persister.getVersionProperty(),
					persister.getVersionMapping(),
					this
			);
			if ( substitute ) {
				persister.setValues( entity, state );
			}
		}
		if ( id == IdentifierGeneratorHelper.POST_INSERT_INDICATOR ) {
			id = persister.insert( state, entity, this );
		}
		else {
			persister.insert( id, state, entity, this );
		}
		persister.setIdentifier( entity, id, this );
		return id;
	}


	// deletes ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void delete(Object entity) {
		checkOpen();
		delete( null, entity );
	}

	@Override
	public void delete(String entityName, Object entity) {
		checkOpen();
		EntityPersister persister = getEntityPersister( entityName, entity );
		Object id = persister.getIdentifier( entity, this );
		Object version = persister.getVersion( entity );
		persister.delete( id, version, entity, this );
	}


	// updates ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void update(Object entity) {
		checkOpen();
		update( null, entity );
	}

	@Override
	public void update(String entityName, Object entity) {
		checkOpen();
		EntityPersister persister = getEntityPersister( entityName, entity );
		Object id = persister.getIdentifier( entity, this );
		Object[] state = persister.getValues( entity );
		Object oldVersion;
		if ( persister.isVersioned() ) {
			oldVersion = persister.getVersion( entity );
			Object newVersion = Versioning.increment( oldVersion, persister.getVersionMapping(), this );
			Versioning.setVersion( state, newVersion, persister );
			persister.setValues( entity, state );
		}
		else {
			oldVersion = null;
		}
		persister.update( id, state, null, false, null, oldVersion, entity, null, this );
	}


	// loading ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override @SuppressWarnings("unchecked")
	public <T> T get(Class<T> entityClass, Object id) {
		return (T) get( entityClass.getName(), id );
	}

	@Override @SuppressWarnings("unchecked")
	public <T> T get(Class<T> entityClass, Object id, LockMode lockMode) {
		return (T) get( entityClass.getName(), id, lockMode );
	}

	@Override
	public Object get(String entityName, Object id) {
		return get( entityName, id, LockMode.NONE );
	}

	@Override
	public Object get(String entityName, Object id, LockMode lockMode) {
		checkOpen();

		final EntityPersister entityDescriptor = getFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( entityName );
		final Object result = entityDescriptor.load( id, null, getNullSafeLockMode( lockMode ), this );

		if ( temporaryPersistenceContext.isLoadFinished() ) {
			temporaryPersistenceContext.clear();
		}
		return result;
	}

	@Override
	public void refresh(Object entity) {
		refresh( bestGuessEntityName( entity ), entity, LockMode.NONE );
	}

	@Override
	public void refresh(String entityName, Object entity) {
		refresh( entityName, entity, LockMode.NONE );
	}

	@Override
	public void refresh(Object entity, LockMode lockMode) {
		refresh( bestGuessEntityName( entity ), entity, lockMode );
	}

	@Override
	public void refresh(String entityName, Object entity, LockMode lockMode) {
		final EntityPersister persister = this.getEntityPersister( entityName, entity );
		final Object id = persister.getIdentifier( entity, this );
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Refreshing transient {0}", MessageHelper.infoString( persister, id, this.getFactory() ) );
		}
		// TODO : can this ever happen???
//		EntityKey key = new EntityKey( id, persister, source.getEntityMode() );
//		if ( source.getPersistenceContext().getEntry( key ) != null ) {
//			throw new PersistentObjectException(
//					"attempted to refresh transient instance when persistent " +
//					"instance was already associated with the Session: " +
//					MessageHelper.infoString( persister, id, source.getFactory() )
//			);
//		}

		if ( persister.canWriteToCache() ) {
			final EntityDataAccess cacheAccess = persister.getCacheAccessStrategy();
			if ( cacheAccess != null ) {
				final Object ck = cacheAccess.generateCacheKey(
						id,
						persister,
						getFactory(),
						getTenantIdentifier()
				);
				cacheAccess.evict( ck );
			}
		}

		String previousFetchProfile = getLoadQueryInfluencers().getInternalFetchProfile();
		Object result;
		try {
			getLoadQueryInfluencers().setInternalFetchProfile( "refresh" );
			result = persister.load( id, entity, getNullSafeLockMode( lockMode ), this );
		}
		finally {
			getLoadQueryInfluencers().setInternalFetchProfile( previousFetchProfile );
		}
		UnresolvableObjectException.throwIfNull( result, id, persister.getEntityName() );
		if ( temporaryPersistenceContext.isLoadFinished() ) {
			temporaryPersistenceContext.clear();
		}
	}

	@Override
	public Object immediateLoad(String entityName, Object id) throws HibernateException {
		if ( getPersistenceContextInternal().isLoadFinished() ) {
			throw new SessionException( "proxies cannot be fetched by a stateless session" );
		}
		// unless we are still in the process of handling a top-level load
		return get( entityName, id );
	}

	@Override
	public void initializeCollection(
			PersistentCollection<?> collection,
			boolean writing) throws HibernateException {
		throw new SessionException( "collections cannot be fetched by a stateless session" );
	}

	@Override
	public Object instantiate(
			String entityName,
			Object id) throws HibernateException {
		final EntityPersister entityDescriptor = getFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( entityName );
		return instantiate( entityDescriptor, id );
	}

	@Override
	public Object instantiate(EntityPersister persister, Object id) throws HibernateException {
		checkOpen();
		return persister.instantiate( id, this );
	}

	@Override
	public Object internalLoad(
			String entityName,
			Object id,
			boolean eager,
			boolean nullable) throws HibernateException {
		checkOpen();

		final EntityPersister entityDescriptor = getFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( entityName );
		final EntityKey entityKey = generateEntityKey( id, entityDescriptor );

		// first, try to load it from the temp PC associated to this SS
		final PersistenceContext persistenceContext = getPersistenceContext();
		Object loaded = persistenceContext.getEntity( entityKey );
		if ( loaded != null ) {
			// we found it in the temp PC.  Should indicate we are in the midst of processing a result set
			// containing eager fetches via join fetch
			return loaded;
		}

		if ( !eager ) {
			// caller did not request forceful eager loading, see if we can create
			// some form of proxy

			// first, check to see if we can use "bytecode proxies"

			final EntityMetamodel entityMetamodel = entityDescriptor.getEntityMetamodel();
			final BytecodeEnhancementMetadata bytecodeEnhancementMetadata = entityMetamodel.getBytecodeEnhancementMetadata();
			if ( bytecodeEnhancementMetadata.isEnhancedForLazyLoading() ) {

				// if the entity defines a HibernateProxy factory, see if there is an
				// existing proxy associated with the PC - and if so, use it
				if ( entityDescriptor.getRepresentationStrategy().getProxyFactory() != null ) {
					final Object proxy = persistenceContext.getProxy( entityKey );

					if ( proxy != null ) {
						if ( LOG.isTraceEnabled() ) {
							LOG.trace( "Entity proxy found in session cache" );
						}
						if ( LOG.isDebugEnabled() && HibernateProxy.extractLazyInitializer( proxy ).isUnwrap() ) {
							LOG.debug( "Ignoring NO_PROXY to honor laziness" );
						}

						return persistenceContext.narrowProxy( proxy, entityDescriptor, entityKey, null );
					}

					// specialized handling for entities with subclasses with a HibernateProxy factory
					if ( entityMetamodel.hasSubclasses() ) {
						// entities with subclasses that define a ProxyFactory can create
						// a HibernateProxy.
						LOG.debug( "Creating a HibernateProxy for to-one association with subclasses to honor laziness" );
						return createProxy( entityKey );
					}
					return bytecodeEnhancementMetadata.createEnhancedProxy( entityKey, false, this );
				}
				else if ( !entityMetamodel.hasSubclasses() ) {
					return bytecodeEnhancementMetadata.createEnhancedProxy( entityKey, false, this );
				}
				// If we get here, then the entity class has subclasses and there is no HibernateProxy factory.
				// The entity will get loaded below.
			}
			else {
				if ( entityDescriptor.hasProxy() ) {
					final Object existingProxy = persistenceContext.getProxy( entityKey );
					if ( existingProxy != null ) {
						return persistenceContext.narrowProxy( existingProxy, entityDescriptor, entityKey, null );
					}
					else {
						return createProxy( entityKey );
					}
				}
			}
		}

		// otherwise immediately materialize it

		// IMPLEMENTATION NOTE: increment/decrement the load count before/after getting the value
		//                      to ensure that #get does not clear the PersistenceContext.
		persistenceContext.beforeLoad();
		try {
			return get( entityName, id );
		}
		finally {
			persistenceContext.afterLoad();
		}
	}

	private Object createProxy(EntityKey entityKey) {
		final Object proxy = entityKey.getPersister().createProxy( entityKey.getIdentifier(), this );
		getPersistenceContext().addProxy( entityKey, proxy );
		return proxy;
	}

	@Override
	public void fetch(Object association) {
		checkOpen();
		PersistenceContext persistenceContext = getPersistenceContext();
		final LazyInitializer initializer = HibernateProxy.extractLazyInitializer( association );
		if ( initializer != null ) {
			if ( initializer.isUninitialized() ) {
				String entityName = initializer.getEntityName();
				Object id = initializer.getIdentifier();
				initializer.setSession(this);
				persistenceContext.beforeLoad();
				try {
					Object entity = initializer.getImplementation(); //forces the load to occur
					if ( entity==null ) {
						getFactory().getEntityNotFoundDelegate().handleEntityNotFound( entityName, id );
					}
					initializer.setImplementation( entity );
				}
				finally {
					initializer.unsetSession();
					persistenceContext.afterLoad();
					if ( persistenceContext.isLoadFinished() ) {
						persistenceContext.clear();
					}
				}
			}
		}
		else if ( isPersistentAttributeInterceptable( association ) ) {
			final PersistentAttributeInterceptor interceptor = asPersistentAttributeInterceptable( association ).$$_hibernate_getInterceptor();
			if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor) {
				EnhancementAsProxyLazinessInterceptor proxyInterceptor =
						(EnhancementAsProxyLazinessInterceptor) interceptor;
				proxyInterceptor.setSession( this );
				try {
					proxyInterceptor.forceInitialize( association, null );
				}
				finally {
					proxyInterceptor.unsetSession();
					if ( persistenceContext.isLoadFinished() ) {
						persistenceContext.clear();
					}
				}
			}
		}
		else if ( association instanceof PersistentCollection ) {
			PersistentCollection<?> persistentCollection = (PersistentCollection<?>) association;
			if ( !persistentCollection.wasInitialized() ) {

				final CollectionPersister collectionDescriptor = getFactory().getRuntimeMetamodels()
						.getMappingMetamodel()
						.getCollectionDescriptor( persistentCollection.getRole() );
				Object key = persistentCollection.getKey();
				persistenceContext.addUninitializedCollection( collectionDescriptor, persistentCollection, key );
				persistentCollection.setCurrentSession(this);
				try {
					collectionDescriptor.initialize( key, this );
				}
				finally {
					persistentCollection.unsetSession(this);
					if ( persistenceContext.isLoadFinished() ) {
						persistenceContext.clear();
					}
				}
			}
		}
	}

	@Override
	public boolean isAutoCloseSessionEnabled() {
		return getFactory().getSessionFactoryOptions().isAutoCloseSessionEnabled();
	}

	@Override
	public boolean shouldAutoClose() {
		return isAutoCloseSessionEnabled() && !isClosed();
	}


	private boolean isFlushModeNever() {
		return false;
	}

	private void managedClose() {
		if ( isClosed() ) {
			throw new SessionException( "Session was already closed" );
		}
		close();
	}

	private void managedFlush() {
		checkOpen();
		getJdbcCoordinator().executeBatch();
	}

	@Override
	public String bestGuessEntityName(Object object) {
		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			object = lazyInitializer.getImplementation();
		}
		return guessEntityName( object );
	}

//	@Override
//	public int executeUpdate(String query, QueryParameters queryParameters) throws HibernateException {
//		checkOpen();
//		queryParameters.validateParameters();
//		HQLQueryPlan plan = getQueryPlan( query, false );
//		boolean success = false;
//		int result = 0;
//		try {
//			result = plan.performExecuteUpdate( queryParameters, this );
//			success = true;
//		}
//		finally {
//			afterOperation( success );
//		}
//		temporaryPersistenceContext.clear();
//		return result;
//	}

	@Override
	public CacheMode getCacheMode() {
		return CacheMode.IGNORE;
	}

	@Override
	public void setCacheMode(CacheMode cm) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setHibernateFlushMode(FlushMode flushMode) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getContextEntityIdentifier(Object object) {
		checkOpen();
		return null;
	}

	@Override
	public String guessEntityName(Object entity) throws HibernateException {
		checkOpen();
		return entity.getClass().getName();
	}

	@Override
	public EntityPersister getEntityPersister(String entityName, Object object)
			throws HibernateException {
		checkOpen();
		if ( entityName == null ) {
			return getFactory().getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( guessEntityName( object ) );
		}
		else {
			return getFactory().getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( entityName )
					.getSubclassEntityPersister( object, getFactory() );
		}
	}

	@Override
	public Object getEntityUsingInterceptor(EntityKey key) throws HibernateException {
		checkOpen();

		final PersistenceContext persistenceContext = getPersistenceContext();
		final Object result = persistenceContext.getEntity( key );
		if ( result != null ) {
			return result;
		}

		final Object newObject = getInterceptor().getEntity( key.getEntityName(), key.getIdentifier() );
		if ( newObject != null ) {
			persistenceContext.addEntity( key, newObject );
			return newObject;
		}

		return null;
	}

	@Override
	public PersistenceContext getPersistenceContext() {
		return temporaryPersistenceContext;
	}

	@Override
	public void setAutoClear(boolean enabled) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object load(String entityName, Object identifier) {
		return null;
	}

	@Override
	public boolean isEventSource() {
		return false;
	}

	@Override
	public EventSource asEventSource() {
		throw new HibernateException( "Illegal Cast to EventSource - guard by invoking isEventSource() first" );
	}

	public boolean isDefaultReadOnly() {
		return false;
	}

	public void setDefaultReadOnly(boolean readOnly) throws HibernateException {
		if ( readOnly ) {
			throw new UnsupportedOperationException();
		}
	}

/////////////////////////////////////////////////////////////////////////////////////////////////////

	//TODO: COPY/PASTE FROM SessionImpl, pull up!

//	@Override
//	public List list(String query, QueryParameters queryParameters) throws HibernateException {
//		checkOpen();
//		queryParameters.validateParameters();
//		HQLQueryPlan plan = getQueryPlan( query, false );
//		boolean success = false;
//		List results = Collections.EMPTY_LIST;
//		try {
//			results = plan.performList( queryParameters, this );
//			success = true;
//		}
//		finally {
//			afterOperation( success );
//		}
//		temporaryPersistenceContext.clear();
//		return results;
//	}

	public void afterOperation(boolean success) {
		temporaryPersistenceContext.clear();
		if ( !isTransactionInProgress() ) {
			getJdbcCoordinator().afterTransaction();
		}
	}

	@Override
	public void afterScrollOperation() {
		temporaryPersistenceContext.clear();
	}

	@Override
	public void flush() {
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return NO_INFLUENCERS;
	}

	@Override
	public PersistenceContext getPersistenceContextInternal() {
		//In this case implemented the same as #getPersistenceContext
		return temporaryPersistenceContext;
	}

	@Override
	public boolean autoFlushIfRequired(Set<String> querySpaces) throws HibernateException {
		return false;
	}

//	@Override
//	public int executeNativeUpdate(
//			NativeSQLQuerySpecification nativeSQLQuerySpecification,
//			QueryParameters queryParameters) throws HibernateException {
//		checkOpen();
//		queryParameters.validateParameters();
//		NativeSQLQueryPlan plan = getNativeQueryPlan( nativeSQLQuerySpecification );
//
//		boolean success = false;
//		int result = 0;
//		try {
//			result = plan.performExecuteUpdate( queryParameters, this );
//			success = true;
//		}
//		finally {
//			afterOperation( success );
//		}
//		temporaryPersistenceContext.clear();
//		return result;
//	}

	@Override
	public void afterTransactionBegin() {

	}

	@Override
	public void beforeTransactionCompletion() {
		flushBeforeTransactionCompletion();
	}

	@Override
	public void afterTransactionCompletion(boolean successful, boolean delayed) {
		if ( shouldAutoClose() && !isClosed() ) {
			managedClose();
		}
	}

	@Override
	public boolean isTransactionInProgress() {
		return connectionProvided || super.isTransactionInProgress();
	}

	@Override
	public void flushBeforeTransactionCompletion() {
		boolean flush;
		try {
			flush = (
					!isClosed()
							&& !isFlushModeNever()
							&& !JtaStatusHelper.isRollback(
							getJtaPlatform().getCurrentStatus()
					) );
		}
		catch (SystemException se) {
			throw new HibernateException( "could not determine transaction status in beforeCompletion()", se );
		}
		if ( flush ) {
			managedFlush();
		}
	}

	private JtaPlatform getJtaPlatform() {
		return getFactory().getServiceRegistry().getService( JtaPlatform.class );
	}

	private LockMode getNullSafeLockMode(LockMode lockMode) {
		return lockMode == null ? LockMode.NONE : lockMode;
	}
}
