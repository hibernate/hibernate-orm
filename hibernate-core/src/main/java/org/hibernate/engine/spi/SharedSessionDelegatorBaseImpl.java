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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SharedSessionContract;
import org.hibernate.SharedStatelessSessionBuilder;
import org.hibernate.Transaction;
import org.hibernate.bytecode.enhance.spi.interceptor.SessionAssociationMarkers;
import org.hibernate.cache.spi.CacheTransactionSynchronization;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaInsert;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.QueryProducerImplementor;
import org.hibernate.query.sql.spi.NativeQueryImplementor;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.type.format.FormatMapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

/**
 * A wrapper class that delegates all method invocations to a delegate instance of
 * {@link SharedSessionContractImplementor}. This is useful for custom implementations
 * of that API, so that only some methods need to be overridden
 *
 * @author Gavin King
 */
@SuppressWarnings("deprecation")
public class SharedSessionDelegatorBaseImpl implements SharedSessionContractImplementor {

	protected final SharedSessionContractImplementor delegate;

	public SharedSessionDelegatorBaseImpl(SessionImplementor delegate) {
		this.delegate = delegate;
	}

	/**
	 * Returns the delegate session.
	 */
	protected SharedSessionContract delegate() {
		return delegate;
	}

	@Override
	public SharedStatelessSessionBuilder statelessWithOptions() {
		return delegate.statelessWithOptions();
	}

	@Override
	public String getTenantIdentifier() {
		return delegate.getTenantIdentifier();
	}

	@Override
	public Object getTenantIdentifierValue() {
		return delegate.getTenantIdentifierValue();
	}

	private QueryProducerImplementor queryDelegate() {
		return delegate;
	}

	@Override
	public MutationQuery createMutationQuery(@SuppressWarnings("rawtypes") CriteriaUpdate updateQuery) {
		//noinspection resource
		return delegate().createMutationQuery( updateQuery );
	}

	@Override
	public MutationQuery createMutationQuery(@SuppressWarnings("rawtypes") CriteriaDelete deleteQuery) {
		//noinspection resource
		return delegate().createMutationQuery( deleteQuery );
	}

	@Override
	public MutationQuery createMutationQuery(@SuppressWarnings("rawtypes") JpaCriteriaInsert insert) {
		//noinspection resource
		return delegate().createMutationQuery( insert );
	}

	@Override
	public <T> QueryImplementor<T> createQuery(CriteriaQuery<T> criteriaQuery) {
		return queryDelegate().createQuery( criteriaQuery );
	}

	@Override
	public @SuppressWarnings("rawtypes") QueryImplementor createQuery(CriteriaUpdate updateQuery) {
		return queryDelegate().createQuery( updateQuery );
	}

	@Override
	public @SuppressWarnings("rawtypes") QueryImplementor createQuery(CriteriaDelete deleteQuery) {
		return queryDelegate().createQuery( deleteQuery );
	}

	@Override
	public @SuppressWarnings("rawtypes") QueryImplementor getNamedQuery(String name) {
		return queryDelegate().getNamedQuery( name );
	}

	@Override
	public @SuppressWarnings("rawtypes") NativeQueryImplementor getNamedNativeQuery(String name) {
		return queryDelegate().getNamedNativeQuery( name );
	}

	@Override
	public @SuppressWarnings("rawtypes") NativeQueryImplementor getNamedNativeQuery(String name, String resultSetMapping) {
		return queryDelegate().getNamedNativeQuery( name, resultSetMapping );
	}

	@Override
	public @SuppressWarnings("rawtypes") QueryImplementor createQuery(String queryString) {
		return queryDelegate().createQuery( queryString );
	}

	@Override
	public SelectionQuery<?> createSelectionQuery(String hqlString) {
		return queryDelegate().createSelectionQuery( hqlString );
	}

	@Override
	public <R> SelectionQuery<R> createSelectionQuery(String hqlString, Class<R> resultType) {
		return queryDelegate().createSelectionQuery( hqlString, resultType );
	}

	@Override
	public <R> SelectionQuery<R> createSelectionQuery(String hqlString, EntityGraph<R> resultGraph) {
		return queryDelegate().createSelectionQuery( hqlString, resultGraph );
	}

