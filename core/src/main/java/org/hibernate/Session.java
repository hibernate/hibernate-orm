/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate;

import java.io.Serializable;
import java.sql.Connection;

import org.hibernate.jdbc.Work;
import org.hibernate.stat.SessionStatistics;

/**
 * The main runtime interface between a Java application and Hibernate. This is the
 * central API class abstracting the notion of a persistence service.<br>
 * <br>
 * The lifecycle of a <tt>Session</tt> is bounded by the beginning and end of a logical
 * transaction. (Long transactions might span several database transactions.)<br>
 * <br>
 * The main function of the <tt>Session</tt> is to offer create, read and delete operations
 * for instances of mapped entity classes. Instances may exist in one of three states:<br>
 * <br>
 * <i>transient:</i> never persistent, not associated with any <tt>Session</tt><br>
 * <i>persistent:</i> associated with a unique <tt>Session</tt><br>
 * <i>detached:</i> previously persistent, not associated with any <tt>Session</tt><br>
 * <br>
 * Transient instances may be made persistent by calling <tt>save()</tt>,
 * <tt>persist()</tt> or <tt>saveOrUpdate()</tt>. Persistent instances may be made transient
 * by calling<tt> delete()</tt>. Any instance returned by a <tt>get()</tt> or
 * <tt>load()</tt> method is persistent. Detached instances may be made persistent
 * by calling <tt>update()</tt>, <tt>saveOrUpdate()</tt>, <tt>lock()</tt> or <tt>replicate()</tt>. 
 * The state of a transient or detached instance may also be made persistent as a new
 * persistent instance by calling <tt>merge()</tt>.<br>
 * <br>
 * <tt>save()</tt> and <tt>persist()</tt> result in an SQL <tt>INSERT</tt>, <tt>delete()</tt>
 * in an SQL <tt>DELETE</tt> and <tt>update()</tt> or <tt>merge()</tt> in an SQL <tt>UPDATE</tt>. 
 * Changes to <i>persistent</i> instances are detected at flush time and also result in an SQL
 * <tt>UPDATE</tt>. <tt>saveOrUpdate()</tt> and <tt>replicate()</tt> result in either an
 * <tt>INSERT</tt> or an <tt>UPDATE</tt>.<br>
 * <br>
 * It is not intended that implementors be threadsafe. Instead each thread/transaction
 * should obtain its own instance from a <tt>SessionFactory</tt>.<br>
 * <br>
 * A <tt>Session</tt> instance is serializable if its persistent classes are serializable.<br>
 * <br>
 * A typical transaction should use the following idiom:
 * <pre>
 * Session sess = factory.openSession();
 * Transaction tx;
 * try {
 *     tx = sess.beginTransaction();
 *     //do some work
 *     ...
 *     tx.commit();
 * }
 * catch (Exception e) {
 *     if (tx!=null) tx.rollback();
 *     throw e;
 * }
 * finally {
 *     sess.close();
 * }
 * </pre>
 * <br>
 * If the <tt>Session</tt> throws an exception, the transaction must be rolled back
 * and the session discarded. The internal state of the <tt>Session</tt> might not
 * be consistent with the database after the exception occurs.
 *
 * @see SessionFactory
 * @author Gavin King
 */
public interface Session extends Serializable {

	/**
	 * Retrieve the entity mode in effect for this session.
	 *
	 * @return The entity mode for this session.
	 */
	public EntityMode getEntityMode();

	/**
	 * Starts a new Session with the given entity mode in effect. This secondary
	 * Session inherits the connection, transaction, and other context
	 * information from the primary Session. It doesn't need to be flushed
	 * or closed by the developer.
	 * 
	 * @param entityMode The entity mode to use for the new session.
	 * @return The new session
	 */
	public Session getSession(EntityMode entityMode);

