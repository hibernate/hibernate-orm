/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;


import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.Filter;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SharedStatelessSessionBuilder;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.graph.GraphSemantic;
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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.ConnectionConsumer;
import jakarta.persistence.ConnectionFunction;
import jakarta.persistence.EntityAgent;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FindOption;
import jakarta.persistence.LockModeType;
import jakarta.persistence.StatementReference;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.CriteriaStatement;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.sql.ResultSetMapping;

/**
 * @author Jan Schatteman
 */
public abstract class StatelessSessionLazyDelegator implements StatelessSession {

	public abstract StatelessSession delegate();

	@Override
	public void insert(@Nonnull Object entity) {
		delegate().insert( entity );
	}

	@Override
	public void insertMultiple(@Nonnull List<?> entities) {
		delegate().insertMultiple( entities );
	}

	@Override
	public Object insert(@Nonnull String entityName, @Nonnull Object entity) {
		return delegate().insert( entityName, entity );
	}

	@Override
	public void update(@Nonnull Object entity) {
		delegate().update( entity );
	}

	@Override
	public void updateMultiple(@Nonnull List<?> entities) {
		delegate().updateMultiple( entities );
	}

	@Override
	public void update(@Nonnull String entityName, @Nonnull Object entity) {
		delegate().update( entityName, entity );
	}

	@Override
	public void delete(@Nonnull Object entity) {
		delegate().delete( entity );
	}

	@Override
	public void deleteMultiple(@Nonnull List<?> entities) {
		delegate().deleteMultiple( entities );
	}

	@Override
	public void delete(@Nonnull String entityName, @Nonnull Object entity) {
		delegate().delete( entityName, entity );
	}

	@Override
	public void upsert(@Nonnull Object entity) {
		delegate().upsert( entity );
	}

	@Override
	public void upsertMultiple(@Nonnull List<?> entities) {
		delegate().upsertMultiple( entities );
	}

	@Override
	public void upsert(@Nonnull String entityName, @Nonnull Object entity) {
		delegate().upsert( entityName, entity );
	}

	@Deprecated(forRemoval = true, since = "8.0")
	@Override
	@Nonnull
	public Object get(@Nonnull String entityName, @Nonnull Object id, @Nonnull LockMode lockMode) {
		return delegate().get( entityName, id, lockMode );
	}

	@Deprecated(forRemoval = true, since = "8.0")
	@Override
	@Nonnull
	public <T> T get(@Nonnull EntityGraph<T> graph, @Nonnull GraphSemantic graphSemantic, @Nonnull Object id) {
		return delegate().get( graph, graphSemantic, id );
	}

	@Deprecated(forRemoval = true, since = "8.0")
	@Override
	@Nonnull
	public <T> T get(@Nonnull EntityGraph<T> graph, @Nonnull GraphSemantic graphSemantic, @Nonnull Object id, @Nonnull LockMode lockMode) {
		return delegate().get( graph, graphSemantic, id, lockMode );
	}

	@Deprecated(forRemoval = true, since = "8.0")
	@Override
	@Nonnull
	public <T> List<T> getMultiple(@Nonnull EntityGraph<T> entityGraph, @Nonnull GraphSemantic graphSemantic, @Nonnull List<?> ids) {
		return delegate().getMultiple( entityGraph, graphSemantic, ids );
	}

	@Override
	public void refresh(@Nonnull Object entity) {
		delegate().refresh( entity );
	}

	@Override
	public void refresh(@Nonnull String entityName, @Nonnull Object entity) {
		delegate().refresh( entityName, entity );
	}

	@Override
	public void refresh(@Nonnull Object entity, @Nonnull LockMode lockMode) {
		delegate().refresh( entity, lockMode );
	}

	@Override
	public void refresh(@Nonnull String entityName, @Nonnull Object entity, @Nonnull LockMode lockMode) {
		delegate().refresh( entityName, entity, lockMode );
	}

	@Override
	public void refresh(@Nonnull Object entity, @Nonnull LockModeType lockModeType) {
		delegate().refresh( entity, lockModeType );
	}

	@Override
	public void refreshMultiple(@Nonnull List<?> entities) {
		delegate().refreshMultiple( entities );
	}

