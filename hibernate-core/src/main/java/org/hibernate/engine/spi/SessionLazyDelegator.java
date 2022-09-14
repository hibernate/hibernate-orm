/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.LobHelper;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.Query;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.Transaction;
import org.hibernate.TypeHelper;
import org.hibernate.UnknownProfileException;
import org.hibernate.graph.RootGraph;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.NativeQuery;
import org.hibernate.stat.SessionStatistics;

/**
 * This helper class allows decorating a Session instance, while the
 * instance itself is lazily provided via a {@code Supplier}.
 * When the decorated instance is readily available, one
 * should prefer using {@code SessionDelegatorBaseImpl}.
 *
 * Another difference with SessionDelegatorBaseImpl is that
 * this type only implements Session.
 *
 * @author <a href="mailto:sanne@hibernate.org">Sanne Grinovero</a> (C) 2022 Red Hat Inc.
 */
public class SessionLazyDelegator implements Session {

	private final Supplier<Session> lazySession;

	public SessionLazyDelegator(Supplier<Session> lazySessionLookup){
		this.lazySession = lazySessionLookup;
	}

	@Override
	public SharedSessionBuilder sessionWithOptions() {
		return lazySession.get().sessionWithOptions();
	}

	@Override
	public void flush() throws HibernateException {
		lazySession.get().flush();
	}

	@Override
	@Deprecated
	public void setFlushMode(FlushMode flushMode) {
		lazySession.get().setFlushMode( flushMode );
	}

	@Override
	public FlushModeType getFlushMode() {
		return lazySession.get().getFlushMode();
	}

	@Override
	public void setHibernateFlushMode(FlushMode flushMode) {
		lazySession.get().setHibernateFlushMode( flushMode );
	}

	@Override
	public FlushMode getHibernateFlushMode() {
		return lazySession.get().getHibernateFlushMode();
	}

	@Override
	public void setCacheMode(CacheMode cacheMode) {
		lazySession.get().setCacheMode( cacheMode );
	}

	@Override
	public CacheMode getCacheMode() {
		return lazySession.get().getCacheMode();
	}

	@Override
	public SessionFactory getSessionFactory() {
		return lazySession.get().getSessionFactory();
	}

	@Override
	public void cancelQuery() throws HibernateException {
		lazySession.get().cancelQuery();
	}

	@Override
	public boolean isDirty() throws HibernateException {
		return lazySession.get().isDirty();
	}

	@Override
	public boolean isDefaultReadOnly() {
		return lazySession.get().isDefaultReadOnly();
	}

	@Override
	public void setDefaultReadOnly(boolean readOnly) {
		lazySession.get().setDefaultReadOnly( readOnly );
	}

	@Override
	public Serializable getIdentifier(Object object) {
		return lazySession.get().getIdentifier( object );
	}

	@Override
	public boolean contains(String entityName, Object object) {
		return lazySession.get().contains( entityName, object );
	}

	@Override
	public void evict(Object object) {
		lazySession.get().evict( object );
	}

	@Override
	public <T> T load(Class<T> theClass, Serializable id, LockMode lockMode) {
		return lazySession.get().load( theClass, id, lockMode );
	}

	@Override
	public <T> T load(Class<T> theClass, Serializable id, LockOptions lockOptions) {
		return lazySession.get().load( theClass, id, lockOptions );
	}

	@Override
	public Object load(String entityName, Serializable id, LockMode lockMode) {
		return lazySession.get().load( entityName, id, lockMode );
	}

	@Override
	public Object load(String entityName, Serializable id, LockOptions lockOptions) {
		return lazySession.get().load( entityName, id, lockOptions );
	}

	@Override
	public <T> T load(Class<T> theClass, Serializable id) {
		return lazySession.get().load( theClass, id );
	}

	@Override
	public Object load(String entityName, Serializable id) {
		return lazySession.get().load( entityName, id );
	}

	@Override
	public void load(Object object, Serializable id) {
		lazySession.get().load( object, id );
	}

	@Override
	public void replicate(Object object, ReplicationMode replicationMode) {
		lazySession.get().replicate( object, replicationMode );
	}

	@Override
	public void replicate(String entityName, Object object, ReplicationMode replicationMode) {
		lazySession.get().replicate( entityName, object, replicationMode );
	}

	@Override
	public Serializable save(Object object) {
		return lazySession.get().save( object );
	}

	@Override
	public Serializable save(String entityName, Object object) {
		return lazySession.get().save( entityName, object );
	}

	@Override
	public void saveOrUpdate(Object object) {
		lazySession.get().saveOrUpdate( object );
	}

	@Override
	public void saveOrUpdate(String entityName, Object object) {
		lazySession.get().saveOrUpdate( entityName, object );
	}

