/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import org.hibernate.audit.spi.AuditWorkQueue;

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
import jakarta.persistence.Timeout;
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
import org.hibernate.Interceptor;
import org.hibernate.KeyType;
import org.hibernate.LobHelper;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.NaturalIdMultiLoadAccess;
import org.hibernate.SessionEventListener;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SharedStatelessSessionBuilder;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.Transaction;
import org.hibernate.UnknownProfileException;
import org.hibernate.bytecode.enhance.spi.interceptor.SessionAssociationMarkers;
import org.hibernate.cache.spi.CacheTransactionSynchronization;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.extension.spi.Extension;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.MutationOrSelectionQuery;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaInsert;
import org.hibernate.query.spi.MutationQueryImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.SelectionQueryImplementor;
import org.hibernate.query.sql.spi.NativeQueryImplementor;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Supplier;


/**
 * A wrapper class that delegates all method invocations to a delegate instance of
 * {@link SessionImplementor}. This is useful for custom implementations of that
 * API, so that only some methods need to be overridden
 * <p>
 * (Used by Hibernate Search).
 *
 * @author Sanne Grinovero
 */
@SuppressWarnings("deprecation")
public class SessionDelegatorBaseImpl implements SessionImplementor {

	protected final SessionImplementor delegate;

	public SessionDelegatorBaseImpl(SessionImplementor delegate) {
		this.delegate = delegate;
	}

	/**
	 * Returns the delegate session.
	 *
	 * @apiNote This returns a different object to the {@link #getDelegate()}
	 *          method inherited from {@link jakarta.persistence.EntityManager}.
	 *
	 * @see SessionDelegatorBaseImpl#getDelegate()
	 */
	protected SessionImplementor delegate() {
		return delegate;
	}

	@Override
	public <T> T execute(Callback<T> callback) {
		return delegate.execute( callback );
	}

	@Override
	public AuditWorkQueue getAuditWorkQueue() {
		return delegate.getAuditWorkQueue();
	}

	@Override
	@Nonnull
	public SharedStatelessSessionBuilder statelessWithOptions() {
		return delegate.statelessWithOptions();
	}

	@Override
	@Nullable
	public String getTenantIdentifier() {
		return delegate.getTenantIdentifier();
	}

	@Override
	@Nullable
	public Object getTenantIdentifierValue() {
		return delegate.getTenantIdentifierValue();
	}

	@Override
	@Nonnull
	public UUID getSessionIdentifier() {
		return delegate.getSessionIdentifier();
	}

	@Override
	@Nonnull
	public JdbcConnectionAccess getJdbcConnectionAccess() {
		return delegate.getJdbcConnectionAccess();
	}

	@Override
	@Nonnull
	public EntityKey generateEntityKey(@Nonnull Object id, @Nonnull EntityPersister persister) {
		return delegate.generateEntityKey( id, persister );
	}

	@Override
	@Nonnull
	public CollectionKey generateCollectionKey(@Nonnull CollectionPersister persister, @Nonnull Object key) {
		return delegate.generateCollectionKey( persister, key );
	}

	@Override
	@Nonnull
	public Interceptor getInterceptor() {
		return delegate.getInterceptor();
	}

	@Override
	public boolean isTransactionInProgress() {
		return delegate.isTransactionInProgress();
	}

	@Override
	public void checkTransactionNeededForUpdateOperation(@Nonnull String exceptionMessage) {
		delegate.checkTransactionNeededForUpdateOperation( exceptionMessage );
	}

	@Override
	public void initializeCollection(@Nonnull PersistentCollection<?> collection, boolean writing) throws HibernateException {
		delegate.initializeCollection( collection, writing );
	}

	@Override
	public Object internalLoad(@Nonnull String entityName, @Nonnull Object id, boolean eager, boolean nullable) throws HibernateException {
		return delegate.internalLoad( entityName, id, eager, nullable );
	}

	@Override
	public Object immediateLoad(@Nonnull String entityName, @Nonnull Object id) throws HibernateException {
		return delegate.immediateLoad( entityName, id );
	}

	@Override
	@Nonnull
	public SessionFactoryImplementor getFactory() {
		return delegate.getFactory();
	}

	@Override
	@Nonnull
	public EntityPersister getEntityPersister(@Nullable String entityName, @Nonnull Object object) {
		return delegate.getEntityPersister( entityName, object );
	}

	@Override
	public Object getEntityUsingInterceptor(@Nonnull EntityKey key) {
		return delegate.getEntityUsingInterceptor( key );
	}

	@Override
	public Object getContextEntityIdentifier(@Nonnull Object object) {
		return delegate.getContextEntityIdentifier( object );
	}

	@Override
	public String bestGuessEntityName(@Nonnull Object object) {
		return delegate.bestGuessEntityName( object );
	}

	@Override
	public String guessEntityName(@Nonnull Object entity) throws HibernateException {
		return delegate.guessEntityName( entity );
	}

	@Override
	@Nonnull
	public PersistenceContext getPersistenceContext() {
		return delegate.getPersistenceContext();
	}