	/**
	 * Force this session to flush. Must be called at the end of a
	 * unit of work, before commiting the transaction and closing the
	 * session (depending on {@link #setFlushMode flush-mode},
	 * {@link Transaction#commit()} calls this method).
	 * <p/>
	 * <i>Flushing</i> is the process of synchronizing the underlying persistent
	 * store with persistable state held in memory.
	 *
	 * @throws HibernateException Indicates problems flushing the session or
	 * talking to the database.
	 */
	public void flush() throws HibernateException;

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
	 * @see FlushMode
	 */
	public void setFlushMode(FlushMode flushMode);

	/**
	 * Get the current flush mode for this session.
	 *
	 * @return The flush mode
	 */
	public FlushMode getFlushMode();

	/**
	 * Set the cache mode.
	 * <p/>
	 * Cache mode determines the manner in which this session can interact with
	 * the second level cache.
	 *
	 * @param cacheMode The new cache mode.
	 */
	public void setCacheMode(CacheMode cacheMode);

	/**
	 * Get the current cache mode.
	 *
	 * @return The current cache mode.
	 */
	public CacheMode getCacheMode();

	/**
	 * Get the session factory which created this session.
	 *
	 * @return The session factory.
	 * @see SessionFactory

	 */
	public SessionFactory getSessionFactory();

	/**
	 * Get the JDBC connection of this Session.<br>
	 * <br>
	 * If the session is using aggressive collection release (as in a
	 * CMT environment), it is the application's responsibility to
	 * close the connection returned by this call. Otherwise, the
	 * application should not close the connection.
	 *
	 * @return the JDBC connection in use by the <tt>Session</tt>
	 * @throws HibernateException if the <tt>Session</tt> is disconnected
	 * @deprecated (scheduled for removal in 4.x).  Replacement depends on need; for doing direct JDBC stuff use
	 * {@link #doWork}; for opening a 'temporary Session' use (TBD).
	 */
	public Connection connection() throws HibernateException;

	/**
	 * End the session by releasing the JDBC connection and cleaning up.  It is
	 * not strictly necessary to close the session but you must at least
	 * {@link #disconnect()} it.
	 *
	 * @return the connection provided by the application or null.
	 * @throws HibernateException Indicates problems cleaning up.
	 */
	public Connection close() throws HibernateException;

	/**
	 * Cancel the execution of the current query.
	 * <p/>
	 * This is the sole method on session which may be safely called from
	 * another thread.
	 *
	 * @throws HibernateException There was a problem canceling the query
	 */
	public void cancelQuery() throws HibernateException;

	/**
	 * Check if the session is still open.
	 *
	 * @return boolean
	 */
	public boolean isOpen();

	/**
	 * Check if the session is currently connected.
	 *
	 * @return boolean
	 */
	public boolean isConnected();

	/**
	 * Does this session contain any changes which must be synchronized with
	 * the database?  In other words, would any DML operations be executed if
	 * we flushed this session?
	 *
	 * @return True if the session contains pending changes; false otherwise.
	 * @throws HibernateException could not perform dirtying checking
	 */
	public boolean isDirty() throws HibernateException;

	/**
	 * Return the identifier value of the given entity as associated with this
	 * session.  An exception is thrown if the given entity instance is transient
	 * or detached in relation to this session.
	 *
	 * @param object a persistent instance
	 * @return the identifier
	 * @throws TransientObjectException if the instance is transient or associated with
	 * a different session
	 */
	public Serializable getIdentifier(Object object) throws HibernateException;

	/**
	 * Check if this instance is associated with this <tt>Session</tt>.
	 *
	 * @param object an instance of a persistent class
	 * @return true if the given instance is associated with this <tt>Session</tt>
	 */
	public boolean contains(Object object);

