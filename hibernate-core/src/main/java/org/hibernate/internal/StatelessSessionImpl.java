/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.internal;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.CacheMode;
import org.hibernate.ConnectionReleaseMode;
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
import org.hibernate.cache.spi.CacheKey;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.StatefulPersistenceContext;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.query.spi.NativeSQLQueryPlan;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.NonFlushedChanges;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.transaction.internal.TransactionCoordinatorImpl;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.engine.transaction.spi.TransactionEnvironment;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.loader.criteria.CriteriaLoader;
import org.hibernate.loader.custom.CustomLoader;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 */
public class StatelessSessionImpl extends AbstractSessionImpl implements StatelessSession {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, StatelessSessionImpl.class.getName());

	private TransactionCoordinator transactionCoordinator;
	private PersistenceContext temporaryPersistenceContext = new StatefulPersistenceContext( this );

	StatelessSessionImpl(Connection connection, String tenantIdentifier, SessionFactoryImpl factory) {
		super( factory, tenantIdentifier );
		this.transactionCoordinator = new TransactionCoordinatorImpl( connection, this );
	}

	// TransactionContext ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public TransactionCoordinator getTransactionCoordinator() {
		return transactionCoordinator;
	}

	@Override
	public TransactionEnvironment getTransactionEnvironment() {
		return factory.getTransactionEnvironment();
	}

	// inserts ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Serializable insert(Object entity) {
		errorIfClosed();
		return insert(null, entity);
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
			id = persister.insert(state, entity, this);
		}
		else {
			persister.insert(id, state, entity, this);
		}
		persister.setIdentifier( entity, id, this );
		return id;
	}


	// deletes ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void delete(Object entity) {
		errorIfClosed();
		delete(null, entity);
	}

	@Override
	public void delete(String entityName, Object entity) {
		errorIfClosed();
		EntityPersister persister = getEntityPersister(entityName, entity);
		Serializable id = persister.getIdentifier( entity, this );
		Object version = persister.getVersion( entity );
		persister.delete(id, version, entity, this);
	}


	// updates ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void update(Object entity) {
		errorIfClosed();
		update(null, entity);
	}

	@Override
	public void update(String entityName, Object entity) {
		errorIfClosed();
		EntityPersister persister = getEntityPersister(entityName, entity);
		Serializable id = persister.getIdentifier( entity, this );
		Object[] state = persister.getPropertyValues( entity );
		Object oldVersion;
		if ( persister.isVersioned() ) {
			oldVersion = persister.getVersion( entity );
			Object newVersion = Versioning.increment( oldVersion, persister.getVersionType(), this );
			Versioning.setVersion(state, newVersion, persister);
			persister.setPropertyValues( entity, state );
		}
		else {
			oldVersion = null;
		}
		persister.update(id, state, null, false, null, oldVersion, entity, null, this);
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
		return get(entityName, id, LockMode.NONE);
	}

	@Override
	public Object get(String entityName, Serializable id, LockMode lockMode) {
		errorIfClosed();
		Object result = getFactory().getEntityPersister(entityName)
				.load(id, null, lockMode, this);
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
			final CacheKey ck = generateCacheKey( id, persister.getIdentifierType(), persister.getRootEntityName() );
			persister.getCacheAccessStrategy().evict( ck );
		}

		String previousFetchProfile = this.getFetchProfile();
		Object result = null;
		try {
			this.setFetchProfile( "refresh" );
			result = persister.load( id, entity, lockMode, this );
		}
		finally {
			this.setFetchProfile( previousFetchProfile );
		}
		UnresolvableObjectException.throwIfNull( result, id, persister.getEntityName() );
	}

	@Override
	public Object immediateLoad(String entityName, Serializable id)
			throws HibernateException {
		throw new SessionException("proxies cannot be fetched by a stateless session");
	}

	@Override
	public void initializeCollection(
			PersistentCollection collection,
	        boolean writing) throws HibernateException {
		throw new SessionException("collections cannot be fetched by a stateless session");
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
	public ConnectionReleaseMode getConnectionReleaseMode() {
		return factory.getSettings().getConnectionReleaseMode();
	}

	@Override
	public boolean shouldAutoJoinTransaction() {
		return true;
	}

	@Override
	public boolean isAutoCloseSessionEnabled() {
		return factory.getSettings().isAutoCloseSessionEnabled();
	}

	@Override
	public boolean isFlushBeforeCompletionEnabled() {
		return true;
	}

	@Override
	public boolean isFlushModeNever() {
		return false;
	}

	@Override
	public void managedClose() {
		if ( isClosed() ) {
			throw new SessionException( "Session was already closed!" );
		}
		transactionCoordinator.close();
		setClosed();
	}

	@Override
	public void managedFlush() {
		errorIfClosed();
		getTransactionCoordinator().getJdbcCoordinator().executeBatch();
	}

	@Override
	public boolean shouldAutoClose() {
		return isAutoCloseSessionEnabled() && !isClosed();
	}

	@Override
	public void afterTransactionBegin(TransactionImplementor hibernateTransaction) {
		// nothing to do here
	}

	@Override
	public void beforeTransactionCompletion(TransactionImplementor hibernateTransaction) {
		// nothing to do here
	}

	@Override
	public void afterTransactionCompletion(TransactionImplementor hibernateTransaction, boolean successful) {
		// nothing to do here
	}

	@Override
	public String onPrepareStatement(String sql) {
		return sql;
	}

	@Override
	public String bestGuessEntityName(Object object) {
		if (object instanceof HibernateProxy) {
			object = ( (HibernateProxy) object ).getHibernateLazyInitializer().getImplementation();
		}
		return guessEntityName(object);
	}

	@Override
	public Connection connection() {
		errorIfClosed();
		return transactionCoordinator.getJdbcCoordinator().getLogicalConnection().getDistinctConnectionProxy();
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
			afterOperation(success);
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
	public Map getEnabledFilters() {
		return Collections.EMPTY_MAP;
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
		if ( entityName==null ) {
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
	public Type getFilterParameterType(String filterParameterName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getFilterParameterValue(String filterParameterName) {
		throw new UnsupportedOperationException();
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
		throw new UnsupportedOperationException();
	}

	@Override
	public String guessEntityName(Object entity) throws HibernateException {
		errorIfClosed();
		return entity.getClass().getName();
	}

	@Override
	public boolean isConnected() {
		return transactionCoordinator.getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected();
	}

	@Override
	public boolean isTransactionInProgress() {
		return transactionCoordinator.isTransactionInProgress();
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
	public Transaction getTransaction() throws HibernateException {
		errorIfClosed();
		return transactionCoordinator.getTransaction();
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
			afterOperation(success);
		}
		temporaryPersistenceContext.clear();
		return results;
	}

	public void afterOperation(boolean success) {
		if ( ! transactionCoordinator.isTransactionInProgress() ) {
			transactionCoordinator.afterNonTransactionalQuery( success );
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
		return new CriteriaImpl(entityName, alias, this);
	}

	@Override
	public Criteria createCriteria(Class persistentClass) {
		errorIfClosed();
		return new CriteriaImpl( persistentClass.getName(), this );
	}

	@Override
	public Criteria createCriteria(String entityName) {
		errorIfClosed();
		return new CriteriaImpl(entityName, this);
	}

	@Override
	public ScrollableResults scroll(CriteriaImpl criteria, ScrollMode scrollMode) {
		errorIfClosed();
		String entityName = criteria.getEntityOrClassName();
		CriteriaLoader loader = new CriteriaLoader(
				getOuterJoinLoadable( entityName ),
		        factory,
		        criteria,
		        entityName,
		        getLoadQueryInfluencers()
		);
		return loader.scroll(this, scrollMode);
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public List list(CriteriaImpl criteria) throws HibernateException {
		errorIfClosed();
		String[] implementors = factory.getImplementors( criteria.getEntityOrClassName() );
		int size = implementors.length;

		CriteriaLoader[] loaders = new CriteriaLoader[size];
		for( int i=0; i <size; i++ ) {
			loaders[i] = new CriteriaLoader(
					getOuterJoinLoadable( implementors[i] ),
			        factory,
			        criteria,
			        implementors[i],
			        getLoadQueryInfluencers()
			);
		}


		List results = Collections.EMPTY_LIST;
		boolean success = false;
		try {
			for( int i=0; i<size; i++ ) {
				final List currentResults = loaders[i].list(this);
				currentResults.addAll(results);
				results = currentResults;
			}
			success = true;
		}
		finally {
			afterOperation(success);
		}
		temporaryPersistenceContext.clear();
		return results;
	}

	private OuterJoinLoadable getOuterJoinLoadable(String entityName) throws MappingException {
		EntityPersister persister = factory.getEntityPersister(entityName);
		if ( !(persister instanceof OuterJoinLoadable) ) {
			throw new MappingException( "class persister is not OuterJoinLoadable: " + entityName );
		}
		return ( OuterJoinLoadable ) persister;
	}

	@Override
	public List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters)
	throws HibernateException {
		errorIfClosed();
		CustomLoader loader = new CustomLoader( customQuery, getFactory() );

		boolean success = false;
		List results;
		try {
			results = loader.list(this, queryParameters);
			success = true;
		}
		finally {
			afterOperation(success);
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
	public NonFlushedChanges getNonFlushedChanges() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void applyNonFlushedChanges(NonFlushedChanges nonFlushedChanges) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getFetchProfile() {
		return null;
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return LoadQueryInfluencers.NONE;
	}

	@Override
	public void setFetchProfile(String name) {
	}

	@Override
	public int executeNativeUpdate(NativeSQLQuerySpecification nativeSQLQuerySpecification,
			QueryParameters queryParameters) throws HibernateException {
		errorIfClosed();
		queryParameters.validateParameters();
		NativeSQLQueryPlan plan = getNativeSQLQueryPlan(nativeSQLQuerySpecification);

		boolean success = false;
		int result = 0;
		try {
			result = plan.performExecuteUpdate(queryParameters, this);
			success = true;
		} finally {
			afterOperation(success);
		}
		temporaryPersistenceContext.clear();
		return result;
	}
}
