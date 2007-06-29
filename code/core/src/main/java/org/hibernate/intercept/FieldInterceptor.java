package org.hibernate.intercept;

import org.hibernate.engine.SessionImplementor;

/**
 * Contract for field interception handlers.
 *
 * @author Steve Ebersole
 */
public interface FieldInterceptor {

	/**
	 * Use to associate the entity to which we are bound to the given session.
	 *
	 * @param session The session to which we are now associated.
	 */
	public void setSession(SessionImplementor session);

	/**
	 * Is the entity to which we are bound completely initialized?
	 *
	 * @return True if the entity is initialized; otherwise false.
	 */
	public boolean isInitialized();

	/**
	 * The the given field initialized for the entity to which we are bound?
	 *
	 * @param field The name of the field to check
	 * @return True if the given field is initialized; otherwise false.
	 */
	public boolean isInitialized(String field);

	/**
	 * Forcefully mark the entity as being dirty.
	 */
	public void dirty();

	/**
	 * Is the entity considered dirty?
	 *
	 * @return True if the entity is dirty; otherwise false.
	 */
	public boolean isDirty();

	/**
	 * Clear the internal dirty flag.
	 */
	public void clearDirty();
}
