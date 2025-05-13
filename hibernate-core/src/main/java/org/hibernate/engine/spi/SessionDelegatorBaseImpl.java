/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FindOption;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockOption;
import jakarta.persistence.RefreshOption;
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
import org.hibernate.Interceptor;
import org.hibernate.LobHelper;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.NaturalIdMultiLoadAccess;
import org.hibernate.ReplicationMode;
import org.hibernate.SessionEventListener;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.Transaction;
import org.hibernate.UnknownProfileException;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.cache.spi.CacheTransactionSynchronization;
import org.hibernate.collection.spi.PersistentCollection;
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
import org.hibernate.stat.SessionStatistics;
import org.hibernate.type.format.FormatMapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

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
	 * <p>
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
	public String getTenantIdentifier() {
		return delegate.getTenantIdentifier();
	}

	@Override
	public Object getTenantIdentifierValue() {
		return delegate.getTenantIdentifierValue();
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
	public EntityKey generateEntityKey(Object id, EntityPersister persister) {
		return delegate.generateEntityKey( id, persister );
	}

	@Override
	public Interceptor getInterceptor() {
		return delegate.getInterceptor();
	}

	@Override
	public boolean isTransactionInProgress() {
		return delegate.isTransactionInProgress();
	}

	@Override
	public void checkTransactionNeededForUpdateOperation(String exceptionMessage) {
		delegate.checkTransactionNeededForUpdateOperation( exceptionMessage );
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
	public SessionFactoryImplementor getFactory() {
		return delegate.getFactory();
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
	public PersistenceContext getPersistenceContext() {
		return delegate.getPersistenceContext();
	}

	@Override
	public CacheMode getCacheMode() {
		return delegate.getCacheMode();
	}

	@Override
	public CacheRetrieveMode getCacheRetrieveMode() {
		return delegate.getCacheRetrieveMode();
	}

	@Override
	public CacheStoreMode getCacheStoreMode() {
		return delegate.getCacheStoreMode();
	}

	@Override
	public void setCacheMode(CacheMode cacheMode) {
		delegate.setCacheMode( cacheMode );
	}

	@Override
	public void setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		delegate.setCacheStoreMode( cacheStoreMode );
	}

	@Override
	public void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		delegate.setCacheRetrieveMode( cacheRetrieveMode );
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
	public void lock(Object entity, LockModeType lockMode) {
		delegate.lock( entity, lockMode );
	}

	@Override
	public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
		delegate.lock( entity, lockMode, properties );
	}

	@Override
	public void lock(Object entity, LockModeType lockMode, LockOption... options) {
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
	public EventSource asEventSource() {
		return delegate.asEventSource();
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
	public void startTransactionBoundary() {
		delegate.startTransactionBoundary();
	}

	@Override
	public CacheTransactionSynchronization getCacheTransactionSynchronization() {
		return delegate.getCacheTransactionSynchronization();
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
	public HibernateCriteriaBuilder getCriteriaBuilder() {
		return delegate.getCriteriaBuilder();
	}

	@Override
	public Metamodel getMetamodel() {
		return delegate.getMetamodel();
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
	public <T> QueryImplementor<T> createQuery(CriteriaSelect<T> selectQuery) {
		return delegate.createQuery( selectQuery );
	}

	@Override
	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
		return delegate.getEntityGraphs( entityClass );
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
	public <T> QueryImplementor<T> createQuery(TypedQueryReference<T> typedQueryReference) {
		return queryDelegate().createQuery( typedQueryReference );
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

	@Override @SuppressWarnings("rawtypes")
	public QueryImplementor createQuery(String queryString) {
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
	public ProcedureCall createStoredProcedureQuery(String procedureName, Class... resultClasses) {
		return delegate.createStoredProcedureQuery( procedureName, resultClasses );
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
		return delegate.createStoredProcedureQuery( procedureName, resultSetMappings );
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
	public <T> T unwrap(Class<T> cls) {
		return delegate.unwrap( cls );
	}

	/**
	 * This is the implementation of {@link jakarta.persistence.EntityManager#getDelegate()}.
	 * It returns this object and <em>not</em> what we call the "delegate" session here.
	 * To get the delegate session, use {@link #delegate()} instead.
	 *
	 * @see SessionDelegatorBaseImpl#delegate()
	 */
	@Override
	public Object getDelegate() {
		return this;
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
	public Object getIdentifier(Object object) {
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
	public void load(Object object, Object id) {
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
	public <T> T merge(T object) {
		return delegate.merge( object );
	}

	@Override
	public <T> T merge(String entityName, T object) {
		return delegate.merge( entityName, object );
	}

	@Override
	public <T> T merge(T object, EntityGraph<?> loadGraph) {
		return delegate.merge( object, loadGraph );
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
	public <T> @Nullable T find(Class<T> entityClass, Object primaryKey) {
		return delegate.find( entityClass, primaryKey );
	}

	@Override
	public <T> @Nullable T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
		return delegate.find( entityClass, primaryKey, properties );
	}

	@Override
	public <T> @Nullable T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
		return delegate.find( entityClass, primaryKey, lockMode );
	}

	@Override
	public <T> @Nullable T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
		return delegate.find( entityClass, primaryKey, lockMode, properties );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, FindOption... options) {
		return delegate.find( entityClass, primaryKey, options );
	}

	@Override
	public <T> T find(EntityGraph<T> entityGraph, Object primaryKey, FindOption... options) {
		return delegate.find( entityGraph, primaryKey, options );
	}

	@Override
	public <T> T getReference(Class<T> entityClass, Object id) {
		return delegate.getReference( entityClass, id );
	}

	@Override
	public Object getReference(String entityName, Object id) {
		return delegate.getReference( entityName, id );
	}

	@Override
	public void persist(String entityName, Object object) {
		delegate.persist( entityName, object );
	}

	@Override
	public void lock(Object object, LockMode lockMode) {
		delegate.lock( object, lockMode );
	}

	@Override
	public void lock(Object object, LockMode lockMode, LockOption... lockOptions) {
		delegate.lock( object, lockMode, lockOptions );
	}

	@Override
	public void lock(String entityName, Object object, LockOptions lockOptions) {
		delegate.lock( entityName, object, lockOptions );
	}

	@Override
	public void lock(Object object, LockOptions lockOptions) {
		delegate.lock( object, lockOptions );
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
	public void refresh(Object entity, RefreshOption... options) {
		delegate.refresh( entity, options );
	}

	@Override
	public void refresh(Object object, LockOptions lockOptions) {
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
	public void detach(Object entity) {
		delegate.detach( entity );
	}

	@Override
	public <E> List<E> findMultiple(Class<E> entityType, List<?> ids, FindOption... options) {
		return delegate.findMultiple( entityType, ids, options );
	}

	@Override
	public <E> List<E> findMultiple(EntityGraph<E> entityGraph, List<?> ids, FindOption... options) {
		return  delegate.findMultiple( entityGraph, ids, options );
	}

	@Override
	public <T> T get(Class<T> theClass, Object id) {
		return delegate.get( theClass, id );
	}

	@Override
	public <T> T get(Class<T> theClass, Object id, LockMode lockMode) {
		return delegate.get( theClass, id, lockMode );
	}

	@Override
	public Object get(String entityName, Object id) {
		return delegate.get( entityName, id );
	}

	@Override
	public Object get(String entityName, Object id, LockMode lockMode) {
		return delegate.get( entityName, id, lockMode );
	}

	@Override
	public <T> T get(Class<T> entityType, Object id, LockOptions lockOptions) {
		return delegate.get( entityType, id, lockOptions );
	}

	@Override
	public Object get(String entityName, Object id, LockOptions lockOptions) {
		return delegate.get( entityName, id, lockOptions );
	}

	@Override
	public String getEntityName(Object object) {
		return delegate.getEntityName( object );
	}

	@Override
	public <T> T getReference(T object) {
		return delegate.getReference( object );
	}

	@Override
	public <T> IdentifierLoadAccess<T> byId(String entityName) {
		return delegate.byId( entityName );
	}

	@Override
	public <T> MultiIdentifierLoadAccess<T> byMultipleIds(Class<T> entityClass) {
		return delegate.byMultipleIds( entityClass );
	}

	@Override
	public <T> MultiIdentifierLoadAccess<T> byMultipleIds(String entityName) {
		return delegate.byMultipleIds( entityName );
	}

	@Override
	public <T> IdentifierLoadAccess<T> byId(Class<T> entityClass) {
		return delegate.byId( entityClass );
	}

	@Override
	public <T> NaturalIdLoadAccess<T> byNaturalId(String entityName) {
		return delegate.byNaturalId( entityName );
	}

	@Override
	public <T> NaturalIdLoadAccess<T> byNaturalId(Class<T> entityClass) {
		return delegate.byNaturalId( entityClass );
	}

	@Override
	public <T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(String entityName) {
		return delegate.bySimpleNaturalId( entityName );
	}

	@Override
	public <T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(Class<T> entityClass) {
		return delegate.bySimpleNaturalId( entityClass );
	}

	@Override
	public <T> NaturalIdMultiLoadAccess<T> byMultipleNaturalId(Class<T> entityClass) {
		return delegate.byMultipleNaturalId( entityClass );
	}

	@Override
	public <T> NaturalIdMultiLoadAccess<T> byMultipleNaturalId(String entityName) {
		return delegate.byMultipleNaturalId( entityName );
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
	public LobHelper getLobHelper() {
		return delegate.getLobHelper();
	}

	@Override
	public Collection<?> getManagedEntities() {
		return delegate.getManagedEntities();
	}

	@Override
	public Collection<?> getManagedEntities(String entityName) {
		return delegate.getManagedEntities( entityName );
	}

	@Override
	public <E> Collection<E> getManagedEntities(Class<E> entityType) {
		return delegate.getManagedEntities( entityType );
	}

	@Override
	public <E> Collection<E> getManagedEntities(EntityType<E> entityType) {
		return delegate.getManagedEntities( entityType );
	}

	@Override
	public void addEventListeners(SessionEventListener... listeners) {
		delegate.addEventListeners( listeners );
	}

	@Override
	public ActionQueue getActionQueue() {
		return delegate.getActionQueue();
	}

	@Override
	public void registerProcess(AfterTransactionCompletionProcess process) {
		delegate.registerProcess( process );
	}

	@Override
	public Object instantiate(EntityPersister persister, Object id) throws HibernateException {
		return delegate.instantiate( persister, id );
	}

	@Override
	public void forceFlush(EntityEntry e) throws HibernateException {
		delegate.forceFlush( e );
	}

	@Override
	public void forceFlush(EntityKey e) throws HibernateException {
		delegate.forceFlush( e );
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
	public int getPreferredSqlTypeCodeForBoolean() {
		return delegate.getPreferredSqlTypeCodeForBoolean();
	}

	@Override
	public LobCreator getLobCreator() {
		return delegate.getLobCreator();
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
	public FormatMapper getJsonFormatMapper() {
		return delegate.getJsonFormatMapper();
	}

	@Override
	public FormatMapper getXmlFormatMapper() {
		return delegate.getXmlFormatMapper();
	}

	@Override
	public Object loadFromSecondLevelCache(EntityPersister persister, EntityKey entityKey, Object instanceToLoad, LockMode lockMode) {
		return delegate.loadFromSecondLevelCache( persister, entityKey, instanceToLoad, lockMode );
	}

	@Override
	public boolean isIdentifierRollbackEnabled() {
		return delegate.isIdentifierRollbackEnabled();
	}
}