	@Override
	public void update(Object object) {
		lazySession.get().update( object );
	}

	@Override
	public void update(String entityName, Object object) {
		lazySession.get().update( entityName, object );
	}

	@Override
	public Object merge(Object object) {
		return lazySession.get().merge( object );
	}

	@Override
	public Object merge(String entityName, Object object) {
		return lazySession.get().merge( entityName, object );
	}

	@Override
	public void persist(Object object) {
		lazySession.get().persist( object );
	}

	@Override
	public void persist(String entityName, Object object) {
		lazySession.get().persist( entityName, object );
	}

	@Override
	public void delete(Object object) {
		lazySession.get().delete( object );
	}

	@Override
	public void delete(String entityName, Object object) {
		lazySession.get().delete( entityName, object );
	}

	@Override
	public void lock(Object object, LockMode lockMode) {
		lazySession.get().lock( object, lockMode );
	}

	@Override
	public void lock(String entityName, Object object, LockMode lockMode) {
		lazySession.get().lock( entityName, object, lockMode );
	}

	@Override
	public LockRequest buildLockRequest(LockOptions lockOptions) {
		return lazySession.get().buildLockRequest( lockOptions );
	}

	@Override
	public void refresh(Object object) {
		lazySession.get().refresh( object );
	}

	@Override
	public void refresh(String entityName, Object object) {
		lazySession.get().refresh( entityName, object );
	}

	@Override
	public void refresh(Object object, LockMode lockMode) {
		lazySession.get().refresh( object, lockMode );
	}

	@Override
	public void refresh(Object object, LockOptions lockOptions) {
		lazySession.get().refresh( object, lockOptions );
	}

	@Override
	public void refresh(String entityName, Object object, LockOptions lockOptions) {
		lazySession.get().refresh( entityName, object, lockOptions );
	}

	@Override
	public LockMode getCurrentLockMode(Object object) {
		return lazySession.get().getCurrentLockMode( object );
	}

	@Override
	@Deprecated
	public Query createFilter(Object collection, String queryString) {
		return lazySession.get().createFilter( collection, queryString );
	}

	@Override
	public void clear() {
		lazySession.get().clear();
	}

	@Override
	public <T> T get(Class<T> entityType, Serializable id) {
		return lazySession.get().get( entityType, id );
	}

	@Override
	public <T> T get(Class<T> entityType, Serializable id, LockMode lockMode) {
		return lazySession.get().get( entityType, id, lockMode );
	}

	@Override
	public <T> T get(Class<T> entityType, Serializable id, LockOptions lockOptions) {
		return lazySession.get().get( entityType, id, lockOptions );
	}

	@Override
	public Object get(String entityName, Serializable id) {
		return lazySession.get().get( entityName, id );
	}

	@Override
	public Object get(String entityName, Serializable id, LockMode lockMode) {
		return lazySession.get().get( entityName, id, lockMode );
	}

	@Override
	public Object get(String entityName, Serializable id, LockOptions lockOptions) {
		return lazySession.get().get( entityName, id, lockOptions );
	}

	@Override
	public String getEntityName(Object object) {
		return lazySession.get().getEntityName( object );
	}

	@Override
	public <T> T getReference(T object) {
		return lazySession.get().getReference( object );
	}

	@Override
	public IdentifierLoadAccess byId(String entityName) {
		return lazySession.get().byId( entityName );
	}

	@Override
	public <T> MultiIdentifierLoadAccess<T> byMultipleIds(Class<T> entityClass) {
		return lazySession.get().byMultipleIds( entityClass );
	}

	@Override
	public MultiIdentifierLoadAccess byMultipleIds(String entityName) {
		return lazySession.get().byMultipleIds( entityName );
	}

	@Override
	public <T> IdentifierLoadAccess<T> byId(Class<T> entityClass) {
		return lazySession.get().byId( entityClass );
	}

	@Override
	public NaturalIdLoadAccess byNaturalId(String entityName) {
		return lazySession.get().byNaturalId( entityName );
	}

	@Override
	public <T> NaturalIdLoadAccess<T> byNaturalId(Class<T> entityClass) {
		return lazySession.get().byNaturalId( entityClass );
	}

	@Override
	public SimpleNaturalIdLoadAccess bySimpleNaturalId(String entityName) {
		return lazySession.get().bySimpleNaturalId( entityName );
	}

	@Override
	public <T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(Class<T> entityClass) {
		return lazySession.get().bySimpleNaturalId( entityClass );
	}

	@Override
	public Filter enableFilter(String filterName) {
		return lazySession.get().enableFilter( filterName );
	}

	@Override
	public Filter getEnabledFilter(String filterName) {
		return lazySession.get().getEnabledFilter( filterName );
	}

