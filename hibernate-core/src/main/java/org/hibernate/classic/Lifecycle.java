/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.classic;

import org.hibernate.CallbackException;
import org.hibernate.Session;

import java.io.Serializable;

/**
 * Provides callbacks from the {@code Session} to the persistent object.
 * Persistent classes <b>may</b> implement this interface but they are not
 * required to.
 * <ul>
 * <li><b>onSave:</b> called just before the object is saved
 * <li><b>onUpdate:</b> called just before an object is updated,
 * ie. when {@code Session.update()} is called
 * <li><b>onDelete:</b> called just before an object is deleted
 * <b>onLoad:</b> called just after an object is loaded
 * </ul>
 * <p>
 * {@code onLoad()} may be used to initialize transient properties of the
 * object from its persistent state. It may <b>not</b> be used to load
 * dependent objects since the {@code Session} interface may not be
 * invoked from inside this method.
 * <p>
 * A further intended usage of {@code onLoad()}, {@code onSave()} and
 * {@code onUpdate()} is to store a reference to the {@code Session}
 * for later use.
 * <p>
 * If {@code onSave()}, {@code onUpdate()} or {@code onDelete()} return
 * {@code VETO}, the operation is silently vetoed. If a
 * {@code CallbackException} is thrown, the operation is vetoed and the
 * exception is passed back to the application.
 * <p>
 * Note that {@code onSave()} is called after an identifier is assigned
 * to the object, except when identity column key generation is used.
 *
 * @see CallbackException
 * @see jakarta.persistence.EntityListeners
 * @see jakarta.persistence.PrePersist
 * @see jakarta.persistence.PreRemove
 * @see jakarta.persistence.PreUpdate
 * @see jakarta.persistence.PostLoad
 * @see jakarta.persistence.PostPersist
 * @see jakarta.persistence.PostRemove
 * @see jakarta.persistence.PostUpdate
 *
 * @author Gavin King
 */
public interface Lifecycle {

	/**
	 * Return value to veto the action (true)
	 */
	boolean VETO = true;

	/**
	 * Return value to accept the action (false)
	 */
	boolean NO_VETO = false;

	/**
	 * Called when an entity is saved.
	 * @param s the session
	 * @return true to veto save
	 * @throws CallbackException Indicates a problem happened during callback
	 */
	default boolean onSave(Session s) throws CallbackException {
		return NO_VETO;
	}

	/**
	 * Called when an entity is passed to {@code Session.update()}.
	 * This method is <em>not</em> called every time the object's
	 * state is persisted during a flush.
	 * @param s the session
	 * @return true to veto update
	 * @throws CallbackException Indicates a problem happened during callback
	 */
	default boolean onUpdate(Session s) throws CallbackException {
		return NO_VETO;
	}

	/**
	 * Called when an entity is deleted.
	 * @param s the session
	 * @return true to veto delete
	 * @throws CallbackException Indicates a problem happened during callback
	 */
	default boolean onDelete(Session s) throws CallbackException {
		return NO_VETO;
	}

	/**
	 * Called after an entity is loaded. <em>It is illegal to
	 * access the {@code Session} from inside this method.</em>
	 * However, the object may keep a reference to the session
	 * for later use.
	 *
	 * @param s the session
	 * @param id the identifier
	 */
	default void onLoad(Session s, Object id) {
		if (id==null || id instanceof Serializable) {
			onLoad(s, (Serializable) id);
		}
	}

	/**
	 * Called after an entity is loaded. <em>It is illegal to
	 * access the {@code Session} from inside this method.</em>
	 * However, the object may keep a reference to the session
	 * for later use.
	 *
	 * @param s the session
	 * @param id the identifier
	 *
	 * @deprecated use {@link #onLoad(Session, Object)}
	 */
	@Deprecated(since = "6.0")
	default void onLoad(Session s, Serializable id) {}
}
