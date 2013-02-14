/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.Interceptor;
import org.hibernate.LobHelper;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.Query;
import org.hibernate.ReplicationMode;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.Transaction;
import org.hibernate.TypeHelper;
import org.hibernate.UnknownProfileException;
import org.hibernate.cache.spi.CacheKey;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.type.Type;


/**
 * This class is meant to be extended.
 * 
 * Wraps and delegates all methods to a {@link SessionImplementor} and
 * a {@link Session}. This is useful for custom implementations of this
 * API so that only some methods need to be overriden
 * (Used by Hibernate Search).
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class SessionDelegatorBaseImpl implements SessionImplementor, Session {

	protected final SessionImplementor sessionImplementor;
	protected final Session session;

	public SessionDelegatorBaseImpl(SessionImplementor sessionImplementor, Session session) {
		if ( sessionImplementor == null ) {
			throw new IllegalArgumentException( "Unable to create a SessionDelegatorBaseImpl from a null sessionImplementor object" );
		}
		if ( session == null ) {
			throw new IllegalArgumentException( "Unable to create a SessionDelegatorBaseImpl from a null session object" );
		}
		this.sessionImplementor = sessionImplementor;
		this.session = session;
	}

	// Delegates to SessionImplementor

	@Override
	public <T> T execute(Callback<T> callback) {
		return sessionImplementor.execute( callback );
	}

	@Override
	public String getTenantIdentifier() {
		return sessionImplementor.getTenantIdentifier();
	}

	@Override
	public JdbcConnectionAccess getJdbcConnectionAccess() {
		return sessionImplementor.getJdbcConnectionAccess();
	}

	@Override
	public EntityKey generateEntityKey(Serializable id, EntityPersister persister) {
		return sessionImplementor.generateEntityKey( id, persister );
	}

	@Override
	public CacheKey generateCacheKey(Serializable id, Type type, String entityOrRoleName) {
		return sessionImplementor.generateCacheKey( id, type, entityOrRoleName );
	}

	@Override
	public Interceptor getInterceptor() {
		return sessionImplementor.getInterceptor();
	}

	@Override
	public void setAutoClear(boolean enabled) {
		sessionImplementor.setAutoClear( enabled );
	}

	@Override
	public void disableTransactionAutoJoin() {
		sessionImplementor.disableTransactionAutoJoin();
	}

	@Override
	public boolean isTransactionInProgress() {
		return sessionImplementor.isTransactionInProgress();
	}

	@Override
	public void initializeCollection(PersistentCollection collection, boolean writing) throws HibernateException {
		sessionImplementor.initializeCollection( collection, writing );
	}

	@Override
	public Object internalLoad(String entityName, Serializable id, boolean eager, boolean nullable) throws HibernateException {
		return sessionImplementor.internalLoad( entityName, id, eager, nullable );
	}

	@Override
	public Object immediateLoad(String entityName, Serializable id) throws HibernateException {
		return sessionImplementor.immediateLoad( entityName, id );
	}

	@Override
	public long getTimestamp() {
		return sessionImplementor.getTimestamp();
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return sessionImplementor.getFactory();
	}

	@Override
	public List list(String query, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.list( query, queryParameters );
	}

	@Override
	public Iterator iterate(String query, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.iterate( query, queryParameters );
	}

	@Override
	public ScrollableResults scroll(String query, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.scroll( query, queryParameters );
	}

	@Override
	public ScrollableResults scroll(Criteria criteria, ScrollMode scrollMode) {
		return sessionImplementor.scroll( criteria, scrollMode );
	}

	@Override
	public List list(Criteria criteria) {
		return sessionImplementor.list( criteria );
	}

	@Override
	public List listFilter(Object collection, String filter, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.listFilter( collection, filter, queryParameters );
	}

	@Override
	public Iterator iterateFilter(Object collection, String filter, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.iterateFilter( collection, filter, queryParameters );
	}

	@Override
	public EntityPersister getEntityPersister(String entityName, Object object) throws HibernateException {
		return sessionImplementor.getEntityPersister( entityName, object );
	}

	@Override
	public Object getEntityUsingInterceptor(EntityKey key) throws HibernateException {
		return sessionImplementor.getEntityUsingInterceptor( key );
	}

	@Override
	public Serializable getContextEntityIdentifier(Object object) {
		return sessionImplementor.getContextEntityIdentifier( object );
	}

	@Override
	public String bestGuessEntityName(Object object) {
		return sessionImplementor.bestGuessEntityName( object );
	}

	@Override
	public String guessEntityName(Object entity) throws HibernateException {
		return sessionImplementor.guessEntityName( entity );
	}

	@Override
	public Object instantiate(String entityName, Serializable id) throws HibernateException {
		return sessionImplementor.instantiate( entityName, id );
	}

	@Override
	public List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.listCustomQuery( customQuery, queryParameters );
	}

	@Override
	public ScrollableResults scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.scrollCustomQuery( customQuery, queryParameters );
	}

	@Override
	public List list(NativeSQLQuerySpecification spec, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.list( spec, queryParameters );
	}

	@Override
	public ScrollableResults scroll(NativeSQLQuerySpecification spec, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.scroll( spec, queryParameters );
	}

	@Override
	public Object getFilterParameterValue(String filterParameterName) {
		return sessionImplementor.getFilterParameterValue( filterParameterName );
	}

	@Override
	public Type getFilterParameterType(String filterParameterName) {
		return sessionImplementor.getFilterParameterType( filterParameterName );
	}

	@Override
	public Map getEnabledFilters() {
		return sessionImplementor.getEnabledFilters();
	}

	@Override
	public int getDontFlushFromFind() {
		return sessionImplementor.getDontFlushFromFind();
	}

	@Override
	public PersistenceContext getPersistenceContext() {
		return sessionImplementor.getPersistenceContext();
	}

	@Override
	public int executeUpdate(String query, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.executeUpdate( query, queryParameters );
	}

	@Override
	public int executeNativeUpdate(NativeSQLQuerySpecification specification, QueryParameters queryParameters) throws HibernateException {
		return sessionImplementor.executeNativeUpdate( specification, queryParameters );
	}

	@Override
	public NonFlushedChanges getNonFlushedChanges() throws HibernateException {
		return sessionImplementor.getNonFlushedChanges();
	}

	@Override
	public void applyNonFlushedChanges(NonFlushedChanges nonFlushedChanges) throws HibernateException {
		sessionImplementor.applyNonFlushedChanges( nonFlushedChanges );
	}

	@Override
	public CacheMode getCacheMode() {
		return sessionImplementor.getCacheMode();
	}

	@Override
	public void setCacheMode(CacheMode cm) {
		sessionImplementor.setCacheMode( cm );
	}

	@Override
	public boolean isOpen() {
		return sessionImplementor.isOpen();
	}

	@Override
	public boolean isConnected() {
		return sessionImplementor.isConnected();
	}

	@Override
	public FlushMode getFlushMode() {
		return sessionImplementor.getFlushMode();
	}

	@Override
	public void setFlushMode(FlushMode fm) {
		sessionImplementor.setFlushMode( fm );
	}

	@Override
	public Connection connection() {
		return sessionImplementor.connection();
	}

	@Override
	public void flush() {
		sessionImplementor.flush();
	}

	@Override
	public Query getNamedQuery(String name) {
		return sessionImplementor.getNamedQuery( name );
	}

	@Override
	public Query getNamedSQLQuery(String name) {
		return sessionImplementor.getNamedSQLQuery( name );
	}

	@Override
	public boolean isEventSource() {
		return sessionImplementor.isEventSource();
	}

	@Override
	public void afterScrollOperation() {
		sessionImplementor.afterScrollOperation();
	}

	@Override
	public String getFetchProfile() {
		return sessionImplementor.getFetchProfile();
	}

	@Override
	public void setFetchProfile(String name) {
		sessionImplementor.setFetchProfile( name );
	}

	@Override
	public TransactionCoordinator getTransactionCoordinator() {
		return sessionImplementor.getTransactionCoordinator();
	}

	@Override
	public boolean isClosed() {
		return sessionImplementor.isClosed();
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return sessionImplementor.getLoadQueryInfluencers();
	}

	// Delegates to Session

	public Transaction beginTransaction() {
		return session.beginTransaction();
	}

	public Transaction getTransaction() {
		return session.getTransaction();
	}

	public Query createQuery(String queryString) {
		return session.createQuery( queryString );
	}

	public SQLQuery createSQLQuery(String queryString) {
		return session.createSQLQuery( queryString );
	}

	public Criteria createCriteria(Class persistentClass) {
		return session.createCriteria( persistentClass );
	}

	public Criteria createCriteria(Class persistentClass, String alias) {
		return session.createCriteria( persistentClass, alias );
	}

	public Criteria createCriteria(String entityName) {
		return session.createCriteria( entityName );
	}

	public Criteria createCriteria(String entityName, String alias) {
		return session.createCriteria( entityName, alias );
	}

	public SharedSessionBuilder sessionWithOptions() {
		return session.sessionWithOptions();
	}

	public SessionFactory getSessionFactory() {
		return session.getSessionFactory();
	}

	public Connection close() throws HibernateException {
		return session.close();
	}

	public void cancelQuery() throws HibernateException {
		session.cancelQuery();
	}

	public boolean isDirty() throws HibernateException {
		return session.isDirty();
	}

	public boolean isDefaultReadOnly() {
		return session.isDefaultReadOnly();
	}

	public void setDefaultReadOnly(boolean readOnly) {
		session.setDefaultReadOnly( readOnly );
	}

	public Serializable getIdentifier(Object object) {
		return session.getIdentifier( object );
	}

	public boolean contains(Object object) {
		return session.contains( object );
	}

	public void evict(Object object) {
		session.evict( object );
	}

	public Object load(Class theClass, Serializable id, LockMode lockMode) {
		return session.load( theClass, id, lockMode );
	}

	public Object load(Class theClass, Serializable id, LockOptions lockOptions) {
		return session.load( theClass, id, lockOptions );
	}

	public Object load(String entityName, Serializable id, LockMode lockMode) {
		return session.load( entityName, id, lockMode );
	}

	public Object load(String entityName, Serializable id, LockOptions lockOptions) {
		return session.load( entityName, id, lockOptions );
	}

	public Object load(Class theClass, Serializable id) {
		return session.load( theClass, id );
	}

	public Object load(String entityName, Serializable id) {
		return session.load( entityName, id );
	}

	public void load(Object object, Serializable id) {
		session.load( object, id );
	}

	public void replicate(Object object, ReplicationMode replicationMode) {
		session.replicate( object, replicationMode );
	}

	public void replicate(String entityName, Object object, ReplicationMode replicationMode) {
		session.replicate( entityName, object, replicationMode );
	}

	public Serializable save(Object object) {
		return session.save( object );
	}

	public Serializable save(String entityName, Object object) {
		return session.save( entityName, object );
	}

	public void saveOrUpdate(Object object) {
		session.saveOrUpdate( object );
	}

	public void saveOrUpdate(String entityName, Object object) {
		session.saveOrUpdate( entityName, object );
	}

	public void update(Object object) {
		session.update( object );
	}

	public void update(String entityName, Object object) {
		session.update( entityName, object );
	}

	public Object merge(Object object) {
		return session.merge( object );
	}

	public Object merge(String entityName, Object object) {
		return session.merge( entityName, object );
	}

	public void persist(Object object) {
		session.persist( object );
	}

	public void persist(String entityName, Object object) {
		session.persist( entityName, object );
	}

	public void delete(Object object) {
		session.delete( object );
	}

	public void delete(String entityName, Object object) {
		session.delete( entityName, object );
	}

	public void lock(Object object, LockMode lockMode) {
		session.lock( object, lockMode );
	}

	public void lock(String entityName, Object object, LockMode lockMode) {
		session.lock( entityName, object, lockMode );
	}

	public LockRequest buildLockRequest(LockOptions lockOptions) {
		return session.buildLockRequest( lockOptions );
	}

	public void refresh(Object object) {
		session.refresh( object );
	}

	public void refresh(String entityName, Object object) {
		session.refresh( entityName, object );
	}

	public void refresh(Object object, LockMode lockMode) {
		session.refresh( object, lockMode );
	}

	public void refresh(Object object, LockOptions lockOptions) {
		session.refresh( object, lockOptions );
	}

	public void refresh(String entityName, Object object, LockOptions lockOptions) {
		session.refresh( entityName, object, lockOptions );
	}

	public LockMode getCurrentLockMode(Object object) {
		return session.getCurrentLockMode( object );
	}

	public Query createFilter(Object collection, String queryString) {
		return session.createFilter( collection, queryString );
	}

	public void clear() {
		session.clear();
	}

	public Object get(Class clazz, Serializable id) {
		return session.get( clazz, id );
	}

	public Object get(Class clazz, Serializable id, LockMode lockMode) {
		return session.get( clazz, id, lockMode );
	}

	public Object get(Class clazz, Serializable id, LockOptions lockOptions) {
		return session.get( clazz, id, lockOptions );
	}

	public Object get(String entityName, Serializable id) {
		return session.get( entityName, id );
	}

	public Object get(String entityName, Serializable id, LockMode lockMode) {
		return session.get( entityName, id, lockMode );
	}

	public Object get(String entityName, Serializable id, LockOptions lockOptions) {
		return session.get( entityName, id, lockOptions );
	}

	public String getEntityName(Object object) {
		return session.getEntityName( object );
	}

	public IdentifierLoadAccess byId(String entityName) {
		return session.byId( entityName );
	}

	public IdentifierLoadAccess byId(Class entityClass) {
		return session.byId( entityClass );
	}

	public NaturalIdLoadAccess byNaturalId(String entityName) {
		return session.byNaturalId( entityName );
	}

	public NaturalIdLoadAccess byNaturalId(Class entityClass) {
		return session.byNaturalId( entityClass );
	}

	public SimpleNaturalIdLoadAccess bySimpleNaturalId(String entityName) {
		return session.bySimpleNaturalId( entityName );
	}

	public SimpleNaturalIdLoadAccess bySimpleNaturalId(Class entityClass) {
		return session.bySimpleNaturalId( entityClass );
	}

	public Filter enableFilter(String filterName) {
		return session.enableFilter( filterName );
	}

	public Filter getEnabledFilter(String filterName) {
		return session.getEnabledFilter( filterName );
	}

	public void disableFilter(String filterName) {
		session.disableFilter( filterName );
	}

	public SessionStatistics getStatistics() {
		return session.getStatistics();
	}

	public boolean isReadOnly(Object entityOrProxy) {
		return session.isReadOnly( entityOrProxy );
	}

	public void setReadOnly(Object entityOrProxy, boolean readOnly) {
		session.setReadOnly( entityOrProxy, readOnly );
	}

	public void doWork(Work work) throws HibernateException {
		session.doWork( work );
	}

	public <T> T doReturningWork(ReturningWork<T> work) throws HibernateException {
		return session.doReturningWork( work );
	}

	public Connection disconnect() {
		return session.disconnect();
	}

	public void reconnect(Connection connection) {
		session.reconnect( connection );
	}

	public boolean isFetchProfileEnabled(String name) throws UnknownProfileException {
		return session.isFetchProfileEnabled( name );
	}

	public void enableFetchProfile(String name) throws UnknownProfileException {
		session.enableFetchProfile( name );
	}

	public void disableFetchProfile(String name) throws UnknownProfileException {
		session.disableFetchProfile( name );
	}

	public TypeHelper getTypeHelper() {
		return session.getTypeHelper();
	}

	public LobHelper getLobHelper() {
		return session.getLobHelper();
	}

}