	@Override
	public void disableFilter(String filterName) {
		lazySession.get().disableFilter( filterName );
	}

	@Override
	public SessionStatistics getStatistics() {
		return lazySession.get().getStatistics();
	}

	@Override
	public boolean isReadOnly(Object entityOrProxy) {
		return lazySession.get().isReadOnly( entityOrProxy );
	}

	@Override
	public void setReadOnly(Object entityOrProxy, boolean readOnly) {
		lazySession.get().setReadOnly( entityOrProxy, readOnly );
	}

	@Override
	public <T> RootGraph<T> createEntityGraph(Class<T> rootType) {
		return lazySession.get().createEntityGraph( rootType );
	}

	@Override
	public RootGraph<?> createEntityGraph(String graphName) {
		return lazySession.get().createEntityGraph( graphName );
	}

	@Override
	public RootGraph<?> getEntityGraph(String graphName) {
		return lazySession.get().getEntityGraph( graphName );
	}

	@Override
	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
		return lazySession.get().getEntityGraphs( entityClass );
	}

	@Override
	public Connection disconnect() {
		return lazySession.get().disconnect();
	}

	@Override
	public void reconnect(Connection connection) {
		lazySession.get().reconnect( connection );
	}

	@Override
	public boolean isFetchProfileEnabled(String name) throws UnknownProfileException {
		return lazySession.get().isFetchProfileEnabled( name );
	}

	@Override
	public void enableFetchProfile(String name) throws UnknownProfileException {
		lazySession.get().enableFetchProfile( name );
	}

	@Override
	public void disableFetchProfile(String name) throws UnknownProfileException {
		lazySession.get().disableFetchProfile( name );
	}

	@Override
	public TypeHelper getTypeHelper() {
		return lazySession.get().getTypeHelper();
	}

	@Override
	public LobHelper getLobHelper() {
		return lazySession.get().getLobHelper();
	}

	@Override
	public void addEventListeners(SessionEventListener... listeners) {
		lazySession.get().addEventListeners( listeners );
	}

	@Override
	public <T> org.hibernate.query.Query<T> createQuery(String queryString, Class<T> resultType) {
		return lazySession.get().createQuery( queryString, resultType );
	}

	@Override
	public <T> org.hibernate.query.Query<T> createQuery(CriteriaQuery<T> criteriaQuery) {
		return lazySession.get().createQuery( criteriaQuery );
	}

	@Override
	public org.hibernate.query.Query createQuery(CriteriaUpdate updateQuery) {
		return lazySession.get().createQuery( updateQuery );
	}

	@Override
	public org.hibernate.query.Query createQuery(CriteriaDelete deleteQuery) {
		return lazySession.get().createQuery( deleteQuery );
	}

	@Override
	public <T> org.hibernate.query.Query<T> createNamedQuery(String name, Class<T> resultType) {
		return lazySession.get().createNamedQuery( name, resultType );
	}

	@Override
	public NativeQuery createSQLQuery(String queryString) {
		return lazySession.get().createSQLQuery( queryString );
	}

	@Override
	public String getTenantIdentifier() {
		return lazySession.get().getTenantIdentifier();
	}

	@Override
	public void close() throws HibernateException {
		lazySession.get().close();
	}

	@Override
	public boolean isOpen() {
		return lazySession.get().isOpen();
	}

	@Override
	public boolean isConnected() {
		return lazySession.get().isConnected();
	}

	@Override
	public Transaction beginTransaction() {
		return lazySession.get().beginTransaction();
	}

	@Override
	public Transaction getTransaction() {
		return lazySession.get().getTransaction();
	}

	@Override
	public org.hibernate.query.Query createQuery(String queryString) {
		return lazySession.get().createQuery( queryString );
	}

	@Override
	public org.hibernate.query.Query getNamedQuery(String queryName) {
		return lazySession.get().getNamedQuery( queryName );
	}

	@Override
	public ProcedureCall getNamedProcedureCall(String name) {
		return lazySession.get().getNamedProcedureCall( name );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName) {
		return lazySession.get().createStoredProcedureCall( procedureName );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, Class... resultClasses) {
		return lazySession.get().createStoredProcedureCall( procedureName, resultClasses );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
		return lazySession.get().createStoredProcedureCall( procedureName, resultSetMappings );
	}

	@Override
	@Deprecated
	public Criteria createCriteria(Class persistentClass) {
		return lazySession.get().createCriteria( persistentClass );
	}

	@Override
	@Deprecated
	public Criteria createCriteria(Class persistentClass, String alias) {
		return lazySession.get().createCriteria( persistentClass, alias );
	}

	@Override
	@Deprecated
	public Criteria createCriteria(String entityName) {
		return lazySession.get().createCriteria( entityName );
	}

	@Override
	@Deprecated
	public Criteria createCriteria(String entityName, String alias) {
		return lazySession.get().createCriteria( entityName, alias );
	}

	@Override
	public Integer getJdbcBatchSize() {
		return lazySession.get().getJdbcBatchSize();
	}

	@Override
	public void setJdbcBatchSize(Integer jdbcBatchSize) {
		lazySession.get().setJdbcBatchSize( jdbcBatchSize );
	}

	@Override
	public void doWork(Work work) throws HibernateException {
		lazySession.get().doWork( work );
	}

	@Override
	public <T> T doReturningWork(ReturningWork<T> work) throws HibernateException {
		return lazySession.get().doReturningWork( work );
	}

	@Override
	public org.hibernate.query.Query createNamedQuery(String name) {
		return lazySession.get().createNamedQuery( name );
	}

	@Override
	public NativeQuery createNativeQuery(String sqlString) {
		return lazySession.get().createNativeQuery( sqlString );
	}

	@Override
	public NativeQuery createNativeQuery(String sqlString, String resultSetMapping) {
		return lazySession.get().createNativeQuery( sqlString, resultSetMapping );
	}

	@Override
	@Deprecated
	public Query getNamedSQLQuery(String name) {
		return lazySession.get().getNamedSQLQuery( name );
	}

	@Override
	public NativeQuery getNamedNativeQuery(String name) {
		return lazySession.get().getNamedNativeQuery( name );
	}
	@Override
	public void remove(Object entity) {
		lazySession.get().remove( entity );
	}
	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey) {
		return lazySession.get().find( entityClass, primaryKey );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
		return lazySession.get().find( entityClass, primaryKey, properties );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
		return lazySession.get().find( entityClass, primaryKey, lockMode );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
		return lazySession.get().find( entityClass, primaryKey, lockMode, properties );
	}

	@Override
	public <T> T getReference(Class<T> entityClass, Object primaryKey) {
		return lazySession.get().getReference( entityClass, primaryKey );
	}

	@Override
	public void setFlushMode(FlushModeType flushMode) {
		lazySession.get().setFlushMode( flushMode );
	}

	@Override
	public void lock(Object entity, LockModeType lockMode) {
		lazySession.get().lock( entity, lockMode );
	}

	@Override
	public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
		lazySession.get().lock( entity, lockMode, properties );
	}

	@Override
	public void refresh(Object entity, Map<String, Object> properties) {
		lazySession.get().refresh( entity, properties );
	}

	@Override
	public void refresh(Object entity, LockModeType lockMode) {
		lazySession.get().refresh( entity, lockMode );
	}

	@Override
	public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
		lazySession.get().refresh( entity, lockMode, properties );
	}

	@Override
	public void detach(Object entity) {
		lazySession.get().detach( entity );
	}

	@Override
	public boolean contains(Object entity) {
		return lazySession.get().contains( entity );
	}

	@Override
	public LockModeType getLockMode(Object entity) {
		return lazySession.get().getLockMode( entity );
	}

	@Override
	public void setProperty(String propertyName, Object value) {
		lazySession.get().setProperty( propertyName, value );
	}

	@Override
	public Map<String, Object> getProperties() {
		return lazySession.get().getProperties();
	}

	@Override
	public NativeQuery createNativeQuery(String sqlString, Class resultClass) {
		return lazySession.get().createNativeQuery( sqlString, resultClass );
	}

	@Override
	public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
		return lazySession.get().createNamedStoredProcedureQuery( name );
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
		return lazySession.get().createStoredProcedureQuery( procedureName );
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
		return lazySession.get().createStoredProcedureQuery( procedureName, resultClasses );
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
		return lazySession.get().createStoredProcedureQuery( procedureName, resultSetMappings );
	}

	@Override
	public void joinTransaction() {
		lazySession.get().joinTransaction();
	}

	@Override
	public boolean isJoinedToTransaction() {
		return lazySession.get().isJoinedToTransaction();
	}

	@Override
	public <T> T unwrap(Class<T> cls) {
		return lazySession.get().unwrap( cls );
	}

	@Override
	public Object getDelegate() {
		return lazySession.get().getDelegate();
	}

	@Override
	public EntityManagerFactory getEntityManagerFactory() {
		return lazySession.get().getEntityManagerFactory();
	}

	@Override
	public CriteriaBuilder getCriteriaBuilder() {
		return lazySession.get().getCriteriaBuilder();
	}

	@Override
	public Metamodel getMetamodel() {
		return lazySession.get().getMetamodel();
	}

	@Override
	public Session getSession() {
		return lazySession.get().getSession();
	}
}
