/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.transaction.SystemException;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.SessionException;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.SessionEventListenerManagerImpl;
import org.hibernate.engine.internal.StatefulPersistenceContext;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.jdbc.internal.JdbcCoordinatorImpl;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.query.spi.NativeSQLQueryPlan;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.loader.criteria.CriteriaLoader;
import org.hibernate.loader.custom.CustomLoader;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * @author Gavin King
 * @author Steve Ebersole
 */
public class StatelessSessionImpl extends AbstractSessionImpl implements StatelessSession {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( StatelessSessionImpl.class );

	private TransactionCoordinator transactionCoordinator;

	private transient JdbcCoordinator jdbcCoordinator;
	private PersistenceContext temporaryPersistenceContext = new StatefulPersistenceContext( this );
	private long timestamp;
	private JdbcSessionContext jdbcSessionContext;

	private LoadQueryInfluencers statelessLoadQueryInfluencers = new LoadQueryInfluencers( null ) {
		@Override
		public String getInternalFetchProfile() {
			return null;
		}

		@Override
		public void setInternalFetchProfile(String internalFetchProfile) {
		}
	};

	StatelessSessionImpl(
			Connection connection,
			String tenantIdentifier,
			SessionFactoryImpl factory) {
		this( connection, tenantIdentifier, factory, factory.getSettings().getRegionFactory().nextTimestamp() );
	}

	StatelessSessionImpl(
			Connection connection,
			String tenantIdentifier,
			SessionFactoryImpl factory,
			long timestamp) {
		super( factory, tenantIdentifier );
		this.jdbcSessionContext = new JdbcSessionContextImpl(
				factory,
				new StatementInspector() {
					@Override
					public String inspect(String sql) {
						return null;
					}
				}
		);
		this.jdbcCoordinator = new JdbcCoordinatorImpl( connection, this );

		this.transactionCoordinator = getTransactionCoordinatorBuilder().buildTransactionCoordinator(
				jdbcCoordinator,
				this
		);
		this.currentHibernateTransaction = getTransaction();
		this.timestamp = timestamp;
	}

	@Override
	public TransactionCoordinator getTransactionCoordinator() {
		return transactionCoordinator;
	}

	@Override
	public JdbcCoordinator getJdbcCoordinator() {
		return this.jdbcCoordinator;
	}

	@Override
	public boolean shouldAutoJoinTransaction() {
		return true;
	}

