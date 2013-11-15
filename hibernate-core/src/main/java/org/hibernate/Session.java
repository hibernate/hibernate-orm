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
package org.hibernate;

import java.io.Serializable;
import java.sql.Connection;

import org.hibernate.jdbc.ReturningWork;
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
public interface Session extends SharedSessionContract {
	/**
	 * Obtain a {@link Session} builder with the ability to grab certain information from this session.
	 *
	 * @return The session builder
	 */
	public SharedSessionBuilder sessionWithOptions();

	/**
	 * Force this session to flush. Must be called at the end of a
	 * unit of work, before committing the transaction and closing the
	 * session (depending on {@link #setFlushMode(FlushMode)},
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
	 * Will entities and proxies that are loaded into this session be made 
	 * read-only by default?
	 *
	 * To determine the read-only/modifiable setting for a particular entity 
	 * or proxy:
	 * @see Session#isReadOnly(Object)
	 *
	 * @return true, loaded entities/proxies will be made read-only by default; 
	 *         false, loaded entities/proxies will be made modifiable by default. 
	 */
	public boolean isDefaultReadOnly();

	/**
	 * Change the default for entities and proxies loaded into this session
	 * from modifiable to read-only mode, or from modifiable to read-only mode.
	 *
	 * Read-only entities are not dirty-checked and snapshots of persistent
	 * state are not maintained. Read-only entities can be modified, but
	 * changes are not persisted.
	 *
	 * When a proxy is initialized, the loaded entity will have the same
	 * read-only/modifiable setting as the uninitialized
	 * proxy has, regardless of the session's current setting.
	 *
	 * To change the read-only/modifiable setting for a particular entity
	 * or proxy that is already in this session:
	 * @see Session#setReadOnly(Object,boolean)
	 *
	 * To override this session's read-only/modifiable setting for entities
	 * and proxies loaded by a Query:
	 * @see Query#setReadOnly(boolean)
	 *
	 * @param readOnly true, the default for loaded entities/proxies is read-only;
	 *                 false, the default for loaded entities/proxies is modifiable
	 */
	public void setDefaultReadOnly(boolean readOnly);

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
	public Serializable getIdentifier(Object object);

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
	 * @param object The entity to evict
	 *
	 * @throws NullPointerException if the passed object is {@code null}
	 * @throws IllegalArgumentException if the passed object is not defined as an entity
	 */
	public void evict(Object object);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * obtaining the specified lock mode, assuming the instance exists.
	 *
	 * @param theClass a persistent class
	 * @param id a valid identifier of an existing persistent instance of the class
	 * @param lockMode the lock level
	 *
	 * @return the persistent instance or proxy
	 *
	 * @deprecated LockMode parameter should be replaced with LockOptions
	 */
	@Deprecated
	public Object load(Class theClass, Serializable id, LockMode lockMode);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * obtaining the specified lock mode, assuming the instance exists.
	 *
	 * @param theClass a persistent class
	 * @param id a valid identifier of an existing persistent instance of the class
	 * @param lockOptions contains the lock level
	 * @return the persistent instance or proxy
	 */
	public Object load(Class theClass, Serializable id, LockOptions lockOptions);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * obtaining the specified lock mode, assuming the instance exists.
	 *
	 * @param entityName a persistent class
	 * @param id a valid identifier of an existing persistent instance of the class
	 * @param lockMode the lock level
	 *
	 * @return the persistent instance or proxy
	 *
	 * @deprecated LockMode parameter should be replaced with LockOptions
	 */
	@Deprecated
	public Object load(String entityName, Serializable id, LockMode lockMode);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * obtaining the specified lock mode, assuming the instance exists.
	 *
	 * @param entityName a persistent class
	 * @param id a valid identifier of an existing persistent instance of the class
	 * @param lockOptions contains the lock level
	 *
	 * @return the persistent instance or proxy
	 */
	public Object load(String entityName, Serializable id, LockOptions lockOptions);

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
	 *
	 * @return the persistent instance or proxy
	 */
	public Object load(Class theClass, Serializable id);

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
	 *
	 * @return the persistent instance or proxy
	 */
	public Object load(String entityName, Serializable id);

