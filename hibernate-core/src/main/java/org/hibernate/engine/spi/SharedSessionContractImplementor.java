/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.persistence.FlushModeType;
import javax.persistence.TransactionRequiredException;
import javax.persistence.criteria.Selection;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.ScrollMode;
import org.hibernate.SharedSessionContract;
import org.hibernate.Transaction;
import org.hibernate.cache.spi.CacheTransactionSynchronization;
import org.hibernate.cfg.Environment;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jpa.spi.HibernateEntityManagerImplementor;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.QueryProducerImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder.Options;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Defines the internal contract shared between {@link org.hibernate.Session} and
 * {@link org.hibernate.StatelessSession} as used by other parts of Hibernate (such as
 * {@link org.hibernate.type.Type}, {@link EntityPersister} and
 * {@link org.hibernate.persister.collection.CollectionPersister} implementors
 *
 * A Session, through this interface and SharedSessionContractImplementor, implements:<ul>
 *     <li>
 *         {@link org.hibernate.resource.jdbc.spi.JdbcSessionOwner} to drive the behavior of a "JDBC session".
 *         Can therefor be used to construct a JdbcCoordinator, which (for now) models a "JDBC session"
 *     </li>
 *     <li>
 *         {@link Options}
 *         to drive the creation of the {@link TransactionCoordinator} delegate.
 *         This allows it to be passed along to
 *         {@link org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder#buildTransactionCoordinator}
 *     </li>
 *     <li>
 *         {@link org.hibernate.engine.jdbc.LobCreationContext} to act as the context for JDBC LOB instance creation
 *     </li>
 *     <li>
 *         {@link org.hibernate.type.descriptor.WrapperOptions} to fulfill the behavior needed while
 *         binding/extracting values to/from JDBC as part of the Type contracts
 *     </li>
 * </ul>
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface SharedSessionContractImplementor
		extends SharedSessionContract, JdbcSessionOwner, Options, LobCreationContext, WrapperOptions, QueryProducerImplementor {

	// todo : this is the shared contract between Session and StatelessSession, but it defines methods that StatelessSession does not implement
	//	(it just throws UnsupportedOperationException).  To me it seems like it is better to properly isolate those methods
	//	into just the Session hierarchy.  They include (at least):
	//		1) get/set CacheMode
	//		2) get/set FlushMode
	//		3) get/set (default) read-only
	//		4) #setAutoClear
	//		5) #disableTransactionAutoJoin

	/**
	 * Get the creating <tt>SessionFactoryImplementor</tt>
	 */
	SessionFactoryImplementor getFactory();

	SessionEventListenerManager getEventListenerManager();

	/**
	 * Get the persistence context for this session.
	 * See also {@link #getPersistenceContextInternal()} for
	 * an alternative.
	 *
	 * This method is not extremely fast: if you need to access
	 * the PersistenceContext multiple times, prefer keeping
	 * a reference to it over invoking this method multiple times.
	 */
	PersistenceContext getPersistenceContext();

	JdbcCoordinator getJdbcCoordinator();

	JdbcServices getJdbcServices();

	/**
	 * The multi-tenancy tenant identifier, if one.
	 *
	 * @return The tenant identifier; may be {@code null}
	 */
	String getTenantIdentifier();

	/**
	 * A UUID associated with each Session.  Useful mainly for logging.
	 *
	 * @return The UUID
	 */
	UUID getSessionIdentifier();

	/**
	 * Checks whether the session is closed.  Provided separately from
	 * {@link #isOpen()} as this method does not attempt any JTA synchronization
	 * registration, whereas {@link #isOpen()} does; which makes this one
	 * nicer to use for most internal purposes.
	 *
	 * @return {@code true} if the session is closed; {@code false} otherwise.
	 */
	boolean isClosed();

	/**
	 * Checks whether the session is open or is waiting for auto-close
	 *
	 * @return {@code true} if the session is closed or if it's waiting for auto-close; {@code false} otherwise.
	 */
	default boolean isOpenOrWaitingForAutoClose() {
		return !isClosed();
	}

	/**
	 * Performs a check whether the Session is open, and if not:<ul>
	 *     <li>marks current transaction (if one) for rollback only</li>
	 *     <li>throws an IllegalStateException (JPA defines the exception type)</li>
	 * </ul>
	 */
	default void checkOpen() {
		checkOpen( true );
	}

	/**
	 * Performs a check whether the Session is open, and if not:<ul>
	 *     <li>if {@code markForRollbackIfClosed} is true, marks current transaction (if one) for rollback only</li>
	 *     <li>throws an IllegalStateException (JPA defines the exception type)</li>
	 * </ul>
	 */
	void checkOpen(boolean markForRollbackIfClosed);

	/**
	 * Marks current transaction (if one) for rollback only
	 */
	void markForRollbackOnly();

	/**
	 * A "timestamp" at or before the start of the current transaction.
	 *
	 * @apiNote This "timestamp" need not be related to timestamp in the Java Date/millisecond
	 * sense.  It just needs to be an incrementing value.  See
	 * {@link CacheTransactionSynchronization#getCurrentTransactionStartTimestamp()}
	 */
	long getTransactionStartTimestamp();

	/**
	 * @deprecated (since 5.3) Use {@link #getTransactionStartTimestamp()} instead.
	 */
	@Deprecated
	default long getTimestamp() {
		return getTransactionStartTimestamp();
	}

	/**
	 * The current CacheTransactionContext associated with the Session.  This may
	 * return {@code null} when the Session is not currently part of a transaction.
	 */
	CacheTransactionSynchronization getCacheTransactionSynchronization();

	/**
	 * Does this <tt>Session</tt> have an active Hibernate transaction
	 * or is there a JTA transaction in progress?
	 */
	boolean isTransactionInProgress();

	/**
	 * Check if an active Transaction is necessary for the update operation to be executed.
	 * If an active Transaction is necessary but it is not then a TransactionRequiredException is raised.
	 *
	 * @param exceptionMessage the message to use for the TransactionRequiredException
	 */
	default void checkTransactionNeededForUpdateOperation(String exceptionMessage) {
		if ( !isTransactionInProgress() ) {
			throw new TransactionRequiredException( exceptionMessage );
		}
	}

	/**
	 * Provides access to the underlying transaction or creates a new transaction if
	 * one does not already exist or is active.  This is primarily for internal or
	 * integrator use.
	 *
	 * @return the transaction
     */
	Transaction accessTransaction();

	/**
	 * Hide the changing requirements of entity key creation
	 *
	 * @param id The entity id
	 * @param persister The entity persister
	 *
	 * @return The entity key
	 */
	EntityKey generateEntityKey(Serializable id, EntityPersister persister);

	/**
	 * Retrieves the interceptor currently in use by this event source.
	 *
	 * @return The interceptor.
	 */
	Interceptor getInterceptor();

	/**
	 * Enable/disable automatic cache clearing from after transaction
	 * completion (for EJB3)
	 */
	void setAutoClear(boolean enabled);

	/**
	 * Initialize the collection (if not already initialized)
	 */
	void initializeCollection(PersistentCollection collection, boolean writing)
			throws HibernateException;

	/**
	 * Load an instance without checking if it was deleted.
	 * <p/>
	 * When <tt>nullable</tt> is disabled this method may create a new proxy or
	 * return an existing proxy; if it does not exist, throw an exception.
	 * <p/>
	 * When <tt>nullable</tt> is enabled, the method does not create new proxies
	 * (but might return an existing proxy); if it does not exist, return
	 * <tt>null</tt>.
	 * <p/>
	 * When <tt>eager</tt> is enabled, the object is eagerly fetched
	 */
	Object internalLoad(String entityName, Serializable id, boolean eager, boolean nullable)
			throws HibernateException;

	/**
	 * Load an instance immediately. This method is only called when lazily initializing a proxy.
	 * Do not return the proxy.
	 */
	Object immediateLoad(String entityName, Serializable id) throws HibernateException;


	/**
	 * Execute a <tt>find()</tt> query
	 */
	List list(String query, QueryParameters queryParameters) throws HibernateException;

	/**
	 * Execute an <tt>iterate()</tt> query
	 */
	Iterator iterate(String query, QueryParameters queryParameters) throws HibernateException;

	/**
	 * Execute a <tt>scroll()</tt> query
	 */
	ScrollableResultsImplementor scroll(String query, QueryParameters queryParameters) throws HibernateException;

	/**
	 * Execute a criteria query
	 */
	ScrollableResultsImplementor scroll(Criteria criteria, ScrollMode scrollMode);

	/**
	 * Execute a criteria query
	 */
	List list(Criteria criteria);

	/**
	 * Execute a filter
	 */
	List listFilter(Object collection, String filter, QueryParameters queryParameters) throws HibernateException;

	/**
	 * Iterate a filter
	 */
	Iterator iterateFilter(Object collection, String filter, QueryParameters queryParameters)
			throws HibernateException;

	/**
	 * Get the <tt>EntityPersister</tt> for any instance
	 *
	 * @param entityName optional entity name
	 * @param object the entity instance
	 */
	EntityPersister getEntityPersister(String entityName, Object object) throws HibernateException;

	/**
	 * Get the entity instance associated with the given <tt>Key</tt>,
	 * calling the Interceptor if necessary
	 */
	Object getEntityUsingInterceptor(EntityKey key) throws HibernateException;

	/**
	 * Return the identifier of the persistent object, or null if
	 * not associated with the session
	 */
	Serializable getContextEntityIdentifier(Object object);

	/**
	 * The best guess entity name for an entity not in an association
	 */
	String bestGuessEntityName(Object object);

	/**
	 * The guessed entity name for an entity not in an association
	 */
	String guessEntityName(Object entity) throws HibernateException;

	/**
	 * Instantiate the entity class, initializing with the given identifier
	 */
	Object instantiate(String entityName, Serializable id) throws HibernateException;

	/**
	 * Execute an SQL Query
	 */
	List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters)
			throws HibernateException;

	/**
	 * Execute an SQL Query
	 */
	ScrollableResultsImplementor scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters)
			throws HibernateException;

	/**
	 * Execute a native SQL query, and return the results as a fully built list.
	 *
	 * @param spec The specification of the native SQL query to execute.
	 * @param queryParameters The parameters by which to perform the execution.
	 *
	 * @return The result list.
	 *
	 * @throws HibernateException
	 */
	List list(NativeSQLQuerySpecification spec, QueryParameters queryParameters)
			throws HibernateException;

	/**
	 * Execute a native SQL query, and return the results as a scrollable result.
	 *
	 * @param spec The specification of the native SQL query to execute.
	 * @param queryParameters The parameters by which to perform the execution.
	 *
	 * @return The resulting scrollable result.
	 *
	 * @throws HibernateException
	 */
	ScrollableResultsImplementor scroll(NativeSQLQuerySpecification spec, QueryParameters queryParameters);

	int getDontFlushFromFind();

	/**
	 * Execute a HQL update or delete query
	 */
	int executeUpdate(String query, QueryParameters queryParameters) throws HibernateException;

	/**
	 * Execute a native SQL update or delete query
	 */
	int executeNativeUpdate(NativeSQLQuerySpecification specification, QueryParameters queryParameters)
			throws HibernateException;


	CacheMode getCacheMode();

	void setCacheMode(CacheMode cm);

	/**
	 * Set the flush mode for this session.
	 * <p/>
	 * The flush mode determines the points at which the session is flushed.
	 * <i>Flushing</i> is the process of synchronizing the underlying persistent
	 * store with persistable state held in memory.
	 * <p/>
	 * For a logically "read only" session, it is reasonable to set the session's
	 * flush mode to {@link FlushMode#MANUAL} at the start of the session (in
	 * order to achieve some extra performance).
	 *
	 * @param flushMode the new flush mode
	 *
	 * @deprecated (since 5.2) use {@link #setHibernateFlushMode(FlushMode)} instead
	 */
	@Deprecated
	void setFlushMode(FlushMode flushMode);

	/**
	 * Get the flush mode for this session.
	 * <p/>
	 * For users of the Hibernate native APIs, we've had to rename this method
	 * as defined by Hibernate historically because the JPA contract defines a method of the same
	 * name, but returning the JPA {@link FlushModeType} rather than Hibernate's {@link FlushMode}.  For
	 * the former behavior, use {@link #getHibernateFlushMode()} instead.
	 *
	 * @return The FlushModeType in effect for this Session.
	 */
	FlushModeType getFlushMode();

	/**
	 * Set the flush mode for this session.
	 * <p/>
	 * The flush mode determines the points at which the session is flushed.
	 * <i>Flushing</i> is the process of synchronizing the underlying persistent
	 * store with persistable state held in memory.
	 * <p/>
	 * For a logically "read only" session, it is reasonable to set the session's
	 * flush mode to {@link FlushMode#MANUAL} at the start of the session (in
	 * order to achieve some extra performance).
	 *
	 * @param flushMode the new flush mode
	 */
	void setHibernateFlushMode(FlushMode flushMode);

	/**
	 * Get the current flush mode for this session.
	 *
	 * @return The flush mode
	 */
	FlushMode getHibernateFlushMode();

	Connection connection();

	void flush();

	boolean isEventSource();

	void afterScrollOperation();

	boolean shouldAutoClose();

	boolean isAutoCloseSessionEnabled();

	default boolean isQueryParametersValidationEnabled(){
		return getFactory().getSessionFactoryOptions().isQueryParametersValidationEnabled();
	}

	/**
	 * Get the load query influencers associated with this session.
	 *
	 * @return the load query influencers associated with this session;
	 *         should never be null.
	 */
	LoadQueryInfluencers getLoadQueryInfluencers();

	/**
	 * The converter associated to a Session might be lazily initialized: only invoke
	 * this getter when there is actual need to use it.
	 *
	 * @return the ExceptionConverter for this Session.
	 */
	ExceptionConverter getExceptionConverter();

	/**
	 * Get the currently configured JDBC batch size either at the Session-level or SessionFactory-level.
	 *
	 * If the Session-level JDBC batch size was not configured, return the SessionFactory-level one.
	 *
	 * @return Session-level or or SessionFactory-level JDBC batch size.
	 *
	 * @since 5.2
	 *
	 * @see org.hibernate.boot.spi.SessionFactoryOptions#getJdbcBatchSize
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyJdbcBatchSize
	 */
	default Integer getConfiguredJdbcBatchSize() {
		final Integer sessionJdbcBatchSize = getJdbcBatchSize();

		return sessionJdbcBatchSize == null ?
			ConfigurationHelper.getInt(
					Environment.STATEMENT_BATCH_SIZE,
					getFactory().getProperties(),
					1
			) :
			sessionJdbcBatchSize;
	}

	/**
	 * @deprecated (since 5.2) - see deprecation note on
	 * org.hibernate.jpa.spi.HibernateEntityManagerImplementor#createQuery(java.lang.String, java.lang.Class, javax.persistence.criteria.Selection, org.hibernate.jpa.spi.HibernateEntityManagerImplementor.QueryOptions)
	 * @return The typed query
	 */
	@Deprecated
	<T> QueryImplementor<T> createQuery(
			String jpaqlString,
			Class<T> resultClass,
			Selection selection,
			HibernateEntityManagerImplementor.QueryOptions queryOptions);

	/**
	 * This is similar to {@link #getPersistenceContext()}, with
	 * two main differences:
	 * a) this version performs better as
	 * it allows for inlining and probably better prediction
	 * b) see SessionImpl{@link #getPersistenceContext()} : it
	 * does some checks on the current state of the Session.
	 *
	 * Choose wisely: performance is important, correctness comes first.
	 *
	 * @return the PersistenceContext associated to this session.
	 */
	PersistenceContext getPersistenceContextInternal();

	/**
	 * Get the current fetch graph context (either {@link org.hibernate.graph.spi.RootGraphImplementor} or {@link org.hibernate.graph.spi.SubGraphImplementor}. 
	 * Suppose fetch graph is "a(b(c))", then during {@link org.hibernate.engine.internal.TwoPhaseLoad}:
	 * <ul>
	 *     <li>when loading root</li>: {@link org.hibernate.graph.spi.RootGraphImplementor root} will be returned
	 *     <li>when internally loading 'a'</li>: {@link org.hibernate.graph.spi.SubGraphImplementor subgraph} of 'a' will be returned
	 *     <li>when internally loading 'b'</li>: {@link org.hibernate.graph.spi.SubGraphImplementor subgraph} of 'a(b)' will be returned
	 *     <li>when internally loading 'c'</li>: {@link org.hibernate.graph.spi.SubGraphImplementor subgraph} of 'a(b(c))' will be returned
	 * </ul>
	 * 
	 * @return current fetch graph context; can be null if fetch graph is not effective or the graph eager loading is done.
	 * @see #setFetchGraphLoadContext(GraphImplementor) 
	 * @see org.hibernate.engine.internal.TwoPhaseLoad
	 */
	default GraphImplementor getFetchGraphLoadContext() {
		return null;
	}

	/**
	 * Set the current fetch graph context (either {@link org.hibernate.graph.spi.RootGraphImplementor} or {@link org.hibernate.graph.spi.SubGraphImplementor}.
	 * 
	 * @param fetchGraphLoadContext new fetch graph context; can be null (this field will be set to null after root entity loading is done).
	 * @see #getFetchGraphLoadContext()                                 
	 */
	default void setFetchGraphLoadContext(GraphImplementor fetchGraphLoadContext) {
	}

}
