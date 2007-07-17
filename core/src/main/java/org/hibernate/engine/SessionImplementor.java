//$Id: SessionImplementor.java 10018 2006-06-15 05:21:06Z steve.ebersole@jboss.com $
package org.hibernate.engine;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.EntityMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Transaction;
import org.hibernate.engine.query.sql.NativeSQLQuerySpecification;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.event.EventListeners;
import org.hibernate.impl.CriteriaImpl;
import org.hibernate.jdbc.Batcher;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;


/**
 * Defines the internal contract between the <tt>Session</tt> and other parts of
 * Hibernate such as implementors of <tt>Type</tt> or <tt>EntityPersister</tt>.
 *
 * @see org.hibernate.Session the interface to the application
 * @see org.hibernate.impl.SessionImpl the actual implementation
 * @author Gavin King
 */
public interface SessionImplementor extends Serializable {

	/**
	 * Retrieves the interceptor currently in use by this event source.
	 *
	 * @return The interceptor.
	 */
	public Interceptor getInterceptor();
	
	/**
	 * Enable/disable automatic cache clearing from after transaction
	 * completion (for EJB3)
	 */
	public void setAutoClear(boolean enabled);
		
	/**
	 * Does this <tt>Session</tt> have an active Hibernate transaction
	 * or is there a JTA transaction in progress?
	 */
	public boolean isTransactionInProgress();

	/**
	 * Initialize the collection (if not already initialized)
	 */
	public void initializeCollection(PersistentCollection collection, boolean writing) 
	throws HibernateException;
	
	/**
	 * Load an instance without checking if it was deleted. 
	 * 
	 * When <tt>nullable</tt> is disabled this method may create a new proxy or 
	 * return an existing proxy; if it does not exist, throw an exception.
	 * 
	 * When <tt>nullable</tt> is enabled, the method does not create new proxies 
	 * (but might return an existing proxy); if it does not exist, return 
	 * <tt>null</tt>.
	 * 
	 * When <tt>eager</tt> is enabled, the object is eagerly fetched
	 */
	public Object internalLoad(String entityName, Serializable id, boolean eager, boolean nullable) 
	throws HibernateException;

	/**
	 * Load an instance immediately. This method is only called when lazily initializing a proxy.
	 * Do not return the proxy.
	 */
	public Object immediateLoad(String entityName, Serializable id) throws HibernateException;

	/**
	 * System time before the start of the transaction
	 */
	public long getTimestamp();
	/**
	 * Get the creating <tt>SessionFactoryImplementor</tt>
	 */
	public SessionFactoryImplementor getFactory();
	/**
	 * Get the prepared statement <tt>Batcher</tt> for this session
	 */
	public Batcher getBatcher();
	
	/**
	 * Execute a <tt>find()</tt> query
	 */
	public List list(String query, QueryParameters queryParameters) throws HibernateException;
	/**
	 * Execute an <tt>iterate()</tt> query
	 */
	public Iterator iterate(String query, QueryParameters queryParameters) throws HibernateException;
	/**
	 * Execute a <tt>scroll()</tt> query
	 */
	public ScrollableResults scroll(String query, QueryParameters queryParameters) throws HibernateException;
	/**
	 * Execute a criteria query
	 */
	public ScrollableResults scroll(CriteriaImpl criteria, ScrollMode scrollMode);
	/**
	 * Execute a criteria query
	 */
	public List list(CriteriaImpl criteria);
	
	/**
	 * Execute a filter
	 */
	public List listFilter(Object collection, String filter, QueryParameters queryParameters) throws HibernateException;
	/**
	 * Iterate a filter
	 */
	public Iterator iterateFilter(Object collection, String filter, QueryParameters queryParameters) throws HibernateException;
	
	/**
	 * Get the <tt>EntityPersister</tt> for any instance
	 * @param entityName optional entity name
	 * @param object the entity instance
	 */
	public EntityPersister getEntityPersister(String entityName, Object object) throws HibernateException;
	
	/**
	 * Get the entity instance associated with the given <tt>Key</tt>,
	 * calling the Interceptor if necessary
	 */
	public Object getEntityUsingInterceptor(EntityKey key) throws HibernateException;

	/**
	 * Notify the session that the transaction completed, so we no longer
	 * own the old locks. (Also we should release cache softlocks.) May
	 * be called multiple times during the transaction completion process.
	 * Also called after an autocommit, in which case the second argument
	 * is null.
	 */
	public void afterTransactionCompletion(boolean successful, Transaction tx);
	