	/**
	 * Read the persistent state associated with the given identifier into the given transient
	 * instance.
	 *
	 * @param object an "empty" instance of the persistent class
	 * @param id a valid identifier of an existing persistent instance of the class
	 */
	public void load(Object object, Serializable id);

	/**
	 * Persist the state of the given detached instance, reusing the current
	 * identifier value.  This operation cascades to associated instances if
	 * the association is mapped with {@code cascade="replicate"}
	 *
	 * @param object a detached instance of a persistent class
	 * @param replicationMode The replication mode to use
	 */
	public void replicate(Object object, ReplicationMode replicationMode);

	/**
	 * Persist the state of the given detached instance, reusing the current
	 * identifier value.  This operation cascades to associated instances if
	 * the association is mapped with {@code cascade="replicate"}
	 *
	 * @param entityName The entity name
	 * @param object a detached instance of a persistent class
	 * @param replicationMode The replication mode to use
	 */
	public void replicate(String entityName, Object object, ReplicationMode replicationMode) ;

	/**
	 * Persist the given transient instance, first assigning a generated identifier. (Or
	 * using the current value of the identifier property if the <tt>assigned</tt>
	 * generator is used.) This operation cascades to associated instances if the
	 * association is mapped with {@code cascade="save-update"}
	 *
	 * @param object a transient instance of a persistent class
	 *
	 * @return the generated identifier
	 */
	public Serializable save(Object object);

	/**
	 * Persist the given transient instance, first assigning a generated identifier. (Or
	 * using the current value of the identifier property if the <tt>assigned</tt>
	 * generator is used.)  This operation cascades to associated instances if the
	 * association is mapped with {@code cascade="save-update"}
	 *
	 * @param entityName The entity name
	 * @param object a transient instance of a persistent class
	 *
	 * @return the generated identifier
	 */
	public Serializable save(String entityName, Object object);

	/**
	 * Either {@link #save(Object)} or {@link #update(Object)} the given
	 * instance, depending upon resolution of the unsaved-value checks (see the
	 * manual for discussion of unsaved-value checking).
	 * <p/>
	 * This operation cascades to associated instances if the association is mapped
	 * with {@code cascade="save-update"}
	 *
	 * @param object a transient or detached instance containing new or updated state
	 *
	 * @see Session#save(java.lang.Object)
	 * @see Session#update(Object object)
	 */
	public void saveOrUpdate(Object object);

	/**
	 * Either {@link #save(String, Object)} or {@link #update(String, Object)}
	 * the given instance, depending upon resolution of the unsaved-value checks
	 * (see the manual for discussion of unsaved-value checking).
	 * <p/>
	 * This operation cascades to associated instances if the association is mapped
	 * with {@code cascade="save-update"}
	 *
	 * @param entityName The entity name
	 * @param object a transient or detached instance containing new or updated state
	 *
	 * @see Session#save(String,Object)
	 * @see Session#update(String,Object)
	 */
	public void saveOrUpdate(String entityName, Object object);

	/**
	 * Update the persistent instance with the identifier of the given detached
	 * instance. If there is a persistent instance with the same identifier,
	 * an exception is thrown. This operation cascades to associated instances
	 * if the association is mapped with {@code cascade="save-update"}
	 *
	 * @param object a detached instance containing updated state
	 */
	public void update(Object object);

	/**
	 * Update the persistent instance with the identifier of the given detached
	 * instance. If there is a persistent instance with the same identifier,
	 * an exception is thrown. This operation cascades to associated instances
	 * if the association is mapped with {@code cascade="save-update"}
	 *
	 * @param entityName The entity name
	 * @param object a detached instance containing updated state
	 */
	public void update(String entityName, Object object);

