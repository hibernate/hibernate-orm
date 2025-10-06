/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.FindOption;
import jakarta.persistence.LockOption;
import jakarta.persistence.RefreshOption;
import jakarta.persistence.metamodel.EntityType;
import org.hibernate.graph.RootGraph;
import org.hibernate.jdbc.Work;
import org.hibernate.query.Query;
import org.hibernate.stat.SessionStatistics;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQueryReference;
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
 * Each persistent instance has a <em>persistent identity</em> determined by its type
 * and identifier value. There may be at most one persistent instance with a given
 * persistent identity associated with a given session. A persistent identity is
 * assigned when an {@linkplain #persist(Object) instance is made persistent}.
 * <p>
 * An instance of an entity class may be associated with at most one open session.
 * Distinct sessions represent state with the same persistent identity using distinct
 * persistent instances of the mapped entity class.
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
 * A persistent instance may be marked for removal, and eventually made transient,
 * by calling {@link #remove(Object)}.
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
 *     {@link #find(Class,Object,LockModeType)}, {@link #find(Class,Object,FindOption...)},
 *     {@link #refresh(Object,LockModeType)}, {@link #refresh(Object,RefreshOption...)},
 *     or {@link org.hibernate.query.SelectionQuery#setLockMode(LockModeType)}.
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
 * <li>If a transaction is rolled back, the state of the persistence context and of its
 *     associated entities must be assumed inconsistent with the database, and the
 *     session must be discarded.
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
	@Override
	void flush();

	/**
	 * Set the current {@linkplain FlushModeType JPA flush mode} for this session.
	 * <p>
	 * <em>Flushing</em> is the process of synchronizing the underlying persistent
	 * store with persistable state held in memory. The current flush mode determines
	 * when the session is automatically flushed.
	 *
	 * @param flushMode the new {@link FlushModeType}
	 *
	 * @see #setHibernateFlushMode(FlushMode)
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
	 *
	 * @see #getHibernateFlushMode()
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
	@Override
	CacheStoreMode getCacheStoreMode();

	/**
	 * The JPA-defined {@link CacheRetrieveMode}.
	 *
	 * @see #getCacheMode()
	 *
	 * @since 6.2
	 */
	@Override
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
	@Override
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
	@Override
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
	 * @return {@code true} if the session contains pending changes;
	 *         {@code false} otherwise.
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
	 * from modifiable to read-only mode, or from read-only to modifiable mode.
	 * <p>
	 * Read-only entities are not dirty-checked, and snapshots of persistent
	 * state are not maintained. Read-only entities can be modified, but a
	 * modification to a field of a read-only entity is not made persistent.
	 * <p>
	 * When a proxy is initialized, the loaded entity will have the same
	 * read-only/modifiable setting as the uninitialized proxy, regardless of
	 * the {@linkplain #isDefaultReadOnly current default read-only mode}
	 * of the session.
	 * <p>
	 * To change the read-only/modifiable setting for a particular entity
	 * or proxy that already belongs to this session, use
	 * {@link #setReadOnly(Object, boolean)}.
	 * <p>
	 * To override the default read-only mode of the current session for
	 * all entities and proxies returned by a given {@code Query}, use
	 * {@link Query#setReadOnly(boolean)}.
	 * <p>
	 * Every instance of an {@linkplain org.hibernate.annotations.Immutable
	 * immutable} entity is loaded in read-only mode.
	 *
	 * @see #setReadOnly(Object,boolean)
	 * @see Query#setReadOnly(boolean)
	 *
	 * @param readOnly {@code true}, the default for loaded entities/proxies is read-only;
	 *				 {@code false}, the default for loaded entities/proxies is modifiable
	 * @throws SessionException if the session was originally
	 *         {@linkplain SessionBuilder#readOnly created in read-only mode}
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
	 * @throws IllegalArgumentException if the given object is not an entity
	 */
	void evict(Object object);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * or null if there is no such persistent instance. If the instance is already associated
	 * with the session, return that instance. This method never returns an uninitialized
	 * instance.
	 * <p>
	 * The object returned by {@code get()} or {@code find()} is either an unproxied instance
	 * of the given entity class, or a fully-fetched proxy object.
	 * <p>
	 * This operation requests {@link LockMode#NONE}, that is, no lock, allowing the object
	 * to be retrieved from the cache without the cost of database access. However, if it is
	 * necessary to read the state from the database, the object will be returned with the
	 * lock mode {@link LockMode#READ}.
	 * <p>
	 * To bypass the {@linkplain Cache second-level cache}, and ensure that the state of the
	 * requested instance is read directly from the database, either:
	 * <ul>
	 * <li>call {@link #find(Class, Object, FindOption...)}, passing
	 *     {@link CacheRetrieveMode#BYPASS} as an option,
	 * <li>call {@link #find(Class, Object, FindOption...)} with the explicit lock mode
	 *     {@link LockMode#READ}, or
	 * <li>{@linkplain #setCacheRetrieveMode set the cache mode} to
	 *     {@link CacheRetrieveMode#BYPASS} before calling this method.
	 * </ul>
	 *
	 * @apiNote This operation is very similar to {@link #get(Class, Object)}.
	 *
	 * @param entityType the entity type
	 * @param id an identifier
	 *
	 * @return a fully-fetched persistent instance or null
	 */
	@Override
	<T> T find(Class<T> entityType, Object id);

	/**
	 * Return the persistent instance of the named entity type with the given identifier,
	 * or null if there is no such persistent instance.
	 * <p/>
	 * Differs from {@linkplain #find(Class, Object)} in that this form accepts
	 * the entity name of a {@linkplain org.hibernate.metamodel.RepresentationMode#MAP dynamic entity}.
	 *
	 * @see #find(Class, Object)
	 */
	Object find(String entityName, Object primaryKey);

	/**
	 * Return the persistent instance of the named entity type with the given identifier
	 * using the specified options, or null if there is no such persistent instance.
	 * <p/>
	 * Differs from {@linkplain #find(Class, Object, FindOption...)} in that this form accepts
	 * the entity name of a {@linkplain org.hibernate.metamodel.RepresentationMode#MAP dynamic entity}.
	 *
	 * @see #find(Class, Object, FindOption...)
	 */
	Object find(String entityName, Object primaryKey, FindOption... options);

	/**
	 * Return the persistent instances of the given entity class with the given identifiers
	 * as a list. The position of an instance in the returned list matches the position of its
	 * identifier in the given list of identifiers, and the returned list contains a null value
	 * if there is no persistent instance matching a given identifier. If an instance is already
	 * associated with the session, that instance is returned. This method never returns an
	 * uninitialized instance.
	 * <p>
	 * Every object returned by {@code findMultiple()} is either an unproxied instance of the
	 * given entity class, or a fully-fetched proxy object.
	 * <p>
	 * This method accepts {@link BatchSize} as an option, allowing control over the number of
	 * records retrieved in a single database request. The performance impact of setting a batch
	 * size depends on whether a SQL array may be used to pass the list of identifiers to the
	 * database:
	 * <ul>
	 * <li>for databases which {@linkplain org.hibernate.dialect.Dialect#supportsStandardArrays
	 *     support standard SQL arrays}, a smaller batch size might be extremely inefficient
	 *     compared to a very large batch size or no batching at all, but
	 * <li>on the other hand, for databases with no SQL array type, a large batch size results
	 *     in long SQL statements with many JDBC parameters.
	 * </ul>
	 *
	 * @param entityType the entity type
	 * @param ids the list of identifiers
	 * @param options options, if any
	 *
	 * @return an ordered list of persistent instances, with null elements representing missing
	 *         entities, whose positions in the list match the positions of their ids in the
	 *         given list of identifiers
	 *
	 * @see MultiFindOption
	 *
	 * @since 7.0
	 */
	<E> List<E> findMultiple(Class<E> entityType, List<?> ids, FindOption... options);

	/**
	 * Return the persistent instances of the root entity of the given {@link EntityGraph}
	 * with the given identifiers as a list, fetching the associations specified by the
	 * graph, which is interpreted as a {@linkplain org.hibernate.graph.GraphSemantic#LOAD
	 * load graph}. The position of an instance in the returned list matches the position of
	 * its identifier in the given list of identifiers, and the returned list contains a null
	 * value if there is no persistent instance matching a given identifier. If an instance
	 * is already associated with the session, that instance is returned. This method never
	 * returns an uninitialized instance.
	 * <p>
	 * Every object returned by {@code findMultiple()} is either an unproxied instance of the
	 * given entity class, or a fully-fetched proxy object.
	 * <p>
	 * This method accepts {@link BatchSize} as an option, allowing control over the number of
	 * records retrieved in a single database request. The performance impact of setting a batch
	 * size depends on whether a SQL array may be used to pass the list of identifiers to the
	 * database:
	 * <ul>
	 * <li>for databases which {@linkplain org.hibernate.dialect.Dialect#supportsStandardArrays
	 *     support standard SQL arrays}, a smaller batch size might be extremely inefficient
	 *     compared to a very large batch size or no batching at all, but
	 * <li>on the other hand, for databases with no SQL array type, a large batch size results
	 *     in long SQL statements with many JDBC parameters.
	 * </ul>
	 *
	 * @param entityGraph the entity graph interpreted as a load graph
	 * @param ids the list of identifiers
	 * @param options options, if any
	 *
	 * @return an ordered list of persistent instances, with null elements representing missing
	 *         entities, whose positions in the list match the positions of their ids in the
	 *         given list of identifiers
	 *
	 * @see MultiFindOption
	 *
	 * @since 7.0
	 */
	<E> List<E> findMultiple(EntityGraph<E> entityGraph, List<?> ids, FindOption... options);

	/**
	 * Read the persistent state associated with the given identifier into the given
	 * transient instance.
	 *
	 * @param object a transient instance of an entity class
	 * @param id an identifier
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
	 * @deprecated With no real replacement. For some use cases try
	 *             {@link StatelessSession#upsert(Object)}.
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
	 * @deprecated With no real replacement. For some use cases try
	 *             {@link StatelessSession#upsert(Object)}.
	 */
	@Deprecated( since = "6.0" )
	void replicate(String entityName, Object object, ReplicationMode replicationMode) ;

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
	@Override
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
	 * Copy the state of the given object onto the persistent object with the same
	 * identifier. If there is no persistent instance currently associated with
	 * the session, it will be loaded. Return the persistent instance. If the
	 * given instance is unsaved, save a copy and return it as a newly persistent
	 * instance. The given instance does not become associated with the session.
	 * This operation cascades to associated instances if the association is mapped
	 * with {@link jakarta.persistence.CascadeType#MERGE}.
	 *
	 * @param object a detached instance with state to be copied
	 * @param loadGraph entity graph interpreted as a load graph
	 *
	 * @return an updated persistent instance
	 */
	<T> T merge( T object, EntityGraph<?> loadGraph);

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
	@Override
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
	 * @param object a persistent instance associated with this session
	 * @param lockMode the lock level
	 *
	 * @see #lock(Object, LockModeType)
	 */
	void lock(Object object, LockMode lockMode);

	/**
	 * Obtain the specified lock level on the given managed instance associated
	 * with this session, applying any other specified options. This operation may
	 * be used to:
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
	 * @param object a persistent instance associated with this session
	 * @param lockMode the lock level
	 *
	 * @see #lock(Object, LockModeType, LockOption...)
	 */
	void lock(Object object, LockMode lockMode, LockOption... lockOptions);

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
	 * call {@link #refresh(Object, RefreshOption...)}, passing the appropriate
	 * {@link LockMode} as an option.
	 *
	 * @param object a persistent instance associated with this session
	 */
	@Override
	void refresh(Object object);

	/**
	 * Mark a persistence instance associated with this session for removal from
	 * the underlying database. Ths operation cascades to associated instances if
	 * the association is mapped {@link jakarta.persistence.CascadeType#REMOVE}.
	 * <p>
	 * Except when operating in fully JPA-compliant mode, this operation does,
	 * contrary to the JPA specification, accept a detached entity instance.
	 *
	 * @param object the managed persistent instance to remove, or a detached
	 *               instance unless operating in fully JPA-compliant mode
	 */
	@Override
	void remove(Object object);

	/**
	 * Determine the current {@linkplain LockMode lock mode} held on the given
	 * managed instance associated with this session.
	 * <p>
	 * Unlike the JPA-standard {@link #getLockMode}, this operation may be
	 * called when no transaction is active, in which case it should return
	 * {@link LockMode#NONE}, indicating that no pessimistic lock is held on
	 * the given entity.
	 *
	 * @param object a persistent instance associated with this session
	 *
	 * @return the lock mode currently held on the given entity
	 *
	 * @throws IllegalStateException if the given instance is not associated
	 *                               with this persistence context
	 * @throws ObjectDeletedException if the given instance was already
	 *                                {@linkplain #remove removed}
	 */
	LockMode getCurrentLockMode(Object object);

	/**
	 * Completely clear the persistence context. Evict all loaded instances,
	 * causing every managed entity currently associated with this session to
	 * transition to the detached state, and cancel all pending insertions,
	 * updates, and deletions.
	 * <p>
	 * Does not close open iterators or instances of {@link ScrollableResults}.
	 */
	@Override
	void clear();

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * or null if there is no such persistent instance. If the instance is already associated
	 * with the session, return that instance. This method never returns an uninitialized
	 * instance.
	 * <p>
	 * The object returned by {@code get()} or {@code find()} is either an unproxied instance
	 * of the given entity class, or a fully-fetched proxy object.
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
	 * <li>{@linkplain #setCacheMode set the cache mode} to {@link CacheMode#IGNORE}
	 *     before calling this method.
	 * </ul>
	 *
	 * @apiNote This operation is very similar to {@link #find(Class, Object)}.
	 *
	 * @param entityType the entity type
	 * @param id an identifier
	 *
	 * @return a persistent instance or null
	 *
	 * @deprecated Because the semantics of this method may change in a future release.
	 *             Use {@link #find(Class, Object)} instead.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	<T> T get(Class<T> entityType, Object id);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * or null if there is no such persistent instance. If the instance is already associated
	 * with the session, return that instance. This method never returns an uninitialized
	 * instance. Obtain the specified lock mode if the instance exists.
	 *
	 * @apiNote This operation is very similar to {@link #find(Class, Object, LockModeType)}.
	 *
	 * @param entityType the entity type
	 * @param id an identifier
	 * @param lockMode the lock mode
	 *
	 * @return a persistent instance or null
	 *
	 * @deprecated Use {@link #find(Class, Object, FindOption...)} instead.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	<T> T get(Class<T> entityType, Object id, LockMode lockMode);

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
	 *
	 * @deprecated The semantics of this method may change in a future release.
	 *             Use {@link SessionFactory#createGraphForDynamicEntity(String)}
	 *             together with {@link #find(EntityGraph, Object, FindOption...)}
	 *             to load {@link org.hibernate.metamodel.RepresentationMode#MAP
	 *             dynamic entities}.
	 *
	 * @see SessionFactory#createGraphForDynamicEntity(String)
	 * @see #find(EntityGraph, Object, FindOption...)
	 */
	@Deprecated(since = "7", forRemoval = true)
	Object get(String entityName, Object id);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * or null if there is no such persistent instance. If the instance is already associated
	 * with the session, return that instance. This method never returns an uninitialized
	 * instance. Obtain the specified lock mode if the instance exists.
	 *
	 * @param entityName the entity name
	 * @param id an identifier
	 * @param lockMode the lock mode
	 *
	 * @return a persistent instance or null
	 *
	 * @see #get(String, Object, LockOptions)
	 *
	 * @deprecated The semantics of this method may change in a future release.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	Object get(String entityName, Object id, LockMode lockMode);

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
	 *
	 * @deprecated This method will be removed.
	 *             Use {@link #find(Class, Object, FindOption...)} instead.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	<T> T get(Class<T> entityType, Object id, LockOptions lockOptions);

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
	 *
	 * @deprecated This method will be removed.
	 *             Use {@link SessionFactory#createGraphForDynamicEntity(String)}
	 *             together with {@link #find(EntityGraph, Object, FindOption...)}
	 *             to load {@link org.hibernate.metamodel.RepresentationMode#MAP
	 *             dynamic entities}.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	Object get(String entityName, Object id, LockOptions lockOptions);

	/**
	 * Obtain a lock on the given managed instance associated with this session,
	 * using the given {@linkplain LockOptions lock options}.
	 * <p>
	 * This operation cascades to associated instances if the association is
	 * mapped with {@link org.hibernate.annotations.CascadeType#LOCK}.
	 *
	 * @param object a persistent instance associated with this session
	 * @param lockOptions the lock options
	 *
	 * @since 6.2
	 *
	 * @deprecated This method will be removed.
	 *             Use {@linkplain #lock(Object, LockModeType, LockOption...)} instead
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	void lock(Object object, LockOptions lockOptions);

	/**
	 * Reread the state of the given managed instance from the underlying database,
	 * obtaining the given {@link LockMode}.
	 *
	 * @param object a persistent instance associated with this session
	 * @param lockOptions contains the lock mode to use
	 *
	 * @deprecated This method will be removed.
	 *             Use {@linkplain #refresh(Object, RefreshOption...)} instead
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	void refresh(Object object, LockOptions lockOptions);

	/**
	 * Return the entity name for the given persistent entity.
	 * <p>
	 * If the given entity is an uninitialized proxy, the proxy is initialized by
	 * side effect.
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
	@Override
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
	 *
	 * @deprecated This method will be removed.
	 *             Use {@link #find(Class, Object, FindOption...)} instead.
	 *             See {@link FindOption}.
	 */
	@Deprecated(since = "7.1", forRemoval = true)
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
	 *
	 * @deprecated This method will be removed.
	 *             Use {@link #find(String, Object, FindOption...)} instead.
	 *             See {@link FindOption}.
	 */
	@Deprecated(since = "7.1", forRemoval = true)
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
	 *
	 * @see #findMultiple(Class, List, FindOption...)
	 *
	 * @deprecated Use {@link #findMultiple(Class, List, FindOption...)} instead.
	 */
	@Deprecated(since = "7.2", forRemoval = true)
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
	 *
	 * @deprecated Use {@link #findMultiple(EntityGraph, List, FindOption...)} instead,
	 * 		with {@linkplain SessionFactory#createGraphForDynamicEntity(String)}.
	 */
	@Deprecated(since = "7.2", forRemoval = true)
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
	 * the instance is never dirty-checked, and mutations to the fields of the
	 * entity are not made persistent.
	 * <p>
	 * If the entity or proxy already has the specified read-only/modifiable
	 * setting, then this method does nothing.
	 * <p>
	 * To set the default read-only/modifiable setting used for all entities
	 * and proxies that are loaded into the session use
	 * {@link #setDefaultReadOnly(boolean)}.
	 * <p>
	 * To override the default read-only mode of the current session for
	 * all entities and proxies returned by a given {@code Query}, use
	 * {@link Query#setReadOnly(boolean)}.
	 * <p>
	 * Every instance of an {@linkplain org.hibernate.annotations.Immutable
	 * immutable} entity is loaded in read-only mode. An immutable entity may
	 * not be set to modifiable.
	 *
	 * @see #setDefaultReadOnly(boolean)
	 * @see Query#setReadOnly(boolean)
	 * @see IdentifierLoadAccess#withReadOnly(boolean)
	 * @see org.hibernate.annotations.Immutable
	 *
	 * @param entityOrProxy an entity or proxy
	 * @param readOnly {@code true} if the entity or proxy should be made read-only;
	 *				   {@code false} if the entity or proxy should be made modifiable
	 *
	 * @throws IllegalStateException if an immutable entity is set to modifiable
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
	 * Obtain a {@linkplain LobHelper} for instances of {@link java.sql.Blob}
	 * and {@link java.sql.Clob}.
	 *
	 * @return an instance of {@link LobHelper}
	 *
	 * @deprecated Use {@link Hibernate#getLobHelper()} instead.
	 */
	@Deprecated(since="7.0", forRemoval = true)
	LobHelper getLobHelper();

	/**
	 * Obtain the collection of all managed entities which belong to this
	 * persistence context.
	 *
	 * @since 7.0
	 */
	@Incubating
	Collection<?> getManagedEntities();

	/**
	 * Obtain a collection of all managed instances of the entity type with the
	 * given entity name which belong to this persistence context.
	 *
	 * @since 7.0
	 */
	@Incubating
	Collection<?> getManagedEntities(String entityName);

	/**
	 * Obtain a collection of all managed entities of the given type which belong
	 * to this persistence context. This operation is not polymorphic, and does
	 * not return instances of subtypes of the given entity type.
	 *
	 * @since 7.0
	 */
	@Incubating
	<E> Collection<E> getManagedEntities(Class<E> entityType);

	/**
	 * Obtain a collection of all managed entities of the given type which belong
	 * to this persistence context. This operation is not polymorphic, and does
	 * not return instances of subtypes of the given entity type.
	 *
	 * @since 7.0
	 */
	@Incubating
	<E> Collection<E> getManagedEntities(EntityType<E> entityType);

	/**
	 * Add one or more listeners to the Session
	 *
	 * @param listeners the listener(s) to add
	 */
	void addEventListeners(SessionEventListener... listeners);

	/**
	 * Set a hint. The hints understood by Hibernate are enumerated by
	 * {@link org.hibernate.jpa.AvailableHints}.
	 *
	 * @see org.hibernate.jpa.HibernateHints
	 * @see org.hibernate.jpa.SpecHints
	 *
	 * @apiNote Hints are a
	 * {@linkplain jakarta.persistence.EntityManager#setProperty
	 * JPA-standard way} to control provider-specific behavior of the
	 * {@code EntityManager}. Clients of the native API defined by
	 * Hibernate should make use of type-safe operations of this
	 * interface. For example, {@link #enableFetchProfile(String)}
	 * should be used in preference to the hint
	 * {@link org.hibernate.jpa.HibernateHints#HINT_FETCH_PROFILE}.
	 */
	@Override
	void setProperty(String propertyName, Object value);

	/**
	 * Create a new mutable instance of {@link EntityGraph}, with only
	 * a root node, allowing programmatic definition of the graph from
	 * scratch.
	 *
	 * @param rootType The root entity of the graph
	 *
	 * @see #find(EntityGraph, Object, FindOption...)
	 * @see org.hibernate.query.SelectionQuery#setEntityGraph(EntityGraph, org.hibernate.graph.GraphSemantic)
	 * @see org.hibernate.graph.EntityGraphs#createGraph(jakarta.persistence.metamodel.EntityType)
	 */
	@Override
	<T> RootGraph<T> createEntityGraph(Class<T> rootType);

	/**
	 * Create a new mutable instance of {@link EntityGraph}, based on
	 * a predefined {@linkplain jakarta.persistence.NamedEntityGraph
	 * named entity graph}, allowing customization of the graph, or
	 * return {@code null} if there is no predefined graph with the
	 * given name.
	 *
	 * @param graphName The name of the predefined named entity graph
	 *
	 * @apiNote This method returns {@code RootGraph<?>}, requiring an
	 * unchecked typecast before use. It's cleaner to obtain a graph using
	 * {@link #createEntityGraph(Class, String)} instead.
	 *
	 * @see #find(EntityGraph, Object, FindOption...)
	 * @see org.hibernate.query.SelectionQuery#setEntityGraph(EntityGraph, org.hibernate.graph.GraphSemantic)
	 * @see jakarta.persistence.EntityManagerFactory#addNamedEntityGraph(String, EntityGraph)
	 */
	@Override
	RootGraph<?> createEntityGraph(String graphName);

	/**
	 * Obtain an immutable reference to a predefined
	 * {@linkplain jakarta.persistence.NamedEntityGraph named entity graph}
	 * or return {@code null} if there is no predefined graph with the given
	 * name.
	 *
	 * @param graphName The name of the predefined named entity graph
	 *
	 * @apiNote This method returns {@code RootGraph<?>}, requiring an
	 * unchecked typecast before use. It's cleaner to obtain a graph using
	 * the static metamodel for the class which defines the graph, or by
	 * calling {@link SessionFactory#getNamedEntityGraphs(Class)} instead.
	 *
	 * @see #find(EntityGraph, Object, FindOption...)
	 * @see org.hibernate.query.SelectionQuery#setEntityGraph(EntityGraph, org.hibernate.graph.GraphSemantic)
	 * @see jakarta.persistence.EntityManagerFactory#addNamedEntityGraph(String, EntityGraph)
	 */
	@Override
	RootGraph<?> getEntityGraph(String graphName);

	/**
	 * Retrieve all named {@link EntityGraph}s with the given root entity type.
	 *
	 * @see jakarta.persistence.EntityManagerFactory#getNamedEntityGraphs(Class)
	 * @see jakarta.persistence.EntityManagerFactory#addNamedEntityGraph(String, EntityGraph)
	 */
	@Override
	<T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass);

	// The following overrides should not be necessary,
	// and are only needed to work around a bug in IntelliJ

	@Override
	<R> Query<R> createQuery(String queryString, Class<R> resultClass);

	@Override
	<R> Query<R> createQuery(TypedQueryReference<R> typedQueryReference);

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

	@Override
	default <T> T unwrap(Class<T> type) {
		return SharedSessionContract.super.unwrap(type);
	}
}