	/**
	 * Remove this instance from the session cache. Changes to the instance will
	 * not be synchronized with the database. This operation cascades to associated
	 * instances if the association is mapped with <tt>cascade="evict"</tt>.
	 *
	 * @param object a persistent instance
	 * @throws HibernateException
	 */
	public void evict(Object object) throws HibernateException;

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * obtaining the specified lock mode, assuming the instance exists.
	 *
	 * @param theClass a persistent class
	 * @param id a valid identifier of an existing persistent instance of the class
	 * @param lockMode the lock level
	 * @return the persistent instance or proxy
	 * @throws HibernateException
	 */
	public Object load(Class theClass, Serializable id, LockMode lockMode) throws HibernateException;

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * obtaining the specified lock mode, assuming the instance exists.
	 *
	 * @param entityName a persistent class
	 * @param id a valid identifier of an existing persistent instance of the class
	 * @param lockMode the lock level
	 * @return the persistent instance or proxy
	 * @throws HibernateException
	 */
	public Object load(String entityName, Serializable id, LockMode lockMode) throws HibernateException;

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * assuming that the instance exists. This method might return a proxied instance that
	 * is initialized on-demand, when a non-identifier method is accessed.
	 * <br><br>
	 * You should not use this method to determine if an instance exists (use <tt>get()</tt>
	 * instead). Use this only to retrieve an instance that you assume exists, where non-existence
	 * would be an actual error.
	 *
	 * @param theClass a persistent class
	 * @param id a valid identifier of an existing persistent instance of the class
	 * @return the persistent instance or proxy
	 * @throws HibernateException
	 */
	public Object load(Class theClass, Serializable id) throws HibernateException;

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * assuming that the instance exists. This method might return a proxied instance that
	 * is initialized on-demand, when a non-identifier method is accessed.
	 * <br><br>
	 * You should not use this method to determine if an instance exists (use <tt>get()</tt>
	 * instead). Use this only to retrieve an instance that you assume exists, where non-existence
	 * would be an actual error.
	 *
	 * @param entityName a persistent class
	 * @param id a valid identifier of an existing persistent instance of the class
	 * @return the persistent instance or proxy
	 * @throws HibernateException
	 */
	public Object load(String entityName, Serializable id) throws HibernateException;

	/**
	 * Read the persistent state associated with the given identifier into the given transient
	 * instance.
	 *
	 * @param object an "empty" instance of the persistent class
	 * @param id a valid identifier of an existing persistent instance of the class
	 * @throws HibernateException
	 */
	public void load(Object object, Serializable id) throws HibernateException;

	/**
	 * Persist the state of the given detached instance, reusing the current
	 * identifier value.  This operation cascades to associated instances if
	 * the association is mapped with <tt>cascade="replicate"</tt>.
	 *
	 * @param object a detached instance of a persistent class
	 */
	public void replicate(Object object, ReplicationMode replicationMode) throws HibernateException;

	/**
	 * Persist the state of the given detached instance, reusing the current
	 * identifier value.  This operation cascades to associated instances if
	 * the association is mapped with <tt>cascade="replicate"</tt>.
	 *
	 * @param object a detached instance of a persistent class
	 */
	public void replicate(String entityName, Object object, ReplicationMode replicationMode) throws HibernateException;

	/**
	 * Persist the given transient instance, first assigning a generated identifier. (Or
	 * using the current value of the identifier property if the <tt>assigned</tt>
	 * generator is used.) This operation cascades to associated instances if the
	 * association is mapped with <tt>cascade="save-update"</tt>.
	 *
	 * @param object a transient instance of a persistent class
	 * @return the generated identifier
	 * @throws HibernateException
	 */
	public Serializable save(Object object) throws HibernateException;

	/**
	 * Persist the given transient instance, first assigning a generated identifier. (Or
	 * using the current value of the identifier property if the <tt>assigned</tt>
	 * generator is used.)  This operation cascades to associated instances if the
	 * association is mapped with <tt>cascade="save-update"</tt>.
	 *
	 * @param object a transient instance of a persistent class
	 * @return the generated identifier
	 * @throws HibernateException
	 */
	public Serializable save(String entityName, Object object) throws HibernateException;

