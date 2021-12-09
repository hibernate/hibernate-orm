/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.classic;

import org.hibernate.CallbackException;
import org.hibernate.Session;

import java.io.Serializable;

/**
 * Provides callbacks from the <tt>Session</tt> to the persistent object.
 * Persistent classes <b>may</b> implement this interface but they are not
 * required to.
 * <ul>
 * <li><b>onSave:</b> called just before the object is saved
 * <li><b>onUpdate:</b> called just before an object is updated,
 * ie. when <tt>Session.update()</tt> is called
 * <li><b>onDelete:</b> called just before an object is deleted
 * <b>onLoad:</b> called just after an object is loaded
 * </ul>
 * <p>
 * <tt>onLoad()</tt> may be used to initialize transient properties of the
 * object from its persistent state. It may <b>not</b> be used to load
 * dependent objects since the <tt>Session</tt> interface may not be
 * invoked from inside this method.
 * <p>
 * A further intended usage of <tt>onLoad()</tt>, <tt>onSave()</tt> and
 * <tt>onUpdate()</tt> is to store a reference to the <tt>Session</tt>
 * for later use.
 * <p>
 * If <tt>onSave()</tt>, <tt>onUpdate()</tt> or <tt>onDelete()</tt> return
 * <tt>VETO</tt>, the operation is silently vetoed. If a
 * <tt>CallbackException</tt> is thrown, the operation is vetoed and the
 * exception is passed back to the application.
 * <p>
 * Note that <tt>onSave()</tt> is called after an identifier is assigned
 * to the object, except when identity column key generation is used.
 *
 * @see CallbackException
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
	 * Called when an entity is passed to <tt>Session.update()</tt>.
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
	 * access the <tt>Session</tt> from inside this method.</em>
	 * However, the object may keep a reference to the session
	 * for later use.
	 *
	 * @param s the session
	 * @param id the identifier
	 */
	default void onLoad(Session s, Object id) {
		if (id instanceof Serializable) {
			onLoad(s, (Serializable) id);
		}
	}

	/**
	 * Called after an entity is loaded. <em>It is illegal to
	 * access the <tt>Session</tt> from inside this method.</em>
	 * However, the object may keep a reference to the session
	 * for later use.
	 *
	 * @param s the session
	 * @param id the identifier
	 *
	 * @deprecated use {@link #onLoad(Session, Object)}
	 */
	@Deprecated
	default void onLoad(Session s, Serializable id) {}
}