	@Override
	@Nonnull
	public <T> T fetch(@Nonnull T association) {
		return delegate().fetch( association );
	}

	@Nullable
	@Override
	public Object getIdentifier(@Nonnull Object entity) {
		return delegate().getIdentifier( entity );
	}

	@Nullable
	@Override
	public String getTenantIdentifier() {
		return delegate().getTenantIdentifier();
	}

	@Nullable
	@Override
	public Object getTenantIdentifierValue() {
		return delegate().getTenantIdentifierValue();
	}

	@Nonnull
	@Override
	public CacheMode getCacheMode() {
		return delegate().getCacheMode();
	}

	@Override
	public void setCacheMode(@Nonnull CacheMode cacheMode) {
		delegate().setCacheMode( cacheMode );
	}

	@Override
	public void setCacheRetrieveMode(@Nonnull CacheRetrieveMode cacheRetrieveMode) {
		delegate().setCacheRetrieveMode( cacheRetrieveMode );
	}

	@Override
	public void setCacheStoreMode(@Nonnull CacheStoreMode cacheStoreMode) {
		delegate().setCacheStoreMode( cacheStoreMode );
	}

	@Nonnull
	@Override
	public CacheStoreMode getCacheStoreMode() {
		return delegate().getCacheStoreMode();
	}

	@Nonnull
	@Override
	public CacheRetrieveMode getCacheRetrieveMode() {
		return delegate().getCacheRetrieveMode();
	}

	@Override
	public void addOption(@Nonnull EntityAgent.Option option) {
		delegate().addOption( option );
	}

	@Nonnull
	@Override
	public Set<EntityAgent.Option> getOptions() {
		return delegate().getOptions();
	}

	@Override
	public void close() {
		delegate().close();
	}

	@Override
	public boolean isOpen() {
		return delegate().isOpen();
	}

	@Override
	public boolean isConnected() {
		return delegate().isConnected();
	}

	@Nonnull
	@Override
	public Transaction beginTransaction() {
		return delegate().beginTransaction();
	}

	@Nonnull
	@Override
	public Transaction getTransaction() {
		return delegate().getTransaction();
	}

	@Override
	public void joinTransaction() {
		delegate().joinTransaction();
	}

	@Override
	public boolean isJoinedToTransaction() {
		return delegate().isJoinedToTransaction();
	}

	@Nullable
	@Override
	public Object find(@Nonnull String entityName, @Nonnull Object key, @Nullable FindOption... findOptions) {
		return delegate().find( entityName, key, findOptions );
	}

	@Nonnull
	@Override
	public Object get(@Nonnull String entityName, @Nonnull Object key, @Nullable FindOption... findOptions) {
		return delegate().get( entityName, key, findOptions );
	}

	@Nonnull
	@Override
	public <E> List<E> findMultiple(@Nonnull Class<E> entityType, @Nonnull List<?> ids, @Nullable FindOption... options) {
		return delegate().findMultiple( entityType, ids, options );
	}

	@Nonnull
	@Override
	public <E> List<E> findMultiple(@Nonnull EntityGraph<E> entityGraph, @Nonnull List<?> ids, @Nullable FindOption... options) {
		return delegate().findMultiple( entityGraph, ids, options );
	}

	@Nullable
	@Override
	public <T> T find(@Nonnull Class<T> entityClass, @Nonnull Object primaryKey) {
		return delegate().find( entityClass, primaryKey );
	}

	@Nullable
	@Override
	public <T> T find(@Nonnull Class<T> entityClass, @Nonnull Object primaryKey, @Nullable FindOption... options) {
		return delegate().find( entityClass, primaryKey, options );
	}

	@Nullable
	@Override
	public <T> T find(@Nonnull EntityGraph<T> entityGraph, @Nonnull Object primaryKey, @Nullable FindOption... options) {
		return delegate().find( entityGraph, primaryKey, options );
	}

	@Nonnull
	@Override
	public <T> T get(@Nonnull Class<T> entityType, @Nonnull Object id) {
		return delegate().get( entityType, id );
	}