	/**
	 * Copy the state of the given object onto the persistent object with the same
	 * identifier. If there is no persistent instance currently associated with
	 * the session, it will be loaded. Return the persistent instance. If the
	 * given instance is unsaved, save a copy of and return it as a newly persistent
	 * instance. The given instance does not become associated with the session.
	 * This operation cascades to associated instances if the association is mapped
	 * with {@code cascade="merge"}
	 * <p/>
	 * The semantics of this method are defined by JSR-220.
	 *
	 * @param object a detached instance with state to be copied
	 *
	 * @return an updated persistent instance
	 */
	public Object merge(Object object);

	/**
	 * Copy the state of the given object onto the persistent object with the same
	 * identifier. If there is no persistent instance currently associated with
	 * the session, it will be loaded. Return the persistent instance. If the
	 * given instance is unsaved, save a copy of and return it as a newly persistent
	 * instance. The given instance does not become associated with the session.
	 * This operation cascades to associated instances if the association is mapped
	 * with {@code cascade="merge"}
	 * <p/>
	 * The semantics of this method are defined by JSR-220.
	 *
	 * @param entityName The entity name
	 * @param object a detached instance with state to be copied
	 *
	 * @return an updated persistent instance
	 */
	public Object merge(String entityName, Object object);

	/**
	 * Make a transient instance persistent. This operation cascades to associated
	 * instances if the association is mapped with {@code cascade="persist"}
	 * <p/>
	 * The semantics of this method are defined by JSR-220.
	 *
	 * @param object a transient instance to be made persistent
	 */
	public void persist(Object object);
	/**
	 * Make a transient instance persistent. This operation cascades to associated
	 * instances if the association is mapped with {@code cascade="persist"}
	 * <p/>
	 * The semantics of this method are defined by JSR-220.
	 *
	 * @param entityName The entity name
	 * @param object a transient instance to be made persistent
	 */
	public void persist(String entityName, Object object);

	/**
	 * Remove a persistent instance from the datastore. The argument may be
	 * an instance associated with the receiving <tt>Session</tt> or a transient
	 * instance with an identifier associated with existing persistent state.
	 * This operation cascades to associated instances if the association is mapped
	 * with {@code cascade="delete"}
	 *
	 * @param object the instance to be removed
	 */
	public void delete(Object object);

	/**
	 * Remove a persistent instance from the datastore. The <b>object</b> argument may be
	 * an instance associated with the receiving <tt>Session</tt> or a transient
	 * instance with an identifier associated with existing persistent state.
	 * This operation cascades to associated instances if the association is mapped
	 * with {@code cascade="delete"}
	 *
	 * @param entityName The entity name for the instance to be removed.
	 * @param object the instance to be removed
	 */
	public void delete(String entityName, Object object);

	/**
	 * Obtain the specified lock level upon the given object. This may be used to
	 * perform a version check (<tt>LockMode.READ</tt>), to upgrade to a pessimistic
	 * lock (<tt>LockMode.PESSIMISTIC_WRITE</tt>), or to simply reassociate a transient instance
	 * with a session (<tt>LockMode.NONE</tt>). This operation cascades to associated
	 * instances if the association is mapped with <tt>cascade="lock"</tt>.
	 *
	 * @param object a persistent or transient instance
	 * @param lockMode the lock level
	 *
	 * @deprecated instead call buildLockRequest(LockMode).lock(object)
	 */
	@Deprecated
	public void lock(Object object, LockMode lockMode);

	/**
	 * Obtain the specified lock level upon the given object. This may be used to
	 * perform a version check (<tt>LockMode.OPTIMISTIC</tt>), to upgrade to a pessimistic
	 * lock (<tt>LockMode.PESSIMISTIC_WRITE</tt>), or to simply reassociate a transient instance
	 * with a session (<tt>LockMode.NONE</tt>). This operation cascades to associated
	 * instances if the association is mapped with <tt>cascade="lock"</tt>.
	 *
	 * @param entityName The name of the entity
	 * @param object a persistent or transient instance
	 * @param lockMode the lock level
	 *
	 * @deprecated instead call buildLockRequest(LockMode).lock(entityName, object)
	 */
	@SuppressWarnings( {"JavaDoc"})
	@Deprecated
	public void lock(String entityName, Object object, LockMode lockMode);

