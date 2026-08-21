/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;


import jakarta.persistence.EntityGraph;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import org.hibernate.CacheMode;
import org.hibernate.Filter;
import org.hibernate.HibernateException;
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
import org.hibernate.query.MutationQuery;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaInsert;

import java.util.List;

/**
 * @author Jan Schatteman
 */
public abstract class StatelessSessionLazyDelegator implements StatelessSession {

	public abstract StatelessSession delegate();

	@Override
	public void delete(Object entity) {
		delegate().delete(entity);
	}

	@Override
	public Object insert(Object entity) {
		return delegate().insert( entity );
	}

	@Override
	public void insertMultiple(List<?> entities) {
		delegate().insertMultiple(entities);
	}

	@Override
	public Object insert(String entityName, Object entity) {
		return delegate().insert(entityName, entity);
	}

	@Override
	public void update(Object entity) {
		delegate().update(entity);
	}

	@Override
	public void updateMultiple(List<?> entities) {
		delegate().updateMultiple(entities);
	}

	@Override
	public void update(String entityName, Object entity) {
		delegate().update(entityName, entity);
	}

	@Override
	public void deleteMultiple(List<?> entities) {
		delegate().deleteMultiple(entities);
	}

	@Override
	public void delete(String entityName, Object entity) {
		delegate().delete(entityName, entity);
	}

	@Override
	public void upsert(Object entity) {
		delegate().upsert(entity);
	}

	@Override
	public void upsertMultiple(List<?> entities) {
		delegate().upsertMultiple(entities);
	}

	@Override
	public void upsert(String entityName, Object entity) {
		delegate().upsert(entityName, entity);
	}

	@Override
	public Object get(String entityName, Object id) {
		return delegate().get(entityName, id);
	}

	@Override
	public <T> T get(Class<T> entityClass, Object id) {
		return delegate().get(entityClass, id);
	}

	@Override
	public Object get(String entityName, Object id, LockMode lockMode) {
		return delegate().get(entityName, id, lockMode);
	}

	@Override
	public <T> T get(Class<T> entityClass, Object id, LockMode lockMode) {
		return delegate().get(entityClass, id, lockMode);
	}

	@Override
	public <T> T get(EntityGraph<T> graph, Object id) {
		return delegate().get(graph, id);
	}

	@Override
	public <T> T get(EntityGraph<T> graph, Object id, LockMode lockMode) {
		return delegate().get(graph, id, lockMode);
	}

	@Override
	public <T> T get(EntityGraph<T> graph, GraphSemantic graphSemantic, Object id) {
		return delegate().get(graph, graphSemantic, id);
	}

	@Override
	public <T> T get(EntityGraph<T> graph, GraphSemantic graphSemantic, Object id, LockMode lockMode) {
		return delegate().get(graph, graphSemantic, id, lockMode);
	}

	@Override
	public <T> List<T> getMultiple(Class<T> entityClass, List<?> ids) {
		return delegate().getMultiple(entityClass, ids);
	}

	@Override
	public <T> List<T> getMultiple(Class<T> entityClass, List<?> ids, LockMode lockMode) {
		return delegate().getMultiple(entityClass, ids, lockMode);
	}

	@Override
	public <T> List<T> getMultiple(EntityGraph<T> entityGraph, List<?> ids) {
		return delegate().getMultiple(entityGraph, ids);
	}

	@Override
	public <T> List<T> getMultiple(EntityGraph<T> entityGraph, GraphSemantic graphSemantic, List<?> ids) {
		return delegate().getMultiple(entityGraph, graphSemantic, ids);
	}

	@Override
	public void refresh(Object entity) {
		delegate().refresh(entity);
	}

	@Override
	public void refresh(String entityName, Object entity) {
		delegate().refresh(entityName, entity);
	}

	@Override
	public void refresh(Object entity, LockMode lockMode) {
		delegate().refresh(entity, lockMode);
	}

	@Override
	public void refresh(String entityName, Object entity, LockMode lockMode) {
		delegate().refresh(entityName, entity, lockMode);
	}

	@Override
	public void fetch(Object association) {
		delegate().fetch(association);
	}

