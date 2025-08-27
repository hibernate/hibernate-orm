/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.ConnectionConsumer;
import jakarta.persistence.ConnectionFunction;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FindOption;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockOption;
import jakarta.persistence.RefreshOption;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.LobHelper;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.NaturalIdMultiLoadAccess;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.Transaction;
import org.hibernate.UnknownProfileException;
import org.hibernate.graph.RootGraph;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaInsert;
import org.hibernate.stat.SessionStatistics;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * This helper class allows decorating a Session instance, while the
 * instance itself is lazily provided via a {@code Supplier}.
 * When the decorated instance is readily available, one
 * should prefer using {@code SessionDelegatorBaseImpl}.
 * <p>
 * Another difference with SessionDelegatorBaseImpl is that
 * this type only implements Session.
 *
 * @author Sanne Grinovero
 */
public class SessionLazyDelegator implements Session {

	private final Supplier<Session> lazySession;

	@Override
	public SessionFactory getFactory() {
		return lazySession.get().getFactory();
	}

	public SessionLazyDelegator(Supplier<Session> lazySessionLookup){
		this.lazySession = lazySessionLookup;
	}

	@Override
	public void flush() {
		this.lazySession.get().flush();
	}

	@Override
	public void setFlushMode(FlushModeType flushMode) {
		this.lazySession.get().setFlushMode( flushMode );
	}

	@Override
	public void setHibernateFlushMode(FlushMode flushMode) {
		this.lazySession.get().setHibernateFlushMode( flushMode );
	}

	@Override
	public FlushModeType getFlushMode() {
		return this.lazySession.get().getFlushMode();
	}

	@Override
	public FlushMode getHibernateFlushMode() {
		return this.lazySession.get().getHibernateFlushMode();
	}

	@Override
	public void setCacheMode(CacheMode cacheMode) {
		this.lazySession.get().setCacheMode( cacheMode );
	}