	/**
	 * Build a LockRequest that specifies the LockMode, pessimistic lock timeout and lock scope.
	 * timeout and scope is ignored for optimistic locking.  After building the LockRequest,
	 * call LockRequest.lock to perform the requested locking. 
	 * <p/>
	 * Example usage:
	 * {@code session.buildLockRequest().setLockMode(LockMode.PESSIMISTIC_WRITE).setTimeOut(60000).lock(entity);}
	 *
	 * @param lockOptions contains the lock level
	 *
	 * @return a lockRequest that can be used to lock the passed object.
	 */
	public LockRequest buildLockRequest(LockOptions lockOptions);

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
	 */
	public void refresh(Object object);

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
	 * @param entityName a persistent class
	 * @param object a persistent or detached instance
	 */
	public void refresh(String entityName, Object object);

	/**
	 * Re-read the state of the given instance from the underlying database, with
	 * the given <tt>LockMode</tt>. It is inadvisable to use this to implement
	 * long-running sessions that span many business tasks. This method is, however,
	 * useful in certain special circumstances.
	 *
	 * @param object a persistent or detached instance
	 * @param lockMode the lock mode to use
	 *
	 * @deprecated LockMode parameter should be replaced with LockOptions
	 */
	@Deprecated
	public void refresh(Object object, LockMode lockMode);

	/**
	 * Re-read the state of the given instance from the underlying database, with
	 * the given <tt>LockMode</tt>. It is inadvisable to use this to implement
	 * long-running sessions that span many business tasks. This method is, however,
	 * useful in certain special circumstances.
	 *
	 * @param object a persistent or detached instance
	 * @param lockOptions contains the lock mode to use
	 */
	public void refresh(Object object, LockOptions lockOptions);

	/**
	 * Re-read the state of the given instance from the underlying database, with
	 * the given <tt>LockMode</tt>. It is inadvisable to use this to implement
	 * long-running sessions that span many business tasks. This method is, however,
	 * useful in certain special circumstances.
	 *
	 * @param entityName a persistent class
	 * @param object a persistent or detached instance
	 * @param lockOptions contains the lock mode to use
	 */
	public void refresh(String entityName, Object object, LockOptions lockOptions);

	/**
	 * Determine the current lock mode of the given object.
	 *
	 * @param object a persistent instance
	 *
	 * @return the current lock mode
	 */
	public LockMode getCurrentLockMode(Object object);

	/**
	 * Create a {@link Query} instance for the given collection and filter string.  Contains an implicit {@code FROM}
	 * element named {@code this} which refers to the defined table for the collection elements, as well as an implicit
	 * {@code WHERE} restriction for this particular collection instance's key value.
	 *
	 * @param collection a persistent collection
	 * @param queryString a Hibernate query fragment.
	 *
	 * @return The query instance for manipulation and execution
	 */
	public Query createFilter(Object collection, String queryString);

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
	 *
	 * @param clazz a persistent class
	 * @param id an identifier
	 *
	 * @return a persistent instance or null
	 */
	public Object get(Class clazz, Serializable id);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * or null if there is no such persistent instance. (If the instance is already associated
	 * with the session, return that instance. This method never returns an uninitialized instance.)
	 * Obtain the specified lock mode if the instance exists.
	 *
	 * @param clazz a persistent class
	 * @param id an identifier
	 * @param lockMode the lock mode
	 *
	 * @return a persistent instance or null
	 *
	 * @deprecated LockMode parameter should be replaced with LockOptions
	 */
	@Deprecated
	public Object get(Class clazz, Serializable id, LockMode lockMode);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * or null if there is no such persistent instance. (If the instance is already associated
	 * with the session, return that instance. This method never returns an uninitialized instance.)
	 * Obtain the specified lock mode if the instance exists.
	 *
	 * @param clazz a persistent class
	 * @param id an identifier
	 * @param lockOptions the lock mode
	 *
	 * @return a persistent instance or null
	 */
	public Object get(Class clazz, Serializable id, LockOptions lockOptions);

