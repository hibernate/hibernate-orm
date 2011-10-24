/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.cache.spi.CacheKey;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.entity.EntityPersister;
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
	public String getTenantIdentifier();

	/**
	 * Provides access to JDBC connections
	 *
	 * @return The contract for accessing JDBC connections.
	 */
	public JdbcConnectionAccess getJdbcConnectionAccess();

	/**
	 * Hide the changing requirements of entity key creation
	 *
	 * @param id The entity id
	 * @param persister The entity persister
	 *
	 * @return The entity key
	 */
	public EntityKey generateEntityKey(Serializable id, EntityPersister persister);

	/**
	 * Hide the changing requirements of cache key creation.
	 *
	 * @param id The entity identifier or collection key.
	 * @param type The type
	 * @param entityOrRoleName The entity name or collection role.
	 *
	 * @return The cache key
	 */
	public CacheKey generateCacheKey(Serializable id, final Type type, final String entityOrRoleName);

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
	 * Disable automatic transaction joining.  The really only has any effect for CMT transactions.  The default
	 * Hibernate behavior is to auto join any active JTA transaction (register {@link javax.transaction.Synchronization}).
	 * JPA however defines an explicit join transaction operation.
	 *
	 * See javax.persistence.EntityManager#joinTransaction
	 */
	public void disableTransactionAutoJoin();

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
	 * @deprecated use #getLoadQueryInfluencers instead
	 */
	@Deprecated
    public Object getFilterParameterValue(String filterParameterName);

	/**
	 * Retreive the type for a given filter parrameter.
	 *
	 * @param filterParameterName The filter parameter name in the format
	 * {FILTER_NAME.PARAMETER_NAME}.
	 * @return The filter param type
	 * @deprecated use #getLoadQueryInfluencers instead
	 */
	@Deprecated
    public Type getFilterParameterType(String filterParameterName);

	/**
	 * Return the currently enabled filters.  The filter map is keyed by filter
	 * name, with values corresponding to the {@link org.hibernate.internal.FilterImpl}
	 * instance.
	 * @return The currently enabled filters.
	 * @deprecated use #getLoadQueryInfluencers instead
	 */
	@Deprecated
    public Map getEnabledFilters();

	public int getDontFlushFromFind();

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


	/**
	 * Return changes to this session that have not been flushed yet.
	 *
	 * @return The non-flushed changes.
	 */
	public NonFlushedChanges getNonFlushedChanges() throws HibernateException;

	/**
	 * Apply non-flushed changes from a different session to this session. It is assumed
	 * that this SessionImpl is "clean" (e.g., has no non-flushed changes, no cached entities,
	 * no cached collections, no queued actions). The specified NonFlushedChanges object cannot
	 * be bound to any session.
	 * <p/>
	 * @param nonFlushedChanges the non-flushed changes
	 */
	public void applyNonFlushedChanges(NonFlushedChanges nonFlushedChanges) throws HibernateException;

	// copied from Session:

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

	/**
	 * Get the <i>internal</i> fetch profile currently associated with this session.
	 *
	 * @return The current internal fetch profile, or null if none currently associated.
	 * @deprecated use #getLoadQueryInfluencers instead
	 */
	@Deprecated
    public String getFetchProfile();

	/**
	 * Set the current <i>internal</i> fetch profile for this session.
	 *
	 * @param name The internal fetch profile name to use
	 * @deprecated use {@link #getLoadQueryInfluencers} instead
	 */
	@Deprecated
    public void setFetchProfile(String name);

	/**
	 * Retrieve access to the session's transaction coordinator.
	 *
	 * @return The transaction coordinator.
	 */
	public TransactionCoordinator getTransactionCoordinator();

	/**
	 * Determine whether the session is closed.  Provided separately from
	 * {@link #isOpen()} as this method does not attempt any JTA synchronization
	 * registration, where as {@link #isOpen()} does; which makes this one
	 * nicer to use for most internal purposes.
	 *
	 * @return True if the session is closed; false otherwise.
	 */
	public boolean isClosed();

	/**
	 * Get the load query influencers associated with this session.
	 *
	 * @return the load query influencers associated with this session;
	 * should never be null.
	 */
	public LoadQueryInfluencers getLoadQueryInfluencers();
}