	/**
	 * Either {@link #save(Object)} or {@link #update(Object)} the given
	 * instance, depending upon resolution of the unsaved-value checks (see the
	 * manual for discussion of unsaved-value checking).
	 * <p/>
	 * This operation cascades to associated instances if the association is mapped
	 * with <tt>cascade="save-update"</tt>.
	 *
	 * @see Session#save(java.lang.Object)
	 * @see Session#update(Object object)
	 * @param object a transient or detached instance containing new or updated state
	 * @throws HibernateException
	 */
	public void saveOrUpdate(Object object) throws HibernateException;

	/**
	 * Either {@link #save(String, Object)} or {@link #update(String, Object)}
	 * the given instance, depending upon resolution of the unsaved-value checks
	 * (see the manual for discussion of unsaved-value checking).
	 * <p/>
	 * This operation cascades to associated instances if the association is mapped
	 * with <tt>cascade="save-update"</tt>.
	 *
	 * @see Session#save(String,Object)
	 * @see Session#update(String,Object)
	 * @param object a transient or detached instance containing new or updated state
	 * @throws HibernateException
	 */
	public void saveOrUpdate(String entityName, Object object) throws HibernateException;

	/**
	 * Update the persistent instance with the identifier of the given detached
	 * instance. If there is a persistent instance with the same identifier,
	 * an exception is thrown. This operation cascades to associated instances
	 * if the association is mapped with <tt>cascade="save-update"</tt>.
	 *
	 * @param object a detached instance containing updated state
	 * @throws HibernateException
	 */
	public void update(Object object) throws HibernateException;

	/**
	 * Update the persistent instance with the identifier of the given detached
	 * instance. If there is a persistent instance with the same identifier,
	 * an exception is thrown. This operation cascades to associated instances
	 * if the association is mapped with <tt>cascade="save-update"</tt>.
	 *
	 * @param object a detached instance containing updated state
	 * @throws HibernateException
	 */
	public void update(String entityName, Object object) throws HibernateException;

	/**
	 * Copy the state of the given object onto the persistent object with the same
	 * identifier. If there is no persistent instance currently associated with
	 * the session, it will be loaded. Return the persistent instance. If the
	 * given instance is unsaved, save a copy of and return it as a newly persistent
	 * instance. The given instance does not become associated with the session.
	 * This operation cascades to associated instances if the association is mapped
	 * with <tt>cascade="merge"</tt>.<br>
	 * <br>
	 * The semantics of this method are defined by JSR-220.
	 *
	 * @param object a detached instance with state to be copied
	 * @return an updated persistent instance
	 */
	public Object merge(Object object) throws HibernateException;

	/**
	 * Copy the state of the given object onto the persistent object with the same
	 * identifier. If there is no persistent instance currently associated with
	 * the session, it will be loaded. Return the persistent instance. If the
	 * given instance is unsaved, save a copy of and return it as a newly persistent
	 * instance. The given instance does not become associated with the session.
	 * This operation cascades to associated instances if the association is mapped
	 * with <tt>cascade="merge"</tt>.<br>
	 * <br>
	 * The semantics of this method are defined by JSR-220.
	 *
	 * @param object a detached instance with state to be copied
	 * @return an updated persistent instance
	 */
	public Object merge(String entityName, Object object) throws HibernateException;

	/**
	 * Make a transient instance persistent. This operation cascades to associated
	 * instances if the association is mapped with <tt>cascade="persist"</tt>.<br>
	 * <br>
	 * The semantics of this method are defined by JSR-220.
	 *
	 * @param object a transient instance to be made persistent
	 */
	public void persist(Object object) throws HibernateException;
	/**
	 * Make a transient instance persistent. This operation cascades to associated
	 * instances if the association is mapped with <tt>cascade="persist"</tt>.<br>
	 * <br>
	 * The semantics of this method are defined by JSR-220.
	 *
	 * @param object a transient instance to be made persistent
	 */
	public void persist(String entityName, Object object) throws HibernateException;

