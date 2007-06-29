//$Id$
package org.hibernate;

/**
 * Thrown when the user passes a persistent instance to a <tt>Session</tt>
 * method that expects a transient instance.
 *
 * @author Gavin King
 */
public class PersistentObjectException extends HibernateException {
	
	public PersistentObjectException(String s) {
		super(s);
	}
}






