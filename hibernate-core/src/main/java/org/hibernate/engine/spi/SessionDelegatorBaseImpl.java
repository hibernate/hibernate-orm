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
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.Metamodel;

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
import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.ReplicationMode;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.SessionEventListener;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.Transaction;
import org.hibernate.TypeHelper;
import org.hibernate.UnknownProfileException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.spi.NativeQueryImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

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
@SuppressWarnings("deprecation")
public class SessionDelegatorBaseImpl implements SessionImplementor {

	protected final SessionImplementor delegate;

	/**
	 * @deprecated (snce 6.0) SessionDelegatorBaseImpl should take just one argument, the SessionImplementor.
	 * Use the {@link #SessionDelegatorBaseImpl(SessionImplementor)} form instead
	 */
	@Deprecated
	public SessionDelegatorBaseImpl(SessionImplementor delegate, Session session) {
		if ( delegate == null ) {
			throw new IllegalArgumentException( "Unable to create a SessionDelegatorBaseImpl from a null delegate object" );
		}
		if ( session == null ) {
			throw new IllegalArgumentException( "Unable to create a SessionDelegatorBaseImpl from a null Session" );
		}
		if ( delegate != session ) {
			throw new IllegalArgumentException( "Unable to create a SessionDelegatorBaseImpl from different Session/SessionImplementor references" );
		}

		this.delegate = delegate;
	}

	public SessionDelegatorBaseImpl(SessionImplementor delegate) {
		this( delegate, delegate );
	}

	@Override
	public <T> T execute(Callback<T> callback) {
		return delegate.execute( callback );
	}

	@Override
	public String getTenantIdentifier() {
		return delegate.getTenantIdentifier();
	}

	@Override
	public UUID getSessionIdentifier() {
		return delegate.getSessionIdentifier();
	}

	@Override
	public JdbcConnectionAccess getJdbcConnectionAccess() {
		return delegate.getJdbcConnectionAccess();
	}

	@Override
	public EntityKey generateEntityKey(Serializable id, EntityPersister persister) {
		return delegate.generateEntityKey( id, persister );
	}

	@Override
	public Interceptor getInterceptor() {
		return delegate.getInterceptor();
	}

	@Override
	public void setAutoClear(boolean enabled) {
		delegate.setAutoClear( enabled );
	}

	@Override
	public boolean isTransactionInProgress() {
		return delegate.isTransactionInProgress();
	}

	@Override
	public LockOptions getLockRequest(LockModeType lockModeType, Map<String, Object> properties) {
		return delegate.getLockRequest( lockModeType, properties );
	}

	@Override
	public LockOptions buildLockOptions(LockModeType lockModeType, Map<String, Object> properties) {
		return delegate.buildLockOptions( lockModeType, properties );
	}

	@Override
	public <T> QueryImplementor<T> createQuery(
			String jpaqlString,
			Class<T> resultClass,
			Selection selection,
			QueryOptions queryOptions) {
		return delegate.createQuery( jpaqlString,resultClass, selection, queryOptions );
	}

	@Override
	public void initializeCollection(PersistentCollection collection, boolean writing) throws HibernateException {
		delegate.initializeCollection( collection, writing );
	}

	@Override
	public Object internalLoad(String entityName, Serializable id, boolean eager, boolean nullable) throws HibernateException {
		return delegate.internalLoad( entityName, id, eager, nullable );
	}

	@Override
	public Object immediateLoad(String entityName, Serializable id) throws HibernateException {
		return delegate.immediateLoad( entityName, id );
	}

	@Override
	public long getTimestamp() {
		return delegate.getTimestamp();
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return delegate.getFactory();
	}

	@Override
	public List list(String query, QueryParameters queryParameters) throws HibernateException {
		return delegate.list( query, queryParameters );
	}

	@Override
	public Iterator iterate(String query, QueryParameters queryParameters) throws HibernateException {
		return delegate.iterate( query, queryParameters );
	}

	@Override
	public ScrollableResultsImplementor scroll(String query, QueryParameters queryParameters) throws HibernateException {
		return delegate.scroll( query, queryParameters );
	}

	@Override
	public ScrollableResultsImplementor scroll(Criteria criteria, ScrollMode scrollMode) {
		return delegate.scroll( criteria, scrollMode );
	}

	@Override
	public List list(Criteria criteria) {
		return delegate.list( criteria );
	}

	@Override
	public List listFilter(Object collection, String filter, QueryParameters queryParameters) throws HibernateException {
		return delegate.listFilter( collection, filter, queryParameters );
	}

