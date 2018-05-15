/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;
import java.util.Iterator;

import org.hibernate.metamodel.model.domain.RepresentationMode;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Allows user code to inspect and/or change property values.
 *
 * Inspection occurs before property values are written and after they are read
 * from the database.
 *
 * There might be a single instance of <tt>Interceptor</tt> for a <tt>SessionFactory</tt>, or a new instance
 * might be specified for each <tt>Session</tt>. Whichever approach is used, the interceptor must be
 * serializable if the <tt>Session</tt> is to be serializable. This means that <tt>SessionFactory</tt>-scoped
 * interceptors should implement <tt>readResolve()</tt>.
 *
 * The <tt>Session</tt> may not be invoked from a callback (nor may a callback cause a collection or proxy to
 * be lazily initialized).
 *
 * Instead of implementing this interface directly, it is usually better to extend <tt>EmptyInterceptor</tt>
 * and override only the callback methods of interest.
 *
 * @see SessionBuilder#interceptor(Interceptor)
 * @see SharedSessionBuilder#interceptor()
 * @see org.hibernate.cfg.Configuration#setInterceptor(Interceptor)
 * @see EmptyInterceptor
 *
 * @author Gavin King
 */
public interface Interceptor {
	/**
	 * Called just before an object is initialized. The interceptor may change the <tt>state</tt>, which will
	 * be propagated to the persistent object. Note that when this method is called, <tt>entity</tt> will be
	 * an empty uninitialized instance of the class.
	 * <p/>
	 * NOTE: The indexes across the <tt>state</tt>, <tt>propertyNames</tt> and <tt>types</tt> arrays match.
	 *
	 * @param entity The entity instance being loaded
	 * @param id The identifier value being loaded
	 * @param state The entity state (which will be pushed into the entity instance)
	 * @param propertyNames The names of the entity properties, corresponding to the <tt>state</tt>.
	 * @param javaTypeDescriptors The {@link JavaTypeDescriptor} of the entity properties, corresponding to the <tt>state</tt>.
	 *
	 * @return {@code true} if the user modified the <tt>state</tt> in any way.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 */
	default boolean onLoad(
			Object entity,
			Object id,
			Object[] state,
			String[] propertyNames,
			JavaTypeDescriptor[] javaTypeDescriptors) throws CallbackException {
		return onLoad( entity, (Serializable) id, state, propertyNames, javaTypeDescriptors );
	}

	/**
	 * @deprecated Use {@link #onLoad(Object, Object, Object[], String[], JavaTypeDescriptor[])} instead
	 */
	@Deprecated
	default boolean onLoad(
			Object entity,
			Serializable id,
			Object[] state,
			String[] propertyNames,
			JavaTypeDescriptor[] javaTypeDescriptors) throws CallbackException {
		return false;
	}

	/**
	 * Called when an object is detected to be dirty, during a flush. The interceptor may modify the detected
	 * <tt>currentState</tt>, which will be propagated to both the database and the persistent object.
	 * Note that not all flushes end in actual synchronization with the database, in which case the
	 * new <tt>currentState</tt> will be propagated to the object, but not necessarily (immediately) to
	 * the database. It is strongly recommended that the interceptor <b>not</b> modify the <tt>previousState</tt>.
	 * <p/>
	 * NOTE: The indexes across the <tt>currentState</tt>, <tt>previousState</tt>, <tt>propertyNames</tt> and
	 * <tt>types</tt> arrays match.
	 *
	 * @param entity The entity instance detected as being dirty and being flushed
	 * @param id The identifier of the entity
	 * @param currentState The entity's current state
	 * @param previousState The entity's previous (load time) state.
	 * @param propertyNames The names of the entity properties
	 * @param javaTypeDescriptors The {@link JavaTypeDescriptor} of the entity properties
	 *
	 * @return {@code true} if the user modified the <tt>currentState</tt> in any way.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 */
	default boolean onFlushDirty(
			Object entity,
			Object id,
			Object[] currentState,
			Object[] previousState,
			String[] propertyNames,
			JavaTypeDescriptor[] javaTypeDescriptors) throws CallbackException {
		return onFlushDirty(
				entity,
				(Serializable) id,
				currentState,
				previousState,
				propertyNames,
				javaTypeDescriptors
		);
	}

