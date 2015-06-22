/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;

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
import org.hibernate.SessionEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.Transaction;
import org.hibernate.TypeHelper;
import org.hibernate.UnknownProfileException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.stat.SessionStatistics;

/**
 * This class is meant to be extended.
 * 
 * Wraps and delegates all methods to a {@link SessionImplementor} and
 * a {@link Session}. This is useful for custom implementations of this
 * API so that only some methods need to be overridden
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
	public TransactionCoordinator getTransactionCoordinator() {
		return sessionImplementor.getTransactionCoordinator();
	}

	@Override
	public JdbcCoordinator getJdbcCoordinator() {
		return sessionImplementor.getJdbcCoordinator();
	}

	@Override
	public boolean isClosed() {
		return sessionImplementor.isClosed();
	}

	@Override
	public boolean shouldAutoClose() {
		return sessionImplementor.shouldAutoClose();
	}

	@Override
	public boolean isAutoCloseSessionEnabled() {
		return sessionImplementor.isAutoCloseSessionEnabled();
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return sessionImplementor.getLoadQueryInfluencers();
	}

	@Override
	public Query createQuery(NamedQueryDefinition namedQueryDefinition) {
		return sessionImplementor.createQuery( namedQueryDefinition );
	}

	@Override
	public SQLQuery createSQLQuery(NamedSQLQueryDefinition namedQueryDefinition) {
		return sessionImplementor.createSQLQuery( namedQueryDefinition );
	}

	@Override
	public SessionEventListenerManager getEventListenerManager() {
		return sessionImplementor.getEventListenerManager();
	}

	// Delegates to Session

	@Override
	public Transaction beginTransaction() {
		return session.beginTransaction();
	}

	@Override
	public Transaction getTransaction() {
		return session.getTransaction();
	}

	@Override
	public Query createQuery(String queryString) {
		return session.createQuery( queryString );
	}

	@Override
	public SQLQuery createSQLQuery(String queryString) {
		return session.createSQLQuery( queryString );
	}

	@Override
	public ProcedureCall getNamedProcedureCall(String name) {
		return session.getNamedProcedureCall( name );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName) {
		return session.createStoredProcedureCall( procedureName );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, Class... resultClasses) {
		return session.createStoredProcedureCall( procedureName, resultClasses );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
		return session.createStoredProcedureCall( procedureName, resultSetMappings );
	}

	@Override
	public Criteria createCriteria(Class persistentClass) {
		return session.createCriteria( persistentClass );
	}

	@Override
	public Criteria createCriteria(Class persistentClass, String alias) {
		return session.createCriteria( persistentClass, alias );
	}

	@Override
	public Criteria createCriteria(String entityName) {
		return session.createCriteria( entityName );
	}

	@Override
	public Criteria createCriteria(String entityName, String alias) {
		return session.createCriteria( entityName, alias );
	}

	@Override
	public SharedSessionBuilder sessionWithOptions() {
		return session.sessionWithOptions();
	}

	@Override
	public SessionFactory getSessionFactory() {
		return session.getSessionFactory();
	}

	@Override
	public void close() throws HibernateException {
		session.close();
	}

	@Override
	public void cancelQuery() throws HibernateException {
		session.cancelQuery();
	}

	@Override
	public boolean isDirty() throws HibernateException {
		return session.isDirty();
	}

	@Override
	public boolean isDefaultReadOnly() {
		return session.isDefaultReadOnly();
	}

	@Override
	public void setDefaultReadOnly(boolean readOnly) {
		session.setDefaultReadOnly( readOnly );
	}

	@Override
	public Serializable getIdentifier(Object object) {
		return session.getIdentifier( object );
	}

	@Override
	public boolean contains(Object object) {
		return session.contains( object );
	}

	@Override
	public void evict(Object object) {
		session.evict( object );
	}

	@Override
	public <T> T load(Class<T> theClass, Serializable id, LockMode lockMode) {
		return session.load( theClass, id, lockMode );
	}

	@Override
	public <T> T load(Class<T> theClass, Serializable id, LockOptions lockOptions) {
		return session.load( theClass, id, lockOptions );
	}

	@Override
	public Object load(String entityName, Serializable id, LockMode lockMode) {
		return session.load( entityName, id, lockMode );
	}

	@Override
	public Object load(String entityName, Serializable id, LockOptions lockOptions) {
		return session.load( entityName, id, lockOptions );
	}

	@Override
	public <T> T load(Class<T> theClass, Serializable id) {
		return session.load( theClass, id );
	}

	@Override
	public Object load(String entityName, Serializable id) {
		return session.load( entityName, id );
	}

	@Override
	public void load(Object object, Serializable id) {
		session.load( object, id );
	}

	@Override
	public void replicate(Object object, ReplicationMode replicationMode) {
		session.replicate( object, replicationMode );
	}

	@Override
	public void replicate(String entityName, Object object, ReplicationMode replicationMode) {
		session.replicate( entityName, object, replicationMode );
	}

	@Override
	public Serializable save(Object object) {
		return session.save( object );
	}

	@Override
	public Serializable save(String entityName, Object object) {
		return session.save( entityName, object );
	}

	@Override
	public void saveOrUpdate(Object object) {
		session.saveOrUpdate( object );
	}

	@Override
	public void saveOrUpdate(String entityName, Object object) {
		session.saveOrUpdate( entityName, object );
	}

	@Override
	public void update(Object object) {
		session.update( object );
	}

	@Override
	public void update(String entityName, Object object) {
		session.update( entityName, object );
	}

	@Override
	public Object merge(Object object) {
		return session.merge( object );
	}

	@Override
	public Object merge(String entityName, Object object) {
		return session.merge( entityName, object );
	}

	@Override
	public void persist(Object object) {
		session.persist( object );
	}

	@Override
	public void persist(String entityName, Object object) {
		session.persist( entityName, object );
	}

	@Override
	public void delete(Object object) {
		session.delete( object );
	}

	@Override
	public void delete(String entityName, Object object) {
		session.delete( entityName, object );
	}

	@Override
	public void lock(Object object, LockMode lockMode) {
		session.lock( object, lockMode );
	}

	@Override
	public void lock(String entityName, Object object, LockMode lockMode) {
		session.lock( entityName, object, lockMode );
	}

	@Override
	public LockRequest buildLockRequest(LockOptions lockOptions) {
		return session.buildLockRequest( lockOptions );
	}

	@Override
	public void refresh(Object object) {
		session.refresh( object );
	}

	@Override
	public void refresh(String entityName, Object object) {
		session.refresh( entityName, object );
	}

	@Override
	public void refresh(Object object, LockMode lockMode) {
		session.refresh( object, lockMode );
	}

	@Override
	public void refresh(Object object, LockOptions lockOptions) {
		session.refresh( object, lockOptions );
	}

	@Override
	public void refresh(String entityName, Object object, LockOptions lockOptions) {
		session.refresh( entityName, object, lockOptions );
	}

	@Override
	public LockMode getCurrentLockMode(Object object) {
		return session.getCurrentLockMode( object );
	}

	@Override
	public Query createFilter(Object collection, String queryString) {
		return session.createFilter( collection, queryString );
	}

	@Override
	public void clear() {
		session.clear();
	}

	@Override
	public <T> T get(Class<T> theClass, Serializable id) {
		return session.get( theClass, id );
	}

	@Override
	public <T> T get(Class<T> theClass, Serializable id, LockMode lockMode) {
		return session.get( theClass, id, lockMode );
	}

	@Override
	public <T> T get(Class<T> theClass, Serializable id, LockOptions lockOptions) {
		return session.get( theClass, id, lockOptions );
	}

	@Override
	public Object get(String entityName, Serializable id) {
		return session.get( entityName, id );
	}

	@Override
	public Object get(String entityName, Serializable id, LockMode lockMode) {
		return session.get( entityName, id, lockMode );
	}

	@Override
	public Object get(String entityName, Serializable id, LockOptions lockOptions) {
		return session.get( entityName, id, lockOptions );
	}

	@Override
	public String getEntityName(Object object) {
		return session.getEntityName( object );
	}

	@Override
	public IdentifierLoadAccess byId(String entityName) {
		return session.byId( entityName );
	}

	@Override
	public <T> IdentifierLoadAccess<T> byId(Class<T> entityClass) {
		return session.byId( entityClass );
	}

	@Override
	public NaturalIdLoadAccess byNaturalId(String entityName) {
		return session.byNaturalId( entityName );
	}

	@Override
	public <T> NaturalIdLoadAccess<T> byNaturalId(Class<T> entityClass) {
		return session.byNaturalId( entityClass );
	}

	@Override
	public SimpleNaturalIdLoadAccess bySimpleNaturalId(String entityName) {
		return session.bySimpleNaturalId( entityName );
	}

	@Override
	public <T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(Class<T> entityClass) {
		return session.bySimpleNaturalId( entityClass );
	}

	@Override
	public Filter enableFilter(String filterName) {
		return session.enableFilter( filterName );
	}

	@Override
	public Filter getEnabledFilter(String filterName) {
		return session.getEnabledFilter( filterName );
	}

	@Override
	public void disableFilter(String filterName) {
		session.disableFilter( filterName );
	}

	@Override
	public SessionStatistics getStatistics() {
		return session.getStatistics();
	}

	@Override
	public boolean isReadOnly(Object entityOrProxy) {
		return session.isReadOnly( entityOrProxy );
	}

	@Override
	public void setReadOnly(Object entityOrProxy, boolean readOnly) {
		session.setReadOnly( entityOrProxy, readOnly );
	}

	@Override
	public void doWork(Work work) throws HibernateException {
		session.doWork( work );
	}

	@Override
	public <T> T doReturningWork(ReturningWork<T> work) throws HibernateException {
		return session.doReturningWork( work );
	}

	@Override
	public Connection disconnect() {
		return session.disconnect();
	}

	@Override
	public void reconnect(Connection connection) {
		session.reconnect( connection );
	}

	@Override
	public boolean isFetchProfileEnabled(String name) throws UnknownProfileException {
		return session.isFetchProfileEnabled( name );
	}

	@Override
	public void enableFetchProfile(String name) throws UnknownProfileException {
		session.enableFetchProfile( name );
	}

	@Override
	public void disableFetchProfile(String name) throws UnknownProfileException {
		session.disableFetchProfile( name );
	}

	@Override
	public TypeHelper getTypeHelper() {
		return session.getTypeHelper();
	}

	@Override
	public LobHelper getLobHelper() {
		return session.getLobHelper();
	}

	@Override
	public void addEventListeners(SessionEventListener... listeners) {
		session.addEventListeners( listeners );
	}
}