	@Nonnull
	@Override
	public <T> T get(@Nonnull Class<T> entityType, @Nonnull Object key, @Nullable FindOption... findOptions) {
		return delegate().get( entityType, key, findOptions );
	}

	@Nonnull
	@Override
	public <T> T get(@Nonnull EntityGraph<T> entityGraph, @Nonnull Object key, @Nullable FindOption... findOptions) {
		return delegate().get( entityGraph, key, findOptions );
	}

	@Nonnull
	@Override
	public <T> List<T> getMultiple(@Nonnull Class<T> entityType, @Nonnull List<?> keys, @Nullable FindOption... findOptions) {
		return delegate().getMultiple( entityType, keys, findOptions );
	}

	@Nonnull
	@Override
	public <T> List<T> getMultiple(@Nonnull EntityGraph<T> entityGraph, @Nonnull List<?> keys, @Nullable FindOption... findOptions) {
		return delegate().getMultiple( entityGraph, keys, findOptions );
	}

	@Nonnull
	@Override
	public <R> SelectionQuery<R> createQuery(@Nonnull String queryString, @Nonnull Class<R> resultClass) {
		return delegate().createQuery( queryString, resultClass );
	}

	@Nonnull
	@Override
	public <T> SelectionQuery<T> createQuery(@Nonnull String hqlString, @Nonnull EntityGraph<T> entityGraph) {
		return delegate().createQuery( hqlString, entityGraph );
	}

	@Override
	public <R> SelectionQuery<R> createSelectionQuery(String hqlString, Class<R> resultType) {
		return delegate().createSelectionQuery( hqlString, resultType );
	}

	@Override
	public <R> SelectionQuery<R> createSelectionQuery(String hqlString, EntityGraph<R> resultGraph) {
		return delegate().createSelectionQuery( hqlString, resultGraph );
	}

	@Override
	public MutationQuery createMutationQuery(String hqlString) {
		return delegate().createMutationQuery( hqlString );
	}

	@Nonnull
	@Override
	public MutationQuery createStatement(@Nonnull String hqlString) {
		return delegate().createStatement( hqlString );
	}

	@Nonnull
	@Override
	public MutationQuery createStatement(@Nonnull StatementReference statementReference) {
		return delegate().createStatement( statementReference );
	}

	@Nonnull
	@Override
	public <R> SelectionQuery<R> createQuery(@Nonnull TypedQueryReference<R> typedQueryReference) {
		return delegate().createQuery( typedQueryReference );
	}

	@Nonnull
	@Override
	public <T> SelectionQuery<T> createQuery(@Nonnull CriteriaSelect<T> criteriaSelect) {
		return delegate().createQuery( criteriaSelect );
	}

	@Nonnull
	@Override
	public MutationQuery createStatement(@Nonnull CriteriaStatement<?> criteriaStatement) {
		return delegate().createStatement( criteriaStatement );
	}

	@Nonnull
	@Override
	public <R> SelectionQuery<R> createSelectionQuery(@Nonnull CriteriaSelect<R> criteria) {
		return delegate().createSelectionQuery( criteria );
	}

	@Nonnull
	@Override
	public <R> SelectionQuery<R> createSelectionQuery(@Nonnull jakarta.persistence.criteria.CriteriaQuery<R> criteria) {
		return delegate().createSelectionQuery( criteria );
	}

	@Nonnull
	@Override
	public MutationQuery createMutationQuery(@Nonnull CriteriaStatement<?> criteriaStatement) {
		return delegate().createMutationQuery( criteriaStatement );
	}

	@Nonnull
	@Override
	public MutationQuery createMutationQuery(@Nonnull JpaCriteriaInsert<?> insert) {
		return delegate().createMutationQuery( insert );
	}

	@Nonnull
	@Override
	public NativeQuery<?> createNativeQuery(@Nonnull String sqlString) {
		return delegate().createNativeQuery( sqlString );
	}

	@Nonnull
	@Override
	public <R> NativeQuery<R> createNativeQuery(@Nonnull String sqlString, @Nonnull Class<R> resultClass) {
		return delegate().createNativeQuery( sqlString, resultClass );
	}