	/**
	 * Notify the session that the transaction is about to complete
	 */
	public void beforeTransactionCompletion(Transaction tx);

	/**
	 * Return the identifier of the persistent object, or null if 
	 * not associated with the session
	 */
	public Serializable getContextEntityIdentifier(Object object);

	/**
	 * The best guess entity name for an entity not in an association
	 */
	public String bestGuessEntityName(Object object);
	
	/**
	 * The guessed entity name for an entity not in an association
	 */
	public String guessEntityName(Object entity) throws HibernateException;
	
	/** 
	 * Instantiate the entity class, initializing with the given identifier
	 */
	public Object instantiate(String entityName, Serializable id) throws HibernateException;
	
	/**
	 * Execute an SQL Query
	 */
	public List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) 
	throws HibernateException;
	
	/**
	 * Execute an SQL Query
	 */
	public ScrollableResults scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) 
	throws HibernateException;

	/**
	 * Execute a native SQL query, and return the results as a fully built list.
	 *
	 * @param spec The specification of the native SQL query to execute.
	 * @param queryParameters The parameters by which to perform the execution.
	 * @return The result list.
	 * @throws HibernateException
	 */
	public List list(NativeSQLQuerySpecification spec, QueryParameters queryParameters)
	throws HibernateException;

	/**
	 * Execute a native SQL query, and return the results as a scrollable result.
	 *
	 * @param spec The specification of the native SQL query to execute.
	 * @param queryParameters The parameters by which to perform the execution.
	 * @return The resulting scrollable result.
	 * @throws HibernateException
	 */
	public ScrollableResults scroll(NativeSQLQuerySpecification spec, QueryParameters queryParameters)
	throws HibernateException;

	/**
	 * Retreive the currently set value for a filter parameter.
	 *
	 * @param filterParameterName The filter parameter name in the format
	 * {FILTER_NAME.PARAMETER_NAME}.
	 * @return The filter parameter value.
	 */
	public Object getFilterParameterValue(String filterParameterName);

	/**
	 * Retreive the type for a given filter parrameter.
	 *
	 * @param filterParameterName The filter parameter name in the format
	 * {FILTER_NAME.PARAMETER_NAME}.
	 */
	public Type getFilterParameterType(String filterParameterName);

	/**
	 * Return the currently enabled filters.  The filter map is keyed by filter
	 * name, with values corresponding to the {@link org.hibernate.impl.FilterImpl}
	 * instance.
	 * @return The currently enabled filters.
	 */
	public Map getEnabledFilters();
	
	public int getDontFlushFromFind();
	
	/**
	 * Retrieves the configured event listeners from this event source.
	 *
	 * @return The configured event listeners.
	 */
	public EventListeners getListeners();
	
	//TODO: temporary
	
	/**
	 * Get the persistence context for this session
	 */
	public PersistenceContext getPersistenceContext();
	
	/**
	 * Execute a HQL update or delete query
	 */
	int executeUpdate(String query, QueryParameters queryParameters) throws HibernateException;
	
	/**
	 * Execute a native SQL update or delete query
	 */
	int executeNativeUpdate(NativeSQLQuerySpecification specification, QueryParameters queryParameters) throws HibernateException;

	// copied from Session:
	
	public EntityMode getEntityMode();
	public CacheMode getCacheMode();
	public void setCacheMode(CacheMode cm);
	public boolean isOpen();
	public boolean isConnected();
	public FlushMode getFlushMode();
	public void setFlushMode(FlushMode fm);
	public Connection connection();
	public void flush();
	
	/**
	 * Get a Query instance for a named query or named native SQL query
	 */
	public Query getNamedQuery(String name);
	/**
	 * Get a Query instance for a named native SQL query
	 */
	public Query getNamedSQLQuery(String name);
	
	public boolean isEventSource();

	public void afterScrollOperation();

	public void setFetchProfile(String name);

	public String getFetchProfile();

	public JDBCContext getJDBCContext();

	/**
	 * Determine whether the session is closed.  Provided seperately from
	 * {@link #isOpen()} as this method does not attempt any JTA synch
	 * registration, where as {@link #isOpen()} does; which makes this one
	 * nicer to use for most internal purposes.
	 *
	 * @return True if the session is closed; false otherwise.
	 */
	public boolean isClosed();
}