	@Override
	public Iterator iterateFilter(Object collection, String filter, QueryParameters queryParameters) throws HibernateException {
		return delegate.iterateFilter( collection, filter, queryParameters );
	}

	@Override
	public EntityPersister getEntityPersister(String entityName, Object object) throws HibernateException {
		return delegate.getEntityPersister( entityName, object );
	}

	@Override
	public Object getEntityUsingInterceptor(EntityKey key) throws HibernateException {
		return delegate.getEntityUsingInterceptor( key );
	}

	@Override
	public Serializable getContextEntityIdentifier(Object object) {
		return delegate.getContextEntityIdentifier( object );
	}

	@Override
	public String bestGuessEntityName(Object object) {
		return delegate.bestGuessEntityName( object );
	}

	@Override
	public String guessEntityName(Object entity) throws HibernateException {
		return delegate.guessEntityName( entity );
	}

	@Override
	public Object instantiate(String entityName, Serializable id) throws HibernateException {
		return delegate.instantiate( entityName, id );
	}

	@Override
	public List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) throws HibernateException {
		return delegate.listCustomQuery( customQuery, queryParameters );
	}

	@Override
	public ScrollableResultsImplementor scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) throws HibernateException {
		return delegate.scrollCustomQuery( customQuery, queryParameters );
	}

	@Override
	public List list(NativeSQLQuerySpecification spec, QueryParameters queryParameters) throws HibernateException {
		return delegate.list( spec, queryParameters );
	}

	@Override
	public ScrollableResultsImplementor scroll(NativeSQLQuerySpecification spec, QueryParameters queryParameters) throws HibernateException {
		return delegate.scroll( spec, queryParameters );
	}

	@Override
	public int getDontFlushFromFind() {
		return delegate.getDontFlushFromFind();
	}

	@Override
	public PersistenceContext getPersistenceContext() {
		return delegate.getPersistenceContext();
	}

	@Override
	public int executeUpdate(String query, QueryParameters queryParameters) throws HibernateException {
		return delegate.executeUpdate( query, queryParameters );
	}

	@Override
	public int executeNativeUpdate(NativeSQLQuerySpecification specification, QueryParameters queryParameters) throws HibernateException {
		return delegate.executeNativeUpdate( specification, queryParameters );
	}

	@Override
	public CacheMode getCacheMode() {
		return delegate.getCacheMode();
	}

	@Override
	public void setCacheMode(CacheMode cm) {
		delegate.setCacheMode( cm );
	}

	@Override
	public boolean isOpen() {
		return delegate.isOpen();
	}

	@Override
	public boolean isConnected() {
		return delegate.isConnected();
	}

	@Override
	public void checkOpen(boolean markForRollbackIfClosed) {
		delegate.checkOpen( markForRollbackIfClosed );
	}

	@Override
	public void markForRollbackOnly() {
		delegate.markForRollbackOnly();
	}

	@Override
	public FlushModeType getFlushMode() {
		return delegate.getFlushMode();
	}

	@Override
	public void setFlushMode(FlushModeType flushModeType) {
		delegate.setFlushMode( flushModeType );
	}

	@Override
	public void setHibernateFlushMode(FlushMode flushMode) {
		delegate.setHibernateFlushMode( flushMode );
	}

	@Override
	public FlushMode getHibernateFlushMode() {
		return delegate.getHibernateFlushMode();
	}

	@Override
	public void setFlushMode(FlushMode fm) {
		delegate.setHibernateFlushMode( fm );
	}

	@Override
	public void lock(Object entity, LockModeType lockMode) {
		delegate.lock( entity, lockMode );
	}

	@Override
	public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
		delegate.lock( entity, lockMode, properties );
	}

	@Override
	public Connection connection() {
		return delegate.connection();
	}

	@Override
	public void flush() {
		delegate.flush();
	}

	@Override
	public boolean isEventSource() {
		return delegate.isEventSource();
	}

	@Override
	public void afterScrollOperation() {
		delegate.afterScrollOperation();
	}

	@Override
	public TransactionCoordinator getTransactionCoordinator() {
		return delegate.getTransactionCoordinator();
	}

	@Override
	public JdbcCoordinator getJdbcCoordinator() {
		return delegate.getJdbcCoordinator();
	}

	@Override
	public JdbcServices getJdbcServices() {
		return delegate.getJdbcServices();
	}

	@Override
	public JdbcSessionContext getJdbcSessionContext() {
		return delegate.getJdbcSessionContext();
	}

	@Override
	public boolean isClosed() {
		return delegate.isClosed();
	}

	@Override
	public boolean shouldAutoClose() {
		return delegate.shouldAutoClose();
	}

	@Override
	public boolean isAutoCloseSessionEnabled() {
		return delegate.isAutoCloseSessionEnabled();
	}

	@Override
	public boolean shouldAutoJoinTransaction() {
		return delegate.shouldAutoJoinTransaction();
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return delegate.getLoadQueryInfluencers();
	}

	@Override
	public ExceptionConverter getExceptionConverter() {
		return delegate.getExceptionConverter();
	}

	@Override
	public SessionEventListenerManager getEventListenerManager() {
		return delegate.getEventListenerManager();
	}

	@Override
	public Transaction accessTransaction() {
		return delegate.accessTransaction();
	}

	@Override
	public Transaction beginTransaction() {
		return delegate.beginTransaction();
	}

	@Override
	public Transaction getTransaction() {
		return delegate.getTransaction();
	}

	@Override
	public void afterTransactionBegin() {
		delegate.afterTransactionBegin();
	}

	@Override
	public void beforeTransactionCompletion() {
		delegate.beforeTransactionCompletion();
	}

	@Override
	public void afterTransactionCompletion(boolean successful, boolean delayed) {
		delegate.afterTransactionCompletion( successful, delayed );
	}

	@Override
	public void flushBeforeTransactionCompletion() {
		delegate.flushBeforeTransactionCompletion();
	}

	@Override
	public EntityManagerFactory getEntityManagerFactory() {
		return delegate.getFactory();
	}

	@Override
	public CriteriaBuilder getCriteriaBuilder() {
		return delegate.getCriteriaBuilder();
	}

	@Override
	public Metamodel getMetamodel() {
		return delegate.getMetamodel();
	}

	@Override
	public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
		return delegate.createEntityGraph( rootType );
	}

	@Override
	public EntityGraph<?> createEntityGraph(String graphName) {
		return delegate.createEntityGraph( graphName );
	}

	@Override
	public EntityGraph<?> getEntityGraph(String graphName) {
		return delegate.getEntityGraph( graphName );
	}

	@Override
	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
		return delegate.getEntityGraphs( entityClass );
	}

	@Override
	public QueryImplementor getNamedQuery(String name) {
		return delegate.getNamedQuery( name );
	}

	@Override
	public NativeQueryImplementor getNamedSQLQuery(String name) {
		return delegate.getNamedSQLQuery( name );
	}

	@Override
	public NativeQueryImplementor getNamedNativeQuery(String name) {
		return delegate.getNamedNativeQuery( name );
	}

	@Override
	public QueryImplementor createQuery(String queryString) {
		return delegate.createQuery( queryString );
	}

	@Override
	public <T> QueryImplementor<T> createQuery(String queryString, Class<T> resultType) {
		return delegate.createQuery( queryString, resultType );
	}

	@Override
	public <T> QueryImplementor<T> createQuery(CriteriaQuery<T> criteriaQuery) {
		return delegate.createQuery( criteriaQuery );
	}

	@Override
	public QueryImplementor createQuery(CriteriaUpdate updateQuery) {
		return delegate.createQuery( updateQuery );
	}

	@Override
	public QueryImplementor createQuery(CriteriaDelete deleteQuery) {
		return delegate.createQuery( deleteQuery );
	}

	@Override
	public QueryImplementor createNamedQuery(String name) {
		return delegate.createNamedQuery( name );
	}

	@Override
	public <T> QueryImplementor<T> createNamedQuery(String name, Class<T> resultClass) {
		return delegate.createNamedQuery( name, resultClass );
	}

	@Override
	public NativeQueryImplementor createNativeQuery(String sqlString) {
		return delegate.createNativeQuery( sqlString );
	}

	@Override
	public NativeQueryImplementor createNativeQuery(String sqlString, Class resultClass) {
		return delegate.createNativeQuery( sqlString, resultClass );
	}

	@Override
	public NativeQueryImplementor createNativeQuery(String sqlString, String resultSetMapping) {
		return delegate.createNativeQuery( sqlString, resultSetMapping );
	}

	@Override
	public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
		return delegate.createNamedStoredProcedureQuery( name );
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
		return delegate.createNamedStoredProcedureQuery( procedureName );
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
		return delegate.createStoredProcedureQuery( procedureName, resultClasses );
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
		return delegate.createStoredProcedureQuery( procedureName, resultSetMappings );
	}

	@Override
	public void joinTransaction() {
		delegate.joinTransaction();
	}

	@Override
	public boolean isJoinedToTransaction() {
		return delegate.isJoinedToTransaction();
	}

	@Override
	public <T> T unwrap(Class<T> cls) {
		return delegate.unwrap( cls );
	}

	@Override
	public Object getDelegate() {
		return delegate;
	}

	@Override
	public NativeQueryImplementor createSQLQuery(String queryString) {
		return delegate.createSQLQuery( queryString );
	}

	@Override
	public ProcedureCall getNamedProcedureCall(String name) {
		return delegate.getNamedProcedureCall( name );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName) {
		return delegate.createStoredProcedureCall( procedureName );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, Class... resultClasses) {
		return delegate.createStoredProcedureCall( procedureName, resultClasses );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
		return delegate.createStoredProcedureCall( procedureName, resultSetMappings );
	}

	@Override
	public Criteria createCriteria(Class persistentClass) {
		return delegate.createCriteria( persistentClass );
	}

	@Override
	public Criteria createCriteria(Class persistentClass, String alias) {
		return delegate.createCriteria( persistentClass, alias );
	}

	@Override
	public Criteria createCriteria(String entityName) {
		return delegate.createCriteria( entityName );
	}

	@Override
	public Criteria createCriteria(String entityName, String alias) {
		return delegate.createCriteria( entityName, alias );
	}

	@Override
	public SharedSessionBuilder sessionWithOptions() {
		return delegate.sessionWithOptions();
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return delegate.getSessionFactory();
	}

	@Override
	public void close() throws HibernateException {
		delegate.close();
	}

	@Override
	public void cancelQuery() throws HibernateException {
		delegate.cancelQuery();
	}

	@Override
	public boolean isDirty() throws HibernateException {
		return delegate.isDirty();
	}

	@Override
	public boolean isDefaultReadOnly() {
		return delegate.isDefaultReadOnly();
	}

	@Override
	public void setDefaultReadOnly(boolean readOnly) {
		delegate.setDefaultReadOnly( readOnly );
	}

	@Override
	public Serializable getIdentifier(Object object) {
		return delegate.getIdentifier( object );
	}

	@Override
	public boolean contains(String entityName, Object object) {
		return delegate.contains( entityName, object );
	}

	@Override
	public boolean contains(Object object) {
		return delegate.contains( object );
	}

	@Override
	public LockModeType getLockMode(Object entity) {
		return delegate.getLockMode( entity );
	}

	@Override
	public void setProperty(String propertyName, Object value) {
		delegate.setProperty( propertyName, value );
	}

	@Override
	public Map<String, Object> getProperties() {
		return delegate.getProperties();
	}

	@Override
	public void evict(Object object) {
		delegate.evict( object );
	}

	@Override
	public <T> T load(Class<T> theClass, Serializable id, LockMode lockMode) {
		return delegate.load( theClass, id, lockMode );
	}

	@Override
	public <T> T load(Class<T> theClass, Serializable id, LockOptions lockOptions) {
		return delegate.load( theClass, id, lockOptions );
	}

	@Override
	public Object load(String entityName, Serializable id, LockMode lockMode) {
		return delegate.load( entityName, id, lockMode );
	}

	@Override
	public Object load(String entityName, Serializable id, LockOptions lockOptions) {
		return delegate.load( entityName, id, lockOptions );
	}

	@Override
	public <T> T load(Class<T> theClass, Serializable id) {
		return delegate.load( theClass, id );
	}

	@Override
	public Object load(String entityName, Serializable id) {
		return delegate.load( entityName, id );
	}

	@Override
	public void load(Object object, Serializable id) {
		delegate.load( object, id );
	}

	@Override
	public void replicate(Object object, ReplicationMode replicationMode) {
		delegate.replicate( object, replicationMode );
	}

	@Override
	public void replicate(String entityName, Object object, ReplicationMode replicationMode) {
		delegate.replicate( entityName, object, replicationMode );
	}

	@Override
	public Serializable save(Object object) {
		return delegate.save( object );
	}

	@Override
	public Serializable save(String entityName, Object object) {
		return delegate.save( entityName, object );
	}

	@Override
	public void saveOrUpdate(Object object) {
		delegate.saveOrUpdate( object );
	}

	@Override
	public void saveOrUpdate(String entityName, Object object) {
		delegate.saveOrUpdate( entityName, object );
	}

	@Override
	public void update(Object object) {
		delegate.update( object );
	}

	@Override
	public void update(String entityName, Object object) {
		delegate.update( entityName, object );
	}

	@Override
	public Object merge(Object object) {
		return delegate.merge( object );
	}

	@Override
	public Object merge(String entityName, Object object) {
		return delegate.merge( entityName, object );
	}

	@Override
	public void persist(Object object) {
		delegate.persist( object );
	}

	@Override
	public void remove(Object entity) {
		delegate.remove( entity );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey) {
		return delegate.find( entityClass, primaryKey );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
		return delegate.find( entityClass, primaryKey, properties );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
		return delegate.find( entityClass, primaryKey, lockMode );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
		return delegate.find( entityClass, primaryKey, lockMode, properties );
	}

	@Override
	public <T> T getReference(Class<T> entityClass, Object primaryKey) {
		return delegate.getReference( entityClass, primaryKey );
	}

	@Override
	public void persist(String entityName, Object object) {
		delegate.persist( entityName, object );
	}

	@Override
	public void delete(Object object) {
		delegate.delete( object );
	}

	@Override
	public void delete(String entityName, Object object) {
		delegate.delete( entityName, object );
	}

	@Override
	public void lock(Object object, LockMode lockMode) {
		delegate.lock( object, lockMode );
	}

	@Override
	public void lock(String entityName, Object object, LockMode lockMode) {
		delegate.lock( entityName, object, lockMode );
	}

	@Override
	public LockRequest buildLockRequest(LockOptions lockOptions) {
		return delegate.buildLockRequest( lockOptions );
	}

	@Override
	public void refresh(Object object) {
		delegate.refresh( object );
	}

	@Override
	public void refresh(Object entity, Map<String, Object> properties) {
		delegate.refresh( entity, properties );
	}

	@Override
	public void refresh(Object entity, LockModeType lockMode) {
		delegate.refresh( entity, lockMode );
	}

	@Override
	public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
		delegate.refresh( entity, lockMode, properties );
	}

	@Override
	public void refresh(String entityName, Object object) {
		delegate.refresh( entityName, object );
	}

	@Override
	public void refresh(Object object, LockMode lockMode) {
		delegate.refresh( object, lockMode );
	}

	@Override
	public void refresh(Object object, LockOptions lockOptions) {
		delegate.refresh( object, lockOptions );
	}

	@Override
	public void refresh(String entityName, Object object, LockOptions lockOptions) {
		delegate.refresh( entityName, object, lockOptions );
	}

	@Override
	public LockMode getCurrentLockMode(Object object) {
		return delegate.getCurrentLockMode( object );
	}

	@Override
	public org.hibernate.query.Query createFilter(Object collection, String queryString) {
		return delegate.createFilter( collection, queryString );
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public void detach(Object entity) {
		delegate.detach( entity );
	}

	@Override
	public <T> T get(Class<T> theClass, Serializable id) {
		return delegate.get( theClass, id );
	}

	@Override
	public <T> T get(Class<T> theClass, Serializable id, LockMode lockMode) {
		return delegate.get( theClass, id, lockMode );
	}

	@Override
	public <T> T get(Class<T> theClass, Serializable id, LockOptions lockOptions) {
		return delegate.get( theClass, id, lockOptions );
	}

	@Override
	public Object get(String entityName, Serializable id) {
		return delegate.get( entityName, id );
	}

	@Override
	public Object get(String entityName, Serializable id, LockMode lockMode) {
		return delegate.get( entityName, id, lockMode );
	}

	@Override
	public Object get(String entityName, Serializable id, LockOptions lockOptions) {
		return delegate.get( entityName, id, lockOptions );
	}

	@Override
	public String getEntityName(Object object) {
		return delegate.getEntityName( object );
	}

	@Override
	public IdentifierLoadAccess byId(String entityName) {
		return delegate.byId( entityName );
	}

	@Override
	public <T> MultiIdentifierLoadAccess<T> byMultipleIds(Class<T> entityClass) {
		return delegate.byMultipleIds( entityClass );
	}

	@Override
	public MultiIdentifierLoadAccess byMultipleIds(String entityName) {
		return delegate.byMultipleIds( entityName );
	}

	@Override
	public <T> IdentifierLoadAccess<T> byId(Class<T> entityClass) {
		return delegate.byId( entityClass );
	}

	@Override
	public NaturalIdLoadAccess byNaturalId(String entityName) {
		return delegate.byNaturalId( entityName );
	}

	@Override
	public <T> NaturalIdLoadAccess<T> byNaturalId(Class<T> entityClass) {
		return delegate.byNaturalId( entityClass );
	}

	@Override
	public SimpleNaturalIdLoadAccess bySimpleNaturalId(String entityName) {
		return delegate.bySimpleNaturalId( entityName );
	}

	@Override
	public <T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(Class<T> entityClass) {
		return delegate.bySimpleNaturalId( entityClass );
	}

	@Override
	public Filter enableFilter(String filterName) {
		return delegate.enableFilter( filterName );
	}

	@Override
	public Filter getEnabledFilter(String filterName) {
		return delegate.getEnabledFilter( filterName );
	}

	@Override
	public void disableFilter(String filterName) {
		delegate.disableFilter( filterName );
	}

	@Override
	public SessionStatistics getStatistics() {
		return delegate.getStatistics();
	}

	@Override
	public boolean isReadOnly(Object entityOrProxy) {
		return delegate.isReadOnly( entityOrProxy );
	}

	@Override
	public void setReadOnly(Object entityOrProxy, boolean readOnly) {
		delegate.setReadOnly( entityOrProxy, readOnly );
	}

	@Override
	public void doWork(Work work) throws HibernateException {
		delegate.doWork( work );
	}

	@Override
	public <T> T doReturningWork(ReturningWork<T> work) throws HibernateException {
		return delegate.doReturningWork( work );
	}

	@Override
	public Connection disconnect() {
		return delegate.disconnect();
	}

	@Override
	public void reconnect(Connection connection) {
		delegate.reconnect( connection );
	}

	@Override
	public boolean isFetchProfileEnabled(String name) throws UnknownProfileException {
		return delegate.isFetchProfileEnabled( name );
	}

	@Override
	public void enableFetchProfile(String name) throws UnknownProfileException {
		delegate.enableFetchProfile( name );
	}

	@Override
	public void disableFetchProfile(String name) throws UnknownProfileException {
		delegate.disableFetchProfile( name );
	}

	@Override
	public TypeHelper getTypeHelper() {
		return delegate.getTypeHelper();
	}

	@Override
	public LobHelper getLobHelper() {
		return delegate.getLobHelper();
	}

	@Override
	public void addEventListeners(SessionEventListener... listeners) {
		delegate.addEventListeners( listeners );
	}

	@Override
	public boolean isFlushBeforeCompletionEnabled() {
		return delegate.isFlushBeforeCompletionEnabled();
	}

	@Override
	public ActionQueue getActionQueue() {
		return delegate.getActionQueue();
	}

	@Override
	public Object instantiate(EntityPersister persister, Serializable id) throws HibernateException {
		return delegate.instantiate( persister, id );
	}

	@Override
	public void forceFlush(EntityEntry e) throws HibernateException {
		delegate.forceFlush( e );
	}

	@Override
	public void merge(String entityName, Object object, Map copiedAlready) throws HibernateException {
		delegate.merge( entityName, object, copiedAlready );
	}

	@Override
	public void persist(String entityName, Object object, Map createdAlready) throws HibernateException {
		delegate.persist( entityName, object, createdAlready );
	}

	@Override
	public void persistOnFlush(String entityName, Object object, Map copiedAlready) {
		delegate.persistOnFlush( entityName, object, copiedAlready );
	}

	@Override
	public void refresh(String entityName, Object object, Map refreshedAlready) throws HibernateException {
		delegate.refresh( entityName, object, refreshedAlready );
	}

	@Override
	public void delete(String entityName, Object child, boolean isCascadeDeleteEnabled, Set transientEntities) {
		delegate.delete( entityName, child, isCascadeDeleteEnabled, transientEntities );
	}

	@Override
	public void removeOrphanBeforeUpdates(String entityName, Object child) {
		delegate.removeOrphanBeforeUpdates( entityName, child );
	}

	@Override
	public SessionImplementor getSession() {
		return this;
	}

	@Override
	public boolean useStreamForLobBinding() {
		return delegate.useStreamForLobBinding();
	}

	@Override
	public LobCreator getLobCreator() {
		return delegate.getLobCreator();
	}

	@Override
	public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		return delegate.remapSqlTypeDescriptor( sqlTypeDescriptor );
	}

	@Override
	public Integer getJdbcBatchSize() {
		return delegate.getJdbcBatchSize();
	}

	@Override
	public void setJdbcBatchSize(Integer jdbcBatchSize) {
		delegate.setJdbcBatchSize( jdbcBatchSize );
	}

	@Override
	public TimeZone getJdbcTimeZone() {
		return delegate.getJdbcTimeZone();
	}
}