	@Override
	public <R> NativeQuery<R> createNativeQuery(String sqlString, Class<R> resultClass, String tableAlias) {
		return delegate().createNativeQuery( sqlString, resultClass, tableAlias );
	}

	@Override
	public <R> NativeQuery<R> createNativeQuery(String sqlString, String resultSetMappingName, Class<R> resultClass) {
		return delegate().createNativeQuery( sqlString, resultSetMappingName, resultClass );
	}

	@Override
	public MutationQuery createNativeMutationQuery(String sqlString) {
		return delegate().createNativeMutationQuery( sqlString );
	}

	@Nonnull
	@Override
	public <R> SelectionQuery<R> createNamedQuery(@Nonnull String name, @Nonnull Class<R> resultClass) {
		return delegate().createNamedQuery( name, resultClass );
	}

	@Nonnull
	@Override
	public MutationQuery createNamedStatement(@Nonnull String name) {
		return delegate().createNamedStatement( name );
	}

	@Nonnull
	@Override
	public <R> NativeQuery<R> createNamedQuery(@Nonnull String name, @Nonnull String resultSetMappingName) {
		return delegate().createNamedQuery( name, resultSetMappingName );
	}

	@Nonnull
	@Override
	public <R> NativeQuery<R> createNamedQuery(@Nonnull String name, @Nonnull String resultSetMappingName, @Nonnull Class<R> resultClass) {
		return delegate().createNamedQuery( name, resultSetMappingName, resultClass );
	}

	@Nonnull
	@Override
	public MutationQuery createNativeStatement(@Nonnull String sql) {
		return delegate().createNativeStatement( sql );
	}

	@Nonnull
	@Override
	public NativeQuery<?> createNativeQuery(@Nonnull String sql, @Nonnull String resultSetMapping) {
		return delegate().createNativeQuery( sql, resultSetMapping );
	}

	@Nonnull
	@Override
	public <T> TypedQuery<T> createNativeQuery(@Nonnull String sql, @Nonnull ResultSetMapping<T> resultSetMapping) {
		return delegate().createNativeQuery( sql, resultSetMapping );
	}

	@Nonnull
	@Override
	public <R> SelectionQuery<R> createNamedSelectionQuery(@Nonnull String name, @Nonnull Class<R> resultType) {
		return delegate().createNamedSelectionQuery( name, resultType );
	}

	@Nonnull
	@Override
	public MutationQuery createNamedMutationQuery(@Nonnull String name) {
		return delegate().createNamedMutationQuery( name );
	}

	@Nonnull
	@Override
	public ProcedureCall getNamedProcedureCall(@Nonnull String name) {
		return delegate().getNamedProcedureCall( name );
	}

	@Nonnull
	@Override
	public ProcedureCall createStoredProcedureCall(@Nonnull String procedureName) {
		return delegate().createStoredProcedureCall( procedureName );
	}

	@Nonnull
	@Override
	public ProcedureCall createStoredProcedureCall(@Nonnull String procedureName, @Nonnull Class<?>... resultClasses) {
		return delegate().createStoredProcedureCall( procedureName, resultClasses );
	}

	@Nonnull
	@Override
	public ProcedureCall createStoredProcedureCall(@Nonnull String procedureName, @Nonnull String... resultSetMappings) {
		return delegate().createStoredProcedureCall( procedureName, resultSetMappings );
	}

	@Nonnull
	@Override
	public ProcedureCall createNamedStoredProcedureQuery(@Nonnull String name) {
		return delegate().createNamedStoredProcedureQuery( name );
	}

	@Nonnull
	@Override
	public ProcedureCall createStoredProcedureQuery(@Nonnull String procedureName) {
		return delegate().createStoredProcedureQuery( procedureName );
	}

	@Nonnull
	@Override
	public ProcedureCall createStoredProcedureQuery(@Nonnull String procedureName, @Nonnull Class<?>... resultClasses) {
		return delegate().createStoredProcedureQuery( procedureName, resultClasses );
	}

	@Nonnull
	@Override
	public ProcedureCall createStoredProcedureQuery(@Nonnull String procedureName, @Nonnull String... resultSetMappings) {
		return delegate().createStoredProcedureQuery( procedureName, resultSetMappings );
	}

