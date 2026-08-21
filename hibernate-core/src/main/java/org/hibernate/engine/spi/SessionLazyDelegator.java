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
public abstract class SessionLazyDelegator implements Session {

	public abstract Session delegate();

	@Override
	@Nonnull
	public SessionFactory getFactory() {
		return delegate().getFactory();
	}

	@Override
	public void flush() {
		this.delegate().flush();
	}

	@Override
	public void setFlushMode(@Nonnull FlushModeType flushMode) {
		this.delegate().setFlushMode( flushMode );
	}

	@Override
	public void setHibernateFlushMode(FlushMode flushMode) {
		this.delegate().setHibernateFlushMode( flushMode );
	}

	@Override
	@Nonnull
	public FlushModeType getFlushMode() {
		return this.delegate().getFlushMode();
	}

	@Override
	public FlushMode getHibernateFlushMode() {
		return this.delegate().getHibernateFlushMode();
	}

	@Override
	public void setCacheMode(@Nonnull CacheMode cacheMode) {
		this.delegate().setCacheMode( cacheMode );
	}

	@Override
	public void setCacheRetrieveMode(@Nonnull CacheRetrieveMode cacheRetrieveMode) {
		this.delegate().setCacheRetrieveMode( cacheRetrieveMode );
	}

	@Override
	public void setCacheStoreMode(@Nonnull CacheStoreMode cacheStoreMode) {
		this.delegate().setCacheStoreMode( cacheStoreMode );
	}

	@Override
	@Nonnull
	public CacheStoreMode getCacheStoreMode() {
		return this.delegate().getCacheStoreMode();
	}

	@Override
	@Nonnull
	public CacheRetrieveMode getCacheRetrieveMode() {
		return this.delegate().getCacheRetrieveMode();
	}

	@Override
	public void addOption(@Nonnull EntityManager.Option option) {
		this.delegate().addOption( option );
	}

	@Override
	@Nonnull
	public Set<EntityManager.Option> getOptions() {
		return this.delegate().getOptions();
	}

	@Override
	@Nonnull
	public CacheMode getCacheMode() {
		return this.delegate().getCacheMode();
	}

	@Override
	@Nonnull
	public SessionFactory getSessionFactory() {
		return this.delegate().getSessionFactory();
	}

	@Override
	public void cancelQuery() {
		this.delegate().cancelQuery();
	}

	@Override
	public boolean isDirty() {
		return this.delegate().isDirty();
	}

	@Override
	public boolean isDefaultReadOnly() {
		return this.delegate().isDefaultReadOnly();
	}

	@Override
	public void setDefaultReadOnly(boolean readOnly) {
		this.delegate().setDefaultReadOnly( readOnly );
	}

	@Override
	@Nullable
	public Object getIdentifier(@Nonnull Object object) {
		return this.delegate().getIdentifier( object );
	}

	@Override
	@SuppressWarnings("removal")
	public boolean contains(@Nonnull String entityName, @Nonnull Object object) {
		return this.delegate().contains( entityName, object );
	}

	@Override
	public void detach(@Nonnull Object object) {
		this.delegate().detach( object );
	}

	@Override
	public void evict(@Nonnull Object object) {
		this.delegate().evict( object );
	}

	@Override
	public void load(@Nonnull Object object, @Nonnull Object id) {
		this.delegate().load( object, id );
	}

	@Override
	@Nonnull
	public <T> T merge(@Nonnull T object) {
		return this.delegate().merge( object );
	}

	@Override
	@Nonnull
	public <T> T merge(@Nonnull String entityName, @Nonnull T object) {
		return this.delegate().merge( entityName, object );
	}

	@Override
	@Nonnull
	public <T> T merge(@Nonnull T object, @Nonnull EntityGraph<? super T> loadGraph) {
		return this.delegate().merge( object, loadGraph );
	}

	@Override
	public void persist(@Nonnull Object object) {
		this.delegate().persist( object );
	}