	/**
	 * @deprecated Use {@link #onFlushDirty(Object, Object, Object[], Object[], String[], JavaTypeDescriptor[])} instead
	 */
	@Deprecated
	default boolean onFlushDirty(
			Object entity,
			Serializable id,
			Object[] currentState,
			Object[] previousState,
			String[] propertyNames,
			JavaTypeDescriptor[] javaTypeDescriptors) throws CallbackException {
		return false;
	}

	/**
	 * Called before an object is saved. The interceptor may modify the <tt>state</tt>, which will be used for
	 * the SQL <tt>INSERT</tt> and propagated to the persistent object.
	 *
	 * @param entity The entity instance whose state is being inserted
	 * @param id The identifier of the entity
	 * @param state The state of the entity which will be inserted
	 * @param propertyNames The names of the entity properties.
	 * @param javaTypeDescriptors The {@link JavaTypeDescriptor} of the entity properties
	 *
	 * @return <tt>true</tt> if the user modified the <tt>state</tt> in any way.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 */
	default boolean onSave(
			Object entity,
			Object id,
			Object[] state,
			String[] propertyNames,
			JavaTypeDescriptor[] javaTypeDescriptors) throws CallbackException {
		return onSave( entity, (Serializable) id, state, propertyNames, javaTypeDescriptors );
	}

	/**
	 * @deprecated Use {@link #onSave(Object, Object, Object[], String[], JavaTypeDescriptor[])} instead
	 */
	@Deprecated
	default boolean onSave(
			Object entity,
			Serializable id,
			Object[] state,
			String[] propertyNames,
			JavaTypeDescriptor[] javaTypeDescriptors) throws CallbackException {
		return false;
	}

	/**
	 * Called before an object is deleted. It is not recommended that the interceptor modify the <tt>state</tt>.
	 *
	 * @param entity The entity instance being deleted
	 * @param id The identifier of the entity
	 * @param state The state of the entity
	 * @param propertyNames The names of the entity properties.
	 * @param javaTypeDescriptors The {@link JavaTypeDescriptor}  of the entity
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 */
	default void onDelete(
			Object entity,
			Object id,
			Object[] state,
			String[] propertyNames,
			JavaTypeDescriptor[] javaTypeDescriptors) throws CallbackException {
		onDelete( entity, (Serializable) id, state, propertyNames, javaTypeDescriptors );
	}

	/**
	 * @deprecated Use {@link #onDelete(Object, Object, Object[], String[], JavaTypeDescriptor[])} instead
	 */
	@Deprecated
	default void onDelete(
			Object entity,
			Serializable id,
			Object[] state,
			String[] propertyNames,
			JavaTypeDescriptor[] javaTypeDescriptors) throws CallbackException {
	}

	/**
	 * Called before a collection is (re)created.
	 *
	 * @param collection The collection instance.
	 * @param key The collection key value.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 */
	default void onCollectionRecreate(Object collection, Object key) throws CallbackException {
		onCollectionRecreate( collection, (Serializable) key );
	}

	/**
	 * @deprecated Use {@link #onCollectionRecreate(Object, Object)} instead
	 */
	@Deprecated
	default void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {
	}

	/**
	 * Called before a collection is deleted.
	 *
	 * @param collection The collection instance.
	 * @param key The collection key value.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 */
	default void onCollectionRemove(Object collection, Object key) throws CallbackException {
		onCollectionRemove( collection, (Serializable) key );
	}

	/**
	 * @deprecated Use {@link #onCollectionRemove(Object, Object)} instead
	 */
	@Deprecated
	default void onCollectionRemove(Object collection, Serializable key) throws CallbackException {
	}

	/**
	 * Called before a collection is updated.
	 *
	 * @param collection The collection instance.
	 * @param key The collection key value.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 */
	default void onCollectionUpdate(Object collection, Object key) throws CallbackException {
		onCollectionUpdate( collection, (Serializable) key );
	}

