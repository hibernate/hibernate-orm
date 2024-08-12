/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.util.Set;
import java.util.UUID;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.TransactionRequiredException;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.StatelessSession;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.event.spi.EventSource;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.query.Query;
import org.hibernate.SharedSessionContract;
import org.hibernate.Transaction;
import org.hibernate.cache.spi.CacheTransactionSynchronization;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.QueryProducerImplementor;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder.Options;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

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
		extends SharedSessionContract, JdbcSessionOwner, Options, LobCreationContext, WrapperOptions,
					QueryProducerImplementor, JavaType.CoercionContext {

	// todo : this is the shared contract between Session and StatelessSession,
	//        but it defines methods that StatelessSession does not implement
	//	      To me it seems like it is better to properly isolate those methods
	//	      into just the Session hierarchy. They include (at least):
	//		  1) get/set CacheMode
	//		  2) get/set FlushMode
	//		  3) get/set (default) read-only

	/**
	 * Obtain the {@linkplain SessionFactoryImplementor factory} which created this session.
	 */
	SessionFactoryImplementor getFactory();

	/**
	 * Obtain the {@linkplain SessionFactoryImplementor factory} which created this session.
	 */
	@Override
	default SessionFactoryImplementor getSessionFactory() {
		return getFactory();
	}

	/**
	 * Obtain the {@link TypeConfiguration} for the factory which created this session.
	 */
	@Override
	default TypeConfiguration getTypeConfiguration() {
		return getFactory().getTypeConfiguration();
	}

	/**
	 * Get the {@link SessionEventListenerManager} associated with this session.
	 */
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
	PersistenceContext getPersistenceContext();

	/**
	 * Obtain the {@link JdbcCoordinator} for this session.
	 */
	JdbcCoordinator getJdbcCoordinator();

	/**
	 * Obtain the {@link JdbcServices} for the factory which created this session.
	 */
	JdbcServices getJdbcServices();

	/**
	 * Obtain a {@link UUID} which uniquely identifies this session.
	 * <p>
	 * The UUID is useful mainly for logging.
	 */
	UUID getSessionIdentifier();

	/**
	 * Returns this object, fulfilling the contract of {@link WrapperOptions}.
	 */
	@Override
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
	 * <p>
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
	default void checkTransactionNeededForUpdateOperation(String exceptionMessage) {
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
	Transaction accessTransaction();

	/**
	 * Instantiate an {@link EntityKey} with the given id and for the
	 * entity represented by the given {@link EntityPersister}.
	 *
	 * @param id The entity id
	 * @param persister The entity persister
	 *
	 * @return The entity key
	 */
	EntityKey generateEntityKey(Object id, EntityPersister persister);

	/**
	 * Retrieves the {@link Interceptor} associated with this session.
	 */
	Interceptor getInterceptor();

	/**
	 * Enable or disable automatic cache clearing from after transaction
	 * completion.
	 *
	 * @deprecated there's no good reason to expose this here
	 */
	@Deprecated(since = "6")
	void setAutoClear(boolean enabled);

	/**
	 * Initialize the given collection (if not already initialized).
	 */
	void initializeCollection(PersistentCollection<?> collection, boolean writing)
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
	Object internalLoad(String entityName, Object id, boolean eager, boolean nullable)
			throws HibernateException;

	/**
	 * Load an instance immediately.
	 * This method is only called when lazily initializing a proxy.
	 * Do not return the proxy.
	 */
	Object immediateLoad(String entityName, Object id) throws HibernateException;


	/**
	 * Get the {@link EntityPersister} for the given entity instance.
	 *
	 * @param entityName optional entity name
	 * @param object the entity instance
	 */
	EntityPersister getEntityPersister(@Nullable String entityName, Object object) throws HibernateException;

	/**
	 * Get the entity instance associated with the given {@link EntityKey},
	 * calling the {@link Interceptor} if necessary.
	 */
	Object getEntityUsingInterceptor(EntityKey key) throws HibernateException;

	/**
	 * Return the identifier of the persistent object, or null if it is
	 * not associated with this session.
	 */
	Object getContextEntityIdentifier(Object object);

	/**
	 * Obtain the best estimate of the entity name of the given entity
	 * instance, which is not involved in an association, by also
	 * considering information held in the proxy, and whether the object
	 * is already associated with this session.
	 */
	String bestGuessEntityName(Object object);

	/**
	 * Obtain the best estimate of the entity name of the given entity
	 * instance, which is not involved in an association, by also
	 * considering information held in the proxy, and whether the object
	 * is already associated with this session.
	 */
	default String bestGuessEntityName(Object object, EntityEntry entry) {
		return bestGuessEntityName( object );
	}

	/**
	 * Obtain an estimate of the entity name of the given entity instance,
	 * which is not involved in an association, using only the
	 * {@link org.hibernate.EntityNameResolver}.
	 */
	String guessEntityName(Object entity) throws HibernateException;

	/**
	 * Instantiate the entity class, initializing with the given identifier.
	 */
	Object instantiate(String entityName, Object id) throws HibernateException;

	/**
	 * Instantiate the entity class of the given {@link EntityPersister},
	 * initializing the new instance with the given identifier.
	 * <p>
	 * This is more efficient than {@link #instantiate(String, Object)},
	 * but not always interchangeable, since a single persister might be
	 * responsible for multiple types.
	 */
	Object instantiate(EntityPersister persister, Object id) throws HibernateException;

	/**
	 * Are entities and proxies loaded by this session read-only by default?
	 */
	boolean isDefaultReadOnly();

	/**
	 * Get the current {@link CacheMode} for this session.
	 */
	CacheMode getCacheMode();

	/**
	 * Set the current {@link CacheMode} for this session.
	 */
	void setCacheMode(CacheMode cm);

	void setCriteriaCopyTreeEnabled(boolean jpaCriteriaCopyComplianceEnabled);

	boolean isCriteriaCopyTreeEnabled();

	boolean getNativeJdbcParametersIgnored();

	void setNativeJdbcParametersIgnored(boolean nativeJdbcParametersIgnored);

	/**
	 * Get the current {@link FlushModeType} for this session.
	 * <p>
	 * For users of the Hibernate native APIs, we've had to rename this method
	 * as defined by Hibernate historically because the JPA contract defines a method of the same
	 * name, but returning the JPA {@link FlushModeType} rather than Hibernate's {@link FlushMode}.
	 * For the former behavior, use {@link #getHibernateFlushMode()} instead.
	 *
	 * @return The {@link FlushModeType} in effect for this Session.
	 *
	 * @deprecated there's no good reason to expose this here
	 */
	@Deprecated(since = "6")
	FlushModeType getFlushMode();

	/**
	 * Set the current {@link FlushMode} for this session.
	 * <p>
	 * The flush mode determines the points at which the session is flushed.
	 * <em>Flushing</em> is the process of synchronizing the underlying persistent
	 * store with persistable state held in memory.
	 * <p>
	 * For a logically "read-only" session, it's reasonable to set the session
	 * flush mode to {@link FlushMode#MANUAL} at the start of the session
	 * (in order skip some work and gain some extra performance).
	 *
	 * @param flushMode the new flush mode
	 */
	void setHibernateFlushMode(FlushMode flushMode);

	/**
	 * Get the current {@link FlushMode} for this session.
	 *
	 * @return The flush mode
	 */
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
	default EventSource asEventSource() {
		throw new ClassCastException( "session is not an EventSource" );
	}

	/**
	 * Called after each operation on a {@link org.hibernate.ScrollableResults},
	 * providing an opportunity for a stateless session to clear its
	 * temporary persistence context. For a stateful session, this method
	 * does nothing.
	 */
	void afterScrollOperation();

	/**
	 * Should this session be automatically closed after the current
	 * transaction completes?
	 *
	 * @deprecated there's no reason to expose this here
	 */
	@Deprecated(since = "6")
	boolean shouldAutoClose();

	/**
	 * Is auto-close at transaction completion enabled?
	 *
	 * @see org.hibernate.cfg.AvailableSettings#AUTO_CLOSE_SESSION
	 * @see SessionFactoryOptions#isAutoCloseSessionEnabled()
	 *
	 * @deprecated there's no reason to expose this here
	 */
	@Deprecated(since = "6")
	boolean isAutoCloseSessionEnabled();

	/**
	 * Get the {@link LoadQueryInfluencers} associated with this session.
	 *
	 * @return the {@link LoadQueryInfluencers} associated with this session;
	 *         should never be null.
	 */
	LoadQueryInfluencers getLoadQueryInfluencers();

	/**
	 * Obtain an {@link ExceptionConverter} for reporting an error.
	 * <p>
	 * The converter associated to a session might be lazily initialized,
	 * so only invoke this getter when there's an actual need to use it.
	 *
	 * @return the ExceptionConverter for this Session.
	 */
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
	PersistenceContext getPersistenceContextInternal();

	/**
	 * detect in-memory changes, determine if the changes are to tables
	 * named in the query and, if so, complete execution the flush
	 *
	 * @param querySpaces the tables named in the query.
	 *
	 * @return true if flush is required, false otherwise.
	 */
	boolean autoFlushIfRequired(Set<String> querySpaces) throws HibernateException;

	default boolean autoFlushIfRequired(Set<String> querySpaces, boolean skipPreFlush)
			throws HibernateException {
		return autoFlushIfRequired( querySpaces );
	}

	default void autoPreFlush(){
	}

	/**
	 * Are we currently enforcing a {@linkplain GraphSemantic#FETCH fetch graph}?
	 *
	 * @deprecated this is not used  anywhere
	 */
	@Deprecated(since = "6", forRemoval = true)
	default boolean isEnforcingFetchGraph() {
		return false;
	}

	/**
	 * Enable or disable {@linkplain GraphSemantic#FETCH fetch graph} enforcement.
	 *
	 * @deprecated this is not used  anywhere
	 */
	@Deprecated(since = "6", forRemoval = true)
	default void setEnforcingFetchGraph(boolean enforcingFetchGraph) {
	}

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
	 * Cast this object to {@link SessionImplementor}, if possible.
	 *
	 * @throws ClassCastException if the cast is not possible
	 */
	default SessionImplementor asSessionImplementor() {
		throw new ClassCastException( "session is not a SessionImplementor" );
	}

	/**
	 * Does this object implement {@link SessionImplementor}?
	 */
	default boolean isSessionImplementor() {
		return false;
	}

	/**
	 * Cast this object to {@link StatelessSession}, if possible.
	 *
	 * @throws ClassCastException if the cast is not possible
	 */
	default StatelessSession asStatelessSession() {
		throw new ClassCastException( "session is not a StatelessSession" );
	}

	/**
	 * Does this object implement {@link StatelessSession}?
	 */
	default boolean isStatelessSession() {
		return false;
	}

}