	@Override
	public void persist(String entityName, Object object) {
		this.delegate().persist( entityName, object );
	}

	@Override
	public void lock(@Nonnull Object object, @Nonnull LockMode lockMode) {
		this.delegate().lock( object, lockMode );
	}

	@Override
	public void lock(@Nonnull Object object, @Nonnull LockMode lockMode, @Nullable LockOption... lockOptions) {
		this.delegate().lock( object, lockMode, lockOptions );
	}

	@Override
	@SuppressWarnings("removal")
	public void lock(@Nonnull Object object, @Nonnull LockOptions lockOptions) {
		this.delegate().lock( object, lockOptions );
	}

	@Override
	public void refresh(@Nonnull Object object) {
		this.delegate().refresh( object );
	}

	@Override
	@SuppressWarnings("removal")
	public void refresh(@Nonnull Object object, @Nonnull LockOptions lockOptions) {
		this.delegate().refresh( object, lockOptions );
	}

	@Override
	public void remove(@Nonnull Object object) {
		this.delegate().remove( object );
	}

	@Override
	public LockMode getCurrentLockMode(Object object) {
		return this.delegate().getCurrentLockMode( object );
	}

	@Override
	public void clear() {
		this.delegate().clear();
	}

	@Override
	@Nonnull
	public <E> List<E> findMultiple(@Nonnull Class<E> entityType, @Nonnull List<?> ids, @Nullable FindOption... options) {
		return this.delegate().findMultiple( entityType, ids, options );
	}

	@Override
	@Nonnull
	public <E> List<E> findMultiple(@Nonnull EntityGraph<E> entityGraph, @Nonnull List<?> ids, @Nullable FindOption... options) {
		return this.delegate().findMultiple( entityGraph, ids, options );
	}

	@Override
	public <T> @Nonnull T get(@Nonnull Class<T> entityType, @Nonnull Object id) {
		return this.delegate().get( entityType, id );
	}

	@Override
	public <T> @Nonnull T get(@Nonnull Class<T> entityType, @Nonnull Object key, @Nullable FindOption... findOptions) {
		return this.delegate().get( entityType, key, findOptions );
	}

	@Override
	public <T> @Nonnull T get(@Nonnull EntityGraph<T> entityGraph, @Nonnull Object key, @Nullable FindOption... findOptions) {
		return this.delegate().get( entityGraph, key, findOptions );
	}

	@Override
	@Nonnull
	public <T> List<T> getMultiple(@Nonnull Class<T> entityType, @Nonnull List<?> keys, @Nullable FindOption... findOptions) {
		return this.delegate().getMultiple( entityType, keys, findOptions );
	}