	@Override
	public <R> SelectionQuery<R> createSelectionQuery(CriteriaQuery<R> criteria) {
		return queryDelegate().createSelectionQuery( criteria );
	}

	@Override
	public <T> QueryImplementor<T> createQuery(String queryString, Class<T> resultType) {
		return queryDelegate().createQuery( queryString, resultType );
	}

	@Override
	public <R> QueryImplementor<R> createQuery(TypedQueryReference<R> typedQueryReference) {
		return queryDelegate().createQuery( typedQueryReference );
	}

	@Override
	public @SuppressWarnings("rawtypes") QueryImplementor createNamedQuery(String name) {
		return queryDelegate().createNamedQuery( name );
	}

	@Override
	public <T> QueryImplementor<T> createNamedQuery(String name, Class<T> resultClass) {
		return queryDelegate().createNamedQuery( name, resultClass );
	}

	@Override
	public SelectionQuery<?> createNamedSelectionQuery(String name) {
		//noinspection resource
		return delegate().createNamedSelectionQuery( name );
	}

	@Override
	public <R> SelectionQuery<R> createNamedSelectionQuery(String name, Class<R> resultType) {
		//noinspection resource
		return delegate().createNamedSelectionQuery( name, resultType );
	}

	@Override
	public @SuppressWarnings("rawtypes") NativeQueryImplementor createNativeQuery(String sqlString) {
		return queryDelegate().createNativeQuery( sqlString );
	}

	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	//note: we're doing something a bit funny here to work around
	//      the clashing signatures declared by the supertypes
	public NativeQueryImplementor createNativeQuery(String sqlString, Class resultClass) {
		return queryDelegate().createNativeQuery( sqlString, resultClass );
	}

	@Override
	public <T> NativeQueryImplementor<T> createNativeQuery(String sqlString, Class<T> resultClass, String tableAlias) {
		return queryDelegate().createNativeQuery( sqlString, resultClass, tableAlias );
	}

	@Override
	public @SuppressWarnings("rawtypes") NativeQueryImplementor createNativeQuery(String sqlString, String resultSetMappingName) {
		return queryDelegate().createNativeQuery( sqlString, resultSetMappingName );
	}

	@Override
	public <T> NativeQueryImplementor<T> createNativeQuery(String sqlString, String resultSetMappingName, Class<T> resultClass) {
		return queryDelegate().createNativeQuery( sqlString, resultSetMappingName, resultClass );
	}

	@Override
	public MutationQuery createMutationQuery(String statementString) {
		return delegate.createMutationQuery( statementString );
	}

	@Override
	public MutationQuery createNamedMutationQuery(String name) {
		return delegate.createNamedMutationQuery( name );
	}

	@Override
	public MutationQuery createNativeMutationQuery(String sqlString) {
		return delegate.createNativeMutationQuery( sqlString );
	}

