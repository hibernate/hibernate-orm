/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.ConnectionConsumer;
import jakarta.persistence.ConnectionFunction;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FindOption;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockOption;
import jakarta.persistence.RefreshOption;
import jakarta.persistence.StatementReference;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.CriteriaStatement;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.sql.ResultSetMapping;
import org.hibernate.CacheMode;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.KeyType;
import org.hibernate.LobHelper;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.NaturalIdMultiLoadAccess;
import org.hibernate.Session;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SharedStatelessSessionBuilder;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.Transaction;
import org.hibernate.UnknownProfileException;
import org.hibernate.graph.RootGraph;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.MutationOrSelectionQuery;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaInsert;
import org.hibernate.stat.SessionStatistics;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	@Nonnull
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
	public void setFlushMode(@Nonnull FlushModeType flushMode) {
		this.lazySession.get().setFlushMode( flushMode );
	}

	@Override
	public void setHibernateFlushMode(FlushMode flushMode) {
		this.lazySession.get().setHibernateFlushMode( flushMode );
	}

	@Override
	@Nonnull
	public FlushModeType getFlushMode() {
		return this.lazySession.get().getFlushMode();
	}

	@Override
	public FlushMode getHibernateFlushMode() {
		return this.lazySession.get().getHibernateFlushMode();
	}

	@Override
	public void setCacheMode(@Nonnull CacheMode cacheMode) {
		this.lazySession.get().setCacheMode( cacheMode );
	}

	@Override
	public void setCacheRetrieveMode(@Nonnull CacheRetrieveMode cacheRetrieveMode) {
		this.lazySession.get().setCacheRetrieveMode( cacheRetrieveMode );
	}

	@Override
	public void setCacheStoreMode(@Nonnull CacheStoreMode cacheStoreMode) {
		this.lazySession.get().setCacheStoreMode( cacheStoreMode );
	}

	@Override
	@Nonnull
	public CacheStoreMode getCacheStoreMode() {
		return this.lazySession.get().getCacheStoreMode();
	}

	@Override
	@Nonnull
	public CacheRetrieveMode getCacheRetrieveMode() {
		return this.lazySession.get().getCacheRetrieveMode();
	}

	@Override
	public void addOption(@Nonnull EntityManager.Option option) {
		this.lazySession.get().addOption( option );
	}

	@Override
	@Nonnull
	public Set<EntityManager.Option> getOptions() {
		return this.lazySession.get().getOptions();
	}

	@Override
	@Nonnull
	public CacheMode getCacheMode() {
		return this.lazySession.get().getCacheMode();
	}

	@Override
	@Nonnull
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
	@Nullable
	public Object getIdentifier(@Nonnull Object object) {
		return this.lazySession.get().getIdentifier( object );
	}

	@Override
	@SuppressWarnings("removal")
	public boolean contains(@Nonnull String entityName, @Nonnull Object object) {
		return this.lazySession.get().contains( entityName, object );
	}

	@Override
	public void detach(@Nonnull Object object) {
		this.lazySession.get().detach( object );
	}

	@Override
	public void evict(@Nonnull Object object) {
		this.lazySession.get().evict( object );
	}

	@Override
	public void load(@Nonnull Object object, @Nonnull Object id) {
		this.lazySession.get().load( object, id );
	}

	@Override
	@Nonnull
	public <T> T merge(@Nonnull T object) {
		return this.lazySession.get().merge( object );
	}

	@Override
	@Nonnull
	public <T> T merge(@Nonnull String entityName, @Nonnull T object) {
		return this.lazySession.get().merge( entityName, object );
	}

	@Override
	@Nonnull
	public <T> T merge(@Nonnull T object, @Nonnull EntityGraph<? super T> loadGraph) {
		return this.lazySession.get().merge( object, loadGraph );
	}

	@Override
	public void persist(@Nonnull Object object) {
		this.lazySession.get().persist( object );
	}

	@Override
	public void persist(String entityName, Object object) {
		this.lazySession.get().persist( entityName, object );
	}

	@Override
	public void lock(@Nonnull Object object, @Nonnull LockMode lockMode) {
		this.lazySession.get().lock( object, lockMode );
	}

	@Override
	public void lock(@Nonnull Object object, @Nonnull LockMode lockMode, @Nullable LockOption... lockOptions) {
		this.lazySession.get().lock( object, lockMode, lockOptions );
	}

	@Override
	@SuppressWarnings("removal")
	public void lock(@Nonnull Object object, @Nonnull LockOptions lockOptions) {
		this.lazySession.get().lock( object, lockOptions );
	}

	@Override
	public void refresh(@Nonnull Object object) {
		this.lazySession.get().refresh( object );
	}

	@Override
	@SuppressWarnings("removal")
	public void refresh(@Nonnull Object object, @Nonnull LockOptions lockOptions) {
		this.lazySession.get().refresh( object, lockOptions );
	}

	@Override
	public void remove(@Nonnull Object object) {
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
	@Nonnull
	public <E> List<E> findMultiple(@Nonnull Class<E> entityType, @Nonnull List<?> ids, @Nullable FindOption... options) {
		return this.lazySession.get().findMultiple( entityType, ids, options );
	}

	@Override
	@Nonnull
	public <E> List<E> findMultiple(@Nonnull EntityGraph<E> entityGraph, @Nonnull List<?> ids, @Nullable FindOption... options) {
		return this.lazySession.get().findMultiple( entityGraph, ids, options );
	}

	@Override
	public <T> @Nonnull T get(@Nonnull Class<T> entityType, @Nonnull Object id) {
		return this.lazySession.get().get( entityType, id );
	}

	@Override
	public <T> @Nonnull T get(@Nonnull Class<T> entityType, @Nonnull Object key, @Nullable FindOption... findOptions) {
		return this.lazySession.get().get( entityType, key, findOptions );
	}

	@Override
	public <T> @Nonnull T get(@Nonnull EntityGraph<T> entityGraph, @Nonnull Object key, @Nullable FindOption... findOptions) {
		return this.lazySession.get().get( entityGraph, key, findOptions );
	}

	@Override
	@Nonnull
	public <T> List<T> getMultiple(@Nonnull Class<T> entityType, @Nonnull List<?> keys, @Nullable FindOption... findOptions) {
		return this.lazySession.get().getMultiple( entityType, keys, findOptions );
	}

	@Override
	@Nonnull
	public <T> List<T> getMultiple(@Nonnull EntityGraph<T> entityGraph, @Nonnull List<?> keys, @Nullable FindOption... findOptions) {
		return this.lazySession.get().getMultiple( entityGraph, keys, findOptions );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public <T> T get(@Nonnull Class<T> entityType, @Nonnull Object id, @Nonnull LockMode lockMode) {
		return this.lazySession.get().get( entityType, id, lockMode );
	}

	@Override
	@Nonnull
	public Object get(@Nonnull String entityName, @Nonnull Object key, @Nullable FindOption... findOptions) {
		return this.lazySession.get().get( entityName, key, findOptions );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public Object get(@Nonnull String entityName, @Nonnull Object id, @Nonnull LockMode lockMode) {
		return this.lazySession.get().get( entityName, id, lockMode );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public <T> T get(@Nonnull Class<T> entityType, @Nonnull Object id, @Nonnull LockOptions lockOptions) {
		return this.lazySession.get().get( entityType, id, lockOptions );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public Object get(@Nonnull String entityName, @Nonnull Object id, @Nonnull LockOptions lockOptions) {
		return this.lazySession.get().get( entityName, id, lockOptions );
	}

	@Override
	@Nonnull
	public String getEntityName(@Nonnull Object object) {
		return this.lazySession.get().getEntityName( object );
	}

	@Override
	@Nonnull
	public <T> T getReference(@Nonnull Class<T> entityType, @Nonnull Object id) {
		return this.lazySession.get().getReference( entityType, id );
	}

	@Override
	@Nonnull
	public Object getReference(@Nonnull String entityName, @Nonnull Object id) {
		return this.lazySession.get().getReference( entityName, id );
	}

	@Override
	@Nonnull
	public <T> T getReference(@Nonnull T object) {
		return this.lazySession.get().getReference( object );
	}

	@Override
	@Nonnull
	public <T> T getReference(@Nonnull Class<T> entityType, @Nonnull Object key, @Nonnull KeyType keyType) {
		return this.lazySession.get().getReference( entityType, key, keyType );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public <T> IdentifierLoadAccess<T> byId(@Nonnull String entityName) {
		return this.lazySession.get().byId( entityName );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public <T> MultiIdentifierLoadAccess<T> byMultipleIds(@Nonnull Class<T> entityClass) {
		return this.lazySession.get().byMultipleIds( entityClass );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public <T> MultiIdentifierLoadAccess<T> byMultipleIds(@Nonnull String entityName) {
		return this.lazySession.get().byMultipleIds( entityName );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public <T> IdentifierLoadAccess<T> byId(@Nonnull Class<T> entityClass) {
		return this.lazySession.get().byId( entityClass );
	}

	@Override @Deprecated
	@Nonnull
	public <T> NaturalIdLoadAccess<T> byNaturalId(@Nonnull String entityName) {
		return this.lazySession.get().byNaturalId( entityName );
	}

	@Override @Deprecated
	@Nonnull
	public <T> NaturalIdLoadAccess<T> byNaturalId(@Nonnull Class<T> entityClass) {
		return this.lazySession.get().byNaturalId( entityClass );
	}

	@Override @Deprecated
	@Nonnull
	public <T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(@Nonnull String entityName) {
		return this.lazySession.get().bySimpleNaturalId( entityName );
	}

	@Override @Deprecated
	@Nonnull
	public <T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(@Nonnull Class<T> entityClass) {
		return this.lazySession.get().bySimpleNaturalId( entityClass );
	}

	@Override @Deprecated
	@Nonnull
	public <T> NaturalIdMultiLoadAccess<T> byMultipleNaturalId(@Nonnull Class<T> entityClass) {
		return this.lazySession.get().byMultipleNaturalId( entityClass );
	}

	@Override @Deprecated
	@Nonnull
	public <T> NaturalIdMultiLoadAccess<T> byMultipleNaturalId(@Nonnull String entityName) {
		return this.lazySession.get().byMultipleNaturalId( entityName );
	}

	@Override
	@Nonnull
	public Filter enableFilter(@Nonnull String filterName) {
		return this.lazySession.get().enableFilter( filterName );
	}

	@Override
	@Nullable
	public Filter getEnabledFilter(@Nonnull String filterName) {
		return this.lazySession.get().getEnabledFilter( filterName );
	}

	@Override
	public void disableFilter(@Nonnull String filterName) {
		this.lazySession.get().disableFilter( filterName );
	}

	@Override
	@Nonnull
	public SessionStatistics getStatistics() {
		return this.lazySession.get().getStatistics();
	}

	@Override
	public boolean isReadOnly(@Nonnull Object entityOrProxy) {
		return this.lazySession.get().isReadOnly( entityOrProxy );
	}

	@Override
	public void setReadOnly(@Nonnull Object entityOrProxy,  boolean readOnly) {
		this.lazySession.get().setReadOnly( entityOrProxy, readOnly );
	}

	@Override
	public boolean isFetchProfileEnabled(@Nonnull String name) throws UnknownProfileException {
		return this.lazySession.get().isFetchProfileEnabled( name );
	}

	@Override
	public void enableFetchProfile(@Nonnull String name) throws UnknownProfileException {
		this.lazySession.get().enableFetchProfile( name );
	}

	@Override
	public void disableFetchProfile(@Nonnull String name) throws UnknownProfileException {
		this.lazySession.get().disableFetchProfile( name );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public LobHelper getLobHelper() {
		return this.lazySession.get().getLobHelper();
	}

	@Override
	@Nonnull
	public Collection<?> getManagedEntities() {
		return this.lazySession.get().getManagedEntities();
	}

	@Override
	@Nonnull
	public Collection<?> getManagedEntities(@Nonnull String entityName) {
		return this.lazySession.get().getManagedEntities( entityName );
	}

	@Override
	@Nonnull
	public <E> Collection<E> getManagedEntities(@Nonnull Class<E> entityType) {
		return this.lazySession.get().getManagedEntities( entityType );
	}

	@Override
	@Nonnull
	public <E> Collection<E> getManagedEntities(@Nonnull EntityType<E> entityType) {
		return this.lazySession.get().getManagedEntities( entityType );
	}

	@Override
	@Nonnull
	public SharedSessionBuilder sessionWithOptions() {
		return this.lazySession.get().sessionWithOptions();
	}

	@Override
	public void addEventListeners(@Nonnull SessionEventListener... listeners) {
		this.lazySession.get().addEventListeners( listeners );
	}

	@Override
	@Nonnull
	public <T> RootGraph<T> createEntityGraph(@Nonnull Class<T> rootType) {
		return this.lazySession.get().createEntityGraph( rootType );
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nullable
	public RootGraph<?> createEntityGraph(@Nonnull String graphName) {
		return this.lazySession.get().createEntityGraph( graphName );
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nullable
	public <T> RootGraph<T> createEntityGraph(@Nonnull Class<T> rootType, @Nonnull String graphName) {
		return this.lazySession.get().createEntityGraph( rootType, graphName );
	}

	@Override
	@Nonnull
	public RootGraph<?> getEntityGraph(@Nonnull String graphName) {
		return this.lazySession.get().getEntityGraph( graphName );
	}

	@Override
	@Nonnull
	public <T> RootGraph<T> getEntityGraph(@Nonnull Class<T> entityClass, @Nonnull String name) {
		return this.lazySession.get().getEntityGraph( entityClass, name );
	}

	@Override
	@Nonnull
	public <T> List<EntityGraph<? super T>> getEntityGraphs(@Nonnull Class<T> entityClass) {
		return this.lazySession.get().getEntityGraphs( entityClass );
	}

	@Override
	public <C> void runWithConnection(@Nonnull ConnectionConsumer<C> action) {
		this.lazySession.get().runWithConnection( action );
	}

	@Override
	public <C, T> T callWithConnection(@Nonnull ConnectionFunction<C, T> function) {
		return this.lazySession.get().callWithConnection( function );
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> createQuery(@Nonnull String queryString, @Nonnull Class<R> resultClass) {
		//noinspection SqlSourceToSinkFlow
		return this.lazySession.get().createQuery( queryString, resultClass );
	}

	@Override
	@Nonnull
	public <T> SelectionQuery<T> createQuery(@Nonnull String query, @Nonnull EntityGraph<T> entityGraph) {
		return this.lazySession.get().createQuery( query, entityGraph );
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> createQuery(@Nonnull TypedQueryReference<R> typedQueryReference) {
		return this.lazySession.get().createQuery( typedQueryReference );
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery createQuery(@Nonnull String queryString) {
		//noinspection SqlSourceToSinkFlow
		return this.lazySession.get().createQuery( queryString );
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> createNamedQuery(@Nonnull String name, @Nonnull Class<R> resultClass) {
		return this.lazySession.get().createNamedQuery( name, resultClass );
	}

	@Override
	@Nonnull
	public MutationQuery createNamedStatement(@Nonnull String name) {
		return this.lazySession.get().createNamedStatement( name );
	}

	@Override
	@Nonnull
	public <R> NativeQuery<R> createNamedQuery(@Nonnull String name, @Nonnull String resultSetMappingName) {
		return this.lazySession.get().createNamedQuery( name, resultSetMappingName );
	}

	@Override
	@Nonnull
	public <R> NativeQuery<R> createNamedQuery(
			@Nonnull String name,
			@Nonnull String resultSetMappingName,
			@Nonnull Class<R> resultClass) {
		return this.lazySession.get().createNamedQuery( name, resultSetMappingName, resultClass );
	}

	@Override
	@Nonnull
	public MutationQuery createNativeStatement(@Nonnull String sql) {
		return this.lazySession.get().createNativeStatement( sql );
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery createNamedQuery(@Nonnull String name) {
		return this.lazySession.get().createNamedQuery( name );
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> createSelectionQuery(@Nonnull CriteriaSelect<R> criteria) {
		return this.lazySession.get().createSelectionQuery( criteria );
	}

	@Override
	@Nonnull
	public <T> SelectionQuery<T> createQuery(@Nonnull CriteriaSelect<T> selectQuery) {
		return this.lazySession.get().createQuery( selectQuery );
	}

	@Override
	public SharedStatelessSessionBuilder statelessWithOptions() {
		return this.lazySession.get().statelessWithOptions();
	}

	@Override
	@Nullable
	public String getTenantIdentifier() {
		return this.lazySession.get().getTenantIdentifier();
	}

	@Override
	@Nullable
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
	@Nonnull
	public Transaction beginTransaction() {
		return this.lazySession.get().beginTransaction();
	}

	@Override
	@Nonnull
	public Transaction getTransaction() {
		return this.lazySession.get().getTransaction();
	}

	@Override
	@Nonnull
	public ProcedureCall getNamedProcedureCall(@Nonnull String name) {
		return this.lazySession.get().getNamedProcedureCall( name );
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureCall(@Nonnull String procedureName) {
		return this.lazySession.get().createStoredProcedureCall( procedureName );
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureCall(@Nonnull String procedureName, @Nonnull Class<?>... resultClasses) {
		return this.lazySession.get().createStoredProcedureCall( procedureName, resultClasses );
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureCall(@Nonnull String procedureName, @Nonnull String... resultSetMappings) {
		return this.lazySession.get().createStoredProcedureCall( procedureName, resultSetMappings );
	}

	@Override
	@Nonnull
	public ProcedureCall createNamedStoredProcedureQuery(@Nonnull String name) {
		return this.lazySession.get().createNamedStoredProcedureQuery( name );
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureQuery(@Nonnull String procedureName) {
		return this.lazySession.get().createStoredProcedureQuery( procedureName );
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureQuery(@Nonnull String procedureName, @Nonnull Class... resultClasses) {
		return this.lazySession.get().createStoredProcedureQuery( procedureName, resultClasses );
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureQuery(@Nonnull String procedureName, @Nonnull String... resultSetMappings) {
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
	@Nonnull
	public HibernateCriteriaBuilder getCriteriaBuilder() {
		return this.lazySession.get().getCriteriaBuilder();
	}

	@Override
	public void doWork(@Nonnull Work work) throws HibernateException {
		this.lazySession.get().doWork( work );
	}

	@Override
	public <T> T doReturningWork(@Nonnull ReturningWork<T> work) throws HibernateException {
		return this.lazySession.get().doReturningWork( work );
	}

	@SuppressWarnings("rawtypes")
	@Override
	@Deprecated
	@Nonnull
	public NativeQuery createNativeQuery(@Nonnull String sqlString) {
		return this.lazySession.get().createNativeQuery( sqlString );
	}

	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	@Nonnull
	public NativeQuery createNativeQuery(@Nonnull String sqlString, @Nonnull Class resultClass) {
		return this.lazySession.get().createNativeQuery( sqlString, resultClass );
	}

	@Override
	public <R> NativeQuery<R> createNativeQuery(String sqlString, Class<R> resultClass, String tableAlias) {
		return this.lazySession.get().createNativeQuery( sqlString, resultClass, tableAlias );
	}

	@SuppressWarnings("rawtypes")
	@Override
	@Deprecated
	@Nonnull
	public NativeQuery createNativeQuery(@Nonnull String sqlString, @Nonnull String resultSetMappingName) {
		return this.lazySession.get().createNativeQuery( sqlString, resultSetMappingName );
	}

	@Override
	@Nonnull
	public <T> TypedQuery<T> createNativeQuery(@Nonnull String sql, @Nonnull ResultSetMapping<T> resultSetMapping) {
		return this.lazySession.get().createNativeQuery( sql, resultSetMapping );
	}

	@Override
	public <R> NativeQuery<R> createNativeQuery(String sqlString, String resultSetMappingName, Class<R> resultClass) {
		return this.lazySession.get().createNativeQuery( sqlString, resultSetMappingName, resultClass );
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
	@Nonnull
	public <R> SelectionQuery<R> createSelectionQuery(@Nonnull CriteriaQuery<R> criteria) {
		return this.lazySession.get().createSelectionQuery( criteria );
	}

	@Override
	@Nonnull
	public MutationQuery createMutationQuery(@Nonnull String hqlString) {
		return this.lazySession.get().createMutationQuery( hqlString );
	}

	@Override
	@Nonnull
	public MutationQuery createStatement(@Nonnull String hqlString) {
		return this.lazySession.get().createStatement( hqlString );
	}

	@Override
	@Nonnull
	public MutationQuery createStatement(@Nonnull StatementReference statementReference) {
		return this.lazySession.get().createStatement( statementReference );
	}

	@Override
	@Nonnull
	public MutationQuery createStatement(@Nonnull CriteriaStatement<?> criteriaStatement) {
		return this.lazySession.get().createStatement( criteriaStatement );
	}

	@Override
	@Nonnull
	public MutationQuery createMutationQuery(@Nonnull CriteriaStatement<?> criteriaStatement) {
		return this.lazySession.get().createMutationQuery( criteriaStatement );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public MutationQuery createQuery(@Nonnull CriteriaStatement<?> criteriaStatement) {
		return createMutationQuery( criteriaStatement );
	}

	@Override
	@Nonnull
	public MutationQuery createMutationQuery(@SuppressWarnings("rawtypes") @Nonnull JpaCriteriaInsert insert) {
		return this.lazySession.get().createMutationQuery( insert );
	}

	@Override
	@Nonnull
	public MutationQuery createNativeMutationQuery(@Nonnull String sqlString) {
		return this.lazySession.get().createNativeMutationQuery( sqlString );
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> createNamedSelectionQuery(@Nonnull String name, @Nonnull Class<R> resultType) {
		return this.lazySession.get().createNamedSelectionQuery( name, resultType );
	}

	@Override
	@Nonnull
	public MutationQuery createNamedMutationQuery(@Nonnull String name) {
		return this.lazySession.get().createNamedMutationQuery( name );
	}

	@Override
	@Nullable
	public <T> T find(@Nonnull Class<T> entityClass, @Nonnull Object primaryKey) {
		return this.lazySession.get().find( entityClass, primaryKey );
	}

	@Override
	@Nullable
	public <T> T find(
			@Nonnull Class<T> entityClass,
			@Nonnull Object primaryKey,
			@Nullable Map<String, Object> properties) {
		return this.lazySession.get().find( entityClass, primaryKey, properties );
	}

	@Override
	@Nullable
	public <T> T find(
			@Nonnull Class<T> entityClass,
			@Nonnull Object primaryKey,
			@Nonnull LockModeType lockMode,
			@Nullable Map<String, Object> properties) {
		return this.lazySession.get().find( entityClass, primaryKey, lockMode, properties );
	}

	@Override
	@Nullable
	public <T> T find(
			@Nonnull Class<T> entityClass,
			@Nonnull Object primaryKey,
			@Nullable FindOption... options) {
		return this.lazySession.get().find( entityClass, primaryKey, options );
	}

	@Override
	@Nullable
	public <T> T find(
			@Nonnull EntityGraph<T> entityGraph,
			@Nonnull Object primaryKey,
			@Nullable FindOption... options) {
		return this.lazySession.get().find( entityGraph, primaryKey, options );
	}

	@Override
	@Nullable
	public Object find(
			@Nonnull String entityName,
			@Nonnull Object primaryKey,
			@Nullable FindOption... options) {
		return this.lazySession.get().find( entityName, primaryKey, options );
	}

	@Override
	public void lock(@Nonnull Object entity, @Nonnull LockModeType lockMode) {
		this.lazySession.get().lock( entity, lockMode );
	}

	@Override
	public void lock(
			@Nonnull Object entity,
			@Nonnull LockModeType lockMode,
			@Nullable  Map<String, Object> properties) {
		this.lazySession.get().lock( entity, lockMode, properties );
	}

	@Override
	public void lock(@Nonnull Object entity, @Nonnull LockModeType lockMode, @Nullable LockOption... options) {
		this.lazySession.get().lock( entity, lockMode, options );
	}

	@Override
	public void refresh(@Nonnull Object entity, @Nullable Map<String, Object> properties) {
		this.lazySession.get().refresh( entity, properties );
	}

	@Override
	public void refresh(@Nonnull Object entity,
						@Nonnull LockModeType lockMode,
						@Nullable  Map<String, Object> properties) {
		this.lazySession.get().refresh( entity, lockMode, properties );
	}

	@Override
	public void refresh(@Nonnull Object entity, @Nullable RefreshOption... options) {
		this.lazySession.get().refresh( entity, options );
	}

	@Override
	public boolean contains(@Nonnull Object entity) {
		return this.lazySession.get().contains( entity );
	}

	@Override
	@Nonnull
	public LockModeType getLockMode(@Nonnull Object entity) {
		return this.lazySession.get().getLockMode( entity );
	}

	@Override
	public void setProperty(@Nonnull String propertyName, @Nullable Object value) {
		this.lazySession.get().setProperty( propertyName, value );
	}

	@Override
	@Nonnull
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
	@Nonnull
	public <T> T unwrap(@Nonnull Class<T> type) {
		return type.isAssignableFrom( Session.class )
				? type.cast( this )
				: lazySession.get().unwrap( type );
	}

	@Override @Deprecated
	@SuppressWarnings({"rawtypes", "removal"})
	@Nonnull
	public NativeQuery getNamedNativeQuery(@Nonnull String name) {
		return lazySession.get().getNamedNativeQuery( name );
	}

	@Override @Deprecated
	@Nonnull
	public Object getDelegate() {
		return lazySession.get().getDelegate();
	}

	@Override
	@Nonnull
	public EntityManagerFactory getEntityManagerFactory() {
		return this.lazySession.get().getEntityManagerFactory();
	}

	@Override
	@Nonnull
	public Metamodel getMetamodel() {
		return this.lazySession.get().getMetamodel();
	}

}
