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

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.type.Type;

/**
 * Defines the internal contract between {@link org.hibernate.Session} / {@link org.hibernate.StatelessSession} and
 * other parts of Hibernate such as {@link Type}, {@link EntityPersister} and
 * {@link org.hibernate.persister.collection.CollectionPersister} implementors
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface SessionImplementor extends Serializable, LobCreationContext {
	/**
	 * Match te method on {@link org.hibernate.Session} and {@link org.hibernate.StatelessSession}
	 *
	 * @return The tenant identifier of this session
	 */
	String getTenantIdentifier();

	/**
	 * Provides access to JDBC connections
	 *
	 * @return The contract for accessing JDBC connections.
	 */
	JdbcConnectionAccess getJdbcConnectionAccess();

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
	 * Disable automatic transaction joining.  The really only has any effect for CMT transactions.  The default
	 * Hibernate behavior is to auto join any active JTA transaction (register {@link javax.transaction.Synchronization}).
	 * JPA however defines an explicit join transaction operation.
	 * <p/>
	 * See javax.persistence.EntityManager#joinTransaction
	 */
	void disableTransactionAutoJoin();

	/**
	 * Does this <tt>Session</tt> have an active Hibernate transaction
	 * or is there a JTA transaction in progress?
	 */
	boolean isTransactionInProgress();

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
	 * System time before the start of the transaction
	 */
	long getTimestamp();

	/**
	 * Get the creating <tt>SessionFactoryImplementor</tt>
	 */
	SessionFactoryImplementor getFactory();

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
	ScrollableResults scroll(String query, QueryParameters queryParameters) throws HibernateException;

	/**
	 * Execute a criteria query
	 */
	ScrollableResults scroll(Criteria criteria, ScrollMode scrollMode);

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
	ScrollableResults scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters)
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
	ScrollableResults scroll(NativeSQLQuerySpecification spec, QueryParameters queryParameters);

	int getDontFlushFromFind();

	//TODO: temporary

	/**
	 * Get the persistence context for this session
	 */
	PersistenceContext getPersistenceContext();

	/**
	 * Execute a HQL update or delete query
	 */
	int executeUpdate(String query, QueryParameters queryParameters) throws HibernateException;

	/**
	 * Execute a native SQL update or delete query
	 */
	int executeNativeUpdate(NativeSQLQuerySpecification specification, QueryParameters queryParameters)
			throws HibernateException;


	// copied from Session:

	CacheMode getCacheMode();

	void setCacheMode(CacheMode cm);

	boolean isOpen();

	boolean isConnected();

	FlushMode getFlushMode();

	void setFlushMode(FlushMode fm);

	Connection connection();

	void flush();

	/**
	 * Get a Query instance for a named query or named native SQL query
	 */
	Query getNamedQuery(String name);

	/**
	 * Get a Query instance for a named native SQL query
	 */
	Query getNamedSQLQuery(String name);

	boolean isEventSource();

	void afterScrollOperation();

	/**
	 * Retrieve access to the session's transaction coordinator.
	 *
	 * @return The transaction coordinator.
	 */
	TransactionCoordinator getTransactionCoordinator();

	JdbcCoordinator getJdbcCoordinator();

	/**
	 * Determine whether the session is closed.  Provided separately from
	 * {@link #isOpen()} as this method does not attempt any JTA synchronization
	 * registration, where as {@link #isOpen()} does; which makes this one
	 * nicer to use for most internal purposes.
	 *
	 * @return True if the session is closed; false otherwise.
	 */
	boolean isClosed();

	boolean shouldAutoClose();

	boolean isAutoCloseSessionEnabled();

	/**
	 * Get the load query influencers associated with this session.
	 *
	 * @return the load query influencers associated with this session;
	 *         should never be null.
	 */
	LoadQueryInfluencers getLoadQueryInfluencers();

	/**
	 * Used from EntityManager
	 *
	 * @param namedQueryDefinition The named query definition
	 *
	 * @return The basic HQL/JPQL query (without saved settings applied)
	 */
	Query createQuery(NamedQueryDefinition namedQueryDefinition);

	/**
	 * Used from EntityManager
	 *
	 * @param namedQueryDefinition The named query definition
	 *
	 * @return The basic SQL query (without saved settings applied)
	 */
	SQLQuery createSQLQuery(NamedSQLQueryDefinition namedQueryDefinition);

	SessionEventListenerManager getEventListenerManager();
}