	/**
	 * Remove a persistent instance from the datastore. The argument may be
	 * an instance associated with the receiving <tt>Session</tt> or a transient
	 * instance with an identifier associated with existing persistent state.
	 * This operation cascades to associated instances if the association is mapped
	 * with <tt>cascade="delete"</tt>.
	 *
	 * @param object the instance to be removed
	 * @throws HibernateException
	 */
	public void delete(Object object) throws HibernateException;

	/**
	 * Remove a persistent instance from the datastore. The <b>object</b> argument may be
	 * an instance associated with the receiving <tt>Session</tt> or a transient
	 * instance with an identifier associated with existing persistent state.
	 * This operation cascades to associated instances if the association is mapped
	 * with <tt>cascade="delete"</tt>.
	 *
	 * @param entityName The entity name for the instance to be removed.
	 * @param object the instance to be removed
	 * @throws HibernateException
	 */
	public void delete(String entityName, Object object) throws HibernateException;

	/**
	 * Obtain the specified lock level upon the given object. This may be used to
	 * perform a version check (<tt>LockMode.READ</tt>), to upgrade to a pessimistic
	 * lock (<tt>LockMode.UPGRADE</tt>), or to simply reassociate a transient instance
	 * with a session (<tt>LockMode.NONE</tt>). This operation cascades to associated
	 * instances if the association is mapped with <tt>cascade="lock"</tt>.
	 *
	 * @param object a persistent or transient instance
	 * @param lockMode the lock level
	 * @throws HibernateException
	 */
	public void lock(Object object, LockMode lockMode) throws HibernateException;

	/**
	 * Obtain the specified lock level upon the given object. This may be used to
	 * perform a version check (<tt>LockMode.READ</tt>), to upgrade to a pessimistic
	 * lock (<tt>LockMode.UPGRADE</tt>), or to simply reassociate a transient instance
	 * with a session (<tt>LockMode.NONE</tt>). This operation cascades to associated
	 * instances if the association is mapped with <tt>cascade="lock"</tt>.
	 *
	 * @param object a persistent or transient instance
	 * @param lockMode the lock level
	 * @throws HibernateException
	 */
	public void lock(String entityName, Object object, LockMode lockMode) throws HibernateException;

	/**
	 * Re-read the state of the given instance from the underlying database. It is
	 * inadvisable to use this to implement long-running sessions that span many
	 * business tasks. This method is, however, useful in certain special circumstances.
	 * For example
	 * <ul>
	 * <li>where a database trigger alters the object state upon insert or update
	 * <li>after executing direct SQL (eg. a mass update) in the same session
	 * <li>after inserting a <tt>Blob</tt> or <tt>Clob</tt>
	 * </ul>
	 *
	 * @param object a persistent or detached instance
	 * @throws HibernateException
	 */
	public void refresh(Object object) throws HibernateException;

	/**
	 * Re-read the state of the given instance from the underlying database, with
	 * the given <tt>LockMode</tt>. It is inadvisable to use this to implement
	 * long-running sessions that span many business tasks. This method is, however,
	 * useful in certain special circumstances.
	 *
	 * @param object a persistent or detached instance
	 * @param lockMode the lock mode to use
	 * @throws HibernateException
	 */
	public void refresh(Object object, LockMode lockMode) throws HibernateException;

	/**
	 * Determine the current lock mode of the given object.
	 *
	 * @param object a persistent instance
	 * @return the current lock mode
	 * @throws HibernateException
	 */
	public LockMode getCurrentLockMode(Object object) throws HibernateException;

	/**
	 * Begin a unit of work and return the associated <tt>Transaction</tt> object.
	 * If a new underlying transaction is required, begin the transaction. Otherwise
	 * continue the new work in the context of the existing underlying transaction.
	 * The class of the returned <tt>Transaction</tt> object is determined by the
	 * property <tt>hibernate.transaction_factory</tt>.
	 *
	 * @return a Transaction instance
	 * @throws HibernateException
	 * @see Transaction
	 */
	public Transaction beginTransaction() throws HibernateException;

