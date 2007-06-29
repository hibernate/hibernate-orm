//$Id: UserVersionType.java 7736 2005-08-03 20:03:34Z steveebersole $
package org.hibernate.usertype;

import java.util.Comparator;

import org.hibernate.engine.SessionImplementor;

/**
 * A user type that may be used for a version property
 * 
 * @author Gavin King
 */
public interface UserVersionType extends UserType, Comparator {
	/**
	 * Generate an initial version.
	 *
	 * @param session The session from which this request originates.  May be
	 * null; currently this only happens during startup when trying to determine
	 * the "unsaved value" of entities.
	 * @return an instance of the type
	 */
	public Object seed(SessionImplementor session);
	/**
	 * Increment the version.
	 *
	 * @param session The session from which this request originates.
	 * @param current the current version
	 * @return an instance of the type
	 */
	public Object next(Object current, SessionImplementor session);

}
