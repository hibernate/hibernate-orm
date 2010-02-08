/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.impl;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.hibernate.cache.CacheKey;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.StatefulPersistenceContext;
import org.hibernate.engine.Versioning;
import org.hibernate.engine.LoadQueryInfluencers;
import org.hibernate.engine.NonFlushedChanges;
import org.hibernate.engine.query.HQLQueryPlan;
import org.hibernate.engine.query.NativeSQLQueryPlan;
import org.hibernate.engine.query.sql.NativeSQLQuerySpecification;
import org.hibernate.event.EventListeners;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.jdbc.Batcher;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.loader.criteria.CriteriaLoader;
import org.hibernate.loader.custom.CustomLoader;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.Type;
import org.hibernate.util.CollectionHelper;

/**
 * @author Gavin King
 */
public class StatelessSessionImpl extends AbstractSessionImpl
		implements JDBCContext.Context, StatelessSession {

	private static final Logger log = LoggerFactory.getLogger( StatelessSessionImpl.class );

	private JDBCContext jdbcContext;
	private PersistenceContext temporaryPersistenceContext = new StatefulPersistenceContext( this );

	StatelessSessionImpl(Connection connection, SessionFactoryImpl factory) {
		super( factory );
		this.jdbcContext = new JDBCContext( this, connection, EmptyInterceptor.INSTANCE );
	}


	// inserts ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Serializable insert(Object entity) {
		errorIfClosed();
		return insert(null, entity);
	}

	public Serializable insert(String entityName, Object entity) {
		errorIfClosed();
		EntityPersister persister = getEntityPersister(entityName, entity);
		Serializable id = persister.getIdentifierGenerator().generate(this, entity);
		Object[] state = persister.getPropertyValues(entity, EntityMode.POJO);
		if ( persister.isVersioned() ) {
			boolean substitute = Versioning.seedVersion(state, persister.getVersionProperty(), persister.getVersionType(), this);
			if ( substitute ) {
				persister.setPropertyValues( entity, state, EntityMode.POJO );
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

	public void delete(Object entity) {
		errorIfClosed();
		delete(null, entity);
	}

	public void delete(String entityName, Object entity) {
		errorIfClosed();
		EntityPersister persister = getEntityPersister(entityName, entity);
		Serializable id = persister.getIdentifier( entity, this );
		Object version = persister.getVersion(entity, EntityMode.POJO);
		persister.delete(id, version, entity, this);
	}


	// updates ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void update(Object entity) {
		errorIfClosed();
		update(null, entity);
	}

	public void update(String entityName, Object entity) {
		errorIfClosed();
		EntityPersister persister = getEntityPersister(entityName, entity);
		Serializable id = persister.getIdentifier( entity, this );
		Object[] state = persister.getPropertyValues(entity, EntityMode.POJO);
		Object oldVersion;
		if ( persister.isVersioned() ) {
			oldVersion = persister.getVersion(entity, EntityMode.POJO);
			Object newVersion = Versioning.increment( oldVersion, persister.getVersionType(), this );
			Versioning.setVersion(state, newVersion, persister);
			persister.setPropertyValues(entity, state, EntityMode.POJO);
		}
		else {
			oldVersion = null;
		}
		persister.update(id, state, null, false, null, oldVersion, entity, null, this);
	}


	// loading ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Object get(Class entityClass, Serializable id) {
		return get( entityClass.getName(), id );
	}

	public Object get(Class entityClass, Serializable id, LockMode lockMode) {
		return get( entityClass.getName(), id, lockMode );
	}

	public Object get(String entityName, Serializable id) {
		return get(entityName, id, LockMode.NONE);
	}

	public Object get(String entityName, Serializable id, LockMode lockMode) {
		errorIfClosed();
		Object result = getFactory().getEntityPersister(entityName)
				.load(id, null, lockMode, this);
		temporaryPersistenceContext.clear();
		return result;
	}

	public void refresh(Object entity) {
		refresh( bestGuessEntityName( entity ), entity, LockMode.NONE );
	}

	public void refresh(String entityName, Object entity) {
		refresh( entityName, entity, LockMode.NONE );
	}

	public void refresh(Object entity, LockMode lockMode) {
		refresh( bestGuessEntityName( entity ), entity, lockMode );
	}

	public void refresh(String entityName, Object entity, LockMode lockMode) {
		final EntityPersister persister = this.getEntityPersister( entityName, entity );
		final Serializable id = persister.getIdentifier( entity, this );
		if ( log.isTraceEnabled() ) {
			log.trace(
					"refreshing transient " +
					MessageHelper.infoString( persister, id, this.getFactory() )
			);
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
			final CacheKey ck = new CacheKey(
					id,
			        persister.getIdentifierType(),
			        persister.getRootEntityName(),
			        this.getEntityMode(),
			        this.getFactory()
			);
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

	public Object immediateLoad(String entityName, Serializable id)
			throws HibernateException {
		throw new SessionException("proxies cannot be fetched by a stateless session");
	}

	public void initializeCollection(
			PersistentCollection collection,
	        boolean writing) throws HibernateException {
		throw new SessionException("collections cannot be fetched by a stateless session");
	}

	public Object instantiate(
			String entityName,
	        Serializable id) throws HibernateException {
		errorIfClosed();
		return getFactory().getEntityPersister( entityName )
				.instantiate( id, this );
	}

	public Object internalLoad(
			String entityName,
	        Serializable id,
	        boolean eager,
	        boolean nullable) throws HibernateException {
		errorIfClosed();
		EntityPersister persister = getFactory().getEntityPersister( entityName );
		// first, try to load it from the temp PC associated to this SS
		Object loaded = temporaryPersistenceContext.getEntity( new EntityKey( id, persister, getEntityMode() ) );
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

	public Iterator iterate(String query, QueryParameters queryParameters) throws HibernateException {
		throw new UnsupportedOperationException();
	}

	public Iterator iterateFilter(Object collection, String filter, QueryParameters queryParameters)
	throws HibernateException {
		throw new UnsupportedOperationException();
	}

	public List listFilter(Object collection, String filter, QueryParameters queryParameters)
	throws HibernateException {
		throw new UnsupportedOperationException();
	}


	public boolean isOpen() {
		return !isClosed();
	}

	public void close() {
		managedClose();
	}

	public ConnectionReleaseMode getConnectionReleaseMode() {
		return factory.getSettings().getConnectionReleaseMode();
	}

	public boolean isAutoCloseSessionEnabled() {
		return factory.getSettings().isAutoCloseSessionEnabled();
	}

	public boolean isFlushBeforeCompletionEnabled() {
		return true;
	}

	public boolean isFlushModeNever() {
		return false;
	}

	public void managedClose() {
		if ( isClosed() ) {
			throw new SessionException( "Session was already closed!" );
		}
		jdbcContext.getConnectionManager().close();
		setClosed();
	}

	public void managedFlush() {
		errorIfClosed();
		getBatcher().executeBatch();
	}

	public boolean shouldAutoClose() {
		return isAutoCloseSessionEnabled() && !isClosed();
	}

	public void afterTransactionCompletion(boolean successful, Transaction tx) {}

	public void beforeTransactionCompletion(Transaction tx) {}

	public String bestGuessEntityName(Object object) {
		if (object instanceof HibernateProxy) {
			object = ( (HibernateProxy) object ).getHibernateLazyInitializer().getImplementation();
		}
		return guessEntityName(object);
	}

	public Connection connection() {
		errorIfClosed();
		return jdbcContext.borrowConnection();
	}

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

	public Batcher getBatcher() {
		errorIfClosed();
		return jdbcContext.getConnectionManager()
				.getBatcher();
	}

	public CacheMode getCacheMode() {
		return CacheMode.IGNORE;
	}

	public int getDontFlushFromFind() {
		return 0;
	}

	public Map getEnabledFilters() {
		return CollectionHelper.EMPTY_MAP;
	}

	public Serializable getContextEntityIdentifier(Object object) {
		errorIfClosed();
		return null;
	}

	public EntityMode getEntityMode() {
		return EntityMode.POJO;
	}

	public EntityPersister getEntityPersister(String entityName, Object object)
			throws HibernateException {
		errorIfClosed();
		if ( entityName==null ) {
			return factory.getEntityPersister( guessEntityName( object ) );
		}
		else {
			return factory.getEntityPersister( entityName )
					.getSubclassEntityPersister( object, getFactory(), EntityMode.POJO );
		}
	}

	public Object getEntityUsingInterceptor(EntityKey key) throws HibernateException {
		errorIfClosed();
		return null;
	}

	public Type getFilterParameterType(String filterParameterName) {
		throw new UnsupportedOperationException();
	}

	public Object getFilterParameterValue(String filterParameterName) {
		throw new UnsupportedOperationException();
	}

	public FlushMode getFlushMode() {
		return FlushMode.COMMIT;
	}

	public Interceptor getInterceptor() {
		return EmptyInterceptor.INSTANCE;
	}

	public EventListeners getListeners() {
		throw new UnsupportedOperationException();
	}

	public PersistenceContext getPersistenceContext() {
		return temporaryPersistenceContext;
	}

	public long getTimestamp() {
		throw new UnsupportedOperationException();
	}

	public String guessEntityName(Object entity) throws HibernateException {
		errorIfClosed();
		return entity.getClass().getName();
	}


	public boolean isConnected() {
		return jdbcContext.getConnectionManager().isCurrentlyConnected();
	}

	public boolean isTransactionInProgress() {
		return jdbcContext.isTransactionInProgress();
	}

	public void setAutoClear(boolean enabled) {
		throw new UnsupportedOperationException();
	}

	public void setCacheMode(CacheMode cm) {
		throw new UnsupportedOperationException();
	}

	public void setFlushMode(FlushMode fm) {
		throw new UnsupportedOperationException();
	}

	public Transaction getTransaction() throws HibernateException {
		errorIfClosed();
		return jdbcContext.getTransaction();
	}

	public Transaction beginTransaction() throws HibernateException {
		errorIfClosed();
		Transaction result = getTransaction();
		result.begin();
		return result;
	}

	public boolean isEventSource() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isDefaultReadOnly() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDefaultReadOnly(boolean readOnly) throws HibernateException {
		if ( readOnly == true ) {
			throw new UnsupportedOperationException();
		}
	}

/////////////////////////////////////////////////////////////////////////////////////////////////////

	//TODO: COPY/PASTE FROM SessionImpl, pull up!

	public List list(String query, QueryParameters queryParameters) throws HibernateException {
		errorIfClosed();
		queryParameters.validateParameters();
		HQLQueryPlan plan = getHQLQueryPlan( query, false );
		boolean success = false;
		List results = CollectionHelper.EMPTY_LIST;
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
		if ( !jdbcContext.isTransactionInProgress() ) {
			jdbcContext.afterNontransactionalQuery(success);
		}
	}

	public Criteria createCriteria(Class persistentClass, String alias) {
		errorIfClosed();
		return new CriteriaImpl( persistentClass.getName(), alias, this );
	}

	public Criteria createCriteria(String entityName, String alias) {
		errorIfClosed();
		return new CriteriaImpl(entityName, alias, this);
	}

	public Criteria createCriteria(Class persistentClass) {
		errorIfClosed();
		return new CriteriaImpl( persistentClass.getName(), this );
	}

	public Criteria createCriteria(String entityName) {
		errorIfClosed();
		return new CriteriaImpl(entityName, this);
	}

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

	public ScrollableResults scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters)
	throws HibernateException {
		errorIfClosed();
		CustomLoader loader = new CustomLoader( customQuery, getFactory() );
		return loader.scroll(queryParameters, this);
	}

	public ScrollableResults scroll(String query, QueryParameters queryParameters) throws HibernateException {
		errorIfClosed();
		HQLQueryPlan plan = getHQLQueryPlan( query, false );
		return plan.performScroll( queryParameters, this );
	}

	public void afterScrollOperation() {
		temporaryPersistenceContext.clear();
	}

	public void flush() {}

	public NonFlushedChanges getNonFlushedChanges() {
		throw new UnsupportedOperationException();
	}

	public void applyNonFlushedChanges(NonFlushedChanges nonFlushedChanges) {
		throw new UnsupportedOperationException();
	}

	public String getFetchProfile() {
		return null;
	}

	public JDBCContext getJDBCContext() {
		return jdbcContext;
	}

	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return LoadQueryInfluencers.NONE;
	}

	public void setFetchProfile(String name) {}

	public void afterTransactionBegin(Transaction tx) {}

	protected boolean autoFlushIfRequired(Set querySpaces) throws HibernateException {
		// no auto-flushing to support in stateless session
		return false;
	}
	
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