	@Override
	@Nonnull
	public CacheMode getCacheMode() {
		return delegate.getCacheMode();
	}

	@Override
	@Nonnull
	public CacheRetrieveMode getCacheRetrieveMode() {
		return delegate.getCacheRetrieveMode();
	}

	@Override
	@Nonnull
	public CacheStoreMode getCacheStoreMode() {
		return delegate.getCacheStoreMode();
	}

	@Override
	public void setCacheMode(@Nonnull CacheMode cacheMode) {
		delegate.setCacheMode( cacheMode );
	}

	@Override
	public void setCacheStoreMode(@Nonnull CacheStoreMode cacheStoreMode) {
		delegate.setCacheStoreMode( cacheStoreMode );
	}

	@Override
	public void setCacheRetrieveMode(@Nonnull CacheRetrieveMode cacheRetrieveMode) {
		delegate.setCacheRetrieveMode( cacheRetrieveMode );
	}

	@Override
	public void addOption(@Nonnull EntityManager.Option option) {
		delegate.addOption( option );
	}

	@Override
	@Nonnull
	public Set<EntityManager.Option> getOptions() {
		return delegate.getOptions();
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
	@Nonnull
	public FlushModeType getFlushMode() {
		return delegate.getFlushMode();
	}

	@Override
	public void setFlushMode(@Nonnull FlushModeType flushModeType) {
		delegate.setFlushMode( flushModeType );
	}

	@Override
	public void setHibernateFlushMode(FlushMode flushMode) {
		delegate.setHibernateFlushMode( flushMode );
	}

	@Override
	@Nonnull
	public FlushMode getHibernateFlushMode() {
		return delegate.getHibernateFlushMode();
	}

	@Override
	public void lock(@Nonnull Object entity, @Nonnull LockModeType lockMode) {
		delegate.lock( entity, lockMode );
	}

	@Override
	public void lock(@Nonnull Object entity, @Nonnull LockModeType lockMode, @Nullable Map<String, Object> properties) {
		delegate.lock( entity, lockMode, properties );
	}

	@Override
	public void lock(@Nonnull Object entity, @Nonnull LockModeType lockMode, @Nullable LockOption... options) {
		delegate.lock( entity, lockMode, options );
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
	@Nonnull
	public EventSource asEventSource() {
		return delegate.asEventSource();
	}

	@Override
	public void afterScrollOperation() {
		delegate.afterScrollOperation();
	}

	@Override
	@Nonnull
	public TransactionCoordinator getTransactionCoordinator() {
		return delegate.getTransactionCoordinator();
	}

	@Override
	@Nonnull
	public JdbcCoordinator getJdbcCoordinator() {
		return delegate.getJdbcCoordinator();
	}

	@Override
	@Nonnull
	public JdbcServices getJdbcServices() {
		return delegate.getJdbcServices();
	}

	@Override
	@Nonnull
	public JdbcSessionContext getJdbcSessionContext() {
		return delegate.getJdbcSessionContext();
	}

	@Override
	public boolean isClosed() {
		return delegate.isClosed();
	}

	@Override
	public void checkOpen() {
		delegate.checkOpen();
	}

	@Override
	public boolean isOpenOrWaitingForAutoClose() {
		return delegate.isOpenOrWaitingForAutoClose();
	}

	@Override
	public boolean shouldAutoJoinTransaction() {
		return delegate.shouldAutoJoinTransaction();
	}

	@Override
	public boolean isAutoCloseSessionEnabled() {
		return delegate.isAutoCloseSessionEnabled();
	}

	@Override
	@Nonnull
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return delegate.getLoadQueryInfluencers();
	}

	@Override
	@SuppressWarnings("removal")
	public LockOptions getDefaultLockOptions() {
		return delegate.getDefaultLockOptions();
	}

	@Override
	@Nullable
	public Timeout getDefaultLockTimeout() {
		return delegate.getDefaultLockTimeout();
	}

	@Override
	@Nullable
	public Timeout getDefaultTimeout() {
		return delegate.getDefaultTimeout();
	}

	@Override
	@Nonnull
	public ExceptionConverter getExceptionConverter() {
		return delegate.getExceptionConverter();
	}

	@Override
	@Nonnull
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
	public boolean autoPreFlushIfRequired(QueryParameterBindings parameterBindings) {
		return delegate.autoPreFlushIfRequired( parameterBindings );
	}

	@Override
	public void afterOperation(boolean success) {
		delegate.afterOperation( success );
	}

	@Override
	@Nonnull
	public SessionEventListenerManager getEventListenerManager() {
		return delegate.getEventListenerManager();
	}

	@Override
	@Nonnull
	public Transaction accessTransaction() {
		return delegate.accessTransaction();
	}

	@Override
	@Nullable
	public Transaction getCurrentTransaction() {
		return delegate.getCurrentTransaction();
	}

	@Override
	@Nonnull
	public Transaction beginTransaction() {
		return delegate.beginTransaction();
	}

	@Override
	@Nonnull
	public Transaction getTransaction() {
		return delegate.getTransaction();
	}

	@Override
	public void startTransactionBoundary() {
		delegate.startTransactionBoundary();
	}

	@Override
	public CacheTransactionSynchronization getCacheTransactionSynchronization() {
		return delegate.getCacheTransactionSynchronization();
	}

	@Override
	public Object getCurrentChangesetIdentifier() {
		return delegate.getCurrentChangesetIdentifier();
	}

	@Override
	public @Nullable Object getCurrentChangesetContext() {
		return delegate.getCurrentChangesetContext();
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
	@Nonnull
	public EntityManagerFactory getEntityManagerFactory() {
		return delegate.getFactory();
	}

	@Override
	@Nonnull
	public HibernateCriteriaBuilder getCriteriaBuilder() {
		return delegate.getCriteriaBuilder();
	}

	@Override
	@Nonnull
	public Metamodel getMetamodel() {
		return delegate.getMetamodel();
	}

	@Override
	public <T> @Nonnull T get(@Nonnull Class<T> entityClass, @Nonnull Object key, @Nullable FindOption... findOptions) {
		//noinspection resource
		return delegate().get( entityClass, key, findOptions );
	}

	@Override
	public <T> @Nonnull T get(@Nonnull EntityGraph<T> entityGraph, @Nonnull Object key, @Nullable FindOption... findOptions) {
		//noinspection resource
		return delegate().get( entityGraph, key, findOptions );
	}

	@Override
	@Nonnull
	public <T> List<T> getMultiple(
			@Nonnull Class<T> entityClass,
			@Nonnull List<?> keys,
			@Nullable FindOption... findOptions) {
		//noinspection resource
		return delegate().getMultiple( entityClass, keys, findOptions );
	}

	@Override
	@Nonnull
	public <T> List<T> getMultiple(
			@Nonnull EntityGraph<T> entityGraph,
			@Nonnull List<?> keys,
			@Nullable FindOption... findOptions) {
		//noinspection resource
		return delegate().getMultiple( entityGraph, keys, findOptions );
	}

	@Override
	@Nonnull
	public <T> RootGraph<T> getEntityGraph(@Nonnull Class<T> entityClass, @Nonnull String name) {
		//noinspection resource
		return delegate().getEntityGraph( entityClass, name );
	}

	@Override
	public <C> void runWithConnection(@Nonnull ConnectionConsumer<C> connectionConsumer) {
		//noinspection resource
		delegate().runWithConnection( connectionConsumer );
	}

	@Override
	public <C, T> T callWithConnection(@Nonnull ConnectionFunction<C, T> connectionFunction) {
		//noinspection resource
		return delegate().callWithConnection( connectionFunction );
	}

	@Override
	@Nonnull
	public <T> List<EntityGraph<? super T>> getEntityGraphs(@Nonnull Class<T> entityClass) {
		//noinspection resource
		return delegate().getEntityGraphs( entityClass );
	}

	@Override
	@Nonnull
	public <T> RootGraphImplementor<T> createEntityGraph(@Nonnull Class<T> rootType) {
		return delegate.createEntityGraph( rootType );
	}

	@Override
	@SuppressWarnings("removal")
	@Nullable
	public RootGraphImplementor<?> createEntityGraph(@Nonnull String graphName) {
		return delegate.createEntityGraph( graphName );
	}

	@Override
	@SuppressWarnings("removal")
	@Nullable
	public <T> RootGraph<T> createEntityGraph(@Nonnull Class<T> rootType, @Nonnull String graphName) {
		return delegate.createEntityGraph( rootType, graphName );
	}

	@Override
	@Nonnull
	public RootGraphImplementor<?> getEntityGraph(@Nonnull String graphName) {
		return delegate.getEntityGraph( graphName );
	}

	@Override
	public <T extends Extension> T getExtension(Class<T> extension) {
		return delegate.getExtension( extension );
	}

	private SessionImplementor queryDelegate() {
		return delegate;
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<?> createMutationQuery(@Nonnull CriteriaStatement<?> updateQuery) {
		return delegate.createMutationQuery( updateQuery );
	}

	@Override
	@SuppressWarnings("rawtypes")
	@Nonnull
	public MutationQueryImplementor createStatement(@Nonnull CriteriaStatement<?> criteriaStatement) {
		return createMutationQuery( criteriaStatement );
	}

	@Override
	@SuppressWarnings("rawtypes")
	@Nonnull
	public MutationQueryImplementor createMutationQuery(@Nonnull JpaCriteriaInsert insert) {
		return delegate.createMutationQuery( insert );
	}

	@Override
	@Nonnull
	public <T> SelectionQueryImplementor<T> createQuery(@Nonnull CriteriaSelect<T> criteriaQuery) {
		return delegate.createQuery( criteriaQuery );
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<?> createQuery(@Nonnull CriteriaStatement<?> criteriaStatement) {
		return createMutationQuery( criteriaStatement );
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery createQuery(@Nonnull String queryString) {
		//noinspection resource,SqlSourceToSinkFlow
		return queryDelegate().createQuery( queryString );
	}

	@Override
	@Nonnull
	public <R> SelectionQueryImplementor<R> createQuery(@Nonnull String hqlString, @Nonnull EntityGraph<R> resultGraph) {
		//noinspection resource
		return queryDelegate().createQuery( hqlString, resultGraph );
	}


	@Override
	@SuppressWarnings("rawtypes")
	@Nonnull
	public MutationQueryImplementor createStatement(@Nonnull String hql) {
		//noinspection resource
		return (MutationQueryImplementor) queryDelegate().createStatement( hql );
	}

	@Override
	@SuppressWarnings("rawtypes")
	@Nonnull
	public MutationQueryImplementor createNamedStatement(@Nonnull String name) {
		//noinspection resource
		return (MutationQueryImplementor) queryDelegate().createNamedStatement( name );
	}

	@Override
	@SuppressWarnings("rawtypes")
	@Nonnull
	public MutationQueryImplementor createStatement(@Nonnull StatementReference statementReference) {
		//noinspection resource
		return (MutationQueryImplementor) queryDelegate().createStatement( statementReference );
	}

	@Override
	@SuppressWarnings("rawtypes")
	@Nonnull
	public MutationQueryImplementor createNativeStatement(@Nonnull String sql) {
		//noinspection resource
		return (MutationQueryImplementor) queryDelegate().createNativeStatement( sql );
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> createSelectionQuery(@Nonnull String hqlString, @Nonnull Class<R> resultType) {
		//noinspection resource
		return queryDelegate().createSelectionQuery( hqlString, resultType );
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> createSelectionQuery(@Nonnull String hqlString, @Nonnull EntityGraph<R> resultGraph) {
		//noinspection resource
		return queryDelegate().createSelectionQuery( hqlString, resultGraph );
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> createSelectionQuery(@Nonnull CriteriaQuery<R> criteria) {
		//noinspection resource
		return queryDelegate().createSelectionQuery( criteria );
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> createSelectionQuery(@Nonnull CriteriaSelect<R> criteria) {
		//noinspection resource
		return queryDelegate().createSelectionQuery( criteria );
	}

	@Override
	@Nonnull
	public <T> SelectionQueryImplementor<T> createQuery(@Nonnull String queryString, @Nonnull Class<T> resultType) {
		//noinspection resource
		return queryDelegate().createQuery( queryString, resultType );
	}

	@Override
	@Nonnull
	public <R> SelectionQueryImplementor<R> createQuery(@Nonnull TypedQueryReference<R> typedQueryReference) {
		//noinspection resource
		return queryDelegate().createQuery( typedQueryReference );
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery createNamedQuery(@Nonnull String name) {
		//noinspection resource
		return queryDelegate().createNamedQuery( name );
	}

	@Override
	@Nonnull
	public <T> SelectionQueryImplementor<T> createNamedQuery(@Nonnull String name, @Nonnull Class<T> resultClass) {
		//noinspection resource
		return queryDelegate().createNamedQuery( name, resultClass );
	}

	@Override
	@Nonnull
	public <R> NativeQueryImplementor<R> createNamedQuery(@Nonnull String name, @Nonnull String resultSetMappingName) {
		//noinspection resource
		return queryDelegate().createNamedQuery( name, resultSetMappingName );
	}

	@Override
	@Nonnull
	public <R> NativeQueryImplementor<R> createNamedQuery(
			@Nonnull String name,
			@Nonnull String resultSetMappingName,
			@Nonnull Class<R> resultClass) {
		//noinspection resource
		return queryDelegate().createNamedQuery( name, resultSetMappingName, resultClass );
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> createNamedSelectionQuery(@Nonnull String name, @Nonnull Class<R> resultType) {
		//noinspection resource
		return delegate().createNamedSelectionQuery( name, resultType );
	}

	@Override
	@SuppressWarnings("rawtypes")
	@Nonnull
	public NativeQueryImplementor createNativeQuery(@Nonnull String sqlString) {
		//noinspection resource
		return queryDelegate().createNativeQuery( sqlString );
	}

	@Nonnull
	public <R> NativeQueryImplementor<R> createNativeQuery(@Nonnull String sqlString, @Nonnull Class<R> resultClass) {
		//noinspection resource
		return queryDelegate().createNativeQuery( sqlString, resultClass );
	}

	@Override
	@Nonnull
	public <T> NativeQueryImplementor<T> createNativeQuery(
			@Nonnull String sqlString,
			@Nonnull Class<T> resultClass,
			@Nonnull String tableAlias) {
		//noinspection resource
		return queryDelegate().createNativeQuery( sqlString, resultClass, tableAlias );
	}

	@Override
	@SuppressWarnings("rawtypes")
	@Nonnull
	public NativeQueryImplementor createNativeQuery(
			@Nonnull String sqlString,
			@Nonnull String resultSetMappingName) {
		//noinspection resource
		return queryDelegate().createNativeQuery( sqlString, resultSetMappingName );
	}

	@Override
	@Nonnull
	public <T> NativeQueryImplementor<T> createNativeQuery(
			@Nonnull String sql,
			@Nonnull ResultSetMapping<T> resultSetMapping) {
		//noinspection resource
		return queryDelegate().createNativeQuery( sql, resultSetMapping );
	}

	@Override
	@Nonnull
	public <T> NativeQueryImplementor<T> createNativeQuery(
			@Nonnull String sqlString,
			@Nonnull String resultSetMappingName,
			@Nonnull Class<T> resultClass) {
		//noinspection resource
		return queryDelegate().createNativeQuery( sqlString, resultSetMappingName, resultClass );
	}

	@Override
	@Nonnull
	public MutationQuery createMutationQuery(@Nonnull String statementString) {
		//noinspection resource
		return queryDelegate().createMutationQuery( statementString );
	}

	@Override
	@Nonnull
	public MutationQuery createNamedMutationQuery(@Nonnull String name) {
		//noinspection resource
		return queryDelegate().createNamedMutationQuery( name );
	}

	@Override
	@Nonnull
	public MutationQuery createNativeMutationQuery(@Nonnull String sqlString) {
		//noinspection resource
		return queryDelegate().createNativeMutationQuery( sqlString );
	}

	@Override
	@Nonnull
	public ProcedureCall createNamedStoredProcedureQuery(@Nonnull String name) {
		//noinspection resource
		return queryDelegate().createNamedStoredProcedureQuery( name );
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureQuery(@Nonnull String procedureName) {
		//noinspection resource
		return queryDelegate().createStoredProcedureQuery( procedureName );
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureQuery(@Nonnull String procedureName, @Nonnull Class<?>... resultClasses) {
		//noinspection resource
		return queryDelegate().createStoredProcedureQuery( procedureName, resultClasses );
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureQuery(@Nonnull String procedureName, @Nonnull String... resultSetMappings) {
		//noinspection resource
		return queryDelegate().createStoredProcedureQuery( procedureName, resultSetMappings );
	}

	@Override
	public void prepareForQueryExecution(boolean requiresTxn) {
		delegate.prepareForQueryExecution( requiresTxn );
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
	@Nonnull
	public <T> T unwrap(@Nonnull Class<T> cls) {
		return delegate.unwrap( cls );
	}

	@Override @Deprecated
	@SuppressWarnings({"rawtypes", "removal"})
	@Nonnull
	public NativeQuery getNamedNativeQuery(@Nonnull String name) {
		//noinspection resource
		return delegate().getNamedNativeQuery( name );
	}

	/**
	 * This is the implementation of {@link jakarta.persistence.EntityManager#getDelegate()}.
	 * It returns this object and <em>not</em> what we call the "delegate" session here.
	 * To get the delegate session, use {@link #delegate()} instead.
	 *
	 * @see SessionDelegatorBaseImpl#delegate()
	 */
	@Override
	@Nonnull
	public Object getDelegate() {
		return this;
	}

	@Override
	@Nonnull
	public ProcedureCall getNamedProcedureCall(@Nonnull String name) {
		return delegate.getNamedProcedureCall( name );
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureCall(@Nonnull String procedureName) {
		return delegate.createStoredProcedureCall( procedureName );
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureCall(@Nonnull String procedureName, @Nonnull Class<?>... resultClasses) {
		return delegate.createStoredProcedureCall( procedureName, resultClasses );
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureCall(@Nonnull String procedureName, @Nonnull String... resultSetMappings) {
		return delegate.createStoredProcedureCall( procedureName, resultSetMappings );
	}

	@Override
	@Nonnull
	public SharedSessionBuilder sessionWithOptions() {
		return delegate.sessionWithOptions();
	}

	@Override
	@Nonnull
	public SessionFactoryImplementor getSessionFactory() {
		return delegate.getSessionFactory();
	}

	@Override
	@Nonnull
	public TypeConfiguration getTypeConfiguration() {
		return delegate.getTypeConfiguration();
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
	@Nullable
	public Object getIdentifier(@Nonnull Object object) {
		return delegate.getIdentifier( object );
	}

	@Override
	@SuppressWarnings("removal")
	public boolean contains(@Nonnull String entityName, @Nonnull Object object) {
		return delegate.contains( entityName, object );
	}

	@Override
	public boolean contains(@Nonnull Object object) {
		return delegate.contains( object );
	}

	@Override
	public boolean isManaged(Object entity) {
		return delegate.isManaged( entity );
	}

	@Override
	@Nonnull
	public LockModeType getLockMode(@Nonnull Object entity) {
		return delegate.getLockMode( entity );
	}

	@Override
	public void setProperty(@Nonnull String propertyName, @Nullable Object value) {
		delegate.setProperty( propertyName, value );
	}

	@Override
	@Nonnull
	public Map<String, Object> getProperties() {
		return delegate.getProperties();
	}

	@Override
	public void evict(@Nonnull Object object) {
		delegate.evict( object );
	}

	@Override
	public void load(@Nonnull Object object, @Nonnull Object id) {
		delegate.load( object, id );
	}

	@Override
	@Nonnull
	public <T> T merge(@Nonnull T object) {
		return delegate.merge( object );
	}

	@Override
	@Nonnull
	public <T> T merge(@Nonnull String entityName, @Nonnull T object) {
		return delegate.merge( entityName, object );
	}

	@Override
	@Nonnull
	public <T> T merge(@Nonnull T object, @Nonnull EntityGraph<? super T> loadGraph) {
		return delegate.merge( object, loadGraph );
	}

	@Override
	public void persist(@Nonnull Object object) {
		delegate.persist( object );
	}

	@Override
	public void remove(@Nonnull Object entity) {
		delegate.remove( entity );
	}

	@Override
	@Nullable
	public <T> T find(@Nonnull Class<T> entityClass, @Nonnull Object primaryKey) {
		return delegate.find( entityClass, primaryKey );
	}

	@Override
	@Nullable
	public <T> T find(
			@Nonnull Class<T> entityClass,
			@Nonnull Object primaryKey,
			@Nullable Map<String, Object> properties) {
		return properties == null
				? delegate.find( entityClass, primaryKey )
				: delegate.find( entityClass, primaryKey, properties );
	}

	@Override
	@Nullable
	public <T> T find(
			@Nonnull Class<T> entityClass,
			@Nonnull Object primaryKey,
			@Nonnull LockModeType lockMode,
			@Nullable Map<String, Object> properties) {
		return properties == null
				? delegate.find( entityClass, primaryKey, lockMode )
				: delegate.find( entityClass, primaryKey, lockMode, properties );
	}

	@Override
	@Nullable
	public <T> T find(
			@Nonnull Class<T> entityClass,
			@Nonnull Object primaryKey,
			@Nullable FindOption... options) {
		return delegate.find( entityClass, primaryKey, options );
	}

	@Override
	@Nullable
	public <T> T find(
			@Nonnull EntityGraph<T> entityGraph,
			@Nonnull Object primaryKey,
			@Nullable FindOption... options) {
		return delegate.find( entityGraph, primaryKey, options );
	}

	@Override
	@Nullable
	public Object find(
			@Nonnull String entityName,
			@Nonnull Object primaryKey,
			@Nullable FindOption... options) {
		return delegate.find( entityName, primaryKey, options );
	}

	@Override
	@Nonnull
	public <T> T getReference(@Nonnull Class<T> entityClass, @Nonnull Object id) {
		return delegate.getReference( entityClass, id );
	}

	@Override
	@Nonnull
	public Object getReference(@Nonnull String entityName, @Nonnull Object id) {
		return delegate.getReference( entityName, id );
	}

	@Override
	public void persist(@Nonnull String entityName, @Nonnull Object object) {
		delegate.persist( entityName, object );
	}

	@Override
	public void lock(@Nonnull Object object, @Nonnull LockMode lockMode) {
		delegate.lock( object, lockMode );
	}

	@Override
	public void lock(@Nonnull Object object, @Nonnull LockMode lockMode, @Nullable LockOption... lockOptions) {
		delegate.lock( object, lockMode, lockOptions );
	}

	@Override
	public void lock(
			@Nonnull String entityName,
			@Nonnull Object object,
			@SuppressWarnings("removal")
			@Nonnull LockOptions lockOptions) {
		delegate.lock( entityName, object, lockOptions );
	}

	@Override
	@SuppressWarnings("removal")
	public void lock(@Nonnull Object object, @Nonnull LockOptions lockOptions) {
		delegate.lock( object, lockOptions );
	}

	@Override
	public void refresh(@Nonnull Object object) {
		delegate.refresh( object );
	}

	@Override
	public void refresh(@Nonnull Object entity, @Nullable Map<String, Object> properties) {
		delegate.refresh( entity, properties );
	}

	@Override
	public void refresh(@Nonnull Object entity, @Nonnull LockModeType lockMode, @Nullable Map<String, Object> properties) {
		delegate.refresh( entity, lockMode, properties );
	}

	@Override
	public void refresh(@Nonnull Object entity, @Nullable RefreshOption... options) {
		delegate.refresh( entity, options );
	}

	@Override
	@SuppressWarnings("removal")
	public void refresh(@Nonnull Object object, @Nonnull LockOptions lockOptions) {
		delegate.refresh( object, lockOptions );
	}

	@Override
	public LockMode getCurrentLockMode(Object object) {
		return delegate.getCurrentLockMode( object );
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public void detach(@Nonnull Object entity) {
		delegate.detach( entity );
	}

	@Override
	@Nonnull
	public <E> List<E> findMultiple(@Nonnull Class<E> entityType, @Nonnull List<?> ids, @Nullable FindOption... options) {
		return delegate.findMultiple( entityType, ids, options );
	}

	@Override
	@Nonnull
	public <E> List<E> findMultiple(@Nonnull EntityGraph<E> entityGraph, @Nonnull List<?> ids, @Nullable FindOption... options) {
		return delegate.findMultiple( entityGraph, ids, options );
	}

	@Override
	@Nonnull
	public <T> T get(@Nonnull Class<T> theClass, @Nonnull Object id) {
		return delegate.get( theClass, id );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public <T> T get(@Nonnull Class<T> theClass, @Nonnull Object id, @Nonnull LockMode lockMode) {
		return delegate.get( theClass, id, lockMode );
	}

	@Override
	@Nonnull
	public Object get(@Nonnull String entityName, @Nonnull Object key, @Nullable FindOption... findOptions) {
		return delegate.get( entityName, key, findOptions );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public Object get(@Nonnull String entityName, @Nonnull Object id, @Nonnull LockMode lockMode) {
		return delegate.get( entityName, id, lockMode );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public <T> T get(@Nonnull Class<T> entityType, @Nonnull Object id, @Nonnull LockOptions lockOptions) {
		return delegate.get( entityType, id, lockOptions );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public Object get(@Nonnull String entityName, @Nonnull Object id, @Nonnull LockOptions lockOptions) {
		return delegate.get( entityName, id, lockOptions );
	}

	@Override
	@Nonnull
	public String getEntityName(@Nonnull Object object) {
		return delegate.getEntityName( object );
	}

	@Override
	@Nonnull
	public <T> T getReference(@Nonnull T object) {
		return delegate.getReference( object );
	}

	@Override
	@Nonnull
	public <T> T getReference(@Nonnull Class<T> entityType, @Nonnull Object key, @Nonnull KeyType keyType) {
		return delegate.getReference( entityType, key, keyType );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public <T> IdentifierLoadAccess<T> byId(@Nonnull String entityName) {
		return delegate.byId( entityName );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public <T> MultiIdentifierLoadAccess<T> byMultipleIds(@Nonnull Class<T> entityClass) {
		return delegate.byMultipleIds( entityClass );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public <T> MultiIdentifierLoadAccess<T> byMultipleIds(@Nonnull String entityName) {
		return delegate.byMultipleIds( entityName );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public <T> IdentifierLoadAccess<T> byId(@Nonnull Class<T> entityClass) {
		return delegate.byId( entityClass );
	}

	@Override
	@Nonnull
	public <T> NaturalIdLoadAccess<T> byNaturalId(@Nonnull String entityName) {
		return delegate.byNaturalId( entityName );
	}

	@Override
	@Nonnull
	public <T> NaturalIdLoadAccess<T> byNaturalId(@Nonnull Class<T> entityClass) {
		return delegate.byNaturalId( entityClass );
	}

	@Override
	@Nonnull
	public <T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(@Nonnull String entityName) {
		return delegate.bySimpleNaturalId( entityName );
	}

	@Override
	@Nonnull
	public <T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(@Nonnull Class<T> entityClass) {
		return delegate.bySimpleNaturalId( entityClass );
	}

	@Override
	@Nonnull
	public <T> NaturalIdMultiLoadAccess<T> byMultipleNaturalId(@Nonnull Class<T> entityClass) {
		return delegate.byMultipleNaturalId( entityClass );
	}

	@Override
	@Nonnull
	public <T> NaturalIdMultiLoadAccess<T> byMultipleNaturalId(@Nonnull String entityName) {
		return delegate.byMultipleNaturalId( entityName );
	}

	@Override
	@Nonnull
	public Filter enableFilter(@Nonnull String filterName) {
		return delegate.enableFilter( filterName );
	}

	@Override
	@Nullable
	public Filter getEnabledFilter(@Nonnull String filterName) {
		return delegate.getEnabledFilter( filterName );
	}

	@Override
	public void disableFilter(@Nonnull String filterName) {
		delegate.disableFilter( filterName );
	}

	@Override
	@Nonnull
	public SessionStatistics getStatistics() {
		return delegate.getStatistics();
	}

	@Override
	public boolean isReadOnly(@Nonnull Object entityOrProxy) {
		return delegate.isReadOnly( entityOrProxy );
	}

	@Override
	public void setReadOnly(@Nonnull Object entityOrProxy,  boolean readOnly) {
		delegate.setReadOnly( entityOrProxy, readOnly );
	}

	@Override
	public void doWork(@Nonnull Work work) throws HibernateException {
		delegate.doWork( work );
	}

	@Override
	public <T> T doReturningWork(@Nonnull ReturningWork<T> work) throws HibernateException {
		return delegate.doReturningWork( work );
	}

	@Override
	public boolean isFetchProfileEnabled(@Nonnull String name) throws UnknownProfileException {
		return delegate.isFetchProfileEnabled( name );
	}

	@Override
	public void enableFetchProfile(@Nonnull String name) throws UnknownProfileException {
		delegate.enableFetchProfile( name );
	}

	@Override
	public void disableFetchProfile(@Nonnull String name) throws UnknownProfileException {
		delegate.disableFetchProfile( name );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public LobHelper getLobHelper() {
		return delegate.getLobHelper();
	}

	@Override
	@Nonnull
	public Collection<?> getManagedEntities() {
		return delegate.getManagedEntities();
	}

	@Override
	@Nonnull
	public Collection<?> getManagedEntities(@Nonnull String entityName) {
		return delegate.getManagedEntities( entityName );
	}

	@Override
	@Nonnull
	public <E> Collection<E> getManagedEntities(@Nonnull Class<E> entityType) {
		return delegate.getManagedEntities( entityType );
	}

	@Override
	@Nonnull
	public <E> Collection<E> getManagedEntities(@Nonnull EntityType<E> entityType) {
		return delegate.getManagedEntities( entityType );
	}

	@Override
	public void addEventListeners(@Nonnull SessionEventListener... listeners) {
		delegate.addEventListeners( listeners );
	}

	@Override @SuppressWarnings("removal")
	public org.hibernate.action.queue.spi.ActionQueue getActionQueue() {
		return delegate.getActionQueue();
	}

	@Override
	public TransactionCompletionCallbacks getTransactionCompletionCallbacks() {
		return delegate.getTransactionCompletionCallbacks();
	}

	@Override
	@Nonnull
	public TransactionCompletionCallbacksImplementor getTransactionCompletionCallbacksImplementor() {
		return delegate.getTransactionCompletionCallbacksImplementor();
	}

	@Override
	@Nonnull
	public Object instantiate(@Nonnull EntityPersister persister, @Nonnull Object id) throws HibernateException {
		return delegate.instantiate( persister, id );
	}

	@Override
	public void forceFlush(@Nonnull EntityEntry e) throws HibernateException {
		delegate.forceFlush( e );
	}

	@Override
	public void forceFlush(@Nonnull EntityKey e) throws HibernateException {
		delegate.forceFlush( e );
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
	public boolean useLanguageTagForLocale() {
		return delegate.useLanguageTagForLocale();
	}

	@Override
	@Nonnull
	public LobCreator getLobCreator() {
		return delegate.getLobCreator();
	}

	@Override
	@Nullable
	public Integer getJdbcBatchSize() {
		return delegate.getJdbcBatchSize();
	}

	@Override
	@Nonnull
	public EventMonitor getEventMonitor() {
		return delegate.getEventMonitor();
	}

	@Override
	public void setJdbcBatchSize(@Nullable Integer jdbcBatchSize) {
		delegate.setJdbcBatchSize( jdbcBatchSize );
	}

	@Override
	public boolean isSubselectFetchingEnabled() {
		return delegate.isSubselectFetchingEnabled();
	}

	@Override
	public void setSubselectFetchingEnabled(boolean enabled) {
		delegate.setSubselectFetchingEnabled( enabled );
	}

	@Override
	public int getFetchBatchSize() {
		return delegate.getFetchBatchSize();
	}

	@Override
	public void setFetchBatchSize(int batchSize) {
		delegate.setFetchBatchSize( batchSize );
	}

	@Override
	public TimeZone getJdbcTimeZone() {
		return delegate.getJdbcTimeZone();
	}

	@Override
	@Nonnull
	public FormatMapper getJsonFormatMapper() {
		return delegate.getJsonFormatMapper();
	}

	@Override
	@Nonnull
	public FormatMapper getXmlFormatMapper() {
		return delegate.getXmlFormatMapper();
	}

	@Override
	public Object loadFromSecondLevelCache(
			@Nonnull EntityPersister persister,
			@Nonnull EntityKey entityKey,
			@Nullable Object instanceToLoad,
			@Nonnull LockMode lockMode) {
		return delegate.loadFromSecondLevelCache( persister, entityKey, instanceToLoad, lockMode );
	}

	@Override
	@Nonnull
	public SessionAssociationMarkers getSessionAssociationMarkers() {
		return delegate.getSessionAssociationMarkers();
	}

	@Override
	public boolean isIdentifierRollbackEnabled() {
		return delegate.isIdentifierRollbackEnabled();
	}

	@Override
	public void afterObtainConnection(@Nonnull Connection connection) throws SQLException {
		delegate.afterObtainConnection( connection );
	}

	@Override
	public void beforeReleaseConnection(Connection connection) throws SQLException {
		delegate.beforeReleaseConnection( connection );
	}

	public void runEntityLifecycleCallback(@Nonnull Runnable callback) {
		delegate.runEntityLifecycleCallback( callback );
	}

	public <T> T callEntityLifecycleCallback(@Nonnull Supplier<T> callback) {
		return delegate.callEntityLifecycleCallback( callback );
	}

	public void runInterceptorCallback(@Nonnull Runnable callback) {
		delegate.runInterceptorCallback( callback );
	}

	public <T> T callInterceptorCallback(@Nonnull Supplier<T> callback) {
		return delegate.callInterceptorCallback( callback );
	}
}