	/**
	 * Return the persistent instance of the given named entity with the given identifier,
	 * or null if there is no such persistent instance. (If the instance is already associated
	 * with the session, return that instance. This method never returns an uninitialized instance.)
	 *
	 * @param entityName the entity name
	 * @param id an identifier
	 *
	 * @return a persistent instance or null
	 */
	public Object get(String entityName, Serializable id);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * or null if there is no such persistent instance. (If the instance is already associated
	 * with the session, return that instance. This method never returns an uninitialized instance.)
	 * Obtain the specified lock mode if the instance exists.
	 *
	 * @param entityName the entity name
	 * @param id an identifier
	 * @param lockMode the lock mode
	 *
	 * @return a persistent instance or null
	 *
	 * @deprecated LockMode parameter should be replaced with LockOptions
	 */
	@Deprecated
	public Object get(String entityName, Serializable id, LockMode lockMode);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * or null if there is no such persistent instance. (If the instance is already associated
	 * with the session, return that instance. This method never returns an uninitialized instance.)
	 * Obtain the specified lock mode if the instance exists.
	 *
	 * @param entityName the entity name
	 * @param id an identifier
	 * @param lockOptions contains the lock mode
	 *
	 * @return a persistent instance or null
	 */
	public Object get(String entityName, Serializable id, LockOptions lockOptions);

	/**
	 * Return the entity name for a persistent entity.
	 *   
	 * @param object a persistent entity
	 *
	 * @return the entity name
	 */
	public String getEntityName(Object object);
	
	/**
	 * Create an {@link IdentifierLoadAccess} instance to retrieve the specified entity type by
	 * primary key.
	 * 
	 * @param entityName The entity name of the entity type to be retrieved
	 *
	 * @return load delegate for loading the specified entity type by primary key
	 *
	 * @throws HibernateException If the specified entity name cannot be resolved as an entity name
	 */
	public IdentifierLoadAccess byId(String entityName);

	/**
	 * Create an {@link IdentifierLoadAccess} instance to retrieve the specified entity by
	 * primary key.
	 *
	 * @param entityClass The entity type to be retrieved
	 *
	 * @return load delegate for loading the specified entity type by primary key
	 *
	 * @throws HibernateException If the specified Class cannot be resolved as a mapped entity
	 */
	public IdentifierLoadAccess byId(Class entityClass);

	/**
	 * Create an {@link NaturalIdLoadAccess} instance to retrieve the specified entity by
	 * its natural id.
	 * 
	 * @param entityName The entity name of the entity type to be retrieved
	 *
	 * @return load delegate for loading the specified entity type by natural id
	 *
	 * @throws HibernateException If the specified entity name cannot be resolved as an entity name
	 */
	public NaturalIdLoadAccess byNaturalId(String entityName);

	/**
	 * Create an {@link NaturalIdLoadAccess} instance to retrieve the specified entity by
	 * its natural id.
	 * 
	 * @param entityClass The entity type to be retrieved
	 *
	 * @return load delegate for loading the specified entity type by natural id
	 *
	 * @throws HibernateException If the specified Class cannot be resolved as a mapped entity
	 */
	public NaturalIdLoadAccess byNaturalId(Class entityClass);

	/**
	 * Create an {@link SimpleNaturalIdLoadAccess} instance to retrieve the specified entity by
	 * its natural id.
	 *
	 * @param entityName The entity name of the entity type to be retrieved
	 *
	 * @return load delegate for loading the specified entity type by natural id
	 *
	 * @throws HibernateException If the specified entityClass cannot be resolved as a mapped entity, or if the
	 * entity does not define a natural-id or if its natural-id is made up of multiple attributes.
	 */
	public SimpleNaturalIdLoadAccess bySimpleNaturalId(String entityName);

	/**
	 * Create an {@link SimpleNaturalIdLoadAccess} instance to retrieve the specified entity by
	 * its simple (single attribute) natural id.
	 *
	 * @param entityClass The entity type to be retrieved
	 *
	 * @return load delegate for loading the specified entity type by natural id
	 *
	 * @throws HibernateException If the specified entityClass cannot be resolved as a mapped entity, or if the
	 * entity does not define a natural-id or if its natural-id is made up of multiple attributes.
	 */
	public SimpleNaturalIdLoadAccess bySimpleNaturalId(Class entityClass);

