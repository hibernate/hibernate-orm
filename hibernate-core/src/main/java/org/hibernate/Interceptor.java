/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;
import java.util.Iterator;

import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.type.Type;

/**
 * Allows user code to inspect and/or change entity property values before they are
 * written to the database, or after they are read from the database.
 * <ul>
 * <li>For a {@linkplain Session stateful session}, the callbacks {@link #onLoad},
 *     {@link #onPersist}, {@link #onRemove}, and {@link #onFlushDirty} reflect the
 *     basic lifecycle of a managed entity.
 * <li>For a {@linkplain StatelessSession stateless session}, the relevant callbacks
 *     are {@link #onLoad}, {@link #onInsert}, {@link #onUpdate}, {@link #onUpsert}, and
 *     {@link #onDelete(Object entity, Object id, String[] propertyNames, Type[] propertyTypes)
 *     onDelete}.
 * </ul>
 * <p>
 * The {@link Session} may not be invoked from a callback (nor may a callback cause
 * a collection or proxy to be lazily initialized).
 * <p>
 * There might be a single instance of {@code Interceptor} for a {@link SessionFactory},
 * or a new instance might be created for each {@link Session}. Use:
 * <ul>
 *     <li>{@link org.hibernate.cfg.AvailableSettings#INTERCEPTOR} to specify an
 *         interceptor shared between sessions, or
 *     <li>{@link org.hibernate.cfg.AvailableSettings#SESSION_SCOPED_INTERCEPTOR} to
 *         specify that there is a dedicated instance of the interceptor for each
 *         session.
 * </ul>
 * <p>
 * Whichever approach is used, the interceptor must be serializable for the
 * {@code Session} to be serializable. This means that {@code SessionFactory}-scoped
 * interceptors should implement {@code readResolve()}.
 * <p>
 * This venerable callback interface, dating to the very earliest days of Hibernate,
 * competes with JPA entity listener callbacks: {@link jakarta.persistence.PostLoad},
 * {@link jakarta.persistence.PrePersist} {@link jakarta.persistence.PreUpdate}, and
 * {@link jakarta.persistence.PreRemove}.
 *
 * @see SessionBuilder#interceptor(Interceptor)
 * @see SharedSessionBuilder#interceptor()
 * @see org.hibernate.cfg.Configuration#setInterceptor(Interceptor)
 *
 * @see org.hibernate.boot.SessionFactoryBuilder#applyInterceptor(Interceptor)
 * @see org.hibernate.boot.SessionFactoryBuilder#applyStatelessInterceptor(Class)
 *
 * @author Gavin King
 */
public interface Interceptor {
	/**
	 * Called just before an object is initialized. The interceptor may change the {@code state}, which will
	 * be propagated to the persistent object. Note that when this method is called, {@code entity} will be
	 * an empty uninitialized instance of the class.
	 *
	 * @apiNote The indexes across the {@code state}, {@code propertyNames}, and {@code types} arrays match.
	 *
	 * @param entity The entity instance being loaded
	 * @param id The identifier value being loaded
	 * @param state The entity state (which will be pushed into the entity instance)
	 * @param propertyNames The names of the entity properties, corresponding to the {@code state}.
	 * @param types The types of the entity properties, corresponding to the {@code state}.
	 *
	 * @return {@code true} if the user modified the {@code state} in any way.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 */
	default boolean onLoad(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types)
			throws CallbackException {
		if (id==null || id instanceof Serializable) {
			return onLoad(entity, (Serializable) id, state, propertyNames, types);
		}
		return false;
	}