	// inserts ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Serializable insert(Object entity) {
		errorIfClosed();
		return insert( null, entity );
	}

	@Override
	public Serializable insert(String entityName, Object entity) {
		errorIfClosed();
		EntityPersister persister = getEntityPersister( entityName, entity );
		Serializable id = persister.getIdentifierGenerator().generate( this, entity );
		Object[] state = persister.getPropertyValues( entity );
		if ( persister.isVersioned() ) {
			boolean substitute = Versioning.seedVersion(
					state, persister.getVersionProperty(), persister.getVersionType(), this
			);
			if ( substitute ) {
				persister.setPropertyValues( entity, state );
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
		errorIfClosed();
		delete( null, entity );
	}

	@Override
	public void delete(String entityName, Object entity) {
		errorIfClosed();
		EntityPersister persister = getEntityPersister( entityName, entity );
		Serializable id = persister.getIdentifier( entity, this );
		Object version = persister.getVersion( entity );
		persister.delete( id, version, entity, this );
	}


	// updates ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void update(Object entity) {
		errorIfClosed();
		update( null, entity );
	}

	@Override
	public void update(String entityName, Object entity) {
		errorIfClosed();
		EntityPersister persister = getEntityPersister( entityName, entity );
		Serializable id = persister.getIdentifier( entity, this );
		Object[] state = persister.getPropertyValues( entity );
		Object oldVersion;
		if ( persister.isVersioned() ) {
			oldVersion = persister.getVersion( entity );
			Object newVersion = Versioning.increment( oldVersion, persister.getVersionType(), this );
			Versioning.setVersion( state, newVersion, persister );
			persister.setPropertyValues( entity, state );
		}
		else {
			oldVersion = null;
		}
		persister.update( id, state, null, false, null, oldVersion, entity, null, this );
	}


	// loading ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Object get(Class entityClass, Serializable id) {
		return get( entityClass.getName(), id );
	}

	@Override
	public Object get(Class entityClass, Serializable id, LockMode lockMode) {
		return get( entityClass.getName(), id, lockMode );
	}

	@Override
	public Object get(String entityName, Serializable id) {
		return get( entityName, id, LockMode.NONE );
	}

	@Override
	public Object get(String entityName, Serializable id, LockMode lockMode) {
		errorIfClosed();
		Object result = getFactory().getEntityPersister( entityName )
				.load( id, null, lockMode, this );
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
		final Serializable id = persister.getIdentifier( entity, this );
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

		if ( persister.hasCache() ) {
			final EntityRegionAccessStrategy cache = persister.getCacheAccessStrategy();
			final Object ck = cache.generateCacheKey( id, persister, getFactory(), getTenantIdentifier() );
			cache.evict( ck );
		}
		String previousFetchProfile = this.getLoadQueryInfluencers().getInternalFetchProfile();
		Object result = null;
		try {
			this.getLoadQueryInfluencers().setInternalFetchProfile( "refresh" );
			result = persister.load( id, entity, lockMode, this );
		}
		finally {
			this.getLoadQueryInfluencers().setInternalFetchProfile( previousFetchProfile );
		}
		UnresolvableObjectException.throwIfNull( result, id, persister.getEntityName() );
	}

	@Override
	public Object immediateLoad(String entityName, Serializable id)
			throws HibernateException {
		throw new SessionException( "proxies cannot be fetched by a stateless session" );
	}

	@Override
	public void initializeCollection(
			PersistentCollection collection,
			boolean writing) throws HibernateException {
		throw new SessionException( "collections cannot be fetched by a stateless session" );
	}

	@Override
	public Object instantiate(
			String entityName,
			Serializable id) throws HibernateException {
		errorIfClosed();
		return getFactory().getEntityPersister( entityName ).instantiate( id, this );
	}

	@Override
	public Object internalLoad(
			String entityName,
			Serializable id,
			boolean eager,
			boolean nullable) throws HibernateException {
		errorIfClosed();
		EntityPersister persister = getFactory().getEntityPersister( entityName );
		// first, try to load it from the temp PC associated to this SS
		Object loaded = temporaryPersistenceContext.getEntity( generateEntityKey( id, persister ) );
		if ( loaded != null ) {
			// we found it in the temp PC.  Should indicate we are in the midst of processing a result set
			// containing eager fetches via join fetch
			return loaded;
		}
		if ( !eager && persister.hasProxy() ) {
			// if the metadata allowed proxy creation and caller did not request forceful eager loading,
			// generate a proxy
			return persister.createProxy( id, this );
		}
		// otherwise immediately materialize it
		return get( entityName, id );
	}

	@Override
	public Iterator iterate(String query, QueryParameters queryParameters) throws HibernateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator iterateFilter(Object collection, String filter, QueryParameters queryParameters)
			throws HibernateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List listFilter(Object collection, String filter, QueryParameters queryParameters)
			throws HibernateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isOpen() {
		return !isClosed();
	}

	@Override
	public void close() {
		managedClose();
	}

	@Override
	public boolean isAutoCloseSessionEnabled() {
		return factory.getSettings().isAutoCloseSessionEnabled();
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
			throw new SessionException( "Session was already closed!" );
		}
		jdbcCoordinator.close();
		setClosed();
	}

	private void managedFlush() {
		errorIfClosed();
		jdbcCoordinator.executeBatch();
	}

	private SessionEventListenerManagerImpl sessionEventsManager;

	@Override
	public SessionEventListenerManager getEventListenerManager() {
		if ( sessionEventsManager == null ) {
			sessionEventsManager = new SessionEventListenerManagerImpl();
		}
		return sessionEventsManager;
	}

	@Override
	public String bestGuessEntityName(Object object) {
		if ( object instanceof HibernateProxy ) {
			object = ( (HibernateProxy) object ).getHibernateLazyInitializer().getImplementation();
		}
		return guessEntityName( object );
	}

	@Override
	public Connection connection() {
		errorIfClosed();
		return jdbcCoordinator.getLogicalConnection().getPhysicalConnection();
	}

	@Override
	public int executeUpdate(String query, QueryParameters queryParameters)
			throws HibernateException {
		errorIfClosed();
		queryParameters.validateParameters();
		HQLQueryPlan plan = getHQLQueryPlan( query, false );
		boolean success = false;
		int result = 0;
		try {
			result = plan.performExecuteUpdate( queryParameters, this );
			success = true;
		}
		finally {
			afterOperation( success );
		}
		temporaryPersistenceContext.clear();
		return result;
	}

	@Override
	public CacheMode getCacheMode() {
		return CacheMode.IGNORE;
	}

	@Override
	public int getDontFlushFromFind() {
		return 0;
	}

	@Override
	public Serializable getContextEntityIdentifier(Object object) {
		errorIfClosed();
		return null;
	}

	public EntityMode getEntityMode() {
		return EntityMode.POJO;
	}

	@Override
	public EntityPersister getEntityPersister(String entityName, Object object)
			throws HibernateException {
		errorIfClosed();
		if ( entityName == null ) {
			return factory.getEntityPersister( guessEntityName( object ) );
		}
		else {
			return factory.getEntityPersister( entityName ).getSubclassEntityPersister( object, getFactory() );
		}
	}

	@Override
	public Object getEntityUsingInterceptor(EntityKey key) throws HibernateException {
		errorIfClosed();
		return null;
	}

	@Override
	public FlushMode getFlushMode() {
		return FlushMode.COMMIT;
	}

	@Override
	public Interceptor getInterceptor() {
		return EmptyInterceptor.INSTANCE;
	}

	@Override
	public PersistenceContext getPersistenceContext() {
		return temporaryPersistenceContext;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public String guessEntityName(Object entity) throws HibernateException {
		errorIfClosed();
		return entity.getClass().getName();
	}

	@Override
	public boolean isConnected() {
		return jdbcCoordinator.getLogicalConnection().isPhysicallyConnected();
	}

	@Override
	public boolean isTransactionInProgress() {
		return !isClosed() && transactionCoordinator.isJoined() && transactionCoordinator.getTransactionDriverControl()
				.getStatus() == TransactionStatus.ACTIVE;
	}

	@Override
	public void setAutoClear(boolean enabled) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void disableTransactionAutoJoin() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCacheMode(CacheMode cm) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setFlushMode(FlushMode fm) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Transaction beginTransaction() throws HibernateException {
		errorIfClosed();
		Transaction result = getTransaction();
		result.begin();
		return result;
	}

	@Override
	public boolean isEventSource() {
		return false;
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

	@Override
	public List list(String query, QueryParameters queryParameters) throws HibernateException {
		errorIfClosed();
		queryParameters.validateParameters();
		HQLQueryPlan plan = getHQLQueryPlan( query, false );
		boolean success = false;
		List results = Collections.EMPTY_LIST;
		try {
			results = plan.performList( queryParameters, this );
			success = true;
		}
		finally {
			afterOperation( success );
		}
		temporaryPersistenceContext.clear();
		return results;
	}

	public void afterOperation(boolean success) {
		if ( !isTransactionInProgress() ) {
			jdbcCoordinator.afterTransaction();
		}
	}

	@Override
	public Criteria createCriteria(Class persistentClass, String alias) {
		errorIfClosed();
		return new CriteriaImpl( persistentClass.getName(), alias, this );
	}

	@Override
	public Criteria createCriteria(String entityName, String alias) {
		errorIfClosed();
		return new CriteriaImpl( entityName, alias, this );
	}

	@Override
	public Criteria createCriteria(Class persistentClass) {
		errorIfClosed();
		return new CriteriaImpl( persistentClass.getName(), this );
	}

	@Override
	public Criteria createCriteria(String entityName) {
		errorIfClosed();
		return new CriteriaImpl( entityName, this );
	}

	@Override
	public ScrollableResults scroll(Criteria criteria, ScrollMode scrollMode) {
		// TODO: Is this guaranteed to always be CriteriaImpl?
		CriteriaImpl criteriaImpl = (CriteriaImpl) criteria;

		errorIfClosed();
		String entityName = criteriaImpl.getEntityOrClassName();
		CriteriaLoader loader = new CriteriaLoader(
				getOuterJoinLoadable( entityName ),
				factory,
				criteriaImpl,
				entityName,
				getLoadQueryInfluencers()
		);
		return loader.scroll( this, scrollMode );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List list(Criteria criteria) throws HibernateException {
		// TODO: Is this guaranteed to always be CriteriaImpl?
		CriteriaImpl criteriaImpl = (CriteriaImpl) criteria;

		errorIfClosed();
		String[] implementors = factory.getImplementors( criteriaImpl.getEntityOrClassName() );
		int size = implementors.length;

		CriteriaLoader[] loaders = new CriteriaLoader[size];
		for ( int i = 0; i < size; i++ ) {
			loaders[i] = new CriteriaLoader(
					getOuterJoinLoadable( implementors[i] ),
					factory,
					criteriaImpl,
					implementors[i],
					getLoadQueryInfluencers()
			);
		}


		List results = Collections.EMPTY_LIST;
		boolean success = false;
		try {
			for ( int i = 0; i < size; i++ ) {
				final List currentResults = loaders[i].list( this );
				currentResults.addAll( results );
				results = currentResults;
			}
			success = true;
		}
		finally {
			afterOperation( success );
		}
		temporaryPersistenceContext.clear();
		return results;
	}

	private OuterJoinLoadable getOuterJoinLoadable(String entityName) throws MappingException {
		EntityPersister persister = factory.getEntityPersister( entityName );
		if ( !( persister instanceof OuterJoinLoadable ) ) {
			throw new MappingException( "class persister is not OuterJoinLoadable: " + entityName );
		}
		return (OuterJoinLoadable) persister;
	}

	@Override
	public List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters)
			throws HibernateException {
		errorIfClosed();
		CustomLoader loader = new CustomLoader( customQuery, getFactory() );

		boolean success = false;
		List results;
		try {
			results = loader.list( this, queryParameters );
			success = true;
		}
		finally {
			afterOperation( success );
		}
		temporaryPersistenceContext.clear();
		return results;
	}

	@Override
	public ScrollableResults scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters)
			throws HibernateException {
		errorIfClosed();
		CustomLoader loader = new CustomLoader( customQuery, getFactory() );
		return loader.scroll( queryParameters, this );
	}

	@Override
	public ScrollableResults scroll(String query, QueryParameters queryParameters) throws HibernateException {
		errorIfClosed();
		HQLQueryPlan plan = getHQLQueryPlan( query, false );
		return plan.performScroll( queryParameters, this );
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
		return statelessLoadQueryInfluencers;
	}

	@Override
	public int executeNativeUpdate(
			NativeSQLQuerySpecification nativeSQLQuerySpecification,
			QueryParameters queryParameters) throws HibernateException {
		errorIfClosed();
		queryParameters.validateParameters();
		NativeSQLQueryPlan plan = getNativeSQLQueryPlan( nativeSQLQuerySpecification );

		boolean success = false;
		int result = 0;
		try {
			result = plan.performExecuteUpdate( queryParameters, this );
			success = true;
		}
		finally {
			afterOperation( success );
		}
		temporaryPersistenceContext.clear();
		return result;
	}

	@Override
	public JdbcSessionContext getJdbcSessionContext() {
		return this.jdbcSessionContext;
	}

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
	public void flushBeforeTransactionCompletion() {
		boolean flush = false;
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
		return factory.getServiceRegistry().getService( JtaPlatform.class );
	}
}