	/**
	 * Enable the named filter for this current session.
	 *
	 * @param filterName The name of the filter to be enabled.
	 *
	 * @return The Filter instance representing the enabled filter.
	 */
	public Filter enableFilter(String filterName);

	/**
	 * Retrieve a currently enabled filter by name.
	 *
	 * @param filterName The name of the filter to be retrieved.
	 *
	 * @return The Filter instance representing the enabled filter.
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
	 *
	 * @return The session statistics being collected for this session
	 */
	public SessionStatistics getStatistics();

	/**
	 * Is the specified entity or proxy read-only?
	 *
	 * To get the default read-only/modifiable setting used for
	 * entities and proxies that are loaded into the session:
	 * @see org.hibernate.Session#isDefaultReadOnly()
	 *
	 * @param entityOrProxy an entity or HibernateProxy
	 * @return {@code true} if the entity or proxy is read-only, {@code false} if the entity or proxy is modifiable.
	 */
	public boolean isReadOnly(Object entityOrProxy);

	/**
	 * Set an unmodified persistent object to read-only mode, or a read-only
	 * object to modifiable mode. In read-only mode, no snapshot is maintained,
	 * the instance is never dirty checked, and changes are not persisted.
	 *
	 * If the entity or proxy already has the specified read-only/modifiable
	 * setting, then this method does nothing.
	 * 
	 * To set the default read-only/modifiable setting used for
	 * entities and proxies that are loaded into the session:
	 * @see org.hibernate.Session#setDefaultReadOnly(boolean)
	 *
	 * To override this session's read-only/modifiable setting for entities
	 * and proxies loaded by a Query:
	 * @see Query#setReadOnly(boolean)
	 * 
	 * @param entityOrProxy an entity or HibernateProxy
	 * @param readOnly {@code true} if the entity or proxy should be made read-only; {@code false} if the entity or
	 * proxy should be made modifiable
	 */
	public void setReadOnly(Object entityOrProxy, boolean readOnly);

	/**
	 * Controller for allowing users to perform JDBC related work using the Connection managed by this Session.
	 *
	 * @param work The work to be performed.
	 * @throws HibernateException Generally indicates wrapped {@link java.sql.SQLException}
	 */
	public void doWork(Work work) throws HibernateException;

	/**
	 * Controller for allowing users to perform JDBC related work using the Connection managed by this Session.  After
	 * execution returns the result of the {@link ReturningWork#execute} call.
	 *
	 * @param work The work to be performed.
	 * @param <T> The type of the result returned from the work
	 *
	 * @return the result from calling {@link ReturningWork#execute}.
	 *
	 * @throws HibernateException Generally indicates wrapped {@link java.sql.SQLException}
	 */
	public <T> T doReturningWork(ReturningWork<T> work) throws HibernateException;

	/**
	 * Disconnect the session from its underlying JDBC connection.  This is intended for use in cases where the
	 * application has supplied the JDBC connection to the session and which require long-sessions (aka, conversations).
	 * <p/>
	 * It is considered an error to call this method on a session which was not opened by supplying the JDBC connection
	 * and an exception will be thrown.
	 * <p/>
	 * For non-user-supplied scenarios, normal transaction management already handles disconnection and reconnection
	 * automatically.
	 *
	 * @return the application-supplied connection or {@code null}
	 *
	 * @see #reconnect(Connection)
	 */
	Connection disconnect();

	/**
	 * Reconnect to the given JDBC connection.
	 *
	 * @param connection a JDBC connection
	 * 
	 * @see #disconnect()
	 */
	void reconnect(Connection connection);

	/**
	 * Is a particular fetch profile enabled on this session?
	 *
	 * @param name The name of the profile to be checked.
	 * @return True if fetch profile is enabled; false if not.
	 * @throws UnknownProfileException Indicates that the given name does not
	 * match any known profile names
	 *
	 * @see org.hibernate.engine.profile.FetchProfile for discussion of this feature
	 */
	public boolean isFetchProfileEnabled(String name) throws UnknownProfileException;

