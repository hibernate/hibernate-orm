//$Id: SessionException.java 9024 2006-01-11 22:38:24Z steveebersole $
package org.hibernate;

/**
 * Thrown when the user calls a method of a {@link Session} that is in an
 * inappropropriate state for the given call (for example, the the session
 * is closed or disconnected).
 *
 * @author Gavin King
 */
public class SessionException extends HibernateException {

	/**
	 * Constructs a new SessionException with the given message.
	 *
	 * @param message The message indicating the specific problem.
	 */
	public SessionException(String message) {
		super( message );
	}

}