	@Override
	public Object getIdentifier(Object entity) {
		return delegate().getIdentifier(entity);
	}

	@Override
	public SharedStatelessSessionBuilder statelessWithOptions() {
		return delegate().statelessWithOptions();
	}

	@Override
	public SharedSessionBuilder sessionWithOptions() {
		return delegate().sessionWithOptions();
	}

	@Override
	public String getTenantIdentifier() {
		return delegate().getTenantIdentifier();
	}

	@Override
	public Object getTenantIdentifierValue() {
		return delegate().getTenantIdentifierValue();
	}

	@Override
	public CacheMode getCacheMode() {
		return delegate().getCacheMode();
	}

	@Override
	public void setCacheMode(CacheMode cacheMode) {
		delegate().setCacheMode(cacheMode);
	}

	@Override
	public void close() throws HibernateException {
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

	@Override
	public Transaction beginTransaction() {
		return delegate().beginTransaction();
	}

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

	@Override
	public ProcedureCall getNamedProcedureCall(String name) {
		return delegate().getNamedProcedureCall(name);
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName) {
		return delegate().createStoredProcedureCall(procedureName);
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, Class<?>... resultClasses) {
		return delegate().createStoredProcedureCall(procedureName, resultClasses);
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
		return delegate().createStoredProcedureCall(procedureName, resultSetMappings);
	}

	@Override
	public ProcedureCall createNamedStoredProcedureQuery(String name) {
		return delegate().createNamedStoredProcedureQuery(name);
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName) {
		return delegate().createStoredProcedureQuery(procedureName);
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName, Class<?>... resultClasses) {
		return delegate().createStoredProcedureQuery(procedureName, resultClasses);
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
		return delegate().createStoredProcedureQuery(procedureName, resultSetMappings);
	}

	@Override
	public Integer getJdbcBatchSize() {
		return delegate().getJdbcBatchSize();
	}

	@Override
	public void setJdbcBatchSize(Integer jdbcBatchSize) {
		delegate().setJdbcBatchSize(jdbcBatchSize);
	}

	@Override
	public HibernateCriteriaBuilder getCriteriaBuilder() {
		return delegate().getCriteriaBuilder();
	}

	@Override
	public void doWork(Work work) throws HibernateException {
		delegate().doWork(work);
	}

	@Override
	public <T> T doReturningWork(ReturningWork<T> work) {
		return delegate().doReturningWork(work);
	}

	@Override
	public <T> RootGraph<T> createEntityGraph(Class<T> rootType) {
		return delegate().createEntityGraph(rootType);
	}

	@Override
	public RootGraph<?> createEntityGraph(String graphName) {
		return delegate().createEntityGraph(graphName);
	}

	@Override
	public <T> RootGraph<T> createEntityGraph(Class<T> rootType, String graphName) {
		return delegate().createEntityGraph(rootType, graphName);
	}

	@Override
	public RootGraph<?> getEntityGraph(String graphName) {
		return delegate().getEntityGraph(graphName);
	}

	@Override
	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
		return delegate().getEntityGraphs(entityClass);
	}

	@Override
	public Filter enableFilter(String filterName) {
		return delegate().enableFilter(filterName);
	}

	@Override
	public Filter getEnabledFilter(String filterName) {
		return delegate().getEnabledFilter(filterName);
	}

	@Override
	public void disableFilter(String filterName) {
		delegate().disableFilter(filterName);
	}

	@Override
	public SessionFactory getFactory() {
		return delegate().getFactory();
	}

	@Override
	@Deprecated
	public Query createQuery(String queryString) {
		return delegate().createQuery(queryString);
	}

	@Override
	public <R> Query<R> createQuery(String queryString, Class<R> resultClass) {
		return delegate().createQuery(queryString, resultClass);
	}

	@Override
	public <R> Query<R> createQuery(TypedQueryReference<R> typedQueryReference) {
		return delegate().createQuery(typedQueryReference);
	}

	@Override
	public <R> Query<R> createQuery(CriteriaQuery<R> criteriaQuery) {
		return delegate().createQuery(criteriaQuery);
	}