	/**
	 * Enable a particular fetch profile on this session.  No-op if requested
	 * profile is already enabled.
	 *
	 * @param name The name of the fetch profile to be enabled.
	 * @throws UnknownProfileException Indicates that the given name does not
	 * match any known profile names
	 *
	 * @see org.hibernate.engine.profile.FetchProfile for discussion of this feature
	 */
	public void enableFetchProfile(String name) throws UnknownProfileException;

	/**
	 * Disable a particular fetch profile on this session.  No-op if requested
	 * profile is already disabled.
	 *
	 * @param name The name of the fetch profile to be disabled.
	 * @throws UnknownProfileException Indicates that the given name does not
	 * match any known profile names
	 *
	 * @see org.hibernate.engine.profile.FetchProfile for discussion of this feature
	 */
	public void disableFetchProfile(String name) throws UnknownProfileException;

	/**
	 * Convenience access to the {@link TypeHelper} associated with this session's {@link SessionFactory}.
	 * <p/>
	 * Equivalent to calling {@link #getSessionFactory()}.{@link SessionFactory#getTypeHelper getTypeHelper()}
	 *
	 * @return The {@link TypeHelper} associated with this session's {@link SessionFactory}
	 */
	public TypeHelper getTypeHelper();

	/**
	 * Retrieve this session's helper/delegate for creating LOB instances.
	 *
	 * @return This session's LOB helper
	 */
	public LobHelper getLobHelper();

	/**
	 * Contains locking details (LockMode, Timeout and Scope).
	 */
	public interface LockRequest {
		/**
		 * Constant usable as a time out value that indicates no wait semantics should be used in
		 * attempting to acquire locks.
		 */
		static final int PESSIMISTIC_NO_WAIT = 0;
		/**
		 * Constant usable as a time out value that indicates that attempting to acquire locks should be allowed to
		 * wait forever (apply no timeout).
		 */
		static final int PESSIMISTIC_WAIT_FOREVER = -1;

		/**
		 * Get the lock mode.
		 *
		 * @return the lock mode.
		 */
		LockMode getLockMode();

		/**
		 * Specify the LockMode to be used.  The default is LockMode.none.
		 *
		 * @param lockMode The lock mode to use for this request
		 *
		 * @return this LockRequest instance for operation chaining.
		 */
		LockRequest setLockMode(LockMode lockMode);

		/**
		 * Get the timeout setting.
		 *
		 * @return timeout in milliseconds, -1 for indefinite wait and 0 for no wait.
		 */
		int getTimeOut();

		/**
		 * Specify the pessimistic lock timeout (check if your dialect supports this option).
		 * The default pessimistic lock behavior is to wait forever for the lock.
		 *
		 * @param timeout is time in milliseconds to wait for lock.  -1 means wait forever and 0 means no wait.
		 *
		 * @return this LockRequest instance for operation chaining.
		 */
		LockRequest setTimeOut(int timeout);

		/**
		 * Check if locking is cascaded to owned collections and relationships.
		 *
		 * @return true if locking will be extended to owned collections and relationships.
		 */
		boolean getScope();

		/**
		 * Specify if LockMode should be cascaded to owned collections and relationships.
		 * The association must be mapped with {@code cascade="lock"} for scope=true to work.
		 *
		 * @param scope {@code true} to cascade locks; {@code false} to not.
		 *
		 * @return {@code this}, for method chaining
		 */
		LockRequest setScope(boolean scope);

		/**
		 * Perform the requested locking.
		 *
		 * @param entityName The name of the entity to lock
		 * @param object The instance of the entity to lock
		 */
		void lock(String entityName, Object object);

		/**
		 * Perform the requested locking.
		 *
		 * @param object The instance of the entity to lock
		 */
		void lock(Object object);
	}

	/**
	 * Add one or more listeners to the Session
	 *
	 * @param listeners The listener(s) to add
	 */
	public void addEventListeners(SessionEventListener... listeners);
}