	@Override
	@Nonnull
	public <T> List<T> getMultiple(@Nonnull EntityGraph<T> entityGraph, @Nonnull List<?> keys, @Nullable FindOption... findOptions) {
		return this.delegate().getMultiple( entityGraph, keys, findOptions );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public <T> T get(@Nonnull Class<T> entityType, @Nonnull Object id, @Nonnull LockMode lockMode) {
		return this.delegate().get( entityType, id, lockMode );
	}

	@Override
	@Nonnull
	public Object get(@Nonnull String entityName, @Nonnull Object key, @Nullable FindOption... findOptions) {
		return this.delegate().get( entityName, key, findOptions );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public Object get(@Nonnull String entityName, @Nonnull Object id, @Nonnull LockMode lockMode) {
		return this.delegate().get( entityName, id, lockMode );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public <T> T get(@Nonnull Class<T> entityType, @Nonnull Object id, @Nonnull LockOptions lockOptions) {
		return this.delegate().get( entityType, id, lockOptions );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public Object get(@Nonnull String entityName, @Nonnull Object id, @Nonnull LockOptions lockOptions) {
		return this.delegate().get( entityName, id, lockOptions );
	}

	@Override
	@Nonnull
	public String getEntityName(@Nonnull Object object) {
		return this.delegate().getEntityName( object );
	}

	@Override
	@Nonnull
	public <T> T getReference(@Nonnull Class<T> entityType, @Nonnull Object id) {
		return this.delegate().getReference( entityType, id );
	}

	@Override
	@Nonnull
	public Object getReference(@Nonnull String entityName, @Nonnull Object id) {
		return this.delegate().getReference( entityName, id );
	}

	@Override
	@Nonnull
	public <T> T getReference(@Nonnull T object) {
		return this.delegate().getReference( object );
	}

	@Override
	@Nonnull
	public <T> T getReference(@Nonnull Class<T> entityType, @Nonnull Object key, @Nonnull KeyType keyType) {
		return this.delegate().getReference( entityType, key, keyType );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public <T> IdentifierLoadAccess<T> byId(@Nonnull String entityName) {
		return this.delegate().byId( entityName );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public <T> MultiIdentifierLoadAccess<T> byMultipleIds(@Nonnull Class<T> entityClass) {
		return this.delegate().byMultipleIds( entityClass );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public <T> MultiIdentifierLoadAccess<T> byMultipleIds(@Nonnull String entityName) {
		return this.delegate().byMultipleIds( entityName );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public <T> IdentifierLoadAccess<T> byId(@Nonnull Class<T> entityClass) {
		return this.delegate().byId( entityClass );
	}

	@Override @Deprecated
	@Nonnull
	public <T> NaturalIdLoadAccess<T> byNaturalId(@Nonnull String entityName) {
		return this.delegate().byNaturalId( entityName );
	}

	@Override @Deprecated
	@Nonnull
	public <T> NaturalIdLoadAccess<T> byNaturalId(@Nonnull Class<T> entityClass) {
		return this.delegate().byNaturalId( entityClass );
	}

	@Override @Deprecated
	@Nonnull
	public <T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(@Nonnull String entityName) {
		return this.delegate().bySimpleNaturalId( entityName );
	}

	@Override @Deprecated
	@Nonnull
	public <T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(@Nonnull Class<T> entityClass) {
		return this.delegate().bySimpleNaturalId( entityClass );
	}

	@Override @Deprecated
	@Nonnull
	public <T> NaturalIdMultiLoadAccess<T> byMultipleNaturalId(@Nonnull Class<T> entityClass) {
		return this.delegate().byMultipleNaturalId( entityClass );
	}

	@Override @Deprecated
	@Nonnull
	public <T> NaturalIdMultiLoadAccess<T> byMultipleNaturalId(@Nonnull String entityName) {
		return this.delegate().byMultipleNaturalId( entityName );
	}

	@Override
	@Nonnull
	public Filter enableFilter(@Nonnull String filterName) {
		return this.delegate().enableFilter( filterName );
	}

	@Override
	@Nullable
	public Filter getEnabledFilter(@Nonnull String filterName) {
		return this.delegate().getEnabledFilter( filterName );
	}

	@Override
	public void disableFilter(@Nonnull String filterName) {
		this.delegate().disableFilter( filterName );
	}

	@Override
	@Nonnull
	public SessionStatistics getStatistics() {
		return this.delegate().getStatistics();
	}

	@Override
	public boolean isReadOnly(@Nonnull Object entityOrProxy) {
		return this.delegate().isReadOnly( entityOrProxy );
	}

	@Override
	public void setReadOnly(@Nonnull Object entityOrProxy,  boolean readOnly) {
		this.delegate().setReadOnly( entityOrProxy, readOnly );
	}

	@Override
	public boolean isFetchProfileEnabled(@Nonnull String name) throws UnknownProfileException {
		return this.delegate().isFetchProfileEnabled( name );
	}

	@Override
	public void enableFetchProfile(@Nonnull String name) throws UnknownProfileException {
		this.delegate().enableFetchProfile( name );
	}

	@Override
	public void disableFetchProfile(@Nonnull String name) throws UnknownProfileException {
		this.delegate().disableFetchProfile( name );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public LobHelper getLobHelper() {
		return this.delegate().getLobHelper();
	}

	@Override
	@Nonnull
	public Collection<?> getManagedEntities() {
		return this.delegate().getManagedEntities();
	}

	@Override
	@Nonnull
	public Collection<?> getManagedEntities(@Nonnull String entityName) {
		return this.delegate().getManagedEntities( entityName );
	}

	@Override
	@Nonnull
	public <E> Collection<E> getManagedEntities(@Nonnull Class<E> entityType) {
		return this.delegate().getManagedEntities( entityType );
	}

	@Override
	@Nonnull
	public <E> Collection<E> getManagedEntities(@Nonnull EntityType<E> entityType) {
		return this.delegate().getManagedEntities( entityType );
	}

	@Override
	@Nonnull
	public SharedSessionBuilder sessionWithOptions() {
		return this.delegate().sessionWithOptions();
	}

	@Override
	public void addEventListeners(@Nonnull SessionEventListener... listeners) {
		this.delegate().addEventListeners( listeners );
	}

	@Override
	@Nonnull
	public <T> RootGraph<T> createEntityGraph(@Nonnull Class<T> rootType) {
		return this.delegate().createEntityGraph( rootType );
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nullable
	public RootGraph<?> createEntityGraph(@Nonnull String graphName) {
		return this.delegate().createEntityGraph( graphName );
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nullable
	public <T> RootGraph<T> createEntityGraph(@Nonnull Class<T> rootType, @Nonnull String graphName) {
		return this.delegate().createEntityGraph( rootType, graphName );
	}

	@Override
	@Nonnull
	public RootGraph<?> getEntityGraph(@Nonnull String graphName) {
		return this.delegate().getEntityGraph( graphName );
	}

	@Override
	@Nonnull
	public <T> RootGraph<T> getEntityGraph(@Nonnull Class<T> entityClass, @Nonnull String name) {
		return this.delegate().getEntityGraph( entityClass, name );
	}

	@Override
	@Nonnull
	public <T> List<EntityGraph<? super T>> getEntityGraphs(@Nonnull Class<T> entityClass) {
		return this.delegate().getEntityGraphs( entityClass );
	}

	@Override
	public <C> void runWithConnection(@Nonnull ConnectionConsumer<C> action) {
		this.delegate().runWithConnection( action );
	}

	@Override
	public <C, T> T callWithConnection(@Nonnull ConnectionFunction<C, T> function) {
		return this.delegate().callWithConnection( function );
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> createQuery(@Nonnull String queryString, @Nonnull Class<R> resultClass) {
		//noinspection SqlSourceToSinkFlow
		return this.delegate().createQuery( queryString, resultClass );
	}

	@Override
	@Nonnull
	public <T> SelectionQuery<T> createQuery(@Nonnull String query, @Nonnull EntityGraph<T> entityGraph) {
		return this.delegate().createQuery( query, entityGraph );
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> createQuery(@Nonnull TypedQueryReference<R> typedQueryReference) {
		return this.delegate().createQuery( typedQueryReference );
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery createQuery(@Nonnull String queryString) {
		//noinspection SqlSourceToSinkFlow
		return this.delegate().createQuery( queryString );
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> createNamedQuery(@Nonnull String name, @Nonnull Class<R> resultClass) {
		return this.delegate().createNamedQuery( name, resultClass );
	}

	@Override
	@Nonnull
	public MutationQuery createNamedStatement(@Nonnull String name) {
		return this.delegate().createNamedStatement( name );
	}

	@Override
	@Nonnull
	public <R> NativeQuery<R> createNamedQuery(@Nonnull String name, @Nonnull String resultSetMappingName) {
		return this.delegate().createNamedQuery( name, resultSetMappingName );
	}

	@Override
	@Nonnull
	public <R> NativeQuery<R> createNamedQuery(
			@Nonnull String name,
			@Nonnull String resultSetMappingName,
			@Nonnull Class<R> resultClass) {
		return this.delegate().createNamedQuery( name, resultSetMappingName, resultClass );
	}

	@Override
	@Nonnull
	public MutationQuery createNativeStatement(@Nonnull String sql) {
		return this.delegate().createNativeStatement( sql );
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery createNamedQuery(@Nonnull String name) {
		return this.delegate().createNamedQuery( name );
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> createSelectionQuery(@Nonnull CriteriaSelect<R> criteria) {
		return this.delegate().createSelectionQuery( criteria );
	}

	@Override
	@Nonnull
	public <T> SelectionQuery<T> createQuery(@Nonnull CriteriaSelect<T> selectQuery) {
		return this.delegate().createQuery( selectQuery );
	}

	@Override
	public SharedStatelessSessionBuilder statelessWithOptions() {
		return this.delegate().statelessWithOptions();
	}

	@Override
	@Nullable
	public String getTenantIdentifier() {
		return this.delegate().getTenantIdentifier();
	}

	@Override
	@Nullable
	public Object getTenantIdentifierValue() {
		return this.delegate().getTenantIdentifierValue();
	}

	@Override
	public void close() throws HibernateException {
		this.delegate().close();
	}

	@Override
	public boolean isOpen() {
		return this.delegate().isOpen();
	}

	@Override
	public boolean isConnected() {
		return this.delegate().isConnected();
	}

	@Override
	@Nonnull
	public Transaction beginTransaction() {
		return this.delegate().beginTransaction();
	}

	@Override
	@Nonnull
	public Transaction getTransaction() {
		return this.delegate().getTransaction();
	}

	@Override
	@Nonnull
	public ProcedureCall getNamedProcedureCall(@Nonnull String name) {
		return this.delegate().getNamedProcedureCall( name );
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureCall(@Nonnull String procedureName) {
		return this.delegate().createStoredProcedureCall( procedureName );
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureCall(@Nonnull String procedureName, @Nonnull Class<?>... resultClasses) {
		return this.delegate().createStoredProcedureCall( procedureName, resultClasses );
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureCall(@Nonnull String procedureName, @Nonnull String... resultSetMappings) {
		return this.delegate().createStoredProcedureCall( procedureName, resultSetMappings );
	}

	@Override
	@Nonnull
	public ProcedureCall createNamedStoredProcedureQuery(@Nonnull String name) {
		return this.delegate().createNamedStoredProcedureQuery( name );
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureQuery(@Nonnull String procedureName) {
		return this.delegate().createStoredProcedureQuery( procedureName );
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureQuery(@Nonnull String procedureName, @Nonnull Class... resultClasses) {
		return this.delegate().createStoredProcedureQuery( procedureName, resultClasses );
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureQuery(@Nonnull String procedureName, @Nonnull String... resultSetMappings) {
		return this.delegate().createStoredProcedureQuery( procedureName, resultSetMappings );
	}

	@Override
	public Integer getJdbcBatchSize() {
		return this.delegate().getJdbcBatchSize();
	}

	@Override
	public void setJdbcBatchSize(Integer jdbcBatchSize) {
		this.delegate().setJdbcBatchSize( jdbcBatchSize );
	}

	@Override
	public int getFetchBatchSize() {
		return this.delegate().getFetchBatchSize();
	}

	@Override
	public void setFetchBatchSize(int batchSize) {
		this.delegate().setFetchBatchSize( batchSize );
	}

	@Override
	public boolean isSubselectFetchingEnabled() {
		return this.delegate().isSubselectFetchingEnabled();
	}

	@Override
	public void setSubselectFetchingEnabled(boolean enabled) {
		this.delegate().setSubselectFetchingEnabled( enabled );
	}

	@Override
	@Nonnull
	public HibernateCriteriaBuilder getCriteriaBuilder() {
		return this.delegate().getCriteriaBuilder();
	}

	@Override
	public void doWork(@Nonnull Work work) throws HibernateException {
		this.delegate().doWork( work );
	}

	@Override
	public <T> T doReturningWork(@Nonnull ReturningWork<T> work) throws HibernateException {
		return this.delegate().doReturningWork( work );
	}

	@SuppressWarnings("rawtypes")
	@Override
	@Deprecated
	@Nonnull
	public NativeQuery createNativeQuery(@Nonnull String sqlString) {
		return this.delegate().createNativeQuery( sqlString );
	}

	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	@Nonnull
	public NativeQuery createNativeQuery(@Nonnull String sqlString, @Nonnull Class resultClass) {
		return this.delegate().createNativeQuery( sqlString, resultClass );
	}

	@Override
	public <R> NativeQuery<R> createNativeQuery(String sqlString, Class<R> resultClass, String tableAlias) {
		return this.delegate().createNativeQuery( sqlString, resultClass, tableAlias );
	}

	@SuppressWarnings("rawtypes")
	@Override
	@Deprecated
	@Nonnull
	public NativeQuery createNativeQuery(@Nonnull String sqlString, @Nonnull String resultSetMappingName) {
		return this.delegate().createNativeQuery( sqlString, resultSetMappingName );
	}

	@Override
	@Nonnull
	public <T> TypedQuery<T> createNativeQuery(@Nonnull String sql, @Nonnull ResultSetMapping<T> resultSetMapping) {
		return this.delegate().createNativeQuery( sql, resultSetMapping );
	}

	@Override
	public <R> NativeQuery<R> createNativeQuery(String sqlString, String resultSetMappingName, Class<R> resultClass) {
		return this.delegate().createNativeQuery( sqlString, resultSetMappingName, resultClass );
	}

	@Override
	public <R> SelectionQuery<R> createSelectionQuery(String hqlString, Class<R> resultType) {
		return this.delegate().createSelectionQuery( hqlString, resultType );
	}

	@Override
	public <R> SelectionQuery<R> createSelectionQuery(String hqlString, EntityGraph<R> resultGraph) {
		return this.delegate().createSelectionQuery( hqlString, resultGraph );
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> createSelectionQuery(@Nonnull CriteriaQuery<R> criteria) {
		return this.delegate().createSelectionQuery( criteria );
	}

	@Override
	@Nonnull
	public MutationQuery createMutationQuery(@Nonnull String hqlString) {
		return this.delegate().createMutationQuery( hqlString );
	}

	@Override
	@Nonnull
	public MutationQuery createStatement(@Nonnull String hqlString) {
		return this.delegate().createStatement( hqlString );
	}

	@Override
	@Nonnull
	public MutationQuery createStatement(@Nonnull StatementReference statementReference) {
		return this.delegate().createStatement( statementReference );
	}

	@Override
	@Nonnull
	public MutationQuery createStatement(@Nonnull CriteriaStatement<?> criteriaStatement) {
		return this.delegate().createStatement( criteriaStatement );
	}

	@Override
	@Nonnull
	public MutationQuery createMutationQuery(@Nonnull CriteriaStatement<?> criteriaStatement) {
		return this.delegate().createMutationQuery( criteriaStatement );
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
		return this.delegate().createMutationQuery( insert );
	}

	@Override
	@Nonnull
	public MutationQuery createNativeMutationQuery(@Nonnull String sqlString) {
		return this.delegate().createNativeMutationQuery( sqlString );
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> createNamedSelectionQuery(@Nonnull String name, @Nonnull Class<R> resultType) {
		return this.delegate().createNamedSelectionQuery( name, resultType );
	}

	@Override
	@Nonnull
	public MutationQuery createNamedMutationQuery(@Nonnull String name) {
		return this.delegate().createNamedMutationQuery( name );
	}

	@Override
	@Nullable
	public <T> T find(@Nonnull Class<T> entityClass, @Nonnull Object primaryKey) {
		return this.delegate().find( entityClass, primaryKey );
	}

	@Override
	@Nullable
	public <T> T find(
			@Nonnull Class<T> entityClass,
			@Nonnull Object primaryKey,
			@Nullable Map<String, Object> properties) {
		return this.delegate().find( entityClass, primaryKey, properties );
	}

	@Override
	@Nullable
	public <T> T find(
			@Nonnull Class<T> entityClass,
			@Nonnull Object primaryKey,
			@Nonnull LockModeType lockMode,
			@Nullable Map<String, Object> properties) {
		return this.delegate().find( entityClass, primaryKey, lockMode, properties );
	}

	@Override
	@Nullable
	public <T> T find(
			@Nonnull Class<T> entityClass,
			@Nonnull Object primaryKey,
			@Nullable FindOption... options) {
		return this.delegate().find( entityClass, primaryKey, options );
	}

	@Override
	@Nullable
	public <T> T find(
			@Nonnull EntityGraph<T> entityGraph,
			@Nonnull Object primaryKey,
			@Nullable FindOption... options) {
		return this.delegate().find( entityGraph, primaryKey, options );
	}

	@Override
	@Nullable
	public Object find(
			@Nonnull String entityName,
			@Nonnull Object primaryKey,
			@Nullable FindOption... options) {
		return this.delegate().find( entityName, primaryKey, options );
	}

	@Override
	public void lock(@Nonnull Object entity, @Nonnull LockModeType lockMode) {
		this.delegate().lock( entity, lockMode );
	}

	@Override
	public void lock(
			@Nonnull Object entity,
			@Nonnull LockModeType lockMode,
			@Nullable  Map<String, Object> properties) {
		this.delegate().lock( entity, lockMode, properties );
	}

	@Override
	public void lock(@Nonnull Object entity, @Nonnull LockModeType lockMode, @Nullable LockOption... options) {
		this.delegate().lock( entity, lockMode, options );
	}

	@Override
	public void refresh(@Nonnull Object entity, @Nullable Map<String, Object> properties) {
		this.delegate().refresh( entity, properties );
	}

	@Override
	public void refresh(@Nonnull Object entity,
						@Nonnull LockModeType lockMode,
						@Nullable  Map<String, Object> properties) {
		this.delegate().refresh( entity, lockMode, properties );
	}

	@Override
	public void refresh(@Nonnull Object entity, @Nullable RefreshOption... options) {
		this.delegate().refresh( entity, options );
	}

	@Override
	public boolean contains(@Nonnull Object entity) {
		return this.delegate().contains( entity );
	}

	@Override
	@Nonnull
	public LockModeType getLockMode(@Nonnull Object entity) {
		return this.delegate().getLockMode( entity );
	}

	@Override
	public void setProperty(@Nonnull String propertyName, @Nullable Object value) {
		this.delegate().setProperty( propertyName, value );
	}

	@Override
	@Nonnull
	public Map<String, Object> getProperties() {
		return this.delegate().getProperties();
	}

	@Override
	public void joinTransaction() {
		this.delegate().joinTransaction();
	}

	@Override
	public boolean isJoinedToTransaction() {
		return this.delegate().isJoinedToTransaction();
	}

	@Override
	@Nonnull
	public <T> T unwrap(@Nonnull Class<T> type) {
		return type.isAssignableFrom( Session.class )
				? type.cast( this )
				: delegate().unwrap( type );
	}

	@Override @Deprecated
	@SuppressWarnings({"rawtypes", "removal"})
	@Nonnull
	public NativeQuery getNamedNativeQuery(@Nonnull String name) {
		return delegate().getNamedNativeQuery( name );
	}

	@Override @Deprecated
	@Nonnull
	public Object getDelegate() {
		return delegate().getDelegate();
	}

	@Override
	@Nonnull
	public EntityManagerFactory getEntityManagerFactory() {
		return this.delegate().getEntityManagerFactory();
	}

	@Override
	@Nonnull
	public Metamodel getMetamodel() {
		return this.delegate().getMetamodel();
	}

}