	@Override
	@Deprecated
	public Query createQuery(CriteriaUpdate updateQuery) {
		return delegate().createQuery(updateQuery);
	}

	@Override
	@Deprecated
	public Query createQuery(CriteriaDelete deleteQuery) {
		return delegate().createQuery(deleteQuery);
	}

	@Override
	@Deprecated
	public NativeQuery createNativeQuery(String sqlString) {
		return delegate().createNativeQuery(sqlString);
	}

	@Override
	public <R> NativeQuery<R> createNativeQuery(String sqlString, Class<R> resultClass) {
		return delegate().createNativeQuery(sqlString, resultClass);
	}

	@Override
	public <R> NativeQuery<R> createNativeQuery(String sqlString, Class<R> resultClass, String tableAlias) {
		return delegate().createNativeQuery(sqlString, resultClass, tableAlias);
	}

	@Override
	@Deprecated
	public NativeQuery createNativeQuery(String sqlString, String resultSetMappingName) {
		return delegate().createNativeQuery(sqlString, resultSetMappingName);
	}

	@Override
	public <R> NativeQuery<R> createNativeQuery(String sqlString, String resultSetMappingName, Class<R> resultClass) {
		return delegate().createNativeQuery(sqlString, resultSetMappingName, resultClass);
	}

	@Override
	@Deprecated
	public SelectionQuery<?> createSelectionQuery(String hqlString) {
		return delegate().createSelectionQuery(hqlString);
	}

	@Override
	public <R> SelectionQuery<R> createSelectionQuery(String hqlString, Class<R> resultType) {
		return delegate().createSelectionQuery(hqlString, resultType);
	}

	@Override
	public <R> SelectionQuery<R> createSelectionQuery(String hqlString, EntityGraph<R> resultGraph) {
		return delegate().createSelectionQuery(hqlString, resultGraph);
	}

	@Override
	public <R> SelectionQuery<R> createSelectionQuery(CriteriaQuery<R> criteria) {
		return delegate().createSelectionQuery(criteria);
	}

	@Override
	public MutationQuery createMutationQuery(String hqlString) {
		return delegate().createMutationQuery(hqlString);
	}

	@Override
	public MutationQuery createMutationQuery(CriteriaUpdate updateQuery) {
		return delegate().createMutationQuery(updateQuery);
	}

	@Override
	public MutationQuery createMutationQuery(CriteriaDelete deleteQuery) {
		return delegate().createMutationQuery(deleteQuery);
	}

	@Override
	public MutationQuery createMutationQuery(JpaCriteriaInsert insert) {
		return delegate().createMutationQuery(insert);
	}

	@Override
	public MutationQuery createNativeMutationQuery(String sqlString) {
		return delegate().createNativeMutationQuery(sqlString);
	}

	@Override
	@Deprecated
	public Query createNamedQuery(String name) {
		return delegate().createNamedQuery(name);
	}

	@Override
	public <R> Query<R> createNamedQuery(String name, Class<R> resultClass) {
		return delegate().createNamedQuery(name, resultClass);
	}

	@Override
	@Deprecated
	public SelectionQuery<?> createNamedSelectionQuery(String name) {
		return delegate().createNamedSelectionQuery(name);
	}

	@Override
	public <R> SelectionQuery<R> createNamedSelectionQuery(String name, Class<R> resultType) {
		return delegate().createNamedSelectionQuery(name, resultType);
	}

	@Override
	public MutationQuery createNamedMutationQuery(String name) {
		return delegate().createNamedMutationQuery(name);
	}

	@Override
	@Deprecated
	public Query getNamedQuery(String queryName) {
		return delegate().getNamedQuery(queryName);
	}

	@Override
	@Deprecated
	public NativeQuery getNamedNativeQuery(String name) {
		return delegate().getNamedNativeQuery(name);
	}

	@Override
	@Deprecated
	public NativeQuery getNamedNativeQuery(String name, String resultSetMapping) {
		return delegate().getNamedNativeQuery(name, resultSetMapping);
	}

	@Override
	public <T> T unwrap(Class<T> type) {
		return type.isAssignableFrom( StatelessSession.class )
				? type.cast( this )
				: delegate().unwrap( type );
	}

}