	@Override
	public void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		this.lazySession.get().setCacheRetrieveMode( cacheRetrieveMode );
	}

	@Override
	public void setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		this.lazySession.get().setCacheStoreMode( cacheStoreMode );
	}

	@Override
	public CacheStoreMode getCacheStoreMode() {
		return this.lazySession.get().getCacheStoreMode();
	}

	@Override
	public CacheRetrieveMode getCacheRetrieveMode() {
		return this.lazySession.get().getCacheRetrieveMode();
	}

	@Override
	public CacheMode getCacheMode() {
		return this.lazySession.get().getCacheMode();
	}

	@Override
	public SessionFactory getSessionFactory() {
		return this.lazySession.get().getSessionFactory();
	}

	@Override
	public void cancelQuery() {
		this.lazySession.get().cancelQuery();
	}

	@Override
	public boolean isDirty() {
		return this.lazySession.get().isDirty();
	}

	@Override
	public boolean isDefaultReadOnly() {
		return this.lazySession.get().isDefaultReadOnly();
	}

	@Override
	public void setDefaultReadOnly(boolean readOnly) {
		this.lazySession.get().setDefaultReadOnly( readOnly );
	}

	@Override
	public Object getIdentifier(Object object) {
		return this.lazySession.get().getIdentifier( object );
	}

	@Override
	public boolean contains(String entityName, Object object) {
		return this.lazySession.get().contains( entityName, object );
	}

	@Override
	public void detach(Object object) {
		this.lazySession.get().detach( object );
	}

	@Override
	public void evict(Object object) {
		this.lazySession.get().evict( object );
	}

	@Override
	public void load(Object object, Object id) {
		this.lazySession.get().load( object, id );
	}

	@Override
	@Deprecated
	public void replicate(Object object, ReplicationMode replicationMode) {
		this.lazySession.get().replicate( object, replicationMode );
	}

	@Override
	@Deprecated
	public void replicate(String entityName, Object object, ReplicationMode replicationMode) {
		this.lazySession.get().replicate( entityName, object, replicationMode );
	}

	@Override
	public <T> T merge(T object) {
		return this.lazySession.get().merge( object );
	}

	@Override
	public <T> T merge(String entityName, T object) {
		return this.lazySession.get().merge( entityName, object );
	}

	@Override
	public <T> T merge(T object, EntityGraph<?> loadGraph) {
		return this.lazySession.get().merge( object, loadGraph );
	}

	@Override
	public void persist(Object object) {
		this.lazySession.get().persist( object );
	}

	@Override
	public void persist(String entityName, Object object) {
		this.lazySession.get().persist( entityName, object );
	}

	@Override
	public void lock(Object object, LockMode lockMode) {
		this.lazySession.get().lock( object, lockMode );
	}

	@Override
	public void lock(Object object, LockMode lockMode, LockOption... lockOptions) {
		this.lazySession.get().lock( object, lockMode, lockOptions );
	}

	@Override
	public void lock(Object object, LockOptions lockOptions) {
		this.lazySession.get().lock( object, lockOptions );
	}

	@Override
	public void refresh(Object object) {
		this.lazySession.get().refresh( object );
	}

	@Override
	public void refresh(Object object, LockOptions lockOptions) {
		this.lazySession.get().refresh( object, lockOptions );
	}

	@Override
	public void remove(Object object) {
		this.lazySession.get().remove( object );
	}

	@Override
	public LockMode getCurrentLockMode(Object object) {
		return this.lazySession.get().getCurrentLockMode( object );
	}

	@Override
	public void clear() {
		this.lazySession.get().clear();
	}

	@Override
	public <E> List<E> findMultiple(Class<E> entityType, List<?> ids, FindOption... options) {
		return this.lazySession.get().findMultiple( entityType, ids, options );
	}

	@Override
	public <E> List<E> findMultiple(EntityGraph<E> entityGraph, List<?> ids, FindOption... options) {
		return this.lazySession.get().findMultiple( entityGraph, ids, options );
	}

	@Override
	public <T> T get(Class<T> entityType, Object id) {
		return this.lazySession.get().get( entityType, id );
	}

	@Override
	public <T> T get(Class<T> entityType, Object id, LockMode lockMode) {
		return this.lazySession.get().get( entityType, id, lockMode );
	}

	@Override
	public Object get(String entityName, Object id) {
		return this.lazySession.get().get( entityName, id );
	}

	@Override
	public Object get(String entityName, Object id, LockMode lockMode) {
		return this.lazySession.get().get( entityName, id, lockMode );
	}

	@Override
	public <T> T get(Class<T> entityType, Object id, LockOptions lockOptions) {
		return this.lazySession.get().get( entityType, id, lockOptions );
	}

	@Override
	public Object get(String entityName, Object id, LockOptions lockOptions) {
		return this.lazySession.get().get( entityName, id, lockOptions );
	}

	@Override
	public String getEntityName(Object object) {
		return this.lazySession.get().getEntityName( object );
	}

	@Override
	public <T> T getReference(Class<T> entityType, Object id) {
		return this.lazySession.get().getReference( entityType, id );
	}

	@Override
	public Object getReference(String entityName, Object id) {
		return this.lazySession.get().getReference( entityName, id );
	}

	@Override
	public <T> T getReference(T object) {
		return this.lazySession.get().getReference( object );
	}

	@Override
	public <T> IdentifierLoadAccess<T> byId(String entityName) {
		return this.lazySession.get().byId( entityName );
	}

	@Override
	public <T> MultiIdentifierLoadAccess<T> byMultipleIds(Class<T> entityClass) {
		return this.lazySession.get().byMultipleIds( entityClass );
	}

	@Override
	public <T> MultiIdentifierLoadAccess<T> byMultipleIds(String entityName) {
		return this.lazySession.get().byMultipleIds( entityName );
	}

	@Override
	public <T> IdentifierLoadAccess<T> byId(Class<T> entityClass) {
		return this.lazySession.get().byId( entityClass );
	}

	@Override
	public <T> NaturalIdLoadAccess<T> byNaturalId(String entityName) {
		return this.lazySession.get().byNaturalId( entityName );
	}

	@Override
	public <T> NaturalIdLoadAccess<T> byNaturalId(Class<T> entityClass) {
		return this.lazySession.get().byNaturalId( entityClass );
	}

	@Override
	public <T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(String entityName) {
		return this.lazySession.get().bySimpleNaturalId( entityName );
	}

	@Override
	public <T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(Class<T> entityClass) {
		return this.lazySession.get().bySimpleNaturalId( entityClass );
	}

	@Override
	public <T> NaturalIdMultiLoadAccess<T> byMultipleNaturalId(Class<T> entityClass) {
		return this.lazySession.get().byMultipleNaturalId( entityClass );
	}

	@Override
	public <T> NaturalIdMultiLoadAccess<T> byMultipleNaturalId(String entityName) {
		return this.lazySession.get().byMultipleNaturalId( entityName );
	}

	@Override
	public Filter enableFilter(String filterName) {
		return this.lazySession.get().enableFilter( filterName );
	}

	@Override
	public Filter getEnabledFilter(String filterName) {
		return this.lazySession.get().getEnabledFilter( filterName );
	}

	@Override
	public void disableFilter(String filterName) {
		this.lazySession.get().disableFilter( filterName );
	}

	@Override
	public SessionStatistics getStatistics() {
		return this.lazySession.get().getStatistics();
	}

	@Override
	public boolean isReadOnly(Object entityOrProxy) {
		return this.lazySession.get().isReadOnly( entityOrProxy );
	}

	@Override
	public void setReadOnly(Object entityOrProxy, boolean readOnly) {
		this.lazySession.get().setReadOnly( entityOrProxy, readOnly );
	}

	@Override
	public boolean isFetchProfileEnabled(String name) throws UnknownProfileException {
		return this.lazySession.get().isFetchProfileEnabled( name );
	}

	@Override
	public void enableFetchProfile(String name) throws UnknownProfileException {
		this.lazySession.get().enableFetchProfile( name );
	}

	@Override
	public void disableFetchProfile(String name) throws UnknownProfileException {
		this.lazySession.get().disableFetchProfile( name );
	}

	@Override
	public LobHelper getLobHelper() {
		return this.lazySession.get().getLobHelper();
	}

	@Override
	public Collection<?> getManagedEntities() {
		return this.lazySession.get().getManagedEntities();
	}

	@Override
	public Collection<?> getManagedEntities(String entityName) {
		return this.lazySession.get().getManagedEntities( entityName );
	}

	@Override
	public <E> Collection<E> getManagedEntities(Class<E> entityType) {
		return this.lazySession.get().getManagedEntities( entityType );
	}

	@Override
	public <E> Collection<E> getManagedEntities(EntityType<E> entityType) {
		return this.lazySession.get().getManagedEntities( entityType );
	}

	@Override
	public SharedSessionBuilder sessionWithOptions() {
		return this.lazySession.get().sessionWithOptions();
	}

	@Override
	public void addEventListeners(SessionEventListener... listeners) {
		this.lazySession.get().addEventListeners( listeners );
	}

	@Override
	public <T> RootGraph<T> createEntityGraph(Class<T> rootType) {
		return this.lazySession.get().createEntityGraph( rootType );
	}

	@Override
	public RootGraph<?> createEntityGraph(String graphName) {
		return this.lazySession.get().createEntityGraph( graphName );
	}

	@Override
	public <T> RootGraph<T> createEntityGraph(Class<T> rootType, String graphName) {
		return this.lazySession.get().createEntityGraph( rootType, graphName );
	}

	@Override
	public RootGraph<?> getEntityGraph(String graphName) {
		return this.lazySession.get().getEntityGraph( graphName );
	}

	@Override
	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
		return this.lazySession.get().getEntityGraphs( entityClass );
	}

	@Override
	public <C> void runWithConnection(ConnectionConsumer<C> action) {
		this.lazySession.get().runWithConnection( action );
	}

	@Override
	public <C, T> T callWithConnection(ConnectionFunction<C, T> function) {
		return this.lazySession.get().callWithConnection( function );
	}

	@Override
	public <R> Query<R> createQuery(String queryString, Class<R> resultClass) {
		return this.lazySession.get().createQuery( queryString, resultClass );
	}

	@Override
	public <R> Query<R> createQuery(TypedQueryReference<R> typedQueryReference) {
		return this.lazySession.get().createQuery( typedQueryReference );
	}

	@SuppressWarnings("rawtypes")
	@Override
	@Deprecated
	public Query createQuery(String queryString) {
		return this.lazySession.get().createQuery( queryString );
	}

	@Override
	public <R> Query<R> createNamedQuery(String name, Class<R> resultClass) {
		return this.lazySession.get().createNamedQuery( name, resultClass );
	}

	@SuppressWarnings("rawtypes")
	@Override
	@Deprecated
	public Query createNamedQuery(String name) {
		return this.lazySession.get().createNamedQuery( name );
	}

	@Override
	public <R> Query<R> createQuery(CriteriaQuery<R> criteriaQuery) {
		return this.lazySession.get().createQuery( criteriaQuery );
	}

	@Override
	public <T> TypedQuery<T> createQuery(CriteriaSelect<T> selectQuery) {
		return this.lazySession.get().createQuery( selectQuery );
	}

	@SuppressWarnings("rawtypes")
	@Override
	@Deprecated
	public Query createQuery(CriteriaDelete deleteQuery) {
		return this.lazySession.get().createQuery( deleteQuery );
	}

	@SuppressWarnings("rawtypes")
	@Override
	@Deprecated
	public Query createQuery(CriteriaUpdate updateQuery) {
		return this.lazySession.get().createQuery( updateQuery );
	}

	@Override
	public String getTenantIdentifier() {
		return this.lazySession.get().getTenantIdentifier();
	}

	@Override
	public Object getTenantIdentifierValue() {
		return this.lazySession.get().getTenantIdentifierValue();
	}

	@Override
	public void close() throws HibernateException {
		this.lazySession.get().close();
	}

	@Override
	public boolean isOpen() {
		return this.lazySession.get().isOpen();
	}

	@Override
	public boolean isConnected() {
		return this.lazySession.get().isConnected();
	}

	@Override
	public Transaction beginTransaction() {
		return this.lazySession.get().beginTransaction();
	}

	@Override
	public Transaction getTransaction() {
		return this.lazySession.get().getTransaction();
	}

	@Override
	public ProcedureCall getNamedProcedureCall(String name) {
		return this.lazySession.get().getNamedProcedureCall( name );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName) {
		return this.lazySession.get().createStoredProcedureCall( procedureName );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, Class<?>... resultClasses) {
		return this.lazySession.get().createStoredProcedureCall( procedureName, resultClasses );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
		return this.lazySession.get().createStoredProcedureCall( procedureName, resultSetMappings );
	}

	@Override
	public ProcedureCall createNamedStoredProcedureQuery(String name) {
		return this.lazySession.get().createNamedStoredProcedureQuery( name );
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName) {
		return this.lazySession.get().createStoredProcedureQuery( procedureName );
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName, Class... resultClasses) {
		return this.lazySession.get().createStoredProcedureQuery( procedureName, resultClasses );
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
		return this.lazySession.get().createStoredProcedureQuery( procedureName, resultSetMappings );
	}

	@Override
	public Integer getJdbcBatchSize() {
		return this.lazySession.get().getJdbcBatchSize();
	}

	@Override
	public void setJdbcBatchSize(Integer jdbcBatchSize) {
		this.lazySession.get().setJdbcBatchSize( jdbcBatchSize );
	}

	@Override
	public int getFetchBatchSize() {
		return this.lazySession.get().getFetchBatchSize();
	}

	@Override
	public void setFetchBatchSize(int batchSize) {
		this.lazySession.get().setFetchBatchSize( batchSize );
	}

	@Override
	public boolean isSubselectFetchingEnabled() {
		return this.lazySession.get().isSubselectFetchingEnabled();
	}

	@Override
	public void setSubselectFetchingEnabled(boolean enabled) {
		this.lazySession.get().setSubselectFetchingEnabled( enabled );
	}

	@Override
	public HibernateCriteriaBuilder getCriteriaBuilder() {
		return this.lazySession.get().getCriteriaBuilder();
	}

	@Override
	public void doWork(Work work) throws HibernateException {
		this.lazySession.get().doWork( work );
	}

	@Override
	public <T> T doReturningWork(ReturningWork<T> work) throws HibernateException {
		return this.lazySession.get().doReturningWork( work );
	}

	@SuppressWarnings("rawtypes")
	@Override
	@Deprecated
	public NativeQuery createNativeQuery(String sqlString) {
		return this.lazySession.get().createNativeQuery( sqlString );
	}

	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	public NativeQuery createNativeQuery(String sqlString, Class resultClass) {
		return this.lazySession.get().createNativeQuery( sqlString, resultClass );
	}

	@Override
	public <R> NativeQuery<R> createNativeQuery(String sqlString, Class<R> resultClass, String tableAlias) {
		return this.lazySession.get().createNativeQuery( sqlString, resultClass, tableAlias );
	}

	@SuppressWarnings("rawtypes")
	@Override
	@Deprecated
	public NativeQuery createNativeQuery(String sqlString, String resultSetMappingName) {
		return this.lazySession.get().createNativeQuery( sqlString, resultSetMappingName );
	}

	@Override
	public <R> NativeQuery<R> createNativeQuery(String sqlString, String resultSetMappingName, Class<R> resultClass) {
		return this.lazySession.get().createNativeQuery( sqlString, resultSetMappingName, resultClass );
	}

	@Override
	public SelectionQuery<?> createSelectionQuery(String hqlString) {
		return this.lazySession.get().createSelectionQuery( hqlString );
	}

	@Override
	public <R> SelectionQuery<R> createSelectionQuery(String hqlString, Class<R> resultType) {
		return this.lazySession.get().createSelectionQuery( hqlString, resultType );
	}

	@Override
	public <R> SelectionQuery<R> createSelectionQuery(String hqlString, EntityGraph<R> resultGraph) {
		return this.lazySession.get().createSelectionQuery( hqlString, resultGraph );
	}

	@Override
	public <R> SelectionQuery<R> createSelectionQuery(CriteriaQuery<R> criteria) {
		return this.lazySession.get().createSelectionQuery( criteria );
	}

	@Override
	public MutationQuery createMutationQuery(String hqlString) {
		return this.lazySession.get().createMutationQuery( hqlString );
	}

	@Override
	public MutationQuery createMutationQuery(@SuppressWarnings("rawtypes") CriteriaUpdate updateQuery) {
		return this.lazySession.get().createMutationQuery( updateQuery );
	}

	@Override
	public MutationQuery createMutationQuery(@SuppressWarnings("rawtypes") CriteriaDelete deleteQuery) {
		return this.lazySession.get().createMutationQuery( deleteQuery );
	}

	@Override
	public MutationQuery createMutationQuery(@SuppressWarnings("rawtypes") JpaCriteriaInsert insert) {
		return this.lazySession.get().createMutationQuery( insert );
	}

	@Override
	public MutationQuery createNativeMutationQuery(String sqlString) {
		return this.lazySession.get().createNativeMutationQuery( sqlString );
	}

	@Override
	public SelectionQuery<?> createNamedSelectionQuery(String name) {
		return this.lazySession.get().createNamedSelectionQuery( name );
	}

	@Override
	public <R> SelectionQuery<R> createNamedSelectionQuery(String name, Class<R> resultType) {
		return this.lazySession.get().createNamedSelectionQuery( name, resultType );
	}

	@Override
	public MutationQuery createNamedMutationQuery(String name) {
		return this.lazySession.get().createNamedMutationQuery( name );
	}

	@SuppressWarnings("rawtypes")
	@Override
	@Deprecated
	public Query getNamedQuery(String queryName) {
		return this.lazySession.get().getNamedQuery( queryName );
	}

	@SuppressWarnings("rawtypes")
	@Override
	@Deprecated
	public NativeQuery getNamedNativeQuery(String name) {
		return this.lazySession.get().getNamedNativeQuery( name );
	}

	@SuppressWarnings("rawtypes")
	@Override
	@Deprecated
	public NativeQuery getNamedNativeQuery(String name, String resultSetMapping) {
		return this.lazySession.get().getNamedNativeQuery( name, resultSetMapping );
	}

	@Override
	public <T> @Nullable T find(Class<T> entityClass, Object primaryKey) {
		return this.lazySession.get().find( entityClass, primaryKey );
	}

	@Override
	public <T> @Nullable T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
		return this.lazySession.get().find( entityClass, primaryKey, properties );
	}

	@Override
	public <T> @Nullable T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
		return this.lazySession.get().find( entityClass, primaryKey, lockMode );
	}

	@Override
	public <T> @Nullable T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
		return this.lazySession.get().find( entityClass, primaryKey, lockMode, properties );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, FindOption... options) {
		return this.lazySession.get().find( entityClass, primaryKey, options );
	}

	@Override
	public <T> T find(EntityGraph<T> entityGraph, Object primaryKey, FindOption... options) {
		return this.lazySession.get().find( entityGraph, primaryKey, options );
	}

	@Override
	public Object find(String entityName, Object primaryKey) {
		return this.lazySession.get().find( entityName, primaryKey );
	}

	@Override
	public Object find(String entityName, Object primaryKey, FindOption... options) {
		return this.lazySession.get().find( entityName, primaryKey, options );
	}

	@Override
	public void lock(Object entity, LockModeType lockMode) {
		this.lazySession.get().lock( entity, lockMode );
	}

	@Override
	public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
		this.lazySession.get().lock( entity, lockMode, properties );
	}

	@Override
	public void lock(Object entity, LockModeType lockMode, LockOption... options) {
		this.lazySession.get().lock( entity, lockMode, options );
	}

	@Override
	public void refresh(Object entity, Map<String, Object> properties) {
		this.lazySession.get().refresh( entity, properties );
	}

	@Override
	public void refresh(Object entity, LockModeType lockMode) {
		this.lazySession.get().refresh( entity, lockMode );
	}

	@Override
	public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
		this.lazySession.get().refresh( entity, lockMode, properties );
	}

	@Override
	public void refresh(Object entity, RefreshOption... options) {
		this.lazySession.get().refresh( entity, options );
	}

	@Override
	public boolean contains(Object entity) {
		return this.lazySession.get().contains( entity );
	}

	@Override
	public LockModeType getLockMode(Object entity) {
		return this.lazySession.get().getLockMode( entity );
	}

	@Override
	public void setProperty(String propertyName, Object value) {
		this.lazySession.get().setProperty( propertyName, value );
	}

	@Override
	public Map<String, Object> getProperties() {
		return this.lazySession.get().getProperties();
	}

	@Override
	public void joinTransaction() {
		this.lazySession.get().joinTransaction();
	}

	@Override
	public boolean isJoinedToTransaction() {
		return this.lazySession.get().isJoinedToTransaction();
	}

	@Override
	public <T> T unwrap(Class<T> cls) {
		if ( cls.isAssignableFrom( Session.class ) ) {
			//noinspection unchecked
			return (T) this;
		}
		return this.lazySession.get().unwrap( cls );
	}

	@Override
	public Object getDelegate() {
		return this.lazySession.get().getDelegate();
	}

	@Override
	public EntityManagerFactory getEntityManagerFactory() {
		return this.lazySession.get().getEntityManagerFactory();
	}

	@Override
	public Metamodel getMetamodel() {
		return this.lazySession.get().getMetamodel();
	}

}