	@Nullable
	@Override
	public Integer getJdbcBatchSize() {
		return delegate().getJdbcBatchSize();
	}

	@Override
	public void setJdbcBatchSize(@Nullable Integer jdbcBatchSize) {
		delegate().setJdbcBatchSize( jdbcBatchSize );
	}

	@Nonnull
	@Override
	public HibernateCriteriaBuilder getCriteriaBuilder() {
		return delegate().getCriteriaBuilder();
	}

	@Override
	public void doWork(@Nonnull Work work) {
		delegate().doWork( work );
	}

	@Override
	public <T> T doReturningWork(@Nonnull ReturningWork<T> work) {
		return delegate().doReturningWork( work );
	}

	@Override
	public <C> void runWithConnection(@Nonnull ConnectionConsumer<C> action) {
		delegate().runWithConnection( action );
	}

	@Override
	public <C, T> T callWithConnection(@Nonnull ConnectionFunction<C, T> function) {
		return delegate().callWithConnection( function );
	}

	@Nonnull
	@Override
	public <T> RootGraph<T> createEntityGraph(@Nonnull Class<T> rootType) {
		return delegate().createEntityGraph( rootType );
	}

	@Deprecated(since = "8.0", forRemoval = true)
	@Nullable
	@Override
	public RootGraph<?> createEntityGraph(@Nonnull String graphName) {
		return delegate().createEntityGraph( graphName );
	}

	@Deprecated(since = "8.0", forRemoval = true)
	@Nullable
	@Override
	public <T> RootGraph<T> createEntityGraph(@Nonnull Class<T> rootType, @Nonnull String graphName) {
		return delegate().createEntityGraph( rootType, graphName );
	}

	@Nonnull
	@Override
	public RootGraph<?> getEntityGraph(@Nonnull String graphName) {
		return delegate().getEntityGraph( graphName );
	}

	@Nonnull
	@Override
	public <T> RootGraph<T> getEntityGraph(@Nonnull Class<T> rootType, @Nonnull String graphName) {
		return delegate().getEntityGraph( rootType, graphName );
	}

	@Nonnull
	@Override
	public <T> List<EntityGraph<? super T>> getEntityGraphs(@Nonnull Class<T> entityClass) {
		return delegate().getEntityGraphs( entityClass );
	}

	@Nonnull
	@Override
	public Filter enableFilter(@Nonnull String filterName) {
		return delegate().enableFilter( filterName );
	}

	@Nullable
	@Override
	public Filter getEnabledFilter(@Nonnull String filterName) {
		return delegate().getEnabledFilter( filterName );
	}

	@Override
	public void disableFilter(@Nonnull String filterName) {
		delegate().disableFilter( filterName );
	}

	@Nonnull
	@Override
	public SessionFactory getFactory() {
		return delegate().getFactory();
	}

	@Nonnull
	@Override
	public EntityManagerFactory getEntityManagerFactory() {
		return delegate().getEntityManagerFactory();
	}

	@Nonnull
	@Override
	public Metamodel getMetamodel() {
		return delegate().getMetamodel();
	}

	@Nonnull
	@Override
	public <T> T unwrap(@Nonnull Class<T> type) {
		return type.isAssignableFrom( StatelessSession.class )
				? type.cast( this )
				: delegate().unwrap( type );
	}

	@Nonnull
	@Override
	public Map<String, Object> getProperties() {
		return delegate().getProperties();
	}

	@Override
	public void setProperty(@Nonnull String propertyName, @Nullable Object value) {
		delegate().setProperty( propertyName, value );
	}

	@Override
	public SharedStatelessSessionBuilder statelessWithOptions() {
		return delegate().statelessWithOptions();
	}

	@Nonnull
	@Override
	public SharedSessionBuilder sessionWithOptions() {
		return delegate().sessionWithOptions();
	}

	@Nonnull
	@Override
	public MutationOrSelectionQuery createQuery(@Nonnull String queryString) {
		return delegate().createQuery( queryString );
	}

	@Nonnull
	@Override
	public MutationOrSelectionQuery createNamedQuery(@Nonnull String name) {
		return delegate().createNamedQuery( name );
	}
}