	/**
	 * Get the <tt>Transaction</tt> instance associated with this session.
	 * The class of the returned <tt>Transaction</tt> object is determined by the
	 * property <tt>hibernate.transaction_factory</tt>.
	 *
	 * @return a Transaction instance
	 * @throws HibernateException
	 * @see Transaction
	 */
	public Transaction getTransaction();

	/**
	 * Create a new <tt>Criteria</tt> instance, for the given entity class,
	 * or a superclass of an entity class.
	 *
	 * @param persistentClass a class, which is persistent, or has persistent subclasses
	 * @return Criteria
	 */
	public Criteria createCriteria(Class persistentClass);

	/**
	 * Create a new <tt>Criteria</tt> instance, for the given entity class,
	 * or a superclass of an entity class, with the given alias.
	 *
	 * @param persistentClass a class, which is persistent, or has persistent subclasses
	 * @return Criteria
	 */
	public Criteria createCriteria(Class persistentClass, String alias);

	/**
	 * Create a new <tt>Criteria</tt> instance, for the given entity name.
	 *
	 * @param entityName
	 * @return Criteria
	 */
	public Criteria createCriteria(String entityName);

	/**
	 * Create a new <tt>Criteria</tt> instance, for the given entity name,
	 * with the given alias.
	 *
	 * @param entityName
	 * @return Criteria
	 */
	public Criteria createCriteria(String entityName, String alias);

	/**
	 * Create a new instance of <tt>Query</tt> for the given HQL query string.
	 *
	 * @param queryString a HQL query
	 * @return Query
	 * @throws HibernateException
	 */
	public Query createQuery(String queryString) throws HibernateException;

	/**
	 * Create a new instance of <tt>SQLQuery</tt> for the given SQL query string.
	 *
	 * @param queryString a SQL query
	 * @return SQLQuery
	 * @throws HibernateException
	 */
	public SQLQuery createSQLQuery(String queryString) throws HibernateException;

	/**
	 * Create a new instance of <tt>Query</tt> for the given collection and filter string.
	 *
	 * @param collection a persistent collection
	 * @param queryString a Hibernate query
	 * @return Query
	 * @throws HibernateException
	 */
	public Query createFilter(Object collection, String queryString) throws HibernateException;

	/**
	 * Obtain an instance of <tt>Query</tt> for a named query string defined in the
	 * mapping file.
	 *
	 * @param queryName the name of a query defined externally
	 * @return Query
	 * @throws HibernateException
	 */
	public Query getNamedQuery(String queryName) throws HibernateException;

	/**
	 * Completely clear the session. Evict all loaded instances and cancel all pending
	 * saves, updates and deletions. Do not close open iterators or instances of
	 * <tt>ScrollableResults</tt>.
	 */
	public void clear();

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * or null if there is no such persistent instance. (If the instance is already associated
	 * with the session, return that instance. This method never returns an uninitialized instance.)
	 * Obtain the specified lock mode if the instance exists.
	 *
	 * @param clazz a persistent class
	 * @param id an identifier
	 * @return a persistent instance or null
	 * @throws HibernateException
	 */
	public Object get(Class clazz, Serializable id) throws HibernateException;

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * or null if there is no such persistent instance. (If the instance is already associated
	 * with the session, return that instance. This method never returns an uninitialized instance.)
	 * Obtain the specified lock mode if the instance exists.
	 *
	 * @param clazz a persistent class
	 * @param id an identifier
	 * @param lockMode the lock mode
	 * @return a persistent instance or null
	 * @throws HibernateException
	 */
	public Object get(Class clazz, Serializable id, LockMode lockMode) throws HibernateException;

