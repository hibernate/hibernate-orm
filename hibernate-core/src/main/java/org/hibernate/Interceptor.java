/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

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
 *
 * @apiNote This venerable callback interface, dating from the very earliest days of
 *          Hibernate, competes with standard JPA entity listener callbacks:
 *          {@link jakarta.persistence.PostLoad}, {@link jakarta.persistence.PrePersist},
 *          {@link jakarta.persistence.PreUpdate}, and {@link jakarta.persistence.PreRemove}.
 *          However, JPA callbacks do not provide the ability to access the previous
 *          value of an updated property in a {@code @PreUpdate} callback, and do not
 *          provide a well-defined way to intercept changes to collections.
 *          <p>
 *          Note that this API exposes the interface {@link Type}, which in modern
 *          versions of Hibernate is considered an SPI. This is unfortunate, and might
 *          change in the future, but is bearable for now.
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
	 */
	default boolean onLoad(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
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
	 * @see Session#persist(Object)
	 * @see Session#merge(Object)
	 */
	default boolean onPersist(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
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
	 * @see Session#remove(Object)
	 */
	default void onRemove(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
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
	 * @see Session#flush()
	 */
	default boolean onFlushDirty(
			Object entity,
			Object id,
			Object[] currentState,
			Object[] previousState,
			String[] propertyNames,
			Type[] types) {
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
	 * @see Session#persist(Object)
	 * @see Session#merge(Object)
	 *
	 * @deprecated Use {@link #onPersist(Object, Object, Object[], String[], Type[])}
	 */
	@Deprecated(since = "6.6")
	default boolean onSave(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
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
	 * @see Session#remove(Object)
	 *
	 * @deprecated Use {@link #onRemove(Object, Object, Object[], String[], Type[])}
	 */
	@Deprecated(since = "6.6")
	default void onDelete(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
	}

	/**
	 * Called before a collection is (re)created.
	 *
	 * @param collection The collection instance.
	 * @param key The collection key value.
	 */
	default void onCollectionRecreate(Object collection, Object key) {
	}

	/**
	 * Called before a collection is deleted.
	 *
	 * @param collection The collection instance.
	 * @param key The collection key value.
	 */
	default void onCollectionRemove(Object collection, Object key) {
	}

	/**
	 * Called before a collection is updated.
	 *
	 * @param collection The collection instance.
	 * @param key The collection key value.
	 */
	default void onCollectionUpdate(Object collection, Object key) {
	}

	/**
	 * Called before a flush.
	 *
	 * @param entities The entities to be flushed.
	 */
	default void preFlush(Iterator<Object> entities) {}

	/**
	 * Called after a flush that actually ends in execution of the SQL statements
	 * required to synchronize in-memory state with the database.
	 *
	 * @param entities The entities that were flushed.
	 */
	default void postFlush(Iterator<Object> entities) {}

	/**
	 * Called to distinguish between transient and detached entities. The return
	 * value determines the state of the entity with respect to the current session.
	 * This method should return:
	 * <ul>
	 * <li>{@code Boolean.TRUE} if the entity is transient,
	 * <li>{@code Boolean.FALSE} if the entity is detached, or
	 * <li>{@code null} to signal that the usual heuristics should be used to determine
	 *     if the instance is transient
	 * </ul>
	 * Heuristics used when this method returns null are based on the value of the
	 * {@linkplain jakarta.persistence.GeneratedValue generated} id field, or the
	 * {@linkplain jakarta.persistence.Version version} field, if any.
	 *
	 * @param entity a transient or detached entity
	 * @return {@link Boolean} or {@code null} to choose default behaviour
	 */
	default Boolean isTransient(Object entity) {
		return null;
	}

	/**
	 * Called from {@code flush()}. The return value determines whether the entity
	 * is updated
	 * <ul>
	 * <li>an array of property indices - the entity is dirty
	 * <li>an empty array - the entity is not dirty
	 * <li>{@code null} - use Hibernate's default dirty-checking algorithm
	 * </ul>
	 *
	 * @param entity The entity for which to find dirty properties.
	 * @param id The identifier of the entity
	 * @param currentState The current entity state as taken from the entity instance
	 * @param previousState The state of the entity when it was last synchronized
	 *                      (generally when it was loaded)
	 * @param propertyNames The names of the entity properties.
	 * @param types The types of the entity properties
	 *
	 * @return array of dirty property indices or {@code null} to indicate Hibernate
	 *         should perform default behaviour
	 */
	default int[] findDirty(
			Object entity,
			Object id,
			Object[] currentState,
			Object[] previousState,
			String[] propertyNames,
			Type[] types) {
		return null;
	}

	/**
	 * Instantiate the entity. Return {@code null} to indicate that Hibernate should
	 * use the default constructor of the class. The identifier property of the
	 * returned instance should be initialized with the given identifier.
	 */
	default Object instantiate(
			String entityName,
			EntityRepresentationStrategy representationStrategy,
			Object id) {
		return instantiate( entityName, representationStrategy.getMode(), id );
	}

	/**
	 * Instantiate the entity. Return {@code null} to indicate that Hibernate should
	 * use the default constructor of the class. The identifier property of the
	 * returned instance should be initialized with the given identifier.
	 */
	default Object instantiate(
			String entityName,
			RepresentationMode representationMode,
			Object id) {
		return null;
	}

	/**
	 * Get the entity name for a persistent or transient instance.
	 *
	 * @param object an entity instance
	 *
	 * @return the name of the entity
	 *
	 * @see EntityNameResolver
	 */
	default String getEntityName(Object object) {
		return null;
	}

	/**
	 * Get a fully loaded entity instance that is cached externally.
	 *
	 * @param entityName the name of the entity
	 * @param id the instance identifier
	 *
	 * @return a fully initialized entity
	 */
	default Object getEntity(String entityName, Object id) {
		return null;
	}

	/**
	 * Called when a Hibernate transaction is begun via the JPA-standard
	 * {@link jakarta.persistence.EntityTransaction} API, or via {@link Transaction}.
	 * This method is not be called if transactions are being controlled via some
	 * other mechanism, for example, if transactions are managed by a container.
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
	 * @see StatelessSession#delete(Object)
	 */
	default void onDelete(Object entity, Object id, String[] propertyNames, Type[] propertyTypes) {}
}
