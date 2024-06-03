/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PessimisticLockScope;
import org.hibernate.graph.RootGraph;
import org.hibernate.jdbc.Work;
import org.hibernate.query.Query;
import org.hibernate.stat.SessionStatistics;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;

/**
 * The main runtime interface between a Java application and Hibernate. Represents the
 * notion of a <em>persistence context</em>, a set of managed entity instances associated
 * with a logical transaction.
 * <p>
 * The lifecycle of a {@code Session} is bounded by the beginning and end of the logical
 * transaction. But a long logical transaction might span several database transactions.
 * <p>
 * The primary purpose of the {@code Session} is to offer create, read, and delete
 * operations for instances of mapped entity classes. An instance may be in one of three
 * states with respect to a given open session:
 * <ul>
 * <li><em>transient:</em> never persistent, and not associated with the {@code Session},
 * <li><em>persistent:</em> currently associated with the {@code Session}, or
 * <li><em>detached:</em> previously persistent, but not currently associated with the
 *     {@code Session}.
 * </ul>
 * <p>
 * At any given time, an instance may be associated with at most one open session.
 * <p>
 * Any instance returned by {@link #get(Class, Object)}, {@link #find(Class, Object)},
 * or by a query is persistent. A persistent instance might hold references to other
 * entity instances, and sometimes these references are <em>proxied</em> by an
 * intermediate object. When an associated entity has not yet been fetched from the
 * database, references to the unfetched entity are represented by uninitialized
 * proxies. The state of an unfetched entity is automatically fetched from the
 * database when a method of its proxy is invoked, if and only if the proxy is
 * associated with an open session. Otherwise, {@link #getReference(Object)} may be
 * used to trade a proxy belonging to a closed session for a new proxy associated
 * with the current session.
 * <p>
 * A transient instance may be made persistent by calling {@link #persist(Object)}.
 * A persistent instance may be made detached by calling {@link #detach(Object)}.
 * A persistent instance may be marked for removal, and eventually made transient, by
 * calling {@link #remove(Object)}.
 * <p>
 * Persistent instances are held in a managed state by the persistence context. Any
 * change to the state of a persistent instance is automatically detected and eventually
 * flushed to the database. This process of automatic change detection is called
 * <em>dirty checking</em> and can be expensive in some circumstances. Dirty checking
 * may be disabled by marking an entity as read-only using
 * {@link #setReadOnly(Object, boolean)} or simply by {@linkplain #detach(Object) evicting}
 * it from the persistence context. A session may be set to load entities as read-only
 * {@linkplain #setDefaultReadOnly(boolean) by default}, or this may be controlled at the
 * {@linkplain Query#setReadOnly(boolean) query level}.
 * <p>
 * The state of a transient or detached instance may be made persistent by copying it to
 * a persistent instance using {@link #merge(Object)}. All older operations which moved a
 * detached instance to the persistent state are now deprecated, and clients should now
 * migrate to the use of {@code merge()}.
 * <p>
 * The persistent state of a managed entity may be refreshed from the database, discarding
 * all modifications to the object held in memory, by calling {@link #refresh(Object)}.
 * <p>
 * From {@linkplain FlushMode time to time}, a {@linkplain #flush() flush operation} is
 * triggered, and the session synchronizes state held in memory with persistent state
 * held in the database by executing SQL {@code insert}, {@code update}, and {@code delete}
 * statements. Note that SQL statements are often not executed synchronously by the methods
 * of the {@code Session} interface. If synchronous execution of SQL is desired, the
 * {@link StatelessSession} allows this.
 * <p>
 * Each managed instance has an associated {@link LockMode}. By default, the session
 * obtains only {@link LockMode#READ} on an entity instance it reads from the database
 * and {@link LockMode#WRITE} on an entity instance it writes to the database. This
 * behavior is appropriate for programs which use optimistic locking.
 * <ul>
 * <li>A different lock level may be obtained by explicitly specifying the mode using
 *     {@link #get(Class, Object, LockMode)}, {@link #find(Class, Object, LockModeType)},
 *     {@link #refresh(Object, LockMode)}, {@link #refresh(Object, LockModeType)}, or
 *     {@link org.hibernate.query.SelectionQuery#setLockMode(LockModeType)}.
 * <li>The lock level of a managed instance already held by the session may be upgraded
 *     to a more restrictive lock level by calling {@link #lock(Object, LockMode)} or
 *     {@link #lock(Object, LockModeType)}.
 * </ul>
 * <p>
 * A persistence context holds hard references to all its entities and prevents them
 * from being garbage collected. Therefore, a {@code Session} is a short-lived object,
 * and must be discarded as soon as a logical transaction ends. In extreme cases,
 * {@link #clear()} and {@link #detach(Object)} may be used to control memory usage.
 * However, for processes which read many entities, a {@link StatelessSession} should
 * be used.
 * <p>
 * A session might be associated with a container-managed JTA transaction, or it might be
 * in control of its own <em>resource-local</em> database transaction. In the case of a
 * resource-local transaction, the client must demarcate the beginning and end of the
 * transaction using a {@link Transaction}. A typical resource-local transaction should
 * use the following idiom:
 * <pre>
 * Session session = factory.openSession();
 * Transaction tx = null;
 * try {
 *     tx = session.beginTransaction();
 *     //do some work
 *     ...
 *     tx.commit();
 * }
 * catch (Exception e) {
 *     if (tx!=null) tx.rollback();
 *     throw e;
 * }
 * finally {
 *     session.close();
 * }
 * </pre>
 * <p>
 * It's crucially important to appreciate the following restrictions and why they exist:
 * <ul>
 * <li>If the {@code Session} throws an exception, the current transaction must be rolled
 *     back and the session must be discarded. The internal state of the {@code Session}
 *     cannot be expected to be consistent with the database after the exception occurs.
 * <li>At the end of a logical transaction, the session must be explicitly {@linkplain
 *     #close() destroyed}, so that all JDBC resources may be released.
 * <li>A {@code Session} is never thread-safe. It contains various different sorts of
 *     fragile mutable state. Each thread or transaction must obtain its own dedicated
 *     instance from the {@link SessionFactory}.
 * </ul>
 * <p>
 * An easy way to be sure that session and transaction management is being done correctly
 * is to {@linkplain SessionFactory#inTransaction(Consumer) let the factory do it}:
 * <pre>
 * sessionFactory.inTransaction(session -&gt; {
 *     //do the work
 *     ...
 * });
 * </pre>
 * <p>
 * A session may be used to {@linkplain #doWork(Work) execute JDBC work} using its JDBC
 * connection and transaction:
 * <pre>
 * session.doWork(connection -&gt; {
 *     try ( PreparedStatement ps = connection.prepareStatement( " ... " ) ) {
 *         ps.execute();
 *     }
 * });
 * </pre>
 * <p>
 * A {@code Session} instance is serializable if its entities are serializable.
 * <p>
 * Every {@code Session} is a JPA {@link EntityManager}. Furthermore, when Hibernate is
 * acting as the JPA persistence provider, the method {@link EntityManager#unwrap(Class)}
 * may be used to obtain the underlying {@code Session}.
 * <p>
 * Hibernate, unlike JPA, allows a persistence unit where an entity class is mapped multiple
 * times, with different entity names, usually to different tables. In this case, the session
 * needs a way to identify the entity name of a given instance of the entity class. Therefore,
 * some operations of this interface, including operations inherited from {@code EntityManager},
 * are overloaded with a form that accepts an explicit entity name along with the instance. An
 * alternative solution to this problem is to provide an {@link EntityNameResolver}.
 *
 * @see SessionFactory
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface Session extends SharedSessionContract, EntityManager {

	/**
	 * Force this session to flush. Must be called at the end of a unit of work,
	 * before the transaction is committed. Depending on the current
	 * {@linkplain #setHibernateFlushMode(FlushMode) flush mode}, the session might
	 * automatically flush when {@link Transaction#commit()} is called, and it is not
	 * necessary to call this method directly.
	 * <p>
	 * <em>Flushing</em> is the process of synchronizing the underlying persistent
	 * store with persistable state held in memory.
	 *
	 * @throws HibernateException if changes could not be synchronized with the database
	 */
	void flush();

	/**
	 * Set the current {@link FlushModeType JPA flush mode} for this session.
	 * <p>
	 * <em>Flushing</em> is the process of synchronizing the underlying persistent
	 * store with persistable state held in memory. The current flush mode determines
	 * when the session is automatically flushed.
	 *
	 * @param flushMode the new {@link FlushModeType}
	 *
	 * @see #setHibernateFlushMode(FlushMode) for additional options
	 */
	@Override
	void setFlushMode(FlushModeType flushMode);

	/**
	 * Set the current {@linkplain FlushMode flush mode} for this session.
	 * <p>
	 * <em>Flushing</em> is the process of synchronizing the underlying persistent
	 * store with persistable state held in memory. The current flush mode determines
	 * when the session is automatically flushed.
	 * <p>
	 * The {@linkplain FlushMode#AUTO default flush mode} is sometimes unnecessarily
	 * aggressive. For a logically "read only" session, it's reasonable to set the
	 * session's flush mode to {@link FlushMode#MANUAL} at the start of the session
	 * in order to avoid some unnecessary work.
	 * <p>
	 * Note that {@link FlushMode} defines more options than {@link FlushModeType}.
	 *
	 * @param flushMode the new {@link FlushMode}
	 */
	void setHibernateFlushMode(FlushMode flushMode);

	/**
	 * Get the current {@linkplain FlushModeType JPA flush mode} for this session.
	 *
	 * @return the {@link FlushModeType} currently in effect
	 */
	@Override
	FlushModeType getFlushMode();

	/**
	 * Get the current {@linkplain FlushMode flush mode} for this session.
	 *
	 * @return the {@link FlushMode} currently in effect
	 */
	FlushMode getHibernateFlushMode();

	/**
	 * Set the current {@linkplain CacheMode cache mode} for this session.
	 * <p>
	 * The cache mode determines the manner in which this session can interact with
	 * the second level cache.
	 *
	 * @param cacheMode the new cache mode
	 */
	void setCacheMode(CacheMode cacheMode);

	/**
	 * Get the current {@linkplain CacheMode cache mode} for this session.
	 *
	 * @return the current cache mode
	 */
	CacheMode getCacheMode();

	/**
	 * The JPA-defined {@link CacheStoreMode}.
	 *
	 * @see #getCacheMode()
	 *
	 * @since 6.2
	 */
	CacheStoreMode getCacheStoreMode();

	/**
	 * The JPA-defined {@link CacheRetrieveMode}.
	 *
	 * @see #getCacheMode()
	 *
	 * @since 6.2
	 */
	CacheRetrieveMode getCacheRetrieveMode();

	/**
	 * Enable or disable writes to the second-level cache.
	 *
	 * @param cacheStoreMode a JPA-defined {@link CacheStoreMode}
	 *
	 * @see #setCacheMode(CacheMode)
	 *
	 * @since 6.2
	 */
	void setCacheStoreMode(CacheStoreMode cacheStoreMode);

	/**
	 * Enable or disable reads from the second-level cache.
	 *
	 * @param cacheRetrieveMode a JPA-defined {@link CacheRetrieveMode}
	 *
	 * @see #setCacheMode(CacheMode)
	 *
	 * @since 6.2
	 */
	void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode);

	/**
	 * Get the maximum batch size for batch fetching associations by
	 * id in this session.
	 *
	 * @since 6.3
	 */
	int getFetchBatchSize();

	/**
	 * Set the maximum batch size for batch fetching associations by
	 * id in this session. Override the
	 * {@linkplain org.hibernate.boot.spi.SessionFactoryOptions#getDefaultBatchFetchSize()
	 * factory-level} default controlled by the configuration property
	 * {@value org.hibernate.cfg.AvailableSettings#DEFAULT_BATCH_FETCH_SIZE}.
	 * <p>
	 * <ul>
	 * <li>If {@code batchSize>1}, then batch fetching is enabled.
	 * <li>If {@code batchSize<0}, the batch size is inherited from
	 *     the factory-level setting.
	 * <li>Otherwise, batch fetching is disabled.
	 * </ul>
	 *
	 * @param batchSize the maximum batch size for batch fetching
	 *
	 * @since 6.3
	 *
	 * @see org.hibernate.cfg.AvailableSettings#DEFAULT_BATCH_FETCH_SIZE
	 */
	void setFetchBatchSize(int batchSize);

	/**
	 * Determine if subselect fetching is enabled in this session.
	 *
	 * @return {@code true} is subselect fetching is enabled
	 *
	 * @since 6.3
	 */
	boolean isSubselectFetchingEnabled();

	/**
	 * Enable or disable subselect fetching in this session. Override the
	 * {@linkplain org.hibernate.boot.spi.SessionFactoryOptions#isSubselectFetchEnabled()
	 * factory-level} default controlled by the configuration property
	 * {@value org.hibernate.cfg.AvailableSettings#USE_SUBSELECT_FETCH}.
	 *
	 * @param enabled {@code true} to enable subselect fetching
	 *
	 * @since 6.3
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_SUBSELECT_FETCH
	 */
	void setSubselectFetchingEnabled(boolean enabled);

	/**
	 * Get the session factory which created this session.
	 *
	 * @return the session factory
	 *
	 * @see SessionFactory
	 */
	SessionFactory getSessionFactory();

	/**
	 * Cancel the execution of the current query.
	 * <p>
	 * This is the sole method on session which may be safely called from
	 * another thread.
	 *
	 * @throws HibernateException if there was a problem cancelling the query
	 */
	void cancelQuery();

	/**
	 * Does this session contain any changes which must be synchronized with
	 * the database?  In other words, would any DML operations be executed if
	 * we flushed this session?
	 *
	 * @return {@code true} if the session contains pending changes; {@code false} otherwise.
	 * @throws HibernateException could not perform dirtying checking
	 */
	boolean isDirty();

	/**
	 * Will entities and proxies that are loaded into this session be made 
	 * read-only by default?
	 * <p>
	 * To determine the read-only/modifiable setting for a particular entity 
	 * or proxy use {@link #isReadOnly(Object)}.
	 *
	 * @see #isReadOnly(Object)
	 *
	 * @return {@code true}, loaded entities/proxies will be made read-only by default;
	 *		 {@code false}, loaded entities/proxies will be made modifiable by default.
	 */
	boolean isDefaultReadOnly();

	/**
	 * Change the default for entities and proxies loaded into this session
	 * from modifiable to read-only mode, or from modifiable to read-only mode.
	 * <p>
	 * Read-only entities are not dirty-checked and snapshots of persistent
	 * state are not maintained. Read-only entities can be modified, but
	 * changes are not persisted.
	 * <p>
	 * When a proxy is initialized, the loaded entity will have the same
	 * read-only/modifiable setting as the uninitialized
	 * proxy has, regardless of the session's current setting.
	 * <p>
	 * To change the read-only/modifiable setting for a particular entity
	 * or proxy that already belongs to this session use
	 * {@link #setReadOnly(Object, boolean)}.
	 * <p>
	 * To override this session's read-only/modifiable setting for all
	 * entities and proxies loaded by a certain {@code Query} use
	 * {@link Query#setReadOnly(boolean)}.
	 *
	 * @see #setReadOnly(Object,boolean)
	 * @see Query#setReadOnly(boolean)
	 *
	 * @param readOnly {@code true}, the default for loaded entities/proxies is read-only;
	 *				 {@code false}, the default for loaded entities/proxies is modifiable
	 */
	void setDefaultReadOnly(boolean readOnly);

	/**
	 * Return the identifier value of the given entity associated with this session.
	 * An exception is thrown if the given entity instance is transient or detached
	 * in relation to this session.
	 *
	 * @param object a persistent instance associated with this session
	 *
	 * @return the identifier
	 *
	 * @throws TransientObjectException if the instance is transient or associated with
	 * a different session
	 */
	Object getIdentifier(Object object);

	/**
	 * Determine if the given entity is associated with this session.
	 *
	 * @param entityName the entity name
	 * @param object an instance of a persistent class
	 *
	 * @return {@code true} if the given instance is associated with this {@code Session}
	 */
	boolean contains(String entityName, Object object);

	/**
	 * Remove this instance from the session cache. Changes to the instance will
	 * not be synchronized with the database. This operation cascades to associated
	 * instances if the association is mapped with
	 * {@link jakarta.persistence.CascadeType#DETACH}.
	 *
	 * @param object the managed instance to detach
	 */
	@Override
	void detach(Object object);

	/**
	 * Remove this instance from the session cache. Changes to the instance will
	 * not be synchronized with the database. This operation cascades to associated
	 * instances if the association is mapped with
	 * {@link jakarta.persistence.CascadeType#DETACH}.
	 * <p>
	 * This operation is a synonym for {@link #detach(Object)}.
	 *
	 * @param object the managed entity to evict
	 *
	 * @throws NullPointerException if the passed object is {@code null}
	 * @throws IllegalArgumentException if the passed object is not mapped as an entity
	 */
	void evict(Object object);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * obtaining the specified lock mode, assuming the instance exists.
	 * <p>
	 * Convenient form of {@link #load(Class, Object, LockOptions)}.
	 *
	 * @param theClass a persistent class
	 * @param id a valid identifier of an existing persistent instance of the class
	 * @param lockMode the lock level
	 *
	 * @return the persistent instance or proxy
	 *
	 * @see #load(Class, Object, LockOptions)
	 *
	 * @deprecated use {@link #get(Class, Object, LockMode)}
	 */
	@Deprecated(since = "6.0")
	<T> T load(Class<T> theClass, Object id, LockMode lockMode);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * obtaining the specified lock mode, assuming the instance exists.
	 *
	 * @param theClass a persistent class
	 * @param id a valid identifier of an existing persistent instance of the class
	 * @param lockOptions contains the lock level
	 * @return the persistent instance or proxy
	 *
	 * @deprecated use {@link #get(Class, Object, LockOptions)}
	 */
	@Deprecated(since = "6.0")
	<T> T load(Class<T> theClass, Object id, LockOptions lockOptions);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * obtaining the specified lock mode, assuming the instance exists.
	 * <p>
	 * Convenient form of {@link #load(String, Object, LockOptions)}.
	 *
	 * @param entityName the entity name
	 * @param id a valid identifier of an existing persistent instance of the class
	 * @param lockMode the lock level
	 *
	 * @return the persistent instance or proxy
	 *
	 * @see #load(String, Object, LockOptions)
	 *
	 * @deprecated use {@link #get(String, Object, LockMode)}
	 */
	@Deprecated(since = "6.0")
	Object load(String entityName, Object id, LockMode lockMode);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * obtaining the specified lock mode, assuming the instance exists.
	 *
	 * @param entityName the entity name
	 * @param id a valid identifier of an existing persistent instance of the class
	 * @param lockOptions contains the lock level
	 *
	 * @return the persistent instance or proxy
	 *
	 * @deprecated use {@link #get(String, Object, LockOptions)}
	 */
	@Deprecated(since = "6.0")
	Object load(String entityName, Object id, LockOptions lockOptions);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * making the assumption that the instance exists in the database. This method might
	 * return a proxied instance that is initialized on-demand, when a non-identifier method
	 * is accessed.
	 * <p>
	 * You should not use this method to determine if an instance exists in the database
	 * (use {@code get()} instead). Use this only to retrieve an instance that you assume
	 * exists, where non-existence would be an actual error.
	 * <p>
	 * This operation is very similar to {@link #getReference(Class, Object)}.
	 *
	 * @param theClass a persistent class
	 * @param id a valid identifier of an existing persistent instance of the class
	 *
	 * @return the persistent instance or proxy
	 *
	 * @deprecated use {@link #getReference(Class, Object)}
	 */
	@Deprecated(since = "6.0")
	<T> T load(Class<T> theClass, Object id);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * making the assumption that the instance exists in the database. This method might
	 * return a proxied instance that is initialized on-demand, when a non-identifier
	 * method is accessed.
	 * <p>
	 * You should not use this method to determine if an instance exists in the database
	 * (use {@code get()} instead). Use this only to retrieve an instance that you assume
	 * exists, where non-existence would be an actual error.
	 *
	 * @param entityName the entity name
	 * @param id a valid identifier of an existing persistent instance of the class
	 *
	 * @return the persistent instance or proxy
	 *
	 * @deprecated use {@link #getReference(String, Object)}
	 */
	@Deprecated(since = "6.0")
	Object load(String entityName, Object id);

	/**
	 * Read the persistent state associated with the given identifier into the given
	 * transient instance.
	 */
	void load(Object object, Object id);

	/**
	 * Persist the state of the given detached instance, reusing the current
	 * identifier value.  This operation cascades to associated instances if
	 * the association is mapped with
	 * {@link org.hibernate.annotations.CascadeType#REPLICATE}.
	 *
	 * @param object a detached instance of a persistent class
	 * @param replicationMode the replication mode to use
	 *
	 * @deprecated With no real replacement
	 */
	@Deprecated( since = "6.0" )
	void replicate(Object object, ReplicationMode replicationMode);

	/**
	 * Persist the state of the given detached instance, reusing the current
	 * identifier value.  This operation cascades to associated instances if
	 * the association is mapped with
	 * {@link org.hibernate.annotations.CascadeType#REPLICATE}.
	 *
	 * @param entityName the entity name
	 * @param object a detached instance of a persistent class
	 * @param replicationMode the replication mode to use
	 *
	 * @deprecated With no real replacement
	 */
	@Deprecated( since = "6.0" )
	void replicate(String entityName, Object object, ReplicationMode replicationMode) ;

	/**
	 * Persist the given transient instance, first assigning a generated identifier.
	 * (Or using the current value of the identifier property if the {@code assigned}
	 * generator is used.) This operation cascades to associated instances if the
	 * association is mapped with
	 * {@link org.hibernate.annotations.CascadeType#SAVE_UPDATE}.
	 * <p>
	 * This operation is very similar to {@link #persist(Object)}.
	 *
	 * @param object a transient instance of a persistent class
	 *
	 * @return the generated identifier
	 *
	 * @deprecated use {@link #persist(Object)}
	 */
	@Deprecated(since = "6.0")
	Object save(Object object);

	/**
	 * Persist the given transient instance, first assigning a generated identifier.
	 * (Or using the current value of the identifier property if the {@code assigned}
	 * generator is used.)  This operation cascades to associated instances if the
	 * association is mapped with {@link org.hibernate.annotations.CascadeType#SAVE_UPDATE}.
	 *
	 * @param entityName the entity name
	 * @param object a transient instance of a persistent class
	 *
	 * @return the generated identifier
	 *
	 * @deprecated use {@link #persist(String, Object)}
	 */
	@Deprecated(since = "6.0")
	Object save(String entityName, Object object);

	/**
	 * Either {@link #save(Object)} or {@link #update(Object)} the given
	 * instance, depending upon resolution of the unsaved-value checks (see the
	 * manual for discussion of unsaved-value checking).
	 * <p>
	 * This operation cascades to associated instances if the association is mapped
	 * with {@link org.hibernate.annotations.CascadeType#SAVE_UPDATE}.
	 *
	 * @param object a transient or detached instance containing new or updated state
	 *
	 * @see Session#save(Object)
	 * @see Session#update(Object object)
	 *
	 * @deprecated use {@link #merge(String, Object)} or {@link #persist(Object)}
	 */
	@Deprecated(since = "6.0")
	void saveOrUpdate(Object object);

	/**
	 * Either {@link #save(String, Object)} or {@link #update(String, Object)}
	 * the given instance, depending upon resolution of the unsaved-value checks
	 * (see the manual for discussion of unsaved-value checking).
	 * <p>
	 * This operation cascades to associated instances if the association is mapped
	 * with {@link org.hibernate.annotations.CascadeType#SAVE_UPDATE}.
	 *
	 * @param entityName the entity name
	 * @param object a transient or detached instance containing new or updated state
	 *
	 * @see Session#save(String,Object)
	 * @see Session#update(String,Object)
	 *
	 * @deprecated use {@link #merge(String, Object)} or {@link #persist(String, Object)}
	 */
	@Deprecated(since = "6.0")
	void saveOrUpdate(String entityName, Object object);

	/**
	 * Update the persistent instance with the identifier of the given detached
	 * instance. If there is a persistent instance with the same identifier,
	 * an exception is thrown. This operation cascades to associated instances
	 * if the association is mapped with
	 * {@link org.hibernate.annotations.CascadeType#SAVE_UPDATE}.
	 *
	 * @param object a detached instance containing updated state
	 *
	 * @deprecated use {@link #merge(Object)}
	 */
	@Deprecated(since = "6.0")
	void update(Object object);

	/**
	 * Update the persistent instance with the identifier of the given detached
	 * instance. If there is a persistent instance with the same identifier,
	 * an exception is thrown. This operation cascades to associated instances
	 * if the association is mapped with
	 * {@link org.hibernate.annotations.CascadeType#SAVE_UPDATE}.
	 *
	 * @param entityName the entity name
	 * @param object a detached instance containing updated state
	 *
	 * @deprecated use {@link #merge(String, Object)}
	 */
	@Deprecated(since = "6.0")
	void update(String entityName, Object object);

	/**
	 * Copy the state of the given object onto the persistent object with the same
	 * identifier. If there is no persistent instance currently associated with
	 * the session, it will be loaded. Return the persistent instance. If the
	 * given instance is unsaved, save a copy and return it as a newly persistent
	 * instance. The given instance does not become associated with the session.
	 * This operation cascades to associated instances if the association is mapped
	 * with {@link jakarta.persistence.CascadeType#MERGE}.
	 *
	 * @param object a detached instance with state to be copied
	 *
	 * @return an updated persistent instance
	 */
	<T> T merge(T object);

	/**
	 * Copy the state of the given object onto the persistent object with the same
	 * identifier. If there is no persistent instance currently associated with
	 * the session, it will be loaded. Return the persistent instance. If the
	 * given instance is unsaved, save a copy and return it as a newly persistent
	 * instance. The given instance does not become associated with the session.
	 * This operation cascades to associated instances if the association is mapped
	 * with {@link jakarta.persistence.CascadeType#MERGE}.
	 *
	 * @param entityName the entity name
	 * @param object a detached instance with state to be copied
	 *
	 * @return an updated persistent instance
	 */
	<T> T merge(String entityName, T object);

	/**
	 * Make a transient instance persistent and mark it for later insertion in the
	 * database. This operation cascades to associated instances if the association
	 * is mapped with {@link jakarta.persistence.CascadeType#PERSIST}.
	 * <p>
	 * For an entity with a {@linkplain jakarta.persistence.GeneratedValue generated}
	 * id, {@code persist()} ultimately results in generation of an identifier for
	 * the given instance. But this may happen asynchronously, when the session is
	 * {@linkplain #flush() flushed}, depending on the identifier generation strategy.
	 *
	 * @param object a transient instance to be made persistent
	 */
	void persist(Object object);

	/**
	 * Make a transient instance persistent and mark it for later insertion in the
	 * database. This operation cascades to associated instances if the association
	 * is mapped with {@link jakarta.persistence.CascadeType#PERSIST}.
	 * <p>
	 * For entities with a {@link jakarta.persistence.GeneratedValue generated id},
	 * {@code persist()} ultimately results in generation of an identifier for the
	 * given instance. But this may happen asynchronously, when the session is
	 * {@linkplain #flush() flushed}, depending on the identifier generation strategy.
	 *
	 * @param entityName the entity name
	 * @param object a transient instance to be made persistent
	 */
	void persist(String entityName, Object object);

	/**
	 * Remove a persistent instance from the datastore. The argument may be
	 * an instance associated with the receiving {@code Session} or a transient
	 * instance with an identifier associated with existing persistent state.
	 * This operation cascades to associated instances if the association is
	 * mapped with {@link jakarta.persistence.CascadeType#REMOVE}.
	 *
	 * @param object the instance to be removed
	 *
	 * @deprecated use {@link #remove(Object)}
	 */
	@Deprecated(since = "6.0")
	void delete(Object object);

	/**
	 * Remove a persistent instance from the datastore. The second argument may
	 * be an instance associated with the receiving {@code Session} or a transient
	 * instance with an identifier associated with existing persistent state.
	 * This operation cascades to associated instances if the association is
	 * mapped with {@link jakarta.persistence.CascadeType#REMOVE}.
	 *
	 * @param entityName the entity name for the instance to be removed.
	 * @param object the instance to be removed
	 *
	 * @deprecated use {@link #remove(Object)}
	 */
	@Deprecated(since = "6.0")
	void delete(String entityName, Object object);

	/**
	 * Obtain the specified lock level on the given managed instance associated
	 * with this session. This operation may be used to:
	 * <ul>
	 * <li>perform a version check on an entity read from the second-level cache
	 *     by requesting {@link LockMode#READ},
	 * <li>schedule a version check at transaction commit by requesting
	 *     {@link LockMode#OPTIMISTIC},
	 * <li>schedule a version increment at transaction commit by requesting
	 *     {@link LockMode#OPTIMISTIC_FORCE_INCREMENT}
	 * <li>upgrade to a pessimistic lock with {@link LockMode#PESSIMISTIC_READ}
	 *     or {@link LockMode#PESSIMISTIC_WRITE}, or
	 * <li>immediately increment the version of the given instance by requesting
	 *     {@link LockMode#PESSIMISTIC_FORCE_INCREMENT}.
	 * </ul>
	 * <p>
	 * If the requested lock mode is already held on the given entity, this
	 * operation has no effect.
	 * <p>
	 * This operation cascades to associated instances if the association is
	 * mapped with {@link org.hibernate.annotations.CascadeType#LOCK}.
	 * <p>
	 * The modes {@link LockMode#WRITE} and {@link LockMode#UPGRADE_SKIPLOCKED}
	 * are not legal arguments to {@code lock()}.
	 *
	 * @param object a persistent instance
	 * @param lockMode the lock level
	 */
	void lock(Object object, LockMode lockMode);

	/**
	 * Obtain a lock on the given managed instance associated with this session,
	 * using the given {@linkplain LockOptions lock options}.
	 * <p>
	 * This operation cascades to associated instances if the association is
	 * mapped with {@link org.hibernate.annotations.CascadeType#LOCK}.
	 *
	 * @param object a persistent instance
	 * @param lockOptions the lock options
	 *
	 * @since 6.2
	 */
	void lock(Object object, LockOptions lockOptions);

	/**
	 * Obtain the specified lock level on the given managed instance associated
	 * with this session. This may be used to:
	 * <ul>
	 * <li>perform a version check with {@link LockMode#READ}, or
	 * <li>upgrade to a pessimistic lock with {@link LockMode#PESSIMISTIC_WRITE}).
	 * </ul>
	 * <p>
	 * This operation cascades to associated instances if the association is
	 * mapped with {@link org.hibernate.annotations.CascadeType#LOCK}.
	 *
	 * @param entityName the name of the entity
	 * @param object a persistent instance associated with this session
	 * @param lockMode the lock level
	 *
	 * @deprecated use {@link #lock(Object, LockMode)}
	 */
	@Deprecated(since = "6.2")
	void lock(String entityName, Object object, LockMode lockMode);

	/**
	 * Build a new {@linkplain LockRequest lock request} that specifies:
	 * <ul>
	 * <li>the {@link LockMode} to use,
	 * <li>the {@linkplain LockRequest#setTimeOut(int) pessimistic lock timeout},
	 *     and
	 * <li>the {@linkplain LockRequest#setLockScope(PessimisticLockScope) scope}
	 *     that is, whether the lock extends to rows of owned collections.
	 * </ul>
	 * <p>
	 * Timeout and scope are ignored if the specified {@code LockMode} represents
	 * a flavor of {@linkplain LockMode#OPTIMISTIC optimistic} locking.
	 * <p>
	 * Call {@link LockRequest#lock(Object)} to actually obtain the requested lock
	 * on a managed entity instance.
	 *
	 * @param lockOptions contains the lock level
	 *
	 * @return a {@link LockRequest} that can be used to lock any given object.
	 *
	 * @deprecated use {@link #lock(Object, LockOptions)}
	 */
	@Deprecated(since = "6.2")
	LockRequest buildLockRequest(LockOptions lockOptions);

	/**
	 * Reread the state of the given managed instance associated with this session
	 * from the underlying database. This may be useful:
	 * <ul>
	 * <li>when a database trigger alters the object state upon insert or update,
	 * <li>after {@linkplain #createMutationQuery(String) executing} any HQL update
	 *     or delete statement,
	 * <li>after {@linkplain #createNativeMutationQuery(String) executing} a native
	 *     SQL statement, or
	 * <li>after inserting a {@link java.sql.Blob} or {@link java.sql.Clob}.
	 * </ul>
	 * <p>
	 * This operation cascades to associated instances if the association is mapped
	 * with {@link jakarta.persistence.CascadeType#REFRESH}.
	 * <p>
	 * This operation requests {@link LockMode#READ}. To obtain a stronger lock,
	 * call {@link #refresh(Object, LockMode)}.
	 *
	 * @param object a persistent instance associated with this session
	 */
	void refresh(Object object);

	/**
	 * Reread the state of the given managed instance associated with this session
	 * from the underlying database. This may be useful:
	 * <ul>
	 * <li>when a database trigger alters the object state upon insert or update,
	 * <li>after {@linkplain #createMutationQuery(String) executing} any HQL update
	 *     or delete statement,
	 * <li>after {@linkplain #createNativeMutationQuery(String) executing} a native
	 *     SQL statement, or
	 * <li>after inserting a {@link java.sql.Blob} or {@link java.sql.Clob}.
	 * </ul>
	 * <p>
	 * This operation cascades to associated instances if the association is mapped
	 * with {@link jakarta.persistence.CascadeType#REFRESH}.
	 *
	 * @param entityName the name of the entity
	 * @param object a persistent instance associated with this session
	 *
	 * @deprecated use {@link #refresh(Object)}
	 */
	@Deprecated(since = "6.0")
	void refresh(String entityName, Object object);

	/**
	 * Reread the state of the given managed instance from the underlying database,
	 * obtaining the given {@link LockMode}.
	 * <p>
	 * Convenient form of {@link #refresh(Object, LockOptions)}
	 *
	 * @param object a persistent instance associated with this session
	 * @param lockMode the lock mode to use
	 *
	 * @see #refresh(Object, LockOptions)
	 */
	void refresh(Object object, LockMode lockMode);

	/**
	 * Reread the state of the given managed instance from the underlying database,
	 * obtaining the given {@link LockMode}.
	 *
	 * @param object a persistent instance associated with this session
	 * @param lockOptions contains the lock mode to use
	 */
	void refresh(Object object, LockOptions lockOptions);

	/**
	 * Reread the state of the given managed instance from the underlying database,
	 * obtaining the given {@link LockMode}.
	 *
	 * @param entityName the name of the entity
	 * @param object a persistent instance associated with this session
	 * @param lockOptions contains the lock mode to use
	 *
	 * @deprecated use {@link #refresh(Object, LockOptions)}
	 */
	@Deprecated(since = "6.0")
	void refresh(String entityName, Object object, LockOptions lockOptions);

	/**
	 * Mark a persistence instance associated with this session for removal from
	 * the underlying database. Ths operation cascades to associated instances if
	 * the association is mapped {@link jakarta.persistence.CascadeType#REMOVE}.
	 *
	 * @param object the managed persistent instance to remove
	 */
	@Override
	void remove(Object object);

	/**
	 * Determine the current {@link LockMode} of the given managed instance associated
	 * with this session.
	 *
	 * @param object a persistent instance associated with this session
	 *
	 * @return the current lock mode
	 */
	LockMode getCurrentLockMode(Object object);

	/**
	 * Completely clear the session. Evict all loaded instances and cancel all pending
	 * saves, updates and deletions. Do not close open iterators or instances of
	 * {@link ScrollableResults}.
	 */
	void clear();

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * or null if there is no such persistent instance. If the instance is already associated
	 * with the session, return that instance. This method never returns an uninitialized
	 * instance.
	 * <p>
	 * This operation is very similar to {@link #find(Class, Object)}.
	 * <p>
	 * The object returned by {@code get()} or {@code find() } is either an unproxied instance
	 * of the given entity class, of a fully-fetched proxy object.
	 * <p>
	 * This operation requests {@link LockMode#NONE}, that is, no lock, allowing the object
	 * to be retrieved from the cache without the cost of database access. However, if it is
	 * necessary to read the state from the database, the object will be returned with the
	 * lock mode {@link LockMode#READ}.
	 * <p>
	 * To bypass the second-level cache, and ensure that the state is read from the database,
	 * either:
	 * <ul>
	 * <li>call {@link #get(Class, Object, LockMode)} with the explicit lock mode
	 *     {@link LockMode#READ}, or
	 * <li>{@linkplain #setCacheMode(CacheMode) set the cache mode} to {@link CacheMode#IGNORE}
	 *     before calling this method.
	 * </ul>
	 *
	 * @param entityType the entity type
	 * @param id an identifier
	 *
	 * @return a persistent instance or null
	 */
	<T> T get(Class<T> entityType, Object id);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * or null if there is no such persistent instance. If the instance is already associated
	 * with the session, return that instance. This method never returns an uninitialized
	 * instance. Obtain the specified lock mode if the instance exists.
	 * <p>
	 * Convenient form of {@link #get(Class, Object, LockOptions)}.
	 * <p>
	 * This operation is very similar to {@link #find(Class, Object, jakarta.persistence.LockModeType)}.
	 *
	 * @param entityType the entity type
	 * @param id an identifier
	 * @param lockMode the lock mode
	 *
	 * @return a persistent instance or null
	 *
	 * @see #get(Class, Object, LockOptions)
	 */
	<T> T get(Class<T> entityType, Object id, LockMode lockMode);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * or null if there is no such persistent instance. If the instance is already associated
	 * with the session, return that instance. This method never returns an uninitialized
	 * instance. Obtain the specified lock mode if the instance exists.
	 *
	 * @param entityType the entity type
	 * @param id an identifier
	 * @param lockOptions the lock mode
	 *
	 * @return a persistent instance or null
	 */
	<T> T get(Class<T> entityType, Object id, LockOptions lockOptions);

	/**
	 * Return the persistent instance of the given named entity with the given identifier,
	 * or null if there is no such persistent instance. If the instance is already associated
	 * with the session, return that instance. This method never returns an uninitialized
	 * instance.
	 *
	 * @param entityName the entity name
	 * @param id an identifier
	 *
	 * @return a persistent instance or null
	 */
	Object get(String entityName, Object id);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * or null if there is no such persistent instance. If the instance is already associated
	 * with the session, return that instance. This method never returns an uninitialized
	 * instance. Obtain the specified lock mode if the instance exists.
	 * <p>
	 * Convenient form of {@link #get(String, Object, LockOptions)}
	 *
	 * @param entityName the entity name
	 * @param id an identifier
	 * @param lockMode the lock mode
	 *
	 * @return a persistent instance or null
	 *
	 * @see #get(String, Object, LockOptions)
	 */
	Object get(String entityName, Object id, LockMode lockMode);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * or null if there is no such persistent instance. If the instance is already associated
	 * with the session, return that instance. This method never returns an uninitialized
	 * instance. Obtain the specified lock mode if the instance exists.
	 *
	 * @param entityName the entity name
	 * @param id an identifier
	 * @param lockOptions contains the lock mode
	 *
	 * @return a persistent instance or null
	 */
	Object get(String entityName, Object id, LockOptions lockOptions);

	/**
	 * Return the entity name for a persistent entity.
	 *   
	 * @param object a persistent entity associated with this session
	 *
	 * @return the entity name
	 */
	String getEntityName(Object object);

	/**
	 * Return a reference to the persistent instance with the given class and identifier,
	 * making the assumption that the instance is still persistent in the database. This
	 * method never results in access to the underlying data store, and thus might return
	 * a proxy that is initialized on-demand, when a non-identifier method is accessed.
	 * <p>
	 * Note that {@link Hibernate#createDetachedProxy(SessionFactory, Class, Object)}
	 * may be used to obtain a <em>detached</em> reference.
	 * <p>
	 * It's sometimes necessary to narrow a reference returned by {@code getReference()}
	 * to a subtype of the given entity type. A direct Java typecast should never be used
	 * in this situation. Instead, the method {@link Hibernate#unproxy(Object, Class)} is
	 * the recommended way to narrow the type of a proxy object. Alternatively, a new
	 * reference may be obtained by simply calling {@code getReference()} again, passing
	 * the subtype. Either way, the narrowed reference will usually not be identical to
	 * the original reference, when the references are compared using the {@code ==}
	 * operator.
	 *
	 * @param entityType the entity type
	 * @param id the identifier of a persistent instance that exists in the database
	 *
	 * @return the persistent instance or proxy
	 */
	@Override
	<T> T getReference(Class<T> entityType, Object id);

	/**
	 * Return a reference to the persistent instance of the given named entity with the
	 * given identifier, making the assumption that the instance is still persistent in
	 * the database. This method never results in access to the underlying data store,
	 * and thus might return a proxy that is initialized on-demand, when a non-identifier
	 * method is accessed.
	 *
	 * @param entityName the entity name
	 * @param id the identifier of a persistent instance that exists in the database
	 *
	 * @return the persistent instance or proxy
	 */
	Object getReference(String entityName, Object id);

	/**
	 * Return a reference to the persistent instance with the same identity as the given
	 * instance, which might be detached, making the assumption that the instance is still
	 * persistent in the database. This method never results in access to the underlying
	 * data store, and thus might return a proxy that is initialized on-demand, when a
	 * non-identifier method is accessed.
	 *
	 * @param object a detached persistent instance
	 *
	 * @return the persistent instance or proxy
	 *
	 * @since 6.0
	 */
	<T> T getReference(T object);

	/**
	 * Create an {@link IdentifierLoadAccess} instance to retrieve an instance of the given
	 * entity type by its primary key.
	 *
	 * @param entityClass the entity type to be retrieved
	 *
	 * @return an instance of {@link IdentifierLoadAccess} for executing the lookup
	 *
	 * @throws HibernateException If the given class does not resolve as a mapped entity
	 */
	<T> IdentifierLoadAccess<T> byId(Class<T> entityClass);

	/**
	 * Create an {@link IdentifierLoadAccess} instance to retrieve an instance of the named
	 * entity type by its primary key.
	 * 
	 * @param entityName the entity name of the entity type to be retrieved
	 *
	 * @return an instance of {@link IdentifierLoadAccess} for executing the lookup
	 *
	 * @throws HibernateException If the given name does not resolve to a mapped entity
	 */
	<T> IdentifierLoadAccess<T> byId(String entityName);

	/**
	 * Create a {@link MultiIdentifierLoadAccess} instance to retrieve multiple instances
	 * of the given entity type by their primary key values, using batching.
	 *
	 * @param entityClass the entity type to be retrieved
	 *
	 * @return an instance of {@link MultiIdentifierLoadAccess} for executing the lookup
	 *
	 * @throws HibernateException If the given class does not resolve as a mapped entity
	 */
	<T> MultiIdentifierLoadAccess<T> byMultipleIds(Class<T> entityClass);

	/**
	 * Create a {@link MultiIdentifierLoadAccess} instance to retrieve multiple instances
	 * of the named entity type by their primary key values, using batching.
	 *
	 * @param entityName the entity name of the entity type to be retrieved
	 *
	 * @return an instance of {@link MultiIdentifierLoadAccess} for executing the lookup
	 *
	 * @throws HibernateException If the given name does not resolve to a mapped entity
	 */
	<T> MultiIdentifierLoadAccess<T> byMultipleIds(String entityName);

	/**
	 * Create a {@link NaturalIdLoadAccess} instance to retrieve an instance of the given
	 * entity type by its {@linkplain org.hibernate.annotations.NaturalId natural id},
	 * which may be a composite natural id. The entity must have at least one attribute
	 * annotated {@link org.hibernate.annotations.NaturalId}.
	 *
	 * @param entityClass the entity type to be retrieved
	 *
	 * @return an instance of {@link NaturalIdLoadAccess} for executing the lookup
	 *
	 * @throws HibernateException If the given class does not resolve as a mapped entity,
	 *                            or if the entity does not declare a natural id
	 */
	<T> NaturalIdLoadAccess<T> byNaturalId(Class<T> entityClass);

	/**
	 * Create a {@link NaturalIdLoadAccess} instance to retrieve an instance of the named
	 * entity type by its {@linkplain org.hibernate.annotations.NaturalId natural id},
	 * which may be a composite natural id. The entity must have at least one attribute
	 * annotated {@link org.hibernate.annotations.NaturalId}.
	 * 
	 * @param entityName the entity name of the entity type to be retrieved
	 *
	 * @return an instance of {@link NaturalIdLoadAccess} for executing the lookup
	 *
	 * @throws HibernateException If the given name does not resolve to a mapped entity,
	 *                            or if the entity does not declare a natural id
	 */
	<T> NaturalIdLoadAccess<T> byNaturalId(String entityName);

	/**
	 * Create a {@link SimpleNaturalIdLoadAccess} instance to retrieve an instance of the
	 * given entity type by its {@linkplain org.hibernate.annotations.NaturalId natural id},
	 * which must be a simple (non-composite) value. The entity must have exactly one
	 * attribute annotated {@link org.hibernate.annotations.NaturalId}.
	 *
	 * @param entityClass the entity type to be retrieved
	 *
	 * @return an instance of {@link SimpleNaturalIdLoadAccess} for executing the lookup
	 *
	 * @throws HibernateException If the given class does not resolve as a mapped entity,
	 *                            or if the entity does not declare a natural id
	 */
	<T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(Class<T> entityClass);

	/**
	 * Create a {@link SimpleNaturalIdLoadAccess} instance to retrieve an instance of the
	 * named entity type by its {@linkplain org.hibernate.annotations.NaturalId natural id},
	 * which must be a simple (non-composite) value. The entity must have exactly one
	 * attribute annotated {@link org.hibernate.annotations.NaturalId}.
	 *
	 * @param entityName the entity name of the entity type to be retrieved
	 *
	 * @return an instance of {@link SimpleNaturalIdLoadAccess} for executing the lookup
	 *
	 * @throws HibernateException If the given name does not resolve to a mapped entity,
	 *                            or if the entity does not declare a natural id
	 */
	<T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(String entityName);

	/**
	 * Create a {@link MultiIdentifierLoadAccess} instance to retrieve multiple instances
	 * of the given entity type by their by {@linkplain org.hibernate.annotations.NaturalId
	 * natural id} values, using batching.
	 *
	 * @param entityClass the entity type to be retrieved
	 *
	 * @return an instance of {@link NaturalIdMultiLoadAccess} for executing the lookup
	 *
	 * @throws HibernateException If the given class does not resolve as a mapped entity,
	 *                            or if the entity does not declare a natural id
	 */
	<T> NaturalIdMultiLoadAccess<T> byMultipleNaturalId(Class<T> entityClass);

	/**
	 * Create a {@link MultiIdentifierLoadAccess} instance to retrieve multiple instances
	 * of the named entity type by their by {@linkplain org.hibernate.annotations.NaturalId
	 * natural id} values, using batching.
	 *
	 * @param entityName the entity name of the entity type to be retrieved
	 *
	 * @return an instance of {@link NaturalIdMultiLoadAccess} for executing the lookup
	 *
	 * @throws HibernateException If the given name does not resolve to a mapped entity,
	 *                            or if the entity does not declare a natural id
	 */
	<T> NaturalIdMultiLoadAccess<T> byMultipleNaturalId(String entityName);

	@Override
	Filter enableFilter(String filterName);

	@Override
	Filter getEnabledFilter(String filterName);

	@Override
	void disableFilter(String filterName);
	
	/**
	 * Get the {@linkplain SessionStatistics statistics} for this session.
	 *
	 * @return the session statistics being collected for this session
	 */
	SessionStatistics getStatistics();

	/**
	 * Is the specified entity or proxy read-only?
	 * <p>
	 * To get the default read-only/modifiable setting used for
	 * entities and proxies that are loaded into the session use
	 * {@link #isDefaultReadOnly()}
	 *
	 * @see #isDefaultReadOnly()
	 *
	 * @param entityOrProxy an entity or proxy
	 * @return {@code true} if the entity or proxy is read-only,
	 *         {@code false} if the entity or proxy is modifiable.
	 */
	boolean isReadOnly(Object entityOrProxy);

	/**
	 * Set an unmodified persistent object to read-only mode, or a read-only
	 * object to modifiable mode. In read-only mode, no snapshot is maintained,
	 * the instance is never dirty checked, and changes are not persisted.
	 * <p>
	 * If the entity or proxy already has the specified read-only/modifiable
	 * setting, then this method does nothing.
	 * <p>
	 * To set the default read-only/modifiable setting used for all entities
	 * and proxies that are loaded into the session use
	 * {@link #setDefaultReadOnly(boolean)}.
	 * <p>
	 * To override this session's read-only/modifiable setting for entities
	 * and proxies loaded by a {@code Query} use
	 * {@link Query#setReadOnly(boolean)}
	 *
	 * @see #setDefaultReadOnly(boolean)
	 * @see Query#setReadOnly(boolean)
	 * @see IdentifierLoadAccess#withReadOnly(boolean)
	 *
	 * @param entityOrProxy an entity or proxy
	 * @param readOnly {@code true} if the entity or proxy should be made read-only;
	 *				   {@code false} if the entity or proxy should be made modifiable
	 */
	void setReadOnly(Object entityOrProxy, boolean readOnly);

	/**
	 * Is the {@link org.hibernate.annotations.FetchProfile fetch profile}
	 * with the given name enabled in this session?
	 *
	 * @param name the name of the profile
	 * @return True if fetch profile is enabled; false if not.
	 *
	 * @throws UnknownProfileException Indicates that the given name does not
	 *                                 match any known fetch profile names
	 *
	 * @see org.hibernate.annotations.FetchProfile
	 */
	boolean isFetchProfileEnabled(String name) throws UnknownProfileException;

	/**
	 * Enable the {@link org.hibernate.annotations.FetchProfile fetch profile}
	 * with the given name in this session. If the requested fetch profile is
	 * already enabled, the call has no effect.
	 *
	 * @param name the name of the fetch profile to be enabled
	 *
	 * @throws UnknownProfileException Indicates that the given name does not
	 *                                 match any known fetch profile names
	 *
	 * @see org.hibernate.annotations.FetchProfile
	 */
	void enableFetchProfile(String name) throws UnknownProfileException;

	/**
	 * Disable the {@link org.hibernate.annotations.FetchProfile fetch profile}
	 * with the given name in this session. If the requested fetch profile is
	 * not currently enabled, the call has no effect.
	 *
	 * @param name the name of the fetch profile to be disabled
	 *
	 * @throws UnknownProfileException Indicates that the given name does not
	 *                                 match any known fetch profile names
	 *
	 * @see org.hibernate.annotations.FetchProfile
	 */
	void disableFetchProfile(String name) throws UnknownProfileException;

	/**
	 * Obtain a {@linkplain LobHelper factory} for instances of {@link java.sql.Blob}
	 * and {@link java.sql.Clob}.
	 *
	 * @return an instance of {@link LobHelper}
	 */
	LobHelper getLobHelper();

	/**
	 * Obtain a {@link Session} builder with the ability to copy certain information
	 * from this session.
	 *
	 * @return the session builder
	 */
	SharedSessionBuilder sessionWithOptions();

	/**
	 * A set of {@linkplain LockOptions locking options} attached
	 * to the session.
	 *
	 * @deprecated simply construct a {@link LockOptions} and pass
	 *             it to {@link #lock(Object, LockOptions)}.
	 */
	@Deprecated(since = "6.2")
	interface LockRequest {
		/**
		 * A timeout value indicating that the database should not
		 * wait at all to acquire a pessimistic lock which is not
		 * immediately available. This has the same effect as
		 * {@link LockMode#UPGRADE_NOWAIT}.
		 */
		int PESSIMISTIC_NO_WAIT = LockOptions.NO_WAIT;

		/**
		 * A timeout value indicating that attempting to acquire
		 * that there is no timeout for the lock acquisition.
		 */
		int PESSIMISTIC_WAIT_FOREVER = LockOptions.WAIT_FOREVER;

		/**
		 * Get the lock mode.
		 *
		 * @return the lock mode.
		 */
		LockMode getLockMode();

		/**
		 * Specify the {@link LockMode} to be used. The default is {@link LockMode#NONE}.
		 *
		 * @param lockMode the lock mode to use for this request
		 *
		 * @return this {@code LockRequest} instance for operation chaining.
		 */
		LockRequest setLockMode(LockMode lockMode);

		/**
		 * Get the timeout setting.
		 *
		 * @return timeout in milliseconds, -1 for indefinite wait and 0 for no wait.
		 */
		int getTimeOut();

		/**
		 * Specify the pessimistic lock timeout. The default pessimistic lock
		 * behavior is to wait forever for the lock. Lock timeout support is
		 * not available in every {@link org.hibernate.dialect.Dialect dialect}
		 * of SQL.
		 *
		 * @param timeout is time in milliseconds to wait for lock.
		 *                -1 means wait forever and 0 means no wait.
		 *
		 * @return this {@code LockRequest} instance for operation chaining.
		 */
		LockRequest setTimeOut(int timeout);

		/**
		 * Check if locking extends to owned collections and associated entities.
		 *
		 * @return true if locking will be extended to owned collections and associated entities
		 *
		 * @deprecated use {@link #getLockScope()}
		 */
		@Deprecated(since = "6.2")
		boolean getScope();

		/**
		 * Obtain the {@link PessimisticLockScope}, which determines if locking
		 * extends to owned collections and associated entities.
		 */
		default PessimisticLockScope getLockScope() {
			return getScope() ? PessimisticLockScope.EXTENDED : PessimisticLockScope.NORMAL;
		}

		/**
		 * Specify whether the {@link LockMode} should extend to owned collections
		 * and associated entities. An association must be mapped with
		 * {@link org.hibernate.annotations.CascadeType#LOCK} for this setting to
		 * have any effect.
		 *
		 * @param scope {@code true} to cascade locks; {@code false} to not.
		 *
		 * @return {@code this}, for method chaining
		 *
		 * @deprecated use {@link #setLockScope(PessimisticLockScope)}
		 */
		@Deprecated(since = "6.2")
		LockRequest setScope(boolean scope);

		/**
		 * Set the {@link PessimisticLockScope}, which determines if locking
		 * extends to owned collections and associated entities.
		 */
		default LockRequest setLockScope(PessimisticLockScope scope) {
			return setScope( scope == PessimisticLockScope.EXTENDED );
		}

		/**
		 * Perform the requested locking.
		 *
		 * @param entityName the name of the entity to lock
		 * @param object the instance of the entity to lock
		 *
		 * @deprecated use {@link #lock(Object)}
		 */
		@Deprecated(since = "6.2")
		void lock(String entityName, Object object);

		/**
		 * Perform the requested locking.
		 *
		 * @param object the instance of the entity to lock
		 */
		void lock(Object object);
	}

	/**
	 * Add one or more listeners to the Session
	 *
	 * @param listeners the listener(s) to add
	 */
	void addEventListeners(SessionEventListener... listeners);

	@Override
	<T> RootGraph<T> createEntityGraph(Class<T> rootType);

	@Override
	RootGraph<?> createEntityGraph(String graphName);

	@Override
	RootGraph<?> getEntityGraph(String graphName);

	@Override
	<T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass);

	// The following overrides should not be necessary,
	// and are only needed to work around a bug in IntelliJ

	@Override
	<R> Query<R> createQuery(String queryString, Class<R> resultClass);

	@Override @Deprecated @SuppressWarnings("rawtypes")
	Query createQuery(String queryString);

	@Override
	<R> Query<R> createNamedQuery(String name, Class<R> resultClass);

	@Override @Deprecated @SuppressWarnings("rawtypes")
	Query createNamedQuery(String name);

	@Override
	<R> Query<R> createQuery(CriteriaQuery<R> criteriaQuery);

	/**
	 * Create a {@link Query} for the given JPA {@link CriteriaDelete}.
	 *
	 * @deprecated use {@link #createMutationQuery(CriteriaDelete)}
	 */
	@Override @Deprecated(since = "6.0") @SuppressWarnings("rawtypes")
	Query createQuery(CriteriaDelete deleteQuery);

	/**
	 * Create a {@link Query} for the given JPA {@link CriteriaUpdate}.
	 *
	 * @deprecated use {@link #createMutationQuery(CriteriaUpdate)}
	 */
	@Override @Deprecated(since = "6.0") @SuppressWarnings("rawtypes")
	Query createQuery(CriteriaUpdate updateQuery);
}