	/**
	 * Return the persistent instance of the given named entity with the given identifier,
	 * or null if there is no such persistent instance. (If the instance is already associated
	 * with the session, return that instance. This method never returns an uninitialized instance.)
	 *
	 * @param entityName the entity name
	 * @param id an identifier
	 * @return a persistent instance or null
	 * @throws HibernateException
	 */
	public Object get(String entityName, Serializable id) throws HibernateException;

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * or null if there is no such persistent instance. (If the instance is already associated
	 * with the session, return that instance. This method never returns an uninitialized instance.)
	 * Obtain the specified lock mode if the instance exists.
	 *
	 * @param entityName the entity name
	 * @param id an identifier
	 * @param lockMode the lock mode
	 * @return a persistent instance or null
	 * @throws HibernateException
	 */
	public Object get(String entityName, Serializable id, LockMode lockMode) throws HibernateException;

	
	/**
	 * Return the entity name for a persistent entity
	 *   
	 * @param object a persistent entity
	 * @return the entity name
	 * @throws HibernateException
	 */
	public String getEntityName(Object object) throws HibernateException;

	/**
	 * Enable the named filter for this current session.
	 *
	 * @param filterName The name of the filter to be enabled.
	 * @return The Filter instance representing the enabled fiter.
	 */
	public Filter enableFilter(String filterName);

	/**
	 * Retrieve a currently enabled filter by name.
	 *
	 * @param filterName The name of the filter to be retrieved.
	 * @return The Filter instance representing the enabled fiter.
	 */
	public Filter getEnabledFilter(String filterName);

	/**
	 * Disable the named filter for the current session.
	 *
	 * @param filterName The name of the filter to be disabled.
	 */
	public void disableFilter(String filterName);
	
	/**
	 * Get the statistics for this session.
	 */
	public SessionStatistics getStatistics();
	
	/**
	 * Set an unmodified persistent object to read only mode, or a read only
	 * object to modifiable mode. In read only mode, no snapshot is maintained
	 * and the instance is never dirty checked.
	 * 
	 * @see Query#setReadOnly(boolean)
	 */
	public void setReadOnly(Object entity, boolean readOnly);

	/**
	 * Controller for allowing users to perform JDBC related work using the Connection
	 * managed by this Session.
	 *
	 * @param work The work to be performed.
	 * @throws HibernateException Generally indicates wrapped {@link java.sql.SQLException}
	 */
	public void doWork(Work work) throws HibernateException;


	/**
	 * Disconnect the <tt>Session</tt> from the current JDBC connection. If
	 * the connection was obtained by Hibernate close it and return it to
	 * the connection pool; otherwise, return it to the application.
	 * <p/>
	 * This is used by applications which supply JDBC connections to Hibernate
	 * and which require long-sessions (or long-conversations)
	 * <p/>
	 * Note that disconnect() called on a session where the connection was
	 * retrieved by Hibernate through its configured
	 * {@link org.hibernate.connection.ConnectionProvider} has no effect,
	 * provided {@link ConnectionReleaseMode#ON_CLOSE} is not in effect.
	 *
	 * @return the application-supplied connection or <tt>null</tt>
	 * @see #reconnect(Connection)
	 * @see #reconnect()
	 */
	Connection disconnect() throws HibernateException;

	/**
	 * Obtain a new JDBC connection. This is used by applications which
	 * require long transactions and do not supply connections to the
	 * session.
	 *
	 * @see #disconnect()
	 * @deprecated Manual reconnection is only needed in the case of
	 * application-supplied connections, in which case the
	 * {@link #reconnect(java.sql.Connection)} for should be used.
	 */
	void reconnect() throws HibernateException;

	/**
	 * Reconnect to the given JDBC connection. This is used by applications
	 * which require long transactions and use application-supplied connections.
	 *
	 * @param connection a JDBC connection
	 * @see #disconnect()
	 */
	void reconnect(Connection connection) throws HibernateException;
}