	@Override
	public ProcedureCall createNamedStoredProcedureQuery(String name) {
		return delegate.createNamedStoredProcedureQuery( name );
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName) {
		return delegate.createStoredProcedureQuery( procedureName );
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName, Class<?>... resultClasses) {
		return delegate.createStoredProcedureQuery( procedureName, resultClasses );
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
		return delegate.createStoredProcedureQuery( procedureName, resultSetMappings );
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
	public ProcedureCall createStoredProcedureCall(String procedureName, Class<?>... resultClasses) {
		return delegate.createStoredProcedureCall( procedureName, resultClasses );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
		return delegate.createStoredProcedureCall( procedureName, resultSetMappings );
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
	public void close() throws HibernateException {
		delegate.close();
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
	public Transaction beginTransaction() {
		return delegate.beginTransaction();
	}

	@Override
	public Transaction getTransaction() {
		return delegate.getTransaction();
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
	public HibernateCriteriaBuilder getCriteriaBuilder() {
		return delegate.getCriteriaBuilder();
	}

	@Override
	public Integer getJdbcBatchSize() {
		return delegate.getJdbcBatchSize();
	}

	@Override
	public EventMonitor getEventMonitor() {
		return delegate.getEventMonitor();
	}

	@Override
	public void setJdbcBatchSize(Integer jdbcBatchSize) {
		delegate.setJdbcBatchSize( jdbcBatchSize );
	}

	@Override
	public <T> T execute(Callback<T> callback) {
		return delegate.execute( callback );
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return delegate.getFactory();
	}

	@Override
	public SessionEventListenerManager getEventListenerManager() {
		return delegate.getEventListenerManager();
	}

	@Override
	public PersistenceContext getPersistenceContext() {
		return delegate.getPersistenceContext();
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
	public UUID getSessionIdentifier() {
		return delegate.getSessionIdentifier();
	}

	@Override
	public boolean isClosed() {
		return delegate.isClosed();
	}

	@Override
	public void checkOpen(boolean markForRollbackIfClosed) {
		delegate.checkOpen( markForRollbackIfClosed );
	}

	@Override
	public void prepareForQueryExecution(boolean requiresTxn) {
		delegate.prepareForQueryExecution( requiresTxn );
	}

	@Override
	public void markForRollbackOnly() {
		delegate.markForRollbackOnly();
	}

	@Override
	public CacheTransactionSynchronization getCacheTransactionSynchronization() {
		return delegate.getCacheTransactionSynchronization();
	}

	@Override
	public boolean isTransactionInProgress() {
		return delegate.isTransactionInProgress();
	}

	@Override
	public Transaction accessTransaction() {
		return delegate.accessTransaction();
	}

	@Override
	public TransactionCompletionCallbacks getTransactionCompletionCallbacks() {
		return delegate.getTransactionCompletionCallbacks();
	}

	@Override
	public EntityKey generateEntityKey(Object id, EntityPersister persister) {
		return delegate.generateEntityKey( id, persister );
	}

	@Override
	public Interceptor getInterceptor() {
		return delegate.getInterceptor();
	}

	@Override
	public void initializeCollection(PersistentCollection<?> collection, boolean writing) throws HibernateException {
		delegate.initializeCollection( collection, writing );
	}

	@Override
	public Object internalLoad(String entityName, Object id, boolean eager, boolean nullable) throws HibernateException {
		return delegate.internalLoad( entityName, id, eager, nullable );
	}

	@Override
	public Object immediateLoad(String entityName, Object id) throws HibernateException {
		return delegate.immediateLoad( entityName, id );
	}

	@Override
	public EntityPersister getEntityPersister(@Nullable String entityName, Object object) throws HibernateException {
		return delegate.getEntityPersister( entityName, object );
	}

	@Override
	public Object getEntityUsingInterceptor(EntityKey key) throws HibernateException {
		return delegate.getEntityUsingInterceptor( key );
	}

	@Override
	public Object getContextEntityIdentifier(Object object) {
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

	@Override @Deprecated
	public Object instantiate(String entityName, Object id) throws HibernateException {
		return delegate.instantiate( entityName, id );
	}

	@Override
	public Object instantiate(EntityPersister persister, Object id) throws HibernateException {
		return delegate.instantiate( persister, id );
	}

	@Override
	public boolean isDefaultReadOnly() {
		return delegate.isDefaultReadOnly();
	}

	@Override
	public CacheMode getCacheMode() {
		return delegate.getCacheMode();
	}

	@Override
	public void setCacheMode(CacheMode cacheMode) {
		delegate.setCacheMode( cacheMode );
	}

	@Override
	public void setCriteriaCopyTreeEnabled(boolean jpaCriteriaCopyComplianceEnabled) {
		delegate.setCriteriaCopyTreeEnabled( jpaCriteriaCopyComplianceEnabled );
	}

	@Override
	public boolean isCriteriaCopyTreeEnabled() {
		return delegate.isCriteriaCopyTreeEnabled();
	}

	@Override
	public boolean isCriteriaPlanCacheEnabled() {
		return delegate.isCriteriaPlanCacheEnabled();
	}

	@Override
	public void setCriteriaPlanCacheEnabled(boolean jpaCriteriaCacheEnabled) {
		delegate.setCriteriaPlanCacheEnabled( jpaCriteriaCacheEnabled );
	}

	@Override
	public boolean getNativeJdbcParametersIgnored() {
		return delegate.getNativeJdbcParametersIgnored();
	}

	@Override
	public void setNativeJdbcParametersIgnored(boolean nativeJdbcParametersIgnored) {
		delegate.setNativeJdbcParametersIgnored( nativeJdbcParametersIgnored );
	}

	@Override
	public FlushMode getHibernateFlushMode() {
		return delegate.getHibernateFlushMode();
	}

	@Override
	public void flush() {
		delegate.flush();
	}

	@Override
	public void afterScrollOperation() {
		delegate.afterScrollOperation();
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
	public PersistenceContext getPersistenceContextInternal() {
		return delegate.getPersistenceContextInternal();
	}

	@Override
	public boolean autoFlushIfRequired(Set<String> querySpaces) throws HibernateException {
		return delegate.autoFlushIfRequired( querySpaces );
	}

	@Override
	public boolean autoFlushIfRequired(Set<String> querySpaces, boolean skipPreFlush)
			throws HibernateException {
		return delegate.autoFlushIfRequired( querySpaces, skipPreFlush );
	}

	@Override
	public void autoPreFlush() {
		delegate.autoPreFlush();
	}

	@Override
	public void afterOperation(boolean success) {
		delegate.afterOperation( success );
	}

	@Override
	public JdbcSessionContext getJdbcSessionContext() {
		return delegate.getJdbcSessionContext();
	}

	@Override
	public JdbcConnectionAccess getJdbcConnectionAccess() {
		return delegate.getJdbcConnectionAccess();
	}

	@Override
	public TransactionCoordinator getTransactionCoordinator() {
		return delegate.getTransactionCoordinator();
	}

	@Override
	public void startTransactionBoundary() {
		delegate.startTransactionBoundary();
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
	public boolean shouldAutoJoinTransaction() {
		return delegate.shouldAutoJoinTransaction();
	}

	@Override
	public boolean useStreamForLobBinding() {
		return delegate.useStreamForLobBinding();
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return delegate.getPreferredSqlTypeCodeForBoolean();
	}

	@Override
	public LobCreator getLobCreator() {
		return delegate.getLobCreator();
	}

	@Override
	public TimeZone getJdbcTimeZone() {
		return delegate.getJdbcTimeZone();
	}

	@Override
	public <T> RootGraphImplementor<T> createEntityGraph(Class<T> rootType) {
		return delegate.createEntityGraph( rootType );
	}

	@Override
	public RootGraphImplementor<?> createEntityGraph(String graphName) {
		return delegate.createEntityGraph( graphName );
	}

	@Override
	public <T> RootGraph<T> createEntityGraph(Class<T> rootType, String graphName) {
		return delegate.createEntityGraph( rootType, graphName );
	}

	@Override
	public RootGraphImplementor<?> getEntityGraph(String graphName) {
		return delegate.getEntityGraph( graphName );
	}

	@Override
	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
		return delegate.getEntityGraphs( entityClass );
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
	public FormatMapper getJsonFormatMapper() {
		return delegate.getJsonFormatMapper();
	}

	@Override
	public FormatMapper getXmlFormatMapper() {
		return delegate.getXmlFormatMapper();
	}

	@Override
	public void lock(String entityName, Object child, LockOptions lockOptions) {
		delegate.lock( entityName, child, lockOptions );
	}

	@Override
	public Object loadFromSecondLevelCache(EntityPersister persister, EntityKey entityKey, Object instanceToLoad, LockMode lockMode) {
		return delegate.loadFromSecondLevelCache( persister, entityKey, instanceToLoad, lockMode );
	}

	@Override
	public SessionAssociationMarkers getSessionAssociationMarkers() {
		return delegate.getSessionAssociationMarkers();
	}

	@Override
	public boolean isIdentifierRollbackEnabled() {
		return delegate.isIdentifierRollbackEnabled();
	}

	@Override
	public void afterObtainConnection(Connection connection) throws SQLException {
		delegate.afterObtainConnection( connection );
	}

	@Override
	public void beforeReleaseConnection(Connection connection) throws SQLException {
		delegate.beforeReleaseConnection(  connection );
	}

	@Override
	public Transaction getCurrentTransaction() {
		return delegate.getCurrentTransaction();
	}

	@Override
	public SharedSessionBuilder sessionWithOptions() {
		return delegate.sessionWithOptions();
	}

	@Override
	public TransactionCompletionCallbacksImplementor getTransactionCompletionCallbacksImplementor() {
		return delegate.getTransactionCompletionCallbacksImplementor();
	}
}