	/**
	 * @deprecated Use {@link #onCollectionUpdate(Object, Object)} instead
	 */
	@Deprecated
	default void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
	}

	/**
	 * Called before a flush.
	 *
	 * @param entities The entities to be flushed.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 */
	void preFlush(Iterator entities) throws CallbackException;

	/**
	 * Called after a flush that actually ends in execution of the SQL statements required to synchronize
	 * in-memory state with the database.
	 *
	 * @param entities The entities that were flushed.
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 */
	void postFlush(Iterator entities) throws CallbackException;

	/**
	 * Called to distinguish between transient and detached entities. The return value determines the
	 * state of the entity with respect to the current session.
	 * <ul>
	 * <li><tt>Boolean.TRUE</tt> - the entity is transient
	 * <li><tt>Boolean.FALSE</tt> - the entity is detached
	 * <li><tt>null</tt> - Hibernate uses the <tt>unsaved-value</tt> mapping and other heuristics to 
	 * determine if the object is unsaved
	 * </ul>
	 * @param entity a transient or detached entity
	 * @return Boolean or <tt>null</tt> to choose default behaviour
	 */
	Boolean isTransient(Object entity);

	/**
	 * Called from <tt>flush()</tt>. The return value determines whether the entity is updated
	 * <ul>
	 * <li>an array of property indices - the entity is dirty
	 * <li>an empty array - the entity is not dirty
	 * <li><tt>null</tt> - use Hibernate's default dirty-checking algorithm
	 * </ul>
	 *
	 * @param entity The entity for which to find dirty properties.
	 * @param id The identifier of the entity
	 * @param currentState The current entity state as taken from the entity instance
	 * @param previousState The state of the entity when it was last synchronized (generally when it was loaded)
	 * @param propertyNames The names of the entity properties.
	 * @param javaTypeDescriptors The {@link JavaTypeDescriptor} of the entity properties
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
			JavaTypeDescriptor[] javaTypeDescriptors) {
		return findDirty(
				entity,
				(Serializable) id,
				currentState,
				previousState,
				propertyNames,
				javaTypeDescriptors
		);
	}

	/**
	 * @deprecated Use {@link #onCollectionUpdate(Object, Object)} instead
	 */
	@Deprecated
	default int[] findDirty(
			Object entity,
			Serializable id,
			Object[] currentState,
			Object[] previousState,
			String[] propertyNames,
			JavaTypeDescriptor[] javaTypeDescriptors) {
		return null;
	}

	/**
	 * Instantiate the entity class. Return <tt>null</tt> to indicate that Hibernate should use
	 * the default constructor of the class. The identifier property of the returned instance
	 * should be initialized with the given identifier.
	 *
	 * @param entityName the name of the entity
	 * @param modelRepresentation The domain model representation (pojo, map, etc)
	 * @param id the identifier of the new instance
	 *
	 * @return an instance of the class, or <tt>null</tt> to choose default behaviour
	 *
	 * @throws CallbackException Thrown if the interceptor encounters any problems handling the callback.
	 */
	default Object instantiate(String entityName, RepresentationMode modelRepresentation, Object id) throws CallbackException {
		return instantiate( entityName, EntityMode.fromRepresentation( modelRepresentation ), (Serializable) id );
	}

	/**
	 * @deprecated (since 6.0) Use {@link #instantiate(String, RepresentationMode, Object)} instead
	 */
	@Deprecated
	default Object instantiate(String entityName, EntityMode entityMode, Serializable id) throws CallbackException {
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
	 */
	String getEntityName(Object object) throws CallbackException;

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
		// make this the default because we are generally calling user/application code here
		// which would implement the Serializable form - they will be warned about the deprecation
		return getEntity( entityName, (Serializable) id );
	}

	/**
	 * @deprecated (since 6.0) Use {@link #getEntity(String, Object)} instead
	 */
	@Deprecated
	default Object getEntity(String entityName, Serializable id) throws CallbackException {
		return null;
	}

	/**
	 * Called when a Hibernate transaction is begun via the Hibernate <tt>Transaction</tt> 
	 * API. Will not be called if transactions are being controlled via some other 
	 * mechanism (CMT, for example).
	 *
	 * @param tx The Hibernate transaction facade object
	 */
	void afterTransactionBegin(Transaction tx);

	/**
	 * Called before a transaction is committed (but not before rollback).
	 *
	 * @param tx The Hibernate transaction facade object
	 */
	void beforeTransactionCompletion(Transaction tx);

	/**
	 * Called after a transaction is committed or rolled back.
	 *
	 * @param tx The Hibernate transaction facade object
	 */
	void afterTransactionCompletion(Transaction tx);
}
