/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.Timeout;
import jakarta.persistence.TransactionRequiredException;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.CriteriaStatement;
import jakarta.persistence.sql.ResultSetMapping;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.Interceptor;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.SharedSessionContract;
import org.hibernate.Transaction;
import org.hibernate.audit.spi.AuditWorkQueue;
import org.hibernate.bytecode.enhance.spi.interceptor.SessionAssociationMarkers;
import org.hibernate.cache.spi.CacheTransactionSynchronization;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.extension.spi.Extension;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.event.spi.EventSource;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.JpaCriteriaInsert;
import org.hibernate.query.spi.MutationQueryImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.SelectionQueryImplementor;
import org.hibernate.query.sql.spi.NativeQueryImplementor;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder.Options;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Defines the internal contract shared between {@link org.hibernate.Session} and
 * {@link org.hibernate.StatelessSession} as used by other parts of Hibernate,
 * including implementors of {@link org.hibernate.type.Type}, {@link EntityPersister},
 * and {@link org.hibernate.persister.collection.CollectionPersister}.
 * <p>
 * The {@code Session}, via this interface, implements:
 * <ul>
 *     <li>
 *         {@link JdbcSessionOwner}, and so the session also acts as the orchestrator
 *         of a "JDBC session", and may be used to construct a {@link JdbcCoordinator}.
 *     </li>
 *     <li>
 *         {@link Options}, allowing the session to control the creation of the
 *         {@link TransactionCoordinator} delegate when it is passed as an argument to
 *         {@link org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder#buildTransactionCoordinator}
 *     </li>
 *     <li>
 *         {@link LobCreationContext}, and so the session may act as the context for
 *         JDBC LOB instance creation.
 *     </li>
 *     <li>
 *         {@link WrapperOptions}, and so the session may influence the process of binding
 *         and extracting values to and from JDBC, which is performed by implementors of
 *         {@link org.hibernate.type.descriptor.jdbc.JdbcType}.
 *     </li>
 * </ul>
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface SharedSessionContractImplementor
		extends SharedSessionContract, JdbcSessionOwner, Options, LobCreationContext, WrapperOptions {

	/**
	 * Obtain the {@linkplain SessionFactoryImplementor factory} which created this session.
	 */
	@Override
	@Nonnull
	SessionFactoryImplementor getFactory();

	/**
	 * Obtain the {@linkplain SessionFactoryImplementor factory} which created this session.
	 */
	@Override
	@Nonnull
	default SessionFactoryImplementor getSessionFactory() {
		return getFactory();
	}

	/**
	 * Obtain the {@link TypeConfiguration} for the factory which created this session.
	 */
	@Override
	@Nonnull
	default TypeConfiguration getTypeConfiguration() {
		return getFactory().getTypeConfiguration();
	}

	/**
	 * Obtain the {@link Dialect}.
	 */
	@Override
	@Nonnull
	default Dialect getDialect() {
		return getSessionFactory().getJdbcServices().getDialect();
	}

	/**
	 * Get the {@link SessionEventListenerManager} associated with this session.
	 */
	@Nonnull
	SessionEventListenerManager getEventListenerManager();

	/**
	 * Get the persistence context for this session.
	 * <p>
	 * See {@link #getPersistenceContextInternal()} for
	 * a faster alternative.
	 *
	 * @implNote This method is not extremely fast: if you need to
	 *            call the {@link PersistenceContext} multiple times,
	 *            prefer keeping a reference to it instead of invoking
	 *            this method multiple times.
	 */
	@Nonnull
	PersistenceContext getPersistenceContext();

	/**
	 * Obtain the {@link JdbcCoordinator} for this session.
	 */
	@Nonnull
	JdbcCoordinator getJdbcCoordinator();

	/**
	 * Obtain the {@link JdbcServices} for the factory which created this session.
	 */
	@Nonnull
	JdbcServices getJdbcServices();

	/**
	 * Obtain a {@link UUID} which uniquely identifies this session.
	 * <p>
	 * The UUID is useful mainly for logging.
	 */
	@Nonnull
	UUID getSessionIdentifier();

	/**
	 * Returns this object, fulfilling the contract of {@link WrapperOptions}.
	 */
	@Override
	@Nonnull
	default SharedSessionContractImplementor getSession() {
		return this;
	}

	/**
	 * Obtain a "token" which uniquely identifies this session.
	 */
	default Object getSessionToken() {
		return this;
	}

	/**
	 * Determines whether the session is closed.
	 *
	 * @apiNote Provided separately from {@link #isOpen()} as this method
	 *          does not attempt any JTA synchronization registration,
	 *          whereas {@link #isOpen()} does. This one is better for most
	 *          internal purposes.
	 *
	 * @return {@code true} if the session is closed; {@code false} otherwise.
	 */
	boolean isClosed();

	/**
	 * Determines whether the session is open or is waiting for auto-close.
	 *
	 * @return {@code true} if the session is closed, or if it's waiting
	 *         for auto-close; {@code false} otherwise.
	 */
	default boolean isOpenOrWaitingForAutoClose() {
		return !isClosed();
	}

	/**
	 * Check whether the session is open, and if not:
	 * <ul>
	 *     <li>mark the current transaction, if any, for rollback only,
	 *         and</li>
	 *     <li>throw an {@code IllegalStateException}.
	 *         (JPA specifies this exception type.)</li>
	 * </ul>
	 */
	default void checkOpen() {
		checkOpen( true );
	}

	/**
	 * Check whether the session is open, and if not:
	 * <ul>
	 *     <li>if {@code markForRollbackIfClosed = true}, mark the
	 *         current transaction, if any, for rollback only, and
	 *     <li>throw an {@code IllegalStateException}.
	 *         (JPA specifies this exception type.)
	 * </ul>
	 */
	void checkOpen(boolean markForRollbackIfClosed);

	/**
	 * Run a Jakarta Persistence entity lifecycle callback.
	 */
	void runEntityLifecycleCallback(Runnable callback);

	/**
	 * Call a Jakarta Persistence entity lifecycle callback.
	 */
	<T> T callEntityLifecycleCallback(@Nonnull Supplier<T> callback);

	/**
	 * Run a Hibernate {@link Interceptor} callback.
	 */
	void runInterceptorCallback(@Nonnull Runnable callback);

	/**
	 * Call a Hibernate {@link Interceptor} callback.
	 */
	<T> T callInterceptorCallback(@Nonnull Supplier<T> callback);

	/**
	 * Prepare for the execution of a {@link Query} or
	 * {@link org.hibernate.procedure.ProcedureCall}
	 */
	void prepareForQueryExecution(boolean requiresTxn);

	/**
	 * Marks current transaction, if any, for rollback only.
	 */
	void markForRollbackOnly();

	/**
	 * The current {@link CacheTransactionSynchronization} associated
	 * with this session. This may be {@code null} if the session is not
	 * currently associated with an active transaction.
	 */
	CacheTransactionSynchronization getCacheTransactionSynchronization();

	/**
	 * The changeset id associated with the current unit of work,
	 * for use with {@linkplain org.hibernate.annotations.Temporal temporal}
	 * effectivity columns and with
	 * {@linkplain org.hibernate.annotations.Audited.Table#changesetIdColumn
	 * audit log changeset id columns}.
	 */
	Object getCurrentChangesetIdentifier();

	/**
	 * Obtain the full changeset context associated with
	 * {@link #getCurrentChangesetIdentifier()}, if the configured changeset
	 * supplier exposes one.
	 * <p>
	 * This is used by graph-queue audit execution to capture changelog state
	 * before materializing audit bind plans. Suppliers which only produce a
	 * scalar changeset identifier return {@code null}.
	 */
	@Nullable Object getCurrentChangesetContext();

	/**
	 * Does this session have an active Hibernate transaction, or is it
	 * associated with a JTA transaction currently in progress?
	 */
	boolean isTransactionInProgress();

	/**
	 * Check if an active {@link Transaction} is available before performing
	 * an update operation against the database.
	 * <p>
	 * If an active transaction is necessary, but no transaction is active,
	 * a {@link TransactionRequiredException} is raised.
	 *
	 * @param exceptionMessage the message to use for the {@link TransactionRequiredException}
	 */
	default void checkTransactionNeededForUpdateOperation(@Nonnull String exceptionMessage) {
		if ( !isTransactionInProgress() ) {
			throw new TransactionRequiredException( exceptionMessage );
		}
	}

	/**
	 * Retrieves the current {@link Transaction}, or creates a new transaction
	 * if there is no transaction active.
	 * <p>
	 * This method is primarily for internal or integrator use.
	 *
	 * @return the {@link Transaction}
	 */
	@Nonnull
	Transaction accessTransaction();

	/**
	 * The current {@link Transaction} object associated with this session,
	 * if it has already been created, or {@code null} otherwise.
	 *
	 * @since 7.2
	 */
	@Incubating
	@Nullable
	Transaction getCurrentTransaction();

	/**
	 * Access to register callbacks for transaction completion processing.
	 *
	 * @since 7.2
	 */
	@Incubating
	TransactionCompletionCallbacks getTransactionCompletionCallbacks();

	/**
	 * Access to registered callbacks for transaction completion processing.
	 *
	 * @since 7.2
	 */
	@Incubating
	@Nonnull
	TransactionCompletionCallbacksImplementor getTransactionCompletionCallbacksImplementor();

	/**
	 * Access the transaction-scoped audit work queue for deferred
	 * audit row writes. Lazily initialized on first access.
	 *
	 * @since 7.4
	 */
	@Incubating
	AuditWorkQueue getAuditWorkQueue();

	/**
	 * Instantiate an {@link EntityKey} with the given id and for the
	 * entity represented by the given {@link EntityPersister}.
	 * <p>
	 * When operating in a temporal context, this will automatically
	 * create a {@link TemporalEntityKey} that includes the transaction
	 * identifier.
	 *
	 * @param id The entity id
	 * @param persister The entity persister
	 *
	 * @return The entity key
	 */
	@Nonnull
	EntityKey generateEntityKey(@Nonnull Object id, @Nonnull EntityPersister persister);

	/**
	 * Instantiate a {@link CollectionKey} with the given key and for the
	 * collection represented by the given {@link CollectionPersister}.
	 * <p>
	 * When operating in a temporal context, this will automatically
	 * create a {@link TemporalCollectionKey} that includes the transaction
	 * identifier.
	 *
	 * @param persister The collection persister
	 * @param key The collection key (owner FK)
	 *
	 * @return The collection key
	 */
	@Nonnull
	CollectionKey generateCollectionKey(@Nonnull CollectionPersister persister, @Nonnull Object key);

	/**
	 * Retrieves the {@link Interceptor} associated with this session.
	 */
	@Nonnull
	Interceptor getInterceptor();

	/**
	 * Initialize the given collection (if not already initialized).
	 */
	void initializeCollection(@Nonnull PersistentCollection<?> collection, boolean writing)
			throws HibernateException;

	/**
	 * Obtain an entity instance with the given id, without checking if it was
	 * deleted or scheduled for deletion.
	 * <ul>
	 * <li>When {@code nullable = false}, this method may create a new proxy or
	 *     return an existing proxy; if it does not exist, an exception is thrown.
	 * <li>When {@code nullable = true}, the method does not create new proxies,
	 *     though it might return an existing proxy; if it does not exist, a
	 *     {@code null} value is returned.
	 * </ul>
	 * <p>
	 * When {@code eager = true}, the object is eagerly fetched from the database.
	 */
	@Nullable
	Object internalLoad(@Nonnull String entityName, @Nonnull Object id, boolean eager, boolean nullable)
			throws HibernateException;

	/**
	 * Load an instance immediately.
	 * This method is only called when lazily initializing a proxy.
	 * Do not return the proxy.
	 */
	Object immediateLoad(@Nonnull String entityName, @Nonnull Object id);


	/**
	 * Get the {@link EntityPersister} for the given entity instance.
	 *
	 * @param entityName optional entity name
	 * @param object the entity instance
	 */
	@Nonnull
	EntityPersister getEntityPersister(@Nullable String entityName, @Nonnull Object object);

	/**
	 * Get the entity instance associated with the given {@link EntityKey},
	 * calling the {@link Interceptor} if necessary.
	 */
	Object getEntityUsingInterceptor(@Nonnull EntityKey key);

	/**
	 * Return the identifier of the persistent object, or null if it is
	 * not associated with this session.
	 */
	Object getContextEntityIdentifier(@Nonnull Object object);

	/**
	 * Obtain the best estimate of the entity name of the given entity
	 * instance, which is not involved in an association, by also
	 * considering information held in the proxy, and whether the object
	 * is already associated with this session.
	 */
	String bestGuessEntityName(@Nonnull Object object);

	/**
	 * Obtain an estimate of the entity name of the given entity instance,
	 * which is not involved in an association, using only the
	 * {@link org.hibernate.EntityNameResolver}.
	 */
	String guessEntityName(@Nonnull Object entity);

	/**
	 * Instantiate the entity class of the given {@link EntityPersister},
	 * initializing the new instance with the given identifier.
	 */
	@Nonnull
	Object instantiate(@Nonnull EntityPersister persister, @Nonnull Object id);

	/**
	 * Are entities and proxies loaded by this session read-only by default?
	 */
	boolean isDefaultReadOnly();

	boolean isIdentifierRollbackEnabled();

	void setCriteriaCopyTreeEnabled(boolean jpaCriteriaCopyComplianceEnabled);

	boolean isCriteriaCopyTreeEnabled();

	void setCriteriaPlanCacheEnabled(boolean jpaCriteriaCacheEnabled);

	boolean isCriteriaPlanCacheEnabled();

	boolean getNativeJdbcParametersIgnored();

	void setNativeJdbcParametersIgnored(boolean nativeJdbcParametersIgnored);

	/**
	 * Get the current {@link FlushMode} for this session.
	 *
	 * @return The flush mode
	 */
	@Nonnull
	FlushMode getHibernateFlushMode();

	/**
	 * Flush this session.
	 */
	void flush();

	/**
	 * Determines if this session implements {@link EventSource}.
	 * <p>
	 * Only stateful session are sources of events. If this object is
	 * a stateless session, this method return {@code false}.
	 */
	default boolean isEventSource() {
		return false;
	}

	/**
	 * Cast this session to {@link EventSource} if possible.
	 * <p>
	 * Only stateful session are sources of events. If this object is
	 * a stateless session, this method throws.
	 *
	 * @throws ClassCastException if the cast is not possible
	 */
	@Nonnull
	default EventSource asEventSource() {
		throw new ClassCastException( "session is not an EventSource" );
	}

	/**
	 * Whether the session {@linkplain StatelessSessionImplementor stateless}, as opposed tp
	 * {@linkplain SessionImplementor stateful}.
	 *
	 * @apiNote Essentially, whether casting this session to {@linkplain StatelessSessionImplementor} will succeed.
	 */
	default boolean isStateless() {
		return false;
	}

	/**
	 * Called after each operation on a {@link org.hibernate.ScrollableResults},
	 * providing an opportunity for a stateless session to clear its
	 * temporary persistence context. For a stateful session, this method
	 * does nothing.
	 */
	void afterScrollOperation();

	/**
	 * Get the {@link LoadQueryInfluencers} associated with this session.
	 *
	 * @return the {@link LoadQueryInfluencers} associated with this session;
	 *         should never be null.
	 */
	@Nonnull
	LoadQueryInfluencers getLoadQueryInfluencers();

	/**
	 * The default lock options associated with this session.
	 */
	@SuppressWarnings("removal")
	LockOptions getDefaultLockOptions();

	/**
	 * Lock timeout specified on the SessionFactory via hint.
	 */
	@Nullable
	Timeout getDefaultLockTimeout();

	/**
	 * Query timeout specified on the SessionFactory via hint.
	 */
	@Nullable
	Timeout getDefaultTimeout();

	/**
	 * Obtain an {@link ExceptionConverter} for reporting an error.
	 * <p>
	 * The converter associated to a session might be lazily initialized,
	 * so only invoke this getter when there's an actual need to use it.
	 *
	 * @return the ExceptionConverter for this Session.
	 */
	@Nonnull
	ExceptionConverter getExceptionConverter();

	/**
	 * Get the currently configured JDBC batch size, which might have
	 * been specified at either the session or factory level.
	 *
	 * @return the session-level JDBC batch size is set, or the
	 *         factory-level setting otherwise
	 *
	 * @since 5.2
	 *
	 * @see org.hibernate.boot.spi.SessionFactoryOptions#getJdbcBatchSize
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyJdbcBatchSize
	 */
	@Nullable
	default Integer getConfiguredJdbcBatchSize() {
		final Integer sessionJdbcBatchSize = getJdbcBatchSize();
		return sessionJdbcBatchSize == null
				? getFactory().getSessionFactoryOptions().getJdbcBatchSize()
				: sessionJdbcBatchSize;
	}

	/**
	 * Similar to {@link #getPersistenceContext()}, with two differences:
	 * <ol>
	 * <li>this version performs better as it allows for inlining
	 *     and probably better prediction, and
	 * <li>it skips some checks of the current state of the session.
	 * </ol>
	 * Choose wisely: performance is important, but correctness comes first.
	 *
	 * @return the {@link PersistenceContext} associated to this session.
	 */
	@Nonnull
	PersistenceContext getPersistenceContextInternal();

	/**
	 * Is the given entity managed by this session?
	 *
	 * @return true if this is a stateful session and
	 *         the entity belongs to its persistence
	 *         context and was not removed
	 */
	boolean isManaged(Object entity);

	/**
	 * detect in-memory changes, determine if the changes are to tables
	 * named in the query and, if so, complete execution the flush
	 *
	 * @param querySpaces the tables named in the query.
	 *
	 * @return true if flush is required, false otherwise.
	 */
	default boolean autoFlushIfRequired(Set<String> querySpaces) {
		return autoFlushIfRequired( querySpaces, false );
	}

	/**
	 * detect in-memory changes, determine if the changes are to tables
	 * named in the query and, if so, complete execution the flush
	 *
	 * @param querySpaces the tables named in the query.
	 * @param skipPreFlush see {@link org.hibernate.event.spi.AutoFlushEvent#isSkipPreFlush}
	 *
	 * @return true if flush is required, false otherwise.
	 */
	boolean autoFlushIfRequired(Set<String> querySpaces, boolean skipPreFlush);

	boolean autoPreFlushIfRequired(QueryParameterBindings parameterBindings);

	/**
	 * Check if there is a Hibernate or JTA transaction in progress and,
	 * if there is not, flush if necessary, making sure that the connection
	 * has been committed (if it is not in autocommit mode), and finally
	 * run the after completion processing.
	 *
	 * @param success {@code true} if the operation a success
	 */
	void afterOperation(boolean success);

	/**
	 * Cascade the lock operation to the given child entity.
	 */
	void lock(
			@Nonnull String entityName,
			@Nonnull Object child,
			@SuppressWarnings("removal")
			@Nonnull LockOptions lockOptions);

	/**
	 * Attempts to load the entity from the second-level cache.
	 *
	 * @param persister The persister for the entity being requested for load
	 * @param entityKey The entity key
	 * @param instanceToLoad The instance that is being initialized, or null
	 * @param lockMode The lock mode
	 *
	 * @return The entity from the second-level cache, or null.
	 *
	 * @since 7.0
	 */
	@Incubating
	Object loadFromSecondLevelCache(
			@Nonnull EntityPersister persister,
			@Nonnull EntityKey entityKey,
			@Nullable Object instanceToLoad,
			@Nonnull LockMode lockMode);

	/**
	 * Wrap all state that lazy loading interceptors might need to
	 * manage association with this session, or to handle lazy loading
	 * after detachment via the UUID of the SessionFactory.
	 * N.B. this captures the current Session, however it can get
	 * updated to a null session (for detached entities) or updated to
	 * a different Session.
	 */
	@Incubating
	@Nonnull
	SessionAssociationMarkers getSessionAssociationMarkers();

	@Override
	@Nonnull
	<T> RootGraphImplementor<T> createEntityGraph(@Nonnull Class<T> rootType);

	@Override @Deprecated
	@Nullable
	@SuppressWarnings("removal")
	RootGraphImplementor<?> createEntityGraph(@Nonnull String graphName);

	@Override
	@Nonnull
	RootGraphImplementor<?> getEntityGraph(@Nonnull String graphName);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query-related covariance

	@Override
	@Nonnull
	<R> SelectionQueryImplementor<R> createQuery(@Nonnull String queryString, @Nonnull Class<R> resultClass);

	@Nonnull
	default <R> SelectionQueryImplementor<R> createQuery(@Nonnull Class<R> resultClass, @Nonnull String hqlString) {
		return createQuery( hqlString, resultClass );
	}

	@Override
	@Nonnull
	<R> SelectionQueryImplementor<R> createQuery(@Nonnull TypedQueryReference<R> typedQueryReference);

	@Override
	@Nonnull
	<R> NativeQueryImplementor<R> createNativeQuery(@Nonnull String sqlString, @Nonnull Class<R> resultClass);

	@Override
	@Nonnull
	default <R> NativeQueryImplementor<R> createNativeQuery(@Nonnull Class<R> resultClass, @Nonnull String sqlString) {
		return createNativeQuery( sqlString, resultClass );
	}

	@Override
	@Nonnull
	<R> NativeQueryImplementor<R> createNativeQuery(
			@Nonnull String sqlString,
			@Nonnull Class<R> resultClass,
			@Nonnull String tableAlias);

	@Override
	@Nonnull
	<R> NativeQueryImplementor<R> createNativeQuery(
			@Nonnull String sqlString,
			@Nonnull String resultSetMappingName,
			@Nonnull Class<R> resultClass);

	@Override
	@Nonnull
	<R> SelectionQueryImplementor<R> createNamedQuery(@Nonnull String name, @Nonnull Class<R> resultClass);

	@Override
	@Nonnull
	<R> NativeQueryImplementor<R> createNamedQuery(@Nonnull String name, @Nonnull String resultSetMappingName);

	@Override
	@Nonnull
	<R> NativeQueryImplementor<R> createNamedQuery(
			@Nonnull String name,
			@Nonnull String resultSetMappingName,
			@Nonnull Class<R> resultClass);

	@Override
	@Nonnull
	<T> SelectionQueryImplementor<T> createQuery(@Nonnull CriteriaSelect<T> criteriaSelect);

	@Override
	@Nonnull
	MutationQueryImplementor<?> createMutationQuery(@Nonnull CriteriaStatement<?> criteriaStatement);

	@Override
	@SuppressWarnings("rawtypes")
	@Nonnull
	MutationQueryImplementor createStatement(@Nonnull CriteriaStatement<?> criteriaStatement);

	@Override
	@Nonnull
	MutationQueryImplementor<?> createMutationQuery(@Nonnull JpaCriteriaInsert<?> insert);

	@Override
	@Nonnull
	<T> SelectionQueryImplementor<T> createQuery(@Nonnull String hqlString, @Nonnull EntityGraph<T> entityGraph);

	@Override
	@SuppressWarnings("rawtypes")
	@Nonnull
	NativeQueryImplementor createNativeQuery(@Nonnull String sql);

	@Override
	@SuppressWarnings("rawtypes")
	@Nonnull
	NativeQueryImplementor createNativeQuery(@Nonnull String sql, @Nonnull String resultSetMappingName);

	@Override
	@Nonnull
	<T> NativeQueryImplementor<T> createNativeQuery(@Nonnull String sql, @Nonnull ResultSetMapping<T> resultSetMapping);

	/**
	 * Allows accessing session scoped extension storages of the particular session instance.
	 * <p>
	 * Extensions first had to be registered with the {@link org.hibernate.SessionFactory}
	 *
	 * @param extension The extension storage attached to the current session.
	 * if the current session does not yet have the particular storage type attached to this session.
	 * @param <E> The type of the extension storage.
	 */
	@Incubating
	<E extends Extension> E getExtension(Class<E> extension);

}