	/**
	 * Called just before an object is initialized. The interceptor may change the {@code state}, which will
	 * be propagated to the persistent object. Note that when this method is called, {@code entity} will be
	 * an empty uninitialized instance of the class.
	 *
	 * @apiNote The indexes across the {@code state}, {@code propertyNames}, and {@code types} arrays match.
	 *
	 * @param entity The entity instance being loaded
	 * @param id The identifier value being loaded
	 * @param state The entity state (which will be pushed into the entity instance)
	 * @param propertyNames The names of the entity properties, corresponding to the {@code state}.
	 * @param types The types of the entity properties, corresponding to the {@code state}.
	 *
	 * @return {@code true} if the user modified the {@code state} in any way.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 *
	 * @deprecated use {@link #onLoad(Object, Object, Object[], String[], Type[])}
	 */
	@Deprecated(since = "6.0")
	default boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types)
			throws CallbackException {
		return false;
	}

	/**
	 * Called before an object is made persistent by a stateful session.
	 * <p>
	 * The interceptor may modify the {@code state}, which will be used for
	 * the SQL {@code INSERT} and propagated to the persistent object.
	 *
	 * @param entity The entity instance whose state is being inserted
	 * @param id The identifier of the entity
	 * @param state The state of the entity which will be inserted
	 * @param propertyNames The names of the entity properties.
	 * @param types The types of the entity properties
	 *
	 * @return {@code true} if the user modified the {@code state} in any way.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 *
	 * @see Session#persist(Object)
	 * @see Session#merge(Object)
	 */
	default boolean onPersist(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types)
			throws CallbackException {
		return onSave(entity, id, state, propertyNames, types);
	}

	/**
	 *  Called before an object is removed by a stateful session.
	 *  <p>
	 *  It is not recommended that the interceptor modify the {@code state}.
	 *
	 * @param entity The entity instance being deleted
	 * @param id The identifier of the entity
	 * @param state The state of the entity
	 * @param propertyNames The names of the entity properties.
	 * @param types The types of the entity properties
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 *
	 * @see Session#remove(Object)
	 */
	default void onRemove(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types)
			throws CallbackException {
		onDelete(entity, id, state, propertyNames, types);
	}

	/**
	 * Called when an object is detected to be dirty, during a flush. The interceptor may modify the detected
	 * {@code currentState}, which will be propagated to both the database and the persistent object.
	 * Note that not all flushes end in actual synchronization with the database, in which case the
	 * new {@code currentState} will be propagated to the object, but not necessarily (immediately) to
	 * the database. It is strongly recommended that the interceptor <b>not</b> modify the {@code previousState}.
	 *
	 * @apiNote The indexes across the {@code currentState}, {@code previousState}, {@code propertyNames}, and
	 *          {@code types} arrays match.
	 *
	 * @param entity The entity instance detected as being dirty and being flushed
	 * @param id The identifier of the entity
	 * @param currentState The entity's current state
	 * @param previousState The entity's previous (load time) state.
	 * @param propertyNames The names of the entity properties
	 * @param types The types of the entity properties
	 *
	 * @return {@code true} if the user modified the {@code currentState} in any way.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 *
	 * @see Session#flush()
	 */
	default boolean onFlushDirty(
			Object entity,
			Object id,
			Object[] currentState,
			Object[] previousState,
			String[] propertyNames,
			Type[] types) throws CallbackException {
		if (id==null || id instanceof Serializable) {
			return onFlushDirty(entity, (Serializable) id, currentState, previousState, propertyNames, types);
		}
		return false;
	}

	/**
	 * Called when an object is detected to be dirty, during a flush. The interceptor may modify the detected
	 * {@code currentState}, which will be propagated to both the database and the persistent object.
	 * Note that not all flushes end in actual synchronization with the database, in which case the
	 * new {@code currentState} will be propagated to the object, but not necessarily (immediately) to
	 * the database. It is strongly recommended that the interceptor <b>not</b> modify the {@code previousState}.
	 *
	 * @apiNote The indexes across the {@code currentState}, {@code previousState}, {@code propertyNames}, and
	 *          {@code types} arrays match.
	 *
	 * @param entity The entity instance detected as being dirty and being flushed
	 * @param id The identifier of the entity
	 * @param currentState The entity's current state
	 * @param previousState The entity's previous (load time) state.
	 * @param propertyNames The names of the entity properties
	 * @param types The types of the entity properties
	 *
	 * @return {@code true} if the user modified the {@code currentState} in any way.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 *
	 * @deprecated use {@link #onFlushDirty(Object, Object, Object[], Object[], String[], Type[])}
	 */
	@Deprecated(since = "6.0")
	default boolean onFlushDirty(
			Object entity,
			Serializable id,
			Object[] currentState,
			Object[] previousState,
			String[] propertyNames,
			Type[] types) throws CallbackException {
		return false;
	}

	/**
	 * Called before an object is made persistent by a stateful session.
	 * <p>
	 * The interceptor may modify the {@code state}, which will be used for
	 * the SQL {@code INSERT} and propagated to the persistent object.
	 *
	 * @param entity The entity instance whose state is being inserted
	 * @param id The identifier of the entity
	 * @param state The state of the entity which will be inserted
	 * @param propertyNames The names of the entity properties.
	 * @param types The types of the entity properties
	 *
	 * @return {@code true} if the user modified the {@code state} in any way.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 *
	 * @deprecated use {@link #onSave(Object, Object, Object[], String[], Type[])}
	 */
	@Deprecated(since = "6.0")
	default boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types)
			throws CallbackException {
		return false;
	}

	/**
	 * Called before an object is made persistent by a stateful session.
	 * <p>
	 * The interceptor may modify the {@code state}, which will be used for
	 * the SQL {@code INSERT} and propagated to the persistent object.
	 *
	 * @param entity The entity instance whose state is being inserted
	 * @param id The identifier of the entity
	 * @param state The state of the entity which will be inserted
	 * @param propertyNames The names of the entity properties.
	 * @param types The types of the entity properties
	 *
	 * @return {@code true} if the user modified the {@code state} in any way.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 *
	 * @see Session#persist(Object)
	 * @see Session#merge(Object)
	 * @see Session#save(Object)
	 *
	 * @deprecated Use {@link #onPersist(Object, Object, Object[], String[], Type[])}
	 */
	@Deprecated(since = "6.6")
	default boolean onSave(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types)
			throws CallbackException {
		if (id==null || id instanceof Serializable) {
			return onSave(entity, (Serializable) id, state, propertyNames, types);
		}
		return false;
	}

	/**
	 *  Called before an object is removed by a stateful session.
	 *  <p>
	 *  It is not recommended that the interceptor modify the {@code state}.
	 *
	 * @param entity The entity instance being deleted
	 * @param id The identifier of the entity
	 * @param state The state of the entity
	 * @param propertyNames The names of the entity properties.
	 * @param types The types of the entity properties
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 *
	 * @deprecated use {@link #onDelete(Object, Object, Object[], String[], Type[])}
	 */
	@Deprecated(since = "6.0")
	default void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types)
			throws CallbackException {}

	/**
	 *  Called before an object is removed by a stateful session.
	 *  <p>
	 *  It is not recommended that the interceptor modify the {@code state}.
	 *
	 * @param entity The entity instance being deleted
	 * @param id The identifier of the entity
	 * @param state The state of the entity
	 * @param propertyNames The names of the entity properties.
	 * @param types The types of the entity properties
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 *
	 * @see Session#remove(Object)
	 * @see Session#delete(Object)
	 *
	 * @deprecated Use {@link #onRemove(Object, Object, Object[], String[], Type[])}
	 */
	@Deprecated(since = "6.6")
	default void onDelete(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types)
			throws CallbackException {
		if (id==null || id instanceof Serializable) {
			onDelete(entity, (Serializable) id, state, propertyNames, types);
		}
	}

	/**
	 * Called before a collection is (re)created.
	 *
	 * @param collection The collection instance.
	 * @param key The collection key value.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 *
	 * @deprecated use {@link #onCollectionRecreate(Object, Object)}
	 */
	@Deprecated(since = "6.0")
	default void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {}

	/**
	 * Called before a collection is (re)created.
	 *
	 * @param collection The collection instance.
	 * @param key The collection key value.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 */
	default void onCollectionRecreate(Object collection, Object key) throws CallbackException {
		if (key instanceof Serializable) {
			onCollectionRecreate(collection, (Serializable) key);
		}
	}

	/**
	 * Called before a collection is deleted.
	 *
	 * @param collection The collection instance.
	 * @param key The collection key value.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 *
	 * @deprecated use {@link #onCollectionRemove(Object, Object)}
	 */
	@Deprecated(since = "6.0")
	default void onCollectionRemove(Object collection, Serializable key) throws CallbackException {}

	/**
	 * Called before a collection is deleted.
	 *
	 * @param collection The collection instance.
	 * @param key The collection key value.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 */
	default void onCollectionRemove(Object collection, Object key) throws CallbackException {
		if (key instanceof Serializable) {
			onCollectionRemove(collection, (Serializable) key);
		}
	}

	/**
	 * Called before a collection is updated.
	 *
	 * @param collection The collection instance.
	 * @param key The collection key value.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 * 
	 * @deprecated use {@link #onCollectionUpdate(Object, Object)}
	 */
	@Deprecated(since = "6.0")
	default void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {}

	/**
	 * Called before a collection is updated.
	 *
	 * @param collection The collection instance.
	 * @param key The collection key value.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 */
	default void onCollectionUpdate(Object collection, Object key) throws CallbackException {
		if (key instanceof Serializable) {
			onCollectionUpdate(collection, (Serializable) key);
		}
	}
	/**
	 * Called before a flush.
	 *
	 * @param entities The entities to be flushed.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 */
	default void preFlush(Iterator<Object> entities) throws CallbackException {}

	/**
	 * Called after a flush that actually ends in execution of the SQL statements required to synchronize
	 * in-memory state with the database.
	 *
	 * @param entities The entities that were flushed.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 */
	default void postFlush(Iterator<Object> entities) throws CallbackException {}

	/**
	 * Called to distinguish between transient and detached entities. The return value determines the
	 * state of the entity with respect to the current session.
	 * <ul>
	 * <li>{@code Boolean.TRUE} - the entity is transient
	 * <li>{@code Boolean.FALSE} - the entity is detached
	 * <li>{@code null} - Hibernate uses the {@code unsaved-value} mapping and other heuristics to 
	 * determine if the object is unsaved
	 * </ul>
	 *
	 * @param entity a transient or detached entity
	 * @return Boolean or {@code null} to choose default behaviour
	 */
	default Boolean isTransient(Object entity) {
		return null;
	}

	/**
	 * Called from {@code flush()}. The return value determines whether the entity is updated
	 * <ul>
	 * <li>an array of property indices - the entity is dirty
	 * <li>an empty array - the entity is not dirty
	 * <li>{@code null} - use Hibernate's default dirty-checking algorithm
	 * </ul>
	 *
	 * @param entity The entity for which to find dirty properties.
	 * @param id The identifier of the entity
	 * @param currentState The current entity state as taken from the entity instance
	 * @param previousState The state of the entity when it was last synchronized (generally when it was loaded)
	 * @param propertyNames The names of the entity properties.
	 * @param types The types of the entity properties
	 *
	 * @return array of dirty property indices or {@code null} to indicate Hibernate should perform default behaviour
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 * 
	 * @deprecated use {@link #findDirty(Object, Object, Object[], Object[], String[], Type[])}
	 */
	@Deprecated(since = "6.0")
	default int[] findDirty(
			Object entity,
			Serializable id,
			Object[] currentState,
			Object[] previousState,
			String[] propertyNames,
			Type[] types) {
		return null;
	}

	/**
	 * Called from {@code flush()}. The return value determines whether the entity is updated
	 * <ul>
	 * <li>an array of property indices - the entity is dirty
	 * <li>an empty array - the entity is not dirty
	 * <li>{@code null} - use Hibernate's default dirty-checking algorithm
	 * </ul>
	 *
	 * @param entity The entity for which to find dirty properties.
	 * @param id The identifier of the entity
	 * @param currentState The current entity state as taken from the entity instance
	 * @param previousState The state of the entity when it was last synchronized (generally when it was loaded)
	 * @param propertyNames The names of the entity properties.
	 * @param types The types of the entity properties
	 *
	 * @return array of dirty property indices or {@code null} to indicate Hibernate should perform default behaviour
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 */
	default int[] findDirty(
			Object entity,
			Object id,
			Object[] currentState,
			Object[] previousState,
			String[] propertyNames,
			Type[] types) {
		if (id==null || id instanceof Serializable) {
			return findDirty(entity, (Serializable) id, currentState, previousState, propertyNames, types);
		}
		return null;
	}

	/**
	 * Instantiate the entity. Return {@code null} to indicate that Hibernate should use
	 * the default constructor of the class. The identifier property of the returned instance
	 * should be initialized with the given identifier.
	 */
	default Object instantiate(
			String entityName,
			EntityRepresentationStrategy representationStrategy,
			Object id) throws CallbackException {
		return instantiate( entityName, representationStrategy.getMode(), id );
	}

	/**
	 * Instantiate the entity. Return {@code null} to indicate that Hibernate should use
	 * the default constructor of the class. The identifier property of the returned instance
	 * should be initialized with the given identifier.
	 */
	default Object instantiate(
			String entityName,
			RepresentationMode representationMode,
			Object id) throws CallbackException {
		return null;
	}

	/**
	 * Get the entity name for a persistent or transient instance.
	 *
	 * @param object an entity instance
	 *
	 * @return the name of the entity
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 *
	 * @see EntityNameResolver
	 */
	default String getEntityName(Object object) throws CallbackException {
		return null;
	}

	/**
	 * Get a fully loaded entity instance that is cached externally.
	 *
	 * @param entityName the name of the entity
	 * @param id the instance identifier
	 *
	 * @return a fully initialized entity
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 * 
	 * @deprecated use {@link #getEntity(String, Object)}
	 */
	@Deprecated(since = "6.0")
	default Object getEntity(String entityName, Serializable id) throws CallbackException {
		return null;
	}

	/**
	 * Get a fully loaded entity instance that is cached externally.
	 *
	 * @param entityName the name of the entity
	 * @param id the instance identifier
	 *
	 * @return a fully initialized entity
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 */
	default Object getEntity(String entityName, Object id) throws CallbackException {
		if (id==null || id instanceof Serializable) {
			return getEntity(entityName, (Serializable) id);
		}
		return null;
	}
	
	/**
	 * Called when a Hibernate transaction is begun via the Hibernate {@code Transaction} 
	 * API. Will not be called if transactions are being controlled via some other 
	 * mechanism (CMT, for example).
	 *
	 * @param tx The Hibernate transaction facade object
	 */
	default void afterTransactionBegin(Transaction tx) {}

	/**
	 * Called before a transaction is committed (but not before rollback).
	 *
	 * @param tx The Hibernate transaction facade object
	 */
	default void beforeTransactionCompletion(Transaction tx) {}

	/**
	 * Called after a transaction is committed or rolled back.
	 *
	 * @param tx The Hibernate transaction facade object
	 */
	default void afterTransactionCompletion(Transaction tx) {}

	/**
	 * Called before a record is inserted by a {@link StatelessSession}.
	 *
	 * @param entity The entity instance being deleted
	 * @param id The identifier of the entity
	 * @param state The entity state
	 * @param propertyNames The names of the entity properties.
	 * @param propertyTypes The types of the entity properties
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 *
	 * @see StatelessSession#insert(Object)
	 */
	default void onInsert(Object entity, Object id, Object[] state, String[] propertyNames, Type[] propertyTypes) {}

	/**
	 * Called before a record is updated by a {@link StatelessSession}.
	 *
	 * @param entity The entity instance being deleted
	 * @param id The identifier of the entity
	 * @param state The entity state
	 * @param propertyNames The names of the entity properties.
	 * @param propertyTypes The types of the entity properties
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 *
	 * @see StatelessSession#update(Object)
	 */
	default void onUpdate(Object entity, Object id, Object[] state, String[] propertyNames, Type[] propertyTypes) {}

	/**
	 * Called before a record is upserted by a {@link StatelessSession}.
	 *
	 * @param entity The entity instance being deleted
	 * @param id The identifier of the entity
	 * @param state The entity state
	 * @param propertyNames The names of the entity properties.
	 * @param propertyTypes The types of the entity properties
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 *
	 * @see StatelessSession#upsert(String, Object)
	 */
	default void onUpsert(Object entity, Object id, Object[] state, String[] propertyNames, Type[] propertyTypes) {}

	/**
	 * Called before a record is deleted by a {@link StatelessSession}.
	 *
	 * @param entity The entity instance being deleted
	 * @param id The identifier of the entity
	 * @param propertyNames The names of the entity properties.
	 * @param propertyTypes The types of the entity properties
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 *
	 * @see StatelessSession#delete(Object)
	 */
	default void onDelete(Object entity, Object id, String[] propertyNames, Type[] propertyTypes) {}
}
